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

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable



/**
 * The definition of "super/large/small block" will depond on amount
 */
@serializable
final class MoneyFlow extends BelongsToSec with TVal with Flag {

  @transient protected var _sector: Sector = _
  def sector = _sector
  def sector_=(sector: Sector) {
    if (sector != null) {
      sector.crckey match {
        case null | "" => // skip this
        case x => _uniSymbol = x
      }
    }
    _sector = sector
  }

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

  @transient var freeFloat = 0.0
  private val data = new Array[Double](30)
  
  def superVolumeIn = data(0)
  def superAmountIn = data(1)
  def superVolumeOut = data(2)
  def superAmountOut = data(3)
  def superVolumeEven = data(4)
  def superAmountEven = data(5)

  def largeVolumeIn = data(6)
  def largeAmountIn = data(7)
  def largeVolumeOut = data(8)
  def largeAmountOut = data(9)
  def largeVolumeEven = data(10)
  def largeAmountEven = data(11)

  def mediumVolumeIn = data(12)
  def mediumAmountIn = data(13)
  def mediumVolumeOut = data(14)
  def mediumAmountOut = data(15)
  def mediumVolumeEven = data(16)
  def mediumAmountEven = data(17)

  def smallVolumeIn = data(18)
  def smallAmountIn = data(19)
  def smallVolumeOut = data(20)
  def smallAmountOut = data(21)
  def smallVolumeEven = data(22)
  def smallAmountEven = data(23)

  def amountInCount = data(24)
  def amountOutCount = data(25)

  /**
   * Weighted average relative amount.
   */
  def relativeAmount = data(26)
  def volumnPercentOfMarket = data(27)
  def netBuyPercent = data(28)

  def superVolumeIn_=(v: Double) {data(0) = v}
  def superAmountIn_=(v: Double) {data(1) = v}
  def superVolumeOut_=(v: Double) {data(2) = v}
  def superAmountOut_=(v: Double) {data(3) = v}
  def superVolumeEven_=(v: Double) {data(4) = v}
  def superAmountEven_=(v: Double) {data(5) = v}

  def largeVolumeIn_=(v: Double) {data(6) = v}
  def largeAmountIn_=(v: Double) {data(7) = v}
  def largeVolumeOut_=(v: Double) {data(8) = v}
  def largeAmountOut_=(v: Double) {data(9) = v}
  def largeVolumeEven_=(v: Double) {data(10) = v}
  def largeAmountEven_=(v: Double) {data(11) = v}

  def mediumVolumeIn_=(v: Double) {data(12) = v}
  def mediumAmountIn_=(v: Double) {data(13) = v}
  def mediumVolumeOut_=(v: Double) {data(14) = v}
  def mediumAmountOut_=(v: Double) {data(15) = v}
  def mediumVolumeEven_=(v: Double) {data(16) = v}
  def mediumAmountEven_=(v: Double) {data(17) = v}

  def smallVolumeIn_=(v: Double) {data(18) = v}
  def smallAmountIn_=(v: Double) {data(19) = v}
  def smallVolumeOut_=(v: Double) {data(20) = v}
  def smallAmountOut_=(v: Double) {data(21) = v}
  def smallVolumeEven_=(v: Double) {data(22) = v}
  def smallAmountEven_=(v: Double) {data(23) = v}

  def amountInCount_=(v: Double) {data(24) = v}
  def amountOutCount_=(v: Double) {data(25) = v}

  /**
   * Weighted average relative amount.
   */
  def relativeAmount_=(v: Double) {data(26) = v}
  def volumnPercentOfMarket_= (v: Double) {data(27) = v}
  def netBuyPercent_= (v: Double) {data(28) = v}

  // --- no db fields
  
  // sum
  def volumeIn = superVolumeIn + largeVolumeIn + mediumVolumeIn + smallVolumeIn
  def amountIn = superAmountIn + largeAmountIn + mediumAmountIn + smallAmountIn
  def volumeOut = superVolumeOut + largeVolumeOut + mediumVolumeOut + smallVolumeOut
  def amountOut = superAmountOut + largeAmountOut + mediumAmountOut + smallAmountOut
  def volumeEven = superVolumeEven + largeVolumeEven + mediumVolumeEven + smallVolumeEven
  def amountEven = superAmountEven + largeAmountEven + mediumAmountEven + smallAmountEven

  // net sum
  def volumeNet = volumeIn + volumeOut
  def amountNet = amountIn + amountOut
  
