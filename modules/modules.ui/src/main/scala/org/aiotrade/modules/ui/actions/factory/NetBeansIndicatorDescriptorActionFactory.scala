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
package org.aiotrade.modules.ui.actions.factory

import java.util.ResourceBundle
import javax.swing.Action
import javax.swing.JOptionPane
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.indicator.IndicatorDescriptorActionFactory
import org.aiotrade.lib.math.indicator.ComputeFrom
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.util.swing.action.DeleteAction
import org.aiotrade.lib.util.swing.action.EditAction
import org.aiotrade.lib.util.swing.action.HideAction
import org.aiotrade.lib.util.swing.action.SaveAction
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.dialogs.ChangeIndicatorFactorsPane
import org.aiotrade.modules.ui.nodes.IndicatorGroupDescriptor
import org.aiotrade.modules.ui.windows.ExplorerTopComponent
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.openide.loaders.DataFolder
import org.openide.nodes.Node
import org.openide.windows.WindowManager

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, December 11, 2006, 10:20 PM
 * @since   1.0.4
 */
class NetBeansIndicatorDescriptorActionFactory extends IndicatorDescriptorActionFactory {
  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.actions.Bundle")

  def createActions(descriptor: IndicatorDescriptor): Array[Action] = {
    Array(
      new IndicatorViewAction(descriptor),
      new IndicatorHideAction(descriptor),
      new IndicatorDeleteAction(descriptor),
      new IndicatorEditAction(descriptor)
    )
  }
    
  private class IndicatorViewAction(descriptor: IndicatorDescriptor) extends ViewAction {
    val show = Bundle.getString("Show")
//    putValue(Action.NAME, "Show")
    putValue(Action.NAME, show)
        
    def execute {
      descriptor.active = true
      descriptor.containerContent.lookupAction(classOf[SaveAction]) foreach {_.execute}
            
      for (analysisTc <- AnalysisChartTopComponent.instanceOf(descriptor.containerContent.uniSymbol);
           viewContainer = analysisTc.viewContainer;
           indicator <- descriptor.serviceInstance(viewContainer.controller.baseSer)
      ) {
        viewContainer.lookupChartView(descriptor) match {
          case Some(view) => viewContainer.selectedView = view
          case None =>
            /* @Note descriptor's opts may be set by this call */
            indicator ! ComputeFrom(0)
                    
            if (indicator.isOverlapping) {
              if (!LookFeel().isAllowMultipleIndicatorOnQuoteChartView) {
                // hide previous overlapping indicator first if there is one
                viewContainer.lookupIndicatorDescriptor(viewContainer.masterView) foreach {existedOne =>
                  existedOne.lookupAction(classOf[HideAction]).get.execute
                }
              }
              viewContainer.addSlaveView(descriptor, indicator, null)
              viewContainer.repaint()
            } else {
              viewContainer.addSlaveView(descriptor, indicator, null)
              viewContainer.adjustViewsHeight(0)
            }
        }

        analysisTc.requestActive
      }
      
    }
  }
    
  private class IndicatorHideAction(descriptor: IndicatorDescriptor) extends HideAction {
    //putValue(Action.NAME, "Hide")
    val Hide = Bundle.getString("Hide")
//    putValue(Action.NAME, "Hide")
    putValue(Action.NAME, Hide)
        
    def execute {
      descriptor.active = false
      descriptor.containerContent.lookupAction(classOf[SaveAction]) foreach {_.execute}
            
      for (analysisTc <- AnalysisChartTopComponent.instanceOf(descriptor.containerContent.uniSymbol)) {
        val viewContainer = analysisTc.viewContainer
        viewContainer.removeSlaveView(descriptor)
                
        analysisTc.requestActive
      }
        
    }
  }
    
  private class IndicatorDeleteAction(descriptor: IndicatorDescriptor) extends DeleteAction {
    val delete = Bundle.getString("Delete")
//    putValue(Action.NAME, "Delete")
    putValue(Action.NAME, delete)
        
