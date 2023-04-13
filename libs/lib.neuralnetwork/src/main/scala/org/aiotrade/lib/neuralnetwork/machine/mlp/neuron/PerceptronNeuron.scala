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


package org.aiotrade.lib.neuralnetwork.machine.mlp.neuron

import org.aiotrade.lib.neuralnetwork.core.model.Neuron
import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.machine.mlp.learner.AbstractBpLearner
import org.aiotrade.lib.neuralnetwork.machine.mlp.learner.RpropBpLearner

/**
 * Perceptron Neuron
 *
 * As y is the output, w is the weight vector, x is the input vector, the function
 * form of perceptron is written as:
 *
 * y = <w * x> + b,
 * here, b is called 'bias' in this form of function
 *
 * or,
 * y = <w * x> - theta
 * here, theta = -b, theta is called 'threshold' in this form of fucntion.
 *
 * @author Caoyuan Deng
 */
abstract class PerceptronNeuron extends Neuron {
    
  private var _weight: Vec = _
    
  /** delta: dE/dnet, is a real value */
  private var _delta: Double = _
    
  /** differential coefficient of error to weight: (dE / dW), ie. gradient of E(W) */
  private var _gradient: Vec = _
    
  private var _learner: AbstractBpLearner = _
    
  private var _minInitWeightValue = Double.NaN //-0.05
    
  private var _maxInitWeightValue = Double.NaN //0.05
    
  override
  def init(inputDimensionWithoutThreshold: Int, hidden: Boolean) {
    /** add a dimension for threshold */
    val inputDimensionWithThreshold = inputDimensionWithoutThreshold + 1
        
    super.init(inputDimensionWithThreshold, hidden)
        
    if (inputDimensionWithoutThreshold > 0) {
      initWeight
      initLearner
    }
  }
    
  private def initLearner() {
    learner = new RpropBpLearner(this)
    //learner = new MomentumBpLearner(this)
  }
    
  /**
   *
   * @Note
   * If initial weight value is too large, the action function may work at
   * saturated status.
   * If initial weight value is too small, the action function may work at a
   * <b>same</b> local minimum each time, this may cause the result is almost
   * always the same.
   */
  private def initWeight() {
    _weight = new DefaultVec(inputDimension)
        
    if (_minInitWeightValue.isNaN && _maxInitWeightValue.isNaN) {
            
      _maxInitWeightValue = +1.0 / Math.sqrt(inputDimension)
      _minInitWeightValue = -1.0 * _maxInitWeightValue
            
    } else if (_minInitWeightValue.isNaN && !_maxInitWeightValue.isNaN) {
            
      _minInitWeightValue = -1.0 * _maxInitWeightValue
            
    } else if (_maxInitWeightValue.isNaN && !_minInitWeightValue.isNaN) {
            
      _maxInitWeightValue = -1.0 * _minInitWeightValue
            
    }
        
    _weight.randomize(_minInitWeightValue, _maxInitWeightValue)
        
    /**
     * threhold's initial weight value should also be set to a very small value.
     * but do not set to 0, which will cause no adapting of its weight value.
     *
     * As the threshold's input value keeps -1.0, it's better to initial its
     * weight value to a very small real value, so we multiple it 0.1, which
     * cause it's less than the other input's weights in 10 scale:
     *
     *   weight.set(THRESHOLD_DIMENSION_IDX, weight.get(THRESHOLD_DIMENSION_IDX) * 0.001);
     *
     * Do we need to do this? a bit large weight for threhold may also be useful
     * to adapt it's value faster and priorly (it's better to adapt threshold's
     * weight priorly than inputs' weight)
     *
     * Actually we can think the weight of threshold as the input, the -1 threhold
     * as weight (value = -1), so we only need to keep threshold as a small value
     * between [-1, 1]. which has the same scale level of initial value as other
     * inputs.
     *
     */
  }
    
  override
  def input_=(inputWithoutThreshold: Vec) {
    /** add a dimension for threshold */
    val inputDimensionWithThreshold = inputWithoutThreshold.dimension + 1
        
    if (input == null || input.dimension != inputDimensionWithThreshold) {
      input = new DefaultVec(inputDimensionWithThreshold)
    }
        
    /** set threshold input value (always be -1) */
    input(threholdDimensionIdx) = PerceptronNeuron.THRESHOLD_INPUT_VALUE
        
    /** As threshold's dimension idx is 0, so we copy to store beginning with 1 */
    input.copy(inputWithoutThreshold, 0, 1, inputWithoutThreshold.dimension)
  }
    
