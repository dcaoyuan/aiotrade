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

import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.WithVolumePane
import org.aiotrade.lib.charting.view.scalar.LinearScalar
import org.aiotrade.lib.charting.view.scalar.Scalar
import org.aiotrade.lib.charting.util.GeomUtil
import org.aiotrade.lib.math.timeseries.BaseTSer

/**
 *
 * @author Caoyuan Deng
 * 
 * @todo the ChangeObservable should notify according to priority
 *
 * call super(view, null) will let the super know this pane will be its
 * own datumPlane.
 * @see Pane#Pane(ChartView, DatumPlane)
 */
 abstract class AbstractDatumPlane(aview: ChartView) extends Pane(aview, null) with DatumPlane {
    
    private var _isGeometryValid: Boolean = _
    
    /** geometry that need to be set before chart plotting and render */
    private var _nBars: Int = _ // fetched from view, number of bars, you may consider it as chart width
    private var _hChart: Int = _ // chart height in pixels, corresponds to the value range (maxValue - minValue)
    private var _hCanvas: Int = _ // canvas height in pixels
    private var _hChartOffsetToCanvas: Int = _ // chart's axis-y offset in canvas, named hXXXX means positive is from lower to upper;
    private var _hSpaceLower: Int = _ // height of spare space at lower side
    private var _hSpaceUpper: Int = _ // height of spare space at upper side
    private var _yCanvasLower: Int = _ // y of canvas' lower side
    private var _yChartLower: Int = _ // y of chart's lower side
    private var _wBar: Double = _ // fetched from viewContainer, pixels per bar
    private var _hOne: Double = _ // pixels per 1.0 value
    private var _maxValue: Double = _ // fetched from view
    private var _minValue: Double = _ // fetched from view
    private var _maxScaledValue: Double = _
    private var _minScaledValue: Double = _
    
    private var _valueScalar: Scalar = new LinearScalar
    
    /**
     * the percent of hCanvas to be used to render charty, is can be used to scale the chart
     */
    private var _yChartScale = 1.0
    
    /** the pixels used to record the chart vertically moving */
    private var _hChartScrolled: Int = _
        
    def computeGeometry {
      this._wBar  = view.controller.wBar
      this._nBars = view.nBars
        
      /**
       * @TIPS:
       * if want to leave spare space at lower side, do hCanvas -= space
       * if want to leave spare space at upper side, do hChart = hCanvas - space
       *     hOne = hChart / (maxValue - minValue)
       */
      _hSpaceLower = 1
      if (view.xControlPane != null) {
        /** leave xControlPane's space at lower side */
        _hSpaceLower += view.xControlPane.getHeight
      }
        
      /** default values: */
      _hSpaceUpper = 0
      _maxValue = view.maxValue
      _minValue = view.minValue
        
      /** adjust if necessary */
      if (this eq view.mainChartPane) {
        _hSpaceUpper += view.TITLE_HEIGHT_PER_LINE
      } else if (view.isInstanceOf[WithVolumePane] && this.eq(view.asInstanceOf[WithVolumePane].volumeChartPane)) {
        _maxValue = view.asInstanceOf[WithVolumePane].maxVolume
        _minValue = view.asInstanceOf[WithVolumePane].minVolume
      }
        
      this._maxScaledValue = _valueScalar.doScale(_maxValue)
      this._minScaledValue = _valueScalar.doScale(_minValue)
        
      this._hCanvas = getHeight - _hSpaceLower - _hSpaceUpper
        
      val hChartCouldBe = _hCanvas
      this._hChart = (hChartCouldBe * _yChartScale).toInt
        
      /** allocate sparePixelsBroughtByYChartScale to upper and lower averagyly */
      val sparePixelsBroughtByYChartScale = hChartCouldBe - _hChart
      _hChartOffsetToCanvas = _hChartScrolled + (sparePixelsBroughtByYChartScale * 0.5).toInt
        
        
      _yCanvasLower = _hSpaceUpper + _hCanvas
      _yChartLower = _yCanvasLower - _hChartOffsetToCanvas
        
      /**
       * @NOTICE
       * the chart height corresponds to value range.
       * (not canvas height, which may contain values exceed max/min)
       */
      _hOne = _hChart.toDouble / (_maxScaledValue - _minScaledValue)
        
      /** avoid hOne == 0 */
      this._hOne = math.max(_hOne, 0.0000000001)
        
      isGeometryValid = true
    }
    
    def isGeometryValid: Boolean = _isGeometryValid
    protected def isGeometryValid_=(b: Boolean) {
      _isGeometryValid = b
    }
    
    def valueScalar: Scalar = _valueScalar
    def valueScalar_=(valueScalar: Scalar) {
      this._valueScalar = valueScalar
    }
    
    def yChartScale: Double = _yChartScale
    def yChartScale_=(yChartScale: Double) {
      val oldValue = this._yChartScale
      this._yChartScale = yChartScale
        
      if (oldValue != this._yChartScale) {
        isGeometryValid = false
        repaint()
      }
    }
    
    def growYChartScale(increment: Double) {
      yChartScale = yChartScale + increment
    }
    
    def yChartScaleByCanvasValueRange_=(canvasValueRange: Double) {
      val oldCanvasValueRange = vy(yCanvasUpper) - vy(yCanvasLower)
      val scale = oldCanvasValueRange / canvasValueRange
      val newYChartScale = _yChartScale * scale
        
      yChartScale = newYChartScale
    }
    
    def scrollChartsVerticallyByPixel(increment: Int) {
      _hChartScrolled += increment
        
      /** let repaint() to update the hChartOffsetToCanvas and other geom */
      repaint()
    }
    
    def baseSer: BaseTSer = view.controller.baseSer
    
    /**
     * barIndex -> x
     *
     * @param i index of bars, start from 1 to nBars
     * @return x
     */
    final def xb(barIndex: Int): Double = {
      _wBar * (barIndex - 1)
    }
    
    final def xr(row: Int): Double = {
      xb(br(row))
    }
    
    /**
     * y <- value
     *
     * @param value
     * @return y on the pane
     */
    final def yv(value: Double): Double = {
      val scaledValue = _valueScalar.doScale(value)
      GeomUtil.yv(scaledValue, _hOne, _minScaledValue, _yChartLower)
    }
    
    /**
     * value <- y
     * @param y y on the pane
     * @return value
     */
    final def vy(y: Double): Double = {
      val scaledValue = GeomUtil.vy(y, _hOne, _minScaledValue, _yChartLower)
      _valueScalar.unScale(scaledValue)
    }
    
    /**
     * barIndex <- x
     *
     * @param x x on the pane
     * @return index of bars, start from 1 to nBars
     */
    final def bx(x: Double): Int = {
      math.round(x / _wBar + 1).toInt
    }
    
    
    /**
     * time <- x
     */
    final def tx(x: Double): Long = {
      tb(bx(x))
    }

    /** row <- x */
    final def rx(x: Double): Int = {
      rb(bx(x))
    }
    
    final def rb(barIndex: Int): Int = {
      /** when barIndex equals it's max: nBars, row should equals rightTimeRow */
      view.controller.rightSideRow - _nBars + barIndex
    }
    
    final def br(row: Int): Int = {
      row - view.controller.rightSideRow + _nBars
    }
    
    /**
     * barIndex -> time
     *
     * @param barIndex, index of bars, start from 1 and to nBars
     * @return time
     */
    final def tb(barIndex: Int): Long = {
      view.controller.baseSer.timeOfRow(rb(barIndex))
    }
    
    /**
     * time -> barIndex
     *
     * @param time
     * @return index of bars, start from 1 and to nBars
     */
    final def bt(time: Long): Int = {
      br(view.controller.baseSer.rowOfTime(time))
    }
    
    def nBars: Int = _nBars
    
    def wBar: Double = _wBar
    
    /**
     * @return height of 1.0 value in pixels
     */
    def hOne: Double = _hOne
    
    def hCanvas: Int = _hCanvas
    
    def yCanvasLower: Int = _yCanvasLower
    
    def yCanvasUpper: Int = _hSpaceUpper
    
    /**
     * @return chart height in pixels, corresponds to the value range (maxValue - minValue)
     */
    def hChart: Int = _hChart
    
    def yChartLower: Int = _yChartLower
    
    def yChartUpper: Int = yChartLower - _hChart
    
    def maxValue: Double = _maxValue
    
    def minValue: Double = _minValue

    @throws(classOf[Throwable])
    override protected def finalize {
      view.controller.removeObserversOf(this)
      view.removeObserversOf(this)

      super.finalize
    }
    
  }
