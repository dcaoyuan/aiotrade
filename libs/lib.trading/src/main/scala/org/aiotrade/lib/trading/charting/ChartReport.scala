package org.aiotrade.lib.trading.charting

import java.awt.BorderLayout
import java.awt.Color
import java.awt.Container
import java.awt.event.ActionListener
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Random
import java.util.concurrent.CountDownLatch
import java.util.logging.Level
import java.util.logging.Logger
import javafx.application.Platform
import javafx.collections.FXCollections
import javafx.embed.swing.JFXPanel
import javafx.geometry.BoundingBox
import javafx.geometry.Bounds
import javafx.scene.Scene
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.LineChart
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.layout.VBox
import javax.imageio.ImageIO
import javax.swing.JFrame
import javax.swing.JOptionPane
import javax.swing.Timer
import org.aiotrade.lib.trading.Param
import org.aiotrade.lib.trading.ReportData
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable
import scala.collection.JavaConversions._
import scala.concurrent.SyncVar

/**
 * 
 * @author Caoyuan Deng
 */
class ChartReport(
  imageFileDirStr: String, isAutoRanging: Boolean = true, 
  upperBound: Int = 0, lowerBound: Int = 1000, 
  width: Int = 1200, height: Int = 900
) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val cssUrl = Thread.currentThread.getContextClassLoader.getResource("chart.css").toExternalForm
  private val imageFileDir = {
    try {
      val dir = new File(if (imageFileDirStr != null) imageFileDirStr else ".")
      if (!dir.exists) {
        dir.mkdirs
      }
      dir
    } catch {
      case ex: Throwable => throw(ex)
    }
  }
  private val fileDf = new SimpleDateFormat("yyMMddHHmm")

  private var imageSavingLatch: CountDownLatch = _
  private var chartTabs = List[ChartTab]()
  
  private val frame = new JFrame()
  private val jfxPanel = new JFXPanel()
  private val tabPane = new TabPane()
  
  var waitTimeBeforeSaving = 2000
  
  initAndShowGUI
  
  private def runInFXThread(block: => Unit) {
    Platform.runLater(new Runnable {
        def run = block // @Note don't write to: def run {block}
      })
  }
  
  private def initAndShowGUI {
    // should put this code outside of runInFXThread, otherwise will cause: Toolkit not initialized
    frame.add(jfxPanel, BorderLayout.CENTER)
    
    runInFXThread {
      val scene = new Scene(tabPane, width, height)
      scene.getStylesheets.add(cssUrl)
      jfxPanel.setScene(scene)

      frame.pack
      frame.setVisible(true)
    }
  }
  
  /**
   * @param for each param in params, will create a new tabbed pane in main frame.
   */
  def roundStarted(params: List[Param]) {
    if (imageSavingLatch != null) {
      try {
        imageSavingLatch.await
      } catch {
        case e: InterruptedException => e.printStackTrace
      }
    }
    chartTabs = params map (new ChartTab(_))
    Thread.sleep(1000) // wait for chartTab inited in FX thread
  }
  
  def roundFinished {
    imageSavingLatch = new CountDownLatch(chartTabs.length)
    Thread.sleep(waitTimeBeforeSaving) // wait for chart painted in FX thread
    chartTabs foreach (_.saveImage)
  }
  
  private class ChartTab(param: Param) extends Reactor {
    private val idToSeries = new mutable.HashMap[String, XYChart.Series[String, Number]]()
    private var valueChart: LineChart[String, Number] = _
    private var referChart: LineChart[String, Number] = _

    private val df = new SimpleDateFormat("yy.MM.dd")

    private val tab = new Tab()
    private var root: VBox = _
    
    initAndShowGUI
  
    reactions += {
      case data: ReportData => updateData(data)
    }
    listenTo(param)
  
    private def initAndShowGUI {
      runInFXThread {
        root = new VBox()

        val xAxis = new CategoryAxis()
        xAxis.setLabel("Time")
        val yAxis = new NumberAxis()
        yAxis.setAutoRanging(isAutoRanging)
        yAxis.setUpperBound(upperBound)
        yAxis.setLowerBound(lowerBound)
      
        valueChart = new LineChart[String, Number](xAxis, yAxis)
        valueChart.setTitle("Equity Monitoring - " + param.titleDescription)
        valueChart.setCreateSymbols(false)
        valueChart.setLegendVisible(false)
        valueChart.setPrefHeight(0.9 * height)

        val xAxisRef = new CategoryAxis()
        xAxisRef.setLabel("Time")
        val yAxisRef = new NumberAxis()
      
        referChart = new LineChart[String, Number](xAxisRef, yAxisRef)
        referChart.setCreateSymbols(false)
        referChart.setLegendVisible(false)
      
        root.getChildren.add(valueChart)
        root.getChildren.add(referChart)
        tab.setContent(root)
        tab.setText(param.titleDescription)

        tabPane.getTabs.add(tab)
      }
    }
  
    private def resetData {
      runInFXThread {
        idToSeries.clear
        valueChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
        referChart.setData(FXCollections.observableArrayList[XYChart.Series[String, Number]]())
      }
    }
  
    private def updateData(data: ReportData) {
      // should run in FX application thread
      runInFXThread {
        val serieId = data.name + data.id
        val serieName = data.name + "-" + data.id
        val chart = if (data.name.contains("Refer")) referChart else valueChart
        val series = idToSeries.getOrElseUpdate(serieId, createSeries(serieName, chart))
        series.getData.add(new XYChart.Data(df.format(new Date(data.time)), data.value))

        val styleSelector = "series-" + serieId
        if (!series.getNode.getStyleClass.contains(styleSelector)) {
          series.getNode.getStyleClass.add(styleSelector)
        }
        
        if (data.color != null) {
          val nodes = chart.lookupAll("." + styleSelector)
          nodes foreach (_.setStyle("-fx-stroke: #" + toHexColor(data.color) +  "; "))
        }
      }
    }
    
    /**
     * Should mask first 2 digits of "XX"-contribution from the Alpha-component (which is not always the case)
     */
    private def toHexColor(color: Color) = Integer.toHexString((color.getRGB & 0xffffff) | 0x1000000).substring(1)
    
    private def createSeries(name: String, chart: XYChart[String, Number]): XYChart.Series[String, Number] = {
      val series = new XYChart.Series[String, Number]()
      series.setName(name)
      chart.getData.add(series)
      series
    }
  
    /**
     * @return SyncVar[ChartTab](this)
     */
    def saveImage {
      val done = new SyncVar[ChartTab]()
      val file = new File(imageFileDir, fileDf.format(new Date(System.currentTimeMillis)) + "_" + param.shortDescription + ".png")
      tabPane.getSelectionModel.select(tab)
      
      // wait for sometime after select this tab via timer
      var timer: Timer = null
      timer = new Timer(1500, new ActionListener {
          def actionPerformed(e: java.awt.event.ActionEvent) {
            ChartReport.saveImage(jfxPanel, file)
            tabPane.getTabs.remove(tab)
            done.put(ChartTab.this)
            imageSavingLatch.countDown
            timer.stop
          }
        }
      )
      timer.start
      
      //done.take
    }
  }
  
}


