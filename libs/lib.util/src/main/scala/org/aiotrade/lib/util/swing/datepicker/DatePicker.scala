package org.aiotrade.lib.util.swing.datepicker

import java.awt.Color
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.HierarchyBoundsListener
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.text.DateFormat
import java.text.ParseException
import java.util.Calendar
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JFormattedTextField
import javax.swing.JPanel
import javax.swing.Popup
import javax.swing.PopupFactory
import javax.swing.SpringLayout
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

object DatePicker {
  def apply() = new DatePicker(new DatePanel(null), null)
  def apply(model: DateModel[_]) = new DatePicker(new DatePanel(model), null)
  def apply(dateFormatter: JFormattedTextField.AbstractFormatter) = new DatePicker(new DatePanel(null), dateFormatter)
  def apply(model: DateModel[_], dateFormatter: JFormattedTextField.AbstractFormatter) = new DatePicker(new DatePanel(model), dateFormatter)
}

class DatePicker(val datePanel: DatePanel, formatter: JFormattedTextField.AbstractFormatter) extends JPanel {

  def this(dateInstantPanel: DatePanel) = this(dateInstantPanel, null)

  private val internalEventHandler = new MyEventHandler
  private var popup: Option[Popup] = None

  datePanel.setBorder(BorderFactory.createLineBorder(Color.BLACK))

  private val layoutx = new SpringLayout
  setLayout(layoutx)

  val formattedTextField = new JFormattedTextField(if (formatter != null) formatter else new DateFieldFormatter)
  formattedTextField.setEditable(false)
  add(formattedTextField)
  setTextFieldValue(formattedTextField, model.year, model.month, model.day)
  layoutx.putConstraint(SpringLayout.WEST, formattedTextField, 0, SpringLayout.WEST, this)
  layoutx.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, formattedTextField)

  private val button = new JButton("...")
  button.setFocusable(true)
  add(button)
  layoutx.putConstraint(SpringLayout.WEST, button, 1, SpringLayout.EAST, formattedTextField)
  layoutx.putConstraint(SpringLayout.EAST, this, 0, SpringLayout.EAST, button)
  layoutx.putConstraint(SpringLayout.SOUTH, this, 0, SpringLayout.SOUTH, button)

  private val h = button.getPreferredSize.getHeight.toInt
  private val w = datePanel.getPreferredSize.getWidth.toInt
  button.setPreferredSize(new Dimension(h, h))
  formattedTextField.setPreferredSize(new Dimension(w - h - 1, h))

  addHierarchyBoundsListener(internalEventHandler)
  //TODO addAncestorListener(listener)
  button.addActionListener(internalEventHandler)
  formattedTextField.addPropertyChangeListener("value", internalEventHandler)
  datePanel.addActionListener(internalEventHandler)
  datePanel.model.addChangeListener(internalEventHandler)

  class DateFieldFormatter extends JFormattedTextField.AbstractFormatter {
    private val format: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    @throws(classOf[ParseException])
    override
    def valueToString(value: Object): String = {
      value match {
        case cal: Calendar => format.format(cal.getTime)
        case _ => ""
      }
    }

    @throws(classOf[ParseException])
    override
    def stringToValue(text: String): Object = {
      text match {
        case null | "" => null
        case _ =>
          val date = format.parse(text)
          val cal = Calendar.getInstance
          cal.setTime(date)
          cal
      }
    }
  }

  private def createPopup = {
    datePanel.setVisible(true)
    val location = getLocationOnScreen
    Some((new PopupFactory).getPopup(this, datePanel, location.getX.toInt, (location.getY.toInt + this.getHeight)))
  }

  def addActionListener(actionListener: ActionListener) {
    datePanel.addActionListener(actionListener)
  }

  def removeActionListener(actionListener: ActionListener) {
    datePanel.removeActionListener(actionListener)
  }

  def model: DateModel[_] = datePanel.model

  def isDoubleClickAction: Boolean = datePanel.isDoubleClickAction
  def isDoubleClickAction_=(doubleClickAction: Boolean) {
    datePanel.isDoubleClickAction = doubleClickAction
  }

  def isShowYearButtons: Boolean = datePanel.isShowYearButtons
  def isShowYearButtons_=(showYearButtons: Boolean) {
    datePanel.isShowYearButtons = showYearButtons
  }

  def isTextEditable: Boolean = formattedTextField.isEditable
  def isTextEditable_=(editable: Boolean) {
    formattedTextField.setEditable(editable)
  }

  def isButtonFocusable: Boolean = button.isFocusable
  def isButtonFocusable_=(focusable: Boolean) {
    button.setFocusable(focusable)
  }

  private def showPopup {
    if (popup.isEmpty) {
      popup = createPopup
      popup foreach {_.show}
    }
  }

  private def hidePopup {
    popup foreach {_.hide}
    popup = None
  }

  private def setTextFieldValue(textField: JFormattedTextField, year: Int, month: Int, day: Int) {
    val cal = Calendar.getInstance
    cal.set(year, month, day, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    textField.setValue(cal)
  }

  private class MyEventHandler extends ActionListener with HierarchyBoundsListener with ChangeListener with PropertyChangeListener {

    def ancestorMoved(arg0: HierarchyEvent) {
      hidePopup
    }

    def ancestorResized(arg0: HierarchyEvent) {
      hidePopup
    }

    def actionPerformed(arg0: ActionEvent) {
      if (arg0.getSource == button) {
        if (popup.isEmpty) {
          showPopup
        } else {
          hidePopup
        }
      } else if (arg0.getSource == datePanel){
        hidePopup
      }
    }

    def stateChanged(arg0: ChangeEvent) {
      if (arg0.getSource == model) {
        setTextFieldValue(formattedTextField, model.year, model.month, model.day)
      }
    }

    def propertyChange(evt: PropertyChangeEvent) {
      if (formattedTextField.isEditable && formattedTextField.getValue != null) {
        val value = formattedTextField.getValue.asInstanceOf[Calendar]
        model.setDate(value.get(Calendar.YEAR), value.get(Calendar.MONTH), value.get(Calendar.DATE))
      }
    }

  }

}