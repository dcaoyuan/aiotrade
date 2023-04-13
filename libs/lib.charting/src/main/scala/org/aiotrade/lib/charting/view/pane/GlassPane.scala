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

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.text.DecimalFormat
import java.util.Calendar
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.event.MouseInputAdapter
import org.aiotrade.lib.math.indicator.Indicator
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.charting.chart.CursorChart
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartValidityObserver
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.ReferCursorObserver
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.widget.Label
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.util.awt.AWTUtil
import org.aiotrade.lib.util.swing.AIOAutoHideComponent
import org.aiotrade.lib.util.swing.AIOCloseButton
import org.aiotrade.lib.util.swing.action.EditAction
import org.aiotrade.lib.util.swing.action.HideAction
import scala.actors.Reactor
import scala.collection.mutable


/**
 * GlassPane overlaps mainChartPane, and is not opaque, thus we should carefully
 * define the content of it, try the best to avoid add components on it, since
 * when the tranparent components change size, bounds, text etc will cause the
 * components repaint(), and cause the overlapped mainChartPane repaint() in chain.
 * That's why we here use a lot of none component-based lightweight textSegments,
 * pathSegments to draw texts, paths directly on GlassPane. Otherwise, should
 * carefully design container layout manage of the labels, especially do not call
 * any set property methods of labels in paint() routine.
 *
 * @author Caoyuan Deng
 */
