/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.modules.ui.nodes

import java.awt.Image
import java.awt.event.ActionEvent
import java.beans.IntrospectionException
import java.beans.PropertyChangeEvent
import java.io.IOException
import java.io.PrintStream
import java.util.ResourceBundle
import java.util.logging.Logger
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JOptionPane
import org.aiotrade.lib.view.securities.AnalysisChartView
import org.aiotrade.lib.view.securities.persistence.ContentPersistenceHandler
import org.aiotrade.lib.indicator.QuoteCompareIndicator
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor
import org.aiotrade.lib.securities.indices.CSI300
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.SecPicking
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sector
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.swing.action.GeneralAction
import org.aiotrade.lib.util.swing.action.SaveAction
import org.aiotrade.lib.util.swing.action.ViewAction
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.aiotrade.modules.ui.actions.AddSymbolAction
import org.aiotrade.modules.ui.GroupDescriptor
import org.aiotrade.modules.ui.windows.RealTimeWatchListTopComponent
import org.aiotrade.modules.ui.dialog.ImportSymbolDialog
import org.netbeans.api.progress.ProgressHandle
import org.netbeans.api.progress.ProgressHandleFactory
import org.netbeans.api.progress.ProgressUtils
import org.openide.ErrorManager
import org.openide.actions.CopyAction
import org.openide.actions.DeleteAction
import org.openide.actions.PasteAction
import org.openide.filesystems.FileLock
import org.openide.filesystems.FileObject
import org.openide.loaders.DataFolder
import org.openide.loaders.DataObject
import org.openide.loaders.DataObjectNotFoundException
import org.openide.nodes.AbstractNode
import org.openide.nodes.BeanNode
import org.openide.nodes.ChildFactory
import org.openide.nodes.Children
import org.openide.nodes.Node
import org.openide.nodes.NodeEvent
import org.openide.nodes.NodeListener
import org.openide.nodes.NodeMemberEvent
import org.openide.nodes.NodeReorderEvent
import org.openide.util.ImageUtilities
import org.openide.util.Lookup
import org.openide.util.actions.SystemAction
import org.openide.util.lookup.AbstractLookup
import org.openide.util.lookup.InstanceContent
import org.openide.windows.WindowManager
import scala.collection.mutable


/**
 * SymbolNode is a representation for sec serialization file "xxx.ser"  or
 * folder contains these files.
 *
 *  The tree view of Symbol and others
 *  + Symbols (config/Symbols)
 *    +- sunw (sunw.ser)
 *       +- Indicators (DescriptorGroupNode)
 *       |  +- MACD (DescriptorNode)
 *       |  |   +-opt1
 *       |  |   +-opt2
 *       |  +- ROC
 *       |     +-opt1
 *       |     +-opt2
 *       +- Drawings (DescriptorGroupNode)
 *          +- layer1
 *          |  +- line
 *          |  +- parallel
 *          |  +- gann period
 *          +- layer2
 *
 *
 *
 *
 * @author Caoyuan Deng
 */
object SymbolNodes {
  private val log = Logger.getLogger(this.getClass.getName)

  private val Bundle = ResourceBundle.getBundle("org.aiotrade.modules.ui.nodes.Bundle")

  private val DEFAUTL_SOURCE_ICON = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/symbol.gif")

  private val secToSymbolNode = new mutable.WeakHashMap[Sec, SymbolNode]

  private val folderIcon = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/market.png")
  private val symbolIcon = ImageUtilities.loadImage("org/aiotrade/modules/ui/resources/stock.png")

  val categories = List("008004", "008002", "csi300")
  /** The sectors that will be opened immediatelly */
  val sectorKeys = List("008004.N07", "008004.N14") 

  val favoriteFolderName = "Favorite"
  private var _favoriteNode: SectorNode = _
  def favoriteNode = _favoriteNode
  
  private val groups = PersistenceManager().lookupAllRegisteredServices(classOf[GroupDescriptor[Descriptor[_]]], "DescriptorGroups")

