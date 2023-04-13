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

package org.aiotrade.lib.neuralnetwork.datasource

import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.InputOutputPointSet
import org.aiotrade.lib.math.vector.Vec

/**
 * 
 * @author Caoyuan Deng
 */
abstract class DataSource(private var _numNetworks: Int, private var _inputDimension: Int, private var _outputDimension: Int) {
  if (_numNetworks != 0) {
    init(_numNetworks, _inputDimension, _outputDimension)
  }
  
  private var _trainingPointSets: Array[InputOutputPointSet[_ <: InputOutputPoint]] = _
  private var _validatingPointSets: Array[InputOutputPointSet[_ <: InputOutputPoint]] = _
    
  def this() = this(0, 0, 0)
    
  def init(nNetworks: Int, inputDimension: Int, outputDimension: Int) {
    _numNetworks = nNetworks
    _inputDimension = inputDimension
    _outputDimension = outputDimension
        
    _trainingPointSets = Array.ofDim[InputOutputPointSet[_ <: InputOutputPoint]](nNetworks)
    _validatingPointSets = Array.ofDim[InputOutputPointSet[_ <: InputOutputPoint]](nNetworks)
  }
    
  /**
   * Return the number of times the networks will be run. 
   * 
   * @return total number of networks in running
   */
  def numNetworks = _numNetworks
  def numNetworks_=(nNetworks: Int) {
    _numNetworks = nNetworks
  }
  
  def inputDimension = _inputDimension
  def inputDimension_=(inputDimension: Int) {
    _inputDimension = inputDimension
  }
    
  def outputDimension = _outputDimension
  def outputDimension_=(outputDimension: Int) {
    _outputDimension = outputDimension
  }
    
  def getTrainingPointSet(networkIdx: Int): InputOutputPointSet[_ <: InputOutputPoint] = {
    _trainingPointSets(networkIdx)
  }
    
  def setTrainingPointSet(networkIdx: Int, trainingPointSet: InputOutputPointSet[_ <: InputOutputPoint]) {
    _trainingPointSets(networkIdx) = trainingPointSet
  }
    
  def getValidatingPointSet(networkIdx: Int): InputOutputPointSet[_] = {
    _validatingPointSets(networkIdx)
  }
    
  def setValidatingPointSet(networkIdx: Int, validatingPointSet: InputOutputPointSet[_ <: InputOutputPoint]) {
    _validatingPointSets(networkIdx) = validatingPointSet
  }
    
  /**
   * Read a training points from the environment.
   *
   *
   * @param networkIdx  The index of the network that will run
   * @return the training points as an array of InputOutputPoint
   * @throws Exception
   */
  @throws(classOf[Exception])
  def getTrainingPoints(networkIdx: Int): Array[_ <: InputOutputPoint]
    
  /**
   * Read validating points from the source. only input vector will be read.
   *
   * @param  networkIdx the networkIdx of the network that to run
   * @return Validating points set
   * @throws Exception
   */
  @throws(classOf[Exception])
  def getValidatingInputs(networkIdx: Int): Array[Vec]
    
  @throws(classOf[Exception])
  def writeResults(results: Array[Vec], networkIdx: Int)
    
  @throws(classOf[Exception])
  def checkValidation
    
}