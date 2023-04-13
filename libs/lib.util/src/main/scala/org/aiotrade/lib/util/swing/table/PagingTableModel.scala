package org.aiotrade.lib.util.swing.table

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Polygon
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ScrollPaneConstants
import javax.swing.table.AbstractTableModel

object PagingTableModel {

  def main(args: Array[String]) {
    val pt = new JFrame("Paged JTable Test")
    pt.setSize(300, 200);
    pt.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)

    val pm = new PagingTableModel
    val jt = new JTable(pm)

    // Use our own custom scrollpane.
    val jsp = createPagingScrollPaneForTable(jt)
    pt.getContentPane().add(jsp, BorderLayout.CENTER)
    pt.setVisible(true)
  }


  // We provide our own version of a scrollpane that includes
  // the page up and page down buttons by default.
  def createPagingScrollPaneForTable(jt: JTable): JScrollPane = {
    val jsp = new JScrollPane(jt);
    val tmodel = jt.getModel

    // Don't choke if this is called on a regular table . . .
    if (!(tmodel.isInstanceOf[PagingTableModel])) {
      return jsp;
    }

    // Okay, go ahead and build the real scrollpane
    val model = tmodel.asInstanceOf[PagingTableModel];
    val upButton = new JButton(new ArrowIcon(ArrowIcon.UP));
    upButton.setEnabled(false); // starts off at 0, so can't go up
    val downButton = new JButton(new ArrowIcon(ArrowIcon.DOWN));
    if (model.pageCount <= 1) {
      downButton.setEnabled(false) // One page...can't scroll down
    }

    upButton.addActionListener(new ActionListener {
        def actionPerformed(ae: ActionEvent) {
          model.pageUp

          // If we hit the top of the data, disable the up button.
          if (model.pageOffset == 0) {
            upButton.setEnabled(false)
          }
          downButton.setEnabled(true)
        }
      });

    downButton.addActionListener(new ActionListener() {
        def actionPerformed(ae: ActionEvent) {
          model.pageDown

          // If we hit the bottom of the data, disable the down button.
          if (model.pageOffset == (model.pageCount - 1)) {
            downButton.setEnabled(false)
          }
          upButton.setEnabled(true)
        }
      })

    // Turn on the scrollbars; otherwise we won't get our corners.
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)

    // Add in the corners (page up/down).
    jsp.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, upButton)
    jsp.setCorner(ScrollPaneConstants.LOWER_RIGHT_CORNER, downButton)

    jsp
  }
}

/**
 * A larger table model that performs "paging" of its data. This model
 * reports a small number of rows (like 100 or so) as a "page" of data. You
 * can switch pages to view all of the rows as needed using the pageDown()
 * and pageUp() methods. Presumably, access to the other pages of data is
 * dictated by other GUI elements such as up/down buttons, or maybe a text
 * field that allows you to enter the page number you want to display.
 */
class PagingTableModel(numRows: Int, protected var _pageSize: Int) extends AbstractTableModel {

  var pageOffset = 0

  protected var data: Array[Record] = new Array[Record](numRows)

  // Fill our table with random data (from the Record() constructor).
  private var i = 0
  while (i < data.length) {
    data(i) = new Record
    i += 1
  }

  def this() = this(10000, 100)

  // Return values appropriate for the visible table part.
  def getRowCount: Int = {
    math.min(_pageSize, data.length)
  }

  def getColumnCount: Int = {
    Record.getColumnCount
  }

  // Work only on the visible part of the table.
  def getValueAt(row: Int, col: Int): Object = {
    val realRow = row + (pageOffset * pageSize);
    data(realRow).getValueAt(col);
  }

  override def getColumnName(col: Int): String = {
    Record.getColumnName(col);
  }

  def pageCount: Int = {
    math.ceil(data.length.toDouble / pageSize).toInt
  }

  // Use this method if you want to know how big the real table is . . . we
  // could also write "getRealValueAt()" if needed.
  def getRealRowCount: Int = {
    data.length;
  }

  def pageSize = _pageSize
  def pageSize_=(s: Int) {
    if (s == pageSize) {
      return;
    }
    val oldPageSize = pageSize
    _pageSize = s
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
    if (pageOffset < pageCount - 1) {
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
  val headers = Array( "Record Number", "Batch Number", "Reserved")
  var counter: Int = 0
  
  def getColumnName(i: Int): String = {
    headers(i)
  }

  def getColumnCount: Int = {
    headers.length
  }
  
  
}

import Record._
class Record {

  protected var data = Array( "" + (counter),
                             "" + System.currentTimeMillis(), "Reserved" )
  counter += 1

  def getValueAt(i: Int): String = {
    data(i)
  }

}

//ArrowIcon.java
//A simple implementation of the Icon interface that can make
//Up and Down arrows.
//
object ArrowIcon {
  val UP = 0;

  val DOWN = 1;
}

class ArrowIcon(direction: Int) extends Icon {


  private val pagePolygon = new Polygon(Array( 2, 4, 4, 10, 10, 2 ),
                                        Array( 4, 4, 2, 2, 12, 12 ), 6)

  private val arrowX = Array( 4, 9, 6 )

  private val arrowUpPolygon = new Polygon(arrowX,
                                           Array( 10, 10, 4 ), 3)

  private val arrowDownPolygon = new Polygon(arrowX,
                                             Array( 6, 6, 11 ), 3)

  def getIconWidth: Int = {
    14
  }

  def getIconHeight: Int = {
    14
  }

  def paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    g.setColor(Color.black)
    pagePolygon.translate(x, y)
    g.drawPolygon(pagePolygon)
    pagePolygon.translate(-x, -y)
    if (direction == ArrowIcon.UP) {
      arrowUpPolygon.translate(x, y)
      g.fillPolygon(arrowUpPolygon)
      arrowUpPolygon.translate(-x, -y)
    } else {
      arrowDownPolygon.translate(x, y)
      g.fillPolygon(arrowDownPolygon)
      arrowDownPolygon.translate(-x, -y)
    }
  }
}
