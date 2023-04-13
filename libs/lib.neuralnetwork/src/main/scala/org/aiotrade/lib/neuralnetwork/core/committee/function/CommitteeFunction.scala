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

import org.aiotrade.lib.math.vector.DefaultVec
import org.aiotrade.lib.math.vector.Vec


/**
 *
 * @author Caoyuan Deng
 */
abstract class CommitteeFunction {
  /**
   * Builds a committe response from an array of outputs. For each feature of
   * the output <code>piecewise()</code> is called.
   *
   * @param x
   *            outputs of the committe members
   * @return the output of the committe
   */
  def assamble(xs: Array[Vec]): Vec = {
    val dim = xs(0).dimension
    val res = new DefaultVec(dim)
    var i = 0
    while (i < dim) {
      val piece = Array.ofDim[Double](xs.length)
      var j = 0
      while (j < xs.length) {
        piece(j) = xs(j)(i)
        j += 1
      }
      res(i) = piecewise(piece)
      i += 1
    }
    res
  }
    
  /**
   * Computes the value of an output feature of a committe.
   *
   * @param x
   *            the feature of each each committe member's output
   * @return the resulting output feature
   */
  protected def piecewise(xs: Array[Double]): Double
}