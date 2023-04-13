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
package org.aiotrade.lib.charting.chart.segment

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle

/**
 *
 * @author Caoyuan Deng
 */
class TextSegment(var text: String, var x: Double, var y: Double, $color: Color, var bgColor: Color) extends AbstractSegment($color) {
    
  private var valid: Boolean = _
  private val bounds = new Rectangle
    
  def this() = this(null, 0f, 0f, null, null)
        
  def this(text: String, x: Double, y: Double, color: Color) = {
    this(text, x, y, color, null)
  }
    
  def setText(text: String) {
    this.text = text
    this.valid = false
  }
    
  private def computeBounds(g: Graphics) {
    val fm = g.getFontMetrics
    bounds.setBounds(math.round(x).toInt, math.round(y).toInt - fm.getHeight + 1,
                     fm.stringWidth(text) + 1, fm.getHeight)
  }
    
  def getBounds(g: Graphics): Rectangle = {
    if (!valid) {
      computeBounds(g)
    }
        
    bounds
  }
    
  def render(g: Graphics) {
    if (bgColor != null) {
      val bounds = getBounds(g)
      g.setColor(bgColor)
      g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
    }
        
    g.setColor(color)
    g.asInstanceOf[Graphics2D].drawString(text, x.toFloat, y.toFloat)
  }
}
