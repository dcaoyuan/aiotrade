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
package org.aiotrade.lib.charting.view

import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.charting.chart.ChartFactory
import org.aiotrade.lib.charting.chart.GridChart
import org.aiotrade.lib.charting.chart.ProfileChart
import org.aiotrade.lib.charting.chart.GradientChart
import org.aiotrade.lib.charting.chart.StickChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.util.swing.GBC
import scala.collection.mutable.HashSet


/**
 *
 * @author Caoyuan Deng
 */
class IndicatorChartView($controller: ChartingController, $mainSer: TSer, $empty: Boolean
) extends ChartView($controller, $mainSer, $empty) {
    
  def this(controller: ChartingController, mainSer: TSer) = this(controller, mainSer, false)
  def this() = this(null, null, true)

  /**
   * Layout of IndicatorView
   *
   * title pane is intersect on chart pane's north
   * +----------------------------------------------------+-----+
   * |    title (0,0)                                     |     |
   * +----------------------------------------------------+     |
   * |    mainLayeredPane (0, 0)                          |     |
   * |    chartPane                                       |axisy|
   * |    drawingPane                                     |(1,0)|
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * |                                                    |     |
   * +----------------------------------------------------+-----+
   * |    axisx                                                 |
   * +----------------------------------------------------------+
   */
  protected def initComponents {
    setLayout(new GridBagLayout)
        
    add(glassPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    add(mainLayeredPane, GBC(0, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(100, 100 - 100 / 6.18))
        
    add(axisYPane, GBC(1, 0, 1, 1).
        setAnchor(GridBagConstraints.CENTER).
        setFill(GridBagConstraints.BOTH).
        setWeight(0, 100))
        
    add(divisionPane, GBC(0, GridBagConstraints.RELATIVE, 2, 1).
        setAnchor(GridBagConstraints.SOUTH).
        setFill(GridBagConstraints.HORIZONTAL).
        setWeight(100, 0))
  }
    
    
  protected def putChartsOfMainSer {
    var depth = Pane.DEPTH_CHART_BEGIN
    var depthGradient = Pane.DEPTH_GRADIENT_BEGIN
        
    for (ser <- allSers) {
      /** add charts */
      for (v <- ser.vars if v.plot != Plot.None) {
        val chart = ChartFactory.createVarChart(v)
        if (chart != null) {
          val vars = mainSerChartToVars.get(chart) getOrElse {
            val x = HashSet[TVar[_]]()
            mainSerChartToVars.put(chart, x)
            x
          }
          vars.add(v)
                    
          chart match {
            case _: GradientChart => chart.depth = depthGradient; depthGradient -= 1
            case _: ProfileChart => chart.depth = depthGradient; depthGradient -= 1
            case _: StickChart => chart.depth = -8
            case _ => chart.depth = depth; depth += 1
          }

          chart.set(mainChartPane, ser)
          mainChartPane.putChart(chart)
        }
      }
            
      /** plot grid */
      val grids = mainSer.grids
      if (grids != null && grids.length > 0) {
        val gridChart = new GridChart
                
        gridChart.model.set(grids, GridChart.Direction.Horizontal)
        gridChart.set(mainChartPane, null, Pane.DEPTH_DRAWING)
                
        mainChartPane.putChart(gridChart)
      }
    }
  }
    
  override def computeMaxMin {
    var min = Double.MaxValue
    var max = Double.MinValue

    var shouldMinBeZero = false
    var i = 1
    while (i <= nBars) {
      val time = tb(i)
      if (mainSer.exists(time)) {
        for (v <- mainSer.vars if v.plot != Plot.None;
             value = v.double(time) if Null.not(value)
        ) {
          if (v.plot == Plot.Volume) {
            shouldMinBeZero = true
          }
          max = math.max(max, value)
          min = math.min(min, value)
        }
      }

      i += 1
    }
        
    if (max == min) max += 1
    if (shouldMinBeZero) min = 0

    setMaxMinValue(max, min)
  }
    
  override def popupToDesktop {
    val popupView = new PopupIndicatorChartView(controller, mainSer)
    val alwaysOnTop = true
    val dim = new Dimension(getWidth, 200)
        
    controller.popupViewToDesktop(popupView, dim, alwaysOnTop, false)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
}


