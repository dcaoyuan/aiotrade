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
import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.InputOutputPointSet
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.core.committee.function.CommitteeFunction
import org.aiotrade.lib.neuralnetwork.core.descriptor.NetworkDescriptor
import org.aiotrade.lib.neuralnetwork.core.model.Parameter
import org.aiotrade.lib.neuralnetwork.core.model.Network

/**
 * A committe or set of neural networks to be trained and used concurrently to
 * provide a consistent learning experiment and predictions.
 *
 * @author Caoyuan
 */
@SerialVersionUID(3256438123029147696L)
class NetworkCommittee(private var _combinationFunction: CommitteeFunction) extends Network {
  val name = "Network Committe"

  private var _committee = new ArrayList[Network]()
  private var _isSerialProcessing  = false
  private var _param: Parameter = _
    
  def addCommitteeMember(member: Network) {
    _committee += member
  }
    
  def committe = _committee

  def param = _param
  def param_=(param: Parameter) {
    _param = param
  }
    
  def isSerialProcessing = _isSerialProcessing
  def isSerialProcessing_=(serialProcessing: Boolean) {
    _isSerialProcessing = serialProcessing
  }
  
  def combinationFunction = _combinationFunction    
  def combinationFunction_=(combinationFunction: CommitteeFunction) {
    _combinationFunction = combinationFunction
  }
    
  def committeSize = _committee.size    

  def inputDimension = 0  // TODO 
  def outputDimension = 0 // TODO 
    
  def learnOnePoint(input: Vec, output: Vec): Double = {
    throw new UnsupportedOperationException()
  }
    
  def predict(input: Vec): Vec = {
    val results = _committee map (_.predict(input))
    _combinationFunction.assamble(results.toArray)
  }
    
  def train(iops: InputOutputPointSet[_ <: InputOutputPoint]) {
    val threadGroup = new ThreadGroup("a-group-of:" + this)
    for (network <- _committee) {
      val trainRunner = new TrainRunner(network, iops.cloneWithRandomizedOrder)
      val aThread = new Thread(threadGroup, trainRunner)
      aThread.start
            
      if (isSerialProcessing) {
        while (aThread.isAlive) {
          Thread.`yield`
        }
      }
    }

    while (threadGroup.activeCount > 0) {
      Thread.`yield`
    }
  }
    
  @throws(classOf[Exception])
  def init(descriptor: NetworkDescriptor) {
    val conf = descriptor.asInstanceOf[NetworkCommitteeConfig]
        
    param = descriptor.param
        
    try {
      combinationFunction = conf.combinationFunctionClass.newInstance
    } catch {
      case ex: Throwable => throw ex
    }
        
    _committee.clear
        
    for (ce <- conf.committeeElements) {
      val elementConfig = ce.networkDescriptor
            
      if (elementConfig.dataSource == null) {
        elementConfig.dataSource = conf.dataSource
      }
            
      
      var j = 0
      while (j < ce.amount) {
        addCommitteeMember(ce.networkDescriptor.createServiceInstance)
        j += 1
      }
    }
  }
    
  def cloneDescriptor: NetworkDescriptor = {
    var res = try {
      this.clone.asInstanceOf[NetworkCommitteeConfig]
    } catch {
      case ex: CloneNotSupportedException =>
        ex.printStackTrace
        new NetworkCommitteeConfig()
    }
    res.combinationFunctionClass = _combinationFunction.getClass
        
    var i = 0
    while (i < _committee.size) {
      val currentMember = _committee(i).cloneDescriptor
      
      //if (EqualsBuilder.reflectionEquals(cur.getNetworkConfig(), currentMember)) {
      res.committeeElements find (_.networkDescriptor == currentMember) match {
        case Some(cur) =>
          
          cur.amount = (cur.amount + 1)
        case None =>
          val ce = new CommitteeElement(currentMember, 1)
          res.addCommitteeMember(ce)
      }
            
      i += 1
    }
        
    res
  }
    
  class TrainRunner(network: Network, iops: InputOutputPointSet[_ <: InputOutputPoint]) extends Runnable {
    def run() {
      network.train(iops)
    }
  }

}