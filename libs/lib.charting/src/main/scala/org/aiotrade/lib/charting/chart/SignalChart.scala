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

import java.awt.Font
import org.aiotrade.lib.charting.widget.Arrow
import org.aiotrade.lib.charting.widget.PathsWidget
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.signal.Corner
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal

/**
 *
 * @author Caoyuan Deng
 */
class SignalChart extends AbstractChart {
  final class Model extends WidgetModel {
    var signalVar: TVar[List[Signal]] = _
    var highVar:   TVar[Double] = _
    var lowVar:    TVar[Double] = _

    def set(signalVar: TVar[List[Signal]], highVar: TVar[Double], lowVar: TVar[Double]) {
      this.signalVar = signalVar
      this.highVar   = highVar
      this.lowVar    = lowVar
    }
  }

  type M = Model
    
  protected def createModel = new Model

  protected def plotChart {
    val m = model

    val laf = LookFeel()
    val posColor = laf.getPositiveColor
    val negColor = laf.getNegativeColor
        
    val antiColor = laf.backgroundColor

    val font = new Font(Font.DIALOG, Font.PLAIN, 10)
    val antiFont = new Font(Font.DIALOG, Font.BOLD, 8)

    val pathsWidget = addChild(new PathsWidget)
    val arrow = new Arrow
    var bar = 1
    while (bar <= nBars) {
            
      var i = -1
      while ({i += 1; i < nBarsCompressed}) {
        val time = tb(bar + i)
        if (ser.exists(time)) {
          var signals = m.signalVar(time)
          var j = 0
          var dyUp = 3
          var dyDn = 3
          while ((signals ne null) && (signals != Nil)) {
            signals = signals.reverse
            val signal = signals.head
            if (signal ne null) {              
              // appoint a reference value for this sign as the drawing position
              val refValue = if (m.lowVar != null && m.highVar != null) {
                signal.kind match {
                  case Side.EnterLong | Side.ExitShort  | Corner.Lower => m.lowVar(time)
                  case Side.ExitLong  | Side.EnterShort | Corner.Upper => m.highVar(time)
                  case _ => Null.Double
                }
              } else 0.0
                
              if (Null.not(refValue)) {
                val x = xb(bar)
                val y = yv(refValue)
                val text = signal.text
                val color = signal.color match {
                  case null => laf.getChartColor(signal.id)
                  case x => x
                }

                signal.kind match {
                  case Side.EnterLong | Side.ExitShort | Corner.Lower =>
                    var height = 12
                    var filled = false
                    if (signal.isSign) {
                      arrow.setForeground(color)
                      arrow.model.set(x, y + dyUp, true, true)
                      height = math.max(height, 12)
                      filled = true
                    }

                    if (signal.hasText) {
                      val label = addChild(new Label)
                      label.setFont(if (filled) antiFont else font)
                      label.setForeground(if (filled) antiColor else color)
                      label.model.setText(text)
                      val bounds = label.textBounds
                      label.model.set(x - math.floor(bounds.width / 2.0).toInt, y + dyUp + bounds.height - 1)
                      height = bounds.height
                    }

                    dyUp += (1 + height)
                    
                  case Side.ExitLong | Side.EnterShort | Corner.Upper =>
                    var height = 12
                    var filled = false
                    if (signal.isSign) {
                      arrow.setForeground(color)
                      arrow.model.set(x, y - dyDn, false, true)
                      height = math.max(height, 12)
                      filled = true
                    }

                    if (signal.hasText) {
                      val label = addChild(new Label)
                      label.setFont(if (filled) antiFont else font)
                      label.setForeground(if (filled) antiColor else color)
                      label.model.setText(text)
                      val bounds = label.textBounds
                      label.model.set(x - math.floor(bounds.width / 2.0).toInt, y - dyDn - 3)
                      height = bounds.height
                    }
                    
                    dyDn += (1 + height)
                  case _ =>
                }
                
                if (signal.isSign) {
                  arrow.plot
                  pathsWidget.appendFrom(arrow)
                }
              }
            }
            
            signals = signals.tail
            j += 1
          }
        }
      }

      bar += nBarsCompressed
    }
        
  }
    
}



