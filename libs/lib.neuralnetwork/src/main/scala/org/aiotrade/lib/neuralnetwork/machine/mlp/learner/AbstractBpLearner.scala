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
 * Backpropagation algorithm
 *
 * We define:
 *   delta: dE/dnet, is a real value
 *   gradient: dE/dW, is a vector
 * here,
 *   E is the error,
 *   net is the neuron's net input
 *   w is the weight vector
 *
 * @author Caoyuan Deng
 */
abstract class AbstractBpLearner(val neuron: PerceptronNeuron, val mode: Mode) extends Learner {
    
  /** 
   * differential coefficient of error to weight: (dE / dW), ie. gradient of E(W) 
   */
  private lazy val _sumGradient: Vec = new DefaultVec(neuron.inputDimension)
  protected def sumGradient = _sumGradient
    
  /**
   * Compute gradient of error to weight (dE / dw_ij)
   * sum it if it's called again and again,
   * gradient should be set to zero after adpat() each time
   *
   * This should be called after computeDelaAs..()
   *
   * @NOTICE
   * gradient should has been computed (several times in batch mode, one time
   * in point by point mode) before adapt() to make sure the gradient has been
   * computed and summed (in batch mode).
   *
   * @param delta, (dE / dnet_ij)
   */
  def computeGradientAndSumIt() {
    /** compute and get neuron's gradient vector */
    val gradient = neuron.gradient
        
    /** sum it */
    var i = 0
    while (i < neuron.inputDimension) {
      _sumGradient(i) = _sumGradient(i) + gradient(i)
      i += 1
    }
  }
    
  /**
   * Reset leaner, here, we just set sumGradient to 0
   * This should be called after adapt() is called each time
   */
  protected def reset() {
    _sumGradient.setAll(0)
  }
    
  def computeDeltaAsOutputNeuron() {
    neuron.computerDeltaAsInOutputLayer
  }
    
  /**
   * Compute the deltas using the subsequent layer deltas and the weight
   * emmanent from this neuron.
   * 
   * @return the value of this neuron's delta
   */
  def computeDeltaAsHiddenNeuron() {
    neuron.computerDeltaAsInHiddenLayer
  }
    
  val learnerName = "Delta Learner"
    
  def setOpts(opts: Double*) {}
}


