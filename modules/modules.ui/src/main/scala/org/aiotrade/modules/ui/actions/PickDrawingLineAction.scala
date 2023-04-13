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
import javax.swing.DefaultSingleSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToggleButton;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.aiotrade.lib.charting.chart.handledchart.HandledChart;
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor;
import org.aiotrade.lib.charting.view.WithDrawingPane;
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.util.swing.action.ViewAction;
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities
import org.openide.util.NbBundle
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.WindowManager;

/**
 *
 * @author Caoyuan Deng
 */
object PickDrawingLineAction {

  private val handledCharts = PersistenceManager().lookupAllRegisteredServices(classOf[HandledChart], "HandledCharts")
  private val toolbarPresenter: JToggleButton = createToolbarPresenter
  val name = NbBundle.getMessage(this.getClass,"CTL_PickDrawingLineAction")
  val pleaseaddalayer = NbBundle.getMessage(this.getClass,"Please_add_a_layer_before_pick_drawing_line")
  val pickline = NbBundle.getMessage(this.getClass,"Pick_Drawing_Line")
  
  private def createToolbarPresenter: JToggleButton = {
    val menuItemListener: MyMenuItemListener = new MyMenuItemListener

    val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/drawingLine.png")
    val icon = new ImageIcon(iconImage)

    val toggleButton = new JToggleButton
    toggleButton.setIcon(icon)
/*     toggleButton.setToolTipText("Pick Drawing Line") */
    toggleButton.setToolTipText(name)

    val popupMenu = new JPopupMenu
    popupMenu.setSelectionModel(new DefaultSingleSelectionModel)
    for (handledChart <- handledCharts) {
      /** it's a selection menu other than an action menu, so use JRadioButtonMenuItem instead of JMenuItem */
      val item = new JRadioButtonMenuItem(handledChart.toString)
      item.addItemListener(menuItemListener)
      popupMenu.add(item)
    }

    toggleButton.addItemListener(new ItemListener {

        def itemStateChanged(e: ItemEvent) {
          if (e.getStateChange == ItemEvent.SELECTED) {
            /** show popup menu on toggleButton at position: (0, height) */
            popupMenu.show(toggleButton, 0, toggleButton.getHeight)
          }
        }
      })

    popupMenu.addPopupMenuListener(new PopupMenuListener {

        def popupMenuCanceled(e: PopupMenuEvent) {
          toggleButton.setSelected(false)
        }

        def popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
          toggleButton.setSelected(false)
        }

        def popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        }
      })

    toggleButton
  }

  private class MyMenuItemListener extends ItemListener {

    def itemStateChanged(e: ItemEvent) {
      if (e.getStateChange != ItemEvent.SELECTED) {
        return
      }

      val item = e.getSource.asInstanceOf[JMenuItem]
      /**
       * clear selected state. if want to do this, do not add items to buttonGroup. see bug report:
       * http://sourceforge.net/tracker/index.php?func=detail&aid=1579592&group_id=152032&atid=782880
       */
      item.setSelected(false)
      val analysisWin = AnalysisChartTopComponent.selected getOrElse {return}

      val viewContainer = analysisWin.viewContainer
      val masterView = viewContainer.masterView
      if (!(masterView.isInstanceOf [WithDrawingPane])) {
        return
      }

      val drawingPane = masterView.asInstanceOf[WithDrawingPane].selectedDrawing
      if (drawingPane == null) {
        JOptionPane.showMessageDialog(
          WindowManager.getDefault.getMainWindow,pleaseaddalayer,pickline,
//          "Please add a layer before pick drawing line",
//          "Pick drawing line",
          JOptionPane.OK_OPTION,
          null
        )
        return
      }

      val selectedStr = item.getText
      val theHandledChart = handledCharts find (_.toString.equalsIgnoreCase(selectedStr)) getOrElse null
      assert(theHandledChart != null, "A just picked handled chart should be there!")

      val content = viewContainer.controller.serProvider.content

      content.lookupDescriptor(
        classOf[DrawingDescriptor],
        drawingPane.layerName,
        viewContainer.controller.baseSer.freq
      ) match {
        case Some(descriptor) =>
          val handledChart = theHandledChart.createNewInstance
          handledChart.attachDrawingPane(drawingPane)
          drawingPane.setSelectedHandledChart(handledChart)

          descriptor.lookupAction(classOf[ViewAction]) foreach {_.execute}
        case None =>
          /** best effort, should not happen */
          viewContainer.controller.isCursorCrossLineVisible = false
          drawingPane.activate

          SwitchHideShowDrawingLineAction.updateToolbar(viewContainer)
      }
    }
  }
}

class PickDrawingLineAction extends CallableSystemAction {
  import PickDrawingLineAction._

  def performAction {
    java.awt.EventQueue.invokeLater(new Runnable {
        def run {
          toolbarPresenter.setSelected(true)
        }
      })
  }

  def getName: String = {
//    "Pick Drawing Line"
    name
  }

  def getHelpCtx: HelpCtx = {
    HelpCtx.DEFAULT_HELP
  }

  override protected def asynchronous: Boolean = {
    false
  }

  override def getToolbarPresenter: Component = {
    toolbarPresenter
  }

}
