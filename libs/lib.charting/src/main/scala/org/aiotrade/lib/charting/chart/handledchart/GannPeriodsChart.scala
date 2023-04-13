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
import org.aiotrade.lib.charting.widget.PathWidget

/**
 *
 * @author Caoyuan Deng
 */
class GannPeriodsChart extends AbstractChart {
  final class Model extends WidgetModel {
    var t1: Long = _
        
    def set(t1: Long) {
      this.t1 = t1
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel().drawingColor
    setForeground(color)
        
    val numPn = 67
        
    val bs = new Array[Double](numPn * 2 + 1)
    bs(0) = bt(m.t1)
        
    val Pn = Array(7, 8, 9, 10, 11, 12,
                  18, 19, 20, 21,
                  28, 29, 30, 31,
                  42, 43, 44, 45, 46, 47, 48, 49,
                  57, 58, 59, 60, 61, 62, 63, 64, 65,
                  85, 86, 87, 88, 89, 90, 91, 92,
                  112, 113, 114, 115, 116, 117, 118, 119, 120,
                  150, 151, 152, 153, 154, 155, 156, 157,
                  175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185
    )
        
    var n = 1
    while (n < numPn) {
      /** positive side */
      bs(n * 2 + 1) = bs(0) + Pn(n)
      /** negative side */
      bs(n * 2 + 2) = bs(0) - Pn(n)

      n += 1
    }
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
    var bar = 1
    while (bar <= nBars) {
      if (bar == math.round(bs(0))) {
        plotVerticalLine(bar, path)
      }
            
      /** search if i is in Periods serials */
      var j = 1
      var break = false
      while (j < numPn * 2 && !break) {
        if (bar == math.round(bs(j)) || bar == math.round(bs(j + 1))) {
          plotVerticalLine(bar, path)
          break = true
        }

        j += 2
      }

      bar += 1
    }
  }
    
}
