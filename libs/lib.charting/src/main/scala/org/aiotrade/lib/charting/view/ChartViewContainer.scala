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

import java.awt.Component
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.image.BufferedImage
import java.awt.image.RenderedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JPanel
import org.aiotrade.lib.charting.chart.Chart
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.util.swing.GBC
import scala.collection.mutable


/**
 *
 * @author Caoyuan Deng
 */
abstract class ChartViewContainer extends JPanel {

  private val descriptorToSlaveView = mutable.Map[IndicatorDescriptor, ChartView]()
  private var _controller: ChartingController = _
  private var _masterView: ChartView = _
  /**
   * each viewContainer can only contains one selectedChart, so we define it here instead of
   * on ChartView or ChartPane;
   */
  private var _selectedChart: Chart = _
  private var _selectedView: ChartView = _
  private var _isInteractive = true
  private var _isPinned = false
  private var _parent: Component = _

  /**
   * init this viewContainer instance. binding with controller (so, MaserSer and Descriptor) here
   */
  def init(focusableParent: Component, controller: ChartingController) {
    this._parent = focusableParent
    this._controller = controller

    initComponents
  }

  override def paint(g: Graphics) {
    super.paint(g)
  }

  protected def initComponents

  def controller: ChartingController = {
    _controller
  }

  /**
   * It's just an interactive hint, the behave for interactive will be defined
   * by ChartView(s) and its Pane(s).
   *
   * @return true if the mouse will work interacticely, false else.
   */
  def isInteractive = _isInteractive
  def isInteractive_=(b: Boolean) {
    _masterView.isInteractive = b

    for (view <- descriptorToSlaveView.valuesIterator) {
      view.isInteractive = b
    }

    _isInteractive = b
  }


  def isPinned: Boolean = _isPinned
  def pin {
    masterView.pin

    _isPinned = true
  }

  def unPin {
    masterView.unPin

    _isPinned = false
  }


  def adjustViewsHeight(increment: Int) {
    /**
     * @TODO
     * Need implement adjusting each views' height ?
     */

    var numSlaveViews = 0
    var sumSlaveViewsHeight = 0f
    for (view <- descriptorToSlaveView.valuesIterator if view != masterView) {
      /** overlapping view is also in masterView, should ignor it */
      sumSlaveViewsHeight += view.getHeight
      numSlaveViews += 1
    }

    if (numSlaveViews == 1 && sumSlaveViewsHeight == 0) {
      /** first slaveView added */
      sumSlaveViewsHeight = 0.382f * _masterView.getHeight
    }

    setVisible(false)

    val gbl = getLayout.asInstanceOf[GridBagLayout]
    val adjustHeight = increment
    val gbc = GBC(0).setFill(GridBagConstraints.BOTH).setWeight(100, _masterView.getHeight + adjustHeight)

    /**
     * We need setConstraints and setSize together to take the effect
     * according to GridBagLayout's behave.
     * We can setSize(new Dimension(0, 0)) and let GridBagLayout arrange
     * the size according to weightx and weighty, but for performence issue,
     * we'd better setSize() to the actual size that we want.
     */
    gbl.setConstraints(_masterView, gbc)
    _masterView.setSize(new Dimension(_masterView.getWidth, gbc.weighty.toInt))
    for (view <- descriptorToSlaveView.valuesIterator if view ne masterView) {
      /** average assigning */
      gbc.weighty = (sumSlaveViewsHeight - adjustHeight) / numSlaveViews
      /*-
       * proportional assigning
       * gbc.weighty = v.getHeight() - adjustHeight * v.getHeight() / iHeight;
       */
      gbl.setConstraints(view, gbc)
      view.setSize(new Dimension(view.getWidth, gbc.weighty.toInt))
    }

    setVisible(true)
  }

  def masterView: ChartView = {
    _masterView
  }

  protected def setMasterView(masterView: ChartView, gbc: GridBagConstraints) {
    _masterView = masterView
    add(masterView, gbc)
  }

  def addSlaveView(descriptor: IndicatorDescriptor, ser: TSer, _gbc: GridBagConstraints): ChartView = {
    if (!descriptorToSlaveView.contains(descriptor)) {
      val view = if (ser.isOverlapping) {
        val view = masterView
        view.addOverlappingCharts(ser)
        view
      } else {
        val view = new IndicatorChartView(controller, ser)
        val gbc = if (_gbc == null) {
          GBC(0).setFill(GridBagConstraints.BOTH)
        } else _gbc
        add(view, gbc)
        view
      }
      descriptorToSlaveView.put(descriptor, view)
      selectedView = view
      view 
    } else null
  }

