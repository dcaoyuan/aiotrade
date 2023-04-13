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
package org.aiotrade.lib.charting.chart

import java.awt.BasicStroke
import java.awt.Color
import java.awt.Stroke
import org.aiotrade.lib.charting.view.pane.DatumPlane
import org.aiotrade.lib.charting.widget.Widget
import org.aiotrade.lib.math.timeseries.TSer


/**
 *
 * @author Caoyuan Deng
 */
trait Chart extends Widget with Ordered[Chart] {
  import Chart._

  /**
   * @NOTICE:
   * It's always better to set datumPlane here. After call following set(,,,)
   * methods, the chart can be properly put in any datumPlane with the same datum,
   * by calling DatumPlane.putChart() for automatically rendering, or, can be
   * drawn on pane by calling render() initiatively (such as mouse cursor chart).
   * So, do not try to separate a setDatumPane(AbstractDatumPlane) method.
   */
  def set(datumPlane: DatumPlane, ser: TSer, depth: Int)
  def set(datumPlane: DatumPlane, ser: TSer)
    
  def isFirstPlotting: Boolean
  def isFirstPlotting_=(b: Boolean)
    
  def depth: Int
  def depth_=(depth: Int): Unit
    
  def ser: TSer
  def ser_=(ser: TSer): Unit
    
  def setStrock(strockWidth: Int, strockType: StrockType)
  def strockWidth: Double
  def strockType: StrockType
    
  def isSelected: Boolean
  def isSelected_=(b: Boolean)
    
  def reset
    
}

object Chart {
  trait StrockType
  object StrockType {
    case object Base extends StrockType
    case object Dash extends StrockType
  }

  val MARK_INTERVAL = 16

  val COLOR_SELECTED = new Color(0x447BCD)
  val COLOR_HIGHLIGHTED = COLOR_SELECTED.darker
  val COLOR_HOVERED = COLOR_SELECTED.brighter

  val BASE_STROKES = Array[Stroke](
    new BasicStroke(1.0f),
    new BasicStroke(2.0f)
  )
  val DASH_PATTERN = Array[Float](5, 2)
  val DASH_STROKES = Array[Stroke](
    new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0),
    new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, DASH_PATTERN, 0)
  )

  /**
   * To allow the mouse pick up accurately a chart, we need seperate a chart to
   * a lot of segment, each segment is a shape that could be sensible for the
   * mouse row. The minimum segment's width is defined here.
   *
   * Although we can define it > 1, such as 3 or 5, but, when 2 bars or more
   * are located in the same one segment, they can have only one color,
   * example: two-colors candle chart. So, we just simplely define it as 1.
   *
   * Another solution is define 1 n-colors chart as n 1-color charts (implemented).
   */
  val MIN_SEGMENT_WIDTH = 1
}
