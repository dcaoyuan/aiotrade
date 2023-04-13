package org.aiotrade.lib


import java.util.logging.Logger
import java.util.concurrent.TimeUnit
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.ComputeFrom
import org.aiotrade.lib.math.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.indicator.SpotIndicator
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.RichInfoContract
import org.aiotrade.lib.securities.dataserver.RichInfoHisContract
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.NewsContract
import org.aiotrade.lib.securities.dataserver.FilingContract
import org.aiotrade.lib.securities.dataserver.AnalysisReportContract
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.Sectors
import org.aiotrade.lib.securities.model.SectorSecs
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m

import ru.circumflex.orm._

/**
 * 
 * @author Caoyuan Deng
 */
package object securities {
  /**
   * @Note private[this] to avoid this private log to be static imported by others, it seems a scala's compiler bug? 
   */
  private[this] val log = Logger.getLogger(this.getClass.getName)
  private[this] val config = org.aiotrade.lib.util.config.Config()
  private[this] val isServer = !config.getBool("dataserver.client", false)

  def getSecsOfSector(category: String, code: String) = {
    val sectors = if (isServer) {
      SELECT (Sectors.*) FROM (Sectors) list()
    } else {
      SELECT (Sectors.*) FROM (AVRO(Sectors)) list()
    }
    sectors foreach {x => log.info("%s, category=%s, code=%s, id=%s".format(x.name, x.category, x.code, Sectors.idOf(x)))}
    val sector = sectors.find(x => x.category == category && x.code == code).get
    val sectorId = Sectors.idOf(sector)
    val secsHolder = Exchange.uniSymbolToSec
    val secs = if (isServer) {
      SELECT (SectorSecs.*) FROM (SectorSecs) list() filter {x => 
        (x.sector eq sector) && (x.validTo == 0)
      } map {x => x.sec}
    } else {
      SELECT (SectorSecs.*) FROM (AVRO(SectorSecs)) list() filter {x => 
        (x.sector eq sector) && (x.validTo == 0)
      } map {x => x.sec}
    }
    secs.toSet.toArray // toSet to remove duplicate sec
  }

  /**
   * Load all sers of secs from persistence and return referSer
   * 
   * @return referSer
   */
  def loadSers(secs: Array[Sec], referSec: Sec, freq: TFreq): QuoteSer = {
    val referSer = loadSer(referSec, freq, false)

    val t0 = System.currentTimeMillis
    var i = 0
    while (i < secs.length) {
      val sec = secs(i)
      loadSer(sec, freq, true)
      i += 1
      log.info("Loaded %s, %s of %s.".format(sec.uniSymbol, i, secs.length))
    }
    log.info("Loaded sers in %s s".format((System.currentTimeMillis - t0) / 1000))
    
    referSer
  }
  
  /**
   * Load ser of sec from persistence and return ser
   * 
   * @return referSer
   */
  def loadSer(sec: Sec, freq: TFreq, doAdjust: Boolean = true): QuoteSer = {
    val ser = sec.serOf(freq).get
    sec.loadSerFromPersistence(ser, false)
    ser.isLoaded = true // should be called to let adjust go and release a reaction later.
    if (doAdjust) {
      ser.adjust(true)
    }
    ser
  }
  
  def getReferTimesViaDB(toTime: Long, referTimePeriod: Int, freq: TFreq, referSec: Sec): Array[Long] = {
    val quotes = getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            (q.sec.field EQ Secs.idOf(referSec)) AND (q.time LE toTime)
          ) ORDER_BY (q.time DESC) list()
        ) toArray
    
      case _ => Array[Quote]()
    }
    
    val referTimes = new ArrayList[Long]()
    var i = 0
    var n = scala.math.min(quotes.length, referTimePeriod)
    while (i < n) {
      referTimes += quotes(n - i - 1).time
      i += 1
    }
    referTimes.toArray
  }

  def getQuotes(sec: Sec, freq: TFreq, limit: Int): Array[Quote] = {
    getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            q.sec.field EQ Secs.idOf(sec)
          ) ORDER_BY (q.time DESC) LIMIT(limit) list() reverse
        ) toArray
    
      case _ => Array()
    }
  }
  
  def getQuotes(sec: Sec, freq: TFreq, fromTime: Long, toTime: Long): Array[Quote] = {
    getQuoteTable(freq) match {
      case Some(q) =>
        (SELECT (q.*) FROM (q) WHERE (
            (q.sec.field EQ Secs.idOf(sec)) AND (q.time LE toTime) AND (q.time GE fromTime)
          ) ORDER_BY (q.time DESC) list() reverse
        ) toArray
    
      case _ => Array()
    }
  }
  
  def doAdjusting(sec: Sec, quotes: Array[Quote]) {
    val divs = Exchanges.dividendsOf(sec)
    if (divs.isEmpty) {
      return
    }
    
    var i = 0
    while (i < quotes.length) {
      val quote = quotes(i)
      val time = quote.time

      var h = quote.high
      var l = quote.low
      var o = quote.open
      var c = quote.close

      val divItr = divs.iterator
      while (divItr.hasNext) {
        val div = divItr.next
        if (time < div.dividendDate) {
          h = div.adjust(h)
          l = div.adjust(l)
          o = div.adjust(o)
          c = div.adjust(c)
        }
      }
      
      quote.high  = h
      quote.low   = l
      quote.open  = o
      quote.close = c
      
      i += 1
    }
  }

  def getQuoteTable(freq: TFreq) = freq match {
    case TFreq.DAILY => Some(Quotes1d)
    case TFreq.ONE_MIN => Some(Quotes1m)
    case _ => None
  }  
  
  
  def createQuoteContract(symbol: String, category: String , sname: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): QuoteContract = {
    val dataContract = new QuoteContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5

    dataContract
  }

  def createTickerContract(symbol: String, category: String, sname: String, freq: TFreq, serverClassName: String): TickerContract = {
    val dataContract = new TickerContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = true
    dataContract.refreshInterval = 5

    dataContract
  }

  def createNewsContract(symbol: String, category: String , sname: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): NewsContract = {
    val dataContract = new NewsContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5000 //ms

    dataContract
  }

  def createFilingContract(symbol: String, category: String , sname: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): FilingContract = {
    val dataContract = new FilingContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5000 //ms

    dataContract
  }

  def createAnalysisReportContract(symbol: String, category: String , sname: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): AnalysisReportContract = {
    val dataContract = new AnalysisReportContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5000 //ms

    dataContract
  }

  def createRichInfoContract(symbol: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): RichInfoContract = {
    val dataContract = new RichInfoContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5

    dataContract
  }

  def createRichInfoHisContract(symbol: String, freq: TFreq, isRefreshable: Boolean, serverClassName: String): RichInfoHisContract = {
    val dataContract = new RichInfoHisContract

    dataContract.active = true
    dataContract.serviceClassName = serverClassName

    dataContract.srcSymbol = symbol

    dataContract.freq = freq
    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5

    dataContract
  }

  def createIndicatorDescriptor[T <: Indicator](clazz: Class[T], freq: TFreq): IndicatorDescriptor = {
    val descriptor = new IndicatorDescriptor
    descriptor.active = true
    descriptor.serviceClassName = clazz.getName
    descriptor.freq = freq
    descriptor
  }


  @Deprecated
  def loadSer_deprecated(sec: Sec, freq: TFreq) {
    var mayNeedsReload = false
    if (sec == null) {
      return
    } else {
      mayNeedsReload = true
    }

    if (mayNeedsReload) {
      sec.resetSers
    }
    val ser = sec.serOf(freq).get

    if (!ser.isLoaded && !ser.isInLoading) {
      sec.loadSer(ser)
    }
  }

  def initIndicators(content: Content, baseSer: BaseTSer): Seq[_ <: Indicator] = {
    var indicators: List[Indicator] = Nil
    for (descriptor <- content.lookupDescriptors(classOf[IndicatorDescriptor])
         if descriptor.active && descriptor.freq.equals(baseSer.freq)
    ) yield {
      descriptor.serviceInstance(baseSer) match {
        case Some(indicator) => indicators ::= indicator
        case _ => log.warning("In test: can not init instance of: " + descriptor.serviceClassName)
      }
    }
    indicators
  }

  def computeSync(indicator: Indicator) {
    indicator match {
      case _: SpotIndicator => // don't compute it right now
      case _ =>
        val t0 = System.currentTimeMillis
        indicator.computeFrom(0)
        log.info("Computed " + indicator.shortName + "(" + indicator.freq + ", size=" + indicator.size +  ") in " + (System.currentTimeMillis - t0) + " ms")
    }
  }

  def computeAsync(indicator: Indicator) {
    indicator match {
      case _: SpotIndicator => // don't compute it right now
      case _ =>
        log.info("Computing " + indicator.shortName + "(" + indicator.freq + ", size=" + indicator.size +  ")")
        indicator ! ComputeFrom(0)
    }
  }

  def printValuesOf(indicator: Indicator): Unit = {
    println
    println(indicator.freq)
    println(indicator.shortName + ":" + indicator.size)
    for (v <- indicator.vars) {
      print(v.name + ": ")
      v.values.reverse foreach {x => print(x + ",")}
      println
    }
  }

  def printLastValueOf(indicator: Indicator) {
    println
    println(indicator.freq + "-" +indicator.shortName + ":" + indicator.size)
    for (v <- indicator.vars if v.size > 0) {
      println(v.name + ": " + v.values.last)
    }
  }

  def reportQuote(sec: Sec) {
    println("\n======= " + new java.util.Date + " size of " + sec.uniSymbol  + " ======")
    sec.serOf(TFreq.DAILY)   foreach {x => println("daily:  "  + x.size)}
    sec.serOf(TFreq.ONE_MIN) foreach {x => println("1 min:  "  + x.size)}
    sec.serOf(TFreq.WEEKLY)  foreach {x => println("weekly: "  + x.size)}
  }

  def reportRichInfo(sec: Sec) {
    //println("\n======= " + new java.util.Date + " size of " + sec.uniSymbol  + " ======")
    sec.infoPointSerOf(TFreq.DAILY) match {
      case Some(ser) => println(sec.secInfo.uniSymbol + " daily:  "  + ser.size)
      case None =>
    }

    sec.infoPointSerOf(TFreq.ONE_MIN) match {
      case Some(ser) => println(sec.secInfo.uniSymbol + " 1 Min:  "  + ser.size)
      case None =>
    }


  }

  def reportInds(inds: Seq[_ <: Indicator]) {
    inds foreach {x: Indicator => println(x.toString)}
  }


  // wait for some ms
  def waitFor(ms: Long) {
    TimeUnit.MILLISECONDS.sleep(ms)
  }
}
