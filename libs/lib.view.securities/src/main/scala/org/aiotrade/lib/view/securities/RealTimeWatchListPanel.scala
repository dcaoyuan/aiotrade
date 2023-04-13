/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.view.securities

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.KeyStroke
import javax.swing.RowSorter
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.securities.api
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.LightTicker
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable

/**
 *
 * @author  Caoyuan Deng
 */
class RealTimeWatchListPanel extends JPanel with Reactor {
  import RealTimeWatchListPanel._

  private val log = Logger.getLogger(this.getClass.getName)

  private val SYMBOL     = "Symbol"
  private val NAME       = "Name"
  private val TIME       = "Time"
  private val LAST_PRICE = "Last"
  private val PERCENT    = "Percent"
  private val DAY_VOLUME = "Volume"
  private val DAY_AMOUNT = "Amount"
  private val PREV_CLOSE = "PrevCls"
  private val DAY_CHANGE = "Change"
  private val DAY_HIGH   = "High"
  private val DAY_LOW    = "Low"
  private val DAY_OPEN   = "Open"

  private val colKeys = Array[String](
    SYMBOL,
    NAME,
    TIME,
    LAST_PRICE,
    PERCENT,
    DAY_VOLUME,
    DAY_AMOUNT,
    PREV_CLOSE,
    DAY_CHANGE,
    DAY_HIGH,
    DAY_LOW,
    DAY_OPEN
  )
  
  private val SYMBOL_COL = colKeys.indexOf(SYMBOL) // @todo, update when columns order changed

  private val uniSymbols = new ArrayList[String]
  private val watchingSymbols = mutable.Set[String]() // symbols will list in this pael

  private class Info {
    val prevTicker = new Ticker
    val colKeyToColor = mutable.Map[String, Color]()
    for (key <- colKeys) {
      colKeyToColor(key) = LookFeel().nameColor
    }
  }
  private val symbolToInfo = mutable.Map[String, Info]()
  
  val table = new JTable
  private val model = new WatchListTableModel
  private val df = new SimpleDateFormat("HH:mm:ss")
  private val cal = Calendar.getInstance
  private val priceDf = new DecimalFormat("0.000")

  initTable

  val scrollPane = new JScrollPane()
  scrollPane.setViewportView(table)
  scrollPane.setBackground(LookFeel().backgroundColor)
  scrollPane.setFocusable(true)

  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollUp"),   "pageup",   meta_pgup, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  scrollPane.registerKeyboardAction(scrollPane.getActionMap.get("scrollDown"), "pagedown", meta_pgdn, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)

  //scrollPane.getVerticalScrollBar.setUI(new BasicScrollBarUI)

  setLayout(new BorderLayout)
  add(BorderLayout.CENTER, scrollPane)

  reactions += {
    case api.TickersEvt(tickers) => updateByTickers(tickers)
  }

  listenTo(TickerServer.publishers: _*)

  /** forward focus to scrollPane, so it can response UP/DOWN key event etc */
  override def requestFocusInWindow: Boolean = {
    scrollPane.requestFocusInWindow
  }

  private def initTable {
    table.setFont(LookFeel().defaultFont)
    table.setModel(
      new DefaultTableModel(
        Array[Array[Object]](),
        Array[Object]()
      )
    )
    table.setModel(model)
    table.setDefaultRenderer(classOf[Object], new TrendSensitiveCellRenderer)
    table.setBackground(LookFeel().backgroundColor)
    table.setGridColor(LookFeel().backgroundColor)
    table.setFillsViewportHeight(true)
    val header = table.getTableHeader
    header.setDefaultRenderer(new TableHeaderRenderer)
    header.setReorderingAllowed(true)

    // --- sorter
    table.setAutoCreateRowSorter(false)
    val sorter = new TableRowSorter(model)
    for (col <- 0 until model.getColumnCount) {
      if (col == 0) sorter.setComparator(col, Comparators.symbolComparator)
      else sorter.setComparator(col, Comparators.comparator)
    }
    // default sort order and precedence
    val sortKeys = new java.util.ArrayList[RowSorter.SortKey]
    sortKeys.add(new RowSorter.SortKey(colKeys.indexWhere(_ == PERCENT), javax.swing.SortOrder.DESCENDING))
    sorter.setSortKeys(sortKeys)
    // @Note sorter.setSortsOnUpdates(true) almost work except that the cells behind sort key
    // of selected row doesn't refresh, TableRowSorter.sort manually

    table.setRowSorter(sorter)
  }

  class WatchListTableModel extends AbstractTableModel {
    private val colNames = {
      val names = new Array[String](colKeys.length)
      var i = 0
      while (i < colKeys.length) {
        names(i) = BUNDLE.getString(colKeys(i))
        i += 1
      }
      names
    }

    private val types = Array(
      classOf[String], classOf[String], classOf[String], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object], classOf[Object]
    )

