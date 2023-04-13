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
package org.aiotrade.lib.securities.model

import java.util.Calendar
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable


/**
 *
 * A stock ticker is a running report of the prices and trading volume of securities
 * traded on the various stock exchanges. A "tick" is an up or down movement in the
 * sale price of a security. Since the days of the paper ticker tape dating back to
 * 1867, stock tickers have evolved with the times, becoming fully electronic with
 * most being presented in real-time or with only a small delay of 15-20 minutes.
 * http://www.nasdaq.com/services/stock-ticker.stm
 *
 * This is just a lightweight value object. So, it can be used to lightly store
 * tickers at various time. That is, you can store many many tickers for same
 * symbol efficiently, as in case of composing an one minute ser.
 *
 * The TickerSnapshot will present the last current snapshot ticker for one
 * symbol, and implement Observable. You only need one TickerSnapshot for each
 * symbol.
 *
 * @author Caoyuan Deng
 */
@serializable @cloneable
final class Ticker($data: Array[Double], private var _marketDepth: MarketDepth) extends LightTicker($data) {

  def this(depth: Int) = this(new Array[Double](LightTicker.FIELD_LENGTH), new MarketDepth(new Array[Double](depth * 4)))
  def this() = this(5)

  def marketDepth = _marketDepth
  def marketDepth_=(marketDepth: MarketDepth) {
    this._marketDepth = marketDepth
  }
  
  def depth = marketDepth.depth

  def bidAsks = marketDepth.bidAsks
  def bidAsks_=(values: Array[Double]) {
    marketDepth.bidAsks = values
  }

  final def bidPrice(idx: Int) = marketDepth.bidPrice(idx)
  final def bidSize (idx: Int) = marketDepth.bidSize (idx)
  final def askPrice(idx: Int) = marketDepth.askPrice(idx)
  final def askSize (idx: Int) = marketDepth.askSize (idx)

  final def setBidPrice(idx: Int, v: Double) = marketDepth.setBidPrice(idx, v)
  final def setBidSize (idx: Int, v: Double) = marketDepth.setBidSize (idx, v)
  final def setAskPrice(idx: Int, v: Double) = marketDepth.setAskPrice(idx, v)
  final def setAskSize (idx: Int, v: Double) = marketDepth.setAskSize (idx, v)


  // --- no db fields:
  private var _name: String = ""
  def name = _name
  def name_=(v: String) {
    this._name = v
    if (_name != v) _isChanged = true
  }
  
  def isChanged = _isChanged || marketDepth.isChanged
  def isChanged_=(changed: Boolean) = {
    this._isChanged = changed
    if (!changed) {
      marketDepth.isChanged = false
    }
  }

  override def reset {
    super.reset

    var i = -1
    while ({i += 1; i < bidAsks.length}) {
      bidAsks(i) = 0
    }
  }

  override def copyFrom(another: LightTicker) {
    super.copyFrom(another)
    another match {
      case x: Ticker => 
        this.name = x.name
        System.arraycopy(x.bidAsks, 0, bidAsks, 0, bidAsks.length)
      case _ =>
    }
  }

  override def importFrom(v: (Long, Array[Double], Array[Double])): this.type = {
    this.time = v._1
    this.data = v._2
    this.marketDepth = MarketDepth(v._3)
    this
  }
  override def exportTo: (Long, Array[Double], Array[Double]) = {
    (time, data, marketDepth.bidAsks)
  }
  
  final def isValueChanged(another: Ticker): Boolean = {
    if (super.isValueChanged(another)) {
      return true
    }

    var i = -1
    while ({i += 1; i < bidAsks.length}) {
      if (bidAsks(i) != another.bidAsks(i)) {
        return true
      }
    }

    false
  }

  def toLightTicker: LightTicker = {
    val light = new LightTicker
    light.copyFrom(this)
    light
  }

  override def clone: Ticker = {
    try {
      return super.clone.asInstanceOf[Ticker]
    } catch {case ex: CloneNotSupportedException => ex.printStackTrace}

    null
  }
  
