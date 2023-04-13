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
import java.awt.geom.GeneralPath
import java.text.DecimalFormat
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel

/**
 *
 * @author Caoyuan Deng
 */
object CursorChart {
  abstract class Type
  object Type {
    case object Refer extends Type
    case object Mouse extends Type
  }

  val MONEY_DECIMAL_FORMAT = new DecimalFormat("0.###")
  val STOCK_DECIMAL_FORMAT = new DecimalFormat("0.00")
}

abstract class CursorChart extends AbstractChart {
  import CursorChart._
    
  protected var laf: LookFeel = _
  protected var fgColor: Color = _
  protected var bgColor: Color = _
    
  protected var referRow: Int = _
  protected var mouseRow: Int = _
  protected var referTime: Long = _
  protected var mouseTime: Long = _
  protected var x: Double = _
    
  protected var cursorPath: GeneralPath = _
    
  private var tpe: Type = Type.Mouse
    
    
  def setType(tpe: Type) {
    this.tpe = tpe
  }
    
  def getType: Type = {
    tpe
  }

  type M = WidgetModel

  protected def createModel: WidgetModel = null.asInstanceOf[WidgetModel]
    
  protected def plotChart {
    laf = LookFeel()
        
    referRow  = datumPlane.view.controller.referCursorRow
    referTime = datumPlane.view.controller.referCursorTime
    mouseRow  = datumPlane.view.controller.mouseCursorRow
    mouseTime = datumPlane.view.controller.mouseCursorTime
        
    val pathWidget = addChild(new PathWidget)
    tpe match {
      case Type.Refer =>
        fgColor = laf.referCursorColor
        bgColor = laf.referCursorColor
        pathWidget.setForeground(fgColor)
                
        cursorPath = pathWidget.getPath
                
        x = xb(br(referRow))
                
        plotReferCursor
                
      case Type.Mouse =>
        if (! datumPlane.view.controller.isMouseEnteredAnyChartPane) {
          return;
        }
                
        fgColor = laf.mouseCursorColor
        bgColor = Color.YELLOW
        pathWidget.setForeground(fgColor)
                
        cursorPath = pathWidget.getPath
                
        x = xb(br(mouseRow))
                
        plotMouseCursor
              
    }
  }
    
  protected def plotReferCursor: Unit
    
  protected def plotMouseCursor: Unit
    
  /** CursorChart always returns false */
  override def isSelected: Boolean = {
    false
  }
    
}
