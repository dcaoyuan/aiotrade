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
 * Neuron.
 *
 * @author Caoyuan Deng
 * 
 * @param  inputDimension
 * @param  If true, hidden node, if false, output node

 */
abstract class Neuron(_inputDimension: Int, private var _isHidden: Boolean = true) {
  def this() = this(0, true)
  
  private var _connectedNeurons = new ArrayList[Neuron]()
  private var _expectedOutput: Double = _
  private var _input: Vec = _ 

  if (_inputDimension != 0) {
    init(_inputDimension, _isHidden)
  } else {
    // do nothing, need to call init() before use it
  }
    
  def init(inputDimension: Int, hidden: Boolean) {
    _input = new DefaultVec(inputDimension)
    _isHidden = hidden
  }
    
  def inputDimension: Int = _input.dimension
    
  /**
   * reset the current activation. Such as: remove the current input.
   */
  def reset {
    _input = null
  }
    
  def setInput(idx: Int, value: Double) {
    _input(idx) = value
  }
    
  def input = _input
  def input_=(source: Vec) {
    if (_input == null || _input.dimension != source.dimension) {
      _input = new DefaultVec(source.dimension)
    }
        
    /**
     * As input may be modified by this neuron (such as setInput(int, double)),
     * other class may not know about this, so we'd better to just copy it.
     */
    _input.copy(source)
  }

  def expectedOutput = _expectedOutput
  def expectedOutput_=(expectedOutput: Double) {
    _expectedOutput = expectedOutput
  }

  def output: Double = activation
    
  def isHidden = _isHidden
  def isHidden_=(b: Boolean) {
    _isHidden = b
  }
    
  def connectTo(neurons: ArrayList[Neuron]) {
    neurons foreach connectTo
  }
    
  def connectTo(neuron: Neuron) {
    _connectedNeurons += neuron
  }
    
  def connectedNeurons = _connectedNeurons
  def numConnectedNeurons = _connectedNeurons.size
    
  protected def activation: Double
}