  override def toString = {
    val sb = new StringBuilder()
    sb.append("Ticker(").append(uniSymbol).append(util.formatTime(time)).append(",")
    sb.append(data.mkString("[", ",", "]")).append(",")
    sb.append(marketDepth)
    sb.append(")")toString
  }
}

// --- table

/**
 * Assume table BidAsk's structure:
 val idx = intColumn("idx")
 val isBid = booleanColumn("isBid")
 val price = numericColumn("price",  12, 2)
 val size = numericColumn("size", 12, 2)

 // Select latest ask_bid in each group of "isBid" and "idx"
 def latest = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)"
 }

 def latest(intraDayId: Long) = {
 "SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay_id = " + intraDayId + ") AND intraDay_id = " + intraDayId
 }

 */
abstract class TickersTable extends Table[Ticker] {
  protected val log = Logger.getLogger(this.getClass.getName)
  protected val ONE_DAY = 24 * 60 * 60 * 1000

  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val prevClose = "prevClose" DOUBLE()
  val lastPrice = "lastPrice" DOUBLE()

  val dayOpen   = "dayOpen"   DOUBLE()
  val dayHigh   = "dayHigh"   DOUBLE()
  val dayLow    = "dayLow"    DOUBLE()
  val dayVolume = "dayVolume" DOUBLE()
  val dayAmount = "dayAmount" DOUBLE()

  val dayChange = "dayChange" DOUBLE()

  val bidAsks = "bidAsks" SERIALIZED(classOf[Array[Double]], 400)

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)
}


