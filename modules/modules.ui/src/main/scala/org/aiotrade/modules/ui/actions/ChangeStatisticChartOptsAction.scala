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
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.aiotrade.lib.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.indicator.ProbMassIndicator
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.aiotrade.modules.ui.dialogs.ChangeIndicatorFactorsPane;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
class ChangeStatisticChartOptsAction extends CallableSystemAction {
    
  def performAction {
    try {
      java.awt.EventQueue.invokeLater(new Runnable() {
          def run {
            val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}
                    
            val selectedView = analysisTc.viewContainer.selectedView
            val ind = selectedView.overlappingSers find (x => x.isInstanceOf[ProbMassIndicator]) getOrElse {return}
            val indicator = ind.asInstanceOf[Indicator]

            val descriptor = new IndicatorDescriptor
            descriptor.serviceClassName = (indicator.getClass.getName)
            descriptor.factors = indicator.factors
                    
            val pane = new ChangeIndicatorFactorsPane(WindowManager.getDefault.getMainWindow, descriptor)
                    
            // added listener, so when spnner changed, could preview
            val spinnerChangeListener = new ChangeListener {
              def stateChanged(e: ChangeEvent) {
                indicator.factors = descriptor.factors
              }
            }
                    
            pane.addSpinnerChangeListener(spinnerChangeListener);
                    
            val retValue = pane.showDialog
                    
            pane.removeSpinnerChangeListener(spinnerChangeListener);
                    
            if (retValue == JOptionPane.OK_OPTION) {
                        
              indicator.factors = descriptor.factors
                        
            } else {
                        
              /** opts may has been changed when preview, so, should do setOpts to restore old params to indicator instance */
              indicator.factors = descriptor.factors
                        
            }
          }
                
        })
    } catch {case ex: Exception=>}
        
  }
    
  def getName: String = {
    //"Change Statistic Chart's Options";
    val name = NbBundle.getMessage(this.getClass,"CTL_ChangeStatisticChartOptsAction")
    name
  }
    
  def getHelpCtx: HelpCtx = {
    return HelpCtx.DEFAULT_HELP;
  }
    
  override
  protected def asynchronous: Boolean = {
    return false;
  }
    
}

