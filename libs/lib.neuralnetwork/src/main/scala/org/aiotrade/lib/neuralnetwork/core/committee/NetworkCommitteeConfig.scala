/*
 * Copyright (c) 2006-2013, AIOTrade Computing Co. and Contributors
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

package org.aiotrade.lib.neuralnetwork.core.committee

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.neuralnetwork.core.committee.function.Average
import org.aiotrade.lib.neuralnetwork.core.committee.function.CommitteeFunction
import org.aiotrade.lib.neuralnetwork.core.descriptor.LayerDescriptor
import org.aiotrade.lib.neuralnetwork.core.descriptor.NetworkDescriptor
import org.aiotrade.lib.neuralnetwork.machine.mlp.MlpNetwork
import org.aiotrade.lib.neuralnetwork.machine.mlp.MlpNetworkDescriptor

/**
 *
 * @author Caoyuan Deng
 */
class NetworkCommitteeConfig extends NetworkDescriptor {
    
  private var _committeeElements = new ArrayList[CommitteeElement]()
  private var _combinationFunctionClass: Class[_ <: CommitteeFunction] = _
  private var _isSerialProcessing: Boolean = _
    
  def numLayers: Int = {
    assert(false, "Todo")
    -1
  }
    
  def layerDescriptors: ArrayList[LayerDescriptor] = {
    assert(false, "Todo")
    null
  }
    
  def isSerialProcessing = _isSerialProcessing    
  def isSerialProcessing_=(serialProcessing: Boolean) {
    _isSerialProcessing = serialProcessing
  }
    
  def combinationFunctionClass = _combinationFunctionClass
  def combinationFunctionClass_=(combinationFunctionClass: Class[_ <: CommitteeFunction]) {
    _combinationFunctionClass = combinationFunctionClass;
  }
    
  def addCommitteeMember(ce: CommitteeElement) {
    _committeeElements += ce
  }
    
  def committeeElements = _committeeElements    
  def CommitteeElements_=(committeeElements: ArrayList[CommitteeElement]) {
    _committeeElements = committeeElements;
  }

  def serviceClass = classOf[NetworkCommittee]
    
  @throws(classOf[Exception])
  protected def checkInternalValidation() {
    var i = 0
    while (i < _committeeElements.size) {
      val comConf = _committeeElements(i).networkDescriptor
      if (comConf.dataSource == null) {
        comConf.dataSource = this.dataSource
      }
      i += 1
    }
        
    try {
      _combinationFunctionClass.newInstance
    } catch {
      case ex: InstantiationException => throw ex
      case ex: IllegalAccessException => throw ex
    }
  }
    
  def createSampleInstance: NetworkDescriptor = {
    val mainConf = new NetworkCommitteeConfig()
    val conf = new MlpNetworkDescriptor()
    conf.param = MlpNetwork.Param(500, 0.5, 0.0, 0.1)
        
    val hiddenLayer = LayerDescriptor("org.aiotrade.lib.neuralnetwork.machine.mlp.neuron.LogiSigmoidNeuron", 20)
    val outputLayer = LayerDescriptor("org.aiotrade.lib.neuralnetwork.machine.mlp.neuron.LinearNeuron", 8)
        
    val layers = new ArrayList[LayerDescriptor]()
    layers += hiddenLayer
    layers += outputLayer
        
    conf.layerDescriptors = layers
        
    val ce = new CommitteeElement(conf, 3)
    mainConf.addCommitteeMember(ce)
    mainConf.isSerialProcessing = false
    mainConf.combinationFunctionClass = classOf[Average]
        
    mainConf
  }
}
