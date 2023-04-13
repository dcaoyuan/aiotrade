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

import org.aiotrade.lib.math.timeseries.TVal
import ru.circumflex.orm._

/**
 * 
 * @author Caoyuan Deng
 */
final class Execution extends BelongsToSec with TVal {
  import Execution._

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  var price:  Double = _
  var volume: Double = _
  private var _amount: Double = _
  def amount = if (_amount != 0) _amount else price * volume 
  def amount_=(amount: Double) {
    _amount = amount
  }

  var flag: Int = _ // @Note jdbc type of TINYINT is Int

  even_!
  same_!

  def even_? : Boolean = (flag & MaskEven) == MaskEven
  def in_?   : Boolean = (flag & MaskIn) == MaskIn
  def out_?  : Boolean = (flag & MaskOut) == MaskOut
  def even_! {flag = (((flag | MaskEven) & ~MaskIn) & ~MaskOut)}
  def out_!  {flag = (((flag | MaskOut) & ~MaskIn) & ~MaskEven)}
  def in_!   {flag = (((flag | MaskIn) & ~MaskOut) & ~MaskEven)}

  def same_? : Boolean = (flag & MaskSame) == MaskSame
  def up_?   : Boolean = (flag & MaskUp) == MaskUp
  def down_? : Boolean = (flag & MaskDown) == MaskDown
  def same_! {flag = (((flag | MaskSame) & ~MaskDown) & ~MaskUp)}
  def up_!   {flag = (((flag | MaskUp) & ~MaskDown) & ~MaskSame)}
  def down_! {flag = (((flag | MaskDown) & ~MaskUp) & ~MaskSame)}
  
  def setDirection(prevPrice: Double, prevDepth: MarketDepth) {
    // directionA
    if (price > prevPrice) {
      up_!
    } else if (price < prevPrice) {
      down_!
    } else {
      same_!
    }
    
    // directionB
    if (prevDepth.depth > 0) {
      val bidPrice0 = prevDepth.bidPrice(0)
      val askPrice0 = prevDepth.askPrice(0)

      /**
       * If the price is up to limit or down to limit, the ask price or bid price will be 0,
       * so we need to judge the askPrice0 or bidPrice0 whether is 0 or not.
       * The price is always more than 0, so the expression "price <= askPrice0" equals "price <= askPrice0 && price > 0", then
       * we needn't judge the askPrice or bidPrice whether is 0 or not.
       * If our code is:
       * if (price >= bidPrice0) {// we need judge the bidPrice0 whether is 0 or not.
       *    in_!
       * } else if (price <= askPrice0) { // we needn't judge the askPrice0 whether is 0 or not.
       *    out_!
       * }
       * ...
       */
      if (price <= askPrice0) {
        out_!
      } else if (price >= bidPrice0) {
        in_!
      } else {
        even_!
      }
      
      
//      if (price >= bidPrice0 && bidPrice0 != 0) {
//        in_!
//      } else if (price <= askPrice0 && askPrice0 != 0) {
//        out_!
//      } else {
//        even_!
//      }
    }
  }
  
  def directionA: Int = if (up_?) 1 else if (down_?) -1 else 0
  def directionB: Int = if (in_?) 1 else if (out_?)  -1 else 0
  
  override def toString = {
    "Execution(" + "price=" + price + ", volume=" + volume + ", amount=" + amount + ", directionA=" + directionA + ", directionB=" + directionB  + ")"
  }
}

object Execution {
  // bit masks for flag
  val MaskEven          = 1 << 0   //    000...00000001
  val MaskIn            = 1 << 1   //    000...00000010
  val MaskOut           = 1 << 2   //    000...00000100
  val MaskSame          = 1 << 3   //    000...00001000
  val MaskUp            = 1 << 4   //    000...00010000
  val MaskDown          = 1 << 5   //    000...00100000
  private val flagbit3  = 1 << 6   //    000...01000000
  private val flagbit4  = 1 << 7   //    000...10000000
}

object Executions extends Table[Execution] {
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()

  val price  = "price"  DOUBLE()
  val volume = "volume" DOUBLE()
  val amount = "amount" DOUBLE()

  val flag = "flag" TINYINT() // @Note jdbc type of TINYINT is Int

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(time.name)

  private val ONE_DAY = 24 * 60 * 60 * 1000

  def executionsOf(sec: Sec, dailyRoundedTime: Long): Seq[Execution] = {
    SELECT (this.*) FROM (this) WHERE (
      (this.sec.field EQ Secs.idOf(sec)) AND (this.time BETWEEN (dailyRoundedTime, dailyRoundedTime + ONE_DAY - 1))
    ) ORDER_BY (this.time) list
  }
}

