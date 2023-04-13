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
package org.aiotrade.modules.ui.actions;

import java.awt.Component
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.ImageIcon
import javax.swing.JToggleButton
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.WithQuoteChart
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.openide.util.HelpCtx
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction

/**
 *
 * @author Caoyuan Deng
 */
object SwitchAdjustQuoteAction {
  private var toggleButton: JToggleButton = _

  def updateToolbar(selectedViewContainer: ChartViewContainer) {
    if (selectedViewContainer.controller.baseSer.isInstanceOf[QuoteSer]) {
      val selected = selectedViewContainer.controller.baseSer.asInstanceOf[QuoteSer].isAdjusted
      toggleButton.setSelected(selected)
    }
  }

  def isAdjusted = toggleButton.isSelected
}

class SwitchAdjustQuoteAction extends CallableSystemAction {
  import SwitchAdjustQuoteAction._

  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
          def run {
            if (toggleButton.isSelected) {
              toggleButton.setSelected(false)
            } else {
              toggleButton.setSelected(true)
            }
          }
        });
    } catch {case ex: Exception =>}
        
  }
    
  def getName: String = {
    //return "Adjust Quote";
    val adjustquote = NbBundle.getMessage(this.getClass,"CTL_SwitchAdjustQuoteAction")
    adjustquote
  }
    
  def getHelpCtx: HelpCtx = {
    return HelpCtx.DEFAULT_HELP;
  }
    
  override protected def iconResource: String = {
    return "org/aiotrade/modules/ui/resources/switchAdjust.png";
  }
    
  override protected def asynchronous: Boolean = {
    return false;
  }
    
  override def getToolbarPresenter: Component = {
    val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/switchAdjust.png")
    val icon = new ImageIcon(iconImage)
        
    val adjustquote = NbBundle.getMessage(this.getClass,"CTL_SwitchAdjustQuoteAction")
    toggleButton = new JToggleButton
    toggleButton.setIcon(icon)
    toggleButton.setToolTipText(adjustquote)
        
    toggleButton.addItemListener(new ItemListener {
        def itemStateChanged(e: ItemEvent) {
          val state = e.getStateChange
                
          val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}
                
          val quoteSeries = analysisTc.viewContainer.masterView.asInstanceOf[WithQuoteChart].quoteSer
                
          if (state == ItemEvent.SELECTED) {
            if (!quoteSeries.isAdjusted) {
              quoteSeries.adjust()
            }
          } else {
            if (quoteSeries.isAdjusted) {
              quoteSeries.unadjust()
            }
          }
                
        }
      })
        
    toggleButton
  }
    
}




