/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table

import java.awt.Point
import java.awt.Rectangle
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionEvent
import javax.swing.table.TableModel

/**
 * @version 1.0 11/26/98
 */
class MultiSpanCellTable(model: TableModel) extends JTable(model) {

  setUI(new MultiSpanCellTableUI)
  getTableHeader().setReorderingAllowed(false)
  setCellSelectionEnabled(true)
  setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)

  override def getCellRect($row: Int, $col: Int, includeSpacing: Boolean): Rectangle = {
    if ($row < 0 || $col < 0 || $row >= getRowCount || $col >= getColumnCount) {
      return super.getCellRect($row, $col, includeSpacing)
    }

    var row = $row
    var col = $col
    val cellAttr = getModel.asInstanceOf[AttributiveCellTableModel].cellAttribute.asInstanceOf[CellSpan]
    if (!cellAttr.isVisible(row, col)) {
      val temp_row = row
      val temp_col = col
      row += cellAttr.getSpan(temp_row, temp_col)(CellSpan.ROW)
      col += cellAttr.getSpan(temp_row, temp_col)(CellSpan.COL)
    }
    val spans = cellAttr.getSpan(row, col)

    val cmodel = getColumnModel
    val cm = cmodel.getColumnMargin
    val rect = new Rectangle
    val cellHeight = rowHeight + rowMargin
    rect.y = row * cellHeight
    rect.height = spans(CellSpan.ROW) * cellHeight

    if (getComponentOrientation.isLeftToRight) {
      for (i <- 0 until col) {
        rect.x += cmodel.getColumn(i).getWidth
      }
    } else {
      for (i <- cmodel.getColumnCount - 1 until col) {
        rect.x += cmodel.getColumn(i).getWidth
      }
    }
    rect.width = cmodel.getColumn(col).getWidth
        
    for (i <- 0 until spans(CellSpan.COL) - 1) {
      rect.width += cmodel.getColumn(col + i).getWidth + cm
    }

    if (!includeSpacing) {
      val rm = getRowMargin
      rect.setBounds(rect.x + cm / 2, rect.y + rm / 2, rect.width - cm, rect.height - rm)
    }

    rect
  }

  private def rowColumnAtPoint(point: Point): Array[Int] = {
    val ret = Array(-1, -1)
    val row = point.y / (rowHeight + rowMargin)
    if (row < 0 || row >= getRowCount) {
      return ret
    }
    val col = getColumnModel.getColumnIndexAtX(point.x)

    val cellAttr = getModel.asInstanceOf[AttributiveCellTableModel].cellAttribute.asInstanceOf[CellSpan]

    if (cellAttr.isVisible(row, col)) {
      ret(CellSpan.COL) = col
      ret(CellSpan.ROW) = row
      return ret
    }
    
    ret(CellSpan.COL) = col + cellAttr.getSpan(row, col)(CellSpan.COL)
    ret(CellSpan.ROW) = row + cellAttr.getSpan(row, col)(CellSpan.ROW)

    ret
  }

  override def rowAtPoint(point: Point): Int = {
    rowColumnAtPoint(point)(CellSpan.ROW)
  }

  override def columnAtPoint(point: Point): Int = {
    rowColumnAtPoint(point)(CellSpan.COL)
  }

  override def columnSelectionChanged(e: ListSelectionEvent) {
    repaint()
  }

  override def valueChanged(e: ListSelectionEvent) {
    val firstIdx = e.getFirstIndex
    val lastIdx  = e.getLastIndex
    if (firstIdx == -1 && lastIdx == -1) { // Selection cleared.
      repaint()
    }
    val dirtyRegion = getCellRect(firstIdx, 0, false)
    val nCols = getColumnCount
    var idx = firstIdx
    for (i <- 0 until nCols) {
      dirtyRegion.add(getCellRect(idx, i, false))
    }
    idx = lastIdx
    for (i <- 0 until nCols) {
      dirtyRegion.add(getCellRect(idx, i, false))
    }
    repaint(dirtyRegion.x, dirtyRegion.y, dirtyRegion.width, dirtyRegion.height)
  }
}

