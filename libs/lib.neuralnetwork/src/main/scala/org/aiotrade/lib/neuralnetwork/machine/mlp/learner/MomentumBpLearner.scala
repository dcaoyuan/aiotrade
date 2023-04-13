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

package org.aiotrade.lib.neuralnetwork.machine.mlp.learner

import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.machine.mlp.neuron.PerceptronNeuron


/**
 * 
 * @author Caoyuan Deng
 */
class MomentumBpLearner(_neuron: PerceptronNeuron) extends AbstractBpLearner(_neuron, Mode.Serial) {
    
  /** weight updated value vector: deltaW_ij(t) */
  private lazy val deltaWeight: Vec = new DefaultVec(_neuron.inputDimension)
    
  /** parameters */
  private var momentumRate: Double = _
  private var learningRate: Double = _
    
  def adapt(args: Double*) {
    learningRate = args(0)
    momentumRate = args(1)
        
    // Adapt the weight using the delta rule.
    val weight = _neuron.weight
    val gradient = sumGradient
        
    val n = _neuron.inputDimension
    var i = 0
    while (i < n) {
      val gradientTerm = gradient(i) * learningRate
      val prevDeltaWeightTerm = deltaWeight(i) * momentumRate
            
      deltaWeight(i) = gradientTerm + prevDeltaWeightTerm
            
      weight(i) + weight(i) + deltaWeight(i)
      
      i += 1
    }
        
    /** this learner should reset gradient to 0 after adapt() is called each time */
    reset
  }
    
  override 
  val learnerName ="Momentum Leaner"
    
  override 
  def setOpts(opts: Double*) {
    learningRate = opts(0)
    momentumRate = opts(1)
  }
}



