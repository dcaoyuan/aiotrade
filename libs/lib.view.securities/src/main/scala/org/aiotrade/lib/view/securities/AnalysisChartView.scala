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
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.WithDrawingPaneHelper
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.view.pane.DrawingPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.charting.view.pane.XControlPane
import org.aiotrade.lib.charting.view.pane.YControlPane
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.lib.util.swing.GBC
import scala.collection.mutable


/**
 *
 * @author Caoyuan Deng
 */
object AnalysisChartView {
  /** all AnalysisQuoteChartView instances share the same type */
  private var quoteChartType: QuoteChart.Type = LookFeel().getQuoteChartType

  def switchAllQuoteChartType(tpe: QuoteChart.Type) {
    quoteChartType = AbstractQuoteChartView.internal_switchAllQuoteChartType(quoteChartType, tpe)
  }
}

import AnalysisChartView._
class AnalysisChartView(acontroller: ChartingController,
                        aquoteSer: QuoteSer,
                        empty: Boolean
) extends {
  private val compareIndicatorToChart = mutable.Map[QuoteCompareIndicator, QuoteChart]()
  private var withDrawingPaneHelper: WithDrawingPaneHelper = null
} with AbstractQuoteChartView(acontroller, aquoteSer, empty) with WithDrawingPane with Reactor {
    
  def this(controller: ChartingController, quoteSer: QuoteSer) = this(controller, quoteSer, false)
  def this() = this(null, null, true)
    
  override def init(controller: ChartingController, quoteSer: TSer) {
        
    /**
     * To avoid null withDrawingPaneHelper when getSelectedDrawing called by other
     * threads (such as dataLoadServer is running and fire a TSerEvent
     * to force a updateView() calling), we should create withDrawingPaneHelper before super.init call
     * (this will makes it be called before the code:
     *     this.mainSer.addSerChangeListener(serChangeListener)
     * in it's super's constructor: @See:ChartView#ChartView(ChartViewContainer, Ser)
     */
    withDrawingPaneHelper = new WithDrawingPaneHelper(this)

    super.init(controller, quoteSer)
  }
    
  protected def initComponents {
    xControlPane = new XControlPane(this, mainChartPane)
    xControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    yControlPane = new YControlPane(this, mainChartPane)
    yControlPane.setPreferredSize(new Dimension(10, CONTROL_HEIGHT))
        
    /** begin to set the layout: */
        
    setLayout(new GridBagLayout)
        
    /**
     * !NOTICE be ware of the components added order:
     * 1. add xControlPane, it will partly cover glassPane in SOUTH,
     * 2. add glassPane, it will exactly cover mainLayeredPane
     * 3. add mainLayeredPane.
     *
     * After that, xControlPane can accept its self mouse events, and so do
     * glassPane except the SOUTH part covered by xControlPane.
     *
     * And glassPane will forward mouse events to whom it covered.
     * @see GlassPane#processMouseEvent(MouseEvent) and
     *      GlassPane#processMouseMotionEvent(MouseEvent)
     */
        
    add(xControlPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
        
    add(glassPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    add(mainLayeredPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    /** add the yControlPane first, it will cover axisYPane partly in SOUTH */
    add(yControlPane, GBC(1, 0, 1, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(0, 0))
        
    /**
     * add the axisYPane in the same grid as yControlPane then, it will be
     * covered by yControlPane partly in SOUTH
     */
    add(axisYPane, GBC(1, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(0, 100))
        
    /** add axisXPane and dividentPane across 2 gridwidth horizontally, */
    add(axisXPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
        
    add(divisionPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
  }
    
  override protected def computeGeometry {
    super.computeGeometry
        
    if (getCompareIndicators.size > 0) {
      refreshQuoteCompareSer
      calcMaxMinWithComparingQuotes
    }
  }
    
  def quoteChartType: QuoteChart.Type = AnalysisChartView.quoteChartType
  def switchQuoteChartType(tpe: QuoteChart.Type) {
    switchAllQuoteChartType(tpe)
        
    repaint()
  }
    
  private def refreshQuoteCompareSer {
    val factorsForCompareIndicator = Array(
      new Factor("Begin of Time Frame", rb(1)),
      new Factor("End of Time Frame",   rb(nBars)),
      new Factor("Max Value", maxValue),
      new Factor("Min Value", minValue)
    )
        
    for (ser <- getCompareIndicators) {
      ser.factors = factorsForCompareIndicator.asInstanceOf[Array[Factor]]
    }
  }
    
  /** calculate maxValue and minValue again, including comparing quotes */
  private def calcMaxMinWithComparingQuotes {
    var maxValue1 = maxValue
    var minValue1 = minValue
    for (ser <- getCompareIndicators) {
      var i = -1
      while ({i += 1; i <= nBars}) {
        val time = tb(i)
        if (ser.exists(time)) {
          val compareHi = ser.high.double(time)
          val compareLo = ser.low.double(time)
          if (Null.not(compareHi) && Null.not(compareLo) && compareHi * compareLo != 0 ) {
            maxValue1 = math.max(maxValue1, compareHi)
            minValue1 = math.min(minValue1, compareLo)
          }
        }
      }
    }
        
        
    if (maxValue1 == minValue1) {
      maxValue1 += 1
    }
        
    setMaxMinValue(maxValue1, minValue1)
  }
    
  def getCompareIndicators = {
    compareIndicatorToChart.keySet
  }
    
  def getCompareIndicatorMapChart: mutable.Map[QuoteCompareIndicator, QuoteChart] = {
    compareIndicatorToChart
  }
    
  def addQuoteCompareChart(ser: QuoteCompareIndicator) {
    listenTo(ser)
        
    val chart = new QuoteChart
    compareIndicatorToChart.put(ser, chart)
        
    val depth = Pane.DEPTH_CHART_BEGIN + compareIndicatorToChart.size
        
    chart.model.set(
      ser.open,
      ser.high,
      ser.low,
      ser.close)
        
    chart.set(mainChartPane, ser, depth)
    mainChartPane.putChart(chart)
        
    repaint()
  }
    
  def removeQuoteCompareChart(ser: QuoteCompareIndicator) {
    deafTo(ser)
        
    compareIndicatorToChart.get(ser) foreach {chart =>
      mainChartPane.removeChart(chart)
      compareIndicatorToChart.remove(ser)
            
      repaint()
    }
  }
    
  /**
   * implement of WithDrawingPane
   * -------------------------------------------------------
   */
    
  def selectedDrawing: DrawingPane = withDrawingPaneHelper.selectedDrawing
  def selectedDrawing_=(drawing: DrawingPane) {
    withDrawingPaneHelper.selectedDrawing = drawing
  }
    
  def addDrawing(descriptor: DrawingDescriptor, drawing: DrawingPane) {
    withDrawingPaneHelper.addDrawing(descriptor, drawing)
  }
    
  def deleteDrawing(descriptor: DrawingDescriptor) {
    withDrawingPaneHelper.deleteDrawing(descriptor)
  }
    
  def descriptorToDrawing: mutable.Map[DrawingDescriptor, DrawingPane] = {
    withDrawingPaneHelper.descriptorToDrawing
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
}

