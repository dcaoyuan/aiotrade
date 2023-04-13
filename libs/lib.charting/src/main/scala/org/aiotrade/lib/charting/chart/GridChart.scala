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

import org.aiotrade.lib.charting.chart.Chart.StrockType
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.math.timeseries.Null

/**
 *
 * @author Caoyuan Deng
 */
object GridChart {
  abstract class Direction
  object Direction {
    case object Horizontal extends Direction
    case object Vertical   extends Direction
  }
}

class GridChart extends AbstractChart {
  import GridChart._

  final class Model extends WidgetModel {

    var values: Array[Double] = _
    var direction: Direction = _

    def set(values: Array[Double], direction: Direction) {
      this.values = values
      this.direction = direction
    }
  }

  type M = Model
  
  protected def createModel = new Model

  override  def strockType: StrockType = StrockType.Dash

  protected def plotChart {
    val m = model

    val color = LookFeel().borderColor.darker.darker
    setForeground(color)

    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath

    val w = datumPlane.getWidth - 1
    val h = datumPlane.getHeight - 1

    val size = m.values.length
    var i = 0
    while (i < size) {
      val value = m.values(i)
      if (Null.not(value)) {
        m.direction match {
          case Direction.Horizontal =>
            val y = yv(value)
            path.moveTo(0, y)
            path.lineTo(w, y)
          case Direction.Vertical =>
            val x = value
            path.moveTo(x, 0)
            path.lineTo(x, h)
          case _ =>
        }
      }

      i += 1
    }
  }
}



