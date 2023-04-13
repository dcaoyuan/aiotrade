/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  o Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  o Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  o Neither the name of AIOTrade Computing Co. nor the names of
 *    its contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.aiotrade.lib.securities.data

import java.io.File
import java.io.FileOutputStream
import java.net.JarURLConnection
import java.net.URL
import java.util.Calendar
import java.util.Date
import java.util.jar.JarFile
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.securities.data.git.Git
import org.aiotrade.lib.securities.model._
import org.eclipse.jgit.api.MergeResult
import ru.circumflex.orm._
import ru.circumflex.orm.avro.AvroReader
import scala.reflect.ClassTag

/**
 *
 * @author Caoyuan Deng
 */
abstract class SyncBasis {
  private val log = Logger.getLogger(this.getClass.getName)
  val timer = new java.util.Timer("Sync")

  private[data] val srcMainResources = "src/main/resources/"
  private[data] val exportDataDirPath = "../data"
  
  private var localDataGit: Option[org.eclipse.jgit.api.Git] = None
  
  /**
   * @Note lazy call them so we can specify config file before orm package
   */
  private lazy val basisTables = List(Secs,
                                      SecInfos,
                                      Sectors,
                                      SectorSecs
  )

  def test() {
    test(Secs)
    test(SecInfos)
    test(Secs)
    test(SecInfos)
  }

  protected def getNearestTime(hour: Int, min: Int): Date = {
    if (hour < 0 || hour > 23 || min < 0 || min > 59) return new Date(System.currentTimeMillis + 5000L)
    val cur = new Date(System.currentTimeMillis)
    val date = if (cur.getHours < hour || (cur.getHours == hour && cur.getMinutes < min)) cur.getDate else cur.getDate + 1
    val cal = Calendar.getInstance
    cal.set(cur.getYear + 1900, cur.getMonth, date, hour, min, 0)
    cal.getTime
  }
  
  private def test[R: ClassTag](table: Table[R]) {
    //org.aiotrade.lib.util.config.Config(srcMainResources + File.separator + "import_to_test.conf")
    val records = selectAvroRecords(exportDataDirPath + File.separator + table.relationName + ".avro", table).toArray
    records foreach println
    println("Total: " + records.length)
    //table.insertBatch(records, false)
  }
  
  
  private def testPrint(table: Table[_]) {
    val tableName = table.relationName
    val fileName = SyncUtil.exportDataDirPath + "/" + tableName + ".avro"
    AvroReader.read(fileName) {
      case null => println("null")
      case row =>  println(row.mkString(" \t| "))
    }
  } 

  def exportAvroDataFileFromProductionMysql() {
    org.aiotrade.lib.util.config.Config()
    exportToAvro(exportDataDirPath)
  }
  
  def importAvroDataFileToTestMysql() {
    org.aiotrade.lib.util.config.Config()
    importDataFrom(exportDataDirPath)
  }


  // --- API methods

  def exportBaseTablesToAvro(destDirPath: String) {
    exportToAvro(destDirPath)
  }
  
  /**
   * all avro file size is about 1708959
   */
  def exportToAvro(destDirPath: String) {
    val t0 = System.currentTimeMillis
    val holdingRecords = basisTables map {x => SELECT (x.*) FROM (x) list()}
    basisTables foreach {x => exportToAvro(destDirPath, x)}
    log.info("Exported to avro in " + (System.currentTimeMillis - t0) + " ms.")
  }

  private def exportToAvro[R](destDirPath: String, x: Relation[R]) {
    SELECT (x.*) FROM (x) toAvro(destDirPath + File.separator + x.relationName + ".avro")
  }

  def importDataFrom(dataDir: String) {
    var t0 = System.currentTimeMillis
    schema()
    log.info("Created schema in " + (System.currentTimeMillis - t0) / 1000.0 + " s.")
    
    t0 = System.currentTimeMillis
    val holdingRecords = basisTables map {x => selectAvroRecords(dataDir + File.separator +  x.relationName + ".avro", x)}
    basisTables foreach {x => importAvroToDb(dataDir + File.separator + x.relationName + ".avro", x)}
    COMMIT
    log.info("Imported data to db in " + (System.currentTimeMillis - t0) / 1000.0 + " s.")
  }
  
