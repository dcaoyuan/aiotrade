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

import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.charting.widget.PathsWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar

/**
 *
 * @author Caoyuan Deng
 */
class ZigzagChart extends AbstractChart {
  final class Model extends WidgetModel {
    var v: TVar[_] = _
        
    def set(v: TVar[_]) {
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
    val time0 = ser.timestamps(0)
    var time1 = getFirstTimeOfEffectiveValue(time0)
    var break = false
    
    while (time1 >= 0 && !break) {
      val position1 = baseSer.rowOfTime(time1)
      val bar1 = br(position1)
      if (bar1 > nBars) {
        /** exceeds visible range */
        break = true
      } else {
        val time2 = getFirstTimeOfEffectiveValue(time1 + 1)
        if (time2 < 0) {
          /** no more values now */
          break = true
        } else {
          val position2 = baseSer.rowOfTime(time2)
          val bar2 = br(position2)
          if (bar2 < 1) {
            /** not in visible range yet */
            time1 = time2
          } else {
            /** now we've got two good positions, go on to draw a line between them */

            val value1 = model.v.double(tb(bar1))
            val value2 = model.v.double(tb(bar2))
            
            /** now try to draw line between these two points */
            val x1 = xb(bar1)
            val x2 = xb(bar2)
            val y1 = yv(value1)
            val y2 = yv(value2)
            
            tp.setForeground(color)
            tp.model.set(x1, y1, x2, y2)
            tp.plot
            pathsWidget.appendFrom(tp)
            
            /** set new position1 for next while loop */
            time1 = time2
          }
        }
      }
    }
  }
    
  private def getFirstTimeOfEffectiveValue(fromTime: Long): Long = {
    val times = model.v.timesIterator
    val values = model.v.valuesIterator
    while (times.hasNext) {
      val time = times.next
      val value = values.next.asInstanceOf[Double] // should call values.next to sync with times.next
      if (time >= fromTime && Null.not(value)) {
        return time
      }
    }
    
    -1
  }
    
}
