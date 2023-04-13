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

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.api
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Execution
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.MarketDepth
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.SecSnap
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.securities.model.TickersLast
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.collection.ArrayList
import ru.circumflex.orm._
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
//case class DepthSnap (
//  prevPrice: Double,
//  prevDepth: MarketDepth,
//  execution: Execution
//)

abstract class TickerServer extends DataServer[Ticker] {
  type C = TickerContract

  private val log = Logger.getLogger(this.getClass.getName)
  
  private lazy val uniSymbolToSnapTicker = mutable.Map[String, Ticker]()

  def snapTickerOf(uniSymbol: String): Ticker = uniSymbolToSnapTicker synchronized {
    uniSymbolToSnapTicker.get(uniSymbol).getOrElse{
      val newOne = new Ticker
      uniSymbolToSnapTicker += (uniSymbol -> newOne)
      newOne.uniSymbol = uniSymbol
      newOne
    }
  }

  private val allTickers = new ArrayList[Ticker]
  private val allExecutions = new ArrayList[Execution]
  private val allUpdatedDailyQuotes = new ArrayList[Quote]
  private val allUpdatedMinuteQuotes = new ArrayList[Quote]
  private val allUpdatedDailyMoneyFlows = new ArrayList[MoneyFlow]
  private val allUpdatedMinuteMoneyFlows = new ArrayList[MoneyFlow]

  private val exchangeToLastTime = mutable.Map[Exchange, Long]()

  private def toSecSnaps(values: Array[Ticker]): (Seq[SecSnap], Seq[Ticker]) = {
    val processedSymbols = mutable.Set[String]() // used to avoid duplicate symbols of each refreshing

    val length = values.length
    val secSnaps = new ArrayList[SecSnap](length)
    val tickersLast = new ArrayList[Ticker](length)
    var i = -1
    while ({i += 1; i < length}) {
      val ticker = values(i)
      val symbol = ticker.uniSymbol

      if (!processedSymbols.contains(symbol) && ticker.dayHigh != 0 && ticker.dayLow != 0) {
        processedSymbols.add(symbol)
        
        Exchange.secOf(symbol) match {
          case Some(sec) =>
            ticker.sec = sec

            val exchange = sec.exchange
            val tickerx = exchange.gotLastTicker(ticker)
            if (subscribedSrcSymbols.contains(symbol)) {
              tickersLast += tickerx
              secSnaps += sec.secSnap.setByTicker(ticker)
            } else {
              log.info("Discard ticker: " + ticker.uniSymbol + " -> subscribedSrcSymbols doesn't contain it")
            }
          case None => log.warning("No sec for " + symbol)
        }
      } else {
        if (ticker.dayHigh == 0 || ticker.dayLow == 0) {
          log.info("Discard ticker: " + ticker.uniSymbol + " -> dayHigh=" + ticker.dayHigh + ", dayLow=" + ticker.dayLow)
        } else {
          log.info("Discard ticker: " + ticker.uniSymbol + " -> duplicate in this batch processing")
        }
      }
    }
    
    (secSnaps, tickersLast)
  }

