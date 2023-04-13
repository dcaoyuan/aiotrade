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
package org.aiotrade.lib.indicator.basic

import org.aiotrade.lib.indicator.Indicator

/**
 *
 * @author Caoyuan Deng
 */
class DMIIndicator extends Indicator {
  sname = "DMI"
  lname = "Directional Movement Index"
  grids = Array(20, 80)

  val periodDi  = Factor("Period DI",  6)
  val periodAdx = Factor("Period ADX", 14)
    
  val diPlus  = TVar[Double]("+DI",  Plot.Line)
  val diMinus = TVar[Double]("-DI",  Plot.Line)
  val adx     = TVar[Double]("ADX",  Plot.Line)
  val adxr    = TVar[Double]("ADXR", Plot.Line)
    
  protected def compute(fromIdx: Int, size: Int) {
    var i = fromIdx
    while (i < size) {
      diPlus (i) = diPlus( i, periodDi)
      diMinus(i) = diMinus(i, periodDi)
      adx    (i) = adx(    i, periodDi, periodAdx)
      adxr   (i) = adxr(   i, periodDi, periodAdx)

      i += 1
    }
  }
    
}

