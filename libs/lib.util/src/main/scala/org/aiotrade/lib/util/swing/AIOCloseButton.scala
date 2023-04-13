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
package org.aiotrade.lib.util.swing

import java.awt.Color
import java.awt.image.BufferedImage
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JButton

/**
 *
 * @author Caoyuan Deng
 */
/**
 *  ICON_SIZE etc should be early def, since it may be used in super() call via setForeground
 */
class AIOCloseButton extends {
  val ICON_SIZE = 12
  private var chosenIcon: Icon = null
  private var unchosenIcon: Icon = null
  private var chosenRolloverIcon: Icon = null
  private var unchosenRolloverIcon: Icon = null
} with JButton {
    
  private var chosen: Boolean = _
    
  setFocusPainted(false)
  setRolloverEnabled(true)
  setBorderPainted(false)
  setContentAreaFilled(false)
        
  setIcons
    
  private def setIcons: Unit = {
    if (getForeground == null) {
      return
    }
        
    val wMark = ICON_SIZE - 3
    val hMark = ICON_SIZE - 3
        
    var image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    var g = image.createGraphics
    g.setColor(getForeground)
    g.drawRect(1, 1, wMark, hMark)
    g.dispose
    chosenIcon = new ImageIcon(image)
        
    image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    unchosenIcon = new ImageIcon(image)
        
    image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    g = image.createGraphics
    g.setColor(getForeground)
    g.drawRect(1, 1, wMark, hMark)
    g.drawLine(3, 3, wMark - 1, hMark - 1)
    g.drawLine(3, wMark - 1, hMark - 1, 3)
    g.dispose
    chosenRolloverIcon = new ImageIcon(image)
        
    image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB)
    g = image.createGraphics
    g.setColor(getForeground)
    g.drawLine(3, 3, wMark - 1, hMark - 1)
    g.drawLine(3, wMark - 1, hMark - 1, 3)
    g.dispose
    unchosenRolloverIcon = new ImageIcon(image)
        
    setIcon(if (isChosen) chosenIcon else unchosenIcon)
    setRolloverIcon(if (isChosen) chosenRolloverIcon else unchosenRolloverIcon)
  }
    
  override def setForeground(fg: Color): Unit = {
    val oldValue = getForeground
    super.setForeground(fg)
    if (oldValue == null || ! oldValue.equals(fg)) {
      setIcons
    }
  }
    
  def isChosen: Boolean = {
    chosen
  }
    
  def setChosen(b: Boolean): Unit = {
    val oldValue = isChosen
    this.chosen = b
    if (oldValue != b) {
      setIcon(if (isChosen) chosenIcon else unchosenIcon)
      setRolloverIcon(if (isChosen) chosenRolloverIcon else unchosenRolloverIcon)
    }
  }
    
}


