/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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

package org.aiotrade.lib.securities.model


import java.util.Calendar
import ru.circumflex.orm._
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.util
import scala.collection.mutable



/**
 * Quote value object
 *
 * @author Caoyuan Deng
 */
@serializable
final class Quote extends BelongsToSec with TVal with Flag {

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  private var _lastModify: Long = _
  def lastModify = _lastModify
  def lastModify_= (time: Long) {
    this._lastModify = time
  }

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag 
  def flag_=(flag: Int) {
    this._flag = flag
  }

  @transient var sourceId = 0L

  var hasGaps = false
  
  private val data = new Array[Double](10) // will increase to 11 to add averagy
  
  def open      = data(0)
  def high      = data(1)
  def low       = data(2)
  def close     = data(3)
  def volume    = data(4)
  def amount    = data(5)
  def vwap      = data(6)
  def prevClose = data(7)
  def execCount = data(8)
  def turnoverRate = data(9)

  def open_=   (v: Double) {data(0) = v}
  def high_=   (v: Double) {data(1) = v}
  def low_=    (v: Double) {data(2) = v}
  def close_=  (v: Double) {data(3) = v}
  def volume_= (v: Double) {data(4) = v}
  def amount_= (v: Double) {data(5) = v}
  def vwap_=   (v: Double) {data(6) = v}
  def prevClose_= (v: Double) {data(7) = v}
  def execCount_=(v: Double) {data(8) = v}
  def turnoverRate_=(v: Double) {data(9) = v}

  // Foreign keys
  @transient var tickers: List[Ticker] = Nil
  @transient var executions: List[Execution] = Nil

  
  // --- no db fields:
  var isTransient: Boolean = true

  /**
   * @Note With var _average, we can make sure that after doAdjust in QuoteSer,
   * the quote that returns by valueOf(time) could be set a proper adjusted value 
   * to _average. We cannot simplely use amount / volume as the adjusted average,
   * since amount/volume won't be changed during adjusting.
   * @see org.aiotrade.lib.securities.QuoteSer#valueOf
   */
  @transient private var _average: Double = Double.NaN
  /** 
   * average price 
   */
  def average: Double = {
    if (_average.isNaN) { // has not been set by outside (for example, by adjusted QuoteSer)
      if (amount != 0 && volume != 0) {
        amount / volume match {
          case x if x >= low && x <= high => x // beware that index's amount/volume may not be average
          case _ => (open + high + low + close) / 4
        }
      } else {
        (open + high + low + close) / 4
      }
    } else { // has been set by outside, just use it
      _average
    }
  }
  def average_=(v: Double) {
    _average = v
  }
  
  def copyFrom(another: Quote) {
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  def reset {
    time = 0
    sourceId = 0
    var i = -1
    while ({i += 1; i < data.length}) {
      data(i) = 0
    }
    hasGaps = false
  }
  
  /**
   * This quote must be a daily quote
   */
  def updateDailyQuoteByTicker(ticker: Ticker) {
    lastModify = ticker.time
    open   = ticker.dayOpen
    high   = ticker.dayHigh
    low    = ticker.dayLow
    close  = ticker.lastPrice
    volume = ticker.dayVolume
    amount = ticker.dayAmount
    prevClose = ticker.prevClose
    execCount += 1
  }

  def toShape: Quote.Shape = {
    val body = math.abs(close - open)
    val upperShadow = high - math.max(close, open)
    val lowerShadow = math.min(close, open) - low
    val isWhite = close >= open
    
    if (body == 0 && upperShadow != 0 && lowerShadow != 0) {
      Quote.Doji
    } else {
      if (upperShadow == 0 && lowerShadow == 0) {
        if (isWhite) Quote.MarubozuWhite else Quote.MarubozuBlack
      } else if (upperShadow == 0 && lowerShadow != 0) {
        if (isWhite) Quote.ClosingMarubozuWhite else Quote.OpeningMarubozuBlack
      } else if (upperShadow != 0 && lowerShadow == 0) {
        if (isWhite) Quote.OpeningMarubozuWhite else Quote.ClosingMarubozuBlack
      } else {
        val bodyProportion = body / (body + upperShadow + lowerShadow)
        if (bodyProportion > 0.618) {
          if (body / open > 0.0618) {
            if (isWhite) Quote.LongDayWhite else Quote.LongDayBlack
          } else {
            if (isWhite) Quote.ShortDayWhite else Quote.ShortDayBlack
          }
        } else {
          if (isWhite) Quote.SpinningTopWhite else Quote.SpinningTopBlack
        }
      }
    }
  }  
  
  override 
  def toString = {
    val sb = new StringBuilder()
    sb.append("Quote(").append(uniSymbol).append(",").append(util.formatTime(time))
    sb.append(",O:").append(open)
    sb.append(",H:").append(high)
    sb.append(",L:").append(low)
    sb.append(",C:").append(close)
    sb.append(",V:").append(volume)
    sb.append(",A:").append(amount)
    sb.append(",R:").append(average)
    sb.append(",E:").append(closed_?)
    sb.append(",PC:").append(prevClose)
    sb.append(",EC:").append(execCount)
    sb.append(",T:").append(turnoverRate)
    sb.append(")").toString
  }

}

// --- table
abstract class Quotes extends Table[Quote] with TableEx {
  private val log = Logger.getLogger(this.getClass.getName)
  
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val open   = "open"   DOUBLE()
  val high   = "high"   DOUBLE()
  val low    = "low"    DOUBLE()
  val close  = "close"  DOUBLE()
  val volume = "volume" DOUBLE()
  val amount = "amount" DOUBLE()
  val vwap   = "vwap"   DOUBLE()
  val prevClose = "prevClose" DOUBLE()
  val execCount = "execCount" DOUBLE()

