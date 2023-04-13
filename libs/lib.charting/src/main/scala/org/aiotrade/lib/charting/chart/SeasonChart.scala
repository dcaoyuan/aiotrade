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

/**
 *
 * @author Caoyuan Deng
 */
class SeasonChart extends AbstractChart {
    
  protected def createModel: M = null
    
  protected def plotChart {
    val barWidthHalf = if (wBar - 2 > 0) ((wBar - 2) / 2).toInt else 0
        
    var bar = 1
    while (bar <= nBars) {
            
      /** @TODO */
      val pathWidget = addChild(new PathsWidget)
      val (path, pathFilled) = pathWidget.pathOf(getForeground)
            
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
                
        //            switch ((quoteSignSeries.get(d)).SEASON) {
        //                case 1:
        //                    shapes[bar].setColor(Color.green.darker().darker().darker().darker());
        //                    break;
        //                case 2:
        //                    shapes[bar].setColor(Color.red.darker().darker().darker().darker());
        //                    break;
        //                case 3:
        //                    shapes[bar].setColor(Color.yellow.darker().darker().darker().darker());
        //                    break;
        //                case 4:
        //                    shapes[bar].setColor(Color.blue.darker().darker().darker().darker());
        //                    break;
        //            }
        i += 1
      }
            
      var j = 0
      while (j < wBar) {
        path.moveTo(xb(bar) - barWidthHalf + j, yv(datumPlane.minValue))
        path.lineTo(xb(bar) - barWidthHalf + j, yv(datumPlane.maxValue))
        
        j += 1
      }

      bar += nBarsCompressed
    } // end for bar
        
  }
    
}
