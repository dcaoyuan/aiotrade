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

import java.awt.Color
import java.awt.Graphics
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.chart.segment.Shading
import org.aiotrade.lib.charting.laf.LookFeel


/**
 *
 * @author Caoyuan Deng
 */
class GradientChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _
    var shading: Shading = _

    def set(v: TVar[_], shading: Shading) {
      this.v = v
      this.shading = shading
    }
  }

  type M = Model

  protected def createModel= new Model
    
  protected def plotChart {
    /** this chart don't use path, just directly draw on screen */
  }
    
  override def render(g: Graphics) {
    val m = model
        
    val lower = model.shading.getLowerBound
    val upper = model.shading.getUpperBound
    val step  = model.shading.getNIntervals
        
    var color = LookFeel().stickChartColor
    setForeground(color)
        
    val radius = if (wBar < 2) 0 else ((wBar - 2) / 2).toInt
        
    var bar = 1
    while (bar <= nBars) {
            
      val time = tb(bar)
      if (ser.exists(time)) {
        val shades = m.v(time).asInstanceOf[Array[Double]]
        if (shades != null) {
          val centre = xb(bar)
          var prevRange = 0.0
          var j = 0
          while (j < shades.length) {
            val range = j * step + lower
                        
            var shade = shades(j)
            if (Null.not(shade)) {
              shade = (math.pow(shade, 1.0 / 3.0))
              color = new Color(shade.toFloat, shade.toFloat, shade.toFloat)
              g.setColor(color)
              g.fillRect((centre - radius - 1).toInt, yv(range).toInt, (2 * (radius + 1)).toInt, yv(prevRange).toInt - yv(range).toInt)
              //g.drawLine((int)barCentre, (int)yv(prevRange), (int)barCentre, (int)yv(range));
            }
            prevRange = range

            j += 1
          }
        }
      }

      bar += 1
    }
        
  }
    
}


