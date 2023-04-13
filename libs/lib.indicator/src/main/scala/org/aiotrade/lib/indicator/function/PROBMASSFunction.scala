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

import org.aiotrade.lib.math.StatsFunctions
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.indicator.Factor

/**
 *
 * @author Caoyuan Deng
 */
final case class PROBMASSFunction extends Function {
  protected def probMass(idx: Int, baseVar: TVar[Double], period: Double, nInterval: Double): Array[Array[Double]] = {
    val begIdx = idx - period.intValue + 1
    val endIdx = idx

    StatsFunctions.probMass(baseVar.values, begIdx, endIdx, nInterval.intValue)
  }

  protected def probMass(idx: Int, baseVar: TVar[Double], weight: TVar[Double], period: Double, nInterval: Double): Array[Array[Double]] = {
    val begIdx = idx - period.intValue + 1
    val endIdx = idx

    StatsFunctions.probMass(baseVar.values, weight.values, begIdx, endIdx, nInterval.intValue)
  }
    
  var period: Factor = _
  var nInterval: Factor = _

  var baseVar: TVar[Double] = _
  var weight:  TVar[Double] = _
    
  /**
   * as this function do not remember previous valus, do not need a Var as probMass
   */
  var _probMass: Array[Array[Double]] = _
    
  override def set(baseSer: BaseTSer, args: Any*) : Unit = {
    super.set(baseSer)
    args match {
      case Seq(a0: TVar[Double], a1: TVar[Double], a2: Factor, a3: Factor) =>
        baseVar = a0
        weight.equals(a1)
        period.equals(a2)
        nInterval.equals(a3)
    }
  }
    
  protected def computeSpot(i: Int) : Unit = {
    if (weight == null) {
            
      _probMass = probMass(i, baseVar, period.value, nInterval.value);
            
    } else {
            
      _probMass = probMass(i, baseVar, weight, period.value, nInterval.value);
            
    }
  }
    
  /**
   * override compute(int), this function is not dependent on previous values
   */
  def compute(idx: Int): Unit = {
    computeSpot(idx)
  }
    
  def probMass(sessionId: Long, idx: Int): Array[Array[Double]] = {
    compute(idx)
        
    _probMass
  }
    
}

