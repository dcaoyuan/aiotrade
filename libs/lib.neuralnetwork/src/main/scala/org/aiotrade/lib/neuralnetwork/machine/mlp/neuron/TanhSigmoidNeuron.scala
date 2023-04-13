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

package org.aiotrade.lib.neuralnetwork.machine.mlp.neuron

/**
 * The double-polarity sigmoid neuron
 * 
 * @author Caoyuan Deng
 */
class TanhSigmoidNeuron extends PerceptronNeuron {
  import TanhSigmoidNeuron._
  
  /**
   * tanh(x) = (1 - e^(x) / (1 + e^(x)) 
   *         = (1 / (1 + e^(x))) * 2 - 1
   *         = logi(x) * 2 - 1
   * It's a double-polarity activate function.
   *
   * Also:
   * tanh(x) = sinh(x) / cosh(x), 
   * sinh(x) = (e^(x) - e^(-x)) / 2
   * cosh(x) = (e^(x) + e^(-x)) / 2
   */
  def f(x: Double): Double = {
    a * math.tanh(b * x)
  }
    
  /**
   * y(u) = tanh(u)  ->  y'(u) = 1 / cosh(u) ^ 2
   * u(x) = b * x    ->  u'(x) = b * x ^ 0 = b
   *
   * y(x) = tanh(b * x) = y(u(x))  ->  y(x)' = y'(u) * u'(x) 
   *                                         = b / cosh(u) ^ 2
   *                                         = b / cosh(b * x) ^ 2
   * 
   * v(x) = tanh(b * x) = a * y(x) ->  v'(x) = a * y'(x) 
   *                                         = a * b / cosh(b * x) ^ 2
   *
   * As: 
   * 1 - tanh(x) ^ 2 = 1 / cosh(x) ^ 2
   * 
   * we get:
   * v'(x) = b / a * (a - a * tanh(b * x) * (a + a * tanh(b * x))
   *       = b / a * (a - f(x)) * (a + f(x)) 
   */
  def df(x: Double): Double = {
    val y = f(x)
    b / a  * (a - y) * (a + y)
  }
}

object TanhSigmoidNeuron {
  private val a = 1.7159
  private val b = 2.0 / 3.0
}
