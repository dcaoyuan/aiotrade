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
import java.awt.Image
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.logging.Logger
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import javax.swing.JSplitPane
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.charting.view.WithDrawingPane
import org.aiotrade.lib.charting.view.pane.DrawingPane
import org.aiotrade.lib.view.securities.AnalysisChartViewContainer
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.lib.indicator.Indicator
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.modules.ui.actions.ChangeOptsAction
import org.aiotrade.modules.ui.actions.ChangeStatisticChartOptsAction
import org.aiotrade.modules.ui.actions.MarketNewsAction
import org.aiotrade.modules.ui.actions.PickIndicatorAction
import org.aiotrade.modules.ui.actions.RemoveCompareQuoteChartsAction
import org.aiotrade.modules.ui.actions.SwitchAdjustQuoteAction
import org.aiotrade.modules.ui.actions.SwitchCandleOhlcAction
import org.aiotrade.modules.ui.actions.SwitchLinearLogScaleAction
import org.aiotrade.modules.ui.actions.SwitchCalendarTradingTimeViewAction
import org.aiotrade.modules.ui.actions.SwitchHideShowDrawingLineAction
import org.aiotrade.modules.ui.actions.ZoomInAction
import org.aiotrade.modules.ui.actions.ZoomOutAction
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.aiotrade.modules.ui.nodes.AddToFavoriteAction
import org.openide.nodes.Node
import org.openide.util.ImageUtilities
import org.openide.util.actions.SystemAction
import org.openide.windows.TopComponent
import org.openide.windows.WindowManager
import scala.collection.mutable


/**
 * This class implements serializbale by inheriting TopComponent, but should
 * overide writeExternal() and readExternal() to implement own serializable
 * instead of via transient modifies.
 *
 * @Note
 * when run/debug modules in NetBeans' IDE, the module will be
 * reloaded always, thus, when moduel has been disable but the reloading
 * procedure still not finished yet, deserialization will fail and throws
 * exception. So, it's better to test serialization out of the IDE.
 *
 * @author Caoyuan Deng
 */
class AnalysisChartTopComponent private ($sec: Sec) extends TopComponent {
  import AnalysisChartTopComponent._

  private var addToFavActionMenuItem: JMenuItem = _

  setFont(LookFeel().axisFont)

  private val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  splitPane.setFocusable(false)
  //splitPane.setBorder(BorderFactory.createEmptyBorder)
  splitPane.setOneTouchExpandable(true)
  splitPane.setDividerSize(3)

  // setting the resize weight to 1.0 makes the right or bottom component's size remain fixed
  splitPane.setResizeWeight(1.0)
  // to make the right component pixels wide
  splitPane.setDividerLocation(splitPane.getSize().width -
                               splitPane.getInsets.right -
                               splitPane.getDividerSize -
                               RealTimeBoardPanel.DIM.width)

  setLayout(new BorderLayout)
  add(splitPane, BorderLayout.CENTER)

  // component should setFocusable(true) to have the ability to gain the focus
  setFocusable(true)
  // Should forward focus to sub-component viewContainer
  this.addFocusListener(new FocusListener {
      def focusGained(e: FocusEvent) {
        if (viewContainer != null) {
          viewContainer.requestFocusInWindow
        }
      }

      def focusLost(e: FocusEvent) {}
    })

  private var state: State = _
  
  /**
   * Current sec of this tc, it may be different from original sec that was passed via constructor.
   */
  def sec = state.sec
  def viewContainer = state.viewContainer
  def realTimeBoard = state.realTimeBoard
  def freq = state.contractFreq

  def init(currSec: Sec) {
    if (state != null) {
      realTimeBoard.unWatch
      splitPane.remove(realTimeBoard)
      splitPane.remove(viewContainer)

      val prevSec = state.sec
      if (currSec != prevSec) {
        prevSec.resetSers
        val prevContent = prevSec.content
        prevContent.lookupDescriptors(classOf[IndicatorDescriptor]) foreach {_.resetInstance}
        // change back to its quote contract to default freq
        prevContent.lookupDescriptors(classOf[QuoteContract]) foreach {_.freq = defaultFreq}
      }
    }
    
    state = new State(currSec)
  }

