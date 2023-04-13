package org.aiotrade.lib.util.swing.plaf;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Color;
import java.awt.Component;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

/**
 * A class which implements a top line border of arbitrary thickness
 * and of a single color.
 *
 * @author Caoyuan Deng
 */
object TopLineBorder {
  private var blackLine: Border = _
  private var grayLine: Border = _

  /**
   * Convenience method for getting the Color.black LineBorder of thickness 1.
   */
  def createBlackLineBorder: Border = {
    if (blackLine == null) {
      blackLine = new LineBorder(Color.black, 1)
    }
    blackLine
  }

  /**
   * Convenience method for getting the Color.gray LineBorder of thickness 1.
   */
  def createGrayLineBorder: Border = {
    if (grayLine == null) {
      grayLine = new LineBorder(Color.gray, 1)
    }
    grayLine
  }
}
/**
 * Creates a line border with the specified color, thickness, and corner shape.
 * @param color the color of the border
 * @param thickness the thickness of the border
 * @param roundedCorners whether or not border corners should be round
 */
class TopLineBorder(lineColor: Color, thickness: Int, roundedCorners: Boolean) extends AbstractBorder {
  /**
   * Creates a line border with the specified color and a thickness = 1.
   * @param color the color for the border
   */
  def this(lineColor: Color) = {
    this(lineColor, 1, false)
  }

  /**
   * Creates a line border with the specified color and thickness.
   * @param color the color of the border
   * @param thickness the thickness of the border
   */
  def this(lineColor: Color, thickness: Int) = {
    this(lineColor, thickness, false)
  }

  /**
   * Paints the border for the specified component with the
   * specified position and size.
   * @param c the component for which this border is being painted
   * @param g the paint graphics
   * @param x the x position of the painted border
   * @param y the y position of the painted border
   * @param width the width of the painted border
   * @param height the height of the painted border
   */
  override def paintBorder(c: Component, g: Graphics , x: Int, y: Int, width: Int, height: Int) :Unit = {
    val oldColor = g.getColor
    g.setColor(lineColor)

    for (i <- 0 until thickness) {
      if (!roundedCorners) {
        g.drawLine(x + i, y + i, width - i - i - 1, y + i)
      } else {
        //g.drawRoundRect(x + i, y + i, width - i - i - 1, height - i - i - 1, thickness, thickness);
      }
    }
    g.setColor(oldColor)
  }

  /**
   * Returns the insets of the border.
   * @param c the component for which this border insets value applies
   */
  override def getBorderInsets(c: Component): Insets = {
    new Insets(thickness, thickness, thickness, thickness)
  }

  /**
   * Reinitialize the insets parameter with this Border's current Insets.
   * @param c the component for which this border insets value applies
   * @param insets the object to be reinitialized
   */
  override def getBorderInsets(c: Component, insets: Insets): Insets = {
    insets.left = thickness
    insets.top = thickness
    insets.right = thickness
    insets.bottom = thickness
    insets
  }

  /**
   * Returns the color of the border.
   */
  def getLineColor: Color = {
    lineColor
  }

  /**
   * Returns the thickness of the border.
   */
  def getThickness: Int = {
    thickness
  }

  /**
   * Returns whether this border will be drawn with rounded corners.
   */
  def getRoundedCorners: Boolean = {
    roundedCorners
  }

  /**
   * Returns whether or not the border is opaque.
   */
  override def isBorderOpaque: Boolean = {
    !roundedCorners
  }
}
