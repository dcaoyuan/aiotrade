package org.aiotrade.platform.test

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.ResourceBundle
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.SwingConstants
import javax.swing.UIManager
import org.aiotrade.lib.charting.chart.QuoteChart
import org.aiotrade.lib.charting.laf.CityLights
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.charting.view.ChartingController
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.util.swing.plaf.AIOTabbedPaneUI
import org.aiotrade.lib.view.securities.AnalysisChartViewContainer
import org.aiotrade.lib.view.securities.AnalysisChartView
import org.aiotrade.lib.view.securities.RealTimeBoardPanel
import org.aiotrade.lib.view.securities.RealTimeChartViewContainer
import org.aiotrade.lib.indicator.VOLIndicator
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.dataserver.yahoo.YahooQuoteServer
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.indicator.basic.MAIndicator
import org.aiotrade.lib.indicator.basic.RSIIndicator
import scala.collection.mutable.HashSet


/**
 *
 * @author Caoyuan Deng
 */
object Util {
  val EAST_REGIONS= Array(Locale.CHINA, Locale.TAIWAN, Locale.JAPAN, Locale.KOREA)

  val BUNDLE = ResourceBundle.getBundle("org.aiotrade.platform.test.Bundle")
  val viewContainers = new HashSet[Reference[AnalysisChartViewContainer]]
  // 544 width is the proper size to fit 2 pixes 241 bar (one trading day's 1min)
  private val MAIN_PANE_WIDTH = 544
}

class Util {
  import Util._

  private var tickerServer: TickerServer = _
  private var sec: Sec = _