  override def open {
    val mode = WindowManager.getDefault.findMode(MODE)
    // hidden others in "editor" mode
    /* for (tc <- mode.getTopComponents if (tc ne this) && tc.isInstanceOf[AnalysisChartTopComponent]) {
     tc.close
     } */

    /**
     * !NOTICE
     * mode.dockInto(this) seems will close this first if this.isOpened()
     * So, when call open(), try to check if it was already opened, if true,
     * no need to call open() again
     */
    mode.dockInto(this)
    super.open
  }

  /**
   * Although we have added a FocusListener to transfer the focuse to viewContainer,
   * it seems that we still need to write the following code. Don't ask me why.
   */
  override def requestFocusInWindow: Boolean = {
    viewContainer.requestFocusInWindow
  }
  
  override protected def componentActivated {
    super.componentActivated
    //updateToolbar
  }
    
  override protected def componentShowing {
    super.componentShowing
  }
    
  override protected def componentClosed {
    instanceRefs.remove(this)
    realTimeBoard.unWatch
    super.componentClosed
    /**
     * componentClosed not means it will be destroied, just make it invisible,
     * so, when to call dispose() ?
     */
    //sec.setSignSeriesLoaded(false);
  }

  override def getIcon: Image = iconImage
  override def getPersistenceType: Int = TopComponent.PERSISTENCE_NEVER
  override protected def preferredID: String = state.tcId
    
  private def updateToolbar {
    SwitchCalendarTradingTimeViewAction.updateToolbar(viewContainer)
    SwitchAdjustQuoteAction.updateToolbar(viewContainer)
    SwitchHideShowDrawingLineAction.updateToolbar(viewContainer)
            
    viewContainer.requestFocusInWindow
  }
    
  def lookupIndicator(descriptor: IndicatorDescriptor): Option[Indicator] = {
    val a = viewContainer.lookupChartView(descriptor) foreach {chartView =>
      chartView.allSers find {_.getClass.getName == descriptor.serviceClassName} match {
        case None =>
        case some => return some.asInstanceOf[Option[Indicator]]
      }
    }
    
    None
  }
    
  def lookupDrawing(descriptor: DrawingDescriptor): Option[DrawingPane] = {
    viewContainer.masterView match {
      case drawingPane: WithDrawingPane => drawingPane.descriptorToDrawing.get(descriptor)
      case _ => None
    }
  }
    
  @throws(classOf[Throwable])
  override protected def finalize {
    super.finalize
  }

  class State(val sec: Sec) {
    // set class fields
    val tcId = sec.secInfo.name
    val symbol = sec.uniSymbol