  def removeSlaveView(descriptor: IndicatorDescriptor) {
    lookupChartView(descriptor) match {
      case Some(view) if view eq masterView =>
        view.removeOverlappingCharts(descriptor.createdServerInstance)
      case Some(view) =>
        remove(view)
        adjustViewsHeight(0)
        view.allSers.clear
        repaint()
      case None =>
    }
    descriptorToSlaveView.remove(descriptor)
  }

  def slaveViews = descriptorToSlaveView.valuesIterator

  def selectedView: ChartView = _selectedView
  def selectedView_=(view: ChartView) {
    if (_selectedView != null) {
      _selectedView.isSelected = false
    }

    if (view != null) {
      _selectedView = view
      _selectedView.isSelected = true
    } else {
      _selectedView = null
    }
  }

  def selectedChart: Chart = _selectedChart

  /**
   * @param chart the chart to be set as selected, could be <b>null</b>
   */
  def selectedChart_=(chart: Chart) {
    if (_selectedChart != null) {
      _selectedChart.isSelected = false
    }

    if (chart != null) {
      _selectedChart = chart
      _selectedChart.isSelected = true
    } else {
      _selectedChart = null
    }

    repaint()
  }

  def lookupIndicatorDescriptor(view: ChartView): Option[IndicatorDescriptor] = {
    descriptorToSlaveView find {case (descriptor, aView) => (aView ne null) && (aView eq view)} map (_._1)
  }

  def lookupChartView(descriptor: IndicatorDescriptor): Option[ChartView] = {
    descriptorToSlaveView.get(descriptor)
  }

  def getDescriptorsWithSlaveView: mutable.Map[IndicatorDescriptor, ChartView] = {
    descriptorToSlaveView
  }

  def getFocusableParent: Component = {
    _parent
  }

  @throws(classOf[Exception])
  def saveToCustomSizeImage(file: File, fileFormat: String, width: Int, height: Int) {
    /** backup: */
    val backupRect = getBounds()

    setBounds(0, 0, width, height)
    validate

    saveToImage(file, fileFormat)

    /** restore: */
    setBounds(backupRect)
    validate
  }

  @throws(classOf[Exception])
  def saveToCustomSizeImage(file: File, fileFormat: String, fromTime: Long, toTime: Long, height: Int) {
    val begPos = _controller.baseSer.rowOfTime(fromTime)
    val endPos = _controller.baseSer.rowOfTime(toTime)
    val nBars = endPos - begPos
    val width = (nBars * _controller.wBar).toInt

    /** backup: */
    val backupRightCursorPos = _controller.rightSideRow
    val backupReferCursorPos = _controller.referCursorRow

    _controller.setCursorByRow(backupReferCursorPos, endPos, true)

    saveToCustomSizeImage(file, fileFormat, width, height)

    /** restore: */
    _controller.setCursorByRow(backupReferCursorPos, backupRightCursorPos, true)
  }

  @throws(classOf[Exception])
  def saveToImage(file: File, fileFormat: String) {
    val fileName = (file.toString + ".png")

    if (_masterView.xControlPane != null) {
      _masterView.xControlPane.setVisible(false)
    }

    if (_masterView.yControlPane != null) {
      _masterView.yControlPane.setVisible(false)
    }

    val image = paintToImage

    ImageIO.write(image, fileFormat, file)

    if (_masterView.xControlPane != null) {
      _masterView.xControlPane.setVisible(true)
    }

    if (_masterView.yControlPane != null) {
      _masterView.yControlPane.setVisible(true)
    }
  }

  @throws(classOf[Exception])
  def paintToImage: RenderedImage = {
    val renderImage = new BufferedImage(getWidth, getHeight, BufferedImage.TYPE_INT_RGB)

    val gImg = renderImage.createGraphics
    try {
      paint(gImg)
    } catch {case ex: Exception => throw ex
    } finally {gImg.dispose}

    renderImage
  }

  @throws(classOf[Throwable])
  override 
  protected def finalize {
    descriptorToSlaveView.clear
    super.finalize
  }
}