  // net super
  def superVolumeNet = superVolumeIn + superVolumeOut
  def superAmountNet = superAmountIn + superAmountOut
  
  // net large
  def largeVolumeNet = largeVolumeIn + largeVolumeOut
  def largeAmountNet = largeAmountIn + largeAmountOut
  
  // net meduam
  def mediumVolumeNet = mediumVolumeIn + mediumVolumeOut
  def mediumAmountNet = mediumAmountIn + mediumAmountOut
  
  // net small
  def smallVolumeNet = smallVolumeIn + smallVolumeOut
  def smallAmountNet = smallAmountIn + smallAmountOut

  var isTransient = true

  def copyFrom(another: MoneyFlow) {
    this._sec = another._sec
    this._sector = another._sector
    this._uniSymbol = another._uniSymbol
    this._time = another._time
    this._lastModify = another._lastModify
    this._flag = another._flag
    this.isTransient = another.isTransient
    System.arraycopy(another.data, 0, data, 0, data.length)
    if (another.freeFloat.isNaN || another.freeFloat.isInfinite || another.freeFloat.isNegInfinity) another.freeFloat = 0.0
    this.freeFloat = another.freeFloat
    if (this.netBuyPercent.isNaN || this.netBuyPercent.isInfinite || this.netBuyPercent.isNegInfinity) this.netBuyPercent = 0
    if (this.relativeAmount.isNaN || this.relativeAmount.isInfinite || this.relativeAmount.isNegInfinity) this.relativeAmount = 0
    if (this.volumnPercentOfMarket.isNaN || this.volumnPercentOfMarket.isInfinite || this.volumnPercentOfMarket.isNegInfinity) this.volumnPercentOfMarket = 0
  }

  def clearData{
    this._time = 0
    this._lastModify = 0
    this._flag = 1
    this.isTransient = true
    this.freeFloat = 0

    var i = -1
    while({i += 1; i < data.length})data(i) = 0
  }

  def isDataOnlyInited: Boolean = {
    if (this.freeFloat != 0) return false

    var i = -1
    while({i += 1; i < data.length}) if (data(i) != 0) return false

    return true
  }

  def addBy(another: MoneyFlow) {
    if (this.time != another.time) return
    if (another._lastModify > this._lastModify) this._lastModify = another._lastModify
    val nbpSum = this.netBuyPercent * this.freeFloat + another.netBuyPercent * another.freeFloat
    var i = -1
    while({i += 1; i < data.length}){
      this.data(i) += another.data(i)
    }

    if (another.freeFloat.isNaN || another.freeFloat.isInfinite || another.freeFloat.isNegInfinity) another.freeFloat = 0.0
    this.freeFloat += another.freeFloat
    this.netBuyPercent = nbpSum / this.freeFloat
    if (this.netBuyPercent.isNaN || this.netBuyPercent.isInfinite || this.netBuyPercent.isNegInfinity) this.netBuyPercent = 0
    this.relativeAmount = this.amountNet / (this.amountEven + this.amountIn - this.amountOut)
    if (this.relativeAmount.isNaN || this.relativeAmount.isInfinite || this.relativeAmount.isNegInfinity) this.relativeAmount = 0
  }

  override def equals(another: Any): Boolean = another match{
    case mf: MoneyFlow =>
      if (this._sec != mf._sec) return false
      if (this.sector != mf.sector) return false
      if (this._uniSymbol != mf._uniSymbol) return false
      if (this._time != mf._time) return false
      if (this._flag != mf._flag) return false
      if (this.freeFloat != mf.freeFloat) return false
      var i = -1
      while({i += 1; i<data.length}){
        if (valueNonEquals(data(i), mf.data(i))) return false
      }
      
      return true
    case _ => false
  }

  private def valueNonEquals(a: Double, b: Double) = !valueEquals(a, b)
  private def valueEquals(a: Double, b: Double): Boolean = {
    if (a.isNaN && b.isNaN) return true
    if (a.isNegInfinity && b.isNegInfinity) return true
    if (a.isInfinite && b.isInfinite) return true
    math.abs(a - b) < 1e-6
  }

