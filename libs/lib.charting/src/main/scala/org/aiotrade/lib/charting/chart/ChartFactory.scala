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

import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.signal.Signal


/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 1, 2006, 3:04 AM
 * @since   1.0.4
 */
object ChartFactory {
    
  def createVarChart(v: TVar[_], additionalVars: TVar[_]*): Chart = {
    v.plot match  {
      case Plot.Volume =>
        val chart = new VolumeChart
        chart.model.set(false)
        chart
      case Plot.Line =>
        val chart = new PolyLineChart
        chart.model.set(v.asInstanceOf[TVar[Double]])
        chart
      case Plot.Stick =>
        val chart = new StickChart
        chart.model.set(v.asInstanceOf[TVar[Double]])
        chart
      case Plot.Dot =>
        val chart = new DotChart
        chart.model.set(v)
        chart
      case Plot.Shade =>
        val chart = new GradientChart
        chart.model.set(v, null)
        chart
      case Plot.Profile =>
        val chart = new ProfileChart
        chart.model.set(v)
        chart
      case Plot.Zigzag =>
        val chart = new ZigzagChart
        chart.model.set(v)
        chart
      case Plot.Signal =>
        val chart = new SignalChart
        additionalVars.toList match {
          case List(var1: TVar[Double], var2: TVar[Double]) =>
            chart.model.set(v.asInstanceOf[TVar[List[Signal]]], var1, var2)
          case _ => chart.model.set(v.asInstanceOf[TVar[List[Signal]]], null, null)
        }
        chart
      case Plot.Info =>
        val chart = new InfoPointChart
        chart.model.set(v,additionalVars(1))
        chart
      case _ =>
        null
    }
  }
    
    
}