object Tickers extends TickersTable {
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)

  private val lastTickersCache = mutable.Map[Long, mutable.Map[Sec, Ticker]]()

  def lastTickerOf(sec: Sec, dailyRoundedTime: Long): Ticker = {
    if (isServer) lastTickerOf_nonCached(sec, dailyRoundedTime) else lastTickerOf_cached(sec, dailyRoundedTime)
  }

  /**
   * @Note do not use it when table is partitioned on secs_id, since this qeury is only on time
   */
  def lastTickerOf_cached(sec: Sec, dailyRoundedTime: Long): Ticker = {
    val cached = lastTickersCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        if (lastTickersCache.size >= 4) {
          val earliest = lastTickersCache.map(_._1).min
          lastTickersCache.remove(earliest)
        }
        val map = lastTickersOf(dailyRoundedTime)
        lastTickersCache.put(dailyRoundedTime, map)
        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new Ticker
        newone.isTransient = true
        newone
    }
  }

  def lastTickerOf_nonCached(sec: Sec, dailyRoundedTime: Long): Ticker = {
    val res = try {
      SELECT (Tickers.*) FROM (Tickers) WHERE (
        (Tickers.sec.field EQ Secs.idOf(sec)) AND (Tickers.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (Tickers.time DESC) LIMIT (1) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res match {
      case Seq(one) =>
        one.isTransient = false
        one
      case Seq() =>
        val newone = new Ticker
        newone.isTransient = true
        newone
    }
  }

  def lastTickerOf_reference(sec: Sec, dailyRoundedTime: Long, tillTime: Long): Ticker = {
    val res = try {
      SELECT (Tickers.*) FROM (Tickers) WHERE (
        (Tickers.sec.field EQ Secs.idOf(sec)) AND (Tickers.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (Tickers.time DESC) LIMIT (2) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res match {
      case Seq() =>
        val newone = new Ticker
        newone.isDayFirst = true
        newone.isTransient = true
        newone
      case Seq(one) =>
        one.isDayFirst = true
        one.isTransient = false
        one
      case Seq(one, _*) =>
        one.isDayFirst = false
        one.isTransient = false
        one
    }
  }

  def tickersOf(sec: Sec, dailyRoundedTime: Long): Seq[Ticker] = {
    try {
      SELECT (Tickers.*) FROM (Tickers) WHERE (
        (Tickers.sec.field EQ Secs.idOf(sec)) AND (Tickers.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
      ) ORDER_BY (Tickers.time) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def lastTraingDayTickersOf(sec: Sec): Seq[Ticker] = {
    val res = try {
      SELECT (Tickers.time) FROM (Tickers) WHERE (
        Tickers.sec.field EQ Secs.idOf(sec)
      ) ORDER_BY (Tickers.time DESC) LIMIT (1) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res.headOption match {
      case Some(time) =>
        val cal = Calendar.getInstance(sec.exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)

        try {
          SELECT (Tickers.*) FROM (Tickers) WHERE (
            (Tickers.sec.field EQ Secs.idOf(sec)) AND (Tickers.time BETWEEN (rounded, rounded + ONE_DAY - 1))
          ) ORDER_BY (Tickers.time) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        }
      case None => Nil
    }
  }

  /**
   * Plan A:
   * SELECT a.* FROM orm.tickers AS a WHERE a.time = (SELECT max(time) FROM orm.tickers AS b LEFT JOIN orm.secs AS secs ON b.secs_id = secs.id WHERE b.time >= 1281715200000 AND b.time < 1281801600000 AND secs.exchanges_id = 1 AND b.secs_id = a.secs_id);
   * Plan B:
   * SELECT tickers.* FROM (SELECT tickers.secs_id as secs_id, MAX(tickers.time) AS maxtime FROM orm.tickers AS tickers LEFT JOIN orm.secs AS secs ON tickers.secs_id = secs.id WHERE tickers.time >= 1281715200000 AND tickers.time < 1281801600000 AND secs.exchanges_id = 1 GROUP BY tickers.secs_id) AS x INNER JOIN orm.tickers AS tickers ON x.secs_id = tickers.secs_id AND x.maxtime = tickers.time;
   */
  private[securities] def lastTradingDayTickersOf(exchange: Exchange): mutable.Map[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    val start = System.currentTimeMillis
    val map = mutable.Map[Sec, Ticker]()
    lastTradingTimeOf(exchange) match {
      case Some(time) =>
        val cal = Calendar.getInstance(exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)

        val res = try {
          new Select(Tickers.*) {
            private val sqlTickersTab = ORM.dialect.relationQualifiedName(Tickers)
            private val sqlSecsTab = ORM.dialect.relationQualifiedName(Secs)

            override def toSql = "SELECT tickers.id AS this_1, tickers.bidAsks AS this_2, tickers.dayChange AS this_3, tickers.dayAmount AS this_4, tickers.dayVolume AS this_5, tickers.dayLow AS this_6, tickers.dayHigh AS this_7, tickers.dayOpen AS this_8, tickers.lastPrice AS this_9, tickers.prevClose AS this_10, tickers.time AS this_11, tickers.secs_id AS this_12" +
            " FROM (SELECT tickers.secs_id AS secs_id, MAX(tickers.time) AS maxtime FROM " + sqlTickersTab + " AS tickers LEFT JOIN " + sqlSecsTab + " AS secs ON tickers.secs_id = secs.id" +
            " WHERE tickers.time >= " + rounded + " AND tickers.time < " + (rounded + ONE_DAY) + " AND secs.exchanges_id = " + Exchanges.idOf(exchange).get +
            " GROUP BY tickers.secs_id) AS x INNER JOIN + " + sqlTickersTab + " AS tickers ON x.secs_id = tickers.secs_id AND x.maxtime = tickers.time;"

          } list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        }
        res foreach {x => map.put(x.sec, x)}

        log.info(exchange.code + ": Loaded last tickers between " + rounded + " - " + (rounded + ONE_DAY - 1) +
                 ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
      case None =>
    }

    map
  }

  private[securities] def lastTickersOf(dailyRoundedTime: Long): mutable.Map[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    val start = System.currentTimeMillis
    val map = mutable.Map[Sec, Ticker]()

    val res = try {
      new Select(Tickers.*) {
        private val sqlTickersTab = ORM.dialect.relationQualifiedName(Tickers)

        override def toSql = "SELECT tickers.id AS this_1, tickers.bidAsks AS this_2, tickers.dayChange AS this_3, tickers.dayAmount AS this_4, tickers.dayVolume AS this_5, tickers.dayLow AS this_6, tickers.dayHigh AS this_7, tickers.dayOpen AS this_8, tickers.lastPrice AS this_9, tickers.prevClose AS this_10, tickers.time AS this_11, tickers.secs_id AS this_12" +
        " FROM (SELECT tickers.secs_id AS secs_id, MAX(tickers.time) AS maxtime FROM " + sqlTickersTab + " AS tickers" +
        " WHERE tickers.time >= " + dailyRoundedTime + " AND tickers.time < " + (dailyRoundedTime + ONE_DAY) +
        " GROUP BY tickers.secs_id) AS x INNER JOIN " + sqlTickersTab + " AS tickers ON x.secs_id = tickers.secs_id AND x.maxtime = tickers.time;"

      } list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res foreach {x => map.put(x.sec, x)}

    log.info("Loaded last tickers between " + dailyRoundedTime + " - " + (dailyRoundedTime + ONE_DAY - 1) +
             ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
    
    map
  }


//  private[securities] def lastTickersOf(exchange: Exchange): HashMap[Sec, Ticker] = {
//    Exchange.uniSymbolToSec // force loaded all secs and secInfos
//    SELECT (Tickers.*, Quotes1d.*) FROM (Tickers JOIN (Quotes1d JOIN Secs)) WHERE (
//      (Quotes1d.time EQ (
//          SELECT (MAX(Quotes1d.time)) FROM (Quotes1d JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange))
//        )
//      ) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
//    ) ORDER_BY (Tickers.time ASC, Tickers.id ASC) list match {
//      case xs if xs.isEmpty => new HashMap[Sec, Ticker]
//      case xs =>
//        val map = new HashMap[Sec, Ticker]
//        xs map (_._1) groupBy (_.quote.sec) foreach {case (sec, tickers) =>
//            map.put(sec, tickers.head)
//        }
//        map
//    }
//  }

  private[model] def lastTradingTimeOf(exchange: Exchange): Option[Long] = {
    Exchange.uniSymbolToSec // force loaded all secs and secInfos

    val res = try {
      SELECT (Tickers.time) FROM (Tickers JOIN Secs) WHERE (Secs.exchange.field EQ Exchanges.idOf(exchange)) ORDER_BY (Tickers.time DESC) LIMIT (1) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res.headOption
  }
}

object TickersLast extends TickersTable {

  private[model] def lastTickersOf(exchange: Exchange): mutable.Map[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force all secs and secInfos loaded

    val start = System.currentTimeMillis
    val map = mutable.Map[Sec, Ticker]()
    val res = try {SELECT(this.*) FROM (this JOIN Secs) WHERE (
        (Secs.exchange.field EQ Exchanges.idOf(exchange))
      ) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res foreach {x => map.put(x.sec, x)}

    log.info(exchange.code + ": Loaded all last tickers" +
             ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
    map
  }

  private[model] def lastTradingDayTickersOf(exchange: Exchange): mutable.Map[Sec, Ticker] = {
    Exchange.uniSymbolToSec // force all secs and secInfos loaded

    val start = System.currentTimeMillis
    val map = mutable.Map[Sec, Ticker]()

    lastTradingTimeOf(exchange) match {
      case Some(time) =>
        log.info(exchange.code + ": Last trading time is " + time)
        val cal = Calendar.getInstance(exchange.timeZone)
        val rounded = TFreq.DAILY.round(time, cal)

        val res = try {
          SELECT(this.*) FROM (this JOIN Secs) WHERE (
            (this.time BETWEEN (rounded, rounded + ONE_DAY - 1)) AND (Secs.exchange.field EQ Exchanges.idOf(exchange))
          ) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x => map.put(x.sec, x)}

        log.info(exchange.code + ": Loaded last tickers between " + rounded + " AND " + (rounded + ONE_DAY - 1) +
                 ": " + map.size + " in " + (System.currentTimeMillis - start) / 1000.0 + "s")
      case None =>
    }

    map
  }

  private[model] def lastTradingTimeOf(exchange: Exchange): Option[Long] = {
    Exchange.uniSymbolToSec // force all secs and secInfos loaded

    val res = try {
      SELECT (this.time) FROM (this JOIN Secs) WHERE (
        Secs.exchange.field EQ Exchanges.idOf(exchange)
      ) ORDER_BY (this.time DESC) LIMIT (1) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res.headOption
  }
}

