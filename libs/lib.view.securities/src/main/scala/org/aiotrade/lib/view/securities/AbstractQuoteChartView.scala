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
package org.aiotrade.lib.view.securities

import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.charting.view.scalar.LgScalar
import org.aiotrade.lib.charting.view.scalar.LinearScalar
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable.HashSet


/**
 *
 * @author Caoyuan Deng
 */
object AbstractQuoteChartView {
  def internal_switchAllQuoteChartType(originalType: QuoteChart.Type, targetType: QuoteChart.Type): QuoteChart.Type = {
    val newType =
      if (targetType != null) {
        targetType
      } else {
        originalType match {
          case QuoteChart.Type.Candle => QuoteChart.Type.Ohlc
          case QuoteChart.Type.Ohlc => QuoteChart.Type.Line
          case QuoteChart.Type.Line => QuoteChart.Type.Candle
          case _ => null
        }
      }

    newType
  }

}

import AbstractQuoteChartView._
abstract class AbstractQuoteChartView(acontroller: ChartingController,
                                      aquoteSer: QuoteSer,
                                      empty: Boolean
) extends {
  private var _quoteChart: QuoteChart = null
  protected var maxVolume, minVolume: Double = 0.0
  protected var sec: Sec = null
} with ChartView(acontroller, aquoteSer, empty) with WithQuoteChart {

  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)

  override def init(controller: ChartingController, mainSer: TSer) {
    super.init(controller, mainSer)
    sec = controller.serProvider.asInstanceOf[Sec]
    if (axisXPane != null) {
      axisXPane.setTimeZone(sec.exchange.timeZone)
    }
  }

  def quoteSer: QuoteSer = mainSer.asInstanceOf[QuoteSer]
  def quoteChart: QuoteChart = _quoteChart

  protected def putChartsOfMainSer {
    _quoteChart = new QuoteChart

    val vars = new HashSet[TVar[_]]
    for (v <- mainSer.vars if v.plot == Plot.Quote) {
      vars.add(v)
    }
    mainSerChartToVars.put(_quoteChart, vars)

    _quoteChart.model.set(
      quoteSer.open,
      quoteSer.high,
      quoteSer.low,
      quoteSer.close)

    _quoteChart.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT)
    mainChartPane.putChart(_quoteChart)
  }

  override def computeMaxMin {
    var min = Double.MaxValue
    var max = Double.MinValue

    /** minimum volume should be 0 */
    minVolume = 0
    maxVolume = Double.MinValue
    val qSer = mainSer.asInstanceOf[QuoteSer]
    var i = 1
    while (i <= nBars) {
      val time = tb(i)
      if (qSer.exists(time) && qSer.close(time) > 0) {
        max = math.max(max, qSer.high(time))
        min = math.min(min, qSer.low(time))
        maxVolume = math.max(maxVolume, qSer.volume(time))
      }

      i += 1
    }

    if (maxVolume == 0) {
      maxVolume = 1
    }

    if (maxVolume == minVolume) {
      maxVolume += 1
    }

    if (max == min) {
      max *= 1.05f
      min *= 0.95f
    }

    setMaxMinValue(max, min)
  }

  def getMaxVolume: Double = {
    maxVolume
  }

  def getMinVolume: Double = {
    minVolume
  }

  def getQuoteChart: QuoteChart = {
    _quoteChart
  }

  def swithScalarType {
    mainChartPane.valueScalar.getType match {
      case Scalar.Type.Linear => mainChartPane.valueScalar = new LgScalar
      case _ => mainChartPane.valueScalar = new LinearScalar
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
}


