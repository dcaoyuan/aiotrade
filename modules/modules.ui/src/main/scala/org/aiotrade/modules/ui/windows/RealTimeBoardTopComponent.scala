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
import org.aiotrade.lib.charting.view.ChartViewContainer
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.securities.model.Sec
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager
import scala.collection.mutable


/** This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @NOTE:
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
class RealTimeBoardTopComponent private (val sec: Sec) extends TopComponent {
  import RealTimeBoardTopComponent._

  instanceRefs.put(this, null)
    

  private var reallyClosed = false
    
  private val tc_id = sec.name + "_TK"
        
  private val boardPanel = RealTimeBoardPanel(sec)
        
  setLayout(new BorderLayout)
        
  add(boardPanel, BorderLayout.CENTER)
  setName("RealTime - " + sec.uniSymbol)
        
  setFocusable(false)

  def setReallyClosed(b: Boolean) {
    this.reallyClosed = b
  }

  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    // hidden others
    for (tc <- mode.getTopComponents if (tc ne this) && tc.isInstanceOf[RealTimeBoardTopComponent]) {
      tc.asInstanceOf[RealTimeBoardTopComponent].setReallyClosed(false)
      tc.close
    }

    mode.dockInto(this)
    super.open
  }

  override def requestActive {
    // do not active it
  }

  override protected def componentActivated {
    // do not active it
  }
    
  override protected def componentShowing {
    super.componentShowing
  }
    
  override protected def componentClosed {
    if (reallyClosed) {
      super.componentClosed
    } else {
      val tc = WindowManager.getDefault.findTopComponent("RealTimeWatchList")
      if (tc != null && tc.isOpened) {
        /** closing is not allowed */
      } else {
        super.componentClosed
      }
      /**
       * do not remove it from instanceRefs here, so can be called back.
       * remove it via RealtimeWatchListTopComponent if necessary
       */
    }
  }
    
  override protected def preferredID: String = {
    tc_id
  }
    
  override def getPersistenceType: Int = {
    TopComponent.PERSISTENCE_NEVER
  }
    
  def realTimeChartViewContainer: Option[ChartViewContainer] = {
    boardPanel.realTimeChartViewContainer
  }
    
  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }
    
}


object RealTimeBoardTopComponent {
  private val instanceRefs = mutable.WeakHashMap[RealTimeBoardTopComponent, AnyRef]()
  def instances = instanceRefs.keys

  /** The Mode this component will live in */
  val MODE = "board"

  def apply(sec: Sec): RealTimeBoardTopComponent = {
    val instance = instances find (_.sec eq sec) getOrElse RealTimeBoardTopComponent(sec)

    if (!instance.isOpened) {
      instance.open
    }

    instance
  }

}