class GlassPane($view: ChartView, $datumPlane: DatumPlane) extends {
  private val TRANSPARENT_COLOR = new Color(0, 0, 0, 0)
  private val BUTTON_SIZE = 12
  private val BUTTON_DIMENSION = new Dimension(BUTTON_SIZE, BUTTON_SIZE)
  private val MONEY_DECIMAL_FORMAT = new DecimalFormat("0.###")
} with Pane($view, $datumPlane) with WithCursorChart {

  private val overlappingSerToNameLabel = mutable.Map[TSer, (AIOCloseButton, JLabel)]()
  private val selectedSerVarToValueLabel = mutable.Map[TVar[_], JLabel]()
  private var _isSelected: Boolean = _
  private var instantValueLabel: JLabel = _
  private var _isUsingInstantTitleValue: Boolean = _

  setOpaque(false)
  setRenderStrategy(RenderStrategy.NoneBuffer)

  private val titlePanel = new JPanel
  titlePanel.setOpaque(false)
  titlePanel.setPreferredSize(new Dimension(10, view.TITLE_HEIGHT_PER_LINE))

  setLayout(new BorderLayout)
  add(titlePanel, BorderLayout.NORTH)

  private var _selectedSer = view.mainSer

  private val closeButton = createCloseButton(view.mainSer)
  private val nameLabel = createNameLabel(view.mainSer)
  private val deltaLabel = createDeltaLable

  /** Container should be JComponent instead of JPanel to show selectedMark ? */
  private val pinnedMark = new PinnedMark
  pinnedMark.setPreferredSize(BUTTON_DIMENSION)

  titlePanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0))
  titlePanel.add(closeButton)
  titlePanel.add(nameLabel)
  //titlePanel.add(pinnedMark)

  case object UpdateTitlePanel
  case object UpdateSerValues
  private val updateActor = new scala.actors.Reactor[Any] {
    start
    def act = loop {
      react {
        case UpdateTitlePanel =>
          updateMainName
          updateOverlappingNames
          if (!isUsingInstantTitleValue) {
            updateSelectedSerVarValues
          }

          titlePanel.revalidate
          titlePanel.repaint()
        case UpdateSerValues =>
          if (!isUsingInstantTitleValue) {
            updateSelectedSerVarValues
            
            titlePanel.revalidate
            titlePanel.repaint()
          }
      }
    }
  }



  val paneMouseListener = new PaneMouseInputAdapter
  addMouseListener(paneMouseListener)
  addMouseMotionListener(paneMouseListener)

  view.controller.addObserver(this, new ReferCursorObserver {
      val updater: Updater = {
        case _: ChartingController => updateActor ! UpdateSerValues
      }
    })

  view.controller.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case _: ChartingController => updateActor ! UpdateTitlePanel
      }
    })

  view.addObserver(this, new ChartValidityObserver {
      val updater: Updater = {
        case _: ChartView => updateActor ! UpdateTitlePanel
      }
    })

  /**
   * @todo updateTitle() when
   * comparing chart added
   */

  private def createCloseButton(ser: TSer): AIOCloseButton = {
    val button = new AIOCloseButton
    button.setOpaque(false)
    button.setForeground(LookFeel().axisColor)
    button.setFocusable(false)
    button.setPreferredSize(BUTTON_DIMENSION)
    button.setMaximumSize(BUTTON_DIMENSION)
    button.setMinimumSize(BUTTON_DIMENSION)
    button.setVisible(true)

    button.addActionListener(new ActionListener {

        def actionPerformed(e: ActionEvent) {
          view.getParent match {
            case _: ChartViewContainer =>
              if (ser eq selectedSer) {
                if (ser ne view.mainSer) {
                  selectedSer = view.mainSer
                } else {
                  selectedSer = null
                }
              }
              val content = view.controller.serProvider.content
              content.lookupDescriptor(classOf[IndicatorDescriptor],
                                        ser.getClass.getName,
                                        ser.freq
              ) foreach {descriptor =>
                descriptor.lookupAction(classOf[HideAction]) foreach (_.execute)
              }
          }
        }
      })

    button
  }

  private def createNameLabel(ser: TSer): JLabel = {
    val label = new JLabel
    label.setOpaque(false)
    label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    label.setHorizontalAlignment(SwingConstants.CENTER)
    label.setPreferredSize(null) // null, let layer manager to decide the size
    label.setVisible(true)
    val mouseListener = new NameLabelMouseInputListener(ser, label)
    label.addMouseListener(mouseListener)
    label.addMouseMotionListener(mouseListener)

    label
  }

  private def createDeltaLable: JLabel = {
    val label = new JLabel
    label.setOpaque(false)
    label.setHorizontalAlignment(SwingConstants.RIGHT)
    label.setVisible(false)
    label.setFont(LookFeel().axisFont)

    label
  }

  private final class NameLabelMouseInputListener(ser: TSer, label: JLabel) extends MouseInputAdapter {

    private var rolloverEffectSet: Boolean = _

    override def mouseClicked(e: MouseEvent) {
      selectedSer = ser
      isSelected = true
      if (e.getClickCount == 2) {
        val content = view.controller.serProvider.content
        content.lookupDescriptor(classOf[IndicatorDescriptor],
                                  ser.getClass.getName,
                                  ser.freq
        ) foreach {descriptor =>
          descriptor.lookupAction(classOf[EditAction]) foreach (_.execute)
        }
      }
    }

    override def mouseMoved(e: MouseEvent) {
      if (!rolloverEffectSet) {
        /** @todo */
        label.setBackground(LookFeel().borderColor)
        rolloverEffectSet = true
        label.repaint()
      }
    }

    override def mouseExited(e: MouseEvent) {
      /** @todo */
      label.setBackground(LookFeel().backgroundColor)
      rolloverEffectSet = false
      label.repaint()
    }
  }

  override protected def plotPane {
  }

  def isUsingInstantTitleValue: Boolean = _isUsingInstantTitleValue
  def isUsingInstantTitleValue_=(b: Boolean) {
    this._isUsingInstantTitleValue = b
  }

  private def updateMainName {
    closeButton.setForeground(LookFeel().axisColor)
    closeButton.setBackground(LookFeel().backgroundColor)
    if (selectedSer eq view.mainSer) {
      closeButton.setChosen(true)
    } else {
      closeButton.setChosen(false)
    }

    nameLabel.setForeground(LookFeel().nameColor)
    nameLabel.setBackground(LookFeel().backgroundColor)
    nameLabel.setFont(LookFeel().axisFont)
    nameLabel.setText(Indicator.displayName(view.mainSer))

    /** name of comparing quote */
    //        if ( view instanceof AnalysisQuoteChartView) {
    //            final FontMetrics fm = getFontMetrics(getFont());
    //            int xPositionLine2 = MARK_SIZE + 1;
    //            Map<QuoteCompareIndicator, QuoteChart> quoteCompareSerChartMap = ((AnalysisQuoteChartView)view).getQuoteCompareSerChartMap();
    //            for (Ser ser : quoteCompareSerChartMap.keySet()) {
    //                String comparingQuoteName = ser.getShortDescription() + "  ";
    //                Color color = quoteCompareSerChartMap.get(ser).getForeground();
    //                TextSegment text2 = new TextSegment(comparingQuoteName, xPositionLine2, ChartView.TITLE_HEIGHT_PER_LINE * 2, color, null);
    //                addSegment(text2);
    //                xPositionLine2 += fm.stringWidth(comparingQuoteName);
    //            }
    //        }
  }

  // should synchronized this method or call via an updateActor
  private def updateOverlappingNames {
    val overlappingSers = view.overlappingSers

    /** remove unused ser's buttons and labels */
    val toRemove = overlappingSerToNameLabel.keysIterator filter {ser => !overlappingSers.contains(ser)}
    for (ser <- toRemove) {
      val (button, label) = overlappingSerToNameLabel(ser)
      overlappingSerToNameLabel.remove(ser)
      AWTUtil.removeAllAWTListenersOf(button)
      AWTUtil.removeAllAWTListenersOf(label)
      titlePanel.remove(button)
      titlePanel.remove(label)
    }

    var idx = 2
    for (ser <- overlappingSers) {
      val (button, label) = overlappingSerToNameLabel.get(ser) match {
        case Some((button, label)) =>
          idx += 2

          (button, label)
          
        case _ =>
          val button = createCloseButton(ser)
          val label = createNameLabel(ser)

          titlePanel.add(button, idx)
          idx += 1
          titlePanel.add(label, idx)
          idx += 1
          overlappingSerToNameLabel.put(ser, (button, label))
          
          (button, label)
      }

      button.setForeground(LookFeel().axisColor)
      button.setBackground(LookFeel().backgroundColor)
      if (selectedSer eq ser) {
        button.setChosen(true)
      } else {
        button.setChosen(false)
      }

      label.setForeground(LookFeel().nameColor)
      label.setBackground(LookFeel().backgroundColor)
      label.setFont(LookFeel().axisFont)
      label.setText(Indicator.displayName(ser))
    }
  }

  /**
   * update name and valueStr of all the vars in this view's selected ser.
   * all those vars with var.getPlot() != Plot.None will be shown with value.
   */
  private def updateSelectedSerVarValues {
    val ser = selectedSer
    if (ser == null) {
      return
    }

    val referTime = view.controller.referCursorTime
    if (ser.exists(referTime)) {
      val vars = ser.vars

      /** remove unused vars and their labels */
      val toRemove = selectedSerVarToValueLabel.keysIterator filter {v => !vars.contains(v) || v.plot == Plot.None}
      for (v <- toRemove) {
        val label = selectedSerVarToValueLabel(v)
        selectedSerVarToValueLabel.remove(v)
        // label maybe null? not init yet?
        if (label != null) {
          AWTUtil.removeAllAWTListenersOf(label)
          titlePanel.remove(label)
        }
      }

      for (v <- vars if v.plot != Plot.None;
           value = v.double(referTime) if Null.not(value)
      ) {
        val vStr = " " + v.name + ": " + MONEY_DECIMAL_FORMAT.format(value)

        /** lookup this var's chart and use chart's color if possible */
        var chart: Chart = null
        val chartToVars = view.chartToVarsOf(ser)
        val charts = chartToVars.keysIterator
        while (charts.hasNext && chart == null) {
          val chartx = charts.next
          chartToVars.get(chartx) find (_.contains(v)) match {
            case Some(x) => chart = chartx
            case None =>
          }
        }
        
        val color = if (chart eq null) {
          LookFeel().nameColor
        } else {
          if (chart.depth >= 0) {
            LookFeel().getChartColor(chart.depth)
          } else {
            LookFeel().nameColor
          }
        }

        val valueLabel = selectedSerVarToValueLabel.get(v) getOrElse {
          val x = new JLabel
          x.setOpaque(false)
          x.setHorizontalAlignment(SwingConstants.LEADING)
          x.setPreferredSize(null) // null, let the UI delegate to decide the size

          titlePanel.add(x)
          selectedSerVarToValueLabel.put(v, x)
          x
        }

        valueLabel.setForeground(color)
        valueLabel.setBackground(LookFeel().backgroundColor)
        valueLabel.setFont(LookFeel().axisFont)
        valueLabel.setText(vStr)
      }

    }
  }

  def updateDeltaToRefer {
    val controller = view.controller
    view match {
      case qView: WithQuoteChart =>
        val referRow = controller.referCursorRow
        val quoteSer = qView.quoteSer

        val mouseRow = controller.mouseCursorRow
        var y, v = 0.0
        if (view.mainChartPane.isMouseEntered) {
          y = datumPlane.yMouse
          v = datumPlane.vy(y)
        } else {
          val time = quoteSer.timeOfRow(mouseRow)
          v = if (quoteSer.exists(time)) quoteSer.close(time) else 0
          y = datumPlane.yv(v)
        }
        val str =
          if (isAutoReferCursorValue) { // normal QuoteChartView
            val time = quoteSer.timeOfRow(referRow)
            val vRefer = if (quoteSer.exists(time)) quoteSer.close(time) else 0

            val period = datumPlane.br(mouseRow) - datumPlane.br(referRow)
            val percent = if (vRefer == 0) 0.0 else 100 * (datumPlane.vy(y) - vRefer) / vRefer

            var amountSum = 0.0
            val rowBeg = math.min(referRow, mouseRow)
            val rowEnd = math.max(referRow, mouseRow)
            var i = rowBeg
            while (i <= rowEnd) {
              val time = quoteSer.timeOfRow(i)
              if (quoteSer.exists(time)) {
                amountSum += quoteSer.amount(time)
              }
              i += 1
            }

            new StringBuilder(20).append("P: ").append(period).append("  ").append("%+3.2f".format(percent)).append("%").append("  A: ").append("%5.0f".format(amountSum)).toString
          } else { // else, usually RealtimeQuoteChartView
            val vRefer = GlassPane.this.referCursorValue
            val vYMouse = datumPlane.vy(y)
            val percent = if (vRefer == 0) 0.0 else 100 * (vYMouse - vRefer) / vRefer

            new StringBuilder(20).append(MONEY_DECIMAL_FORMAT.format(vYMouse)).append("  ").append("%+3.2f".format(percent)).append("%").toString
          }

        deltaLabel.setForeground(LookFeel().nameColor)
        deltaLabel.setText(str)

        //val fm = getFontMetrics(deltaLabel.getFont)
        //label.model.set(w - fm.stringWidth(str) - (BUTTON_SIZE + 1), view.TITLE_HEIGHT_PER_LINE - 2, str)
        //label.plot
      case _ =>
    }
  }

  def updateInstantValue(valueStr: String, color: Color) {
    if (instantValueLabel eq null) {
      instantValueLabel = new JLabel
      instantValueLabel.setOpaque(false)
      instantValueLabel.setHorizontalAlignment(SwingConstants.LEADING)
      instantValueLabel.setPreferredSize(null) // null, let the UI delegate to decide the size

      titlePanel.add(instantValueLabel)
    }

    instantValueLabel.setForeground(color)
    instantValueLabel.setBackground(LookFeel().backgroundColor)
    instantValueLabel.setFont(LookFeel().axisFont)
    instantValueLabel.setText(valueStr)
  }

  def isSelected = _isSelected
  def isSelected_=(b: Boolean) {
    val oldValue = isSelected
    this._isSelected = b
    if (isSelected != oldValue) {
      /** todo: still need this? */
    }
  }

  def interactive(b: Boolean) {
    closeButton.setVisible(b)
  }

  def pin(b: Boolean) {
    pinnedMark.setAutoHidden(!b)
  }


  private def selectedSer = _selectedSer
  private def selectedSer_=(selectedSer: TSer) {
    val oldOne = _selectedSer
    _selectedSer = selectedSer
    if (selectedSer ne oldOne) {
      // update selected mark and value labels
      updateActor ! UpdateTitlePanel
    }
  }


  /**
   * @NOTICE
   * This will be and only be called when I have mouse motion listener
   */
  override protected def processMouseMotionEvent(e: MouseEvent) {
    /** fire to my listeners */
    super.processMouseMotionEvent(e)

    forwardMouseEventToWhoMayBeCoveredByMe(e)
  }

  /**
   * !NOTICE
   * This will be and only be called when I have mouse listener
   */
  override protected def processMouseEvent(e: MouseEvent) {
    /** fire to my listeners */
    super.processMouseEvent(e)

    forwardMouseEventToWhoMayBeCoveredByMe(e)
  }

  private def forwardMouseEventToWhoMayBeCoveredByMe(e: MouseEvent) {
    forwardMouseEvent(this, view.mainChartPane, e)
    forwardMouseEvent(this, view.getParent, e)

    view match {
      case x: WithDrawingPane =>
        val drawingPane = x.selectedDrawing
        if (drawingPane ne null) {
          forwardMouseEvent(this, drawingPane, e)
          if (drawingPane.getSelectedHandledChart ne null) {
            setCursor(drawingPane.getSelectedHandledChart.getCursor)
          } else {
            /**
             * @credit from msayag@users.sourceforge.net
             * set to default cursor what ever, especilly when a handledChart
             * was just deleted.
             */
            setCursor(Cursor.getDefaultCursor)
          }
        }
      case _ =>
    }
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    view.controller.removeObserversOf(this)
    view.removeObserversOf(this)

    AWTUtil.removeAllAWTListenersOf(nameLabel)
    AWTUtil.removeAllAWTListenersOf(this)

    super.finalize
  }

  class PaneMouseInputAdapter extends MouseInputAdapter {

    override def mouseClicked(e: MouseEvent) {
      getActiveComponentAt(e) match {
        case Some(activeComponent) =>
          view.getParent match {
            case viewContainer: ChartViewContainer =>
              if (activeComponent eq titlePanel) {
                e.getClickCount match {
                  case 1 if viewContainer.isInteractive => viewContainer.selectedView = view
                  case 1 => if (viewContainer.isPinned) viewContainer.unPin else viewContainer.pin
                  case 2 => view.popupToDesktop
                  case _ =>
                }
              } else if (activeComponent eq pinnedMark) {
                if (view.isPinned) view.unPin else view.pin
              }
            case _ =>
          }
        case None =>
      }
    }

    override def mouseMoved(e: MouseEvent) {
      getActiveComponentAt(e)
    }

    override def mouseExited(e: MouseEvent) {
      getActiveComponentAt(e)
    }

    /**
     * Decide which componet is active and return it.
     * @return actived component or <code>null</code>
     */
    private def getActiveComponentAt(e: MouseEvent): Option[Component] = {
      val p = e.getPoint

      if (pinnedMark.contains(p)) {
        pinnedMark.setHidden(false)
        return Some(pinnedMark)
      } else {
        pinnedMark.setHidden(true)
      }

      if (titlePanel.contains(p)) {
        return Some(titlePanel)
      }

      None
    }

    override def mouseDragged(e: MouseEvent) {
    }
  }

  /**
   * Inner pinned mark class
   */
  private class PinnedMark extends AIOAutoHideComponent {

    setOpaque(false)
    setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE))

    setCursor(Cursor.getDefaultCursor)
    
    override protected def paintComponent(g0: Graphics) {
      if (isHidden) {
        return
      }

      val g = g0.asInstanceOf[Graphics2D]
      g.setColor(LookFeel().axisColor)
      val w = getWidth  - 3
      val h = getHeight - 3

      if (!autoHidden) {
        /** pinned, draw pinned mark (an filled circle) */
        g.fillOval(2, 2, w, h)
      } else {
        if (!hidden) {
          /** draw to pin mark (an filled circle) */
          g.fillOval(2, 2, w, h)
        }
      }
    }
  }

  /**
   * implement WithCursorChart
   * ----------------------------------------------------
   */
  def createCursorChartInstance(datumPlane: DatumPlane): CursorChart = {
    new MyCursorChart
  }

  private class MyCursorChart extends CursorChart {

    private val cal = Calendar.getInstance

    protected def plotReferCursor {
      val h = GlassPane.this.getHeight
      val w = GlassPane.this.getWidth

      /** plot cross' vertical line */
      if (isCursorCrossVisible) {
        cursorPath.moveTo(x, 0)
        cursorPath.lineTo(x, h)
      }

      GlassPane.this.view match {
        case x: WithQuoteChart =>
          val quoteSer = x.quoteSer
          val time = quoteSer.timeOfRow(referRow)
          if (quoteSer.exists(time)) {
            val y = if (isAutoReferCursorValue) yv(quoteSer.close(time)) else yv(referCursorValue)

            /** plot cross' horizonal line */
            if (isCursorCrossVisible) {
              cursorPath.moveTo(0, y)
              cursorPath.lineTo(w, y)
            }
          }
        case _ =>
      }

    }

    protected def plotMouseCursor {
      val w = GlassPane.this.getWidth
      val h = GlassPane.this.getHeight

      val mainChartPane = GlassPane.this.view.mainChartPane

      /** plot vertical line */
      if (isCursorCrossVisible) {
        cursorPath.moveTo(x, 0)
        cursorPath.lineTo(x, h)
      }

      var y = 0.0
      GlassPane.this.view match {
        case x: WithQuoteChart =>
          val quoteSer = x.quoteSer

          cal.setTimeInMillis(mouseTime)
          val time = quoteSer.timeOfRow(mouseRow)
          val vMouse = if (quoteSer.exists(time)) quoteSer.close(time) else 0

          if (mainChartPane.isMouseEntered) {
            y = mainChartPane.yMouse
          } else {
            y = if (quoteSer.exists(time)) mainChartPane.yv(quoteSer.close(time)) else 0
          }


          /** plot horizonal line */
          if (isCursorCrossVisible) {
            cursorPath.moveTo(0, y)
            cursorPath.lineTo(w, y)
          }

          val vDisplay = mainChartPane.vy(y)

          val str = 
            if (isAutoReferCursorValue) { // normal QuoteChartView
              val time = quoteSer.timeOfRow(referRow)
              val vRefer = if (quoteSer.exists(time)) quoteSer.close(time) else 0

              val period = br(mouseRow) - br(referRow)
              val percent = if (vRefer == 0) 0.0 else 100 * (mainChartPane.vy(y) - vRefer) / vRefer

              var amountSum = 0.0
              val rowBeg = math.min(referRow, mouseRow)
              val rowEnd = math.max(referRow, mouseRow)
              var i = rowBeg
              while (i <= rowEnd) {
                val time = quoteSer.timeOfRow(i)
                if (quoteSer.exists(time)) {
                  amountSum += quoteSer.amount(time)
                }
                i += 1
              }

              new StringBuilder(20).append("P: ").append(period).append("  ").append("%+3.2f".format(percent)).append("%").append("  A: ").append("%5.0f".format(amountSum)).toString
            } else { // else, usually RealtimeQuoteChartView
              val vRefer = GlassPane.this.referCursorValue
              val percent = if (vRefer == 0) 0.0 else 100 * (mainChartPane.vy(y) - vRefer) / vRefer

              new StringBuilder(20).append(MONEY_DECIMAL_FORMAT.format(vDisplay)).append("  ").append("%+3.2f".format(percent)).append("%").toString
            }

          val label = addChild(new Label)
          label.setForeground(laf.nameColor)
          label.setFont(laf.axisFont)

          val fm = getFontMetrics(label.getFont)
          label.model.set(w - fm.stringWidth(str) - (BUTTON_SIZE + 1), view.TITLE_HEIGHT_PER_LINE - 2, str)
          label.plot
          
        case _ => // indicator view
          if (mainChartPane.isMouseEntered) {
            y = mainChartPane.yMouse

            /** plot horizonal line */
            if (isCursorCrossVisible) {
              cursorPath.moveTo(0, y)
              cursorPath.lineTo(w, y)
            }
          }
      }

    }
  }
}
