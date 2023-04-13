package org.aiotrade.lib.securities.dataserver

import java.util.logging.Logger

import java.util.Calendar
import java.util.TimeZone
import org.aiotrade.lib.info.model.AnalysisReport
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.Singleton

/**
 * @author Guibin Zhang
 */
object AnalysisReportDataServer extends AnalysisReportDataServer with Singleton {
  def getSingleton = this
}
abstract class AnalysisReportDataServer extends DataServer[AnalysisReport] {
  type C = AnalysisReportContract

  private val log = Logger.getLogger(this.getClass.getName)
  protected val infoQueue = new java.util.concurrent.ConcurrentLinkedQueue[AnalysisReport]

  /**
   * Invoked by the InfoApiServer
   */
  def addAnalysisReports(reports: List[AnalysisReport]){
    reports.foreach(infoQueue.add(_))
  }

  protected def requestData(contracts: Iterable[AnalysisReportContract]) {
    if (!infoQueue.isEmpty) {
      for (contract <- contracts) {
        publishData(DataLoaded(Array(infoQueue.poll), contract))
      }
    }
  }
  
  def processData(reports: Array[AnalysisReport], contract: AnalysisReportContract): Long = {
    var time = Long.MinValue
    for (report <- reports) {
      if (report.generalInfo != null && report.generalInfo.infoSecs.size > 0){
        val uniSymbol = report.generalInfo.infoSecs(0).sec.secInfo.uniSymbol
        time = math.max(report.time, time)
        Exchange.secOf(uniSymbol) match {
          case Some(sec) =>
            sec.infoSerOf(TFreq.DAILY) match {
              case Some(infoSer) =>
//              if(infoSer.exists(roundToDay(report.publishTime))){
//                report +=: infoSer.analysisReports.values.asInstanceOf[ArrayList[AnalysisReport]]
//              }
//              else{
//                infoSer.analysisReports(report.time) = ArrayList(report)
//              }
              case None => log.warning("Get None of DAILY infoSer of " + uniSymbol)
            }
          case None => log.warning("Get None of " + uniSymbol + " from Exchange")
        }
      }
    }
    time
  }

  def roundToDay(time: Long) = TFreq.DAILY.round(System.currentTimeMillis, Calendar.getInstance(Exchange.SS.timeZone))

  val displayName: String = "Analysis Report Data Server"
  val defaultDatePattern: String = "MM/dd/yyyy hh:mm"
  val serialNumber = 11
  val sourceTimeZone = TimeZone.getTimeZone("Asia/Shanghai")
}