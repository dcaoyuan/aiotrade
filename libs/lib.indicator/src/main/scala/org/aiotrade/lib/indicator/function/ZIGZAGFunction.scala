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
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.indicator.Factor

/**
 *
 * @author Caoyuan Deng
 */
class ZIGZAGFunction extends Function {
    
  var percent: Factor = _
    
  val _peakHi    = TVar[Double]()
  val _peakLo    = TVar[Double]()
  val _peakHiIdx = TVar[Int]()
  val _peakLoIdx = TVar[Int]()
  val _side = TVar[Side]()
    
  val _zigzag       = TVar[Double]()
  val _pseudoZigzag = TVar[Double]()
    
  override def set(baseSer: BaseTSer, args: Any*): Unit = {
    super.set(baseSer)
        
    this.percent = args(0).asInstanceOf[Factor]
  }
    
  /**
   * @TODO
   * Re-think how to effictively get this pseudoZigzag
   */
  override protected def preComputeTo(sessionId: Long, idx: Int): Unit = {
    /**
     * the last zigzag is not a real turn over point, it's just a peakLo/Hi
     * in last trend, so should clear it. and if necessary, re compute
     * from this point.
     */
    //        int lastPeakIdx = indexOfLastValidValue(pseudoZigzag);
    //        if (lastPeakIdx >= 0) {
    //            pseudoZigzag.set(lastPeakIdx, Null.Double);
    //
    //            setComputedIdx(math.min(getComputedIdx(), lastPeakIdx));
    //        }
  }
    
  override protected def postComputeTo(sessionId: Long, idx: Int): Unit = {

    val lastIdx = size - 1

    /**
     * did this computing session compute till the last data? if not, do not
     * try to compute pseudo zigzag (ie. last peakHi/Lo in current trend)
     */
    if (idx != lastIdx) {
      return
    }
        
    /** get the last zigzag as the first pseudo point */
    val lastZigzagIdx = indexOfLastValidValue(_zigzag)
    if (lastZigzagIdx >= 0) {
      _pseudoZigzag(lastZigzagIdx) = _zigzag(lastZigzagIdx)
    }
        
    /** set pseudo zigzag to the last peakHi/Lo in current trend */
    if (lastIdx >= 0) {
      if (_side(lastIdx) == Side.EnterLong) {
        val lastPeakHiIdx = _peakHiIdx(lastIdx)
        _pseudoZigzag(lastPeakHiIdx) = H(lastPeakHiIdx)
      } else {
        val lastPeakLoIdx = _peakLoIdx(lastIdx)
        _pseudoZigzag(lastPeakLoIdx) = L(lastPeakLoIdx)
      }
    }
        
  }
    
  protected def computeSpot(i: Int): Unit = {
        
    if (i == 0) {
            
      _side(i) = Side.EnterLong
      _zigzag(i) = Null.Double
      _pseudoZigzag(i) = Null.Double
      _peakHi(i) = H(i)
      _peakLo(i) = L(i)
      _peakHiIdx(i) = i
      _peakLoIdx(i) = i
            
    } else {
            
      if (_side(i - 1) == Side.EnterLong) {
                
        if ((H(i) - _peakHi(i - 1)) / _peakHi(i - 1) <= -percent.value) {
          /** turn over to short trend */
          _side(i) = Side.ExitLong
                    
          /** and we get a new zigzag peak of high at (idx - 1) */
          val newZigzagIdx = _peakHiIdx(i - 1)
          _zigzag(newZigzagIdx) = H(newZigzagIdx)
                    
          _peakLo(i) = L(i)
          _peakLoIdx(i) = i
                    
        } else {
          /** long trend goes on */
          _side(i) = _side(i - 1)
                    
          if (H(i) > _peakHi(i - 1)) {
            /** new high */
            _peakHi(i) = H(i)
            _peakHiIdx(i) = i
          } else {
            /** keep same */
            _peakHi(i) = _peakHi(i - 1)
            _peakHiIdx(i) = _peakHiIdx(i - 1)
          }
                    
        }
                
      } else {
                
        if ((L(i) - _peakLo(i - 1)) / _peakLo(i - 1) >= percent.value) {
          /** turn over to long trend */
          _side(i) = Side.EnterLong
                    
          /** and we get a new zigzag peak of low at (idx - 1) */
          val newZigzagIdx = _peakLoIdx(i - 1)
          _zigzag(newZigzagIdx) = L(newZigzagIdx)
                    
          _peakHi(i) = H(i)
          _peakHiIdx(i) = i
                    
        } else {
          /** short trend goes on */
          _side(i) = _side(i - 1)
                    
          if (L(i) < _peakLo(i - 1)) {
            /** new low */
            _peakLo(i) = L(i)
            _peakLoIdx(i) = i
          } else {
            /** keep same */
            _peakLo(i) = _peakLo(i - 1)
            _peakLoIdx(i) = _peakLoIdx(i - 1)
          }
                    
        }
                
      }
    }
        
  }
    
  def zigzag(sessionId: Long, idx: Int): Double = {
    /**
     * @NOTICE
     * as zigzag's value is decided by future (+n step) idx, we should
     * go on computing untill a turn over happened.
     */
    val size = baseSer.size
    var i = idx
    var break = false
    while (i < size && !break) {
      computeTo(sessionId, i);
      if (i > 0 && _side(i - 1) != _side(i)) {
        /** a turn over happened */
        break = true
      }
      i += 1
    }
        
    _zigzag(idx)
  }
    
  def pseudoZigzag(sessionId: Long, idx:Int): Double = {
    /**
     * @NOTICE
     * as pseudo zigzag's value is decided by future (+n step) idx, we should
     * go on computing untill a turn over happened.
     */
    val size = baseSer.size
    var i = idx
    var break = false
    while (i < size && !break) {
      computeTo(sessionId, i)
      if (i > 0 && _side(i - 1) != _side(i)) {
        /** a turn over happened */
        break = true
      }
      i += 1
    }
        
    _pseudoZigzag(idx)
  }

  def zigzagSide(sessionId: Long, idx: Int): Side = {
    /**
     * @NOTICE
     * as zigzag Side 's value is decided by future (+n step) idx, we should
     * go on computing untill a turn over happened.
     */
    val size = baseSer.size
    var i = idx
    var break = false
    while (i < size && !break) {
      computeTo(sessionId, i)
      if (i > 0 && _side(i - 1) != _side(i)) {
        /** a turn over happened */
        break = true
      }
      i += 1
    }
        
    _side(idx)
  }
}





