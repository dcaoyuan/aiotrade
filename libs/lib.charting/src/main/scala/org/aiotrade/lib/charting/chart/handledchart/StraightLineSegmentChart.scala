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
import org.aiotrade.lib.charting.chart.segment.ValuePoint
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.PathWidget
import scala.collection.mutable.ArrayBuffer


/**
 *
 * @author Caoyuan Deng
 */
class StraightLineSegmentChart extends AbstractChart {
  final class Model extends WidgetModel {
    var points: ArrayBuffer[ValuePoint] = _
        
    def set(points: ArrayBuffer[ValuePoint]) {
      this.points = points
    }
  }

  type M = Model

  protected def createModel = new Model
    
  protected def plotChart {
    val m = model
        
    val color = LookFeel().drawingColor
    setForeground(color)
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
    val nPoints = m.points.size
    var i = 0
    while (i < nPoints - 1) {
      val x1 = xb(bt(m.points(i).t))
      val x2 = xb(bt(m.points(i + 1).t))
      val y1 = yv(m.points(i).v)
      val y2 = yv(m.points(i + 1).v)
            
      val dx = x2 - x1
      val dy = y2 - y1
            
      val k = if (dx == 0) 1F else dy / dx
            
      plotLineSegment(x1, y1, x2, y2, path)

      i += 1
    }
  }
    
}