  final def diffValues(that: MoneyFlow): List[(String, Any, Any)] = {
    var result: List[(String, Any, Any)] = Nil

    if (this._uniSymbol != that._uniSymbol)
      result = ("unisymbol", this._uniSymbol, that._uniSymbol) :: result

    if (this._time != that._time)
      result = ("time", this._time, that._time) :: result

    if (this._flag != that._flag)
      result = ("flag", this._flag, that._flag) :: result

    if (valueNonEquals(this.freeFloat, that.freeFloat))
      result = ("freeFloat", this.freeFloat, that.freeFloat) :: result
    
    var i = -1
    while({i += 1; i<data.length}){
      if (valueNonEquals(this.data(i), that.data(i)))
        result = ("data " + i, this.data(i), that.data(i)) :: result
    }

    result
  }
}

abstract class MoneyFlows extends Table[MoneyFlow] with TableEx{
  protected val log = Logger.getLogger(this.getClass.getName)
  
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val superVolumeIn = "superVolumeIn" DOUBLE()
  val superAmountIn = "superAmountIn" DOUBLE()
  val superVolumeOut = "superVolumeOut" DOUBLE()
  val superAmountOut = "superAmountOut" DOUBLE()
  val superVolumeEven = "superVolumeEven" DOUBLE()
  val superAmountEven = "superAmountEven" DOUBLE()

  val largeVolumeIn = "largeVolumeIn" DOUBLE()
  val largeAmountIn = "largeAmountIn" DOUBLE()
  val largeVolumeOut = "largeVolumeOut" DOUBLE()
  val largeAmountOut = "largeAmountOut" DOUBLE()
  val largeVolumeEven = "largeVolumeEven" DOUBLE()
  val largeAmountEven = "largeAmountEven" DOUBLE()

  val mediumVolumeIn = "mediumVolumeIn" DOUBLE()
  val mediumAmountIn = "mediumAmountIn" DOUBLE()
  val mediumVolumeOut = "mediumVolumeOut" DOUBLE()
  val mediumAmountOut = "mediumAmountOut" DOUBLE()
  val mediumVolumeEven = "mediumVolumeEven" DOUBLE()
  val mediumAmountEven = "mediumAmountEven" DOUBLE()

  val smallVolumeIn = "smallVolumeIn" DOUBLE()
  val smallAmountIn = "smallAmountIn" DOUBLE()
  val smallVolumeOut = "smallVolumeOut" DOUBLE()
  val smallAmountOut = "smallAmountOut" DOUBLE()
  val smallVolumeEven = "smallVolumeEven" DOUBLE()
  val smallAmountEven = "smallAmountEven" DOUBLE()
  
