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

import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.InputOutputPointSet
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.core.descriptor.NetworkDescriptor
import org.aiotrade.lib.util.actors.Publisher
import scala.concurrent.SyncVar

/**
 * A neural network
 * 
 * @author Caoyuan Deng
 */
trait Network extends Publisher with Serializable {
    
  private var _inAdapting: Boolean = _
    
  def isInAdapting = _inAdapting
  def isInAdapting_=(b: Boolean) {
    _inAdapting = b
  }
  
  @throws(classOf[Exception])
  def init(descriptor: NetworkDescriptor)
    
  /**
   * one learning step to learn one of the training points.
   *
   * @param input  The input vector.
   * @param output The desired output vector.
   *
   * @return the error of this learning step.
   */
  def learnOnePoint(input: Vec, output: Vec): Double
    
  /**
   * Compute a network prediction
   *
   * @param input The input to propagate.
   * @return the network output.
   */
  def predict(input: Vec): Vec
    
  /**
   * Train the network until the stop criteria is met.
   *
   * @param iop  The training set to be learned.
   */
  def train(iops: InputOutputPointSet[_ <: InputOutputPoint])
    
  def inputDimension: Int
    
  def outputDimension: Int
    
  def cloneDescriptor: NetworkDescriptor
    
  def name: String    
}


trait NetworkChangeEvent
/**
 * @param source of network
 * @param epoch
 * @param meanError
 * @param syncVar For sync usage, you can lock the training process to freeze network status 
 *                for a while by set this, and wait for it to be unlocked later.
 */
case class NetworkUpdated(network: Network, epoch: Long, meanError: Double, syncVar: SyncVar[Boolean]) extends NetworkChangeEvent
case class NetworkTrained(network: Network, epoch: Long, meanError: Double, syncVar: SyncVar[Boolean]) extends NetworkChangeEvent

trait Parameter
