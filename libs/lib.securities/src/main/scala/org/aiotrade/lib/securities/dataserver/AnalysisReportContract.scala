package org.aiotrade.lib.securities.dataserver

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq

class AnalysisReportContract extends DataContract[AnalysisReportDataServer] {
  val log = Logger.getLogger(this.getClass.getSimpleName)

  serviceClassName = "com.aiotrade.lib.dataserver.info.AnalysisReportDataServer"
  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")
  isRefreshable = true

  def isFreqSupported(freq: TFreq) = true
  
  override def clone: AnalysisReportContract = {
    try {
      super.clone.asInstanceOf[AnalysisReportContract]
    } catch {
      case e: CloneNotSupportedException => log.log(Level.SEVERE, e.getMessage, e); null
    }
  }
  
  val displayName = "AnalysisReport Data Contract[" + srcSymbol + "]"
}
