/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

/**
 * @version 1.0 11/22/98
 */
object CellSpan {
  val ROW = 0
  val COL = 1
}

trait CellSpan {

  def getSpan(row: Int, col: Int): Array[Int]

  def setSpan(span: Array[Int], row: Int, col: Int): Unit

  def isVisible(row: Int, col: Int): Boolean

  def combine(rows: Array[Int], cols: Array[Int]): Unit

  def split(row: Int, col: Int): Unit
}
