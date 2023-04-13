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
package org.aiotrade.lib.charting.chart

import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.GeneralPath
import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.charting.view.pane.DatumPlane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.AbstractWidget
import scala.collection.mutable.ArrayBuffer


/**
 *
 * @author Caoyuan Deng
 */
abstract class AbstractChart extends AbstractWidget with Chart {

  private val markPoints = new ArrayBuffer[Point] // used to draw selected mark
    
  /** Component that charts x-y based on */
  protected var datumPlane: DatumPlane = _
    
  /** baseSer that will be got from: datumPlane.baseSer */
  protected var baseSer: BaseTSer = _
    
  private var _ser: TSer = _
    
  /**
   * the depth of this chart in pane,
   * the chart's container will decide the chart's defaultColor according to the depth
   */
  private var _depth: Int = _
  private var _strockWidth = 1.0F
  private var _strockType: Chart.StrockType = Chart.StrockType.Base
  private var _isSelected: Boolean = _
  private var _firstPlotting: Boolean = _

  /**
   * Keep convenient references to datumPane's geometry, thus we can also
   * shield the changes from datumPane.
   */
  protected var nBars: Int = _
  protected var wBar: Double = _
    

  protected var wSeg = Chart.MIN_SEGMENT_WIDTH
  protected var nSegs: Int = _
    
  protected var nBarsCompressed = 1
    
  /** @TODO */
  override def isContainerOnly: Boolean = {
    true
  }
    
  /**
   * NOTICE
   * It's always better to set datumPlane here.
   * After call following set(,,,) methods, the chart can be put in the any
   * pane that has this datumPlane referenced by pane.putChart() for
   * automatical drawing, or, can be drawn on these pane by call pane.render(g)
   * initiatively (such as mouse cursor chart).
   * So, do not try to separate a setPane(Pane) method.
   */
  def set(datumPane: DatumPlane, ser: TSer, depth: Int) {
    this.datumPlane = datumPane
    this.ser = ser
    this.depth = depth
  }
    
  def set(datumPane: DatumPlane, ser: TSer) {
    set(datumPane, ser, this._depth)
  }
    
  def isFirstPlotting = _firstPlotting
  def isFirstPlotting_=(b: Boolean) {
    _firstPlotting = b
  }
    
    
  /**
   * present only prepare the chart's pathSegs and textSegs, but not really render,
   * should call render(Graphics2D g) to render this chart upon g
   */
  protected def plotWidget {
    baseSer = datumPlane.baseSer
    nBars = datumPlane.nBars
    wBar = datumPlane.wBar
        
    wSeg = math.max(wBar, Chart.MIN_SEGMENT_WIDTH).toInt
    nSegs = (nBars * wBar / wSeg).toInt + 1
        
    nBarsCompressed = if (wBar >= 1) 1 else (1 / wBar).toInt
        
    reset
        
    plotChart
  }
    
  protected def plotChart
    
  override def reset {
    super.reset

    markPoints.clear
  }
    
  protected def addMarkPoint(x: Int, y: Int) {
    markPoints += new Point(x, y)
  }
    