    // should keep contractFreq before load realtime ser which may change quoteContract in content
    // @Todo: use multiple quoteContracts?
    val content = sec.content
    private val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]) get
    val contractFreq = quoteContract.freq

    sec.resetSers
    content.lookupDescriptors(classOf[IndicatorDescriptor]) foreach {_.resetInstance}

    val realTimeBoard = RealTimeBoardPanel(sec)
    realTimeBoard.watch
    
    private val ser = sec.serOf(contractFreq).get
    if (!ser.isLoaded) sec.loadSer(ser)

    private val isRealtime = contractFreq == TFreq.ONE_SEC
    if (!isRealtime) {
      if (SwitchAdjustQuoteAction.isAdjusted) ser.adjust()
    }
    sec.subscribeTickerServer(true)
    
    val viewContainer = createViewContainer(sec, ser, isRealtime)

    splitPane.setLeftComponent(viewContainer)
    splitPane.setRightComponent(realTimeBoard)
    splitPane.revalidate

    setName(sec.secInfo.name + " - " + contractFreq)

    private val popupMenu = new JPopupMenu
    popupMenu.add(SystemAction.get(classOf[SwitchCandleOhlcAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchCalendarTradingTimeViewAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchLinearLogScaleAction]))
    popupMenu.add(SystemAction.get(classOf[SwitchAdjustQuoteAction]))
    popupMenu.add(SystemAction.get(classOf[ZoomInAction]))
    popupMenu.add(SystemAction.get(classOf[ZoomOutAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[PickIndicatorAction]))
    popupMenu.add(SystemAction.get(classOf[ChangeOptsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[ChangeStatisticChartOptsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[MarketNewsAction]))
    popupMenu.addSeparator
    popupMenu.add(SystemAction.get(classOf[RemoveCompareQuoteChartsAction]))

    SymbolNodes.findSymbolNode(sec.uniSymbol) foreach {node =>
      AnalysisChartTopComponent.this.setActivatedNodes(Array(node))
      popupMenu.add(node.getLookup.lookup(classOf[AddToFavoriteAction]))
      injectActionsToDescriptors(node)
    }

    /** inject popup menu from this TopComponent */
    viewContainer.setComponentPopupMenu(popupMenu)


    /**
     * Since we use 1min freq as the ser's freq when contractFreq is 1sec, we should pass contractFreq here
     */
    private def createViewContainer(sec: Sec, baseSer: QuoteSer, isRealtime: Boolean) = {
      log.info("Creating viewContainer for ser: " + System.identityHashCode(baseSer) + " - , isRealtime=" + isRealtime)

      val controller = ChartingController(sec, baseSer)
      if (isRealtime) {
        controller.createChartViewContainer(classOf[RealTimeChartViewContainer], AnalysisChartTopComponent.this)
      } else {
        controller.createChartViewContainer(classOf[AnalysisChartViewContainer], AnalysisChartTopComponent.this)
      }
    }

    /** we choose here to lazily create actions instances */
    private def injectActionsToDescriptors(node: Node) {
      /** init all children of node to create the actions that will be injected to descriptor */
      initNodeChildrenRecursively(node)
    }

    private def initNodeChildrenRecursively(node: Node) {
      if (!node.isLeaf) {
        /** call getChildren().getNodes(true) to initialize all children nodes */
        val childrenNodes = node.getChildren.getNodes(true)
        for (child <- childrenNodes) {
          initNodeChildrenRecursively(child)
        }
      }
    }
  }
}

object AnalysisChartTopComponent {
  private val log = Logger.getLogger(this.getClass.getName)

  private val instanceRefs = mutable.WeakHashMap[AnalysisChartTopComponent, AnyRef]()
  def instances = instanceRefs.keys

  val STANDALONE = "STANDALONE"

  private var singleton: AnalysisChartTopComponent = _
  private var defaultFreq = TFreq.DAILY

  // The Mode this component will live in.
  val MODE = "chart"

  private val iconImage = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/stock.png")

  def instanceOf(symbol: String): Option[AnalysisChartTopComponent] = {
    instances find {_.sec.uniSymbol.equalsIgnoreCase(symbol)}
  }

  def apply(sec: Sec, standalone: Boolean = false): AnalysisChartTopComponent = {
    val content = sec.content
    val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get
    val freq = quoteContract.freq
    
    if (standalone) {
      val instance = instances find {x => x.sec == sec && x.freq == freq} getOrElse {
        val x = new AnalysisChartTopComponent(sec)
        instanceRefs.put(x, null)
        x
      }
      
      instance.init(sec)
      if (!instance.isOpened) {
        instance.open
      }
      instance
    } else {
      if (singleton == null) {
        singleton = new AnalysisChartTopComponent(sec)
        instanceRefs.put(singleton, null)
      }

      singleton.init(sec)
      singleton
    }
  }

  def selected: Option[AnalysisChartTopComponent] = {
    TopComponent.getRegistry.getActivated match {
      case x: AnalysisChartTopComponent => Some(x)
      case _ => instances find (_.isShowing)
    }
  }
}