  /**
   * @NOTICE
   * When move a node from a folder to another folder, a new node could
   * be created first, then the old node is removed. so the nodeMap may
   * has been updated by the new node, and lookupContent(node) will
   * return a null since it lookup via the old node.
   * Check it here
   */

  def createSymbolXmlFile(folder: DataFolder, symbol: String): Option[FileObject] =  {
    val folderObject = folder.getPrimaryFile
    val fileName = symbol
    
    if (folderObject.getFileObject(fileName, "sec") != null) {
      log.warning("Symbol :" + symbol + " under " + folder + " has existed.")
      return None
    }

    var lock: FileLock = null
    var out: PrintStream = null
    try {
      val fo = folderObject.createData(fileName, "sec")
      lock = fo.lock
      out = new PrintStream(fo.getOutputStream(lock))

      val content = PersistenceManager().defaultContent
      content.uniSymbol = symbol
      content.lookupDescriptors(classOf[DataContract[_]]) foreach {_.srcSymbol = symbol}
      out.print(ContentPersistenceHandler.dumpContent(content))

      Option(fo)
    } catch {
      case ex: IOException => ErrorManager.getDefault.notify(ex); None
    } finally {
      /** should remember to out.close() here */
      if (out != null) out.close
      if (lock != null) lock.releaseLock
    }

  }

  private def symbolOf(fo: FileObject): String = {
    val name = fo.getName
    val extIdx = name.indexOf(".sec")
    if (extIdx > 0) {
      name.substring(0, extIdx)
    } else name
  }

  def findSymbolNode(symbol: String): Option[SymbolNode] = {
    Exchange.secOf(symbol) match {
      case Some(sec) =>
        Option(new SymbolNode(sec)) match {
          case None => None
          case some@Some(x) => secToSymbolNode(sec) = x; some
        }
      case None => None
    }
  }

  private def getSectorNode(node: Node): Option[SectorNode] = {
    node match {
      case x: SectorNode => Some(x)
      case x: SymbolNode =>
        val parent = node.getParentNode
        if (parent != null) {
          getSectorNode(parent)
        } else None
    }
  }

  def sectorNodeOf(sector: Sector): Option[SectorNode] = {
    RootSymbolsNode.getChildren.findChild(sector.category) match {
      case null => None
      case categoryNode => Option(categoryNode.getChildren.findChild(sector.key)).asInstanceOf[Option[SectorNode]]
    }
  }

  def openAllSectors {
    for {
      sectorKey <- sectorKeys
      sector <- Sector.withKey(sectorKey)
    } {
      openSector(sector)
    }
  }

  def openSector(sector: Sector) {
    val start = System.currentTimeMillis
    log.info("Opening sector: " + sector)
    
    sectorNodeOf(sector) foreach {sectorNode =>
      val watchListTc = RealTimeWatchListTopComponent.getInstance(sectorNode)
      watchListTc.requestActive

      val lastTickers = new ArrayList[Ticker]
      val secs = Sector.secsOf(sector)
      for (sec <- secs) {
        watchListTc.watch(sec)
        sec.exchange.uniSymbolToLastTradingDayTicker.get(sec.uniSymbol) foreach (lastTickers += _)
      }

      watchListTc.watchListPanel.updateByTickers(lastTickers.toArray)
    }
    
    log.info("Opened sector: " + sector + " in " + (System.currentTimeMillis - start) + " ms")
  }


  // ----- Node classes

  /**
   * The root node of SymbolNode
   * It will be 'symbols' folder in default file system, usually the 'config' dir in userdir
   * Physical folder "symbols" is defined in layer.xml
   */
  @throws(classOf[DataObjectNotFoundException])
  @throws(classOf[IntrospectionException])
  object RootSymbolsNode extends BeanNode[String]("Symbols", Children.create(RootChildFactory, false)) {
    override def getIcon(tpe: Int) = folderIcon
    override def getOpenedIcon(tpe: Int) = getIcon(0)
    override def getDisplayName = Bundle.getString("SN_title")
  }
  
