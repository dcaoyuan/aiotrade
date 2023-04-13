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

import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.chart.AbstractChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.PathWidget


/**
 *
 * @author Caoyuan Deng
 */
class FibonacciRetracementsChart extends AbstractChart {
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
    val k = if (xs(1) - xs(0) == 0) 1F else (ys(1) - ys(0)) / (xs(1) - xs(0))
    val interval = ys(1) - ys(0)
    val xmin = math.min(xs(0), xs(1))
    val xmax = math.max(xs(0), xs(1))
        
    val y01 = ys(0)
    val y02 = ys(0) + interval * 0.236
    val y03 = ys(0) + interval * 0.382
    val y04 = ys(0) + interval * 0.500
    val y05 = ys(0) + interval * 0.618
    val y06 = ys(0) + interval * 0.763
    val y07 = ys(1)
    val y08 = ys(0) + interval * 1.618
    val y09 = ys(0) + interval * 2.0
    val y10 = ys(0) + interval * 2.618
    val y11 = ys(0) + interval * 3.0
    val y12 = ys(0) + interval * 4.237
        
    val label1 = addChild(new Label)
    label1.setFont(LookFeel().axisFont)
    label1.setForeground(color)
    label1.model.set(xs(0), y02 - 2, "23.6%")
    label1.plot
        
    val label2 = addChild(new Label)
    label2.setFont(LookFeel().axisFont)
    label2.setForeground(color)
    label2.model.set(xs(0), y03 - 2, "38.2%")
    label2.plot
        
    val label3 = addChild(new Label)
    label3.setFont(LookFeel().axisFont)
    label3.setForeground(color)
    label3.model.set(xs(0), y04 - 2, "50.0%")
    label3.plot

    val label4 = addChild(new Label)
    label4.setFont(LookFeel().axisFont)
    label4.setForeground(color)
    label4.model.set(xs(0), y05 - 2, "61.8%")
    label4.plot

    val label5 = addChild(new Label)
    label5.setFont(LookFeel().axisFont)
    label5.setForeground(color)
    label5.model.set(xs(0), y06 - 2, "76.3%")
    label5.plot

    val label6 = addChild(new Label)
    label6.setFont(LookFeel().axisFont)
    label6.setForeground(color)
    label6.model.set(xs(0), y07 - 2, "100%")
    label6.plot

    val label7 = addChild(new Label)
    label7.setFont(LookFeel().axisFont)
    label7.setForeground(color)
    label7.model.set(xs(0), y08 - 2, "161.8%")
    label7.plot

    val label8 = addChild(new Label)
    label8.setFont(LookFeel().axisFont)
    label8.setForeground(color)
    label8.model.set(xs(0), y09 - 2, "200%")
    label8.plot

    val label9 = addChild(new Label)
    label9.setFont(LookFeel().axisFont)
    label9.setForeground(color)
    label9.model.set(xs(0), y10 - 2, "261.8%")
    label9.plot

    val label10 = addChild(new Label)
    label10.setFont(LookFeel().axisFont)
    label10.setForeground(color)
    label10.model.set(xs(0), y11 - 2, "300%")
    label10.plot
        
    val label11 = addChild(new Label)
    label11.setFont(LookFeel().axisFont)
    label11.setForeground(color)
    label11.model.set(xs(0), y12 - 2, "423.7%")
    label11.plot
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
    var x1 = xb(0)
    var x2 = 0.0
    var bar = 1
    while (bar <= nBars) {
      x2 = xb(bar)
      if (x2 >= xmin && x2 <= xmax) {
        path.moveTo(x1, y01)
        path.lineTo(x2, y01)
        path.moveTo(x1, y02)
        path.lineTo(x2, y02)
        path.moveTo(x1, y03)
        path.lineTo(x2, y03)
        path.moveTo(x1, y04)
        path.lineTo(x2, y04)
        path.moveTo(x1, y05)
        path.lineTo(x2, y05)
        path.moveTo(x1, y06)
        path.lineTo(x2, y06)
        path.moveTo(x1, y07)
        path.lineTo(x2, y07)
        path.moveTo(x1, y08)
        path.lineTo(x2, y08)
        path.moveTo(x1, y09)
        path.lineTo(x2, y09)
        path.moveTo(x1, y10)
        path.lineTo(x2, y10)
        path.moveTo(x1, y11)
        path.lineTo(x2, y11)
        path.moveTo(x1, y12)
        path.lineTo(x2, y12)
      }
            
      /**
       * should avoid the 1 point intersect at the each path's end point,
       * especially in XOR mode
       */
      x1 = x2 + 1


      bar += nBarsCompressed
    }
        
  }
    
}



