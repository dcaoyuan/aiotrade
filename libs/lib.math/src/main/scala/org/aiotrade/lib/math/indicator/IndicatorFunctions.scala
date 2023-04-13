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
package org.aiotrade.lib.math.indicator

import org.aiotrade.lib.math.StatsFunctions
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.collection.ArrayList

/**
 *
 * @author Caoyuan Deng
 */
object IndicatorFunctions {
    
  def dmPlus(idx: Int, highs: ArrayList[Double], lows: ArrayList[Double]): Double = {
    if (idx == 0) {
            
      Null.Double
            
    } else {
            
      if (highs(idx) > highs(idx - 1) && lows(idx) > lows(idx - 1)) {
        highs(idx) - highs(idx - 1)
      } else if (highs(idx) < highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        0f
      } else if (highs(idx) > highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        if (highs(idx) - highs(idx - 1) > lows(idx - 1) - lows(idx)) {
          highs(idx) - highs(idx - 1)
        } else {
          0f
        }
      } else if (highs(idx) <  highs(idx - 1) && lows(idx) >  lows(idx - 1)) {
        0f
      } else if (highs(idx) == highs(idx - 1) && lows(idx) == lows(idx - 1)) {
        0f
      } else if (lows(idx) > highs(idx - 1)) {
        highs(idx) - highs(idx)
      } else if (highs(idx) < lows(idx - 1)) {
        0f
      } else {
        0f
      }
            
    }
  }
    
  def dmMinus(idx: Int, highs: ArrayList[Double], lows: ArrayList[Double]): Double = {
    if (idx == 0) {
            
      Null.Double
            
    } else {
            
      if (highs(idx) > highs(idx - 1) && lows(idx) > lows(idx - 1)) {
        0f
      } else if (highs(idx) < highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        lows(idx - 1) - lows(idx)
      } else if (highs(idx) > highs(idx - 1) && lows(idx) < lows(idx - 1)) {
        if (highs(idx) - highs(idx - 1) > lows(idx - 1) - lows(idx)) {
          0f
        } else {
          lows(idx - 1) - lows(idx)
        }
      } else if (highs(idx) <  highs(idx - 1) && lows(idx) >  lows(idx - 1)) {
        0f
      } else if (highs(idx) == highs(idx - 1) && lows(idx) == lows(idx - 1)) {
        0f
      } else if (lows(idx) > highs(idx - 1)) {
        0f
      } else if (highs(idx) < lows(idx - 1)) {
        lows(idx - 1) - lows(idx)
      } else {
        0f
      }
            
    }
  }
    
  def tr(idx: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx == 0) {
            
      Null.Double
            
    } else {
            
      val tr_tmp = math.max(highs(idx) - lows(idx), math.abs(highs(idx) - closes(idx - 1)))
      math.max(tr_tmp, math.abs(lows(idx) - closes(idx - 1)))
            
    }
  }
    
  def diPlus(idx: Int, period: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx < period - 1) {
            
      Null.Double
            
    } else {
            
      val dms = new ArrayList[Double]
      val trs = new ArrayList[Double]
            
      val fromIdx = idx - (period - 1)
      val toIdx   = idx

      var i = fromIdx
      while (i <= toIdx) {
                
        dms += dmPlus(i, highs, lows)
        trs += tr(i, highs, lows, closes)

        i += 1
      }
            
      val ma_dm = StatsFunctions.ma(dms, 0, period - 1)
      val ma_tr = StatsFunctions.ma(trs, 0, period - 1)
            
      if (ma_tr == 0) 0 else ma_dm / ma_tr * 100f
            
    }
  }
    
  def diMinus(idx: Int, period: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx < period - 1) {
            
      Null.Double
            
    } else {
            
      val dms = new ArrayList[Double]
      val trs = new ArrayList[Double]
            
      val fromIdx = idx - (period - 1)
      val toIdx   = idx
            
      var i = fromIdx
      while (i <= toIdx) {
                
        dms += dmMinus(i, highs, lows)
        trs += tr(i, highs, lows, closes)

        i += 1
      }
            
      val ma_dm = StatsFunctions.ma(dms, 0, period - 1)
      val ma_tr = StatsFunctions.ma(trs, 0, period - 1)
            
      if (ma_tr == 0) 0 else ma_dm / ma_tr * 100f
            
    }
  }
    
  def dx(idx: Int, period: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx < period - 1) {
            
      Null.Double
            
    } else {
            
      val diPlus1  = diPlus( idx, period, highs, lows, closes)
      val diMinus1 = diMinus(idx, period, highs, lows, closes)
            
      if (diPlus1 + diMinus1 == 0) 0 else math.abs(diPlus1 - diMinus1) / (diPlus1 + diMinus1) * 100f
            
    }
  }
    
  def adx(idx: Int, periodDI: Int, periodADX: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx < periodDI - 1 || idx < periodADX - 1) {
            
      Null.Double
            
    } else {
            
      val dxes = new ArrayList[Double]
            
      val fromIdx = idx - (periodADX - 1)
      val toIdx   = idx
            
      var i = fromIdx
      while (i <= toIdx) {
                
        dxes += dx(i, periodDI, highs, lows, closes)

        i += 1
      }
            
      StatsFunctions.ma(dxes, 0, periodADX - 1)
    }
  }
    
  def adxr(idx: Int, periodDI: Int, periodADX: Int, highs: ArrayList[Double], lows: ArrayList[Double], closes: ArrayList[Double]): Double = {
    if (idx < periodDI - 1 || idx < periodADX - 1) {
            
      Null.Double
            
    } else {
            
      val adx1 = adx(idx,             periodDI, periodADX, highs, lows, closes)
      val adx2 = adx(idx - periodADX, periodDI, periodADX, highs, lows, closes)
            
      (adx1 + adx2) / 2f
            
    }
  }
    
}


