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

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.view.ChartViewContainer;
import org.aiotrade.lib.charting.view.WithDrawingPane;
import org.aiotrade.lib.util.swing.action.HideAction;
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
object SwitchHideShowDrawingLineAction {
  private var toggleButton: JToggleButton = _

  def updateToolbar(selectedViewContainer: ChartViewContainer) {
    val masterView = selectedViewContainer.masterView;
    if (masterView.isInstanceOf[WithDrawingPane]) {
      val drawing = masterView.asInstanceOf[WithDrawingPane].selectedDrawing
      if (drawing != null) {
        val selected = drawing.isActivated
        toggleButton.setSelected(selected)
      } else {
        toggleButton.setSelected(false)
      }
    }
  }

}
class SwitchHideShowDrawingLineAction extends CallableSystemAction {
  import SwitchHideShowDrawingLineAction._
    
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          if (toggleButton.isSelected) {
            toggleButton.setSelected(false);
          } else {
            toggleButton.setSelected(true);
          }
        }
      })
  }
    
  def getName: String = {
    //"Hide or Show Drawing Line"
    val name = NbBundle.getMessage(this.getClass,"CTL_SwitchHideShowDrawingLineAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/hideDrawingLine.gif"
  }
    
  override protected def asynchronous: Boolean = {
    false
  }
        
  override def getToolbarPresenter: Component = {
    val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/hideDrawingLine.gif");
    val icon = new ImageIcon(iconImage);
    val name = NbBundle.getMessage(this.getClass,"CTL_SwitchHideShowDrawingLineAction")

    toggleButton = new JToggleButton();
    toggleButton.setIcon(icon);
    toggleButton.setToolTipText(name);
        
    toggleButton.addItemListener(new ItemListener() {
        def itemStateChanged(e: ItemEvent) {
          val analysisWin = AnalysisChartTopComponent.selected getOrElse {return}
                
          val viewContainer = analysisWin.viewContainer
          val masterView = viewContainer.masterView.asInstanceOf[WithDrawingPane]
          if (masterView.selectedDrawing == null) {
            return;
          }
                
          val baseSer = viewContainer.controller.baseSer
          val content = viewContainer.controller.serProvider.content
          content.lookupDescriptor(
            classOf[DrawingDescriptor],
            masterView.selectedDrawing.layerName,
            baseSer.freq
          ) foreach {descriptor =>
            if (e.getStateChange == ItemEvent.SELECTED) {
              /** judge again to aviod recursively calling */
              if (!masterView.asInstanceOf[WithDrawingPane].selectedDrawing.isActivated) {
                descriptor.lookupAction(classOf[ViewAction]) foreach {_.execute}
              }
            } else {
              if (masterView.asInstanceOf[WithDrawingPane].selectedDrawing.isActivated) {
                descriptor.lookupAction(classOf[HideAction]) foreach {_.execute}
              }
            }
          }
        }
      })
        
    toggleButton
  }
    
}



