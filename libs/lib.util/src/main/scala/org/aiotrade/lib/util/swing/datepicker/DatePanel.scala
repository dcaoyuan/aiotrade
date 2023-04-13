package org.aiotrade.lib.util.swing.datepicker

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.text.DateFormat
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Date
import java.util.ResourceBundle
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JSpinner
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.SpinnerModel
import javax.swing.SwingConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableModel
import org.aiotrade.lib.util.swing.datepicker.icons.NextIcon
import org.aiotrade.lib.util.swing.datepicker.icons.PreviousIcon
import scala.collection.mutable.HashSet


object DatePanel {
  def apply() = new DatePanel(null)
}

class DatePanel (model$: DateModel[_]) extends JPanel {
  private val BUNDLE = ResourceBundle.getBundle("org.aiotrade.lib.util.swing.datepicker.Bundle")
  private val dateSymbols = new DateFormatSymbols

  private val actionListeners = new HashSet[ActionListener]

  private val dateModel = new CalendarTableModel(if (model$ != null) model$ else new DateModel[Calendar])
  private val dateView = new DateView

  private var _isShowYearButtons: Boolean = false
  private var _isDoubleClickAction: Boolean = false

  setLayout(new BorderLayout)
  add(dateView, BorderLayout.CENTER)

  def addActionListener(actionListener: ActionListener) {
    actionListeners.add(actionListener)
  }

  def removeActionListener(actionListener: ActionListener) {
    actionListeners.remove(actionListener)
  }

  private def fireActionPerformed {
    for (actionListener <- actionListeners) {
      actionListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Date selected"))
    }
  }

  def isShowYearButtons = _isShowYearButtons
  def isShowYearButtons_=(showYearButtons: Boolean) {
    this._isShowYearButtons = showYearButtons
    dateView.updateShowYearButtons
  }

  def isDoubleClickAction: Boolean = _isDoubleClickAction
  def isDoubleClickAction_=(doubleClickAction: Boolean) {
    this._isDoubleClickAction = doubleClickAction
  }

  def model = dateModel.model

  private class DateView extends JPanel {

    private val eventListener = new MyEventListener
    private val buttonDimension = new Dimension(18, 20)

    val monthLabel = {
      val monthLabel = new javax.swing.JLabel
      monthLabel.setForeground(java.awt.SystemColor.activeCaptionText)
      monthLabel.setHorizontalAlignment(SwingConstants.CENTER)
      monthLabel.addMouseListener(eventListener)
      monthLabel
    }

    val yearSpinner = {
      val yearSpinner = new javax.swing.JSpinner
      yearSpinner.setModel(dateModel)
      yearSpinner
    }

    val todayLabel = {
      val todayLabel = new JLabel
      todayLabel.setHorizontalAlignment(SwingConstants.CENTER)
      todayLabel.addMouseListener(eventListener)
      val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
      todayLabel.setText(BUNDLE.getString("messages.today") + ": " + df.format(new Date))
      todayLabel
    }

    val dayTableCellRenderer = new DateTableCellRenderer

    val dayTable = {
      val dayTable = new JTable
      dayTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS)
      dayTable.setRowHeight(18)
      dayTable.setPreferredSize(new Dimension(100, 80))
      dayTable.setModel(dateModel)
      dayTable.setShowGrid(true)
      dayTable.setGridColor(Color.WHITE)
      dayTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      dayTable.setCellSelectionEnabled(true)
      dayTable.setRowSelectionAllowed(true)
      dayTable.setFocusable(false)
      dayTable.addMouseListener(eventListener)
      for (i <- 0 until 7) {
        val column = dayTable.getColumnModel.getColumn(i)
        column.setPreferredWidth(15)
        column.setCellRenderer(dayTableCellRenderer)
      }
      dayTable
    }

    val dayTableHeader = {
      val dayTableHeader = dayTable.getTableHeader
      dayTableHeader.setResizingAllowed(false)
      dayTableHeader.setReorderingAllowed(false)
      dayTableHeader.setDefaultRenderer(dayTableCellRenderer)
      dayTableHeader
    }