  object RootChildFactory extends ChildFactory[String] {
    protected def createKeys(toPopulate: java.util.List[String]): Boolean = {
      categories foreach toPopulate.add
      true
    }
    
    override protected def createNodesForKey(key: String): Array[Node] = Array(new CategoryNode(key))
  }
  
  class CategoryNode(category: String, ic: InstanceContent
  ) extends BeanNode[String](category, Children.create(new CategoryChildFactory(category), false), new AbstractLookup(ic)) {
    setName(category)
    
    @throws(classOf[DataObjectNotFoundException])
    @throws(classOf[IntrospectionException])
    def this(category: String) = this(category, new InstanceContent)
    
    override def getIcon(tpe: Int) = folderIcon
    override def getOpenedIcon(tpe: Int) = getIcon(0)
    override def getDisplayName = category
  }
  
  class CategoryChildFactory(category: String) extends ChildFactory[Either[Sector, SecPicking]] {
    protected def createKeys(toPopulate: java.util.List[Either[Sector, SecPicking]]): Boolean = {
      if (category == "csi300") {
        toPopulate.add(Right(CSI300.buildSecPicking))
      } else {
        val sectors = Sector.sectorsOf(category)
        sectors map (Left(_)) foreach toPopulate.add
      }
      true
    }
    
    override 
    protected def createNodesForKey(key: Either[Sector, SecPicking]): Array[Node] = Array(new SectorNode(key))
  }
  
  @throws(classOf[IntrospectionException])
  class SectorNode(val sector: Either[Sector, SecPicking], ic: InstanceContent
  ) extends BeanNode[Either[Sector, SecPicking]](sector, new SectorChildren(sector), new AbstractLookup(ic)) {

    sector match {
      case Left(sector) =>
        setName(sector.key)
    
        if (sector.key == favoriteFolderName) {
          _favoriteNode = this
        }
      case Right(secPicking) =>
        setName("secPicking") // @todo
    }

    /* add additional items to the lookup */
    ic.add(SystemAction.get(classOf[AddSymbolAction]))
    ic.add(new SymbolStartWatchAction(this))
    ic.add(new SymbolStopWatchAction(this))
    ic.add(new SymbolRefreshDataAction(this))
    ic.add(new SymbolReimportDataAction(this))
    ic.add(new SymbolViewAction(this))

    this.addNodeListener(new NodeListener {
        def childrenAdded(nodeMemberEvent: NodeMemberEvent) {
          // Is this node added to a folder that has an opened corresponding watchlist tc ?
          RealTimeWatchListTopComponent.instanceOf(SectorNode.this) match {
            case Some(listTc) if listTc.isOpened =>
              nodeMemberEvent.getDelta foreach {
                case node: SymbolNode =>
                  val sec = node.sec
                  sec.subscribeTickerServer(true)
                    
                  listTc.watch(sec)
                  node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(false)
                  node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
                case _ =>
              }
            case _ =>
          }
        }

        def childrenRemoved(nodeMemberEvent: NodeMemberEvent) {
          // Is this node added to a folder that has an opened corresponding watchlist tc ?
          RealTimeWatchListTopComponent.instanceOf(SectorNode.this) match {
            case Some(listTc) if listTc.isOpened =>
              nodeMemberEvent.getDelta foreach {
                case node: SymbolNode =>
                  val sec = node.sec
                  sec.unSubscribeTickerServer
                    
                  listTc.unWatch(sec)
                  node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
                  node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(false)
                case _ =>
              }
            case _ =>
          }
        }

        def childrenReordered(nodeReorderEvent: NodeReorderEvent) {}
        def nodeDestroyed(nodeEvent: NodeEvent) {}
        def propertyChange(evt: PropertyChangeEvent) {}
      })

    /**
     * @param the sector
     */
    @throws(classOf[DataObjectNotFoundException])
    @throws(classOf[IntrospectionException])
    def this(sector: Either[Sector, SecPicking]) = this(sector, new InstanceContent)

    /** Declaring the actions that can be applied to this node */
    override 
    def getActions(popup: Boolean): Array[Action] = {
      val df = getLookup.lookup(classOf[DataFolder])
      Array(
        getLookup.lookup(classOf[SymbolStartWatchAction]),
        getLookup.lookup(classOf[SymbolStopWatchAction]),
        null,
        getLookup.lookup(classOf[AddSymbolAction]),
        new AddFolderAction(df),
        null,
        getLookup.lookup(classOf[SymbolViewAction]),
        null,
        getLookup.lookup(classOf[SymbolRefreshDataAction]),
        getLookup.lookup(classOf[SymbolReimportDataAction]),
        null,
        SystemAction.get(classOf[PasteAction]),
        SystemAction.get(classOf[DeleteAction])
      )
    }

    override 
    def getIcon(tpe: Int): Image = folderIcon

    override 
    def getOpenedIcon(tpe: Int): Image = getIcon(0)

    override 
    def getDisplayName: String = {
      sector match {
        case Left(sector) =>
          (if (Bundle.containsKey(sector.name)) Bundle.getString(sector.name) else sector.name) + " (" + getChildren.asInstanceOf[SectorChildren].secs.size + ")"
        case Right(secPicking) =>
          "secPicking"
      }
    }
    
    /** tooltip */
    override 
    def getShortDescription = {
      sector match {
        case Left(sector) => 
          sector.code + " (" + (if (Bundle.containsKey(sector.name)) Bundle.getString(sector.name) else sector.name) + ")"
        case Right(secPicking) =>
          "secPicking"
      }
    }
  }
  
