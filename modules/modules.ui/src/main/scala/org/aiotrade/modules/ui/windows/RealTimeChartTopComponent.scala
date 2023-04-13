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
package org.aiotrade.modules.ui.windows

import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Action
import javax.swing.JPopupMenu
import javax.swing.SwingConstants
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.modules.ui.actions.SwitchCandleOhlcAction
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.modules.ui.actions.SwitchCalendarTradingTimeViewAction
import org.aiotrade.modules.ui.actions.ZoomInAction
import org.aiotrade.modules.ui.actions.ZoomOutAction
import org.openide.util.actions.SystemAction
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager
import scala.collection.mutable

/**
 * This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @note
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 * 
 * @author Caoyuan Deng
 */
class RealTimeChartTopComponent private (val sec: Sec) extends TopComponent {
  import RealTimeChartTopComponent._

  instanceRefs.put(this, null)

  private val symbol = sec.uniSymbol
  private val tc_id = sec.name + "_RT"

  private val rtSer = sec.realtimeSer
  if (!rtSer.isLoaded) sec.loadSer(rtSer)
  private val controller = ChartingController(sec, rtSer)
  val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], this)
  
  setLayout(new BorderLayout)
        
  add(viewContainer, SwingConstants.CENTER)
  setName(sec.name + " - RealTime")
        
  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
  popup.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
  popup.add(SystemAction.get(classOf[ZoomInAction]))
  popup.add(SystemAction.get(classOf[ZoomOutAction]))
        
  addMouseListener(new MouseAdapter {
      private def showPopup(e: MouseEvent) {
        if (e.isPopupTrigger) {
          popup.show(RealTimeChartTopComponent.this, e.getX, e.getY)
        }
      }

      override def mouseClicked(e: MouseEvent) {
        showPopup(e)
      }
      override def mousePressed(e: MouseEvent) {
        showPopup(e)
      }
      override def mouseReleased(e: MouseEvent) {
        showPopup(e)
      }
    })
        
  /** this component should setFocusable(true) to have the ability to grab the focus */
  setFocusable(true)

  /** Should forward focus to sub-component viewContainer */
  override def requestFocusInWindow: Boolean = {
    viewContainer.requestFocusInWindow
  }

  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    // hidden others in "editor" mode
    for (tc <- mode.getTopComponents if (tc ne this) && tc.isInstanceOf[RealTimeChartTopComponent]) {
      tc.close
    }

    mode.dockInto(this)
    super.open
  }
    
  override protected def componentActivated {
    super.componentActivated
  }
    
  override protected def componentClosed {
    instanceRefs.remove(this)
    super.componentClosed
  }
    
  override protected def preferredID: String = {
    tc_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }
    
  override def getActions: Array[Action] = {
    val actions = super.getActions
    val newActions = new Array[Action](actions.length + 1)
    for (i <- 0 until actions.length) {
      newActions(i) = actions(i)
    }
    newActions(actions.length) = SystemAction.get(classOf[SwitchCandleOhlcAction])
        
    newActions
  }

  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
    
  /*-
   final TopComponent.Registry registry = TopComponent.getRegistry();
   registry.addPropertyChangeListener(
   new PropertyChangeListener() {
   public void propertyChange(PropertyChangeEvent evt) {
   if (TopComponent.Registry.PROP_OPENED.equals(evt.getPropertyName())) {
   Set openedSet = registry.getOpened();
   if (openedSet != null) {
   for (Iterator iter = openedSet.iterator(); iter.hasNext(); ) {
   TopComponent topComponent = (TopComponent ) iter.next();
   // now see if the topComponent contains Java file
   Node[] nodes =  topComponent.getActivatedNodes();
   if (nodes != null && nodes.length > 0) {
   // you may want to go through all nodes here...I am showing 0th node only
   DataObject dataObject = (DataObject) nodes[0].getLookup().lookup(DataObject.class);
   if (dataObject instanceof HtmlDataObject) {
   FileObject theFile = dataObject.getPrimaryFile();
   OpenJavaClassThread run = new OpenJavaClassThread(theFile);
   RequestProcessor.getDefault().post(run);
   }
   }
   }
   }
   }
   }
   }
   );
   */
    
}

object RealTimeChartTopComponent {
  private val instanceRefs = mutable.WeakHashMap[RealTimeChartTopComponent, AnyRef]()
  def instances = instanceRefs.keys

  private val MODE = "chart"

  def apply(sec: Sec): RealTimeChartTopComponent = {
    val instance = instances find (_.sec eq sec) getOrElse new RealTimeChartTopComponent(sec)
    
    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

  def selected: Option[RealTimeChartTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: RealTimeChartTopComponent => Some(x)
      case _ => instances find (_.isShowing)
    }
  }

}