    def getRowCount: Int = uniSymbols.size
    def getColumnCount: Int = colNames.length

    def getValueAt(row: Int, col: Int): Object = {
      val symbol = uniSymbols(row)
      val sec = Exchange.secOf(symbol) getOrElse {return null}
      val exchange = sec.exchange
      
      exchange.uniSymbolToLastTradingDayTicker.get(symbol) match {
        case Some(ticker) =>
          colKeys(col) match {
            case SYMBOL => symbol
            case NAME => sec.name
            case TIME =>
              val tz = exchange.timeZone
              val cal = Calendar.getInstance(tz)
              cal.setTimeInMillis(ticker.time)
              df.setTimeZone(tz)
              df format cal.getTime
            case LAST_PRICE => priceDf   format ticker.lastPrice
            case PERCENT    => "%3.2f%%" format ticker.changeInPercent
            case DAY_VOLUME => "%10.0f"  format ticker.dayVolume / 100.0
            case DAY_AMOUNT => "%10.2f"  format ticker.dayAmount / 10000.0
            case PREV_CLOSE => priceDf   format ticker.prevClose
            case DAY_CHANGE => priceDf   format ticker.dayChange
            case DAY_HIGH   => priceDf   format ticker.dayHigh
            case DAY_LOW    => priceDf   format ticker.dayLow
            case DAY_OPEN   => priceDf   format ticker.dayOpen
            case _ => null
          }
        case None =>
          colKeys(col) match {
            case SYMBOL => symbol
            case NAME => sec.name
            case TIME       => "-"
            case LAST_PRICE => "-"
            case PERCENT    => "-"
            case DAY_VOLUME => "-"
            case DAY_AMOUNT => "-"
            case PREV_CLOSE => "-"
            case DAY_CHANGE => "-"
            case DAY_HIGH   => "-"
            case DAY_LOW    => "-"
            case DAY_OPEN   => "-"
            case _ => null
          }
      }

    }

    override def getColumnName(col: Int) = colNames(col)

    override def getColumnClass(columnIndex: Int): Class[_] = {
      types(columnIndex)
    }