  val flag = "flag" INTEGER()

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  def moneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    try {
      val mfs = {SELECT (this.*) FROM (this) WHERE (
          this.sec.field EQ Secs.idOf(sec)
        ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list}

      mfs foreach {mf =>
        if (mf.amountNet >= 0){
          mf.amountInCount = 1
          mf.amountOutCount = 0
        }
        else {
          mf.amountInCount = 0
          mf.amountOutCount = 1
        }

        mf.relativeAmount = mf.volumeNet / (mf.volumeEven + mf.volumeIn - mf.volumeOut)
        mf.lastModify = mf.time
      }

      mfs
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def closedMoneyFlowOf(sec: Sec): Seq[MoneyFlow] = {
    val xs = new ArrayList[MoneyFlow]()
    for (x <- moneyFlowOf(sec) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedMoneyFlowOf__filterByDB(sec: Sec): Seq[MoneyFlow] = {
    try {
      val mfs = {SELECT (this.*) FROM (this) WHERE (
          (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
        ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list}

      mfs foreach {mf =>
        if (mf.amountNet >= 0){
          mf.amountInCount = 1
          mf.amountOutCount = 0
        }
        else {
          mf.amountInCount = 0
          mf.amountOutCount = 1
        }

        mf.relativeAmount = mf.volumeNet / (mf.volumeEven + mf.volumeIn - mf.volumeOut)
        mf.lastModify = mf.time
      }

      mfs
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }
  
  def saveBatch(sec: Sec, sortedMfs: Seq[MoneyFlow]) {
    if (sortedMfs.isEmpty) return

    val head = sortedMfs.head
    val last = sortedMfs.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, MoneyFlow]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedMfs.partition(x => exists.contains(x.time))
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
  
  def saveBatch(atSameTime: Long, mfs: Array[MoneyFlow]) {
    if (mfs.isEmpty) return

    val exists = mutable.Map[Sec, MoneyFlow]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sec.field GT 0) AND (this.sec.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res foreach {x => exists.put(x.sec, x)}

    val updates = new ArrayList[MoneyFlow]()
    val inserts = new ArrayList[MoneyFlow]()
    var i = -1
    while ({i += 1; i < mfs.length}) {
      val quote = mfs(i)
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

// --- table
object MoneyFlows1d extends MoneyFlows {  
  private val dailyCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  @deprecated
  def dailyMoneyFlowOf(sec: Sec, dailyRoundedTime: Long): MoneyFlow = {
    val cached = dailyCache.get(dailyRoundedTime) match {
      case Some(map) => map
      case None =>
        dailyCache.clear
        val map = mutable.Map[Sec, MoneyFlow]()
        dailyCache.put(dailyRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ dailyRoundedTime)
          ) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        }
        res foreach {x =>
          if (x.amountNet >= 0){
            x.amountInCount = 1
            x.amountOutCount = 0
          }
          else {
            x.amountInCount = 0
            x.amountOutCount = 1
          }

          x.relativeAmount = x.volumeNet / (x.volumeEven + x.volumeIn - x.volumeOut)
          x.lastModify = x.time

          map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.DAILY, newone)
        newone
    }
  }

  @deprecated
  def dailyMoneyFlowOf_nonCached(sec: Sec, dailyRoundedTime: Long): MoneyFlow = synchronized {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ dailyRoundedTime)
      ) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res headOption match {
      case Some(one) =>
        one.isTransient = false
        if (one.amountNet >= 0){
          one.amountInCount = 1
          one.amountOutCount = 0
        }
        else {
          one.amountInCount = 0
          one.amountOutCount = 1
        }
        one.relativeAmount = one.volumeNet / (one.volumeEven + one.volumeIn - one.volumeOut)
        one.lastModify = one.time
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = dailyRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.DAILY, newone)
        newone
    }
  }
}

object MoneyFlows1m extends MoneyFlows {
  private val config = org.aiotrade.lib.util.config.Config()
  protected val isServer = !config.getBool("dataserver.client", false)

  private val minuteCache = mutable.Map[Long, mutable.Map[Sec, MoneyFlow]]()

  @deprecated
  def minuteMoneyFlowOf(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    if (isServer) minuteMoneyFlowOf_nonCached(sec, minuteRoundedTime) else minuteMoneyFlowOf_cached(sec, minuteRoundedTime)
  }

  /**
   * @Note do not use it when table is partitioned on secs_id, since this qeury is only on time
   */
  @deprecated
  def minuteMoneyFlowOf_cached(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    val cached = minuteCache.get(minuteRoundedTime) match {
      case Some(map) => map
      case None =>
        minuteCache.clear
        val map = mutable.Map[Sec, MoneyFlow]()
        minuteCache.put(minuteRoundedTime, map)

        val res = try {
          SELECT (this.*) FROM (this) WHERE (
            (this.time EQ minuteRoundedTime)
          ) list
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
        } 
        res foreach {x =>
          if (x.amountNet >= 0){
            x.amountInCount = 1
            x.amountOutCount = 0
          }
          else {
            x.amountInCount = 0
            x.amountOutCount = 1
          }

          x.relativeAmount = x.volumeNet / (x.volumeEven + x.volumeIn - x.volumeOut)
          x.lastModify = x.time

          map.put(x.sec, x)}

        map
    }

    cached.get(sec) match {
      case Some(one) =>
        one.isTransient = false
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.ONE_MIN, newone)
        newone
    }
  }

  @deprecated
  def minuteMoneyFlowOf_nonCached(sec: Sec, minuteRoundedTime: Long): MoneyFlow = {
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time EQ minuteRoundedTime)
      ) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    } 
    res headOption match {
      case Some(one) =>
        one.isTransient = false
        if (one.amountNet >= 0){
          one.amountInCount = 1
          one.amountOutCount = 0
        }
        else {
          one.amountInCount = 0
          one.amountOutCount = 1
        }
        one.relativeAmount = one.volumeNet / (one.volumeEven + one.volumeIn - one.volumeOut)
        one.lastModify = one.time
        one
      case None =>
        val newone = new MoneyFlow
        newone.time = minuteRoundedTime
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.ONE_MIN, newone)
        newone
    }
  }
}

abstract class SectorMoneyFlows extends Table[MoneyFlow] with TableEx{
  protected val log = Logger.getLogger(this.getClass.getName)

  val sector = "sectors_id" BIGINT() REFERENCES(Sectors)
  val time = "time" BIGINT()

  val amountInCount = "amountInCount" DOUBLE()
  val amountOutCount = "amountOutCount" DOUBLE()

