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

package org.aiotrade.lib.neuralnetwork.math

import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.core.model.Network


/**
 * 
 * @author Caoyuan Deng
 */
class DefaultStatisticsFacility extends StatisticsFacility {

  /**
   * Calculates the prediction error as the mean squared error
   */
  def computePredictionError(network: Network, set: Array[InputOutputPoint]): Double = {
    meanSquaredError(set, getPredictions(network, set))
  }

  protected def checkDimensions(desired: Array[InputOutputPoint], predictions: Array[Vec]) {
    if (desired.length != predictions.length) {
      throw new ArrayIndexOutOfBoundsException("Using arrays with different sizes.")
    }
  }

  /**
   * 
   * 
   * @param iop the dataset to be used as test iop
   * @param predictions the values of the predictions made with the above iop
   * @return the mean squared error
   */
  def meanSquaredError(iop: Array[InputOutputPoint], predictions: Array[Vec]): Double = {
    var result = 0.0
    checkDimensions(iop, predictions)
    var i = 0
    while (i < iop.length) {
      result += iop(i).output.metric(predictions(i)) / iop(i).output.dimension
      i += 1
    }
    result / iop.length
  }

  protected def getPredictions(net: Network, iop: Array[InputOutputPoint]): Array[Vec] = {
    val result = Array.ofDim[Vec](iop.length)
    var i = 0
    while (i < iop.length) {
      result(i) = net.predict(iop(i).input)
      i += 1
    }
    result
  }
}
