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
package org.aiotrade.modules.ui.windows;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.Action;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.swing.AIOScrollView
import org.aiotrade.modules.ui.actions.SwitchCandleOhlcAction
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
 * @NOTICE:
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
class RealTimeChartsTopComponent private () extends TopComponent {
  import RealTimeChartsTopComponent._

  instanceRefs.put(this, null)
    
  private val tc_id = "RealtimeCharts"
    
  private var secToViewContainers = Map[Sec, ChartViewContainer]()
    
  private var reallyClosed = false
            
  private val scrollTimerListener = new ScrollTimerListener
  private val scrollTimer = new Timer(SCROLL_SPEED_THROTTLE, scrollTimerListener)
  scrollTimer.setInitialDelay(0)  // default InitialDelay
    
  setName("Watch List RealTime Charts")
        
  private val scrollView = new AIOScrollView(this, secToViewContainers.valuesIterator.toList)
  scrollView.setBackground(LookFeel().backgroundColor)
        
  //viewPort = new JViewport();
  //viewPort.setView(scrollView);
        
  setLayout(new BorderLayout)
  add(scrollView, BorderLayout.CENTER)
        
  private val popup = new JPopupMenu
  popup.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
  popup.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
  popup.add(SystemAction.get(classOf[ZoomInAction]))
  popup.add(SystemAction.get(classOf[ZoomOutAction]))
        
  private val myMouseAdapter = new MouseAdapter {
    override def mouseClicked(e: MouseEvent) {
      showPopup(e)
    }
            
    override def mousePressed(e: MouseEvent) {
      showPopup(e)
    }
            
    override def mouseReleased(e: MouseEvent) {
      showPopup(e)
    }
  }
  addMouseListener(myMouseAdapter)
        
  /** this component should setFocusable(true) to have the ability to grab the focus */
  setFocusable(true)
  
    
  def watch(sec: Sec) {
    if (!secToViewContainers.contains(sec)) {
      val rtSer = sec.realtimeSer
      if (!rtSer.isLoaded) sec.loadSer(rtSer)
      val controller = ChartingController(sec, rtSer)
      val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], this)
            
      viewContainer.isInteractive = false
            
      secToViewContainers += (sec -> viewContainer)
            
      scrollView.add(viewContainer)
      scrollView.addPicture(viewContainer)
      scrollView.repaint()
    }
        
    scrollTimer.stop
    scrollTimerListener.startScrollTimerIfNecessary
  }
    
  def unWatch(sec: Sec) {
    for (viewContainer <- secToViewContainers.get(sec)) {
      scrollView.remove(viewContainer)
      scrollView.removePicture(viewContainer)
      secToViewContainers -= sec
    }
  }
    
  def viewContainers = {
    secToViewContainers.valuesIterator.toList
  }
    
  def showPopup(e: MouseEvent) {
    if (e.isPopupTrigger) {
      popup.show(this, e.getX, e.getY)
    }
  }
    
  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    mode.dockInto(this)
        
    scrollTimer.stop
    scrollTimerListener.startScrollTimerIfNecessary
        
    super.open
  }
    
  override protected def componentActivated {
    super.componentActivated
  }
    
  def setReallyClosed(b: Boolean) {
    this.reallyClosed = b
  }

  override protected def componentClosed {
    scrollTimer.stop
        
    if (reallyClosed) {
      super.componentClosed
    } else {
      val win = WindowManager.getDefault().findTopComponent("RealtimeWatchList");
      if (win.isOpened) {
        /** closing is not allowed */
      } else {
        super.componentClosed
      }
    }
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
    if (myMouseAdapter != null) {
      removeMouseListener(myMouseAdapter)
    }
    super.finalize
  }
    
    
    
  /**
   * Listener for timer events.
   */
  private class ScrollTimerListener extends ActionListener {
        
    def startScrollTimerIfNecessary {
      if (secToViewContainers.isEmpty || scrollTimer.isRunning) {
        return
      }
            
      scrollTimer.start
    }
        
    def actionPerformed(e: ActionEvent) {
      /** mouse is in this TopComponent? if yes, do nothing, else scroll */
      if (RealTimeChartsTopComponent.this.getMousePosition() == null) {
        scrollView.setBackground(LookFeel().backgroundColor)
        scrollView.scrollByPicture(1)
        //scrollView.scrollByPixel(3);
      }
    }
  }
    
    
}


object RealTimeChartsTopComponent {
  private val instanceRefs = mutable.WeakHashMap[RealTimeChartsTopComponent, AnyRef]()
  def instances = instanceRefs.keys

  private val SCROLL_SPEED_THROTTLE = 2400 // delay in milli seconds

  /** The Mode this component will live in. */
  private val MODE = "chart"


  def apply(): RealTimeChartsTopComponent = {
    val instance = if (instances.isEmpty) {
      new RealTimeChartsTopComponent
    } else instances.head

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }
}