  private def selectAvroRecords[R](avroFile: String, table: Table[R]) = {
    SELECT (table.*) FROM (AVRO(table, avroFile)) list()
  }

  /**
   * @Note
   * per: tables foreach {x => ...}, 'x' will be infered by Scala as ScalaObject, we have to define
   * this standalone function to get type right
   */
  private def importAvroToDb[R: ClassTag](avroFile: String, table: Table[R]) {
    val records = selectAvroRecords(avroFile, table).toArray
    table.insertBatch(records, false)
  }
  
  /**
   * @Note All tables should be ddl.dropCreate together, since schema will be
   * droped before create tables each time.
   */
  def schema() {
    val tables = List(
      // -- basic tables
      Secs, SecInfos, Sectors, SectorSecs
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => log.info(msg.body))
  }
  
  /**
   * Extract data to destPath from jar file
   * @see http://bits.netbeans.org/dev/javadoc/org-openide-modules/org/openide/modules/InstalledFileLocator.html
   */
  def extractDataTo(destPath: String) {
    var jarFile: JarFile = null
    try {
      // locate jar 
      val locator = classOf[Locator]
      // @Note We'll get a org.netbeans.JarClassLoader$NbJarURLConnection, which seems cannot call jarUrl.openStream
      val url = locator.getProtectionDomain.getCodeSource.getLocation
      log.info("Initial data is located at: " + url)

      val urlStr = url.toExternalForm
      val jarUrl = if (urlStr.startsWith("jar:")) url else new URL("jar:" + urlStr + "!/")
      jarFile = jarUrl.openConnection.asInstanceOf[JarURLConnection].getJarFile

      val t0 = System.currentTimeMillis
      val buf = new Array[Byte](1024)
      val entries = jarFile.entries
      while (entries.hasMoreElements) {
        val entry = entries.nextElement
        val entryName = entry.getName
        if (entryName != "data/" && entryName.startsWith("data/")) {
          var fileName = entryName.substring(4, entryName.length)
          if (fileName.charAt(fileName.length - 1) == '/') fileName = fileName.substring(0, fileName.length - 1)
          if (fileName.charAt(0) == '/') fileName = fileName.substring(1)
          if (File.separatorChar != '/') fileName = fileName.replace('/', File.separatorChar)
        
          val file = new File(destPath, fileName)
          if (entry.isDirectory) {
            // make sure the directory exists
            file.mkdirs
          }  else {
            // make sure the directory exists
            val parent = file.getParentFile
            if (parent != null && !parent.exists) {
              parent.mkdirs
            }
            
            // dump the file
            val in = jarFile.getInputStream(entry)
            val out = new FileOutputStream(file)
            var len = 0
            while ({len = in.read(buf, 0, buf.length); len != -1}) {
              out.write(buf, 0, len)
            }
            out.flush
            out.close
            file.setLastModified(entry.getTime)
            in.close
          }
        }         
      }
      
      // rename folder "data/dotgit" to "data/.git"
      val gitFile = new File(destPath, "dotgit")
      if (gitFile.exists) {
        gitFile.renameTo(new File(destPath, ".git"))
        initLocalDataGit(destPath)
      }
      
      log.info("Extract data to " + destPath + " in " + (System.currentTimeMillis - t0) + "ms")
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
    } finally {
      if (jarFile != null) {
        try {
          jarFile.close
        } catch {
          case _: Throwable =>
        }
      }
    }
  }
  
  def initLocalDataGit(destPath: String) {
    localDataGit = Git.getGit(destPath + "/.git")
  }
  
  @throws(classOf[Exception])
  def syncLocalData() {
    localDataGit match {
      case None => // @todo, clone a new one
      case Some(git) =>
        Git.pull(git) match {
          case Some(pullResult) =>
            val willResetSearchTable = pullResult.getMergeResult match {
              case null => false
              case x => 
                import MergeResult.MergeStatus
                val status = x.getMergeStatus 
                log.warning("Pull status is: " + status)
                status match {
                  case MergeStatus.FAST_FORWARD | MergeStatus.MERGED => true
                  case MergeStatus.ALREADY_UP_TO_DATE => false
                  case _ => false
                }
            }
          
            // refresh secs, secInfos, secDividends etc
            if (willResetSearchTable) {
              Exchange.resetSearchTables
            }
            
          case None => log.warning("Pull result is none")
        }
    }
  }
    

}
