/*
 * (swing1.1beta3)
 * 
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTableUI;

/**
 * @version 1.0 11/26/98
 */
class MultiSpanCellTableUI extends BasicTableUI {

  override def paint(g: Graphics, c: JComponent) {
    val oldClipBounds = g.getClipBounds
    val clipBounds = new Rectangle(oldClipBounds)
    val tableWidth = table.getColumnModel.getTotalColumnWidth
    clipBounds.width = Math.min(clipBounds.width, tableWidth)
    g.setClip(clipBounds)

    val beginRow = table.rowAtPoint(new Point(0, clipBounds.y))

    val rowRect = new Rectangle(0, 0,
                                tableWidth, table.getRowHeight + table.getRowMargin)
    rowRect.y = beginRow * rowRect.height

    for (row <- beginRow until table.getRowCount) {
      if (rowRect.intersects(clipBounds)) {
        //System.out.println();                  // debug
        //System.out.print("" + index +": ");    // row
        paintRow(g, row)
      }
      rowRect.y += rowRect.height
    }
    g.setClip(oldClipBounds)
  }

  private def paintRow(g: Graphics, row: Int): Unit = {
    val rect = g.getClipBounds
    var drawn = false

    val tableModel = table.getModel.asInstanceOf[AttributiveCellTableModel]
    val cellAttr = tableModel.cellAttribute.asInstanceOf[CellSpan]
    val nCols = table.getColumnCount

    def loop(col: Int) {
      if (col < nCols) {
        val cellRect = table.getCellRect(row, col, true)
        val(cellRow, cellCol) = if (cellAttr.isVisible(row, col)) {
          (row, col)
          //  System.out.print("   "+column+" ");  // debug
        } else {
          val cellRow1 = row + cellAttr.getSpan(row, col)(CellSpan.ROW)
          val cellCol1 = col + cellAttr.getSpan(row, col)(CellSpan.COL)
          (cellRow1, cellCol1)
          //  System.out.print("  ("+column+")");  // debug
        }

        if (cellRect.intersects(rect)) {
          drawn = true
          paintCell(g, cellRect, cellRow, cellCol)
          loop(col + 1)
        } else {
          if (drawn) { // break
          } else {
            loop(col + 1)
          }
        }
      }
    }
    loop(0)
  }

  private def paintCell(g: Graphics, cellRect: Rectangle, row: Int, col: Int): Unit = {
    val spacingHeight = table.getRowMargin
    val spacingWidth = table.getColumnModel.getColumnMargin

    val c = g.getColor
    g.setColor(table.getGridColor)
    val x1 = cellRect.x
    val y1 = cellRect.y
    val x2 = cellRect.x + cellRect.width  - 1
    val y2 = cellRect.y + cellRect.height - 1
    if (table.getShowHorizontalLines) {
      g.drawLine(x1, y1, x2, y1)
      g.drawLine(x1, y2, x2, y2)
    }
    if (table.getShowVerticalLines) {
      g.drawLine(x1, y1, x1, y2)
      g.drawLine(x2, y1, x2, y2)
    }
    g.setColor(c)

    cellRect.setBounds(cellRect.x + spacingWidth / 2, cellRect.y + spacingHeight / 2,
                       cellRect.width - spacingWidth, cellRect.height - spacingHeight)

    if (table.isEditing && table.getEditingRow == row && table.getEditingColumn == col) {
      val component = table.getEditorComponent
      component.setBounds(cellRect)
      component.validate
    } else {
      val renderer = table.getCellRenderer(row, col)
      val component = table.prepareRenderer(renderer, row, col)

      if (component.getParent == null) {
        rendererPane.add(component)
      }
      
      rendererPane.paintComponent(g, component, table, cellRect.x, cellRect.y,
                                  cellRect.width, cellRect.height, true)
    }
  }
}