    override def isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = false
  }

  def updateByTickers(tickers: Array[Ticker]) {
    /*
     * To avoid:
     java.lang.NullPointerException
     at javax.swing.DefaultRowSorter.convertRowIndexToModel(DefaultRowSorter.java:501)
     at javax.swing.JTable.convertRowIndexToModel(JTable.java:2620)
     at javax.swing.JTable.getValueAt(JTable.java:2695)
     at javax.swing.JTable.prepareRenderer(JTable.java:5712)
     at javax.swing.plaf.basic.BasicTableUI.paintCell(BasicTableUI.java:2072)
     * We should call addRow, removeRow, setValueAt etc in EventDispatchThread
     */
    SwingUtilities.invokeLater(new Runnable {
        def run {
          log.info("Batch updating, tickers: " + tickers.length)
          var isUpdated = false
          var i = -1
          while ({i += 1; i < tickers.length}) {
            val ticker = tickers(i)
            if (watchingSymbols.contains(ticker.uniSymbol)) {
              isUpdated = updateByTicker(ticker) | isUpdated // don't use shortcut one: "||"
            }
          }

          if (isUpdated) {
            table.getRowSorter.asInstanceOf[TableRowSorter[_]].sort() // force to re-sort all rows
            table.getModel.asInstanceOf[AbstractTableModel].fireTableDataChanged()
          }
        }
      })
  }

  private def updateByTicker(ticker: LightTicker): Boolean = {
    if (ticker == null) return false

    val symbol = ticker.uniSymbol
    if (!uniSymbols.contains(symbol)) {
      uniSymbols += symbol
    }

    val (info, dayFirst) = symbolToInfo.get(symbol) match {
      case Some(x) => (x, false)
      case None =>
        val x = new Info
        symbolToInfo.put(symbol, x)
        (x, true)
    }

    val prevTicker = info.prevTicker
    /**
     * @Note
     * Should set columeColors[] before addRow() or setValue() of table to
     * make the color effects take place at once.
     */
    var isUpdated = false
    val colKeyToColor = info.colKeyToColor
    if (dayFirst) {
      setColColorsByTicker(info, ticker)
      isUpdated = true
    } else {
      if (ticker.isDayVolumeGrown(prevTicker)) {
        setColColorsByTicker(info, ticker)
        isUpdated = true
      }
    }

    prevTicker.copyFrom(ticker)
    
    isUpdated
  }

  private val SWITCH_COLOR_A = LookFeel().nameColor
  private val SWITCH_COLOR_B = new Color(128, 192, 192) //Color.CYAN;

  private def neuColor = LookFeel().getNeutralBgColor
  private def posColor = LookFeel().getPositiveBgColor
  private def negColor = LookFeel().getNegativeBgColor

  private def setColColorsByTicker(info: Info, ticker: LightTicker) {
    val fgColor = LookFeel().nameColor

    val colKeyToColor = info.colKeyToColor
    for (key <- colKeyToColor.keysIterator) {
      colKeyToColor(key) = fgColor
    }

    /** color of volume should be recorded for switching between two colors */
    colKeyToColor(DAY_VOLUME) = fgColor

    if (ticker.dayChange > 0) {
      colKeyToColor(DAY_CHANGE) = posColor
      colKeyToColor(PERCENT)    = posColor
    } else if (ticker.dayChange < 0) {
      colKeyToColor(DAY_CHANGE) = negColor
      colKeyToColor(PERCENT)    = negColor
    } else {
      colKeyToColor(DAY_CHANGE) = neuColor
      colKeyToColor(PERCENT)    = neuColor
    }

    setColorByPrevClose(colKeyToColor, ticker.prevClose, ticker.dayOpen,   DAY_OPEN)
    setColorByPrevClose(colKeyToColor, ticker.prevClose, ticker.dayHigh,   DAY_HIGH)
    setColorByPrevClose(colKeyToColor, ticker.prevClose, ticker.dayLow,    DAY_LOW)
    setColorByPrevClose(colKeyToColor, ticker.prevClose, ticker.lastPrice, LAST_PRICE)

    if (ticker.isDayVolumeChanged(info.prevTicker)) {
      /** lastPrice's color */
      /* ticker.compareLastCloseTo(prevTicker) match {
       case 1 =>
       symbolToColColor += (LAST_PRICE -> positiveColor)
       case 0 =>
       symbolToColColor += (LAST_PRICE -> neutralColor)
       case -1 =>
       symbolToColColor += (LAST_PRICE -> negativeColor)
       case _ =>
       } */

      /** volumes color switchs between two colors if ticker renewed */
      if (colKeyToColor(DAY_VOLUME) == SWITCH_COLOR_A) {
        colKeyToColor(DAY_VOLUME) = SWITCH_COLOR_B
      } else {
        colKeyToColor(DAY_VOLUME) = SWITCH_COLOR_A
      }
    }
  }

  private def setColorByPrevClose(colKeyToColor: mutable.Map[String, Color], prevClose: Double, value: Double, columnName: String) {
    if (value > prevClose) {
      colKeyToColor(columnName) = posColor
    } else if (value < prevClose) {
      colKeyToColor(columnName) = negColor
    } else {
      colKeyToColor(columnName) = neuColor
    }
  }


  def watch(sec: Sec) {
    val uniSymbol = sec.uniSymbol
    watchingSymbols += uniSymbol
  }

  def unWatch(sec: Sec) {
    val uniSymbol = sec.uniSymbol
    watchingSymbols -= uniSymbol
  }

  def clearAllWatch {
    symbolToInfo.clear
  }

  def symbolAtRow(row: Int): String = {
    if (row >= 0 && row < model.getRowCount) {
      table.getValueAt(row, SYMBOL_COL).asInstanceOf[String]
    } else null
  }
  
  class TrendSensitiveCellRenderer extends JLabel with TableCellRenderer {
    private val defaultFont = new Font("Dialog", Font.PLAIN, 12)
    private val bgColorSelected = new Color(56, 86, 111)//new Color(24, 24, 24) //new Color(169, 178, 202)
    setOpaque(true)
    
    def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean,
                                      hasFocus: Boolean, row: Int, col: Int): Component = {

      setFont(defaultFont)
      
      /**
       * @Note
       * Here we should use table.getColumeName(column) which is
       * not the same as tableModel.getColumeName(column).
       * Especially: after you draged and moved the table colume, the
       * column index of table will change, but the column index
       * of tableModel will remain the same.
       */
      val symbol = symbolAtRow(row)
      symbolToInfo.get(symbol) match {
        case Some(info) =>
          if (isSelected) {
            setBackground(bgColorSelected)
          } else {
            setBackground(LookFeel().backgroundColor)
          }

          val colKeyToColor = info.colKeyToColor
          val colKey = colKeys(col)
          setForeground(colKeyToColor(colKey))

          if (value != null) {
            colKey match {
              case SYMBOL => setHorizontalAlignment(SwingConstants.LEADING)
              case _      => setHorizontalAlignment(SwingConstants.TRAILING)
            }

            setText(value.toString)
          } else {
            setText(null)
          }
        case None =>
      }

      this
    }
  }

}

object RealTimeWatchListPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.view.securities.Bundle")

  val orig_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_UP.toChar)   // Fn + UP   in mac
  val orig_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_PAGE_DOWN.toChar) // Fn + DOWN in mac
  val meta_pgup = KeyStroke.getKeyStroke(KeyEvent.VK_UP.toChar,   InputEvent.META_MASK)
  val meta_pgdn = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN.toChar, InputEvent.META_MASK)
}

