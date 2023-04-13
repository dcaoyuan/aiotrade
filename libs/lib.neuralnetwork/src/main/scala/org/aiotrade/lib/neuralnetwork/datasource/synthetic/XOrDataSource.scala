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
package org.aiotrade.lib.neuralnetwork.datasource.synthetic

import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.datasource.DataSource


/**
 *
 * @author Caoyuan Deng
 */
class XOrDataSource extends SyntheticDataSource {
  import XOrDataSource._

  protected def generateRandomInput: Vec = {
    val input = new DefaultVec(inputSpace.length)
    input.randomize(-1.0, 1.0)
    input
    //return new DefaultVec(inputSpace[randomer.nextSecureInt(0, inputSpace.length)]);
  }
    
  protected def synthetizeOutput(input: Vec): Vec = {
    if (input.normOne > 0 && input.normOne < 2) {
      new DefaultVec(ONE_PAT)
    } else {
      new DefaultVec(ZERO_PAT)
    }
  }
    
  def createSampleInstance: DataSource = {
    // TODO Auto-generated method stub
    new XOrDataSource()
  }
    
  @throws(classOf[Exception])
  def checkValidation() {}
}

object XOrDataSource {
  protected val inputSpace = Array(
    Array(0.0, 0.0), Array(0.0, 1.0), Array(1.0, 0.0), Array(1.0, 1.0)
  )
    
  //private RandomData randomer = new RandomDataImpl();
    
  protected val ONE_PAT = Array(1.0)
    
  protected val ZERO_PAT = Array(0.0)
}