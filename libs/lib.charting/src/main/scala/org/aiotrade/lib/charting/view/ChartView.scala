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
package org.aiotrade.lib.charting.view

import java.awt.Dimension
import javax.swing.JLayeredPane
import java.awt.Graphics
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JComponent
import org.aiotrade.lib.charting.view.pane.AxisXPane
import org.aiotrade.lib.charting.view.pane.AxisYPane
import org.aiotrade.lib.charting.view.pane.ChartPane
import org.aiotrade.lib.charting.view.pane.DivisionPane
import org.aiotrade.lib.charting.view.pane.GlassPane
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.charting.chart.ChartFactory
import org.aiotrade.lib.charting.chart.GradientChart
import org.aiotrade.lib.charting.chart.ProfileChart
import org.aiotrade.lib.charting.chart.StickChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.charting.view.pane.XControlPane
import org.aiotrade.lib.charting.view.pane.YControlPane
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.ChangeSubject
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable

/**
 * A ChartView's container can be any Component even without a ChartViewContainer,
 * but should reference back to a controller. All ChartViews shares the same
 * controller will have the same cursor behaves.
 *
 * Example: you can add a ChartView directly to a JFrame.
 *
 * baseSer: the ser instaceof BaseTSer, with the calendar time feature,
 *            it's put in the masterView to control the cursor;
 * mainSer: vs overlappingSer, this view's main ser.
 *
 *       1..n           1..n
 * ser --------> chart ------> var
 *
 *
 * ChangeSubject cases:
 *   rightSideRow
 *   referCursorRow
 *   wBar
 *   onCalendarMode
 * @author Caoyuan Deng
 */
