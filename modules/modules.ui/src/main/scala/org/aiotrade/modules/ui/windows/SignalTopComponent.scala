package org.aiotrade.modules.ui.windows

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.JLabel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.RowSorter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableCellRenderer
import org.aiotrade.lib.collection.ArrayList
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.math.signal.Signal
import org.aiotrade.lib.math.signal.SignalX
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.actors.Reactor
import org.aiotrade.lib.view.securities.Comparators
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.lib.util.swing.action.ViewAction
import org.openide.windows.TopComponent

class SignalTopComponent extends TopComponent with Reactor {
  private val log = Logger.getLogger(this.getClass.getName)
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.modules.ui.windows.Bundle")

  private val tc_id = "SignalTopComponent"

  private var table: JTable = _
  private var model: AbstractTableModel = _

  private val signalEvents = new ArrayList[SignalX]


  private val TIME = "time"
  private val SYMBOL = "symbol"
  private val SIGN = "sign"
  private val NAME = "name"

  private val colKeys = Array[String](
    TIME,
    SYMBOL,
    SIGN,
    NAME
  )
  
  initComponent

  reactions += {
    case x: SignalX =>
      requestActive
      updateSignalTable(x)
      table.repaint()
  }
  listenTo(Signal)
  
  private def initComponent {
    
    model = new AbstractTableModel {
      private val colNames = {
        val names = new Array[String](colKeys.length)
        var i = 0
        while (i < colKeys.length) {
          names(i) = BUNDLE.getString(colKeys(i))
          i += 1
        }
        names
      }

      def getRowCount: Int = signalEvents.size
      def getColumnCount: Int = colNames.length

      def getValueAt(row: Int, col: Int): Object = {
        val event = signalEvents(row)
        col match {
          case 0 =>
            val sec = Exchange.secOf(event.symbol).getOrElse(return null)
            if (sec.exchange != null) {
              val timeZone = sec.exchange.timeZone
              val df = event.freq match {
                case "1D" => new SimpleDateFormat("MM-dd")
                case "1m" => new SimpleDateFormat("HH:mm")
                case _ => new SimpleDateFormat("MM-dd")
              }
              df.setTimeZone(timeZone)

              val cal = Calendar.getInstance(timeZone)
              cal.setTimeInMillis(event.signal.time)
              df format cal.getTime
            } else {
              log.warning("Sec: " + event.symbol + "'s exchange is null!"); null
            }
          case 1 => event.symbol
          case 2 => event.signal.kind match {
              case Side.EnterLong  => BUNDLE.getString("enterLong")
              case Side.ExitLong   => BUNDLE.getString("exitLong")
              case Side.EnterShort => BUNDLE.getString("enterShort")
              case Side.ExitShort  => BUNDLE.getString("exitShort")
            }
          case 3 => event.signal.id + "号"
          case _ => null
        }
      }

      override def getColumnName(col: Int) = colNames(col)
    }

    table = new JTable(model)
    table.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    table.setFocusable(false)
    table.setShowHorizontalLines(false)
    table.setShowVerticalLines(false)
    table.setForeground(Color.WHITE)
    table.setBackground(LookFeel().backgroundColor)
    table.setFillsViewportHeight(true)
    table.getTableHeader.setDefaultRenderer(new TableHeaderRenderer)

    table.setAutoCreateRowSorter(false)
    val sorter = new TableRowSorter(model)
    for (col <- 0 until colKeys.length) {
      if (col == 1) sorter.setComparator(col, Comparators.symbolComparator)
      else sorter.setComparator(col, Comparators.comparator)
    }
    // default sort order and precedence
    val sortKeys = new java.util.ArrayList[RowSorter.SortKey]
    sortKeys.add(new RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING))
    sortKeys.add(new RowSorter.SortKey(2, javax.swing.SortOrder.ASCENDING))
    sortKeys.add(new RowSorter.SortKey(1, javax.swing.SortOrder.ASCENDING))
    sortKeys.add(new RowSorter.SortKey(3, javax.swing.SortOrder.ASCENDING))
    sorter.setSortKeys(sortKeys)
    // @Note sorter.setSortsOnUpdates(true) almost work except that the cells behind sort key
    // of selected row doesn't refresh, TableRowSorter.sort manually

    table.setRowSorter(sorter)