object ChartReport {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private def saveImage(container: Container, file: File)  {
    try {
      val name = file.getName
      val ext = name.lastIndexOf(".") match {
        case dot if dot >= 0 => name.substring(dot + 1)
        case _ => "jpg"
      }
      val boundbox = new BoundingBox(0, 0, container.getWidth, container.getHeight)
      ImageIO.write(toBufferedImage(container, boundbox), ext, file)
      println("=== Image saved ===")
    } catch {
      case ex: Throwable =>
        log.log(Level.WARNING, ex.getMessage, ex)
        JOptionPane.showMessageDialog(null, "The image couldn't be saved", "Error", JOptionPane.ERROR_MESSAGE)
    }
  }
  
  // http://docs.oracle.com/javafx/2/api/javafx/scene/doc-files/cssref.html#typecolor
  val aliceblue = Color.decode("#f0f8ff")	
  val antiquewhite = Color.decode("#faebd7")
  val aqua = Color.decode("#00ffff")
  val aquamarine = Color.decode("#7fffd4")
  val azure = Color.decode("#f0ffff")
  val beige = Color.decode("#f5f5dc")
  val bisque = Color.decode("#ffe4c4")
  val black = Color.decode("#000000")
  val blanchedalmond = Color.decode("#ffebcd")
  val blue = Color.decode("#0000ff")
  val blueviolet = Color.decode("#8a2be2")
  val brown = Color.decode("#a52a2a")
  val burlywood = Color.decode("#deb887")
  val cadetblue = Color.decode("#5f9ea0")
  val chartreuse = Color.decode("#7fff00")
  val chocolate = Color.decode("#d2691e")
  val coral = Color.decode("#ff7f50")
  val cornflowerblue = Color.decode("#6495ed")
  val cornsilk = Color.decode("#fff8dc")
  val crimson = Color.decode("#dc143c")
  val cyan = Color.decode("#00ffff")
  val darkblue = Color.decode("#00008b")
  val darkcyan = Color.decode("#008b8b")
  val darkgoldenrod = Color.decode("#b8860b")
  val darkgray = Color.decode("#a9a9a9")
  val darkgreen = Color.decode("#006400")
  val darkgrey = Color.decode("#a9a9a9")
  val darkkhaki = Color.decode("#bdb76b")
  val darkmagenta = Color.decode("#8b008b")
  val darkolivegreen = Color.decode("#556b2f")
  val darkorange = Color.decode("#ff8c00")
  val darkorchid = Color.decode("#9932cc")
  val darkred = Color.decode("#8b0000")
  val darksalmon = Color.decode("#e9967a")
  val darkseagreen = Color.decode("#8fbc8f")
  val darkslateblue = Color.decode("#483d8b")
  val darkslategray = Color.decode("#2f4f4f")
  val darkslategrey = Color.decode("#2f4f4f")
  val darkturquoise = Color.decode("#00ced1")
  val darkviolet = Color.decode("#9400d3")
  val deeppink = Color.decode("#ff1493")
  val deepskyblue = Color.decode("#00bfff")
  val dimgray = Color.decode("#696969")
  val dimgrey = Color.decode("#696969")
  val dodgerblue = Color.decode("#1e90ff")
  val firebrick = Color.decode("#b22222")
  val floralwhite = Color.decode("#fffaf0")
  val forestgreen = Color.decode("#228b22")
  val fuchsia = Color.decode("#ff00ff")
  val gainsboro = Color.decode("#dcdcdc")
  val ghostwhite = Color.decode("#f8f8ff")
  val gold = Color.decode("#ffd700")
  val goldenrod = Color.decode("#daa520")
  val gray = Color.decode("#808080")
  val green = Color.decode("#008000")
  val greenyellow = Color.decode("#adff2f")
  val grey = Color.decode("#808080")
  val honeydew = Color.decode("#f0fff0")
  val hotpink = Color.decode("#ff69b4")
  val indianred = Color.decode("#cd5c5c")
  val indigo = Color.decode("#4b0082")
  val ivory = Color.decode("#fffff0")
  val khaki = Color.decode("#f0e68c")
  val lavender = Color.decode("#e6e6fa")
  val lavenderblush = Color.decode("#fff0f5")
  val lawngreen = Color.decode("#7cfc00")
  val lemonchiffon = Color.decode("#fffacd")
  val lightblue = Color.decode("#add8e6")
  val lightcoral = Color.decode("#f08080")
  val lightcyan = Color.decode("#e0ffff")
  val lightgoldenrodyellow = Color.decode("#fafad2")
  val lightgray = Color.decode("#d3d3d3")
  val lightgreen = Color.decode("#90ee90")
  val lightgrey = Color.decode("#d3d3d3")
  val lightpink = Color.decode("#ffb6c1")
  val lightsalmon = Color.decode("#ffa07a")
  val lightseagreen = Color.decode("#20b2aa")
  val lightskyblue = Color.decode("#87cefa")
  val lightslategrey = Color.decode("#778899")
  val lightsteelblue = Color.decode("#b0c4de")
  val lightyellow = Color.decode("#ffffe0")
  val lime = Color.decode("#00ff00")
  val limegreen = Color.decode("#32cd32")
  val linen = Color.decode("#faf0e6")
  val magenta = Color.decode("#ff00ff")
  val maroon = Color.decode("#800000")
  val mediumaquamarine = Color.decode("#66cdaa")
  val mediumblue = Color.decode("#0000cd")
  val mediumorchid = Color.decode("#ba55d3")
  val mediumpurple = Color.decode("#9370db")
  val mediumseagreen = Color.decode("#3cb371")
  val mediumslateblue = Color.decode("#7b68ee")
  val mediumspringgreen = Color.decode("#00fa9a")
  val mediumturquoise = Color.decode("#48d1cc")
  val mediumvioletred = Color.decode("#c71585")
  val midnightblue = Color.decode("#191970")
  val mintcream = Color.decode("#f5fffa")
  val mistyrose = Color.decode("#ffe4e1")
  val moccasin = Color.decode("#ffe4b5")
  val navajowhite = Color.decode("#ffdead")
  val navy = Color.decode("#000080")
  val oldlace = Color.decode("#fdf5e6")
  val olive = Color.decode("#808000")
  val olivedrab = Color.decode("#6b8e23")
  val orange = Color.decode("#ffa500")
  val orangered = Color.decode("#ff4500")
  val orchid = Color.decode("#da70d6")
  val palegoldenrod = Color.decode("#eee8aa")
  val palegreen = Color.decode("#98fb98")
  val paleturquoise = Color.decode("#afeeee")
  val palevioletred = Color.decode("#db7093")
  val papayawhip = Color.decode("#ffefd5")
  val peachpuff = Color.decode("#ffdab9")
  val peru = Color.decode("#cd853f")
  val pink = Color.decode("#ffc0cb")
  val plum = Color.decode("#dda0dd")
  val powderblue = Color.decode("#b0e0e6")
  val purple = Color.decode("#800080")
  val red = Color.decode("#ff0000")
  val rosybrown = Color.decode("#bc8f8f")
  val royalblue = Color.decode("#4169e1")
  val saddlebrown = Color.decode("#8b4513")
  val salmon = Color.decode("#fa8072")
  val sandybrown = Color.decode("#f4a460")
  val seagreen = Color.decode("#2e8b57")
  val seashell = Color.decode("#fff5ee")
  val sienna = Color.decode("#a0522d")
  val silver = Color.decode("#c0c0c0")
  val skyblue = Color.decode("#87ceeb")
  val slateblue = Color.decode("#6a5acd")
  val slategrey = Color.decode("#708090")
  val snow = Color.decode("#fffafa")
  val springgreen = Color.decode("#00ff7f")
  val steelblue = Color.decode("#4682b4")
  val tan = Color.decode("#d2b48c")
  val teal = Color.decode("#008080")
  val thistle = Color.decode("#d8bfd8")
  val tomato = Color.decode("#ff6347")
  val turquoise = Color.decode("#40e0d0")
  val violet = Color.decode("#ee82ee")
  val wheat = Color.decode("#f5deb3")
  val white = Color.decode("#ffffff")
  val whitesmoke = Color.decode("#f5f5f5")
  val yellow = Color.decode("#ffff00")
  val yellowgreen = Color.decode("#9acd32")
  
