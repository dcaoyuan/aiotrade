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
package org.aiotrade.lib.charting.widget

import java.awt.Container
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.JTextField
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.util.swing.action.EditAction

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 29, 2006, 1:58 PM
 * @since   1.0.4
 */
class Label extends AbstractWidget {
  final class Model extends WidgetModel {
    var x: Double = _
    var y: Double = _
    var text = "Click me to edit"
    var editable = false
        
    def set(x: Double, y: Double, text: String, editable: Boolean = false) {
      this.x = x
      this.y = y
      this.text = text
      setEditable(editable)
    }
        
    def set(x: Double, y: Double) {
      this.x = x
      this.y = y
    }

    def setText(text: String) {
      this.text = text
    }

    def setEditable(b: Boolean) {
      this.editable = b
      if (editable) {
        if (lookupAction(classOf[EditAction]).isEmpty) addAction(new LabelEditAction)
      } else {
        lookupAction(classOf[EditAction]) foreach removeAction
      }
    }

  }

  type M = Model

  private val scratchBuffer = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
    
  private var font: Font = LookFeel().axisFont

  /**
   * To get the right bounds/size if the text, we need to have a FontRenderContext.
   * And that context has to come from the Graphics2D object, which will come
   * from graphics, that we haven't known yet! A bit of a chicken-and-egg
   * problem. The solution is to create a scratch buffer just to get the
   * FontRenderContext. Then call Font.getStringBounds() to get the size of
   * the text and draw it later.
   */
  def textBounds = {
    val g = scratchBuffer.createGraphics
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val frc = g.getFontRenderContext
    val bounds = font.getStringBounds(model.text, frc).getBounds
    g.dispose
    bounds
  }

  protected def createModel = {
    val m = new Model
    if (m.editable) addAction(new LabelEditAction)
    m
  }

  def setFont(font: Font) {
    this.font = font
  }
    
  def getFont: Font = font
    
  override protected def makePreferredBounds: Rectangle = {
    val bounds = textBounds
    val m = model
    /** x, y is the baseline of string, we need to shift the top-left of bounds */
    new Rectangle(
      (m.x - 1).intValue, (m.y - 1).intValue - bounds.height,
      if (bounds.width < 5) 5 else bounds.width + 2, bounds.height + 2)
  }
    
  protected def widgetIntersects(x: Double, y: Double, width: Double, height: Double): Boolean = {
    return getBounds.intersects(x, y, width, height)
  }
    
  override protected def widgetContains(x: Double, y: Double, width: Double, height: Double): Boolean = {
    return getBounds.contains(x, y, width, height)
  }
    
  protected def plotWidget {}
    
  def renderWidget(g0: Graphics) {
    val g = g0.asInstanceOf[Graphics2D]
        
    g.setColor(getForeground)
    g.setFont(getFont)
    val backupRendingHint = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
    val m = model
    g.drawString(m.text, m.x.toFloat, m.y.toFloat)
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, backupRendingHint)
  }
      
  class LabelEditAction extends EditAction {
    private var container: Container = _
    private var textField: JTextField = _
        
    def execute {
      val textField = getEditorPresenter.asInstanceOf[JTextField]
      textField.setFont(Label.this.getFont)
      val bounds = Label.this.getBounds
      textField.setText(Label.this.model.text)
      textField.selectAll
      textField.setVisible(true)
      textField.setBounds(
        bounds.x, bounds.y,
        if (bounds.width < 80) 80 else bounds.width + 20, bounds.height)
      textField.grabFocus
    }
        
    override def anchorEditor(container: Container) {
      this.container = container
      container.add(getEditorPresenter)
    }
        
    private def getEditorPresenter: JComponent = {
      if (textField == null) {
        textField = new JTextField();
        textField.addActionListener(new ActionListener {
            override def actionPerformed(e: ActionEvent) {
              Label.this.model.setText(textField.getText)
              textField.setVisible(false)
              container.remove(getEditorPresenter)
              container = null
            }
          })
      }
            
      textField
    }
  }
    
}