  private def threholdDimensionIdx: Int = {
    /**
     * @NOTICE
     * We assume the idx of threshold dimension in input vector is 0,
     * Don't change it to other value.
     *
     * @see input_=(Vec inputWithoutThreshold)
     */
    0
  }
    
  /**
   *
   * @return the weigths of this neuron
   */
  def weight = _weight    
  def weight_=(weight: Vec) {
    _weight = weight
  }
    
  def delta = _delta
  
  def learner = _learner
  def learner_=(learner: AbstractBpLearner) {
    _learner = learner
  }
    
  
  def adapt(learningRate: Double, momentumRate: Double) {
    _learner.adapt(learningRate, momentumRate);
  }
    
  /**
   *
   * The output of neuron's activation.
   *
   * It may has another form:
   *   f(net(input)) - threshold,
   * if we do not define threshold as a input dimension that weighted -1
   *
   */
  protected def activation = f(net())
    
  /**
   * @return net input value of this neuron 
   */
  protected def net() = input.innerProduct(_weight)
    
  def computerDeltaAsInOutputLayer() {
    _delta = (expectedOutput - output) * df(net())
  }
    
  /**
   * Compute the deltas using the subsequent layer deltas and the weights
   * to connected neurons.
   * 
   */
  def computerDeltaAsInHiddenLayer() {
    _delta = weightToConnectedNeurons.innerProduct(deltaOfConnectedNeurons) * df(net())
  }
    
  def gradient: Vec = {
    if (_gradient == null || _gradient.dimension != inputDimension) {
      _gradient = new DefaultVec(inputDimension)
    }
        
    var i = 0
    while (i < inputDimension) {
      /** gradient (dE / dW_ij) = input_ij * delta */
      _gradient(i) = input(i) * _delta
      i += 1
    }
        
    _gradient
  }
    
  private var buf_deltaOfConnectedNeurons: Vec = _
  private def deltaOfConnectedNeurons: Vec = {
    if (buf_deltaOfConnectedNeurons == null || buf_deltaOfConnectedNeurons.dimension != numConnectedNeurons) {
      buf_deltaOfConnectedNeurons = new DefaultVec(numConnectedNeurons)
    }
        
    var i = 0
    while (i < connectedNeurons.length) {
      val connectedNeuron = connectedNeurons(i).asInstanceOf[PerceptronNeuron]
      buf_deltaOfConnectedNeurons(i) = connectedNeuron.delta
      i += 1
    }
        
    buf_deltaOfConnectedNeurons
  }
    
  /**
   * We should re-organize the weight from the connected neurons in next layer
   *  Layer(i)      nextLayer(j)
   * ---------      ------------
   *
   *  (0) bias-----------+
   *                     |
   *  (1) neuron------+  |
   *                  V  V
   *  (2) neuron----> neuron       (*) index in weight of connectedNeuron
   *                  ^
   *  (n) neuron------+
   */
  private var buf_weightToConnectedNeurons: Vec = null
  private def weightToConnectedNeurons: Vec = {
    if (buf_weightToConnectedNeurons == null || buf_weightToConnectedNeurons.dimension != numConnectedNeurons) {
      buf_weightToConnectedNeurons = new DefaultVec(numConnectedNeurons)
    }
        
    /**
     * @NOTICE
     * We should consider the weight of connectedNeuron's threshold.
     * which is of the idx of 0, so the neurons' weight start from 1,
     * so, we should use weight.get(1 + i) here.
     **/
    var i = 0
    while (i < connectedNeurons.length) {
      val connectedNeuron = connectedNeurons(i).asInstanceOf[PerceptronNeuron]
            
      val weightToConnectedNeuron = if (i < connectedNeuron.threholdDimensionIdx) {
        connectedNeuron.weight(i)
      } else {
        connectedNeuron.weight(i + 1)
      }
            
      buf_weightToConnectedNeurons(i) = weightToConnectedNeuron
      i += 1
    }
        
    buf_weightToConnectedNeurons
  }
    
  def maxInitWeightValue = _maxInitWeightValue    
  def maxInitWeightValue_=(maxInitWeightValue: Double) {
    _maxInitWeightValue = maxInitWeightValue
  }
    
  def minInitWeightValue = _minInitWeightValue
  def minInitWeightValue(minInitWeightValue: Double) {
    _minInitWeightValue = minInitWeightValue
  }
    
  def f(x: Double): Double
  def df(x: Double): Double
}

object PerceptronNeuron {
  val THRESHOLD_INPUT_VALUE = -1.0
}