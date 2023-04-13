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
import org.aiotrade.lib.charting.widget.StickBar
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
class StickChart extends AbstractChart {
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
        
    val posColor = LookFeel().getPositiveColor
    val negColor = LookFeel().getNegativeColor
        
    var color = posColor
    setForeground(color)
        
    val pathsWidget = addChild(new PathsWidget)
    val tp = new StickBar
    var bar = 1
    while (bar <= nBars) {
      var max = Double.MinValue
      var min = Double.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          val value = m.v(time)
          max = math.max(max, value)
          min = math.min(min, value)
        }

        i += 1
      }
            
            
      max = math.max(max, 0) // max not less than 0
      min = math.min(min, 0) // min not more than 0

      if (! (max == 0 && min == 0)) {
        var yValue = 0.0
        var yDatum = 0.0
        if (math.abs(max) > math.abs(min)) {
          color = posColor
          yValue = yv(max)
          yDatum = yv(min)
        } else {
          color = negColor
          yValue = yv(min)
          yDatum = yv(max)
        }
                
        val x = xb(bar)
        tp.setForeground(color)
        tp.model.set(x, yDatum, yValue, wBar, true, false)
        tp.plot
        pathsWidget.appendFrom(tp)

        if (x % Chart.MARK_INTERVAL == 0) {
          addMarkPoint(x.toInt, yValue.toInt)
        }
      }

      bar += nBarsCompressed
    }
        
  }
    
}

