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
package org.aiotrade.lib.charting.view.pane

import java.awt.AlphaComposite
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.SwingUtilities
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.charting.chart.CursorChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.MouseCursorObserver
import org.aiotrade.lib.charting.widget.Widget
import scala.collection.immutable.SortedSet
import scala.collection.immutable.TreeSet
import scala.collection.mutable.ArrayBuffer

protected abstract class RenderStrategy
protected object RenderStrategy {
  case object NoneBuffer extends RenderStrategy
  case object BufferedImage extends RenderStrategy
}

/**
 *
 * @author Caoyuan Deng
 */
object Pane {
  val DEPTH_FRONT = 1000
  /** usually for quote chart, so charts of other indicatos can begin from 0: */
  val DEPTH_DEFAULT = -1
  val DEPTH_CHART_BEGIN = 0
  /** usually for drawing chart, it will be in front: */
  val DEPTH_DRAWING = 100
  val DEPTH_GRADIENT_BEGIN = -10
  val DEPTH_INVISIBLE = -100
}

import Pane._
abstract class Pane(val view: ChartView, $datumPlane: DatumPlane) extends JComponent {

  private val widgets = new ArrayBuffer[Widget]
  private var _referCursorValue: Double = _
  private var _isAutoReferCursorValue: Boolean = true
  private var _charts = new TreeSet[Chart]
  private var referCursorChart: CursorChart = _
  private var mouseCursorChart: CursorChart = _
  private var backRenderBuffer: BufferedImage = _
  private var wBackRenderBuffer: Int = _
  private var hBackRenderBuffer: Int = _
  private var renderStrategy: RenderStrategy = RenderStrategy.NoneBuffer
  
  val datumPlane = if ($datumPlane == null) {
    /** if a null datumPlane given, we assume it will be just me, such as a ChartPane */
    assert(this.isInstanceOf[DatumPlane], "A null datumPlane given, the datumPlane should be me!")
    this.asInstanceOf[DatumPlane]
  } else $datumPlane

  if (this.isInstanceOf[WithCursorChart]) {
    createCursorChart(this.datumPlane)
    view.controller.addObserver(this, new MouseCursorObserver {
        val updater: Updater = {
          case _: ChartingController =>
            paintChartOnXORMode(mouseCursorChart)
        }
      })
  }

  setDoubleBuffered(true)

  protected def setRenderStrategy(renderStrategy: RenderStrategy) {
    this.renderStrategy = renderStrategy
  }

  /** helper method for implementing WithCursorChart */
  private def createCursorChart(datumPlane: DatumPlane) {
    if (!(this.isInstanceOf[WithCursorChart])) {
      assert(false, "Only WithCursorChart supports this method!")
      return
    }

    /** create refer cursor chart */
    referCursorChart = this.asInstanceOf[WithCursorChart].createCursorChartInstance(datumPlane)
    referCursorChart.setType(CursorChart.Type.Refer)
    referCursorChart.set(datumPlane, view.controller.baseSer, DEPTH_DEFAULT - 1)

    /** create mouse cursor chart */
    mouseCursorChart = this.asInstanceOf[WithCursorChart].createCursorChartInstance(datumPlane)
    mouseCursorChart.setType(CursorChart.Type.Mouse)
    mouseCursorChart.set(datumPlane, view.controller.baseSer, DEPTH_FRONT)

    referCursorChart.isFirstPlotting = true
    mouseCursorChart.isFirstPlotting = true
  }

  /**
   * @NOTICE
   * Should reset: chart.setFirstPlotting(true) when ever repaint() or
   * paint() happened.
   *
   * @param chart, chart to be plot and paint
   * @see #postPaintComponent()
   */
  protected def paintChartOnXORMode(chart: Chart) {
    val g = getGraphics
    if (g != null) {
      try {
        g.setXORMode(getBackground)

        if (chart.isFirstPlotting) {
          chart.isFirstPlotting = false
        } else {
          /** erase previous drawing */
          chart.render(g)
        }
        /** current new drawing */
        chart.plot
        chart.render(g)

        /** restore to paintMode */
        g.setPaintMode
      } finally {
        g.dispose
      }
    }
  }

  override protected def paintComponent(g: Graphics) {
    prePaintComponent

    if (renderStrategy == RenderStrategy.NoneBuffer) {
      render(g)
    } else {
      paintToBackRenderBuffer
      g.drawImage(getBackRenderBuffer, 0, 0, this)
    }

    postPaintComponent
  }

  protected def prePaintComponent {
    assert(datumPlane != null, "datumPlane can not be null!")
    /**
     * @NOTICE
     * The repaint order in Java Swing is not certain, the axisXPane and
     * axisYPane etc may be painted earlier than datumPlane, that will cause
     * incorrect geometry for axisXPane and axisYPane, for example: the
     * maxValue and minValue changed, but the datumPlane's computeGeometry()
     * is still not been called yet. Although we can let datumPlane aware of
     * all of the changes that may affect geometry, but the simplest way is
     * just ask the Objects that depend on datumPlane and may be painted
     * earlier than datumPlane, call datumPlane.computeGeomtry() first.
     */
    datumPlane.computeGeometry
  }

  protected def postPaintComponent {
    if (this.isInstanceOf[WithCursorChart]) {
      referCursorChart.isFirstPlotting = true
      mouseCursorChart.isFirstPlotting = true
    }
  }