  /**
   * The children of the sector node, child should be a symbol
   */
  class SectorChildren(sector: Either[Sector, SecPicking]) extends Children.Keys[Sec] {
    val secs = sector match {
      case Left(sector) => Sector.secsOf(sector)
      case Right(secPicking) => secPicking.allSecs
    }

    val keys = new java.util.TreeSet[Sec]

    override 
    protected def addNotify {
      keys.clear
      secs foreach keys.add
      setKeys(keys)
    }

    /**
     * @param a symbol
     */
    protected def createNodes(sec: Sec): Array[Node] = {
      try {
        val symbolNode = secToSymbolNode.getOrElse(sec, new SymbolNode(sec))

        // with "open" hint ?
//          fo.getAttribute("open") match {
//            case attr: java.lang.Boolean if attr.booleanValue =>
//              fo.setAttribute("open", null)
//
//              // @Error when a /** */ at there, causes syntax highlighting disappear, but /* */ is ok
//              // open it
//              SwingUtilities.invokeLater(new Runnable {
//                  def run {
//                    oneSymbolNode.getLookup.lookup(classOf[ViewAction]).execute
//                  }
//                })
//            case _ =>
//          }

        Array(symbolNode)
      } catch {
        case ioe: IOException => ErrorManager.getDefault.notify(ioe); Array()
        case exc: IntrospectionException => ErrorManager.getDefault.notify(exc); Array()
      }
    }
  }

