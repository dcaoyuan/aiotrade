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

package org.aiotrade.lib.neuralnetwork.core.committee.function

import org.aiotrade.lib.math.vector.Vec
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
class Popularity extends CommitteeFunction {
    
  protected def piecewise(x: Array[Double]): Double = {
    throw new UnsupportedOperationException()
  }
    
  override
  def assamble(xs: Array[Vec]): Vec = {
    val frecuencier = new mutable.HashMap[Vec, Int]()
        
    var i = 0
    while (i < xs.length) {
      val x = xs(i)
      frecuencier.get(xs(i)) match {
        case Some(v) => frecuencier(x) = v + 1
        case None => frecuencier(x) = 1
      }
      i += 1
    }
        
    var mostPopular: Vec  = null
    var maxVote = Int.MinValue
    for ((x, v) <- frecuencier) {
      if (v > maxVote) {
        maxVote = v
        mostPopular = x
      }
    }

    mostPopular
  }
}