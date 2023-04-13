/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.swing

import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Polygon
import javax.swing.Icon


object ArrowIcon {
  val UP = 0
  val DOWN = 1
}

import ArrowIcon._
class ArrowIcon(direction: Int) extends Icon {

  private val pagePolygon = new Polygon(Array(2, 4, 4, 10, 10, 2),
                                        Array(4, 4, 2, 2, 12, 12),
                                        6)

  private val arrowX = Array(4, 9, 6)
  private val arrowUpPolygon   = new Polygon(arrowX, Array(10, 10, 4), 3)
  private val arrowDownPolygon = new Polygon(arrowX, Array(6,  6, 11), 3)

  def getIconWidth = {
    14
  }

  def getIconHeight = {
    14
  }

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    g.setColor(Color.black)
    pagePolygon.translate(x, y)
    g.drawPolygon(pagePolygon)
    pagePolygon.translate(-x, -y)
    direction match {
      case UP =>
        arrowUpPolygon.translate(x, y)
        g.fillPolygon(arrowUpPolygon)
        arrowUpPolygon.translate(-x, -y)
      case DOWN =>
        arrowDownPolygon.translate(x, y)
        g.fillPolygon(arrowDownPolygon)
        arrowDownPolygon.translate(-x, -y)
    }
  }
}