  /**
   * This function is used to get the BufferedImage of the container as JFXPanel etc
   * @param container
   * @param bounds
   * @return
   */
  private def toBufferedImage(container: Container, bounds: Bounds): BufferedImage = {
    val bufferedImage = new BufferedImage(bounds.getWidth.toInt,
                                          bounds.getHeight.toInt,
                                          BufferedImage.TYPE_INT_ARGB)

    val g = bufferedImage.getGraphics
    g.translate(-bounds.getMinX.toInt, -bounds.getMinY.toInt) // translating to upper-left corner
    container.paint(g)
    g.dispose
    bufferedImage
  }
  

  // -- simple test
  def main(args: Array[String]) {
    val cal = Calendar.getInstance
    // should hold chartReport instance, otherwise it may be GCed and cannot receive message. 
    val chartReport = new ChartReport(".")
    val params = List(TestParam(1), TestParam(2))
    chartReport.roundStarted(params)
    
    val random = new Random(System.currentTimeMillis)
    cal.add(Calendar.DAY_OF_YEAR, -10)
    for (i <- 1 to 10) {
      cal.add(Calendar.DAY_OF_YEAR, i)
      val time = cal.getTimeInMillis
      params foreach {_.publish(ReportData("series", 0, time, random.nextDouble, Color.RED))}
      if (i != 5) {
        params foreach {_.publish(ReportData("series", 1, time, random.nextDouble, Color.BLUE))}
      }
    }
    
    chartReport.roundFinished
  }
  
  private case class TestParam(v: Int) extends Param
}
