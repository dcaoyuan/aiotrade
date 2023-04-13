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

import org.aiotrade.lib.charting.widget.PathsWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class PolyLineChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[Double] = _
        
    def set(v: TVar[Double]) {
      this.v = v
    }
  }

  type M = Model
  
  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
    val color = LookFeel().getChartColor(depth)
    setForeground(color)
        
    val pathsWidget = addChild(new PathsWidget)
    val tp = new LineSegment
    var y1 = Null.Double   // for prev
    var y2 = Null.Double   // for curr
    var bar = 1
    while (bar <= nBars) {
      var value = Null.Double
      var max = Double.MinValue
      var min = Double.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val  time = tb(bar + i)
        if (ser.exists(time)) {
          value = m.v(time)
          max = math.max(max, value)
          min = math.min(min, value)
        }

        i += 1
      }
            
      if (Null.not(value)) {
        tp.setForeground(color)
                
        y2 = yv(value)
        if (nBarsCompressed > 1) {
          /** draw a vertical line to cover the min to max */
          val x = xb(bar)
          tp.model.set(x, yv(min), x, yv(max))
        } else {
          if (Null.not(y1)) {
            /**
             * x1 shoud be decided here, it may not equal prev x2:
             * think about the case of on calendar day mode
             */
            val x1 = xb(bar - nBarsCompressed)
            val x2 = xb(bar)
            tp.model.set(x1, y1, x2, y2)
                        
            if (x2 % Chart.MARK_INTERVAL == 0) {
              addMarkPoint(x2.toInt, y2.toInt)
            }
                        
          }
        }
        y1 = y2
                
        tp.plot
        pathsWidget.appendFrom(tp)
      }

      bar += nBarsCompressed
    }
        
  }
    
}
