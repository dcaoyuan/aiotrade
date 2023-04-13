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
class FibonacciTimeZonesChart extends AbstractChart {
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
        
    val numFn = 40
        
    val bs = new Array[Double](numFn * 2 + 1)
    bs(0) = bt(m.t1)
        
    /** calculate Fibonacci serials */
    val Fn = new Array[Double](numFn)
    Fn(0) = 5
    Fn(1) = 8
        
    /**
     * @NOTICE
     * only apply compressedFactor to distance between bar index, not to x.
     */
    bs(1) = bs(0) + Fn(0)
    bs(2) = bs(0) - Fn(0)
    bs(3) = bs(0) + Fn(1)
    bs(4) = bs(0) - Fn(1)

    var n = 2
    while (n < numFn) {
            
      /*- @RESERVE
       * double Fn = 100 * math.pow(math.PI, 2) * math.pow(0.6180339, 15 - i);
       * int step = (int)(math.sqrt(Fn) * 29.5306);
       */
            
      Fn(n) = Fn(n - 1) + Fn(n - 2)
            
      /** positive side */
      bs(n * 2 + 1) = bs(0) + Fn(n)
      /** negative side */
      bs(n * 2 + 2) = bs(0) - Fn(n)

      n += 1
    }
        
    val pathWidget = addChild(new PathWidget)
    pathWidget.setForeground(color)
    val path = pathWidget.getPath
    var bar = 1
    while (bar <= nBars) {
      var i = 0
      while (i < nBarsCompressed) {
        if (bar + i == math.round(bs(0))) {
          plotVerticalLine(bar + i, path)
        }
                
        /** search if i is in Fibonacci serials */
        var j = 1
        var break = false
        while (j < numFn * 2 && !break) {
          if (bar + i == math.round(bs(j)) || bar + i== math.round(bs(j + 1))) {
            plotVerticalLine(bar + i, path)
            break = true
          }
          j += 2
        }

        i += 1
      }

      bar += nBarsCompressed
    }
        
  }
    
}