  val flag = "flag" INTEGER()

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  def quotesOf(sec: Sec): Seq[Quote] = {
    try {
      val list1 = SELECT (this.*) FROM (this) WHERE (
        this.sec.field EQ Secs.idOf(sec)
      ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list;
      list1 foreach{ x => x.lastModify = x.time}
      list1
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def closedQuotesOf(sec: Sec): Seq[Quote] = {
    val xs = new ArrayList[Quote]()
    for (x <- quotesOf(sec) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedQuotesOf_filterByDB(sec: Sec): Seq[Quote] = {
    try {
      val list1 = SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
      ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list;
      list1 foreach{ x => x.lastModify = x.time}
      list1
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def saveBatch(sec: Sec, sortedQuotes: Seq[Quote]) {
    if (sortedQuotes.isEmpty) return

    val head = sortedQuotes.head
    val last = sortedQuotes.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, Quote]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedQuotes.partition(x => exists.contains(x.time))
    try {
      for (x <- updates) {
        val existOne = exists(x.time)
        existOne.copyFrom(x)
        this.update_!(existOne)
      }

      this.insertBatch_!(inserts.toArray)
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
  
  def saveBatch(atSameTime: Long, quotes: Array[Quote]) {
    if (quotes.isEmpty) return

    val exists = mutable.Map[Sec, Quote]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sec.field GT 0) AND (this.sec.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.sec, x)}

    val updates = new ArrayList[Quote]()
    val inserts = new ArrayList[Quote]()
    var i = -1
    while ({i += 1; i < quotes.length}) {
      val quote = quotes(i)
      exists.get(quote.sec) match {
        case Some(existOne) => 
          existOne.copyFrom(quote)
          updates += existOne
        case None =>
          inserts += quote
      }
    }
    
    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
}

object Quotes1d extends Quotes {
  private val log = Logger.getLogger(this.getClass.getSimpleName)

  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def lastDailyQuoteOf(sec: Sec): Option[Quote] = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (this.sec.field EQ Secs.idOf(sec)) ORDER_BY (this.time DESC) LIMIT (1) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }

    res foreach{ x => x.lastModify = x.time}
    res.headOption
  }

  @deprecated
  def dailyQuoteOf(sec: Sec, dailyRoundedTime: Long): Quote = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = mutable.Map[Sec, Quote]()
        dailyCache.put(dailyRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ dailyRoundedTime)
          ) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x => x.lastModify = x.time; map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Quote
        newone.time = dailyRoundedTime
        newone.lastModify = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        log.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }

  @deprecated
  def dailyQuoteOf_nonCached(sec: Sec, dailyRoundedTime: Long): Quote = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ dailyRoundedTime)
      ) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res.headOption match {
      case Some(one) =>
        one.isTransient = false
        one.lastModify  = one.time
        one
      case None =>
        val newone = new Quote
        newone.time = dailyRoundedTime
        newone.lastModify = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        log.fine("Start a new daily quote of sec(id=" + Secs.idOf(sec) + "), time=" + dailyRoundedTime)
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        newone
    }
  }


