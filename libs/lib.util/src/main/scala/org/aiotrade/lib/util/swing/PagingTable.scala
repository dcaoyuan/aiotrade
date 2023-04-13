/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.swing

import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JButton
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ScrollPaneConstants
import javax.swing.table.AbstractTableModel

object PagingTable {

  //PagingModel.java
//A larger table model that performs "paging" of its data. This model
//reports a small number of rows (like 100 or so) as a "page" of data. You
//can switch pages to view all of the rows as needed using the pageDown()
//and pageUp() methods. Presumably, access to the other pages of data is
//dictated by other GUI elements such as up/down buttons, or maybe a text
//field that allows you to enter the page number you want to display.
//

  class PagingModel(numRows: Int, var pageSize: Int) extends AbstractTableModel {

    protected var pageOffset = 0

    protected val data = new Array[Record](numRows)
    // Fill our table with random data (from the Record() constructor).
    for (i <- 0 until data.length) {
      data(i) = new Record
    }

    def this() = this(10000, 100)

    // Return values appropriate for the visible table part.
    def getRowCount = {
      Math.min(pageSize, data.length)
    }

    def getColumnCount = {
      Record.getColumnCount
    }

    // Work only on the visible part of the table.
    def getValueAt(row: Int, col: Int): Object = {
      val realRow = row + (pageOffset * pageSize);
      data(realRow).getValueAt(col)
    }

    override def getColumnName(col: Int) = {
      Record.getColumnName(col)
    }

    // Use this method to figure out which page you are on.
    def getPageOffset: Int = {
      pageOffset
    }

    def getPageCount: Int = {
      Math.ceil(data.length.toDouble / pageSize).toInt
    }

    // Use this method if you want to know how big the real table is . . . we
    // could also write "getRealValueAt()" if needed.
    def getRealRowCount: Int = {
      data.length
    }

    def getPageSize: Int = {
      pageSize;
    }

    def setPageSize(s: Int) {
      if (s == pageSize) {
        return
      }
      val oldPageSize = pageSize
      pageSize = s
      pageOffset = (oldPageSize * pageOffset) / pageSize
      fireTableDataChanged
      /*
       * if (pageSize < oldPageSize) { fireTableRowsDeleted(pageSize,
       * oldPageSize - 1); } else { fireTableRowsInserted(oldPageSize,
       * pageSize - 1); }
       */
    }

    // Update the page offset and fire a data changed (all rows).
    def pageDown {
      if (pageOffset < getPageCount - 1) {
        pageOffset += 1
        fireTableDataChanged
      }
    }

    // Update the page offset and fire a data changed (all rows).
    def pageUp {
      if (pageOffset > 0) {
        pageOffset -= 1
        fireTableDataChanged
      }
    }
  }

  object Record {
    val headers = Array("Record Number", "Batch Number", "Reserved")

    def getColumnName(i: Int): String = {
      headers(i)
    }

    def getColumnCount: Int = {
      headers.length
    }
  }

  class Record {
    private var counter: Int = 0

    val data = Array("" + {counter += 1; counter}, "" + System.currentTimeMillis, "Reserved" );

    def getValueAt(i: Int) = {
      data(i)
    }

  }

  def createPagingScrollPaneForTable(table: JTable): JScrollPane = {
    val scollPane = new JScrollPane(table)
    val tmodel = table.getModel

    // Don't choke if this is called on a regular table . . .
    /* if (!(tmodel instanceof PagingModel)) {
     return jsp;
     } */

    // Okay, go ahead and build the real scrollpane
    //val PagingModel model = (PagingModel) tmodel;
    val upButton = new JButton(new ArrowIcon(ArrowIcon.UP))
    upButton.setEnabled(false); // starts off at 0, so can't go up
    val downButton = new JButton(new ArrowIcon(ArrowIcon.DOWN))
    //if (model.getPageCount() <= 1) {
    //  downButton.setEnabled(false); // One page...can't scroll down
    //}

    upButton.addActionListener(new ActionListener {
        def actionPerformed(ae: ActionEvent) {
          //model.pageUp();

          // If we hit the top of the data, disable the up button.
          /* if (model.getPageOffset() == 0) {
           upButton.setEnabled(false);
           }
           downButton.setEnabled(true); */
        }
      });

    downButton.addActionListener(new ActionListener {
        def actionPerformed(ae: ActionEvent) {
          //model.pageDown();

          // If we hit the bottom of the data, disable the down button.
          //if (model.getPageOffset() == (model.getPageCount() - 1)) {
          //  downButton.setEnabled(false);
          //}
          //upButton.setEnabled(true);
        }
      })

    // Turn on the scrollbars; otherwise we won't get our corners.
    scollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    scollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)

    // Add in the corners (page up/down).
    scollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, upButton)
    scollPane.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, downButton)

    scollPane
  }

}
