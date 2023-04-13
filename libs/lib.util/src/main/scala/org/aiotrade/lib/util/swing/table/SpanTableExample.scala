/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.util.swing.table;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/**
 *
 * @author dcaoyuan
 */
object SpanTableExample {
  def main(args: Array[String]) {
    val frame = new SpanTableExample
    frame.addWindowListener(new WindowAdapter {
        override def windowClosing(e: WindowEvent) {
          System.exit(0)
        }
      }
    )
  }
}

class SpanTableExample extends JFrame("Multi-Span Cell Example") {
  val ml = AttributiveCellTableModel(10, 6)
  //  val ml = new AttributiveCellTableModel(
  //    Array(Array("代码：", "北京纽约太平洋证券有限公司", null, null),
  //          Array("最新：", new Integer(111), "总量：", new Integer(123000)),
  //          Array("涨跌：", new Integer(11), "最高：", new Integer(122)),
  //          Array("涨跌(%)：", new Integer(10), "最低：", new Integer(122)),
  //          Array("前收：", new Integer(100), "开盘：", new Integer(122))
  //    ).asInstanceOf[Array[Array[Object]]],
  //    Array("Ask/Bid", "Price", "Size", "None").asInstanceOf[Array[Object]]
  //  )
  /*
   AttributiveCellTableModel ml = new AttributiveCellTableModel(10,6) {
   public Object getValueAt(int row, int col) {
   return "" + row + ","+ col;
   }
   };
   */
  val cellAtt = ml.cellAttribute.asInstanceOf[CellSpan]
  val table = new MultiSpanCellTable(ml)

  cellAtt.combine(Array(0), Array(1, 2, 3))
  table.revalidate
  table.repaint()

  val scroll = new JScrollPane(table)

  val b_one = new JButton("Combine")
  b_one.addActionListener(new ActionListener {

      def actionPerformed(e: ActionEvent): Unit = {
        val columns = table.getSelectedColumns
        val rows = table.getSelectedRows
        cellAtt.combine(rows, columns)
        table.clearSelection
        table.revalidate
        table.repaint()
      }
    }
  )
  
  val b_split = new JButton("Split")
  b_split.addActionListener(new ActionListener {

      def actionPerformed(e: ActionEvent): Unit = {
        val column = table.getSelectedColumn
        val row = table.getSelectedRow
        cellAtt.split(row, column)
        table.clearSelection
        table.revalidate
        table.repaint()
      }
    }
  )

  val p_buttons = new JPanel
  p_buttons.setLayout(new GridLayout(2, 1))
  p_buttons.add(b_one)
  p_buttons.add(b_split)

  val box = new Box(BoxLayout.X_AXIS)
  box.add(scroll)
  box.add(new JSeparator(SwingConstants.HORIZONTAL))
  box.add(p_buttons)
  getContentPane.add(box)
  setSize(400, 200)
  setVisible(true)
}


