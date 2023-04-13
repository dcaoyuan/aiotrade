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
package org.aiotrade.lib.charting.view.pane

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import javax.swing.JLabel
import javax.swing.SwingConstants
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartValidityObserver
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.MouseCursorObserver
import org.aiotrade.lib.charting.view.ReferCursorObserver
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.charting.widget.PathWidget

/**
 *
 * @author Caoyuan Deng
 */
class AxisXPane(aview: ChartView, adatumPlane: DatumPlane) extends Pane(aview, adatumPlane) {

  private val TICK_SPACING = 100 // in pixels
  private var timeZone: TimeZone = _
  private var cal: Calendar = _
  private var currDate: Date = _
  private var prevDate: Date = _


  setTimeZone(TimeZone.getDefault)

  setOpaque(true);
  setRenderStrategy(RenderStrategy.NoneBuffer)

  private val mouseCursorLabel = new JLabel
  mouseCursorLabel.setOpaque(true)
  mouseCursorLabel.setHorizontalAlignment(SwingConstants.CENTER)
  mouseCursorLabel.setVisible(false)

  private val referCursorLabel = new JLabel
  referCursorLabel.setOpaque(true)
  referCursorLabel.setHorizontalAlignment(SwingConstants.CENTER)
  referCursorLabel.setVisible(false)

  setLayout(null)
  add(mouseCursorLabel)
  add(referCursorLabel)

  view.controller.addObserver(this, new MouseCursorObserver {
      val updater: Updater = {
        case subject: ChartingController =>
          updateMouseCursorLabel
      }
    })

  view.controller.addObserver(this, new ReferCursorObserver {
      val updater: Updater = {
        case subject: ChartingController =>
          updateReferCursorLabel
      }
    })

  view.controller.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case subject: ChartingController =>
          updateReferCursorLabel
      }
    })
  

  def setTimeZone(timeZone: TimeZone) {
    this.timeZone = timeZone
    this.cal = Calendar.getInstance(timeZone)
    this.currDate = cal.getTime
    this.prevDate = cal.getTime
  }

  private def updateMouseCursorLabel {
    datumPlane.computeGeometry
    val controller = view.controller

    if (controller.isMouseEnteredAnyChartPane) {
      val mousePosition = controller.mouseCursorRow
      val mouseTime = controller.mouseCursorTime
      val freq = controller.baseSer.freq
      val x = datumPlane.xr(mousePosition).toInt
      cal.setTimeInMillis(mouseTime)
      val dateStr = freq.unit.formatNormalDate(cal.getTime, timeZone)

      mouseCursorLabel.setForeground(LookFeel().mouseCursorTextColor)
      mouseCursorLabel.setBackground(LookFeel().mouseCursorTextBgColor)
      mouseCursorLabel.setFont(LookFeel().axisFont)
      mouseCursorLabel.setText(dateStr)
      val fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont)
      mouseCursorLabel.setBounds(x + 1, 1, fm.stringWidth(mouseCursorLabel.getText) + 2, getHeight - 2)

      mouseCursorLabel.setVisible(true)
    } else {
      mouseCursorLabel.setVisible(false)
    }

  }

  private def updateReferCursorLabel {
    datumPlane.computeGeometry
    val controller = view.controller

    val referPosition = controller.referCursorRow
    val referTime = controller.referCursorTime
    val freq = controller.baseSer.freq
    val x = datumPlane.xr(referPosition).toInt
    cal.setTimeInMillis(referTime)
    val dateStr = freq.unit.formatNormalDate(cal.getTime, timeZone)

    referCursorLabel.setForeground(LookFeel().referCursorTextColor)
    referCursorLabel.setBackground(LookFeel().referCursorTextBgColor)
    referCursorLabel.setFont(LookFeel().axisFont)
    referCursorLabel.setText(dateStr)
    val fm = referCursorLabel.getFontMetrics(referCursorLabel.getFont)
    referCursorLabel.setBounds(x + 1, 1, fm.stringWidth(referCursorLabel.getText) + 2, getHeight - 2)

    referCursorLabel.setVisible(true)
  }

  def syncWithView {
    updateReferCursorLabel
  }

  override protected def plotPane {
    plotAxisX
  }

  private def plotAxisX {
    val nTicks = getWidth / TICK_SPACING

    val nBars = datumPlane.nBars
    /** bTickUnit(bars per tick) cound not be 0, actually it should not less then 2 */
    var bTickUnit = math.round(nBars.toDouble / nTicks.toDouble)
    if (bTickUnit < 2) {
      bTickUnit = 2
    }

    val pathWidget = addWidget(new PathWidget)
    pathWidget.setForeground(LookFeel().axisColor)
    val path = pathWidget.getPath
    path.reset

    /** Draw border line */
    path.moveTo(0, 0)
    path.lineTo(getWidth, 0)
    path.moveTo(0, getHeight - 1)
    path.lineTo(getWidth, getHeight - 1)

    val hTick = getHeight
    val xLastTick = datumPlane.xb(nBars)
    var i = 1
    while (i <= nBars) {
      if (i % bTickUnit == 0 || i == nBars || i == 1) {
        val xCurrTick = datumPlane.xb(i)

        if (xLastTick - xCurrTick < TICK_SPACING && i != nBars) {
          /** too close */
        } else {
          path.moveTo(xCurrTick, 1)
          path.lineTo(xCurrTick, hTick)

          val time = datumPlane.tb(i)
          cal.setTimeInMillis(time)
          currDate = cal.getTime
          var stridingDate = false
          val freqUnit = view.mainSer.freq.unit
          freqUnit match {
            case TUnit.Day =>
              cal.setTime(currDate)
              val currDateYear = cal.get(Calendar.YEAR)
              cal.setTime(prevDate)
              val prevDateYear = cal.get(Calendar.YEAR)
              if (currDateYear > prevDateYear && i != nBars || i == 1) {
                stridingDate = true
              }
            case TUnit.Hour | TUnit.Minute | TUnit.Second =>
              cal.setTime(currDate)
              val currDateDay = cal.get(Calendar.DAY_OF_MONTH)
              cal.setTime(prevDate)
              val prevDateDay = cal.get(Calendar.DAY_OF_MONTH)
              if (currDateDay > prevDateDay && i != nBars || i == 1) {
                stridingDate = true
              }
            case _ =>
          }

          val dateStr = if (stridingDate) freqUnit.formatStrideDate(currDate, cal.getTimeZone)
          else freqUnit.formatNormalDate(currDate, cal.getTimeZone)

          val label = addWidget(new Label)
          label.setForeground(LookFeel().axisColor)
          label.setFont(LookFeel().axisFont)
          label.model.set(xCurrTick, getHeight - 2, " " + dateStr)
          label.plot

          cal.setTimeInMillis(time)
          prevDate = cal.getTime
        }
      }

      i += 1
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    view.controller.removeObserversOf(this)
    view.removeObserversOf(this)

    super.finalize
  }
}
