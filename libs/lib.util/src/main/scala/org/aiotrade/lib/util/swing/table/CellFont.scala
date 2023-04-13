/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

import java.awt.Font

/**
 * @version 1.0 11/22/98
 */
trait CellFont {

  def getFont(row: Int, column: Int): Font

  def setFont(font: Font, row: Int, column: Int)

  def setFont(font: Font, rows: Array[Int], columns: Array[Int])
}