    // --- set column width
    var columnModel = table.getColumnModel
    columnModel.getColumn(0).setMinWidth(20)
    columnModel.getColumn(1).setMinWidth(70)
    columnModel.getColumn(2).setMinWidth(10)

    val scrollPane = new JScrollPane
    scrollPane.setViewportView(table)
    scrollPane.setBackground(LookFeel().backgroundColor)
    scrollPane.setFocusable(true)

    setLayout(new BorderLayout)
    add(BorderLayout.CENTER, scrollPane)

    table.getSelectionModel.addListSelectionListener(new ListSelectionListener {
        private var prevSelected: String = _
        def valueChanged(e: ListSelectionEvent) {
          val lsm = e.getSource.asInstanceOf[ListSelectionModel]
          if (lsm.isSelectionEmpty) {
            // no rows are selected
          } else {
            val row = table.getSelectedRow
            if (row >= 0 && row < table.getRowCount) {
              val symbol = symbolAtRow(row)
              if (symbol != null && prevSelected != symbol) {
                prevSelected = symbol
                SymbolNodes.findSymbolNode(symbol) foreach {x =>
                  val viewAction = x.getLookup.lookup(classOf[ViewAction])
                  viewAction.putValue(AnalysisChartTopComponent.STANDALONE, false)
                  viewAction.execute
                }
              }
            }
          }
        }
      })

  }

  def symbolAtRow(row: Int): String = {
    if (row >= 0 && row < model.getRowCount) {
      table.getValueAt(row, 1).asInstanceOf[String]
    } else null
  }

  /**
   * Update last execution row in depth table
   */
  private def updateSignalTable(event: SignalX) {
    signalEvents += event
    model.fireTableDataChanged
    scrollToLastRow(table)
  }

  private def scrollToLastRow(table: JTable) {
    if (table.getRowCount < 1) return
    
    // wrap in EDT to wait enough time to get rowCount updated
    SwingUtilities.invokeLater(new Runnable {
        def run {
          showCell(table, table.getRowCount - 1, 0)
        }
      }
    )
  }

  private def showCell(table: JTable, row: Int, column: Int) {
    val rect = table.getCellRect(row, column, true)
    table.scrollRectToVisible(rect)
  }

  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_ALWAYS
  }

  override protected def preferredID: String = tc_id

  override def getDisplayName: String = BUNDLE.getString("CTL_SignalTopComponent")


  class TableHeaderRenderer extends JLabel with TableCellRenderer {
    private val defaultFont = new Font("Dialog", Font.BOLD, 12)
    setOpaque(true)

    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      setForeground(Color.WHITE)
      setBackground(LookFeel().backgroundColor)
      setFont(defaultFont)
      setHorizontalAlignment(SwingConstants.CENTER)
      setText(value.toString)

      setToolTipText(value.toString)

      this
    }

    // The following methods override the defaults for performance reasons
    override def validate {}
    override def revalidate() {}
    override protected def firePropertyChange(propertyName: String, oldValue: Object, newValue: Object) {}
    override def firePropertyChange(propertyName: String, oldValue: Boolean, newValue: Boolean) {}
  }

  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {
    private val defaultFont = new Font("Dialog", Font.PLAIN, 12)
    private val bgColorSelected = new Color(56, 86, 111)
    setOpaque(true)

    override def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                               hasFocus: Boolean, row: Int, column: Int): Component = {

      /** Beacuse this will be a sinleton for all cells, so, should clear it first */
      setFont(defaultFont)
      if (isSelected) {
        setBackground(bgColorSelected)
      } else {
        setBackground(LookFeel().backgroundColor)
      }
      setForeground(Color.WHITE)

      if (value != null) {
        column match {
          case 0 => // Time
            setHorizontalAlignment(SwingConstants.LEADING)
          case 1 => // Symbol
            setHorizontalAlignment(SwingConstants.TRAILING)
          case 2 => // Sign
            setHorizontalAlignment(SwingConstants.CENTER)
            if (row >= 0) {
              value.toString.trim match {
                case "买" | "卖空"=> setForeground(LookFeel().getPositiveBgColor)
                case "卖" | "买空" => setForeground(LookFeel().getNegativeBgColor)
                case _  => setForeground(LookFeel().getNegativeBgColor)
              }
            }
          case 3 => // Name
            setHorizontalAlignment(SwingConstants.LEADING)
        }

        setText(value.toString)
      } else {
        setText(null)
      }

      this
    }
  }

}
