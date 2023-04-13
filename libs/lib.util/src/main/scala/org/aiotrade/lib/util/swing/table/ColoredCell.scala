/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

import java.awt.Color

/**
 * @version 1.0 11/22/98
 */
trait ColoredCell {

  def getForeground(row: Int, column: Int): Color

  def setForeground(color: Color, row: Int, column: Int)

  def setForeground(color: Color, rows: Array[Int], columns: Array[Int])

  def getBackground(row: Int, column: Int): Color

  def setBackground(color: Color, row: Int, column: Int)

  def setBackground(color: Color, rows: Array[Int], columns: Array[Int])

  def getHorizontalAlignment(row: Int, column: Int) : Int

  def setHorizontalAlignment(horizontalAlignment: Int, row: Int, column: Int)
}