  /***
   * @param para parameters defined
   * @param data in previsous format. current only dailly supported.
   * @return a image
   */
  def init(
    pane: Container,
    width: Int, height: Int,
    symbol: String,
    category: String,
    sname: String,
    quoteServer: Class[_],
    tickerServer: Class[_]
  ): Iterable[Reference[AnalysisChartViewContainer]] = {

    val leftPaneWidth = 240
    
    val mainWidth = width - leftPaneWidth

    val locale = Locale.getDefault

    val laf = new CityLights
    laf.setAntiAlias(false)
    EAST_REGIONS find (_.getCountry.equals(locale.getCountry)) foreach {x =>
      laf.setPositiveNegativeColorReversed(true)
    }
    LookFeel() = laf

    setUIStyle

    sec = Exchange.secOf(symbol).get
    
    val content = sec.content
    val dailyQuoteContract = createQuoteContract(symbol, category, sname, TFreq.DAILY, false, quoteServer)
    val supportOneMin = dailyQuoteContract.isFreqSupported(TFreq.ONE_MIN)
    val oneMinQuoteContract = createQuoteContract(symbol, category, sname, TFreq.ONE_MIN, supportOneMin, quoteServer)
    val tickerContract =
      if (tickerServer ne null) {
        createTickerContract(symbol, category, sname, TFreq.ONE_MIN, tickerServer)
      } else null

    content.addDescriptor(dailyQuoteContract)
    content.addDescriptor(oneMinQuoteContract)
    
    sec.tickerContract = tickerContract
    val exchange =
      if (quoteServer.getName == YahooQuoteServer.getClass.getName) {
        YahooQuoteServer.exchangeOf(symbol)
      } else {
        null//ApcQuoteServer.GetMarket(symbol)
      }
    sec.exchange = exchange

    createAndAddIndicatorDescritors(content, TFreq.DAILY)
    loadSer(sec, TFreq.DAILY)
    createAndAddIndicatorDescritors(content, TFreq.ONE_MIN)
    loadSer(sec, TFreq.ONE_MIN)

    // --- other freqs:
//    val oneMinViewContainer = createViewContainer(
//      sec.serOf(freqOneMin).getOrElse(null),
//      rtContent,
//      symbol,
//      QuoteChart.Type.Line,
//      pane)
//
//    oneMinViewContainer.setPreferredSize(new Dimension(mainWidth, height))
//    viewContainers.add(new WeakReference[AnalysisChartViewContainer](oneMinViewContainer))

    val dailyViewContainer = createViewContainer(
      sec.serOf(TFreq.DAILY).getOrElse(null),
      symbol,
      QuoteChart.Type.Candle,
      pane)
    //dailyViewContainer.setPreferredSize(new Dimension(mainWidth, height))
    viewContainers.add(new WeakReference[AnalysisChartViewContainer](dailyViewContainer))

    //val rtViewContainer = createRealTimeViewContainer(sec, rtContent, pane)

    try {
      pane.setLayout(new BorderLayout)
      pane.setLayout(new BorderLayout)


//      val tabbedPane = createTabbedPane
//      tabbedPane.setFocusable(false)

      //tabbedPane.setBorder(new AIOScrollPaneStyleBorder(LookFeel()().borderColor));

      //splitPane.add(JSplitPane.LEFT, tabbedPane)

//      val rtPanel = new JPanel(new BorderLayout)
//      rtPanel.add(BorderLayout.CENTER, rtViewContainer)
//
//      val oneMinPanel = new JPanel(new BorderLayout)
//      oneMinPanel.add(BorderLayout.CENTER, oneMinViewContainer)
//
//      tabbedPane.addTab(BUNDLE.getString("daily"), dailyPanel)
//      tabbedPane.addTab(BUNDLE.getString("realTime"), rtPanel)
//      tabbedPane.addTab(BUNDLE.getString("oneMin"), oneMinPanel)
//      
//      val rtBoardBoxV = Box.createVerticalBox
//      rtBoardBoxV.add(Box.createVerticalStrut(22)) // use to align top of left pane's content pane
//      rtBoardBoxV.add(rtBoard)
//
//      val rtBoardBoxH = Box.createHorizontalBox
//      rtBoardBoxH.add(Box.createHorizontalStrut(5))
//      rtBoardBoxH.add(rtBoardBoxV)

      val dailyPanel = new JPanel(new BorderLayout)
      dailyPanel.add(BorderLayout.CENTER, dailyViewContainer)

      val rtBoard = RealTimeBoardPanel(sec)
      rtBoard.setPreferredSize(new Dimension(leftPaneWidth, height))

      val splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
      splitPane.setFocusable(false)
      splitPane.setBorder(BorderFactory.createEmptyBorder)
      splitPane.setOneTouchExpandable(true)
      splitPane.setDividerSize(3)

      // setting the resize weight to 1.0 makes the right or bottom component's size remain fixed
      splitPane.setResizeWeight(1.0)

      splitPane.add(JSplitPane.LEFT, dailyPanel)
      splitPane.add(JSplitPane.RIGHT, rtBoard)
      //pane.add(BorderLayout.NORTH, createToolBar(width));
      pane.add(BorderLayout.CENTER, splitPane)

      watchRealTime(content, rtBoard)

      //container.getController().setCursorCrossLineVisible(showLastClose(apcpara));
    } catch {case ex: Exception => ex.printStackTrace}

    viewContainers
  }

//    private JToolBar createToolBar(int width) {
//        JToolBar toolBar = new JToolBar();
//        toolBar.add(new ZoomInAction());
//        toolBar.add(new ZoomOutAction());
//
//        toolBar.setPreferredSize(new Dimension(width, 18));
//        for (Component c : toolBar.getComponents()) {
//            c.setFocusable(false);
//        }
//
//        return toolBar;
//    }

  private def createQuoteContract(symbol: String, category: String, sname: String, freq: TFreq, isRefreshable: Boolean, server: Class[_]): QuoteContract = {
    val dataContract = new QuoteContract

    dataContract.active = true
    dataContract.serviceClassName = server.getName

    dataContract.srcSymbol = symbol

    dataContract.datePattern = Some("yyyy-MM-dd")

    dataContract.freq = freq

    dataContract.isRefreshable = isRefreshable
    dataContract.refreshInterval = 5

    dataContract
  }

