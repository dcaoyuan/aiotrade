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
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartView

/**
 *
 * @author Caoyuan Deng
 */
class XControlPane(aview: ChartView, adatumPlane: DatumPlane) extends Pane(aview, adatumPlane) {
    
  setOpaque(false)
  setRenderStrategy(RenderStrategy.NoneBuffer)
        
  val scrollControl = new MyScrollControl
  scrollControl.setExtendable(true)
  scrollControl.setScalable(true)

  setLayout(new BorderLayout)
  add(scrollControl, BorderLayout.CENTER)
    
  def setAlwaysHidden(b: Boolean) {
    scrollControl.setAutoHidden(b)
  }
    
  def syncWithView {
    val baseSer = view.controller.baseSer
        
    val vModelRange = baseSer.size
    val vShownRange = view.nBars
    val vModelEnd = baseSer.lastOccurredRow
    val vShownEnd = view.controller.rightSideRow
    val unit = 1.0
    val nUnitsBlock = (vShownRange * 0.168).toInt
        
    scrollControl.setValues(vModelRange, vShownRange, vModelEnd, vShownEnd, unit, nUnitsBlock)
        
    val autoHidden = LookFeel().isAutoHideScroll
    scrollControl.setAutoHidden(autoHidden)
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
  class MyScrollControl extends AbstractScrollControl {
        
    protected def viewScrolledByUnit(nUnitsWithDirection: Double) {
      view.controller.scrollChartsHorizontallyByBar(nUnitsWithDirection.toInt)
    }
        
    protected def viewScaledToRange(valueShownRange: Double) {
      view.controller.setWBarByNBars(getWidth, valueShownRange.toInt)
    }
  }
    
}