    val nextMonthButton = {
      val nextMonthButton = new JButton(new NextIcon(4, 7))
      nextMonthButton.setText("")
      nextMonthButton.setPreferredSize(buttonDimension)
      nextMonthButton.setBorder(BorderFactory.createLineBorder(Color.BLACK))
      nextMonthButton.setFocusable(false)
      nextMonthButton.addActionListener(eventListener)
      nextMonthButton.setToolTipText(BUNDLE.getString("messages.nextMonth"))
      nextMonthButton
    }

    val nextYearButton =  {
      val nextYearButton = new JButton(new NextIcon(8, 7, true))
      nextYearButton.setText("")
      nextYearButton.setPreferredSize(buttonDimension)
      nextYearButton.setBorder(BorderFactory.createLineBorder(Color.BLACK))
      nextYearButton.setFocusable(false)
      nextYearButton.addActionListener(eventListener)
      nextYearButton.setToolTipText(BUNDLE.getString("messages.nextYear"))
      nextYearButton
    }

    val prevMonthButton = {
      val prevMonthButton = new JButton(new PreviousIcon(4, 7))
      prevMonthButton.setText("")
      prevMonthButton.setPreferredSize(buttonDimension)
      prevMonthButton.setBorder(BorderFactory.createLineBorder(Color.BLACK))
      prevMonthButton.setFocusable(false)
      prevMonthButton.addActionListener(eventListener)
      prevMonthButton.setToolTipText(BUNDLE.getString("messages.previousMonth"))
      prevMonthButton
    }

    val prevYearButton = {
      val prevYearButton = new JButton(new PreviousIcon(8, 7, true))
      prevYearButton.setText("")
      prevYearButton.setPreferredSize(buttonDimension)
      prevYearButton.setBorder(BorderFactory.createLineBorder(Color.BLACK))
      prevYearButton.setFocusable(false)
      prevYearButton.addActionListener(eventListener)
      prevYearButton.setToolTipText(BUNDLE.getString("messages.previousYear"))
      prevYearButton
    }

    val nextButtonPanel = {
      val nextButtonPanel = new JPanel
      val layout = new GridLayout(1, 2)
      layout.setHgap(3)
      nextButtonPanel.setLayout(layout)
      nextButtonPanel.setName("")
      nextButtonPanel.setBackground(java.awt.SystemColor.activeCaption)
      nextButtonPanel.add(nextMonthButton)
      if (isShowYearButtons) {
        nextButtonPanel.add(nextYearButton)
      }
      nextButtonPanel
    }

    val prevButtonPanel = {
      val prevButtonPanel = new JPanel
      val layout = new GridLayout(1, 2)
      layout.setHgap(3)
      prevButtonPanel.setLayout(layout)
      prevButtonPanel.setName("")
      prevButtonPanel.setBackground(java.awt.SystemColor.activeCaption)
      if (isShowYearButtons) {
        prevButtonPanel.add(prevYearButton)
      }
      prevButtonPanel.add(prevMonthButton)
      prevButtonPanel
    }

    val monthPopupMenuItems = {
      val months = dateSymbols.getMonths
      val x = new Array[JMenuItem](months.length - 1)
      for (i <- 0 until months.length - 1) {
        val mi = new JMenuItem(months(i))
        mi.addActionListener(eventListener)
        x(i) = mi
      }
      x
    }

    val monthPopupMenu = {
      val x = new JPopupMenu
      val menuItems = monthPopupMenuItems
      for (i <- 0 until menuItems.length) {
        x.add(menuItems(i))
      }
      x
    }