  /**
   * compose ser using data from Tickers
   * @param Tickers
   */
  protected def processData(tickers: Array[Ticker], Contract: TickerContract): Long = {
    var lastTime = Long.MinValue

    log.info("Composing quote from tickers: " + tickers.length)
    if (tickers.length == 0) return lastTime

    if (TickerServer.isServer) Exchange.checkIfSomethingNew(tickers)
    
    val (secSnaps, tickersLast) = toSecSnaps(tickers)
    log.info("Composing quote from secSnaps: " + secSnaps.length)
    if (secSnaps.length == 0) return lastTime

    allTickers.clear
    allExecutions.clear
    allUpdatedDailyQuotes.clear
    allUpdatedMinuteQuotes.clear
    allUpdatedDailyMoneyFlows.clear
    allUpdatedMinuteMoneyFlows.clear

    exchangeToLastTime.clear

    var i = -1
    while ({i += 1; i < secSnaps.length}) {
      val secSnap = secSnaps(i)

      val sec = secSnap.sec
      val ticker = secSnap.newTicker
      val lastTicker = secSnap.lastTicker
      val isDayFirst = ticker.isDayFirst
      val dayQuote = secSnap.dayQuote
      val minQuote = secSnap.minQuote
      val dayMoneyFlow = secSnap.dayMoneyFlow
      val minMoneyFlow = secSnap.minMoneyFlow

      log.fine("Composing from ticker: " + ticker + ", lasticker: " + lastTicker)

      var tickerValid = false
      var execution: Execution = null
      if (isDayFirst) {
        log.fine("Got day's first ticker: " + ticker)
        
        /**
         * this is today's first ticker we got when begin update data server,
         * actually it should be, so maybe we should check this.
         * As this is the first data of today:
         * 1. set OHLC = Ticker.LAST_PRICE
         * 2. to avoid too big volume that comparing to following dataSeries.
         * so give it a small 0.0001 (if give it a 0, it will won't be calculated
         * in calcMaxMin() of ChartView)
         */

        tickerValid = true
        
        dayQuote.unjustOpen_!
        dayMoneyFlow.unjustOpen_!
        minMoneyFlow.unjustOpen_!

        minQuote.unjustOpen_!
        minQuote.open   = ticker.dayOpen
        minQuote.high   = ticker.dayHigh
        minQuote.low    = ticker.dayLow
        minQuote.close  = ticker.lastPrice
        minQuote.volume = ticker.dayVolume
        minQuote.amount = ticker.dayAmount
        minQuote.execCount += 1

        execution = new Execution
        execution.sec = sec
        execution.time = ticker.time
        execution.price  = ticker.lastPrice
        execution.volume = ticker.dayVolume
        execution.amount = ticker.dayAmount
        
        // re-init lastTime
        lastTime = ticker.time
      } else {
                
        /**
         *    ticker.time    prevTicker.time
         *          |------------------|------------------->
         *          |<----- 1000 ----->|
         */
        if (ticker.time + 1000 > lastTicker.time) { // 1000ms, @Note: we may add +1 to ticker.time later
          // some datasources only count on second, but we may truly have a new ticker
          if (ticker.time <= lastTicker.time) {
            ticker.time = lastTicker.time + 1 // avoid duplicate key
          }

          tickerValid = true

          if (ticker.dayVolume > lastTicker.dayVolume) {
            execution = new Execution
            execution.sec = sec
            execution.time = ticker.time
            execution.price = ticker.lastPrice
            execution.volume = ticker.dayVolume - lastTicker.dayVolume
            execution.amount = ticker.dayAmount - lastTicker.dayAmount
          } else {
            log.fine("dayVolome curr: " + ticker.dayVolume + ", last: " + lastTicker.dayVolume)
          }

          if (minQuote.justOpen_?) {
            minQuote.unjustOpen_!
            
            // init minQuote values:
            minQuote.open = ticker.lastPrice
            minQuote.high = ticker.lastPrice
            minQuote.low  = ticker.lastPrice
            minQuote.volume = 0
            minQuote.amount = 0
            minQuote.execCount = 0
          }

          minQuote.execCount += 1

          if (lastTicker.dayHigh > 0 && ticker.dayHigh > 0) {
            if (ticker.dayHigh > lastTicker.dayHigh) {
              // this is a new day high happened during prevTicker to this ticker
              minQuote.high = ticker.dayHigh
            }
          }
          if (ticker.lastPrice > 0) {
            minQuote.high = math.max(minQuote.high, ticker.lastPrice)
          }

          if (lastTicker.dayLow > 0 && ticker.dayLow > 0) {
            if (ticker.dayLow < lastTicker.dayLow) {
              // this is a new day low happened during prevTicker to this ticker
              minQuote.low = ticker.dayLow
            }
          }
          if (ticker.lastPrice > 0) {
            minQuote.low = math.min(minQuote.low, ticker.lastPrice)
          }
          
          minQuote.close = ticker.lastPrice
          if (execution != null && execution.volume > 0) {
            minQuote.volume += execution.volume
            minQuote.amount += execution.amount
          }
          else if (minMoneyFlow.isDataOnlyInited){
            allUpdatedMinuteMoneyFlows += minMoneyFlow
            minMoneyFlow.lastModify = ticker.time
              
            allUpdatedDailyMoneyFlows += dayMoneyFlow
            dayMoneyFlow.lastModify = ticker.time
          }

        } else {
          log.warning("Discard ticker: " + ticker.uniSymbol + " -> time=" + ticker.time + ", but lastTicker.time=" + lastTicker.time)
        }
      }


      if (tickerValid) {
        sec.publish(api.TickerEvt(ticker))
        allTickers += ticker

        if (execution != null) {
          val prevPrice = if (isDayFirst) ticker.prevClose else lastTicker.lastPrice
          val prevDepth = if (isDayFirst) MarketDepth.Empty else MarketDepth(lastTicker.bidAsks, copy = true)
          execution.setDirection(prevPrice, prevDepth)

          sec.publish(api.ExecutionEvt(ticker.prevClose, execution))
          allExecutions += execution
        }

        // update daily quote and ser
        dayQuote.updateDailyQuoteByTicker(ticker)
//        minQuote.lastModify = ticker.time

        // updated quote ser
        sec.updateQuoteSer(TFreq.DAILY, dayQuote)
        sec.updateQuoteSer(TFreq.ONE_MIN, minQuote)
        
        allUpdatedDailyQuotes += dayQuote
        allUpdatedMinuteQuotes += minQuote
        
        exchangeToLastTime.put(sec.exchange, ticker.time)

        lastTicker.copyFrom(ticker)
        lastTime = math.max(lastTime, ticker.time)
      }
    }
    
    /* else {

     /**
      * no new ticker got, but should consider if it's necessary to to update quoteSer
      * as the quote window may be just opened.
      */
     sec.lastData.prevTicker match {
     case null =>
     case ticker =>
     if (ticker != null && ticker.dayHigh != 0 && ticker.dayLow != 0) {
     val dayQuote = sec.dailyQuoteOf(ticker.time)
     updateDailyQuote(dayQuote, ticker)
     // update chainSers
     sec.serOf(TFreq.DAILY) match {
     case Some(x) if x.loaded => x.updateFrom(dayQuote)
     case _ =>
     }
     sec.serOf(TFreq.ONE_MIN) match {
     case Some(x) if x.loaded => x.updateFrom(minQuote)
     case _ =>
     }
     }
     }
     } */

