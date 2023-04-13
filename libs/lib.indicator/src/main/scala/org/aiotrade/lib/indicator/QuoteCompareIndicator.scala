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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.securities.QuoteSer


/**
 *
 * @author Caoyuan Deng
 */
class QuoteCompareIndicator($baseSer: BaseTSer) extends Indicator($baseSer) {
    
  var serToBeCompared: QuoteSer = _
    
  val begPosition = Factor("Begin of Time Frame", 0L)
  val endPosition = Factor("End of Time Frame",   0L)
  val maxValue    = Factor("Max Value", Double.MinValue)
  val minValue    = Factor("Min Value", Double.MaxValue)
    
  var open   = TVar[Double]("O", Plot.Quote)
  var high   = TVar[Double]("H", Plot.Quote)
  var low    = TVar[Double]("L", Plot.Quote)
  var close  = TVar[Double]("C", Plot.Quote)
  var volume = TVar[Double]("V", Plot.Volume)
    
  protected def compute(begIdx: Int, size: Int) {
    /** camparing base point is the value of begin time (the most left on screen */
        
    /** always compute from most left position on screen */
    val begPos = begPosition.value.toInt//math.min((int)begPosition.value(), begIdx);
    val endPos = endPosition.value.toInt//math.min((int)endPosition.value(),   _dataSize - 1);

    val baseQSer = baseSer.asInstanceOf[QuoteSer]
    /** get first value of baseSer in time frame, it will be the comparing base point */
    var baseNorm = Null.Double
    var row = begPosition.value.toInt
    var end = endPosition.value.toInt
    var break = false
    while (row <= end & !break) {
      val baseTime = baseQSer.timeOfRow(row)
      if (baseQSer.exists(baseTime)) {
        baseNorm = baseQSer.close(baseTime)
        break = true
      }
            
      row += 1
    }
        
    if (Null.is(baseNorm)) {
      return
    }
        
    if (baseSer.asInstanceOf[QuoteSer].isAdjusted) {
      if (!serToBeCompared.isAdjusted) {
        serToBeCompared.adjust()
      }
    } else {
      if (serToBeCompared.isAdjusted) {
        serToBeCompared.unadjust()
      }
    }
        
    var compareNorm = Null.Double
    /**
     * !NOTICE
     * we only calculate this indicator's value for a timeSet showing in screen,
     * instead of all over the time frame of baseSer, thus, we use
     * this time set for loop instead of the usaully usage in other indicators:
     *        for (int i = fromIndex; i < size(); i++) {
     *            ....
     *        }
     *
     * Think about it, when the baseSer updated, we should re-calculate
     * all Ser instead from fromIndex.
     */
    var i = begPos
    while (i <= endPos) {
      if (i < begPosition.value) {
        /** don't calulate those is less than beginPosition to got a proper compareBeginValue */
      } else {
            
        val time = baseSer.asInstanceOf[BaseTSer].timeOfRow(i)
            
        /**
         * !NOTICE:
         * we should fetch serToBeCompared by time instead by position which may
         * not sync with baseSer.
         */
        val compareQSer = serToBeCompared.asInstanceOf[QuoteSer]
        if (compareQSer.exists(time)) {
            /** get first value of serToBeCompared in time frame */
            if (Null.is(compareNorm)) {
              compareNorm = compareQSer.close(time)
            }
                        
            if (exists(time)) {
              open(time)  = linearAdjust(compareQSer.open(time),  compareNorm, baseNorm)
              high(time)  = linearAdjust(compareQSer.high(time),  compareNorm, baseNorm)
              low(time)   = linearAdjust(compareQSer.low(time),   compareNorm, baseNorm)
              close(time) = linearAdjust(compareQSer.close(time), compareNorm, baseNorm)
            }
        }
      }
            
      i += 1
    }
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

}