  val superVolumeIn = "superVolumeIn" DOUBLE()
  val superAmountIn = "superAmountIn" DOUBLE()
  val superVolumeOut = "superVolumeOut" DOUBLE()
  val superAmountOut = "superAmountOut" DOUBLE()
  val superVolumeEven = "superVolumeEven" DOUBLE()
  val superAmountEven = "superAmountEven" DOUBLE()

  val largeVolumeIn = "largeVolumeIn" DOUBLE()
  val largeAmountIn = "largeAmountIn" DOUBLE()
  val largeVolumeOut = "largeVolumeOut" DOUBLE()
  val largeAmountOut = "largeAmountOut" DOUBLE()
  val largeVolumeEven = "largeVolumeEven" DOUBLE()
  val largeAmountEven = "largeAmountEven" DOUBLE()

  val mediumVolumeIn = "mediumVolumeIn" DOUBLE()
  val mediumAmountIn = "mediumAmountIn" DOUBLE()
  val mediumVolumeOut = "mediumVolumeOut" DOUBLE()
  val mediumAmountOut = "mediumAmountOut" DOUBLE()
  val mediumVolumeEven = "mediumVolumeEven" DOUBLE()
  val mediumAmountEven = "mediumAmountEven" DOUBLE()

  val smallVolumeIn = "smallVolumeIn" DOUBLE()
  val smallAmountIn = "smallAmountIn" DOUBLE()
  val smallVolumeOut = "smallVolumeOut" DOUBLE()
  val smallAmountOut = "smallAmountOut" DOUBLE()
  val smallVolumeEven = "smallVolumeEven" DOUBLE()
  val smallAmountEven = "smallAmountEven" DOUBLE()

  val relativeAmount = "relativeAmount" DOUBLE()
  val volumnPercentOfMarket = "volumnPercentOfMarket" DOUBLE()
  val netBuyPercent = "netBuyPercent" DOUBLE()