  /** Getting the Symbol node and wrapping it in a FilterNode */
  class SymbolNode private (val sec: Sec, ic: InstanceContent
  ) extends BeanNode[Sec](sec, new SymbolChildren(sec), new AbstractLookup(ic)) {
    secToSymbolNode.put(sec, this)

    /* add additional items to the lookup */
    ic.add(new SymbolViewAction(this))
    ic.add(new SymbolReimportDataAction(this))
    ic.add(new SymbolRefreshDataAction(this))
    ic.add(new SymbolSetDataSourceAction(this))
    ic.add(new SymbolStartWatchAction(this))
    ic.add(new SymbolStopWatchAction(this))
    ic.add(new SymbolCompareToAction(this))
    ic.add(new SymbolClearDataAction(this))
    ic.add(new SymbolAddToFavoriteAction(this))

    log.fine("SymbolNode(" + sec.uniSymbol + ") created.")

    /* As the lookup needs to be constucted before Node's constructor is called,
     * it might not be obvious how to add Node or other objects into it without
     * type casting. Here is the recommended suggestion that uses public/private
     * pair of constructors:
     */
    @throws(classOf[IOException])
    @throws(classOf[IntrospectionException])
    def this(sec: Sec) = this(sec, new InstanceContent)

    override 
    def getDisplayName = sec.uniSymbol + " (" + sec.name + ")"

    override 
    def getIcon(tpe: Int): Image = symbolIcon
    
    override 
    def getOpenedIcon(tpe: Int): Image = getIcon(0)

    override 
    def canRename = false

    override 
    def getActions(context: Boolean): Array[Action] = {
      Array(
        getLookup.lookup(classOf[SymbolViewAction]),
        getLookup.lookup(classOf[SymbolRefreshDataAction]),
        getLookup.lookup(classOf[SymbolReimportDataAction]),
        null,
        getLookup.lookup(classOf[SymbolStartWatchAction]),
        getLookup.lookup(classOf[SymbolStopWatchAction]),
        null,
        getLookup.lookup(classOf[SymbolCompareToAction]),
        null,
        getLookup.lookup(classOf[SymbolSetDataSourceAction]),
        null,
        getLookup.lookup(classOf[SymbolClearDataAction]),
        null,
        SystemAction.get(classOf[CopyAction]),
        SystemAction.get(classOf[DeleteAction]),
        null,
        getLookup.lookup(classOf[SymbolAddToFavoriteAction])
      )
    }

    /**
     * The getPreferredAction() simply returns the action that should be
     * run if the user double-clicks this node
     */
    override 
    def getPreferredAction: Action = {
      getActions(true)(0)
    }

//    override protected def createNodeListener: NodeListener = {
//      //val delegate = super.createNodeListener
//      val newListener = new NodeListener {
//
//        def childrenAdded(nodeMemberEvent: NodeMemberEvent) {
//          super.childrenAdded(nodeMemberEvent)
//        }
//
//        def childrenRemoved(nodeMemberEvent: NodeMemberEvent) {
//          super.childrenRemoved(nodeMemberEvent)
//        }
//
//        def childrenReordered(nodeReorderEvent: NodeReorderEvent) {
//          super.childrenReordered(nodeReorderEvent)
//        }
//
//        def nodeDestroyed(nodeEvent: NodeEvent) {
//          /**
//           * We should check if this is a delete call, and clear data in db
//           * only when true, since it will also be called when you move a
//           * node from a folder to another.
//           * The checking is simplely like the following code:
//           * if returns null, means another copy-pasted node has been created,
//           * and owned the descriptors now, so returns null, in this case,
//           * it's a moving call. Otherwise, if returns no null, we are sure
//           * this is a real delete call other than moving.
//           * @NOTICE
//           * Here we should find via SymbolNode.this instead of nodeEvent.getNode(),
//           * which may return the delegated node.
//           */
//          if (occupiedContentOf(SymbolNode.this) != null) {
//            getLookup.lookup(classOf[SymbolClearDataAction]).perform(false)
//          }
//
//          removeNode(nodeEvent.getNode)
//          super.nodeDestroyed(nodeEvent)
//        }
//
//        def propertyChange(evt: PropertyChangeEvent) {
//          super.propertyChange(evt)
//        }
//      }
//      newListener
//    }
  }
  


