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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TUnit
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;

/**
 *
 * @author Caoyuan Deng
 */
object SwitchFrequencyAction {
  private var toggleButton: JToggleButton = _
  private var buttonGroup: ButtonGroup = _
  private var popup: JPopupMenu = _
  
  def setSelectedItem(baseSignSeries: BaseTSer) {
//        AbstractSignSeries.CombinedFrequency combinedFrequency = baseSignSeries.getCombinedFrequency();
//        
//        for (MenuElement item : popup.getSubElements()) {
//            if (item instanceof JRadioButtonMenuItem) {
//                if (((JRadioButtonMenuItem)item).getText().equalsIgnoreCase(combinedFrequency.toString())) {
//                    ((JRadioButtonMenuItem)item).setSelected(true);
//                    
//                    updateToggleButtonText(combinedFrequency, baseSignSeries);
//                }
//            }
//        }
  }
    
  private def updateToggleButtonText(unit: TUnit, baseSer: BaseTSer) {
        
  }


}
class SwitchFrequencyAction extends CallableSystemAction {
  import SwitchFrequencyAction._

  private var menuItemListener: MenuItemListener = _
    
  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable() {
        def run {
        }
      });
        
  }
    
  def getName: String = {
    //"Switch Frequency"
    val name = NbBundle.getMessage(this.getClass,"CTL_SwitchFrequencyAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    return HelpCtx.DEFAULT_HELP;
  }
    
  override protected def asynchronous: Boolean = {
    return false;
  }
    
  override def getToolbarPresenter: Component = {
    val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/dropdown.png");
    val icon = new ImageIcon(iconImage);
    val name = NbBundle.getMessage(this.getClass,"CTL_SwitchFrequencyAction")
        
    toggleButton = new JToggleButton
        
    toggleButton.setForeground(Color.BLUE);
    toggleButton.setFont(new Font("Courier New", Font.ITALIC, 15));
    toggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
    toggleButton.setText("1d");
    toggleButton.setIcon(icon);
    toggleButton.setToolTipText(name)
        
    popup = new JPopupMenu
    menuItemListener = new MenuItemListener
        
    buttonGroup = new ButtonGroup
        
    for (unit <- TUnit.values) {
      val item = new JRadioButtonMenuItem(unit.toString)
      if (unit == TUnit.Day) {
        item.setSelected(true)
      }
      item.addActionListener(menuItemListener);
      buttonGroup.add(item);
      popup.add(item);
    }
        
    toggleButton.addItemListener(new ItemListener() {
        def itemStateChanged(e: ItemEvent) {
          val state = e.getStateChange
          if (state == ItemEvent.SELECTED) {
            /** show popup menu on toggleButton at position: (0, height) */
            popup.show(toggleButton, 0, toggleButton.getHeight());
          }
        }
      });
        
    popup.addPopupMenuListener(new PopupMenuListener() {
        def popupMenuCanceled(e: PopupMenuEvent) {
          toggleButton.setSelected(false);
        }
        def popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
          toggleButton.setSelected(false);
        }
        def popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        }
      })
        
        
    return toggleButton;
  }
    
    
  private class MenuItemListener extends ActionListener {
    def actionPerformed(ev: ActionEvent) {
            
    }
  }
    
}