  /**
   * use intersects instead of contains here, contains means:
   * A specified coordinate is inside the boundary of this chart.
   * But not each kind of chart has boundary.
   * For example: in the case of Line, objects it always contains nothing,
   * since a line contains no area.
   */
  protected def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    false
  }
    
  protected def renderWidget(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]
        
    val w = strockWidth.toInt
    val stroke = strockType match {
      case Chart.StrockType.Base =>
        if (w <= Chart.BASE_STROKES.length) Chart.BASE_STROKES(w - 1)
        else new BasicStroke(w)
      case Chart.StrockType.Dash =>
        if (w <= Chart.DASH_STROKES.length) Chart.DASH_STROKES(w - 1)
        else new BasicStroke(w, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, Chart.DASH_PATTERN, 0)
      case _ => new BasicStroke(w)
    }
    g.setStroke(stroke)
        
    if (isSelected) {
      markPoints foreach (renderMarkAtPoint(g, _))
    }
  }
    
  private def renderMarkAtPoint(g: Graphics, point: Point) {
    g.setColor(LookFeel().handleColor)
    g.fillRect(point.x - 2, point.y - 2, 5, 5)
  }
    
  def depth = _depth
  def depth_=(depth: Int) {
    _depth = depth
  }
    
    
  def isSelected = _isSelected
  def isSelected_=(b: Boolean) {
    _isSelected = b
  }
    
  def setStrock(width: Int, tpe: Chart.StrockType) {
    _strockWidth = width
    _strockType = tpe
  }
    
  /**
   * @return width of chart. not width of canvas!
   */
  def strockWidth: Double = _strockWidth
  def strockType: Chart.StrockType = _strockType
    
  def ser = _ser
  def ser_=(ser: TSer) {
    _ser = ser
  }
    
    
  /**
   * Translate barIndex to X point for drawing
   *
   * @param barIndex: index of bars, start from 1 to nBars
   */
  final protected def xb(barIndex: Int): Double = {
    datumPlane.xb(barIndex)
  }

  /**
   * Translate value to Y point for drawing
   * @param value
   */
  final protected def yv(value: Double): Double = {
    datumPlane.yv(value)
  }

  final protected def bx(x: Double): Int = {
    datumPlane.bx(x)
  }

  final protected def vy(y: Double): Double = {
    datumPlane.vy(y)
  }

  /**
   * @return row in ser corresponding to barIndex
   */
  final protected def rb(barIndex: Int): Int = {
    datumPlane.rb(barIndex)
  }

  final protected def br(row: Int): Int = {
    datumPlane.br(row)
  }

  /**
   * @return segment index corresponding to barIdx
   */
  final protected def sb(barIdx: Int): Int = {
    (barIdx * wBar / wSeg).toInt + 1
  }

  /* final */ protected def bs(segIdx: Int): Int = {
    (((segIdx - 1) * wSeg) / wBar).toInt
  }

  /**
   * @param barIdx: index of bars, start from 1 to nBars
   */
  final protected def tb(barIdx: Int): Long = {
    datumPlane.tb(barIdx)
  }

  final protected def bt(time: Long): Int = {
    datumPlane.bt(time)
  }

  protected def plotLine(xBase: Double, yBase: Double, k: Double,  path: GeneralPath) {
    val xBeg = 0
    val yBeg = GeomUtil.yOfLine(xBeg, xBase, yBase, k)
    val xEnd = datumPlane.getWidth
    val yEnd = GeomUtil.yOfLine(xEnd, xBase, yBase, k)
    path.moveTo(xBeg, yBeg)
    path.lineTo(xEnd, yEnd)
  }

  protected def plotVerticalLine(bar: Int, path: GeneralPath) {
    val x = xb(bar)
    val yBeg = datumPlane.yCanvasLower
    val yEnd = datumPlane.yCanvasUpper
    path.moveTo(x, yBeg)
    path.lineTo(x, yEnd)
  }

  protected def plotLineSegment(xBeg: Double, yBeg: Double, xEnd: Double, yEnd: Double, path: GeneralPath) {
    path.moveTo(xBeg, yBeg)
    path.lineTo(xEnd, yEnd)
  }

  protected def plotVerticalLineSegment(bar: Int, yBeg: Double, yEnd: Double, path: GeneralPath) {
    val x = xb(bar)
    path.moveTo(x, yBeg)
    path.lineTo(x, yEnd)
  }

  /** compare according to the depth of chart, used for SortedSet<Chart> */
  final def compare(another: Chart): Int = {
    if (this.depth == another.depth) {
      if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
    } else {
      if (this.depth < another.depth) -1 else 1
    }
  }

    
  /**
   * @ReferenceOnly methods:
   * ----------------------------------------------------------------
   */
    
  /**
   * @deprecated
   */
  @deprecated private def plotLine_seg(xCenter: Double, yCenter: Double, k: Double, path: GeneralPath) {
    var xlast = xb(0) // bar 0
    var ylast = Null.Double
    var bar = 1
    while (bar <= nBars) {
            
      var x1 = xlast
      var y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k)
            
      var x2 = xb(bar)
      var y2 = GeomUtil.yOfLine(x2, xCenter, yCenter, k)
            
      /**
       * if (xlast, y1) is the same point of (xlast, ylast), let
       *     x1 = xlast + 1
       * to avoid the 1 point intersect at the each path's
       * end point, especially in XOR mode:
       */
      var break = false
      while (x1 < x2 && !break) {
        if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
          x1 +=1
          y1 = GeomUtil.yOfLine(x1, xCenter, yCenter, k)
        } else {
          break = true
        }
      }
            
      path.moveTo(x1, y1)
      path.lineTo(x2, y2)
            
      ylast = y2
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotLineSegment_seg(xBeg: Double, yBeg: Double, xEnd: Double, yEnd: Double, path: GeneralPath) {
    val dx = xEnd - xBeg
    val dy = yEnd - yBeg
        
    val k: Double = if (dx == 0) 1 else dy / dx
    val xmin = math.min(xBeg, xEnd)
    val xmax = math.max(xBeg, xEnd)
    val ymin = math.min(yBeg, yEnd)
    val ymax = math.max(yBeg, yEnd)
        
    var xlast = xb(0) // bar 0
    var ylast = Null.Double
    var bar = 1
    while (bar <= nBars) {
            
      var x1 = xlast
      var x2 = xb(bar)
            
      var y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k)
      var y2 = GeomUtil.yOfLine(x2, xBeg, yBeg, k)
            
            
      if (x1 >= xmin && x1 <= xmax && x2 >= xmin && x2 <= xmax &&
          y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax
      ) {
                
        /**
         * if (xlast, y1) is the same point of (xlast, ylast), let
         *     x1 = xlast + 1
         * to avoid the 1 point intersect at the each path's
         * end point, especially in XOR mode:
         */
        var break = false
        while (x1 < x2 && !break) {
          if (GeomUtil.samePoint(x1, y1, xlast, ylast)) {
            x1 += 1
            y1 = GeomUtil.yOfLine(x1, xBeg, yBeg, k)
          } else {
            break = true
          }
        }
                
        path.moveTo(x1, y1)
        path.lineTo(x2, y2)
                
        ylast = y2
                
      }
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotVerticalLine_seg(bar: Int, path: GeneralPath) {
    if (bar >= 1 && bar <= nBars) {
            
      val y1 = yv(datumPlane.minValue)
      val y2 = yv(datumPlane.minValue)
      val x = xb(bar)
            
      path.moveTo(x, y1)
      path.lineTo(x, y2)
            
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotVerticalLineSegment_seg(bar: Int, yBeg: Double, yEnd: Double, path: GeneralPath) {
    if (bar >= 1 && bar <= nBars) {
            
      val x = xb(bar)
            
      path.moveTo(x, yBeg)
      path.lineTo(x, yEnd)
            
    }
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotArc_seg(xCenter: Double, yCenter: Double, radius: Double, path: GeneralPath) {
    plotHalfArc_seg(xCenter, yCenter, radius, true, path)
    plotHalfArc_seg(xCenter, yCenter, radius, false, path)
  }
    
  /**
   * @deprecated
   */
  @deprecated private def plotHalfArc_seg(xCenter: Double, yCenter: Double, radius: Double, positiveSide: Boolean, path: GeneralPath) {
    var xlast = xb(0) // bar 0
    var ylast = Null.Double
    var bar = 1
    while (bar <= nBars) {
            
      val x1 = xlast
      val x2 = xb(bar)
            
      /** draw positive arc from x1 to x2 */
      val y1 = GeomUtil.yOfCircle(x1, xCenter, yCenter, radius, positiveSide)
            
      /**
       * if (xlast, y1) is the same point of (xlast, ylast), let
       *     x1 = xlast + 1
       * to avoid the 1 point intersect at the each path's
       * end point, especially in XOR mode:
       *
       * In case of: step = (xfactor <= 2) ? 3 : 1, following code could be ignored:
       *
       * if (isTheSamePoint(x1, y1, xlast, ylast)) {
       *     x1 = xlast + 1;
       *     y1 = yOfArc(x1, xCenter, yCenter, radius, positiveSide);
       * }
       *
       */
            
            
      if (Null.not(y1)) {
        path.moveTo(x1, y1)

        var x = x1 + 1
        while (x <= x2) {
          val y =  GeomUtil.yOfCircle(x, xCenter, yCenter, radius, positiveSide)
                    
          if (Null.not(y)) {
            path.lineTo(x, y)
                        
            ylast = y
          }
          x += 1
        }
      }
            
      xlast = x2

      bar += nBarsCompressed
    }
  }
    
    
}
