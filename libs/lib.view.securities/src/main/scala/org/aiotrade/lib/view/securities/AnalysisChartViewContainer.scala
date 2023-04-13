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
package org.aiotrade.lib.view.securities

import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.math.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.indicator.ComputeFrom
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.util.swing.GBC
import org.aiotrade.lib.collection.ArrayList

/**
 *
 * @author Caoyuan Deng
 */
class AnalysisChartViewContainer extends ChartViewContainer {
    
  override def init(focusableParent: Component, controller: ChartingController) {
    super.init(focusableParent, controller)
  }
    
  protected def initComponents {
    setLayout(new GridBagLayout)
    val gbc = GBC(0).setFill(GridBagConstraints.BOTH).setWeight(100, 618)
    
    val quoteSer = controller.baseSer.asInstanceOf[QuoteSer]
    quoteSer.shortName = controller.serProvider.uniSymbol
    val quoteChartView = new AnalysisChartView(controller, quoteSer)
    setMasterView(quoteChartView, gbc)
        
    /** use two list to record the active indicators and their order(index) for later showing */
    val indicatorDescriptorsToBeShowing = new ArrayList[IndicatorDescriptor]
    val  indicatorsToBeShowing = new ArrayList[Indicator]
    val content = controller.serProvider.content
    for (descriptor <- content.lookupDescriptors(classOf[IndicatorDescriptor])
         if descriptor.active && descriptor.freq == controller.baseSer.freq
    ) {
      descriptor.serviceInstance(quoteSer) foreach {indicator =>
        /**
         * @NOTICE
         * As the quoteSer may has been loaded, there may be no more UpdatedEvent
         * etc fired, so, computeFrom(0) first.
         */
        indicator ! ComputeFrom(0) // don't remove me
                    
        if (indicator.isOverlapping) {
          addSlaveView(descriptor, indicator, null)
        } else {
          /** To get the extract size of slaveViews to be showing, store them first, then add them later */
          indicatorDescriptorsToBeShowing += descriptor
          indicatorsToBeShowing += indicator.asInstanceOf[Indicator]
        }
      }
    }
        
    /** now add slaveViews, the size has excluded those indicators not showing */
    val size = indicatorDescriptorsToBeShowing.length
    var i = 0
    while (i < size) {
      gbc.weighty = 382.0 / size
      addSlaveView(indicatorDescriptorsToBeShowing(i), indicatorsToBeShowing(i), gbc)

      i += 1
    }

    for (descriptor <- content.lookupDescriptors(classOf[DrawingDescriptor])
         if descriptor.freq == controller.baseSer.freq
    ) {
      descriptor.serviceInstance(masterView) foreach {drawing =>
        masterView.asInstanceOf[WithDrawingPane].addDrawing(descriptor, drawing)
      }
    }
  }
}