  def dailyQuotesOf(exchange: Exchange, time: Long): Seq[Quote] = {
    val cal = Calendar.getInstance(exchange.timeZone)
    val rounded = TFreq.DAILY.round(time, cal)

    try {
      val list1 = SELECT (Quotes1d.*) FROM (Quotes1d JOIN Secs) WHERE (
        (this.time EQ rounded) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
      ) list;
      list1 foreach{ x => x.lastModify = x.time}
      list1
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def lastDailyQuotesOf(exchange: Exchange): Seq[Quote] = {
    try {
      val list1 = SELECT (Quotes1d.*) FROM Quotes1d WHERE (
        Quotes1d.time EQ (
          SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
        )
      ) list;
      list1 foreach{ x => x.lastModify = x.time}
      list1
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

}

object Quotes1m extends Quotes {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)

  private val ONE_DAY = 24 * 60 * 60 * 1000

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, Quote]]()

  def mintueQuotesOf(sec: Sec, dailyRoundedTime: Long): Seq[Quote] = {    
    try {
      val list1 = SELECT (this.*) FROM (this) WHERE (
        this.sec.field EQ Secs.idOf(sec) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (this.time DESC)  list;
      list1 foreach{ x => x.lastModify = x.time}
      list1
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  @deprecated
  def minuteQuoteOf(sec: Sec, minuteRoundedTime: Long): Quote = {
    if (isServer) minuteQuoteOf_nonCached(sec, minuteRoundedTime) else minuteQuoteOf_cached(sec, minuteRoundedTime)
  }
  
  /**
   * @Note do not use it when table is partitioned on secs_id (for example, quotes1m on server side), since this qeury is only on time
   */
  @deprecated
  def minuteQuoteOf_cached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = mutable.Map[Sec, Quote]()
        minuteCache.put(minuteRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ minuteRoundedTime)
          ) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x => x.lastModify = x.time; map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Quote
        newone.time = minuteRoundedTime
        newone.lastModify = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.ONE_MIN, newone)
        newone
    }
  }

  @deprecated
  def minuteQuoteOf_nonCached(sec: Sec, minuteRoundedTime: Long): Quote = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res match {
      case Seq(one) =>
        one.isTransient = false
        one.lastModify = one.time
        one
      case Seq() =>
        val newone = new Quote
        newone.time = minuteRoundedTime
        newone.lastModify = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.ONE_MIN, newone)
        newone
    }
  }
}

object Quote {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(vmap: collection.Map[String, Array[_]]): Array[Quote] = {
    if (vmap.isEmpty) return Array()
    
    val quotes = new ArrayList[Quote]()
    try {
      val times   = vmap(".")
      val opens   = vmap("O")
      val highs   = vmap("H")
      val lows    = vmap("L")
      val closes  = vmap("C")
      val volumes = vmap("V")
      val amounts = vmap("A")
    
      var i = -1
      while ({i += 1; i < times.length}) {
        val quote = new Quote

        // the time should be properly set to 00:00 of exchange location's local time, i.e. rounded to TFreq.DAILY
        quote.time   = times(i).asInstanceOf[Long]
        quote.open   = opens(i).asInstanceOf[Double]
        quote.high   = highs(i).asInstanceOf[Double]
        quote.low    = lows(i).asInstanceOf[Double]
        quote.close  = closes(i).asInstanceOf[Double]
        quote.volume = volumes(i).asInstanceOf[Double]
        quote.amount = amounts(i).asInstanceOf[Double]

        if (quote.high * quote.low * quote.close == 0) {
          // bad quote, do nothing
        } else {
          quotes += quote
        }
      }
    } catch {
      case ex: Throwable => log.warning(ex.getMessage)
    }

    quotes.toArray
  }
  
  /**
   * http://candlestickforum.com/PPF/Parameters/16_19_/candlestick.asp
   * http://stockcharts.com/school/doku.php?id=chart_school:chart_analysis:introduction_to_cand
   */
  trait Shape
  
