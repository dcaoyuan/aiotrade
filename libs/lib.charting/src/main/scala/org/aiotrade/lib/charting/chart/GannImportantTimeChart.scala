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
import java.util.Calendar
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.util.NaturalCalendar

/**
 *
 * @author Caoyuan Deng
 */
class GannImportantTimeChart extends AbstractChart {
    
  protected def createModel: M = null
    
  protected def plotChart {
    val cal = Calendar.getInstance
        
    val color = Color.red.darker.darker.darker
    setForeground(color)
        
    val barWidthHalf = if (wBar - 2 > 0) ((wBar - 2) / 2).intValue else 0
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
    /** don't apply nBarsCompressed to it */
    var bar = 1
    while (bar <= nBars) {
            
      var gannImportantTime = -1
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
                
        cal.setTimeInMillis(time)
        gannImportantTime = NaturalCalendar.getGannImportantDate(cal.getTime)

        i += 1
      }
            
      if (gannImportantTime > 0) {
        var j = 0
        while (j < wBar) {
          path.moveTo(xb(bar) - barWidthHalf + j, yv(datumPlane.minValue))
          path.lineTo(xb(bar) - barWidthHalf + j, yv(datumPlane.maxValue))

          j += 1
        }
      }

      bar += nBarsCompressed
    }
        
  }
    
}
