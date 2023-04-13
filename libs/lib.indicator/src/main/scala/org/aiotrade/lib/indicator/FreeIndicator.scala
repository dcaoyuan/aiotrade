/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.DefaultBaseTSer
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.math.timeseries.TFreq

/**
 * @author Caoyuan Deng
 */
class FreeIndicator($serProvider: SerProvider, $freq: TFreq) extends DefaultBaseTSer($serProvider, $freq)
                                                                with org.aiotrade.lib.math.indicator.Indicator {

  private var _baseSer: BaseTSer = _

  private var _uniSymbol: Option[String] = None

  def uniSymbol = _uniSymbol
  def uniSymbol_=(uniSymbol: String) {
    _uniSymbol = uniSymbol match {
      case null | "" => None
      case _ => Some(uniSymbol)
    }
  }

  def set(baseSer: BaseTSer) {
    _baseSer = baseSer
    if (baseSer != null) {
      super.set(baseSer.freq)
    }
  }

  def baseSer: BaseTSer = _baseSer
  def baseSer_=(baseSer: BaseTSer) {
    set(baseSer)
  }

  /**
   * @param time to be computed from
   */
  def computeFrom(time: Long) {}
  def computedTime: Long = this.lastOccurredTime

  def dispose {}

}