  /**
   * A long day represents a large price move from open to close. Long represents 
   * the length of the candle body. What qualifies a candle body to be considered 
   * long? That is a question that has to be answered relative to the chart being 
   * analyzed. The recent price action of a stock will determine whether a "long" 
   * candle has been formed. Analysis of the previous two or three weeks of trading 
   * should be a current representative sample of the price action.
   * 
   *  |    |
   * +-+  +-+
   * | |  |#|
   * | |  |#|
   * | |  |#|
   * +-+  +-+
   *  |    |
   */
  case object LongDayWhite extends Shape
  case object LongDayBlack extends Shape
  
  /**
   * Short days can be interpreted by the same analytical process of the long candles. 
   * There are a large percentage of the trading days that do not fall into either
   * of these two catagories.
   * 
   *  |    |
   * +-+  +-+
   * | |  |/|
   * +-+  +-+
   *  |    |
   */
  case object ShortDayWhite extends Shape
  case object ShortDayBlack extends Shape
  
  /**
   * In Japanese, Marubozu means close cropped or close-cut. Bald or Shaven Head 
   * are more commonly used in candlestick analysis. It's meaning reflects the 
   * fact that there are no shadows extending from either end of the body.
   * 
   * The White Marubozu is a long white body with no shadows on either end. This 
   * is an extremely strong pattern. Consider how it is formed. It opens on the 
   * low and immediately heads up. It continues upward until it closes, on its high.
   * Counter to the Black Marubozu, it is often the first part of a bullish continuation 
   * pattern or bearish reversal pattern. It is called a Major Yang or Marubozu of Yang.
   * 
   * A long black body with no shadows at either end is known as a Black Marubozu. 
   * It is considered a weak indicator. It is often identified in a bearish continuation 
   * or bullish reversal pattern, especially if it occurs during a downtrend. A long
   * black candle could represent the final sell off, making it an "alert" to a 
   * bullish reversal setting up. The Japanese often call it the Major Yin or Marubozu of Yin.
   * 
   * +-+  +-+
   * | |  |/|
   * | |  |/|
   * +-+  +-+
   */
  case object MarubozuWhite extends Shape
  case object MarubozuBlack extends Shape
  
  /**
   * A Closing Marubozu has no shadow at it's closing end. A white body will not 
   * have a shadow at the top. A black body will not have a shadow at the bottom. 
   * In both cases, these are strong signals corresponding to the direction that 
   * they each represent.
   * 
   *       |
   * +-+  +-+
   * | |  |/|
   * | |  |/|
   * +-+  +-+
   *  | 
   */
  case object ClosingMarubozuWhite extends Shape
  case object ClosingMarubozuBlack extends Shape
  
  /**
   * The Opening Marubozu has no shadows extending from the open price end of the body. 
   * A white body would not have a shadow at the bottom end , the black candle would 
   * not have a shadow at it's top end. Though these are strong signals, they are 
   * not as strong as the Closing Marubozu.
   * 
   *  |
   * +-+  +-+
   * | |  |/|
   * | |  |/|
   * +-+  +-+
   *       |
   */
  case object OpeningMarubozuWhite extends Shape
  case object OpeningMarubozuBlack extends Shape
  
  /**
   * Spinning Tops are depicted with small bodies relative to the shadows. This 
   * demonstrates some indecision on the part of the bulls and the bears. They are 
   * considered neutral when trading in a sideways market. However, in a trending 
   * or oscillating market, a relatively good rule of thumb is that the next days 
   * trading will probably move in the direction of the opening price. The size of 
   * the shadow is not as important as  the size of the body for forming a Spinning Top.
   *  |    |
   *  |    |
   * +-+  +/+
   * +-+  +/+
   *  |    |
   *  |    |
   */
  case object SpinningTopWhite extends Shape
  case object SpinningTopBlack extends Shape
  
  /**
   * The Doji is one of the most important signals in candlestick analysis. It is 
   * formed when the open and the close are the same or very near the same. The 
   * lengths of the shadows can vary. The longer the shadows are, the more significance 
   * the Doji becomes. More will be explained about the Doji in the next few pages. 
   * ALWAYS pay attention to the Doji.
   *   |
   *  -+-
   *   |
   */
  case object Doji extends Shape
  
}
