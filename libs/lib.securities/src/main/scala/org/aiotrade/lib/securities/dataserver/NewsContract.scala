package org.aiotrade.lib.securities.dataserver

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq

class NewsContract extends DataContract[NewsDataServer] {
  val log = Logger.getLogger(this.getClass.getSimpleName)

  serviceClassName = "com.aiotrade.lib.dataserver.info.NewsDataServer"
  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")
  isRefreshable = true

  def isFreqSupported(freq: TFreq) = true
  
  override def clone: NewsContract = {
    try {
      super.clone.asInstanceOf[NewsContract]
    } catch {
      case e: CloneNotSupportedException => log.log(Level.SEVERE, e.getMessage, e); null
    }
  }

  val displayName = "News Data Contract[" + srcSymbol + "]"
}
