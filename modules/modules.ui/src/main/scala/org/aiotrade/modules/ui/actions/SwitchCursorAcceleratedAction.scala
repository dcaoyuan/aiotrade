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
import org.aiotrade.lib.charting.view.ChartingController
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
object SwitchCursorAcceleratedAction {
  private var toggleButton: JToggleButton = _

  def isCursorAccelerated: Boolean = {
    toggleButton.isSelected
  }

  def setSelected(b: Boolean) {
    toggleButton.setSelected(b)
  }
}

import SwitchCursorAcceleratedAction._
class SwitchCursorAcceleratedAction extends CallableSystemAction {
    
  /**
   * @NOTICE
   * If override getToolbarPresenter(), you should process action in that
   * component instead of here.
   */
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          if (toggleButton.isSelected) {
            toggleButton.setSelected(false)
          } else {
            toggleButton.setSelected(true)
          }
        }
      })
        
  }
    
  def getName: String = {
    "Accelerate Cursor Moving"
  }
    
  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }
    
  override protected def iconResource: String = {
    "org/aiotrade/modules/ui/resources/switchCursorAcceleratedAction.gif"
  }
    
  override protected def asynchronous: Boolean = {
    false
  }
    
  override def getToolbarPresenter: Component = {
    val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/switchCursorAcceleratedAction.gif")
    val icon = new ImageIcon(iconImage);
    val fastmoving = NbBundle.getMessage(this.getClass,"CTL_SwitchCursorAcceleratedAction")

    toggleButton = new JToggleButton
    toggleButton.setIcon(icon)
    toggleButton.setToolTipText(fastmoving)
        
    toggleButton.addItemListener(new ItemListener {
        def itemStateChanged(e: ItemEvent) {
          val state = e.getStateChange
                
          ChartingController.DefaultChartingController.isCursorAccelerated = (state == ItemEvent.SELECTED)
        }
      })
        
    toggleButton
  }
}


