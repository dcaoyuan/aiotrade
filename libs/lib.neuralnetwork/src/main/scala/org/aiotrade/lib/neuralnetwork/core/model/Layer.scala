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

package org.aiotrade.lib.neuralnetwork.core.model

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec

/**
 * 
 * @author Caoyuan Deng
 */
class Layer(private var _nextLayer: Layer, private var _inputDimension: Int) {
    
  private var _neurons = new ArrayList[Neuron]()
  protected var _isInAdapting: Boolean = _
    
  def connectTo(nextLayer: Layer) {
    _nextLayer = nextLayer
    neurons foreach (_.connectTo(nextLayer.neurons))
  }
    
  protected def neuronsActivation: Vec = {
    val result = new DefaultVec(numNeurons)
        
    var i = 0
    while (i < numNeurons) {
      result(i) = _neurons(i).output
      i += 1
    }
    result
  }
    
  def inputDimension = _inputDimension
  protected def inputDimension_=(inputDimension: Int) {
    _inputDimension = inputDimension
  }
    
  def neurons = _neurons
  def neurons_=(neurons: ArrayList[Neuron]) {
    _neurons = neurons
  }
    
  def nextLayer = _nextLayer
  def nextLayer_=(nextLayer: Layer) {
    _nextLayer = nextLayer
  }
    
    
  def isInAdapting = _isInAdapting
  def isInAdapting_=(b: Boolean) {
    _isInAdapting = b
  }
    
  def neuronsOutput: Vec = neuronsActivation
    
  def propagateToNextLayer() {
    if (_nextLayer != null) {
      _nextLayer.setInputToNeurons(neuronsOutput)
    }
  }
    
  def reset() {
    neurons foreach (_.reset)
  }
    
  def setInputToNeurons(input: Vec) {
    neurons foreach (_.input = input)
  }
    
  def setExpectedOutputToNeurons(expectedOutput: Vec) {
    var i = 0
    while (i < neurons.length) {
      neurons(i).expectedOutput = expectedOutput(i)
      i += 1
    }
  }
    
  def numNeurons: Int = _neurons.length
    
  def addNeuron(neuron: Neuron) {
    _neurons += neuron
  }
    
}