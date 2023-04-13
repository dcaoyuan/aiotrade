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

import org.aiotrade.lib.neuralnetwork.datasource.DataSource
import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec


/**
 * A synthetic environment for the circle-in-the-square problem. In this problem
 * inputs are distributed in a two-dimensional square. The objective is to
 * determine of weather a given point is located inside a circunsference laid on
 * the center of the square.
 * 
 * In this case, if the point belongs to the circle it will have `1' as output
 * or `0' in the other case.
 *
 * @author Caoyuan Deng 
 */
class CircleInTheSquareDataSource extends SyntheticDataSource {
  import CircleInTheSquareDataSource._
  
  private var _center: Vec = _
  private var _innerCircleRadius = 0.399
  private var _squareDimension = 1.0
    
  def innerCircleRadius = _innerCircleRadius    
  def innerCircleRadius_=(innerCircleRadius: Double) {
    _innerCircleRadius = innerCircleRadius
  }

  def squareDimension = _squareDimension    
  def squareDimension_=(squareDimension: Double) {
    _squareDimension = squareDimension
  }
    
  override
  protected def synthetizeOutput(input: Vec): Vec = {
    if (_center == null) {
      val cent = Array( _squareDimension / 2, _squareDimension / 2 )
      _center = new DefaultVec(cent)
    }
        
    val dist = _center.metric(input)
    if (dist > _innerCircleRadius) {
      new DefaultVec(ZERO_PAT)
    } else {
      new DefaultVec(ONE_PAT)
    }
  }
    
  override
  protected def generateRandomInput: Vec = {
    val input = new DefaultVec(2)
    input.randomize(0, _squareDimension)
    input
  }
    
  @throws(classOf[Exception])
  def checkValidation() {
    super.validate
    if (_squareDimension <= 0 || _innerCircleRadius * 2 > _squareDimension) {
      throw new Exception("Wrong parameters")
    }
  }
    
  def createSampleInstance: DataSource = {
    new CircleInTheSquareDataSource
  }
    
}
   
object CircleInTheSquareDataSource {
  protected val ONE_PAT = Array(1.0)
  protected val ZERO_PAT = Array(0.0)
}