  /**
   * The children wrap class
   * ------------------------------------------------------------------------
   *
   * Defining the all children of a Symbol node
   * They will be representation for descriptorGroups in this case. it's simply
   * a wrap class of DescriptorGroupNode with addNotify() and createNodes()
   * implemented
   *
   * Typical usage of Children.Keys:
   *
   *  1. Subclass.
   *  2. Decide what type your key should be.
   *  3. Implement createNodes(java.lang.Object) to create some nodes (usually exactly one) per key.
   *  4. Override Children.addNotify() to construct a set of keys and set it using setKeys(Collection). The collection may be ordered.
   *  5. Override Children.removeNotify() to just call setKeys on Collections.EMPTY_SET.
   *  6. When your model changes, call setKeys with the new set of keys. Children.Keys will be smart and calculate exactly what it needs to do effficiently.
   *  7. (Optional) if your notion of what the node for a given key changes (but the key stays the same), you can call refreshKey(java.lang.Object). Usually this is not necessary.
   */
  class SymbolChildren(sec: Sec) extends Children.Keys[GroupDescriptor[Descriptor[_]]] {

    /**
     * Called when children are first asked for nodes. Typical implementations at this time
     * calculate their node list (or keys for Children.Keys etc.).
     *
     * !Notice: call to getNodes() inside of this method will return an empty array of nodes.
     *
     * Since setKeys(childrenKeys) will copy the elements of childrenKeys, it's safe to
     * use a repeatly used bufChildrenKeys here.
     * And, to sort them in letter order, we can use a SortedSet to copy from collection.(TODO)
     */
    val keys = new java.util.HashSet[GroupDescriptor[Descriptor[_]]]()

    override 
    protected def addNotify {
      keys.clear
      /** each symbol should create new NodeInfo instance that belongs to itself */
      for (group <- groups) {
        keys add group.clone
      }
      setKeys(keys)
    }

    def createNodes(key: GroupDescriptor[Descriptor[_]]): Array[Node] = {
      try {
        val content = sec.content
        Array(new GroupNode(key, content))
      } catch {
        case ex: IntrospectionException =>
          ErrorManager.getDefault.notify(ErrorManager.INFORMATIONAL, ex)
          /** Should never happen - no reason for it to fail above */
          Array(
            new AbstractNode(Children.LEAF) {
              override def getHtmlDisplayName = "<font color='red'>" + ex.getMessage() + "</font>"
            }
          )
      }
    }
  }
  

  // ----- node actions

  class SymbolViewAction(node: Node) extends ViewAction {
    putValue(Action.NAME, Bundle.getString("AC_view"))

    def execute {
      node match {
        case x: SectorNode => 
          for (child <- node.getChildren.getNodes) {
            child.getLookup.lookup(classOf[SymbolViewAction]).execute
          }
        case x: SymbolNode =>
          val sec = x.sec
          var mayNeedsReload = false

          val standalone = getValue(AnalysisChartTopComponent.STANDALONE) match {
            case null => false
            case x => x.asInstanceOf[Boolean]
          }
          
          log.info("Open standalone AnalysisChartTopComponent: " + standalone)
          val analysisTc = AnalysisChartTopComponent(sec, standalone)
          analysisTc.setActivatedNodes(Array(node))
          /**
           * !NOTICE
           * close a TopComponent doen's mean this TopComponent is null, it still
           * exsit, just invsible
           */
          /** if TopComponent of this stock has been shown before, should reload quote data, why */
          /* if (mayNeedsReload) {
           sec.clearSer(quoteContract.freq)
           } */

          if (!analysisTc.isOpened) {
            analysisTc.open
          }

          analysisTc.requestActive
      }
    }
  }

  class SymbolStartWatchAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_start_watching"))
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/resources/startWatch.gif")

