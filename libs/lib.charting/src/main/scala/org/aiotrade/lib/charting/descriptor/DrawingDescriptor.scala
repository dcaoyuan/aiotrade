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
package org.aiotrade.lib.charting.descriptor

import javax.swing.Action
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor
import org.aiotrade.lib.charting.chart.handledchart.HandledChart
import org.aiotrade.lib.charting.chart.segment.ValuePoint
import org.aiotrade.lib.charting.view.ChartView
import org.aiotrade.lib.charting.view.pane.DrawingPane
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable


/**
 *
 * @author Caoyuan Deng
 */
class DrawingDescriptor(layerName: String) extends Descriptor[DrawingPane] {

  private var handledChartMapPoints = mutable.Map[HandledChart, ArrayBuffer[ValuePoint]]()
  var displayName = "Layout One"
    
  serviceClassName = layerName
  setDisplayName(layerName)

  def this() = this("Layout One")

  override def set(layerName: String, freq: TFreq) {
    this.displayName = layerName
    this.freq = freq
  }
    
  override def serviceClassName_=(layerName: String) {
    setDisplayName(layerName)
  }
    
  override def serviceClassName: String = {
    getDisplayName
  }
    
    
  def putHandledChart(handledChart: HandledChart, handlePoints: ArrayBuffer[ValuePoint]) {
    handledChartMapPoints.put(handledChart, handlePoints)
  }
    
  def removeHandledChart(handledChart: HandledChart) {
    handledChartMapPoints.remove(handledChart)
  }
    
  def getHandledChartMapPoints: mutable.Map[HandledChart, ArrayBuffer[ValuePoint]] = {
    handledChartMapPoints
  }
    
  def setHandledChartMapPoints(handledChartMapPoints: mutable.Map[HandledChart, ArrayBuffer[ValuePoint]]) {
    this.handledChartMapPoints = handledChartMapPoints
  }
    
  def setDisplayName(displayName: String) {
    this.displayName = displayName
  }
    
  def getDisplayName: String = {
    displayName
  }
    
  /**
   * @param a Chartview on which the drawing pane is going to stand.
   */
  protected def createServiceInstance(args: Any*): Option[DrawingPane] = {
    args match {
      case Seq(view: ChartView, _*) =>
        Some(new DrawingPane(view, view.mainChartPane, this))
      case _ => None
    }
  }
    
  override def createDefaultActions: Array[Action] = {
    DrawingDescriptorActionFactory().createActions(this)
  }
    
}


