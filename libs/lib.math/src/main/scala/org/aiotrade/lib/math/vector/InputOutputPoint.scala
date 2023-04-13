/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.math.vector

import java.util.Random
import scala.reflect.ClassTag

/**
 * An (Input, Output) pair: one point of combined input-output space.
 *
 * @author Caoyuan Deng
 */
class InputOutputPoint protected (val input: Vec, val output: Vec)

object InputOutputPoint {
  def apply(input: Vec, output: Vec) = new InputOutputPoint(input, output)
  def apply(inputDimension: Int, outputDimension: Int) = new InputOutputPoint(new DefaultVec(inputDimension), new DefaultVec(outputDimension))
  
  def unapply(iop: InputOutputPoint): Option[(Vec, Vec)] = Some(iop.input, iop.output)
  
  def randomizeOrder_createNew[T <: InputOutputPoint: ClassTag](iops: Array[T]): Array[T] = {
    val size = iops.length
    val result = Array.ofDim[T](size)

    System.arraycopy(iops, 0, result, 0, size)
    val random = new Random(System.currentTimeMillis)
    var i = 0
    while (i < size) {
      val next = random.nextInt(size - i)
      val iop = result(next)
      result(next) = result(i)
      result(i) = iop
      i += 1
    }

    result
  }
}