    // broadcast events via TickerServer, don't wait until data saved to db. 

    // the interesting listeners can use these evts:
    // 1. to forward to remote message system;
    // 2. to compute money flow etc.
    if (allTickers.length > 0) {
      TickerServer.publish(api.TickersEvt(allTickers.toArray))
    }
    if (allExecutions.length > 0) {
      TickerServer.publish(api.ExecutionsEvt(allExecutions.toArray))
    }
    if (allUpdatedDailyQuotes.length > 0) {
      TickerServer.publish(api.QuotesEvt(TFreq.DAILY.shortName, allUpdatedDailyQuotes.toArray))
    }
    if (allUpdatedMinuteQuotes.length > 0) {
      TickerServer.publish(api.QuotesEvt(TFreq.ONE_MIN.shortName, allUpdatedMinuteQuotes.toArray))
    }

    if (allUpdatedMinuteMoneyFlows.length > 0) {
      TickerServer.publish(("null", api.MoneyFlowsEvt(TFreq.ONE_MIN.shortName, allUpdatedMinuteMoneyFlows.toArray)))
      TickerServer.publish(("null", api.MoneyFlowsEvt(TFreq.DAILY.shortName, allUpdatedDailyMoneyFlows.toArray)))
    }

    // batch save to db

    val (tickersLastToInsert, tickersLastToUpdate) = tickersLast.partition(_.isTransient)
    log.info("Going to save to db ...")
    try {
      var willCommit = false
      val t0 = System.currentTimeMillis
      if (tickersLastToInsert.length > 0) {
        TickersLast.insertBatch_!(tickersLastToInsert.toArray)
        willCommit = true
      }
      if (tickersLastToUpdate.length > 0) {
        TickersLast.updateBatch_!(tickersLastToUpdate.toArray)
        willCommit = true
      }
      if (willCommit) {
        log.info("Saved tickersLast in " + (System.currentTimeMillis - t0) + "ms: tickersLastToInsert=" + tickersLastToInsert.length + ", tickersLastToUpdate=" + tickersLastToUpdate.length)
      }

      if (TickerServer.isServer && TickerServer.isSaveTickers) {
        val t1 = System.currentTimeMillis
        if (allTickers.length > 0) {
          Tickers.insertBatch_!(allTickers.toArray)
          willCommit = true
        }
        if (allExecutions.length > 0) {
          Executions.insertBatch_!(allExecutions.toArray)
          willCommit = true
        }
        if (willCommit) {
          log.info("Saved Tickers/Executions in " + (System.currentTimeMillis - t1) + "ms: tickers=" + allTickers.length + ", executions=" + allExecutions.length)
        }
      }

      // @Note if there is no update/insert on db, do not call commit, which may cause deadlock
      if (willCommit) {
        COMMIT
        log.info("Committed")
      }
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }

    // Update exchange status and try to close and save updated quotes, moneyflows etc
    for ((exchange, lastTime) <- exchangeToLastTime) {
      val status = exchange.statusOf(lastTime)
      exchange.status = status
      log.info("Trading status of " + exchange.code + ": " + status.toString(exchange))
      
      val alsoSave = TickerServer.isServer
      exchange.tryClosing(status, alsoSave)
    }

    lastTime
  }
  
  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}

/**
 * To avoid singleton publisher's poor performance, we use more actors.
 * To listen to it, listenTo(publishers: _*)
 * To deaf to it, deafTo(publishers: _*)
 */
object TickerServer {
  private val log = Logger.getLogger(this.getClass.getName)

  private val config = org.aiotrade.lib.util.config.Config()
  val isServer = !config.getBool("dataserver.client", false)
  val isSaveTickers = config.getBool("dataserver.savetickers", false)
  log.info("Ticker server is started as " + (if (TickerServer.isServer) "server" else "client"))
  
  
  val publishers = {
    var xs: List[Publisher] = Nil
    val numPublishers = config.getInt("dataserver.numpublisher", 10)
    var i = -1
    while ({i += 1; i < numPublishers}) xs ::= new Publisher {}
    xs
  }
  
  private var publisherRevolver: List[Publisher] = Nil
  private def nextPublisher = publisherRevolver match {
    case Nil => 
      publisherRevolver = publishers.tail
      publishers.head
    case head :: tail =>
      publisherRevolver = tail
      head
  }
  
  def publish(e: Any) {
    nextPublisher.publish(e)
  }
}

