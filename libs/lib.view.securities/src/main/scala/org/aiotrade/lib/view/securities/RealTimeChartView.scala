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

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.util.Calendar
import org.aiotrade.lib.charting.chart.GridChart
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.securities.api
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.lib.util.swing.GBC

/**
 *
 * @author Caoyuan Deng
 */
object RealTimeChartView {
  /** all RealtimeQuoteChartView instances share the same type */
  private var quoteChartType: QuoteChart.Type = _
  
  def switchAllQuoteChartType(tpe: QuoteChart.Type) {
    quoteChartType = AbstractQuoteChartView.internal_switchAllQuoteChartType(quoteChartType, tpe)
  }

}

import RealTimeChartView._
class RealTimeChartView(_controller: ChartingController,
                        _quoteSer: QuoteSer,
                        _empty: Boolean
) extends {
  private var _prevClose = Null.Double
  private var gridValues: Array[Double] = null
  private val cal = Calendar.getInstance
  private var exchange: Exchange = null
} with AbstractQuoteChartView(_controller, _quoteSer, _empty) with Reactor {

  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)

  override 
  def init(controller: ChartingController, mainSer: TSer) {
    super.init(controller, mainSer)

    exchange = sec.exchange

    controller.isAutoScrollToNewData = false
    controller.isOnCalendarMode = true
    controller.fixedNBars = exchange.nMinutes
    controller.fixedLeftSideTime = exchange.openTime(baseSer.timestamps.firstOccurredTime)
    axisYPane.isSymmetricOnMiddleValue = true

    RealTimeChartView.quoteChartType = QuoteChart.Type.Line

    reactions += {
      case api.TickerEvt(ticker) => updateByTicker(ticker)
    }
    listenTo(sec)
    listenTo(mainSer)

    sec.exchange.uniSymbolToLastTradingDayTicker.get(sec.uniSymbol) foreach {updateByTicker(_)}
  }

  protected def initComponents {
    glassPane.isUsingInstantTitleValue = true

    setLayout(new GridBagLayout)

    add(glassPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 618))

    add(mainLayeredPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 618))

    add(axisYPane, GBC(1, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(0, 100))

    add(axisXPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))

    add(divisionPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
  }

  override protected def putChartsOfMainSer {
    super.putChartsOfMainSer

    // draw prevClose value grid
    val prevCloseGrid = new GridChart
    prevCloseGrid.set(mainChartPane, mainSer, Pane.DEPTH_DEFAULT)
    gridValues = Array(prevClose)
    prevCloseGrid.model.set(gridValues, GridChart.Direction.Horizontal)
    mainChartPane.putChart(prevCloseGrid)
  }

  override def computeMaxMin {
    super.computeMaxMin

    // adjust max/min according to prevClose
    if (Null.not(prevClose)) {
      var min = minValue
      var max = maxValue
      val maxDelta = math.max(math.abs(max - prevClose), math.abs(min - prevClose))
      max = prevClose + maxDelta
      min = prevClose - maxDelta
      setMaxMinValue(max, min)
    }
  }

  def quoteChartType: QuoteChart.Type = RealTimeChartView.quoteChartType
  def switchQuoteChartType(tpe: QuoteChart.Type) {
    switchAllQuoteChartType(tpe)

    repaint()
  }

  def prevClose = _prevClose
  def prevClose_=(prevClose: Double) {
    _prevClose = prevClose
    gridValues(0) = prevClose
    mainChartPane.referCursorValue = prevClose
    glassPane.referCursorValue = prevClose
  }

  override 
  def popupToDesktop {
    val popupView = new RealTimeChartView(controller, quoteSer)
    popupView.isInteractive = false
    val dimension = new Dimension(200, 150)
    val alwaysOnTop = true

    controller.popupViewToDesktop(popupView, dimension, alwaysOnTop, false)
  }

  override 
  def updateView(evt: TSerEvent) {
    evt match {
      case TSerEvent(_, _, _, _, _, _) => 
      case _ =>
    }
  }

  private def updateByTicker(ticker: Ticker) {
    val percentValue = ticker.changeInPercent
    val strValue = ("%+3.2f%% " format percentValue) + ticker.lastPrice
    val color = if (percentValue >= 0) LookFeel().getPositiveColor else LookFeel().getNegativeColor

    glassPane.updateInstantValue(strValue, color)
    prevClose = ticker.prevClose

    controller.fixedLeftSideTime = exchange.openTime(ticker.time)
  }

}


