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

package org.aiotrade.lib.neuralnetwork.core.descriptor

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.neuralnetwork.core.model.Parameter
import org.aiotrade.lib.neuralnetwork.core.model.Network
import org.aiotrade.lib.neuralnetwork.datasource.DataSource
import org.aiotrade.lib.util.Descriptor

/**
 * @author Caoyuan Deng
 */

abstract class NetworkDescriptor protected () extends Descriptor {
    
  private var _param: Parameter = _
  private var _dataSource: DataSource = _
    
  def numLayers: Int
    
  def layerDescriptors: ArrayList[_ <: LayerDescriptor]
    
  def param = _param
  def param_=(param: Parameter) {
    _param = param
  }
    
  /**
   * A factory of configured and ready to train neural networks.
   *
   * @return configured network.
   */
  def createServiceInstance: Network = {
    var networkInstance: Network = null
        
    try {
      networkInstance = serviceClass.newInstance.asInstanceOf[Network]
            
      if (networkInstance != null) {
        networkInstance.init(this)
      }
    } catch {
      case ex: Throwable =>  throw new RuntimeException(ex)
    }
        
    networkInstance
  }
    
  def serviceClass: Class[_]
  def serviceClass_=(clazz: Class[_]) {}
    
  def dataSource = _dataSource
  def dataSource_=(dataSource: DataSource) {
    _dataSource = dataSource
  }
    
  @throws(classOf[CloneNotSupportedException])
  override 
  def clone: NetworkDescriptor = {
    /** 
     * @TODO 
     */
    super.clone.asInstanceOf[NetworkDescriptor]
  }
}