    def execute {
      val sectorNode = getSectorNode(node) getOrElse (return)
      val handle = ProgressHandleFactory.createHandle(Bundle.getString("MSG_init_symbols") + " " + node.getDisplayName + " ...")
      ProgressUtils.showProgressDialogAndRun(new Runnable {
          def run {
            handle.finish
            node.getLookup.lookup(classOf[SymbolStopWatchAction]).setEnabled(true)
            SymbolStartWatchAction.this.setEnabled(false)
            sectorNode.sector match {
              case Left(sector) => openSector(sector)
              case Right(secPicking) => // @todo
            }
          }
        }, handle, false)
    }
  }

  class SymbolStopWatchAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_stop_watching"))
    putValue(Action.SMALL_ICON, "org/aiotrade/modules/ui/resources/stopWatch.gif")

    if (node.getLookup.lookup(classOf[DataFolder]) != null) {
      this.setEnabled(true)
    } else {
      this.setEnabled(false)
    }

    def execute {
      node.getLookup.lookup(classOf[SymbolStartWatchAction]).setEnabled(true)
      this.setEnabled(false)

      node match {
        case x: SectorNode =>
          for (child <- node.getChildren.getNodes) {
            child.getLookup.lookup(classOf[SymbolStopWatchAction]).execute
          }
        case x: SymbolNode =>
          val sec = x.sec
          val content = sec.content
          sec.unSubscribeTickerServer

//      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
//        RealTimeWatchListTopComponent.instanceRefs.head.get.unWatch(sec)
//      }
//
//      if (!RealTimeWatchListTopComponent.instanceRefs.isEmpty) {
//        RealTimeChartsTopComponent.instanceRefs.head.get.unWatch(sec)
//      }
//
//      RealTimeBoardTopComponent(content) foreach {rtBoardWin =>
//        rtBoardWin.unWatch
//      }          
      }


    }
  }

  /**
   * We We shouldn't implement deleting data in db in NodeListener#nodeDestroyed(NodeEvent),
   * since  it will be called also when you move a node from a folder to another
   * folder. So we need a standalone action here.
   *
   * @TODO
   */
  class SymbolClearDataAction(node: SymbolNode) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_clear_data"))

    def perform(shouldConfirm: Boolean) {
      /**
       * don't get descriptors from getLookup.lookup(..), becuase
       * if node destroy is invoked by parent node, such as folder,
       * the lookup content may has been destroyed before node destroyed.
       */
      val content = node.sec.content
      val confirm = if (shouldConfirm) {
        JOptionPane.showConfirmDialog(WindowManager.getDefault.getMainWindow(),
                                      "Are you sure you want to clear data of : " + content.uniSymbol + " ?",
                                      "Clearing data ...",
                                      JOptionPane.YES_NO_OPTION)
      } else JOptionPane.YES_OPTION

      if (confirm == JOptionPane.YES_OPTION) {
        val symbol = content.uniSymbol
        /** drop tables in database */
        PersistenceManager().dropAllQuoteTables(symbol)
      }
    }

    def execute {
      perform(true)
    }
  }

  class SymbolReimportDataAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_reimport_data"))

    def execute {
      node match {
        case x: SectorNode =>
          for (child <- node.getChildren.getNodes) {
            child.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
          }
        case x: SymbolNode =>
          val sec = x.sec
          val content = sec.content
          
          val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get
          val fromTime = quoteContract.fromTime
          val freq = quoteContract.freq
          PersistenceManager().deleteQuotes(content.uniSymbol, freq, fromTime, Long.MaxValue)

          /**
           * @TODO
           * need more works, the clear(long) in default implement of Ser doesn't work good!
           */
          sec.resetSers
          val ser = sec.serOf(freq).get

          node.getLookup.lookup(classOf[ViewAction]).execute
      }
    }
  }

  class SymbolRefreshDataAction(node: Node) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_refresh_data"))

    def execute {
      node match {
        case x: SectorNode =>
          for (child <- node.getChildren.getNodes) {
            child.getLookup.lookup(classOf[SymbolRefreshDataAction]).execute
          }
        case x: SymbolNode =>
          val sec = x.sec
          val content = sec.content
          val quoteContract = content.lookupActiveDescriptor(classOf[QuoteContract]).get

          sec.resetSers
          val ser = sec.serOf(quoteContract.freq).get

          node.getLookup.lookup(classOf[ViewAction]).execute      
      }
    }
  }

  class SymbolSetDataSourceAction(node: SymbolNode) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_set_data_source"))

    def execute {
      val content = node.sec.content

      val pane = new ImportSymbolDialog(
        WindowManager.getDefault.getMainWindow,
        content.lookupActiveDescriptor(classOf[QuoteContract]).getOrElse(null),
        false)
      if (pane.showDialog != JOptionPane.OK_OPTION) {
        return
      }

      content.lookupAction(classOf[SaveAction]) foreach {_.execute}
      node.getLookup.lookup(classOf[SymbolReimportDataAction]).execute
    }
  }

  class SymbolCompareToAction(node: SymbolNode) extends GeneralAction {
    putValue(Action.NAME, Bundle.getString("AC_compare_to_current"))

    def execute {
      val sec = node.sec
      val content = sec.content

      val analysisTc = AnalysisChartTopComponent.selected getOrElse {return}
      val freq = analysisTc.freq
      val serToCompare = sec.serOf(freq).get
      if (!serToCompare.isLoaded) {
        sec.loadSer(serToCompare)
      }

      val viewContainer = analysisTc.viewContainer

      val baseSer = viewContainer.controller.baseSer
      val quoteCompareIndicator = new QuoteCompareIndicator(baseSer)
      quoteCompareIndicator.shortName = sec.uniSymbol
      quoteCompareIndicator.serToBeCompared = serToCompare
      quoteCompareIndicator.computeFrom(0)

      viewContainer.controller.scrollReferCursorToLeftSide
      viewContainer.masterView.asInstanceOf[AnalysisChartView].addQuoteCompareChart(quoteCompareIndicator)

      analysisTc.requestActive
    }

  }

  class SymbolAddToFavoriteAction(node: Node) extends AddToFavoriteAction {
    putValue(Action.NAME, Bundle.getString("AC_add_to_favorite"))

    def execute {
      val dobj = node.getLookup.lookup(classOf[DataObject])
      val favFolder = favoriteNode.getLookup.lookup(classOf[DataFolder])
      if (!favFolder.getChildren.exists(_.getName == node.getName)) {
        dobj.createShadow(favFolder)
      }

      val content = node.getLookup.lookup(classOf[Content])
      watchSymbolsOfSector(favoriteNode, Array(content))
    }
  }

  /** Creating an action for adding a folder to organize stocks into groups */
  class AddFolderAction(folder: DataFolder) extends AbstractAction {
    putValue(Action.NAME, Bundle.getString("AC_add_folder"))

    def actionPerformed(ae: ActionEvent) {
      var floderName = JOptionPane.showInputDialog(
        WindowManager.getDefault.getMainWindow,
        Bundle.getString("SN_askfolder_msg"),
        Bundle.getString("AC_add_folder"),
        JOptionPane.OK_CANCEL_OPTION
      )

      if (floderName == null) {
        return
      }

      floderName = floderName.trim

      try {
        DataFolder.create(folder, floderName)
      } catch {
        case ex: IOException => ErrorManager.getDefault().notify(ex)
      }
    }
  }

  private def watchSymbolsOfSector(sectorNode: SectorNode, symbolContents: Array[Content]) {
    val watchListTc = RealTimeWatchListTopComponent.getInstance(sectorNode)
    watchListTc.requestActive

    val lastTickers = new ArrayList[Ticker]
    var i = 0
    while (i < symbolContents.length) {
      val content = symbolContents(i)
      val uniSymbol = content.uniSymbol
      Exchange.secOf(uniSymbol) match {
        case Some(sec) =>
          watchListTc.watch(sec)
          sec.exchange.uniSymbolToLastTradingDayTicker.get(uniSymbol) foreach (lastTickers += _)
        case None =>
      }
      
      i += 1
    }

    watchListTc.watchListPanel.updateByTickers(lastTickers.toArray)
  }

}

abstract class AddToFavoriteAction extends GeneralAction
