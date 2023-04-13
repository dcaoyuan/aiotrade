package org.aiotrade.lib.securities.data

import java.util.TimerTask
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 
 * @author Caoyuan Deng
 */
object SyncExportToAvro extends SyncBasis {
  private val log = Logger.getLogger(this.getClass.getName)
  def main(args: Array[String]){
    val date = getNearestTime(9, 0)
    println("Will export data from db at " + date)
    timer.scheduleAtFixedRate(new TimerTask(){
        def run(){
          try{
            exportAvroDataFileFromProductionMysql
          }
          catch{
            case ex: Throwable =>log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      }, date, 12 * 3600 * 1000)

  }
}