package org.aiotrade.lib.io

import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import java.util.logging.{Logger, Level}
import scala.collection.mutable


/**
 * The DirWatcher watches two directories simutaneously.
 * If a file with the same file name emerges in the two directories,
 * only the latest one will be notified, the old one will be ignored.
 *
 * If only one path is provided for DirWatcher and the other path is null,
 * the Dirwatcher will only watch one directory and ignore the null path.
 *
 * @author Caoyuan Deng, Guibin Zhang
 */
@throws(classOf[IOException])
abstract class DirWatcher(paths: List[File], filter: FileFilter, includingExistingFiles: Boolean = false) extends TimerTask {
  private val log = Logger.getLogger(this.getClass.getName)
  log.log(Level.INFO, "Watching on " + paths)
  
  protected val NOT_SURE = Long.MinValue
  protected val fileNameToLastModified = init

  def this(paths: List[String], endWith: String) =
    this(paths.map(new File(_)), new DefaultWatcherFilter(endWith), false)

  def this(paths: List[String], filter: FileFilter) =
    this(paths.map(new File(_)), filter, false)
  
  def this(path: File, filter: FileFilter, includingExistingFiles: Boolean) =
    this(List(path), filter, includingExistingFiles)

  def this(path: File, filter: FileFilter) = this(path, filter, false)
  def this(path: String, filter: FileFilter) = this(new File(path), filter)
  def this(path: String, endWith: String) = this(path, new DefaultWatcherFilter(endWith))
  def this(path: String) = this(path, "")

  /**
   * Processing the existing files in the watched directories
   */
  private def init = {
    val map = scanFiles
    /** @TODO Here are NullPointerException bug to be fixed!! */
    if (includingExistingFiles && map.size > 0) {
      map.values.sortWith{(a, b) => a._1.compareTo(b._1) < 0} foreach {x => onChange(FileAdded(x._1, x._2))}
    }
    
    map
  }
  
  /**
   * Scan all directories to get current files.
   */ 
  private def scanFiles: WatcherMap = {
    val map = new WatcherMap
    
    for (path <- paths if path != null) {
      try {
        path listFiles filter match {
          case null => log.log(Level.SEVERE, path + " is not a valid directory or I/O error occurs.")
          case files => 
            var i = 0
            while (i < files.length) {
              val file = files(i)
              lastModified(file) match {
                case NOT_SURE =>
                case time => map.put(file, time)
              }
              i += 1
            }
        }
      } catch {
        case ex: Exception => log.log(Level.SEVERE, ex.getMessage, ex)
      }
    }
    
    map
  }

  final def run {
    apply()
  }

  /** always add () for empty apply method */
  final def apply() {
    val start = System.currentTimeMillis
    val newMap = scanFiles
    
    val listDuration = System.currentTimeMillis - start
    if (listDuration > 1000) {
      log.log(Level.WARNING, "Listing " + paths + " costs " + listDuration/1000.0 + " seconds")
    }

    // It is to Guarantee that the name strings in the resulting array will appear in alphabetical order.
    val fileNames = newMap.toFileNames.sortWith(_.compareTo(_) < 0)
    val checkedFiles = mutable.Set[String]()

    var i = 0
    while (i < fileNames.length) {
      val fileName = fileNames(i)
      checkedFiles += fileName

      fileNameToLastModified.get(fileName) match {
        case None =>
          // new file
          val newTime = newMap.get(fileName).get
          val file = newMap.fileOf(fileName).get
          if (file.canRead) {
            fileNameToLastModified.put(file, newTime)
            onChange(FileAdded(file, newTime))
          }
        case Some(oldTime)  =>
          // modified file
          val newTime = newMap.get(fileName).get
          if (oldTime < newTime) {
            val file = newMap.fileOf(fileName).get
            if (file.canRead) {
              fileNameToLastModified.put(file, newTime)
              onChange(FileModified(file, newTime))
            }
          } else {
            // Ingore the old one and current one, only care the newer one.
          }
      }
      
      i += 1
    }

    // deleted files
    val deletedfiles = (mutable.Set() ++ fileNameToLastModified.toFileNames.clone) -- checkedFiles
    deletedfiles foreach {fileName =>
      val file = fileNameToLastModified.fileOf(fileName).get
      val time = fileNameToLastModified.get(fileName).get
      fileNameToLastModified remove file
      onChange(FileDeleted(file, time))
    }
    
    val duration = System.currentTimeMillis - start
    if (duration > 2000) {
      log.log(Level.WARNING, "Scaning " + paths + " costs " + duration / 1000F + " seconds")
    }
  }

  /**
   * Override it if you want sync processing
   */
  protected def onChange(event: FileEvent) {}

  /**
   * Override it if you want to get the timestamp by other way,
   * such as by some the content of the file
   *
   * @return last modified time or NOT_SURE
   */
  protected def lastModified(file: File): Long = {
    file.lastModified
  }
    
  protected class WatcherMap {
    private val map = mutable.Map[String, (File, Long)]()

    /**
     * Filter out the duplicated file whose file name is same,
     * only keep the latest one according to lastModified()
     */
    def put(file: File, lastModified: Long): Boolean = {
      map.get(file.getName) match {
        case Some((f, time)) =>
          // Only keep the latest one
          if (time < lastModified) {
            map(file.getName) = (file, lastModified)
            true
          } else {
            false
          }
        case None =>
          map(file.getName) = (file, lastModified)
          true
      }
    }

    def get(file: File): Option[Long] = {
      map.get(file.getName) match {
        case Some((file, time)) => Some(time)
        case None => None
      }
    }

    def get(fileName: String): Option[Long] = {
      map.get(fileName) match {
        case Some((file, time)) => Some(time)
        case None => None
      }
    }

    def fileOf(fileName: String): Option[File] = {
      map.get(fileName) match {
        case Some((file, time)) => Some(file)
        case None => None
      }
    }

    def remove(file: File) {
      map.get(file.getName) match {
        case Some((file, time)) => map -= file.getName
        case _ =>
      }
    }
    
    def keys = map.keys.toArray
    def values = map.values.toArray

    def toFiles: Array[File] = {
      map.values.map(_._1).toArray
    }

    def toFileNames: Array[String] = {
      map.keys.toArray
    }

    def clear {
      map.clear
    }

    def getAll = map

    def size = map.size
  }
}


class DefaultWatcherFilter(filter: String) extends FileFilter {
  def this() = this("")

  def accept(file: File): Boolean = {
    filter == "" || file.getName.endsWith(filter)
  }
}



object DirWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    //Test watching the single directory
    val task = new DirWatcher("/tmp/test01", "txt" ) {
      override protected def onChange(event: FileEvent) {
        println("task:----- " + event)
      }
      override protected def lastModified(file: File): Long = {
        val f = new java.io.BufferedReader(new java.io.FileReader(file))
        val timestamp = f.readLine.toLong
        f.close
        timestamp
      }

    }
    val timer = new Timer
    timer.schedule(task , new Date, 1000)

    //Test watching the two directories simutaneously
    val task2 = new DirWatcher(List("/tmp/test02", "/tmp/test03"), ".txt") {
      override protected def onChange(event: FileEvent) {
        println("task2:----- " + event)
      }

      override protected def lastModified(file: File): Long = {
        val f = new java.io.BufferedReader(new java.io.FileReader(file))
        val timestamp = f.readLine.toLong
        f.close
        timestamp
      }
    }
    val timer2 = new Timer
    timer2.schedule(task2, new Date, 1000)
    
  }
}
