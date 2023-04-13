/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.util.swing.plaf

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Insets
import javax.swing.border.AbstractBorder
import javax.swing.plaf.metal.MetalLookAndFeel

/**
 * This border style is very good for Applet display
 *
 * @author dcaoyuan
 */
class AIOScrollPaneStyleBorder(color: Color, needShadow: Boolean) extends AbstractBorder {

  def this() = {
    this(null, false)
  }

  def this(color: Color) = {
    this(color, false)
  }

  override def paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int): Unit = {
    g.translate(x, y)

    if (color != null) {
      g.setColor(color)
    } else {
      g.setColor(MetalLookAndFeel.getControlDarkShadow)
    }
    g.drawRect(0, 0, w - 1, h - 1)

    if (needShadow) {
      // paint shadow
      if (color != null) {
        g.setColor(color)
      } else {
        g.setColor(MetalLookAndFeel.getControlHighlight)
      }
      g.drawLine(w - 1, 1, w - 1, h - 1)
      g.drawLine(1, h - 1, w - 1, h - 1)

      if (color != null) {
        g.setColor(color)
      } else {
        g.setColor(MetalLookAndFeel.getControl)
      }
      g.drawLine(w - 2, 2, w - 2, 2)
      g.drawLine(1, h - 2, 1, h - 2)

      g.translate(-x, -y)
    }
  }

  override def getBorderInsets(c: Component, insets: Insets): Insets = {
    insets.set(1, 1, 2, 2)
    insets
  }
}
