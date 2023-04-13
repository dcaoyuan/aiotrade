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
import org.aiotrade.lib.neuralnetwork.machine.mlp.neuron.PerceptronNeuron


/**
 * Resilient Backpropagation (RPROP) algorithm
 *
 * Each weight has its own individual update-value (delta_ij(t)) represented
 * by the next object.
 *
 * The weight update deltaW_ij(t) is defined as follows (dE(t) / dW_ij is the
 * summed gradient for a single epoch):
 *                | -delta_ij(t), if dE(t) / dW_ij > 0
 * deltaW_ij(t) = |  delta_ij(t), if dE(t) / dW_ij < 0
 *                |  0          , otherwise
 *
 * The delta_ij values are updated as follows:
 *               | learningRatePlus  * delta_ij(t-1), if dE(t-1)/ dW_ij * dE(t)/ dW_ij > 0
 * delta_ij(t) = | learningRateMinus * delta_ij(t-1), if dE(t-1)/ dW_ij * dE(t)/ dW_ij < 0
 *               | delta_ij(t-1),                   , otherwise
 *
 * where 0 < learningRateMinus < 1 < learningRatePlus
 *
 * The learningRatePlus  usally set to 1.2
 * The learningRateMinus usally set to 0.5
 *
 * @author Caoyuan Deng
 */
class RpropBpLearner(_neuron: PerceptronNeuron) extends AbstractBpLearner(_neuron, Mode.Batch) {
    
  private val learningRatePlus  = 1.2
  private val learningRateMinus = 0.5
  private val initDeltaWeightValue = 0.1
  private val maxDeltaWeightValue  = 50.0
  private val minDelatWeightValue  = 0.000001
    
  /** weight updated value vector: deltaW_ij(t) */
  private lazy val deltaWeight = new DefaultVec(neuron.inputDimension)
    
  /** The summed gradient of the previous epoch (dE(t-1) / dW_ij) */
  private lazy val prevSumGradient = new DefaultVec(neuron.inputDimension)
    
  /** weight update refering base vec: delta_ij(t) */
  private lazy val deltaWeightBase = {
    val x = new DefaultVec(neuron.inputDimension)
    x.setAll(initDeltaWeightValue)
    x
  }
    
  def adapt(args: Double*) {
    val weight = neuron.weight
    /**
     * @NOTICE:
     * dE/dw = (dE/de * de/dy * ...) *...
     * de/dy = -1, however * -1 is neglected when compute gradientWeight, so
     * gradientWeight has an opposite sign to dE/dw.
     * we fix this here by times gradientWeight by -1.0
     */
    val currSumGradient = sumGradient.times(-1.0)
        
    // for each input's weight, do cycle:
    val n = neuron.inputDimension
    var i = 0
    while (i < n) {
      val prevGW_i = prevSumGradient(i)
      val currGW_i = currSumGradient(i)

      if (prevGW_i * currGW_i > 0) {
                
        deltaWeightBase(i) = math.min(deltaWeightBase(i) * learningRatePlus, maxDeltaWeightValue)
        deltaWeight(i) = -1.0 * math.signum(currGW_i) * deltaWeightBase(i)
        prevSumGradient(i) = currGW_i
                
      } else if (prevGW_i * currGW_i < 0) {
                
        /**
         * gradient's sign changed -> the previous step was too large and
         * the minimum that is looking for was missed, the previous
         * weight update is reverted.
         *
         * And we should also minus deltaWeightBase by times the learningRateMinus(> 0 && < 1)
         */
        deltaWeightBase(i) = math.max(deltaWeightBase(i) * learningRateMinus, minDelatWeightValue)

        /** revert previous weight update */
        deltaWeight(i) = -1.0 * deltaWeight(i)

        /**
         * due the backtracking step the derivative is supposed to change its sign
         * again in the following step. To prevent double punishement we set the
         * gradient to 0
         */
        prevSumGradient(i) = 0
                
      } else /** prevGW * currGW == 0 */ {
                
        /** no need to set a new deltaWeightBase */
        deltaWeight(i) = -1.0 * math.signum(currGW_i) * deltaWeightBase(i)
        prevSumGradient(i) = currGW_i
                
      }
            
      weight(i) = weight(i) + deltaWeight(i)
      
      i += 1
    }
        
    /** this learner should reset gradient to 0 after adapt() is called each time */
    reset
  }
    
  override 
  val learnerName = "Resilient Backpropagation (RPROP) Leaner"
}