abstract class ChartView(protected var _controller: ChartingController,
                         protected var _mainSer: TSer,
                         empty: Boolean
) extends {
  val AXISX_HEIGHT = 12
  val AXISY_WIDTH = 50
  val CONTROL_HEIGHT = 12
  val TITLE_HEIGHT_PER_LINE = 12
} with JComponent with ChangeSubject with Reactor {
  private val log = Logger.getLogger(getClass.getName)
  
  protected val overlappingSerChartToVars = mutable.Map[TSer, mutable.Map[Chart, mutable.Set[TVar[_]]]]()

  val mainSerChartToVars = mutable.Map[Chart, mutable.Set[TVar[_]]]()

  val mainChartPane = new ChartPane(this)
  val glassPane = new GlassPane(this, mainChartPane)
  val axisXPane = new AxisXPane(this, mainChartPane)
  val axisYPane = new AxisYPane(this, mainChartPane)
  val divisionPane = new DivisionPane(this, mainChartPane)
  val mainLayeredPane = new JLayeredPane {
    /** this will let the pane components getting the proper size when init */
    override protected def paintComponent(g: Graphics) {
      val width = getWidth
      val height = getHeight
      for (c <- getComponents if c.isInstanceOf[Pane]) {
        c.setBounds(0, 0, width, height)
      }
    }
  }

  private var _xControlPane: XControlPane = _
  private var _yControlPane: YControlPane = _
  /** geometry */
  private var _nBars: Int = _ // number of bars
  private var _maxValue = 1.0
  private var _minValue = 0.0
  private var _oldMaxValue = _maxValue
  private var _oldMinValue = _minValue

  private var _isInteractive = true
  private var _isPinned = false

  private var _baseSer: BaseTSer = _
  private var lastDepthOfOverlappingChart = Pane.DEPTH_CHART_BEGIN

  if (!empty) {
    init(_controller, _mainSer)
  }

  reactions += {
    case evt@TSerEvent.Computed(_, _, _, _, msg, callback) =>
      updateView(evt)
      if (msg != null) processEvtMessage(msg)
      if (callback != null) processEvtCallback(callback)
    case evt@TSerEvent.Updated(_, _, _, _, msg, callback) =>
      updateView(evt)
      if (msg != null) processEvtMessage(msg)
      if (callback != null) processEvtCallback(callback)
    case TSerEvent(_, _, _, _, msg, callback) =>
      if (msg != null) processEvtMessage(msg)
      if (callback != null) processEvtCallback(callback)
  }
  
  private def processEvtMessage(msg: String) {
    //WindowManager.getDefault().setStatusText(msg)
  }
  
  private def processEvtCallback(callback: () => Any) {
    callback() match {
      case imgFile: File =>
        getParent match {
          case viewContainer: ChartViewContainer =>
            try {
              viewContainer.saveToImage(imgFile, "png")
            } catch {
              case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
            }
          case _ =>
        }

      case (imgFile: File, fromTime: Long, toTime: Long, height: Int) =>
        getParent match {
          case viewContainer: ChartViewContainer =>
            try {
              viewContainer.saveToCustomSizeImage(imgFile, "png", fromTime, toTime, height)
            } catch {
              case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
            }
          case _ =>
        }
        
      case _ =>
    }
  }

  def this(controller: ChartingController, mainSer: TSer) = this(controller, mainSer, false)
  def this() = this(null, null, true)

  def init(controller: ChartingController, mainSer: TSer) {
    this._controller = controller
    this._baseSer = controller.baseSer
    this._mainSer = mainSer

    createBasisComponents

    initComponents

    putChartsOfMainSer

    listenTo(this._mainSer)

    /** @TODO should consider: in case of overlapping indciators, how to avoid multiple repaint() */
  }

  protected def initComponents

  private def createBasisComponents {
    setDoubleBuffered(true)

    /**
     * !NOTICE
     * To make background works, should keep three conditions:
     * 1. It should be a JPanel instead of a JComponent(which may has no background);
     * 2. It should be opaque;
     * 3. If override paintComponent(g0), should call super.paintComponent(g0) ?
     */
    setOpaque(true)

    mainLayeredPane.setPreferredSize(new Dimension(10, (10 - 10 / 6.18).toInt))
    mainLayeredPane.add(mainChartPane, JLayeredPane.DEFAULT_LAYER)

    glassPane.setPreferredSize(new Dimension(10, (10 - 10 / 6.18).toInt))

    axisXPane.setPreferredSize(new Dimension(10, AXISX_HEIGHT))
    axisYPane.setPreferredSize(new Dimension(AXISY_WIDTH, 10))
    divisionPane.setPreferredSize(new Dimension(10, 1))
  }

  /**
   * The paintComponent() method will always be called automatically whenever
   * the component need to be reconstructed as it is a JComponent.
   */
  override protected def paintComponent(g: Graphics) {
    prePaintComponent

    if (isOpaque) {
      /**
       * Process background by self,
       *
       * @NOTICE
       * don't forget to setBackgroud() to keep this component's properties consistent
       */
      setBackground(LookFeel().backgroundColor)
      g.setColor(getBackground)
      g.fillRect(0, 0, getWidth, getHeight)
    }

    /**
     * @NOTICE:
     * if we call:
     *   super.paintComponent(g);
     * here, this.paintComponent(g) will be called three times!!!, the reason
     * may be that isOpaque() == true
     */
    postPaintComponent
  }

  protected def prePaintComponent {
    computeGeometry

    /** @TODO, use notify ? */
    mainChartPane.computeGeometry
  }

  /**
   * what may affect the geometry:
   * 1. the size of this component changed;
   * 2. the rightCursorRow changed;
   * 3. the ser's value changed or its items added, which need computeMaxMin();
   *
   * The controller only define wBar (the width of each bar), this component
   * will compute number of bars according to its size. So, if you want to more
   * bars displayed, such as an appointed newNBars, you should compute the size of
   * this's container, and call container.setBounds() to proper size, then, the
   * layout manager will layout the size of its ChartView instances automatically,
   * and if success, the newNBars computed here will equals the newNBars you want.
   */
  protected def computeGeometry {
    /**
     * @Note
     * 1.Should get wBar firstly, then calculator nBars
     * 2.Get this view's width to compute nBars instead of mainChartPane's
     * width, because other panes may be repainted before mainChartPane is
     * properly layouted (the width of mainChartPane is still not good)
     */
    val nBars1 = if (_controller.isFixedNBars) {
      _controller.fixedNBars
    } else {
      (wChart / _controller.wBar).toInt
    }

    /** avoid nBars == 0 */
    nBars = math.max(nBars1, 1)

    if (_controller.isFixedLeftSideTime) {
      _controller.setLeftSideRowByTime(_controller.fixedLeftSideTime, false)
    }

    /**
     * We only need computeMaxMin() once when a this should be repainted,
     * so do it here.
     */
    computeMaxMin
    if (_maxValue != _oldMaxValue || _minValue != _oldMinValue) {
      _oldMaxValue = _maxValue
      _oldMinValue = _minValue
      notifyChanged(classOf[ChartValidityObserver])
    }
  }

  protected def setMaxMinValue(max: Double, min: Double) {
    _maxValue = max
    _minValue = min
  }

  protected def postPaintComponent {
    /**
     * update controlPane's scrolling thumb position etc.
     *
     * @NOTICE
     * We choose here do update controlPane, because the paint() called in
     * Java Swing is async, we not sure when it will be really called from
     * outside, even in this's container, so here is relative safe place to
     * try, because here means the paint() is truely beging called by awt.
     */
    if (axisXPane != null) {
      axisXPane.syncWithView
    }

    if (axisYPane != null) {
      axisYPane.syncWithView
    }

    if (xControlPane != null) {
      xControlPane.syncWithView
    }

    if (yControlPane != null) {
      yControlPane.syncWithView
    }

  }

  final def nBars: Int = _nBars
  private def nBars_=(nBars: Int) {
    val oldValue = this._nBars
    this._nBars = nBars
    if (this._nBars != oldValue) {
      notifyChanged(classOf[ChartValidityObserver])
    }
  }

  // should decide width by this component's width and constant AXISY_WIDTH, since the width of children may not be decided yet.
  final def wChart = getWidth - AXISY_WIDTH

  def isSelected = glassPane.isSelected
  def isSelected_=(b: Boolean) {
    glassPane.isSelected = b
  }

  def isInteractive = _isInteractive
  def isInteractive_=(b: Boolean) {
    glassPane.interactive(b)

    this._isInteractive = b
  }

  def isPinned =  _isPinned
  def pin {
    glassPane.pin(true)

    this._isPinned = true
  }

  def unPin {
    glassPane.pin(false)

    this._isPinned = false
  }

  def yChartScale = mainChartPane.yChartScale
  def yChartScale_=(yChartScale: Double) {
    if (mainChartPane != null) {
      val datumPane = mainChartPane
      datumPane.yChartScale = yChartScale
    }

    repaint()
  }

  def valueScalar_=(valueScalar: Scalar) {
    if (mainChartPane != null) {
      val datumPane = mainChartPane
      datumPane.valueScalar = valueScalar
    }

    repaint()
  }

  def adjustYChartScale(increment: Double) {
    if (mainChartPane != null) {
      val datumPane = mainChartPane
      datumPane.growYChartScale(increment)
    }

    repaint()
  }

  def yChartScaleByCanvasValueRange_=(canvasValueRange: Double) {
    if (mainChartPane != null) {
      val datumPane = mainChartPane
      datumPane.yChartScaleByCanvasValueRange_=(canvasValueRange)
    }

    repaint()
  }

  def scrollChartsVerticallyByPixel(increment: Int) {
    val datumPane = mainChartPane
    if (datumPane != null) {
      datumPane.scrollChartsVerticallyByPixel(increment)
    }

    repaint()
  }

  /**
   * barIndex -> time
   *
   * @param barIndex, index of bars, start from 1 and to nBars
   * @return time
   */
  final def tb(barIndex: Int): Long = {
    _baseSer.timeOfRow(rb(barIndex))
  }

  final def rb(barIndex: Int): Int = {
    /** when barIndex equals it's max: nBars, row should equals rightTimeRow */
    controller.rightSideRow - _nBars + barIndex
  }

  /**
   * time -> barIndex
   *
   * @param time
   * @return index of bars, start from 1 and to nBars
   */
  final def bt(time: Long): Int = {
    br(_baseSer.rowOfTime(time))
  }

  final def br(row: Int): Int = {
    row - controller.rightSideRow + _nBars
  }

  def maxValue: Double = _maxValue
  def minValue: Double = _minValue


  def baseSer: BaseTSer = _baseSer
  final def mainSer: TSer = _mainSer
  final def controller: ChartingController = _controller

  def chartToVarsOf(ser: TSer): mutable.Map[Chart, mutable.Set[TVar[_]]] = {
    assert(ser != null, "Do not pass me a null ser!")
    if (ser eq mainSer) mainSerChartToVars else overlappingSerChartToVars.get(ser).get
  }

  def overlappingSers = overlappingSerChartToVars.keySet

  def allSers = {
    val _allSers = mutable.Set[TSer]()

    _allSers += mainSer
    _allSers ++= overlappingSers

    _allSers
  }

  def popupToDesktop {
  }

  def addOverlappingCharts(ser: TSer) {
    listenTo(ser)

    val chartToVars = overlappingSerChartToVars.get(ser) getOrElse {
      val x = mutable.Map[Chart, mutable.Set[TVar[_]]]()
      overlappingSerChartToVars.put(ser, x)
      x
    }

    var depthGradient = Pane.DEPTH_GRADIENT_BEGIN

    for (v <- ser.vars if v.plot != Plot.None) {
      val chart = if (v.plot == Plot.Signal && baseSer.isInstanceOf[QuoteSer]) {
        val qser = baseSer.asInstanceOf[QuoteSer]
        ChartFactory.createVarChart(v, qser.high, qser.low)
      } else if (v.plot == Plot.Info) {
        ChartFactory.createVarChart(v,ser.vars : _*)
      }else ChartFactory.createVarChart(v)
      
      if (chart != null) {
        val vars = chartToVars.get(chart) getOrElse {
          val x = mutable.Set[TVar[_]]()
          chartToVars.put(chart, x)
          x
        }
        vars.add(v)

        chart match {
          case _: GradientChart => chart.depth = depthGradient; depthGradient -= 1
          case _: ProfileChart =>  chart.depth = depthGradient; depthGradient -= 1
          case _: StickChart => chart.depth = -8
          case _ => chart.depth = lastDepthOfOverlappingChart; lastDepthOfOverlappingChart += 1
        }

        chart.set(mainChartPane, ser)
        mainChartPane.putChart(chart)
      }
    }

    notifyChanged(classOf[ChartValidityObserver])

    repaint()
  }

  def removeOverlappingCharts(ser: TSer) {
    deafTo(ser)

    overlappingSerChartToVars.get(ser) foreach {chartToVars =>
      for (chart <- chartToVars.keySet) {
        mainChartPane.removeChart(chart)
        chart match {
          case _: GradientChart => /** noop */
          case _: ProfileChart => /** noop */
          case _: StickChart => /** noop */
          case _ => lastDepthOfOverlappingChart -= 1
        }
      }
      /** release chartToVars */
      chartToVars.clear
      overlappingSerChartToVars.remove(ser)
    }

    notifyChanged(classOf[ChartValidityObserver])

    repaint()
  }

  def computeMaxMin {
    /** if don't need maxValue/minValue, don't let them all equal 0, just set them to 1 and 0 */
    _maxValue = 1
    _minValue = 0
  }

  protected def putChartsOfMainSer: Unit

  /** this method only process FinishedComputing event, if you want more, do it in subclass */
  protected def updateView(evt: TSerEvent) {
    evt match {
      case TSerEvent.Computed(_, _, _, _, _, _) =>
        ChartView.this match {
          case drawPane: WithDrawingPane =>
            val drawing = drawPane.selectedDrawing
            if (drawing != null && drawing.isInDrawing) {
              return
            }
          case _ =>
        }

        notifyChanged(classOf[ChartValidityObserver])

        // repaint this chart view
        repaint()
      case _ =>
    }
  }

  /**
   * @return x-control pane, may be <code>null</code>
   */
  def xControlPane: XControlPane = _xControlPane
  def xControlPane_=(pane: XControlPane) {
    _xControlPane = pane
  }

  /**
   * @return y-control pane, may be <code>null</code>
   */
  def yControlPane: YControlPane = _yControlPane
  def yControlPane_=(pane: YControlPane) {
    _yControlPane = pane
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    deafTo(_mainSer)
    super.finalize
  }
}



