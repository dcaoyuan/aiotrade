package org.aiotrade.lib.securities

import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.info.model.News
import org.aiotrade.lib.info.model.AnalysisReport
import org.aiotrade.lib.info.model.Filing
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.InfoContent
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable.Map
import scala.collection.JavaConversions._


object InfoSer {
  def main(args: Array[String]) {
    val nDays = 100
    val oneday = 24 * 60 * 60 * 1000
    val cal = Calendar.getInstance
    cal.add(Calendar.DAY_OF_YEAR, -nDays)

    val values = new ArrayList[News]

    var i = 0
    while (i < nDays) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
      val curTime = cal.getTimeInMillis

      val news01 = new News
      news01.time = curTime
      news01.author = "Author" + i
      news01.orgPublisher = "orgPublisher" + i
      news01.hotness = 1F
      val gInfo01 = new GeneralInfo
      gInfo01.publishTime = curTime
      gInfo01.title = "Title" + i
      gInfo01.infoClass = GeneralInfo.NEWS
      news01.generalInfo = gInfo01

      val news02 = new News
      news02.time = curTime
      news02.author = "@Author" + i
      news02.orgPublisher = "orgPublisher" + i
      news02.hotness = 1F
      val gInfo02 = new GeneralInfo
      gInfo02.publishTime = curTime
      gInfo02.title = "@Title" + i
      gInfo02.infoClass = GeneralInfo.NEWS
      news02.generalInfo = gInfo02
      
      val newsAtSameTime = new News
      newsAtSameTime.time = curTime

      newsAtSameTime += news01
      newsAtSameTime += news02

      values += newsAtSameTime
      i += 1
    }

    val ser = new InfoSer(null, TFreq.DAILY)
    
    ser ++= values.toArray
    println(ser)

    println("ser.newses.values=" + ser.newses.values)
    println(ser.newses.values.getClass)
    println("ser.vars=" + ser.vars)
    println(ser.vars.getClass)

  }

}

class InfoSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {

  val newses = TVar[ArrayList[News]]("I", Plot.Info)
  val filings = TVar[ArrayList[Filing]]("I", Plot.Info)
  val analysisReports = TVar[ArrayList[AnalysisReport]]("I", Plot.Info)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    
    tval match {
      case news: News => newses(time) = news.newses
      case filing: Filing => filings(time) = filing.filings
      case analysisReport : AnalysisReport => analysisReports(time) = analysisReport.analysisReports

      case _ => assert(false, "Should pass a Info type TimeValue")
    }
  }

}
