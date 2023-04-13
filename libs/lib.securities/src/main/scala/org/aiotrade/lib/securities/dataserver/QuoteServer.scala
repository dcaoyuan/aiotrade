/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.securities.dataserver

import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import ru.circumflex.orm._

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 *
 * @author Caoyuan Deng
 */
abstract class QuoteServer extends DataServer[Quote] {
  type C = QuoteContract

  private val log = Logger.getLogger(this.getClass.getSimpleName)

  /**
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   */
  protected def processData(quotes: Array[Quote], contract: QuoteContract): Long = {
    val uniSymbol = toUniSymbol(contract.srcSymbol)
    val sec = Exchange.secOf(uniSymbol) getOrElse {
      log.warning("No sec for: " + uniSymbol)
      return contract.loadedTime
    }    
    log.info("Got quotes from source for " + uniSymbol + "(" + contract.freq + "), size=" + quotes.length)

    var frTime = contract.loadedTime
    var toTime = contract.loadedTime
    var i = -1
    while ({i += 1; i < quotes.length}) {
      val quote = quotes(i)
      quote.sec = sec
      quote.unfromMe_!
      frTime = math.min(quote.time, frTime)
      toTime = math.max(quote.time, toTime)
    }

    val ser = contract.freq match {
      case TFreq.ONE_SEC => sec.realtimeSer
      case x => sec.serOf(x).get
    }
    ser ++= quotes

    ser.publish(TSerEvent.Loaded(ser, uniSymbol, frTime, toTime))

    // save to db after published TSerEvent, so the chart showing won't be blocked
    contract.freq match {
      case TFreq.DAILY =>
        Quotes1d.saveBatch(sec, quotes)
        COMMIT
      case TFreq.ONE_MIN =>
        Quotes1m.saveBatch(sec, quotes)
        COMMIT
      case _ =>
        // we won't save quote to quotes1m when contract.freq is ONE_SEC, so we can always keep
        // quoteSer of 1min after loaded from db will not be blocked by this period of time.
    }

    if (contract.isRefreshable) {
      startRefresh
    }
    
    contract.loadedTime = toTime
    toTime
  }

  /**
   * Override to provide your options
   * @return supported frequency array.
   */
  def supportedFreqs: Array[TFreq] = Array()

  def isFreqSupported(freq: TFreq): Boolean = supportedFreqs exists (_ == freq)

  def classNameOfTickerServer: Option[String]

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}

