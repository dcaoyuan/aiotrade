package org.aiotrade.lib.util.swing.datepicker

import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

import javax.swing.JFrame
import javax.swing.JPanel

object DatePickerTest {
	
  def main(args: Array[String]) {
    testDatePanel
  }

  def testDatePicker {
    val testFrame = new JFrame
    testFrame.setSize(200, 180)
    testFrame.addWindowFocusListener(new WindowAdapter {
        override def windowClosing(arg0: WindowEvent) {
          System.exit(0)
        }
      })

    val panel = DatePanel()
    panel.isShowYearButtons = true

    testFrame.getContentPane.add(panel)
    testFrame.setVisible(true)
  }

  def testDatePanel {
    val testFrame = new JFrame
    testFrame.setSize(500, 500)
    testFrame.addWindowListener(new WindowAdapter {
        override def windowClosing(arg0: WindowEvent) {
          System.exit(0)
        }
      })

    val datePicker = DatePicker()
    datePicker.isTextEditable = true
    datePicker.isShowYearButtons = true
    
    val pickerPanel = new JPanel
    pickerPanel.add(datePicker)

    val panel = new JPanel
    panel.setLayout(new BorderLayout)
    panel.add(pickerPanel, BorderLayout.WEST)
    
    testFrame.getContentPane().add(panel)
    testFrame.setVisible(true)
  }

}
