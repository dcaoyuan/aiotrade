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
package org.aiotrade.lib.charting.chart.handledchart

import java.awt.geom.GeneralPath
import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.chart.AbstractChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.Label

/**
 *
 * @author Caoyuan Deng
 */
class GannAngleChart extends AbstractChart {
  final class Model extends WidgetModel {
    var t1: Long = _
    var v1: Double = _
    var t2: Long = _
    var v2: Double = _
        
    def set(t1: Long, v1: Double, t2: Long, v2: Double) {
      this.t1 = t1
      this.v1 = v1
      this.t2 = t2
      this.v2 = v2
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel().drawingColor
    setForeground(color)
        
    val xs = Array(xb(bt(model.t1)), xb(bt(model.t2)))
    val ys = Array(yv(model.v1), yv(model.v2))
        
    val b1 = bt(m.t1)
    val b2 = bt(m.t2)
        
    var dBar = b2 - b1
    dBar = if (dBar == 0) 1 else dBar
        
    val rate = (model.v2 - model.v1) / dBar
                
    val label1 = addChild(new Label)
    label1.setFont(LookFeel().axisFont)
    label1.setForeground(color)
    label1.model.set(xs(1) + 2, ys(1), rate.toString)
    label1.plot
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
        
    plotOneDirection(xs, ys, true, true, true, path)
        
    xs(0) = xb(bt(model.t2))
    xs(1) = xb(bt(model.t1))
    ys(0) = yv(model.v2)
    ys(1) = yv(model.v1)
    plotOneDirection(xs, ys, false, false, false, path)
        
    xs(0) = xb(bt(model.t1))
    xs(1) = xb(bt(model.t2))
    ys(0) = yv(model.v2)
    ys(1) = yv(model.v1)
    plotOneDirection(xs, ys, true, false, false, path)
        
    xs(0) = xb(bt(model.t2))
    xs(1) = xb(bt(model.t1))
    ys(0) = yv(model.v1)
    ys(1) = yv(model.v2)
    plotOneDirection(xs, ys, false, false, false, path)
  }
    
  /**
   * should avoid dupliacte line
   */
  private def plotOneDirection(xs: Array[Double], ys: Array[Double], drawMain: Boolean, drawHorizontal: Boolean, drawVertical: Boolean, path: GeneralPath) {
    val m = model
        
    val k = if (xs(1) - xs(0) == 0) 1f else (ys(1) - ys(0)) / (xs(1) - xs(0))
    val xmin = math.min(xs(0), xs(1))
    val xmax = math.max(xs(0), xs(1))
    val ymin = math.min(ys(0), ys(1))
    val ymax = math.max(ys(0), ys(1))
        
    /** main angle */
    if (drawMain) {
      plotLineSegment(xs(0), ys(0), xs(1), ys(1), path)
    }
        
    /** horizontal angle */
    if (drawHorizontal) {
      plotLineSegment(xs(0), ys(0), xs(1), ys(0), path)
      plotLineSegment(xs(0), ys(1), xs(1), ys(1), path)
    }
        
    /** vertical angle */
    if (drawVertical) {
      plotVerticalLineSegment(bt(model.t1), ymin, ymax, path)
      plotVerticalLineSegment(bt(model.t2), ymin, ymax, path)
    }
        
    var xlast = xb(0)
    var bar = 1
    while (bar <= nBars) {
            
      val x1 = xlast
      val x2 = xb(bar)
      if (x1 >= xmin && x1 <= xmax && x2 >= xmin && x2 <= xmax) {
        var y1 = 0.0
        var y2 = 0.0
        var j = 2
        while (j <= 3) {
          y1 = GeomUtil.yOfLine(x1, xs(0), ys(0), k * j)
          y2 = GeomUtil.yOfLine(x2, xs(0), ys(0), k * j)
          if (y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax) {
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
          }
                    
          y1 = GeomUtil.yOfLine(x1, xs(0), ys(0), k / j)
          y2 = GeomUtil.yOfLine(x2, xs(0), ys(0), k / j)
          if (y1 >= ymin && y1 <= ymax && y2 >= ymin && y2 <= ymax) {
            path.moveTo(x1, y1)
            path.lineTo(x2, y2)
          }

          j += 1
        }
      }
            
      xlast = x2

      bar += nBarsCompressed
    }
        
  }
    
}
