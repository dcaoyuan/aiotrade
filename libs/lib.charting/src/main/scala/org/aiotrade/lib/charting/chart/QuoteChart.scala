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

import java.awt.Color
import org.aiotrade.lib.charting.widget.CandleBar
import org.aiotrade.lib.charting.widget.LineSegment
import org.aiotrade.lib.charting.widget.PathsWidget
import org.aiotrade.lib.charting.widget.OhlcBar
import org.aiotrade.lib.charting.widget.WidgetModel
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.charting.view.pane.Pane
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar


/**
 *
 * @author Caoyuan Deng
 */
object QuoteChart {
  /**
   * Type will be got from the static property quoteChartType of view and we
   * should consider the case of view's repaint() being called, so we do not
   * include it in Model.
   */
  abstract class Type
  object Type {
    case object Candle extends Type
    case object Ohlc   extends Type
    case object Line   extends Type

    def values = Array(Candle, Ohlc, Line)
  }
}

class QuoteChart extends AbstractChart {
  import QuoteChart._

  class Model extends WidgetModel {
    var openVar:  TVar[Double] = _
    var highVar:  TVar[Double] = _
    var lowVar:   TVar[Double] = _
    var closeVar: TVar[Double] = _
        
    def set(openVar: TVar[Double], highVar: TVar[Double], lowVar: TVar[Double], closeVar: TVar[Double]) {
      this.openVar  = openVar
      this.highVar  = highVar
      this.lowVar   = lowVar
      this.closeVar = closeVar
    }
  }

  type M = Model

  private var posColor: Color = _
  private var negColor: Color = _
    

  protected def createModel: Model = new Model
    
  protected def plotChart {

    if (depth == Pane.DEPTH_DEFAULT) {
      posColor = LookFeel().getPositiveColor
      negColor = LookFeel().getNegativeColor
    } else {
      /** for comparing quotes charts */
      posColor = LookFeel().getChartColor(depth)
      negColor = posColor
    }
        
    val color = posColor
    setForeground(color)
        
    val tpe = datumPlane.view.asInstanceOf[WithQuoteChart].quoteChartType
    tpe match {
      case Type.Candle | Type.Ohlc =>
        plotCandleOrOhlcChart(tpe)
      case Type.Line =>
        plotLineChart

      case _ =>
    }
        
  }
    
  private def plotCandleOrOhlcChart(tpe: Type) {
    val m = model
        
    /**
     * @NOTICE
     * re-create and re-add children each time, so the children will release
     * its resource when reset();
     */
    val pathsWidget = addChild(new PathsWidget)
    val tp = if (tpe == Type.Candle) new CandleBar else new OhlcBar
    var bar = 1
    while (bar <= nBars) {
            
      /**
       * @TIPS:
       * use Null.Double to test if value has been set at least one time
       */
      var open  = Null.Double
      var close = Null.Double
      var high  = Double.MinValue
      var low   = Double.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time) && m.closeVar(time) != 0) {
          if (Null.is(open)) {
            /** only get the first open as compressing period's open */
            open = m.openVar(time)
          }
          high  = math.max(high, m.highVar(time))
          low   = math.min(low,  m.lowVar (time))
          close = m.closeVar(time)
        }

        i += 1
      }
            
      if (Null.not(close) && close != 0) {
        val color = if (close >= open) posColor else negColor
                
        val yOpen  = yv(open)
        val yHigh  = yv(high)
        val yLow   = yv(low)
        val yClose = yv(close)
                
        tpe match {
          case Type.Candle =>
            val isFilled = LookFeel().isFillBar
            tp.asInstanceOf[CandleBar].model.set(xb(bar), yOpen, yHigh, yLow, yClose, wBar, isFilled || close < open)
          case Type.Ohlc =>
            tp.asInstanceOf[OhlcBar].model.set(xb(bar), yOpen, yHigh, yLow, yClose, wBar)
          case _ =>
        }
        tp.setForeground(color)
        tp.plot
        pathsWidget.appendFrom(tp)
      }

      bar += nBarsCompressed
    }
        
  }
    
  private def plotLineChart {
    val m = model
        
    val pathsWidget = addChild(new PathsWidget)
    val tp = new LineSegment
    var y1 = Null.Double   // for prev
    var y2 = Null.Double   // for curr
    var bar = 1
    while (bar <= nBars) {
            
      /**
       * @TIPS:
       * use Null.Double to test if value has been set at least one time
       */
      var open  = Null.Double
      var close = Null.Double
      var max   = -Double.MaxValue
      var min   = +Double.MaxValue
      var i = 0
      while (i < nBarsCompressed) {
        val time = tb(bar + i)
        if (ser.exists(time) && m.closeVar(time) != 0) {
          if (Null.is(open)) {
            /** only get the first open as compressing period's open */
            open = m.openVar(time)
          }
          close = m.closeVar(time)
          max = math.max(max, close)
          min = math.min(min, close)
        }

        i += 1
      }
            
      if (Null.not(close) && close != 0) {
        val color = if (close >= open) posColor else negColor
                
        y2 = yv(close)
        if (nBarsCompressed > 1) {
          /** draw a vertical line to cover the min to max */
          val x = xb(bar)
          tp.model.set(x, yv(min), x, yv(max))
        } else {
          if (Null.not(y1)) {
            /**
             * x1 shoud be decided here, it may not equal prev x2:
             * think about the case of on calendar day mode
             */
            val x1 = xb(bar - nBarsCompressed)
            val x2 = xb(bar)
            tp.model.set(x1, y1, x2, y2)
          }
        }
        y1 = y2
                
        tp.setForeground(color)
        tp.plot
        pathsWidget.appendFrom(tp)
      }

      bar += 1
    }
        
  }
    
}
