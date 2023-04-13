/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

/**
 * @version 1.0 11/22/98
 */
trait CellAttribute {

  def addColumn

  def addRow

  def insertRow(row: Int)

  def dim: Dim

  def dim_=(dim: Dim)
}
