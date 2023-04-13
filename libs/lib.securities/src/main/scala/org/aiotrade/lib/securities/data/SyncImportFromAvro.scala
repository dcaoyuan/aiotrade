/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.data


import java.util.TimerTask
import java.util.logging.Level
import java.util.logging.Logger

/**
 * 
 * @author Caoyuan Deng
 */
object SyncImportFromAvro extends SyncBasis {
  private val log = Logger.getLogger(this.getClass.getName)
  def main(args: Array[String]){
    val date = getNearestTime(9, 5)
    println("Will import data to db at " + date)
    timer.scheduleAtFixedRate(new TimerTask(){
        def run(){
          try{
            importAvroDataFileToTestMysql
          }
          catch{
            case ex: Throwable =>log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      }, date, 12 * 3600 * 1000L)
    
  }
}