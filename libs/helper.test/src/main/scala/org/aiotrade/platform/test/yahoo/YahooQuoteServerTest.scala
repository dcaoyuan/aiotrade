package org.aiotrade.platform.test.yahoo

import org.aiotrade.lib.math.timeseries._
import org.aiotrade.lib.math.timeseries.descriptor._
import org.aiotrade.lib.securities._
import org.aiotrade.lib.securities.model._
import java.util.Timer
import java.util.TimerTask
import org.aiotrade.lib.dataserver.yahoo._
import org.aiotrade.platform.test.TestHelper
import scala.collection.mutable.ArrayBuffer

object YahooQuoteServerTest extends TestHelper {

  def main(args: Array[String]) {
    testBatch
  }

  def testBatch {
    val size = 5
    val syms = Exchange.symbolsOf(Exchange.SS)
    val testers = new ArrayBuffer[TestOne]

    var i = 0
    val itr = syms.iterator
    while (i < size && itr.hasNext) {
      val sym = itr.next
      val tester = new TestOne(sym)
      testers += tester
      i += 1
    }

    val timer = new Timer
    timer.schedule(new TimerTask {
        def run {
          testers foreach {x =>
            reportQuote(x.sec)
            reportInds(x.oneMinInds)
            reportInds(x.dailyInds)
            reportInds(x.weeklyInds)
          }
        }
      }, 5000, 6000)
  }

  class TestOne(symbol:String) {

    val quoteServer  = YahooQuoteServer.getClass.getName
    val tickerServer = YahooTickerServer.getClass.getName

    val sec = Exchange.secOf(symbol).get
    val exchange = YahooQuoteServer.exchangeOf(symbol)
    sec.exchange = exchange

    val content = sec.content

    val dailyQuoteContract = createQuoteContract(symbol, "", "", TFreq.DAILY, false, quoteServer)

    val supportOneMin = dailyQuoteContract.isFreqSupported(TFreq.ONE_MIN)

    val oneMinQuoteContract = createQuoteContract(symbol, "", "", TFreq.ONE_MIN, false, quoteServer)
    val tickerContract = createTickerContract(symbol, "", "", TFreq.ONE_MIN, tickerServer)

    content.addDescriptor(dailyQuoteContract)
    content.addDescriptor(oneMinQuoteContract)
    sec.tickerContract = tickerContract


    createAndAddIndicatorDescritors(content, TFreq.DAILY)
    createAndAddIndicatorDescritors(content, TFreq.ONE_MIN)
    createAndAddIndicatorDescritors(content, TFreq.WEEKLY)
    //weeklyContent.addDescriptor(dailyQuoteContract)

    val daySer  = sec.serOf(TFreq.DAILY).get
    val minSer = sec.serOf(TFreq.ONE_MIN).get

    // * init indicators before loadSer, so, they can receive the Loaded evt
    val dailyInds  = initIndicators(content, daySer)
    val oneMinInds = initIndicators(content, minSer)

    loadSer(sec, TFreq.DAILY)
    //loadSer(sec, TFreq.ONE_MIN)

    val weeklySer = sec.serOf(TFreq.WEEKLY).get
    val weeklyInds = initIndicators(content, weeklySer)
    
    // wait for some secs for data loading
    //waitFor(10000)

    // * Here, we test two possible condiction:
    // * 1. inds may have been computed by FinishedLoading evt,
    // * 2. data loading may not finish yet
    // * For what ever condiction, we force to compute it again to test concurrent
    dailyInds  foreach {x => computeAsync(x)}
    oneMinInds foreach {x => computeAsync(x)}
    weeklyInds foreach {x => computeAsync(x)}

    sec.subscribeTickerServer()
  }

}