    def execute {
      val Are_you_sure = Bundle.getString("Are_You_Sure_Delete_indicator")
      val deleting = Bundle.getString("Deleting_Indicator")
      val confirm = JOptionPane.showConfirmDialog(
        WindowManager.getDefault.getMainWindow,Are_you_sure,deleting,
//        "Are you sure you want to delete indicator: " + descriptor.displayName + " ?",
//        "Deleting indicator ...",
        JOptionPane.YES_NO_OPTION
      )
            
      if (confirm == JOptionPane.YES_OPTION) {
        descriptor.lookupAction(classOf[HideAction]) foreach (_.execute)
                
        descriptor.containerContent.removeDescriptor(descriptor)
        descriptor.containerContent.lookupAction(classOf[SaveAction]) foreach (_.execute)
      }
    }
        
  }
    
    
  /** Action to change options */
  private class IndicatorEditAction(descriptor: IndicatorDescriptor) extends EditAction {
    val options = Bundle.getString("Change_Options")
    putValue(Action.NAME,options)
        
    def execute {
      val pane = new ChangeIndicatorFactorsPane(WindowManager.getDefault.getMainWindow, descriptor)
            
      // added listener, so when spnner changed, could preview
      val spinnerChangeListener = new ChangeListener {
        def stateChanged(e: ChangeEvent) {
          showEffect(descriptor)
        }
      }
            
      pane.addSpinnerChangeListener(spinnerChangeListener)
      val retValue = pane.showDialog
      pane.removeSpinnerChangeListener(spinnerChangeListener)
            
      if (retValue == JOptionPane.OK_OPTION) {
        // apple to all ?
        if (pane.isApplyToAll) {
          val root = ExplorerTopComponent().rootNode
          setIndicatorFactorsRecursively(root, descriptor)
        } else { // else, only apply to this one
          setIndicatorFactors(descriptor, descriptor.factors)
        }
                
        if (pane.isSaveAsDefault) {
          val defaultContent = PersistenceManager().defaultContent
          defaultContent.lookupDescriptor(classOf[IndicatorDescriptor],
                                          descriptor.serviceClassName,
                                          descriptor.freq
          ) match {
            case Some(x) =>
              x.factors = descriptor.factors
            case None =>
              val defaultOne = new IndicatorDescriptor(descriptor.serviceClassName, descriptor.freq, descriptor.factors, false)
              defaultContent.addDescriptor(defaultOne)
          }
                    
          PersistenceManager().saveContent(defaultContent)
        }
      } else { // else, opts may have been changed when preview, so, should do setOpts to restore old opts to indicator instance
        setIndicatorFactors(descriptor, descriptor.factors)
      }
            
    }
        
    /**
     * @TODO
     * If node not expanded yet, getChilder() seems return null, because the children will
     * not be created yet.
     */
    private def setIndicatorFactorsRecursively(rootNodeToBeSet: Node, descriptorWithOpts: IndicatorDescriptor) {
      // folder node ?
      if (rootNodeToBeSet.getLookup.lookup(classOf[DataFolder]) != null) {
        for (child <- rootNodeToBeSet.getChildren.getNodes) {
          // do recursive call
          setIndicatorFactorsRecursively(child, descriptorWithOpts)
        }
      } else { // else, a SecurityNode
        val content = rootNodeToBeSet.getLookup.lookup(classOf[Content])
        val indicatorGroupNode = rootNodeToBeSet.getChildren.findChild(IndicatorGroupDescriptor.NAME)
        if (indicatorGroupNode != null) {
          for (descriptorToBeSet <- content.lookupDescriptor(classOf[IndicatorDescriptor],
                                                             descriptorWithOpts.serviceClassName,
                                                             descriptorWithOpts.freq);
               child <- indicatorGroupNode.getChildren.getNodes
          ) {
            setIndicatorFactors(descriptorToBeSet, descriptorWithOpts.factors)
          }
        }
      }
    }
        
    private def setIndicatorFactors(descriptorToBeSet: IndicatorDescriptor, factors: Array[Factor]) {
      descriptorToBeSet.factors = factors
      descriptorToBeSet.containerContent.lookupAction(classOf[SaveAction]) foreach (_.execute)
            
      showEffect(descriptorToBeSet)
    }
        
    private def showEffect(descriptorToBeSet: IndicatorDescriptor) {
      for (analysisWin <- AnalysisChartTopComponent.instanceOf(descriptorToBeSet.containerContent.uniSymbol);
           descriptor <- analysisWin.lookupIndicator(descriptor)
      ) {
        descriptor.factors = descriptorToBeSet.factors
      }
    }
  }
}