  val flag = "flag" INTEGER()

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  /**
   * Save the sector's money flow
   * @Note Need to COMMIT after it.
   */
  def saveBatch(sector: Sector, sortedMfs: Seq[MoneyFlow]) {
    if (sortedMfs.isEmpty) return

    val head = sortedMfs.head
    val last = sortedMfs.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[Long, MoneyFlow]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.sector.field EQ Sectors.idOf(sector)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.time, x)}

    val (updates, inserts) = sortedMfs.partition(x => exists.contains(x.time))
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

  def saveBatch(atSameTime: Long, mfs: Array[MoneyFlow]) {
    if (mfs.isEmpty) return

    val exists = mutable.Map[Sec, MoneyFlow]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sector.field GT 0) AND (this.sector.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.sec, x)}

    val updates = new ArrayList[MoneyFlow]()
    val inserts = new ArrayList[MoneyFlow]()
    var i = -1
    while ({i += 1; i < mfs.length}) {
      val quote = mfs(i)
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

  def moneyFlowOf(sector: Sector): Seq[MoneyFlow] = {
    try {
      val mfs = {SELECT (this.*) FROM (this) WHERE (
          this.sector.field EQ Sectors.idOf(sector)
        ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list}

      mfs foreach {mf => mf.lastModify = mf.time}

      mfs
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  def closedMoneyFlowOf(sector: Sector): Seq[MoneyFlow] = {
    val xs = new ArrayList[MoneyFlow]()
    for (x <- moneyFlowOf(sector) if x.closed_?) {
      xs += x
    }
    xs
  }

  def closedMoneyFlowOf__filterByDB(sector: Sector): Seq[MoneyFlow] = {
    try {
      val mfs = {SELECT (this.*) FROM (this) WHERE (
          (this.sector.field EQ Sectors.idOf(sector)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
        ) ORDER_BY (this.time DESC) LIMIT(MAX_DATA_LENGTH) list}

      mfs foreach {mf => mf.lastModify = mf.time}

      mfs
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

}

object SectorMoneyFlows1d extends SectorMoneyFlows{
  
}

object SectorMoneyFlows1m extends SectorMoneyFlows{
  
}

object MoneyFlow {
  private val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(vmap: collection.Map[String, Array[_]]): Array[MoneyFlow] = {
    val mfs = new ArrayList[MoneyFlow]()
    try {
      val times = vmap(".")

      val amountInCount = vmap("aIC")
      val amountOutCount = vmap("aOC")
      val relativeAmount = vmap("RA")

      val volumeIns = vmap("Vi")
      val amountIns = vmap("Ai")
      val volumeOuts = vmap("Vo")
      val amountOuts = vmap("Ao")
      val volumeEvens = vmap("Ve")
      val amountEvens = vmap("Ae")
  
      val superVolumeIns = vmap("suVi")
      val superAmountIns = vmap("suAi")
      val superVolumeOuts = vmap("suVo")
      val superAmountOuts = vmap("suAo")
      val superVolumeEvens = vmap("suVe")
      val superAmountEvens = vmap("suAe")

      val largeVolumeIns = vmap("laVi")
      val largeAmountIns = vmap("laAi")
      val largeVolumeOuts = vmap("laVo")
      val largeAmountOuts = vmap("laAo")
      val largeVolumeEvens = vmap("laVe")
      val largeAmountEvens = vmap("laAe")

      val mediumVolumeIns = vmap("meVi")
      val mediumAmountIns = vmap("meAi")
      val mediumVolumeOuts = vmap("meVo")
      val mediumAmountOuts = vmap("meAo")
      val mediumVolumeEvens = vmap("meVe")
      val mediumAmountEvens = vmap("meAe")

      val smallVolumeIns = vmap("smVi")
      val smallAmountIns = vmap("smAi")
      val smallVolumeOuts = vmap("smVo")
      val smallAmountOuts = vmap("smAo")
      val smallVolumeEvens = vmap("smVe")
      val smallAmountEvens = vmap("smAe")

      val volumeNets = vmap("V")
      val amountNets = vmap("A")
      val superVolumeNets = vmap("suV")
      val superAmountNets = vmap("suA")
      val largeVolumeNets = vmap("laV")
      val largeAmountNets = vmap("laA")
      val mediumVolumeNets = vmap("meV")
      val mediumAmountNets = vmap("meA")
      val smallVolumeNets = vmap("smV")
      val smallAmountNets = vmap("smA")
      
      var i = -1
      while ({i += 1; i < times.length}) {
        val mf = new MoneyFlow

        mf.time = times(i).asInstanceOf[Long]

        mf.amountInCount = amountInCount(i).asInstanceOf[Double]
        mf.amountOutCount = amountOutCount(i).asInstanceOf[Double]
        mf.relativeAmount = relativeAmount(i).asInstanceOf[Double]

        mf.superVolumeIn = superVolumeIns(i).asInstanceOf[Double]
        mf.superAmountIn = superAmountIns(i).asInstanceOf[Double]
        mf.superVolumeOut = superVolumeOuts(i).asInstanceOf[Double]
        mf.superAmountOut = superAmountOuts(i).asInstanceOf[Double]
        mf.superVolumeEven = superVolumeEvens(i).asInstanceOf[Double]
        mf.superAmountEven = superAmountEvens(i).asInstanceOf[Double]

        mf.largeVolumeIn = largeVolumeIns(i).asInstanceOf[Double]
        mf.largeAmountIn = largeAmountIns(i).asInstanceOf[Double]
        mf.largeVolumeOut = largeVolumeOuts(i).asInstanceOf[Double]
        mf.largeAmountOut = largeAmountOuts(i).asInstanceOf[Double]
        mf.largeVolumeEven = largeVolumeEvens(i).asInstanceOf[Double]
        mf.largeAmountEven = largeAmountEvens(i).asInstanceOf[Double]

        mf.mediumVolumeIn = mediumVolumeIns(i).asInstanceOf[Double]
        mf.mediumAmountIn = mediumAmountIns(i).asInstanceOf[Double]
        mf.mediumVolumeOut = mediumVolumeOuts(i).asInstanceOf[Double]
        mf.mediumAmountOut = mediumAmountOuts(i).asInstanceOf[Double]
        mf.mediumVolumeEven = mediumVolumeEvens(i).asInstanceOf[Double]
        mf.mediumAmountEven = mediumAmountEvens(i).asInstanceOf[Double]

        mf.smallVolumeIn = smallVolumeIns(i).asInstanceOf[Double]
        mf.smallAmountIn = smallAmountIns(i).asInstanceOf[Double]
        mf.smallVolumeOut = smallVolumeOuts(i).asInstanceOf[Double]
        mf.smallAmountOut = smallAmountOuts(i).asInstanceOf[Double]
        mf.smallVolumeEven = smallVolumeEvens(i).asInstanceOf[Double]
        mf.smallAmountEven = smallAmountEvens(i).asInstanceOf[Double]

        mfs += mf
      }
    } catch {
      case ex: Throwable => log.warning(ex.getMessage)
    }

    mfs.toArray
  }
}
