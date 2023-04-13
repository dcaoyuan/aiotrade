package org.aiotrade.lib.io

import java.io.File
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import org.aiotrade.lib.util.actors.Publisher


class FileWatcher(file: File) extends TimerTask with Publisher {
  private var timeStamp: Long = file.lastModified

  final def run {
    apply()
  }

  final def apply() {
    if (file.exists) {
      val timestamp = file.lastModified
      if (this.timeStamp != timestamp) {
        this.timeStamp = timestamp
        onChange(FileModified(file, timestamp))
      }
    } else {
      onChange(FileDeleted(file, System.currentTimeMillis))
    }
  }

  /**
   * Override it if you want sync processing
   */
  protected def onChange(event: FileEvent) {
    publish(event)
  }
}


object FileWatcher {
  // ----- simple test
  def main(args: Array[String]) {
    val task = new FileWatcher(new File("temp.txt")) {
      override protected def onChange(event: FileEvent) {
        println(event)
      }
    }

    val timer = new Timer
    timer.schedule(task, new Date, 1000)
  }
}