  private def createTickerContract(symbol: String, category: String, sname: String, freq: TFreq, server: Class[_]): TickerContract = {
    val dataContract = new TickerContract

    dataContract.active = true
    dataContract.serviceClassName = server.getName

    dataContract.srcSymbol = symbol

    dataContract.datePattern = Some("yyyy-MM-dd-HH-mm-ss")
    dataContract.freq = freq
    dataContract.isRefreshable = true
    dataContract.refreshInterval = 5

    dataContract
  }

  private def createAndAddIndicatorDescritors(content: Content, freq: TFreq) {
    content.addDescriptor(createIndicatorDescriptor(classOf[MAIndicator],  freq))
    content.addDescriptor(createIndicatorDescriptor(classOf[VOLIndicator], freq))
    content.addDescriptor(createIndicatorDescriptor(classOf[RSIIndicator], freq))
  }

//    private static final Content createRealTimeContent(String symbol, Frequency freq, Class quoteServer) {
//        
//
//        content.addDescriptor(createIndicatorDescriptor(VOLIndicator.class, freq));
//
//        QuoteContract quoteContract = createQuoteContract(symbol, freq, quoteServer);
//        TickerContract tickerContract = createTickerContract(symbol, ApcTickerServer.class);
//        content.addDescriptor(quoteContract);
//        content.addDescriptor(tickerContract);
//
//        return content;
//    }
  private def loadSer(sec: Sec, freq: TFreq) {
    var mayNeedsReload = false
    if (sec == null) {
      return
    } else {
      mayNeedsReload = true
    }

    if (mayNeedsReload) {
      sec.resetSers
    }
    val ser = sec.serOf(freq).get

    if (!ser.isLoaded && !ser.isInLoading) {
      sec.loadSer(ser)
    }
		
  }

  private def createIndicatorDescriptor(clazz: Class[_], freq: TFreq): IndicatorDescriptor = {
    val indicator = new IndicatorDescriptor
    indicator.active = true
    indicator.serviceClassName = clazz.getName
    indicator.freq = freq
    indicator
  }

  private def createViewContainer(
    ser: QuoteSer,
    atitle: String,
    tpe: QuoteChart.Type,
    parent: Component
  ): AnalysisChartViewContainer = {

    var title = atitle

    if (!ser.isLoaded) sec.loadSer(ser)
    val controller = ChartingController(sec, ser)
    val viewContainer = controller.createChartViewContainer(classOf[AnalysisChartViewContainer], parent)

    if (title == null) {
      title = ser.freq.name
    }
    title = " " + title + " "

    viewContainer.controller.isCursorCrossLineVisible = true
    viewContainer.controller.isOnCalendarMode = false
    val masterView = viewContainer.masterView.asInstanceOf[AnalysisChartView]
    masterView.switchQuoteChartType(tpe)
    masterView.xControlPane.setVisible(true)
    masterView.yControlPane.setVisible(true)

    /** inject popup menu from this TopComponent */
    //viewContainer.setComponentPopupMenu(popupMenuForViewContainer);
    viewContainer
  }

  private def createRealTimeViewContainer(sec: Sec, parent: Component): RealTimeChartViewContainer = {
    var baseSer = sec.serOf(TFreq.ONE_MIN).get
    val controller = ChartingController(sec, baseSer)
    val viewContainer = controller.createChartViewContainer(classOf[RealTimeChartViewContainer], parent)
    viewContainer
  }