  private def processUserRenderOptions(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]

    if (LookFeel().isAntiAlias) {
      g.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON)
    }

    if (isOpaque) {
      /**
       * Process background by self,
       *
       * @NOTICE
       * don't forget to setBackgroud() to keep this component's properties consistent
       */
      setBackground(LookFeel().backgroundColor)
      g.setColor(getBackground)
      g.fillRect(0, 0, getWidth, getHeight)
    }

    setFont(LookFeel().axisFont)
    g.setFont(getFont)
  }

  /**
   * @NOTICE
   * charts should be set() here only, because this method will be called in
   * paintComponent() after fetching some very important parameters which will
   * be used by charts' plotting;
   */
  private def render(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]

    processUserRenderOptions(g)

    /** plot and render segments added by plotMore() */
    widgets.clear
    plotPane
    for (widget <- widgets) {
      widget.render(g)
    }

    /** plot and render charts that have been put */
    for (chart <- _charts) {
      chart.plot
      chart.render(g)
    }

    /** plot and render refer cursor chart */
    if (this.isInstanceOf[WithCursorChart]) {
      referCursorChart.plot
      referCursorChart.render(g)
    }
  }

  private final def getBackRenderBuffer: BufferedImage = {
    backRenderBuffer
  }

  private final def checkBackRenderBuffer {
    if (backRenderBuffer != null &&
        (wBackRenderBuffer != getWidth || hBackRenderBuffer != getHeight)) {
      backRenderBuffer.flush
      backRenderBuffer = null
    }

    if (backRenderBuffer == null) {
      wBackRenderBuffer = getWidth
      hBackRenderBuffer = getHeight
      backRenderBuffer = createBackRenderBuffer
    }
  }

  final private def createBackRenderBuffer: BufferedImage = {
    new BufferedImage(wBackRenderBuffer, hBackRenderBuffer, BufferedImage.TYPE_INT_ARGB)
  }

  final private def paintToBackRenderBuffer {
    checkBackRenderBuffer
    val g = getBackRenderBuffer.getGraphics.asInstanceOf[Graphics2D]

    /** clear image with transparent alpha */
    val backupComposite = g.getComposite
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR, 0.0f))
    g.fillRect(0, 0, wBackRenderBuffer, hBackRenderBuffer)
    g.setComposite(backupComposite)

    render(g)

    g.dispose
  }

  protected def charts: SortedSet[Chart] = _charts
  
  def putChart(chart: Chart) {
    _charts += chart
  }

  def containsChart(chart: Chart): Boolean = {
    _charts.contains(chart)
  }

  def removeChart(chart: Chart) {
    _charts -= chart
  }

  def addWidget[T <: Widget](widget: T): T = {
    widgets += widget
    widget
  }

  def referCursorValue: Double = _referCursorValue
  def referCursorValue_=(referCursorValue: Double) {
    this._referCursorValue = referCursorValue
  }


  def isAutoReferCursorValue: Boolean = _isAutoReferCursorValue
  def isAutoReferCursorValue_=(b: Boolean) {
    this._isAutoReferCursorValue = b
  }

  def chartAt(x: Int, y: Int): Chart = {
    for (chart <- _charts) {
      if (chart.isInstanceOf[CursorChart]) {
      } else if (chart.hits(x, y)) {
        return chart
      }
    }
    null
  }

  def isCursorCrossVisible: Boolean = {
    view.controller.isCursorCrossLineVisible
  }

  /*- @RESERVER
   * MouseEvent retargetedEvent = new MouseEvent(target,
   *   e.getID(),
   *   e.getWhen(),
   *   e.getModifiers() | e.getModifiersEx(),
   *   e.getX(),
   *   e.getY(),
   *   e.getClickCount(),
   *   e.isPopupTrigger());
   *
   * Helper method
   */
  protected def forwardMouseEvent(source: Component, target: Component, e: MouseEvent) {
    if (target != null && target.isVisible) {
      val retargetedEvent = SwingUtilities.convertMouseEvent(source, e, target)
      target.dispatchEvent(retargetedEvent)
    }
  }

  /**
   * plot more custom segments into segsPlotMore
   *   -- beyond the charts put by putCharts()
   */
  protected def plotPane {
  }

  /**
   * The releasing is required for preventing memory leaks.
   */
  @throws(classOf[Throwable])
  override protected def finalize {
    view.controller.removeObserversOf(this)
    view.removeObserversOf(this)

    super.finalize
  }

  /**
   * @Deprecated
   * @see AbstractChart#CompareTo(Chart)
   *
   * sort charts according to its depth
   */
  @deprecated private def sortCharts(charts: Set[Chart] ): Array[Chart] = {
    val sortedCharts = charts.toArray
    var i = 0
    var break = false
    while (i < sortedCharts.length && !break) {
      var exchangeHappened = false
      for (j <- sortedCharts.length - 2 to i) {
        if (sortedCharts(j + 1).depth < sortedCharts(j).depth) {
          val tmp = sortedCharts(j + 1)
          sortedCharts(j + 1) = sortedCharts(j)
          sortedCharts(j) = tmp

          exchangeHappened = true
        }
      }

      if (!exchangeHappened) {
        break = true
      }
      
      i += 1
    }

    sortedCharts
  }
}







