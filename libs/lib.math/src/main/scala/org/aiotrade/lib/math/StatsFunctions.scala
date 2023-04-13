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
package org.aiotrade.lib.math

import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.collection.ArrayList

/**
 *
 * @author Caoyuan Deng
 */
object StatsFunctions {

  val MAX = 0
  val MIN = 1
  val VALUE = 0
  val MASS = 1

  def sum(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = sum(values.toArray, fromIdx, toIdx)
  def sum(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    if (fromIdx < 0 || toIdx >= values.length) {
      return Null.Double
    }

    var sum = 0.0
    var i = fromIdx
    while (i <= toIdx) {
      val value = values(i)
      if (Null.not(value)) {
        sum += value
      }
      i += 1
    }

    sum
  }

  def isum(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = isum(idx, values.toArray, period, prev)
  def isum(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.length) {
      Null.Double
    } else if (lookbackIdx == 0) {
      /** compute first availabe sum (in case of enough period first time) */
      sum(values, 0, idx)
    } else {
      if (Null.is(prev)) {
        /**
         * although the 'values' size is enough, it may contains Null.Double
         * element, thus cause the prevSum to be a Null.Double, we should
         * precess this case by:
         */
        sum(values, lookbackIdx, idx)
      } else {
        prev + values(idx) - values(lookbackIdx - 1)
      }
    }
  }

  def ma(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = ma(values.toArray, fromIdx, toIdx)
  def ma(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    if (fromIdx < 0 || toIdx >= values.length) {
      return Null.Double
    }

    val period1 = period(fromIdx, toIdx).toDouble
    sum(values, fromIdx, toIdx) / period1
  }

  /**
   * ma(t + 1) = ma(t) + ( x(t) / N - x(t - n) / N )
   */
  def ima(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = ima(idx, values.toArray, period, prev)
  def ima(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.length) {
      Null.Double
    } else if (lookbackIdx == 0) {
      /** compute first available ma (in case of enough period first time) */
      ma(values, 0, idx)
    } else {
      if (Null.is(prev)) {
        /**
         * although the 'values' size is enough, it may contains Null.Double
         * element, thus cause the prevSum to be a Null.Double, we should
         * precess this case by:
         */
        ma(values, lookbackIdx, idx)
      } else {
        prev + (values(idx) - values(lookbackIdx - 1)) / (period * 1f)
      }
    }
  }

  def ema(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = ema(values.toArray, fromIdx, toIdx)
  def ema(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    if (fromIdx < 0 || toIdx >= values.length) {
      return Null.Double
    }

    val period1 = period(fromIdx, toIdx).toDouble
    var ema = 0.0
    var i = fromIdx
    while (i <= toIdx) {
      ema += ((period1 - 1.0) / (period1 + 1.0)) * ema + (2.0 / (period1 + 1.0)) * values(i)
      i += 1
    }

    ema
  }

  /**
   * ema(t + 1) = ema(t) + ( x(t) / N - ema(t) / N )
   *            = (1 - 1/N) * ema(t) + (1/N) * x(t)
   *            = (1 - a) * ema(t) + a * x(t)  // let a = 1/N
   */
  def iema(idx: Int, values: Array[Double], period: Int, prev: Double): Double = iema(idx, values.toArray, period, prev)
  def iema(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = {
    var value = values(idx)
    if (Null.is(value)) value = 0.0


    /** @todo */
    if (Null.is(prev)) {
      0F
    } else {
      val a = 1.0 / (period * 1.0)
      (1.0 - a) * prev + a * value
    }
    //return ((period - 1.0f) / (period + 1.0f)) * prevEma + (2.0f / (period + 1.0f)) * value;
  }

  def max(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = max(values.toArray, fromIdx, toIdx)
  def max(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    maxmin(values, fromIdx, toIdx)(MAX)
  }

  def imax(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = imax(idx, values.toArray, period, prev)
  def imax(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.length) {
      Null.Double
    } else if (lookbackIdx == 0) {
      max(values, 0, idx)
    } else {
      if (Null.is(prev) || values(lookbackIdx - 1) == prev) {
        max(values, lookbackIdx, idx)
      } else {
        val value = values(idx)
        if (prev >= value) prev else value
      }
    }
  }

  def min(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = min(values.toArray, fromIdx, toIdx)
  def min(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    maxmin(values, fromIdx, toIdx)(MIN)
  }

  def imin(idx: Int, values: ArrayList[Double], period: Int, prev: Double): Double = imin(idx, values.toArray, period, prev)
  def imin(idx: Int, values: Array[Double], period: Int, prev: Double): Double = {
    val lookbackIdx = lookback(idx, period)

    if (lookbackIdx < 0 || idx >= values.length) {
      Null.Double
    } else if (lookbackIdx == 0) {
      min(values, 0, idx)
    } else {
      if (Null.is(prev) || values(lookbackIdx - 1) == prev) {
        min(values, lookbackIdx, idx)
      } else {
        val value = values(idx)
        if (prev <= value) prev else value
      }
    }
  }

  def maxmin(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Array[Double] = maxmin(values.toArray, fromIdx, toIdx)
  def maxmin(values: Array[Double], fromIdx: Int, toIdx: Int): Array[Double] = {
    if (fromIdx < 0) {
      return Array(Null.Double, Null.Double)
    }

    var max = Double.MinValue
    var min = Double.MaxValue
    val lastIdx = math.min(toIdx, values.length - 1)
    var i = fromIdx
    while (i <= lastIdx) {
      val value = values(i)
      if (Null.not(value)) {
        max = math.max(max, value)
        min = math.min(min, value)
      }
      i += 1
    }

    Array(max, min)
  }

  /**
   * Standard Deviation
   */
  def stdDev(values: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = stdDev(values.toArray, fromIdx, toIdx)
  def stdDev(values: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    if (fromIdx < 0 || toIdx >= values.length) {
      return Null.Double
    }

    val ma1 = ma(values, fromIdx, toIdx)
    val lastIdx = math.min(toIdx, values.length - 1)
    var deviation_square_sum = 0.0
    var i = fromIdx
    while (i <= lastIdx) {
      val deviation = values(i) - ma1
      deviation_square_sum += deviation * deviation
      i += 1
    }

    val period1 = period(fromIdx, toIdx).toDouble
    math.sqrt(deviation_square_sum / period1)
  }

  def correlation(xs: ArrayList[Double], ys: ArrayList[Double], fromIdx: Int, toIdx: Int): Double = correlation(xs.toArray, ys.toArray, fromIdx, toIdx)
  def correlation(xs: Array[Double], ys: Array[Double], fromIdx: Int, toIdx: Int): Double = {
    assert(
      xs != null && ys != null && xs.length >= 2 && xs.length == ys.length, 
      "Invald params: x.length(>=2)=%s, y.length(=x.length)=%s".format(xs.length, ys.length)
    )
    
    if (fromIdx < 0 || toIdx >= xs.length) {
      return Null.Double
    }
    
    val ma_x = ma(xs, fromIdx, toIdx)
    val ma_y = ma(ys, fromIdx, toIdx)
    
    val lastIdx = math.min(toIdx, xs.length - 1)
    var covxy = 0.0
    var dev_x = 0.0
    var dev_y = 0.0
    var r = 0.0
    var i = fromIdx
    while (i <= lastIdx) {
      val dx = if (Null.not(xs(i))) xs(i) - ma_x else 0
      val dy = if (Null.not(ys(i))) ys(i) - ma_y else 0
      covxy += dx * dy
      dev_x += dx * dx
      dev_y += dy * dy
      i += 1
    }
    
    if (dev_x == 0 || dev_y == 0) {
      0
    } else {
      covxy / math.sqrt(dev_x * dev_y)
    }
  }
  
  /**
   * Probability Mass Function
   */
  def probMass(values: ArrayList[Double], fromIdx: Int, toIdx: Int, nIntervals: Int): Array[Array[Double]] = probMass(values.toArray, fromIdx, toIdx, nIntervals)
  def probMass(values: Array[Double], fromIdx: Int, toIdx: Int, nIntervals: Int): Array[Array[Double]] = {
    probMass(values, null.asInstanceOf[Array[Double]], fromIdx, toIdx, nIntervals)
  }
  
  /**
   * Probability Mass Function
   */
  def probMass(values: ArrayList[Double], weights: ArrayList[Double], fromIdx: Int, toIdx: Int, nIntervals: Int): Array[Array[Double]] = probMass(values.toArray, weights.toArray, fromIdx, toIdx, nIntervals)
  def probMass(values: Array[Double], weights: Array[Double],
               fromIdx: Int, toIdx: Int, nIntervals: Int
  ): Array[Array[Double]] = {

    if (nIntervals <= 0) {
      return null
    }

    val begIdx1 = if (fromIdx < 0) 0 else fromIdx

    val maxmin1 = maxmin(values, begIdx1, toIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    probMass(values, weights, begIdx1, toIdx, max, min, nIntervals)
  }

  /**
   * Probability Density Function
   */
  def probMass(values: ArrayList[Double], fromIdx: Int, toIdx: Int, interval: Double): Array[Array[Double]] = probMass(values.toArray, fromIdx, toIdx, interval)
  def probMass(values: Array[Double],
               fromIdx: Int, toIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    probMass(values, null, fromIdx, toIdx, interval)
  }

  /**
   * Probability Mass Function
   */
  def probMass(values: ArrayList[Double], weights: ArrayList[Double], fromIdx: Int, toIdx: Int, interval: Double): Array[Array[Double]] = probMass(values.toArray, weights.toArray, fromIdx, toIdx, interval)
  def probMass(values: Array[Double], weights: Array[Double],
               fromIdx: Int, toIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    if (interval <= 0) {
      return null
    }

    val begIdx1 = if (fromIdx < 0) 0 else fromIdx

    val maxmin1 = maxmin(values, begIdx1, toIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    val nIntervals = (((max - min) / interval) + 1).toInt
    probMass(values, weights, begIdx1, toIdx, max, min, nIntervals)
  }

  /**
   * Probability Mass Function
   */
  private def probMass(values: ArrayList[Double], weights: ArrayList[Double], fromIdx: Int, toIdx: Int, max: Double, min: Double, nIntervals: Int): Array[Array[Double]] = probMass(values.toArray, weights.toArray, fromIdx, toIdx, max, min, nIntervals)
  private def probMass(values: Array[Double], weights: Array[Double],
                       fromIdx: Int, toIdx: Int, max: Double, min: Double, nIntervals: Int
  ): Array[Array[Double]] = {

    if (nIntervals <= 0) {
      return null
    }

    val begIdx1 = if (fromIdx < 0) 0 else fromIdx

    val interval = (max - min) / ((nIntervals - 1) * 1.0)
    val mass = Array.ofDim[Double](2, nIntervals)
    var i = 0
    while (i < nIntervals) {
      mass(VALUE)(i) = min + i * interval
      mass(MASS)(i) = 0.0
      i += 1
    }

    val lastIdx = math.min(toIdx, values.size - 1)
    var total = 0.0
    i = begIdx1
    while (i <= lastIdx) {
      val value = values(i)
      val weight = if (weights == null) 1.0 else weights(i)
      if (value >= min && value <= max) {
        /** only calculate those between max and min */
        val densityIdx = ((value - min) / interval).toInt
        mass(MASS)(densityIdx) += weight
      }

      total += weight
      i += 1
    }

    mass(MASS) map {_ / total}

    mass
  }

  /**
   * Probability Density Function
   */
  def probMassWithTimeInfo(values: ArrayList[Double], weights: ArrayList[Double], fromIdx: Int, toIdx: Int, interval: Double): Array[Array[Double]] = probMassWithTimeInfo(values.toArray, weights.toArray, fromIdx, toIdx, interval)
  def probMassWithTimeInfo(values: Array[Double], weights: Array[Double],
                           fromIdx: Int, toIdx: Int, interval: Double
  ): Array[Array[Double]] = {

    if (fromIdx < 0 || interval <= 0) {
      return null
    }

    val maxmin1 = maxmin(values, fromIdx, toIdx)
    val max = maxmin1(MAX)
    val min = maxmin1(MIN)
    val nIntervals = (((max - min) / interval) + 1).toInt
    val period1 = period(fromIdx, toIdx).toDouble
    val mass = Array.ofDim[Double](2, nIntervals)
    var i = 0
    while (i < nIntervals) {
      mass(VALUE)(i) = min + i * interval
      mass(MASS) (i) = 0.0
      i += 1
    }

    val lastIdx = math.min(toIdx, values.length - 1)
    var total = 0.0
    i = fromIdx
    while (i <= lastIdx) {
      val value = values(i)
      val weight = if (weights == null) 1f else weights(i)
      if (value >= min && value <= max) {
        /** only calculate those between max and min */
        val densityIdx = ((value - min) / interval).toInt
        mass(MASS)(densityIdx) += weight
      }

      total += weight
      i += 1
    }

    mass(MASS) map {_ / total}

    mass
  }
  
  private def period(fromIdx: Int, toIdx: Int): Int = {
    toIdx - fromIdx + 1
  }

  private def lookback(idx: Int, period: Int): Int = {
    idx - period + 1
  }
}

