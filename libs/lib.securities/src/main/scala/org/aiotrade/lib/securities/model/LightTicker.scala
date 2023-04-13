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
import org.aiotrade.lib.math.timeseries.TVal

/**
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

@cloneable @serializable @SerialVersionUID(1L)
class LightTicker(private var _data: Array[Double]) extends BelongsToSec with TVal {
  import LightTicker._
  
  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  @transient final protected var _isChanged: Boolean = _

  def this() = this(new Array[Double](LightTicker.FIELD_LENGTH))

  final protected def data = _data
  final protected def data_=(data: Array[Double]) {
    this._data = data
  }
  
  final def prevClose = data(PREV_CLOSE)
  final def lastPrice = data(LAST_PRICE)
  final def dayOpen   = data(DAY_OPEN)
  final def dayHigh   = data(DAY_HIGH)
  final def dayLow    = data(DAY_LOW)
  final def dayVolume = data(DAY_VOLUME)
  final def dayAmount = data(DAY_AMOUNT)
  final def dayChange = data(DAY_CHANGE)

  final def prevClose_=(v: Double) = updateFieldValue(PREV_CLOSE, v)
  final def lastPrice_=(v: Double) = updateFieldValue(LAST_PRICE, v)
  final def dayOpen_=  (v: Double) = updateFieldValue(DAY_OPEN,   v)
  final def dayHigh_=  (v: Double) = updateFieldValue(DAY_HIGH,   v)
  final def dayLow_=   (v: Double) = updateFieldValue(DAY_LOW,    v)
  final def dayVolume_=(v: Double) = updateFieldValue(DAY_VOLUME, v)
  final def dayAmount_=(v: Double) = updateFieldValue(DAY_AMOUNT, v)
  final def dayChange_=(v: Double) = updateFieldValue(DAY_CHANGE, v)

  
  // --- no db fields
  var isTransient = true
  var isDayFirst = false

  protected def updateFieldValue(fieldIdx: Int, v: Double) {
    if (data(fieldIdx) != v) {
      _isChanged = true
    }
    data(fieldIdx) = v
  }

  final def changeInPercent: Double = {
    if (prevClose == 0) 0f  else (lastPrice - prevClose) / prevClose * 100f
  }

  final def isDayVolumeGrown(prevTicker: LightTicker): Boolean = {
    dayVolume > prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isDayVolumeChanged(prevTicker: LightTicker): Boolean = {
    dayVolume != prevTicker.dayVolume // && isSameDay(prevTicker) @todo
  }

  final def isSameDay(prevTicker: LightTicker, cal: Calendar): Boolean = {
    cal.setTimeInMillis(time)
    val monthA = cal.get(Calendar.MONTH)
    val dayA = cal.get(Calendar.DAY_OF_MONTH)
    cal.setTimeInMillis(prevTicker.time)
    val monthB = cal.get(Calendar.MONTH)
    val dayB = cal.get(Calendar.DAY_OF_MONTH)

    monthB == monthB && dayA == dayB
  }

  final def compareLastPriceTo(prevTicker: LightTicker): Int = {
    if (lastPrice > prevTicker.lastPrice) 1
    else if (lastPrice == prevTicker.lastPrice) 0
    else 1
  }

  def reset {
    time = 0

    var i = -1
    while ({i += 1; i < data.length}) {
      data(i) = 0
    }
  }

  def copyFrom(another: LightTicker) {
    this.sec = another.sec
    this.time = another.time
    this.uniSymbol = another.uniSymbol
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  def importFrom(v: (Long, Array[Double], Array[Double])): this.type = {
    this.time = v._1
    this.data = v._2
    this
  }
  
  /** export to tuple */
  def exportTo: (Long, Array[Double], Array[Double]) = {
    (time, data, Array())
  }
  
  def isValueChanged(another: LightTicker): Boolean = {
    var i = -1
    while ({i += 1; i < data.length}) {
      if (data(i) != another.data(i)) {
        return true
      }
    }

    false
  }

  override def clone: LightTicker = {
    val cloneOne = new LightTicker
    cloneOne.copyFrom(this)
    cloneOne
  }

  override def toString = {
    "LightTicker(" + "symbol=" + uniSymbol + ", time=" + time + ", data=" + data.mkString("[", ",", "]") + ")"
  }
}

object LightTicker {
  private val PREV_CLOSE = 0
  private val LAST_PRICE = 1
  private val DAY_OPEN   = 2
  private val DAY_HIGH   = 3
  private val DAY_LOW    = 4
  private val DAY_VOLUME = 5
  private val DAY_AMOUNT = 6
  private val DAY_CHANGE = 7

  val FIELD_LENGTH = 8
}

