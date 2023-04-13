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
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.securities.QuoteSer


/**
 *
 * @author Caoyuan Deng
 */
class DiagonalChart extends AbstractChart {
  final class Model extends WidgetModel {
    var originTime: Long = _
    var b0: Double = _
    var step: Double = _
    var k: Double = _
    var color: Color = _
        
    def set(originTime: Long, b0: Double, step: Double, k: Double, color: Color) {
      this.originTime = originTime
      this.b0 = b0
      this.step = step
      this.k = k
      this.color = color
    }
  }

  type M = Model
    
  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
    val quoteSer = this.ser.asInstanceOf[QuoteSer]
        
    val color = m.color
    setForeground(color)
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
        
    val a0 = 0 //(int)(originTime - quoteSeries.get(0).time);  // originTime's x-axis in days
    val a1 = 0
    val a2 = nBars - 1
    var i = 0
    while (i < 10) {
      val b1 = (a1 - a0 + i * m.step) * 365.25 / 365 * m.k + m.b0
      val b2 = (a2 - a0 + i * m.step) * 365.25 / 365 * m.k + m.b0
      path.moveTo(xb(a1), yv(b1))
      path.lineTo(xb(a2), yv(b2))

      i += 1
    }
        
  }
    
}
