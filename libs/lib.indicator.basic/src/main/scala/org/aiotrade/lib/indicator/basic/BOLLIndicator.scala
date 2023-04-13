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
class BOLLIndicator extends Indicator {
  sname = "BOLL"
  lname = "Bollinger Bands"
  isOverlapping = true

    
  val period = Factor("Period", 20)
  val alpha1 = Factor("Alpha1", 2.0, 0.1)
  val alpha2 = Factor("Alpha2", 2.0, 0.1)
    
  val boll_m  = TVar[Double]("MA",    Plot.Line)
  val boll_u1 = TVar[Double]("UPPER", Plot.Line)
  val boll_l1 = TVar[Double]("LOWER", Plot.Line)
  val boll_u2 = TVar[Double]("UPPER", Plot.Line)
  val boll_l2 = TVar[Double]("LOWER", Plot.Line)
    
  protected def compute(fromIdx: Int, size: Int) {
    var i = fromIdx
    while (i < size) {
      boll_m (i) = bollMiddle(i, C, period, alpha1)
      boll_u1(i) = bollUpper (i, C, period, alpha1)
      boll_l1(i) = bollLower (i, C, period, alpha1)
      boll_u2(i) = bollUpper (i, C, period, alpha2)
      boll_l2(i) = bollLower (i, C, period, alpha2)

      i += 1
    }
  }
    
    
}



