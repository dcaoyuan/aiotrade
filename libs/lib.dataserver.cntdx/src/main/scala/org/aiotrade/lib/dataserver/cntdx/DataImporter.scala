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

package org.aiotrade.lib.dataserver.cntdx

import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quotes1d
import ru.circumflex.orm._

/**
 * @author Caoyuan Deng
 */
object DataImporter {
  private val log = Logger.getLogger(getClass.getName)
  
  private val debug = false
  
  private val srcMainResources = "src/main/resources/"
  private val userHome = System.getProperty("user.home")
  val vipDocPath = userHome + "/myprjs/aiotrade.git/data/tdxdata/Vipdoc/"

  def main(args: Array[String]) {
    try {
      importTdxDayFilesToTestMysql
    } catch {
      case ex: Throwable => 
        log.log(Level.SEVERE, ex.getMessage, ex)
        System.exit(1)
    }
    System.exit(0)
  }
  
  def importTdxDayFilesToTestMysql {
    org.aiotrade.lib.util.config.Config(srcMainResources + File.separator + "import_to_test.conf")
    readQuotesFrom(vipDocPath)
  }

  def readQuotesFrom(dataPath: String) {
    val shDir = new File(dataPath + "sh/lday")
    val szDir = new File(dataPath + "sz/lday")

    for (dir <- List(shDir, szDir) if dir.exists) {
      var success = 0
      var failure = 0
      for (file <- dir.listFiles) {
        val fileName = file.getName
        val ex = fileName.substring(0, 2)
        val symbol = fileName.substring(2, 8) + (if (ex.equalsIgnoreCase("SH")) ".SS" else ".SZ")

        val quotes = TdxDayReader.readQuotes(file)
        val sec = Exchange.secOf(symbol) match {
          case Some(sec) =>
            quotes.foreach{_.sec = sec}
              
            if (!debug) {
              try {
                Quotes1d.insertBatch_!(quotes)
                COMMIT
                println("Commited")
                success += 1
                println("Success " + symbol + ": " + quotes.length)
              } catch {
                case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
              }
            }
              
          case None =>
            failure += 1
            println("!!!! Failed to find sec of " + symbol + ": " + quotes.length)
        }
      }
      println("\n======== Processed files with success: " + success + ", failure: " + failure)
    }
  }
}
