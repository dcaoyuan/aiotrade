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
package org.aiotrade.lib.charting.laf

import java.awt.Color
import java.awt.Font
import org.aiotrade.lib.charting.chart.QuoteChart

/**
 *
 * @author Caoyuan Deng
 */
object LookFeel {
  private var current: LookFeel = _

  def apply(): LookFeel = {
    if (current == null) {
      current = new CityLights
    }
    current
  }

  def update(colorTheme: LookFeel) {
    current = colorTheme
  }

  private var positiveNegativeColorReversed: Boolean = _
  private var quoteChartType: QuoteChart.Type = QuoteChart.Type.Ohlc
  private var thinVolumeBar: Boolean = _
  private var antiAlias: Boolean = _
  private var autoHideScroll: Boolean = _
  /** won't persistent */
  private var allowMultipleIndicatorOnQuoteChartView: Boolean = _

  def compareColor(c1: Color, c2: Color): Double = {
    val R1 = c1.getRed
    val G1 = c1.getGreen
    val B1 = c1.getBlue

    val R2 = c2.getRed
    val G2 = c2.getGreen
    val B2 = c2.getBlue

    math.sqrt(((R1 - R2) * (R1 - R2) + (G1 - G2) * (G1 - G2) + (B1 - B1) * (B1 - B2)))
  }
}

abstract class LookFeel {
  import LookFeel._

  protected val monthColors: Array[Color]
  protected val planetColors: Array[Color]
  protected val chartColors: Array[Color]
  protected val positiveColor: Color
  protected val negativeColor: Color
  protected val positiveBgColor: Color
  protected val negativeBgColor: Color
  protected val neutralColor = Color.YELLOW
  protected val neutralBgColor: Color
  /** scrolling controller colors */
  protected val trackColor: Color = backgroundColor
  protected val thumbColor: Color = backgroundColor

  var isFillBar = false
  val axisFont: Font
  val defaultFont = new Font("Dialog Input", Font.PLAIN, 10)
  val systemBackgroundColor: Color
  val nameColor: Color
  val backgroundColor: Color
  val infoBackgroundColor: Color
  val heavyBackgroundColor: Color
  val axisBackgroundColor: Color
  val stickChartColor: Color
  val borderColor: Color
  val axisColor = borderColor: Color
  /** same as new Color(0.0f, 0.0f, 1.0f, 0.382f) */
  val referCursorColor: Color //new Color(0.5f, 0.0f, 0.5f, 0.618f); //new Color(0.0f, 0.0f, 1.0f, 0.618f);
  //new Color(131, 129, 221);
  /** same as new Color(1.0f, 1.0f, 1.0f, 0.618f) */
  val mouseCursorColor: Color
  //new Color(239, 237, 234);
  val mouseCursorTextColor: Color
  val mouseCursorTextBgColor: Color
  val referCursorTextColor: Color
  val referCursorTextBgColor: Color
  val drawingMasterColor: Color // new Color(128, 128, 255); //new Color(128, 0, 128);
  val drawingColor: Color // new Color(128, 128, 255); //new Color(128, 0, 128);
  val drawingColorTransparent: Color //new Color(128, 0, 128);
  val handleColor: Color // new Color(128, 128, 255); //new Color(128, 0, 128);
  val astrologyColor: Color

  def isPositiveNegativeColorReversed = {
    positiveNegativeColorReversed
  }

  def setPositiveNegativeColorReversed(b: Boolean) {
    positiveNegativeColorReversed = b
  }

  def isAllowMultipleIndicatorOnQuoteChartView = {
    allowMultipleIndicatorOnQuoteChartView
  }

  def setAllowMultipleIndicatorOnQuoteChartView(b: Boolean) {
    allowMultipleIndicatorOnQuoteChartView = b
  }

  def isAntiAlias = {
    antiAlias
  }

  def setAntiAlias(b: Boolean) {
    antiAlias = b
  }

  def isAutoHideScroll = {
    autoHideScroll
  }

  def setAutoHideScroll(b: Boolean) {
    autoHideScroll = b
  }

  def isThinVolumeBar = {
    thinVolumeBar
  }

  def setThinVolumeBar(b: Boolean) {
    thinVolumeBar = b
  }

  def setQuoteChartType(tpe: QuoteChart.Type) {
    quoteChartType = tpe
  }

  def getQuoteChartType = {
    quoteChartType
  }

  def getGradientColor(depth: Int, beginDepth: Int): Color = {
    val steps = math.abs((depth - beginDepth))
    val alpha = math.pow(0.618d, steps).toFloat

    //        Color color = Color.RED;
    //        int r = alpha * color.getRed();
    //        int g = alpha * color.getGreen();
    //        int b = alpha * color.getBlue();

    //        return new Color(r * alpha, g * alpha, b * alpha);
    new Color(0.0f * alpha, 1.0f * alpha, 1.0f * alpha);
  }

  //    public Color getGradientColor(int depth) {
  //        double steps = math.abs((depth - AbstractPart.DEPTH_GRADIENT_BEGIN));
  //        float  alpha = (float)math.pow(0.618d, steps);
  //
  //        Color color = Color.RED;
  //        for (int i = 0; i < steps; i++) {
  //            color.brighter().brighter();
  //        }
  //
  //        return color;
  //    }
  def getChartColor(depth: Int): Color = {
    val multiple = depth / chartColors.length
    val remainder = depth % chartColors.length
    var color = chartColors(remainder)
    var i = 1
    while (i <= multiple) {
      color = color.darker
      i += 1
    }
    color
  }

  def getNeutralColor = {
    neutralColor
  }

  def getNeutralBgColor = {
    neutralBgColor
  }

  def getPositiveColor: Color = {
    if (isPositiveNegativeColorReversed) negativeColor else positiveColor
  }

  def getNegativeColor: Color = {
    if (isPositiveNegativeColorReversed) positiveColor else negativeColor
  }

  def getPositiveBgColor: Color = {
    if (isPositiveNegativeColorReversed) negativeBgColor else positiveBgColor
  }

  def getNegativeBgColor: Color = {
    if (isPositiveNegativeColorReversed) positiveBgColor else negativeBgColor
  }

  def getMonthColor(month: Int): Color = {
    monthColors(month)
  }

  def getPlanetColor(planet: Int): Color = {
    planetColors(planet)
  }

  def getTrackColor: Color = {
    if (trackColor == null) {
      axisColor
    } else {
      trackColor
    }
  }

  def getThumbColor: Color = {
    if (thumbColor == null) {
      axisColor
    } else {
      thumbColor
    }
  }

}
