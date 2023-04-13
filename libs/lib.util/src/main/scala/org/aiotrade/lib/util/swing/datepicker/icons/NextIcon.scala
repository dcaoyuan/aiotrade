package org.aiotrade.lib.util.swing.datepicker.icons

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

class NextIcon(private var _width: Int, private var _height: Int, private var _isDoubleArrow: Boolean = false) extends Icon {
	
  protected val xPoints = new Array[Int](3)
  protected val yPoints = new Array[Int](3)
	
  def setDimension(width: Int, height: Int) {
    this._width = width
    this._height = height
  }

  def isDoubleArrow = _isDoubleArrow
  def isDoubleArrow(doubleArrow: Boolean) {
    this._isDoubleArrow = doubleArrow
  }

  def getIconWidth = _width

  def getIconHeight = _height

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (_isDoubleArrow) {

      xPoints(0) = x + (_width / 2)
      yPoints(0) = y + (_height / 2)

      xPoints(1) = x
      yPoints(1) = y - 1

      xPoints(2) = x
      yPoints(2) = y + _height

      g.setColor(Color.BLACK)
      g.fillPolygon(xPoints, yPoints, 3)

      xPoints(0) = x + _width
      yPoints(0) = y + (_height / 2)

      xPoints(1) = x + (_width / 2)
      yPoints(1) = y - 1

      xPoints(2) = x + (_width / 2)
      yPoints(2) = y + _height

      g.setColor(Color.BLACK);
      g.fillPolygon(xPoints, yPoints, 3)
      
    } else {

      xPoints(0) = x + _width
      yPoints(0) = y + (_height / 2)
			
      xPoints(1) = x
      yPoints(1) = y - 1
			
      xPoints(2) = x
      yPoints(2) = y + _height
			
      g.setColor(Color.BLACK)
      g.fillPolygon(xPoints, yPoints, 3)

    }
  }
}

