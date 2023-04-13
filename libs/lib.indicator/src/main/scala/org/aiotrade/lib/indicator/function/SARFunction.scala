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
package org.aiotrade.lib.indicator.function

import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.indicator.Factor

/**
 *
 * @author Caoyuan Deng
 */
class SARFunction extends Function {
    
  var initial, step, maximum: Factor = _
    
  val _side = TVar[Side]()
  val _ep   = TVar[Double]()
  val _af   = TVar[Double]()
    
  val _sar = TVar[Double]()
    
  override def set(baseSer: BaseTSer, args: Any*): Unit = {
    super.set(baseSer)
        
    this.initial = args(0).asInstanceOf[Factor]
    this.step = args(1).asInstanceOf[Factor]
    this.maximum = args(2).asInstanceOf[Factor]
  }
    
  protected def computeSpot(i: Int): Unit = {
    if (i == 0) {
            
      _side(i) = Side.EnterLong
            
      val currLow = L(i)
      _sar(i) = currLow
            
      _af(i) = initial.value
            
      val currHigh = H(i)
      _ep(i) = currHigh
            
    } else {
            
      if (_side(i - 1) == Side.EnterLong) {
        /** in long-term */
                
        val currHigh = H(i)
        val prevHigh = H(i - 1)
                
        if (currHigh > _ep(i - 1)) {
          /** new high, acceleration adds 'step' each day, till 'maximum' */
          _af(i) = math.min(_af(i - 1) + step.value, maximum.value)
          _ep(i) = currHigh
        } else {
          /** keep same acceleration */
          _af(i) = _af(i - 1)
          _ep(i) = _ep(i - 1)
        }
        _sar(i) = _sar(i - 1) + _af(i) * (prevHigh - _sar(i - 1))
                
        if (_sar(i) >= currHigh) {
          /** turn to short-term */
                    
          _side(i) = Side.ExitLong
                    
          _sar(i) = currHigh
                    
          _af(i) = initial.value
          _ep(i) = L(i)
                    
        } else {
          /** still in long-term */
                    
          _side(i) = Side.EnterLong
        }
                
      } else {
        /** in short-term */
                
        val currLow = L(i)
        val prevLow = L(i - 1)
                
        if (currLow < _ep(i - 1)) {
          _af(i) = math.min(_af(i - 1) + step.value, maximum.value)
          _ep(i) = currLow
        } else {
          _af(i) = _af(i - 1)
          _ep(i) = _ep(i - 1)
        }
        _sar(i) = _sar(i - 1) + _af(i) * (prevLow - _sar(i - 1))
                
        if (_sar(i) <= currLow) {
          /** turn to long-term */
                    
          _side(i) = Side.EnterLong
                    
          _sar(i) = currLow
                    
          _af(i) = initial.value
          _ep(i) = H(i)
                    
        } else {
          /** still in short-term */
                    
          _side(i) = Side.ExitLong
        }
      }
            
    }
  }
    
  def sar(sessionId: Long, idx: Int): Double = {
    computeTo(sessionId, idx)
        
    _sar(idx)
  }
    
  def sarSide(sessionId: Long, idx: Int): Side = {
    computeTo(sessionId, idx)
        
    _side(idx)
  }
}