  private def createTabbedPane: JTabbedPane = {
    val tabbedPane = new JTabbedPane(SwingConstants.TOP)
    tabbedPane.setFocusable(false)
    return tabbedPane
//        tabbedPane.addChangeListener(new ChangeListener() {
//            private Color selectedColor = new Color(177, 193, 209);
//
//            public void stateChanged(ChangeEvent e) {
//                JTabbedPane tp = (JTabbedPane)e.getSource();
//
//                for (int i = 0; i < tp.getTabCount(); i++) {
//                    tp.setBackgroundAt(i, null);
//                }
//                int idx = tp.getSelectedIndex();
//                tp.setBackgroundAt(idx, selectedColor);
//
//                //updateToolbar();
//
//                if (tp.getSelectedComponent() instanceof AnalysisChartViewContainer) {
//                    AnalysisChartViewContainer viewContainer = (AnalysisChartViewContainer)tp.getSelectedComponent();
//                    MasterSer masterSer = viewContainer.getController().getMasterSer();
//
//                    /** update the descriptorGourp node's children according to selected viewContainer's time frequency: */
//
//                    Node secNode = NetBeansPersistenceManager.getOccupantNode(content);
//                    assert secNode != null : "There should be at least one created node bound with descriptors here, as view has been opened!";
//                    for (Node groupNode : secNode.getChildren().getNodes()) {
//                        ((GroupNode)groupNode).setTimeFrequency(masterSer.getFreq());
//                    }
//
//                    /** update the supportedFreqsComboBox */
//                    setSelectedFreqItem(masterSer.getFreq());
//                }
//            }
//        });
//
  }

  private def setUIStyle {
//        UIDefaults defs = UIManager.getDefaults();
//        Enumeration keys = defs.keys();
//        while (keys.hasMoreElements()) {
//            Object key = keys.nextElement();
//            if (key.toString().startsWith("TabbedPane")) {
//                System.out.println(key);
//            }
//        }

    UIManager.put("TabbedPaneUI", classOf[AIOTabbedPaneUI].getName)
    /** get rid of the ugly border of JTabbedPane: */
//        Insets oldInsets = UIManager.getInsets("TabbedPane.contentBorderInsets");
    /*- set top insets as 1 for TOP placement if you want:
     UIManager.put("TabbedPane.contentBorderInsets", new Insets(1, 0, 0, 0));
     */
    //UIManager.getColor("TabbedPane.tabAreaBackground");
    UIManager.put("TabbedPane.selected", LookFeel().backgroundColor)
    UIManager.put("TabbedPane.selectHighlight", LookFeel().backgroundColor)

    UIManager.put("TabbedPane.unselectedBackground", Color.WHITE)
    UIManager.put("TabbedPane.selectedBorderColor", LookFeel().borderColor)
//        UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 1));
//        UIManager.put("TabbedPane.contentBorderInsets", oldInsets);
//        UIManager.put("TabbedPane.font", new Font("Dialog", Font.PLAIN, 11));
//        UIManager.put("TabbedPane.foreground", LookFeel()().borderColor);
//        UIManager.put("TabbedPane.background", Color.WHITE);
//        UIManager.put("TabbedPane.shadow", Color.GRAY);
//        UIManager.put("TabbedPane.darkShadow", Color.GRAY);
  }

  def watchRealTime(content: Content, rtBoard: RealTimeBoardPanel) {
    sec.subscribeTickerServer()
    rtBoard.watch
  }

  @throws(classOf[Exception])
  def paintToImage(
    container: AnalysisChartViewContainer,
    controller: ChartingController,
    begTime: Long,
    endTime: Long,
    width: Int,
    height: Int,
    fm: JFrame
  ): BufferedImage = {

    container.setPreferredSize(new Dimension(width, height))

    container.setBounds(0, 0, width, height)

    fm.getContentPane.add(container)
    fm.pack

    val begPos = controller.baseSer.rowOfTime(begTime)
    val endPos = controller.baseSer.rowOfTime(endTime)
    val nBars = endPos - begPos + 1

    // wViewport should minus AxisYPane's width
    val wViewPort = width - container.masterView.axisYPane.getWidth
    controller.setWBarByNBars(wViewPort, nBars)


    // backup:
    val backupRightCursorPos = controller.rightSideRow
    val backupReferCursorPos = controller.referCursorRow

    controller.setCursorByRow(backupReferCursorPos, endPos - 1, true)

    container.paintToImage

    container.paintToImage.asInstanceOf[BufferedImage]
    /** restore: */
    //controller.setCursorByRow(backupReferCursorPos, backupRightCursorPos);
  }

  def releaseAll {
    // Since ticker server is singleton, will be reused in browser, should unSubscribe it to get snapTicker etc to be reset
    sec.unSubscribeTickerServer
  }
}
