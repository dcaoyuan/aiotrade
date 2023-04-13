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
package org.aiotrade.lib.securities

import java.util.Calendar
import java.util.TimeZone
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable.WeakHashMap

/**
 * @Note to get this Combiner react to srcSer, it should be held as strong ref by some instances
 *
 * @todo 5 min quotes in 9:25 will appears long quote line.
 * @author Caoyuan Deng
 */
class QuoteSerCombiner(srcSer: QuoteSer, tarSer: QuoteSer, timeZone: TimeZone) extends Reactor {
  import QuoteSerCombiner._

  private val log = Logger.getLogger(this.getClass.getName)

  strongRefHolders.put(tarSer, this)
  
  reactions += {
    case TSerEvent.Loaded(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Computed(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Updated(_, _, fromTime, _, _, _) => compute(fromTime)
    case TSerEvent.Cleared(_, _, fromTime, _, _, _) => compute(fromTime)
  }
  listenTo(srcSer)

  private val sec = srcSer.serProvider.asInstanceOf[Sec]
  private val freq = tarSer.freq
  private var quote: Quote = _

  /**
   * Combine data according to wanted frequency, such as Weekly, Monthly etc.
   */
  def computeFrom_old(fromTime: Long) {
    val tarUnit = freq.unit

    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(fromTime)
    val masterFromTime = freq.round(fromTime, cal)
    val masterFromIdx1 = srcSer.timestamps.indexOrNextIndexOfOccurredTime(masterFromTime)
    val masterFromIdx = if (masterFromIdx1 < 0) 0 else masterFromIdx1

    //targetQuoteSer.clear(myFromTime);
        
    // --- begin combining
                
    val n = srcSer.size
    var i = masterFromIdx
    while (i < n) {
      val time_i = srcSer.timeOfIndex(i)
      if (time_i >= masterFromTime) {
        val intervalBegin = tarUnit.beginTimeOfUnitThatInclude(time_i, cal)
            
        tarSer.createOrReset(intervalBegin)
            
        var prevNorm = srcSer.close(time_i)
        var postNorm = srcSer.close(time_i) //srcSer.close_adj(time_i)
            
        tarSer.open(intervalBegin)   = linearAdjust(srcSer.open(time_i),  prevNorm, postNorm)
        tarSer.high(intervalBegin)   = Double.MinValue
        tarSer.low(intervalBegin)    = Double.MaxValue
        tarSer.volume(intervalBegin) = 0
        tarSer.amount(intervalBegin) = 0
        tarSer.execCount(intervalBegin) = 0
            
        /** compose followed source data of this interval to targetData */
        var j = 0
        var break = false
        while (i + j < n && !break) {
          val time_j = srcSer.timeOfIndex(i + j)

          /**
           * @NOTICE
           * There is strange behave of JDK's Calendar when a very big Long
           * timeInMillis assigned to it. so anyway, we let inSamePeriod = true
           * if j == 0, because jdata is the same data as idata in this case:
           */
          val inSameInterval = if (j == 0) true else {
            freq.sameInterval(time_i, time_j, cal)
          }
        
          if (inSameInterval) {
            /**
             * @TIPS
             * when combine, do adjust on source's value, then de adjust on combined quote data.
             * this will prevent bad high, open, and low into combined quote data:
             *
             * During the combining period, an adjust may happened, but we only record last
             * close_adj, the high, low, and open of the data before adjusted acutally may has
             * different scale close_adj, so must do adjust with its own close_adj firstly. then
             * use the last close_orj to de-adjust it.
             */
            prevNorm = srcSer.close(time_j)
            postNorm = srcSer.close(time_j) //srcSer.close_adj(time_j)

            tarSer.high(intervalBegin)  = math.max(tarSer.high(intervalBegin), linearAdjust(srcSer.high(time_j),  prevNorm, postNorm))
            tarSer.low(intervalBegin)   = math.min(tarSer.low(intervalBegin),  linearAdjust(srcSer.low(time_j),   prevNorm, postNorm))
            tarSer.close(intervalBegin) = linearAdjust(srcSer.close(time_j),   prevNorm, postNorm)

            tarSer.volume(intervalBegin) = tarSer.volume(intervalBegin) + srcSer.volume(time_j)
            tarSer.amount(intervalBegin) = tarSer.amount(intervalBegin) + srcSer.amount(time_j)
            tarSer.execCount(intervalBegin) = tarSer.execCount(intervalBegin) + srcSer.execCount(time_j)

            tarSer.close_ori(intervalBegin) = srcSer.close_ori(time_j)
            //tarSer.close_adj(intervalBegin) = srcSer.close_adj(time_j)

            j += 1
          } else {
            break = true
          }
        }
            
        /** de adjust on combined quote data */
            
        prevNorm = tarSer.close(intervalBegin)
        postNorm = tarSer.close_ori(intervalBegin)
            
        tarSer.high(intervalBegin)  = linearAdjust(tarSer.high(intervalBegin),  prevNorm, postNorm)
        tarSer.low(intervalBegin)   = linearAdjust(tarSer.low(intervalBegin),   prevNorm, postNorm)
        tarSer.open(intervalBegin)  = linearAdjust(tarSer.open(intervalBegin),  prevNorm, postNorm)
        tarSer.close(intervalBegin) = linearAdjust(tarSer.close(intervalBegin), prevNorm, postNorm)

        i += j
      } else {
        i += 1
      }
    }

    val evt = TSerEvent.Updated(tarSer, null, masterFromTime, tarSer.lastOccurredTime)
    tarSer.publish(evt)
  }

  def compute(fromTime: Long) {
    val cal = Calendar.getInstance(timeZone)
    cal.setTimeInMillis(fromTime)
    val roundedFromTime = freq.round(fromTime, cal)
    cal.setTimeInMillis(roundedFromTime)
    val srcFromIdx = math.max(0, srcSer.timestamps.indexOrNextIndexOfOccurredTime(roundedFromTime))

    // --- begin combining

    val n = srcSer.size
    var i = srcFromIdx - 1
    while ({i += 1; i < n}) {
      val time_i = srcSer.timeOfIndex(i)
      if (time_i >= roundedFromTime) {
        val quote = quoteOf(time_i)

        var prevNorm = srcSer.close(time_i)
        var postNorm = srcSer.close(time_i)//srcSer.close_adj(time_i) @todo deal with adjusted
        
        /**
         * @TIPS
         * when combine, do adjust on source's value, then de adjust on combined quote data.
         * this will prevent bad high, open, and low into combined quote data:
         *
         * During the combining period, an adjust may happened, but we only record last
         * close_adj, the high, low, and open of the data before adjusted acutally may has
         * different scale close_adj, so must do adjust with its own close_adj firstly. then
         * use the last close_orj to de-adjust it.
         */
        if (quote.justOpen_?) {
          quote.unjustOpen_!
          quote.open   = linearAdjust(srcSer.open(time_i),  prevNorm, postNorm)
          quote.high   = linearAdjust(srcSer.high(time_i),  prevNorm, postNorm)
          quote.low    = linearAdjust(srcSer.low(time_i),   prevNorm, postNorm)
          quote.close  = linearAdjust(srcSer.close(time_i), prevNorm, postNorm)
          quote.volume += srcSer.volume(time_i)
          quote.amount += srcSer.amount(time_i)
          quote.execCount += srcSer.execCount(time_i)
        } else {
          quote.high   = math.max(quote.high, linearAdjust(srcSer.high(time_i),  prevNorm, postNorm))
          quote.low    = math.min(quote.low,  linearAdjust(srcSer.low(time_i),   prevNorm, postNorm))
          quote.close  = linearAdjust(srcSer.close(time_i), prevNorm, postNorm)
          quote.volume += srcSer.volume(time_i)
          quote.amount += srcSer.amount(time_i)
          quote.execCount += srcSer.execCount(time_i)
        }
        
        tarSer.updateFrom(quote)
      }
    }

//    val evt = TSerEvent.Updated(tarSer, null, baseFromTime, tarSer.lastOccurredTime)
//    tarSer.publish(evt)
  }

  def quoteOf(time: Long): Quote = {
    val cal = Calendar.getInstance(timeZone)
    val rounded = freq.round(time, cal)
    quote match {
      case one: Quote if one.time == rounded =>
        one
      case prevOneOrNull => // interval changes or null
        val newone = new Quote
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true

        quote = newone
        newone
    }
  }

  /**
   * This function keeps the adjusting linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }
    
  def dispose {}
}

object QuoteSerCombiner {
  // Holding strong reference to ser combiner etc, see @QuoteSerCombiner
  private val strongRefHolders = WeakHashMap[QuoteSer, QuoteSerCombiner]()
}


