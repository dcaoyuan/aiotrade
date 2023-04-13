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
package org.aiotrade.lib.securities

import java.util.Calendar
import java.util.TimeZone
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.util.actors.Reactor

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSerJointer(srcSers: Map[QuoteSer, Double], targetSer: QuoteSer, timeZone: TimeZone) extends Reactor {

  reactions += {
    case TSerEvent.Loaded(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Computed(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Updated(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Cleared(_, _, fromTime, _, _, _) => compute(fromTime)
  }

  srcSers foreach (x => listenTo(x._1))

  var serToBaseNorm = srcSers map {case (ser, weight) =>
      if (ser.size > 0) {
        val time = ser.timeOfIndex(0)
        (ser, ser.valueOf(time))
      } else (ser, None)
  } toMap

  def computeFrom(fromTime: Long) {
    compute(fromTime)
  }
    
  /**
   * Combine data according to wanted frequency, such as Weekly, Monthly etc.
   */
  protected def compute(fromTime: Long) {
    val targetFreq = targetSer.freq
    val targetUnit = targetFreq.unit

    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(fromTime)
    val masterFromTime = targetUnit.round(cal)
        
    /** begin summing: */

    val now = System.currentTimeMillis
    var time_i = masterFromTime
    while (time_i < now) {
      targetSer.createOrReset(time_i)
      for ((srcSer, weight) <- srcSers if srcSer.exists(time_i)) {
        targetSer.open(time_i)   = targetSer.open(time_i) + (srcSer.open(time_i) / serToBaseNorm(srcSer).get.open) * weight
        targetSer.high(time_i)   = targetSer.high(time_i) + (srcSer.high(time_i) / serToBaseNorm(srcSer).get.high) * weight
        targetSer.low (time_i)   = targetSer.low (time_i) + (srcSer.low (time_i) / serToBaseNorm(srcSer).get.low)  * weight
        targetSer.volume(time_i) = targetSer.volume(time_i) + srcSer.volume(time_i)
        targetSer.amount(time_i) = targetSer.amount(time_i) + srcSer.amount(time_i)
        targetSer.execCount(time_i) = targetSer.execCount(time_i) + srcSer.execCount(time_i)
      }
      
      time_i = targetFreq.nextTime(time_i)
    }
        
    val evt = TSerEvent.Updated(targetSer, null, masterFromTime, targetSer.lastOccurredTime)
    targetSer.publish(evt)
  }
    
  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }
    
  def dispose {}
}
