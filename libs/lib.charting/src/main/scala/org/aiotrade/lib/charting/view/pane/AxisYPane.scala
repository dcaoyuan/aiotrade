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

import java.text.DecimalFormat
import java.text.ParseException
import javax.swing.JLabel
import javax.swing.SwingConstants
import org.aiotrade.lib.charting.widget.PathWidget
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartValidityObserver
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.MouseCursorObserver
import org.aiotrade.lib.charting.view.ReferCursorObserver
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.widget.Label

/**
 *
 * @author Caoyuan Deng
 */
object AxisYPane {
  val CURRENCY_DECIMAL_FORMAT = new DecimalFormat("0.###")
  val COMMON_DECIMAL_FORMAT = new DecimalFormat("0.###")
}

import AxisYPane._
class AxisYPane(aview: ChartView, adatumPlane: DatumPlane) extends Pane(aview, adatumPlane) {

  private var _symmetricOnMiddleValue: Boolean = _

  setOpaque(true)
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
        case _: ChartingController =>
          updateMouseCursorLabel
      }
    })

  view.controller.addObserver(this, new ReferCursorObserver {
      val updater: Updater = {
        case _: ChartingController =>
          updateReferCursorLabel
      }
    })

  view.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case _: ChartView =>
          updateReferCursorLabel
      }
    })
    
  private def updateMouseCursorLabel {
    datumPlane.computeGeometry
    val controller = view.controller
    if (controller.isMouseEnteredAnyChartPane) {
      var y, v = 0.0
      if (datumPlane.view.isInstanceOf[WithQuoteChart]) {
        if (datumPlane.isMouseEntered) {
          y = datumPlane.yMouse
          v = datumPlane.vy(y)
        } else {
          val mouseRow = controller.mouseCursorRow
          val quoteSer = datumPlane.view.asInstanceOf[WithQuoteChart].quoteSer
          val time = quoteSer.timeOfRow(mouseRow)
          v = if (quoteSer.exists(time)) quoteSer.close(time) else 0
          y = datumPlane.yv(v)
        }
        val valueStr = COMMON_DECIMAL_FORMAT.format(v)

        mouseCursorLabel.setForeground(LookFeel().mouseCursorTextColor)
        mouseCursorLabel.setBackground(LookFeel().mouseCursorTextBgColor)
        mouseCursorLabel.setFont(LookFeel().axisFont)
        mouseCursorLabel.setText(valueStr)
        val fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont)
        mouseCursorLabel.setBounds(
          3, math.round(y).toInt - fm.getHeight + 1,
          fm.stringWidth(mouseCursorLabel.getText) + 2, fm.getHeight + 1)

        mouseCursorLabel.setVisible(true)
      } else {
        if (datumPlane.isMouseEntered) {
          y = datumPlane.yMouse
          v = datumPlane.vy(y)
          val valueStr = COMMON_DECIMAL_FORMAT.format(v)

          mouseCursorLabel.setForeground(LookFeel().mouseCursorTextColor)
          mouseCursorLabel.setBackground(LookFeel().mouseCursorTextBgColor)
          mouseCursorLabel.setFont(LookFeel().axisFont)
          mouseCursorLabel.setText(valueStr)
          val fm = mouseCursorLabel.getFontMetrics(mouseCursorLabel.getFont)
          mouseCursorLabel.setBounds(3, math.round(y).toInt - fm.getHeight + 1,
                                     fm.stringWidth(mouseCursorLabel.getText) + 2, fm.getHeight + 1)

          mouseCursorLabel.setVisible(true)
        } else {
          mouseCursorLabel.setVisible(false)
        }
      }
    } else {
      mouseCursorLabel.setVisible(false)
    }
  }

  /**
   * CursorChangeEvent only notice the cursor's position changes, but the
   * referCursorLable is also aware of the refer value's changes, so we could
   * not rely on the CursorChangeEvent only, instead, we call this method via
   * syncWithView()
   */
  private def updateReferCursorLabel {
    datumPlane.computeGeometry
    val controller = view.controller

    var y, v = 0.0
    if (datumPlane.view.isInstanceOf[WithQuoteChart]) {
      val referRow = controller.referCursorRow
      val quoteSer = datumPlane.view.asInstanceOf[WithQuoteChart].quoteSer
      val time = quoteSer.timeOfRow(referRow)
      v = if (quoteSer.exists(time)) quoteSer.close(time) else 0
      y = datumPlane.yv(v)
      val valueStr = COMMON_DECIMAL_FORMAT.format(v)

      referCursorLabel.setForeground(LookFeel().referCursorTextColor)
      referCursorLabel.setBackground(LookFeel().referCursorTextBgColor)
      referCursorLabel.setFont(LookFeel().axisFont)
      referCursorLabel.setText(valueStr)
      val fm = referCursorLabel.getFontMetrics(referCursorLabel.getFont)
      referCursorLabel.setBounds(3, math.round(y).toInt - fm.getHeight + 1,
                                 fm.stringWidth(referCursorLabel.getText) + 2, fm.getHeight)

      referCursorLabel.setVisible(true)
    } else {
      referCursorLabel.setVisible(false)
    }
  }

  def syncWithView {
    updateReferCursorLabel
  }

  override protected def plotPane {
    plotAxisY
  }

  private def plotAxisY {
    val fm = getFontMetrics(getFont)
    val hFm = fm.getHeight

    var nTicks = 6f
    while (datumPlane.hCanvas / nTicks < hFm + 20 && nTicks > -2) {
      nTicks -= 2 // always keep even
    }

    val maxValueOnCanvas = datumPlane.vy(datumPlane.yCanvasUpper)
    val minValueOnCanvas =
      if (view.yControlPane != null) datumPlane.vy(datumPlane.yCanvasLower - view.yControlPane.getHeight)
    else datumPlane.vy(datumPlane.yCanvasLower)


    val vMaxTick = maxValueOnCanvas // init value, will adjust later
    var vMinTick = minValueOnCanvas // init value, will adjust later

    val vRange = vMaxTick - vMinTick
    var vTickUnit = vRange / nTicks

    if (!_symmetricOnMiddleValue) {
      vTickUnit = roundTickUnit(vTickUnit)
      vMinTick = (vMinTick / vTickUnit).toInt * vTickUnit
    }

    val pathWidget = addWidget(new PathWidget)
    pathWidget.setForeground(LookFeel().axisColor)
    val path = pathWidget.getPath
    path.reset

    /** Draw left border line */
    path.moveTo(0, 0)
    path.lineTo(0, getHeight)

    var shouldScale = false
    var i = 0
    var break = false
    while (i < nTicks + 2 && !break) {
      var vTick = vMinTick + vTickUnit * i

      val yTick = datumPlane.yv(vTick)

      if (yTick < hFm) {
        break = true
      } else {
        path.moveTo(0, yTick)
        path.lineTo(2, yTick)

        if (math.abs(vTick) >= 100000) {
          vTick = math.abs(vTick / 100000.0)
          shouldScale = true
        } else {
          vTick = math.abs(vTick)
        }

        if (i == 0 && shouldScale) {
          val multiple = "x10000"

          val label = addWidget(new Label)
          label.setForeground(LookFeel().axisColor)
          label.setFont(LookFeel().axisFont)
          label.model.set(4, yTick, multiple)
          label.plot
        } else {
          val label = addWidget(new Label)
          label.setForeground(if (vTick >= 0) LookFeel().axisColor else LookFeel().getNegativeColor)
          label.setFont(LookFeel().axisFont)
          label.model.set(4, yTick, COMMON_DECIMAL_FORMAT.format(vTick))
          label.plot
        }
      }

      i += 1

    }

  }

  /**
   * Try to round tickUnit
   */
  private def roundTickUnit(avTickUnit: Double): Double = {
    /** sample : 0.032 */
    var vTickUnit = avTickUnit
    val roundedExponent = math.round(math.log10(vTickUnit)).toInt - 1   // -2
    val adjustFactor = math.pow(10, -roundedExponent)               // 100
    val adjustedValue = (vTickUnit * adjustFactor).toInt       // 3.2 -> 3
    vTickUnit = adjustedValue.toDouble / adjustFactor     // 0.03

    /** following DecimalFormat <-> double converts are try to round the decimal */
    if (vTickUnit <= 0.001) {
      /** for currency */
      vTickUnit = 0.001
    } else if (vTickUnit > 0.001 && vTickUnit < 0.005) {
      /** for currency */
      val unitStr = CURRENCY_DECIMAL_FORMAT.format(vTickUnit)
      try {
        vTickUnit = CURRENCY_DECIMAL_FORMAT.parse(unitStr.trim).doubleValue
      } catch {case ex: ParseException => ex.printStackTrace}
    } else if (vTickUnit > 0.005 && vTickUnit < 1) {
      /** for stock */
      val unitStr = COMMON_DECIMAL_FORMAT.format(vTickUnit)
      try {
        vTickUnit = COMMON_DECIMAL_FORMAT.parse(unitStr.trim).doubleValue
      } catch {case ex: ParseException => ex.printStackTrace}
    }

    vTickUnit
  }

  def isSymmetricOnMiddleValue = _symmetricOnMiddleValue
  def isSymmetricOnMiddleValue_=(b: Boolean) {
    this._symmetricOnMiddleValue = b
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    view.controller.removeObserversOf(this)
    view.removeObserversOf(this)

    super.finalize
  }
}
