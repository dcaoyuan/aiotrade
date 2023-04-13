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

import java.util.logging.Logger
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.DefaultBaseTSer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Reactions
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
class QuoteSer(_sec: Sec, _freq: TFreq) extends DefaultBaseTSer(_sec, _freq) with WithFreeFloat  {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _shortName: String = _sec.uniSymbol
  private var _isAdjusted: Boolean = false
  
  override 
  def serProvider: Sec = super.serProvider.asInstanceOf[Sec]

  val open = TVar[Double]("O", Plot.Quote)
  val high = TVar[Double]("H", Plot.Quote)
  val low = TVar[Double]("L", Plot.Quote)
  val close = TVar[Double]("C", Plot.Quote)
  val volume = TVar[Double]("V", Plot.Volume)
  val amount = TVar[Double]("A", Plot.Volume)
  val average = TVar[Double]("R", Plot.None)
  val prevClose = TVar[Double]("PC", Plot.None)
  val prev5Close = TVar[Double]("P5C", Plot.None)
  val execCount = TVar[Double]("E", Plot.None)
  val turnoverRate = TVar[Double]("T", Plot.None)
  val lastModify = TVar[Long]("LM", Plot.None)
    
  // unadjusted values
  val open_ori = TVar[Double]("O")
  val high_ori = TVar[Double]("H")
  val low_ori = TVar[Double]("L")
  val close_ori = TVar[Double]("C")
  val average_ori = TVar[Double]("R")

  val isClosed = TVar[Boolean]()
  
  override 
  val exportableVars = List(open_ori, high_ori, low_ori, close_ori, volume, amount, prevClose, prev5Close, execCount, turnoverRate, lastModify)

  override 
  protected def assignValue(tval: TVal) {
    super.assignValue(tval)
    val time = tval.time
    tval match {
      case quote: Quote =>
        open(time) = quote.open
        high(time) = quote.high
        low(time) = quote.low
        close(time) = quote.close
        volume(time) = quote.volume
        amount(time) = quote.amount
        average(time) = quote.average
        prevClose(time) = quote.prevClose
        execCount(time) = quote.execCount
        turnoverRate(time) = quote.volume / freeFloat(time)
        lastModify(time) = quote.lastModify

        open_ori(time) = quote.open
        high_ori(time) = quote.high
        low_ori(time) = quote.low
        close_ori(time) = quote.close
        average_ori(time) = quote.average

        isClosed(time) = quote.closed_?

        val idx = timestamps.nearestIndexOfOccurredTime(time) - 5
        prev5Close(time) = if (idx >= 0) prevClose(idx) else prevClose(0)
      case _ => assert(false, "Should pass a Quote type TimeValue")
    }

  }

  def valueOf(time: Long): Option[Quote] = {
    if (exists(time)) {
      val quote = new Quote
      
      quote.time = time
      quote.open = open(time)
      quote.high = high(time)
      quote.low = low(time)
      quote.close = close(time)
      quote.volume = volume(time)
      quote.amount = amount(time)
      quote.average = average(time)
      quote.prevClose = prevClose(time)
      quote.execCount = execCount(time)
      quote.turnoverRate = turnoverRate(time)
      quote.lastModify = lastModify(time)
      if (isClosed(time)) quote.closed_! else quote.unclosed_!
      
      Some(quote)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(quote: Quote) {
    val time = quote.time
    createOrReset(time)

    assignValue(quote)

    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, quote.uniSymbol, time, time))
  }

  def isAdjusted = _isAdjusted
  def isAdjusted_=(b: Boolean) {
    _isAdjusted = b
  }
  
  def adjust  (force: Boolean = false) {doAdjust(true,  force)}
  def unadjust(force: Boolean = false) {doAdjust(false, force)}
  
  private def doAdjust(b: Boolean, force: Boolean) {
    if (isLoaded) {
      doAdjusting(b, force)
    } else {
      // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
      var reaction: Reactions.Reaction = null
      reaction = {
        case TSerEvent.Loaded(ser: QuoteSer, _, _, _, _, _) if ser eq this =>
          reactions -= reaction
          doAdjusting(b, force)
      }
      reactions += reaction

      // TSerEvent.Loaded may have been missed during above procedure, so confirm it
      if (isLoaded) {
        reactions -= reaction
        doAdjusting(b, force)
      }
    }
  }

  /**
   * @param boolean b: if true, do adjust, else, de adjust
   */
  private def doAdjusting(b: Boolean, force: Boolean) {
    if (!force && (isAdjusted && b || !isAdjusted && !b)) return
    
    val divs = Exchanges.dividendsOf(serProvider)
    if (divs.isEmpty) {
      isAdjusted = b
      return
    }
    
    var i = -1
    while ({i += 1; i < size}) {
      val time = timestamps(i)

      var h = high_ori(i)
      var l = low_ori(i)
      var o = open_ori(i)
      var c = close_ori(i)
      var r = average_ori(i)

      if (b) {
        val divItr = divs.iterator
        while (divItr.hasNext) {
          val div = divItr.next
          if (time < div.dividendDate) {
            h = div.adjust(h)
            l = div.adjust(l)
            o = div.adjust(o)
            c = div.adjust(c)
            r = div.adjust(r)
          }
        }
      }
      
      high   (i) = h
      low    (i) = l
      open   (i) = o
      close  (i) = c
      average(i) = r
    }

    isAdjusted = b
    
    log.info(serProvider.uniSymbol + (if (isAdjusted) " adjusted." else " unadjusted."))

    publish(TSerEvent.Updated(this, null, 0, lastOccurredTime))
  }

  def doCalcRate{
    if (isLoaded) {
      calcRateByFreeFloat(turnoverRate, volume)
      calPreClose
    } else {
      // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
      var reaction: Reactions.Reaction = null
      reaction = {
        case TSerEvent.Loaded(ser: QuoteSer, _, _, _, _, _) if ser eq this =>
          reactions -= reaction
          calcRateByFreeFloat(turnoverRate, volume)
          calPreClose
      }
      reactions += reaction

      // TSerEvent.Loaded may have been missed during above procedure, so confirm it
      if (isLoaded) {
        reactions -= reaction
        calcRateByFreeFloat(turnoverRate, volume)
        calPreClose
      }
    }
  }
  
  private def calPreClose {
    if (prevClose(0) == 0) prevClose(0) = open(0)
    
    val divs = Exchanges.dividendsOf(serProvider)
    if (divs.isEmpty) {
      var i = 0
      while ({i += 1; i < size}) {
        if (prevClose(i) == 0) {
          prevClose(i) = close_ori(i - 1)
        }
      }
    } else {
      var i = 0
      while ({i += 1; i < size}) {
        if (prevClose(i) == 0) {
          prevClose(i) = close_ori(i - 1)
          val time = TFreq.DAILY.round(timestamps(i), java.util.Calendar.getInstance)
          val divItr = divs.iterator
          while (divItr.hasNext) {
            val div = divItr.next
            if (time == div.dividendDate) {
              prevClose(i) = div.adjust(prevClose(i))
            }
          }
        }
      }
    }
    
  }

  override 
  def shortName: String = _shortName + {if (isAdjusted) "(*)" else ""}

}