    val northCenterPanel = {
      val x = new JPanel
      x.setLayout(new BorderLayout)
      x.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5))
      x.setOpaque(false)
      x.add(monthLabel, BorderLayout.CENTER)
      x.add(yearSpinner, BorderLayout.EAST)
      x
    }

    val northPanel = {
      val x = new JPanel
      x.setLayout(new BorderLayout)
      x.setName("")
      x.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3))
      x.setBackground(java.awt.SystemColor.activeCaption)
      x.add(prevButtonPanel, BorderLayout.WEST)
      x.add(nextButtonPanel, BorderLayout.EAST)
      x.add(northCenterPanel, BorderLayout.CENTER)
      x
    }

    val southPanel = {
      val x = new JPanel
      x.setLayout(new BorderLayout)
      //southPanel.setOpaque(false);
      x.setBackground(Color.WHITE)
      x.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3))
      x.add(todayLabel, BorderLayout.CENTER)
      x
    }

    val centerPanel = {
      val x = new JPanel
      x.setLayout(new BorderLayout)
      x.setOpaque(false)
      x.add(dayTableHeader, BorderLayout.NORTH)
      x.add(dayTable, BorderLayout.CENTER)
      x
    }

    initComponents

    private def initComponents {
      this.setLayout(new BorderLayout)
      this.setSize(200, 180)
      this.setPreferredSize(new Dimension(200, 180))
      this.setBackground(java.awt.SystemColor.activeCaptionText)
      this.setOpaque(false)
      this.add(northPanel, BorderLayout.NORTH)
      this.add(southPanel, BorderLayout.SOUTH)
      this.add(centerPanel, BorderLayout.CENTER)
      updateMonthLabel
    }

    def updateShowYearButtons {
      if (_isShowYearButtons) {
        nextButtonPanel.add(nextYearButton)
        prevButtonPanel.removeAll
        prevButtonPanel.add(prevYearButton)
        prevButtonPanel.add(prevMonthButton)
      } else {
        nextButtonPanel.remove(nextYearButton)
        prevButtonPanel.remove(prevYearButton)
      }
    }

    def updateMonthLabel {
      monthLabel.setText(dateSymbols.getMonths.apply(model.month))
    }

  }

  /**
   * This inner class renders the table of the days, setting colors based on
   * whether it is in the month, if it is today, if it is selected etc.
   *
   */
  @SerialVersionUID(-2341614459632756921L)
  private class DateTableCellRenderer extends DefaultTableCellRenderer {
    private val selectedFgColor = new Color(10, 36, 106)

    override def getTableCellRendererComponent(table: JTable, value: Object, isSelected: Boolean, hasFocus: Boolean, row: Int, col: Int): Component = {
      val label = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col).asInstanceOf[JLabel]
      label.setHorizontalAlignment(SwingConstants.CENTER)

      if (row == -1) {
        label.setForeground(selectedFgColor)
        label.setBackground(Color.WHITE)
        label.setHorizontalAlignment(SwingConstants.CENTER)
        return label
      }

      val cal = Calendar.getInstance
      cal.set(model.year, model.month, model.day)
      val today = Calendar.getInstance
      val day = value.asInstanceOf[Int]
      val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

      // Setting Foreground
      if (cal.get(Calendar.DATE) == day) {
        if (today.get(Calendar.DATE) == day &&
            today.get(Calendar.MONTH) == model.month &&
            today.get(Calendar.YEAR) == model.year) {
          label.setForeground(Color.RED)
        } else {
          label.setForeground(Color.WHITE)
        }
      } else if (day < 1 || day > lastDay) {
        label.setForeground(Color.LIGHT_GRAY)
        if (day > lastDay) {
          label.setText((day - lastDay).toString)
        } else {
          val lastMonth = Calendar.getInstance
          lastMonth.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) - 1, 1)
          val lastDayLastMonth = lastMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
          label.setText((lastDayLastMonth + day).toString)
        }
      } else {
        if (today.get(Calendar.DATE) == day &&
            today.get(Calendar.MONTH) == model.month &&
            today.get(Calendar.YEAR) == model.year) {
          label.setForeground(Color.RED)
        } else {
          label.setForeground(Color.BLACK)
        }
      }

      // Setting background
      if (cal.get(Calendar.DATE) == day) {
        label.setBackground(selectedFgColor)
      } else {
        label.setBackground(Color.WHITE)
      }

      label
    }

  }

  private class MyEventListener extends ActionListener with MouseListener {

    /**
     * Update model when Next, Previous and Month buttons clicked.
     */
    def actionPerformed(evt: ActionEvent) {
      if (evt.getSource == dateView.nextMonthButton) {
        model.addMonth(1)
      } else if (evt.getSource == dateView.prevMonthButton) {
        model.addMonth(-1)
      } else if (evt.getSource == dateView.nextYearButton) {
        model.addYear(1)
      } else if (evt.getSource == dateView.prevYearButton) {
        model.addYear(-1)
      } else {
        for (month <- 0 until dateView.monthPopupMenuItems.length if evt.getSource == dateView.monthPopupMenuItems(month)) {
          model.month = month
        }
      }
    }

    /**
     * Click on monthLabel pops up a table.
     * Click on todayLabel sets the value of the internal model to today.
     * Click on dayTable sets the day to the value.
     */
    def mouseClicked(evt: MouseEvent) {
      if (evt.getSource == dateView.monthLabel) {
        dateView.monthPopupMenu.setLightWeightPopupEnabled(false)
        dateView.monthPopupMenu.show(evt.getSource.asInstanceOf[Component], evt.getX, evt.getY)
      } else if (evt.getSource == dateView.todayLabel) {
        val today = Calendar.getInstance
        model.setDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DATE))
      } else if (evt.getSource == dateView.dayTable) {
        val row = dateView.dayTable.getSelectedRow
        val col = dateView.dayTable.getSelectedColumn
        if (row >= 0 && row <= 5) {
          val date = dateModel.getValueAt(row, col).asInstanceOf[Int]
          model.day = date

          if (_isDoubleClickAction && evt.getClickCount == 2) {
            fireActionPerformed
          }
          if (!_isDoubleClickAction) {
            fireActionPerformed
          }
        }
      }
    }

    def mouseEntered(arg0: MouseEvent) {}

    def mouseExited(arg0: MouseEvent) {}

    def mousePressed(arg0: MouseEvent) {}

    def mouseReleased(arg0: MouseEvent) {}

  }

  protected class CalendarTableModel(val model: DateModel[_]) extends TableModel with SpinnerModel with ChangeListener {

    private val spinnerChangeListeners = new HashSet[ChangeListener]
    private val tableModelListeners = new HashSet[TableModelListener]

    model.addChangeListener(this)

    def addChangeListener(arg0: ChangeListener) {
      spinnerChangeListeners.add(arg0)
    }

    def removeChangeListener(arg0: ChangeListener) {
      spinnerChangeListeners.remove(arg0)
    }

    def stateChanged(e: ChangeEvent) {
      fireValueChanged
    }

    def getNextValue: Object = {
      (model.year + 1).toString
    }

    def getPreviousValue: Object = {
      (model.year - 1).toString
    }

    def setValue(text: Object) {
      model.year = text.asInstanceOf[String].toInt
    }

    def getValue: Object = {
      model.year.toString
    }

    def addTableModelListener(arg0: TableModelListener) {
      tableModelListeners.add(arg0)
    }

    def removeTableModelListener(arg0: TableModelListener) {
      tableModelListeners.remove(arg0)
    }

    def getColumnCount: Int = 7
    def getColumnName(arg0: Int): String = {
      val shortDays = dateSymbols.getShortWeekdays
      shortDays(arg0 + 1)
    }

    def getColumnClass(arg0: Int): Class[_] = classOf[Int]

    def getRowCount: Int = 6

    def getValueAt(arg0: Int, arg1: Int): Object = {
      val firstDayOfMonth = Calendar.getInstance
      firstDayOfMonth.set(model.year, model.month, 1)
      val week = firstDayOfMonth.get(Calendar.DAY_OF_WEEK)
      val value = arg1 - week + arg0 * 7 + 2
      value.asInstanceOf[AnyRef]
    }

    def isCellEditable(arg0: Int, arg1: Int) = false

    def setValueAt(arg0: Object, arg1: Int, arg2: Int) {}

    private def fireValueChanged {
      for (l <- spinnerChangeListeners) {
        l.stateChanged(new ChangeEvent(this))
      }

      dateView.updateMonthLabel

      for (l <- tableModelListeners) {
        l.tableChanged(new TableModelEvent(this))
      }
    }

  }

}
