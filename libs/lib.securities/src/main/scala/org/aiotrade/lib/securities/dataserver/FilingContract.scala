package org.aiotrade.lib.securities.dataserver

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq

class FilingContract extends DataContract[FilingDataServer] {
  val log = Logger.getLogger(this.getClass.getSimpleName)

  serviceClassName = "com.aiotrade.lib.dataserver.info.FilingDataServer"
  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")
  isRefreshable = true

  def isFreqSupported(freq: TFreq) = true
  
  override def clone: FilingContract = {
    try {
      super.clone.asInstanceOf[FilingContract]
    } catch {
      case e: CloneNotSupportedException => log.log(Level.SEVERE, e.getMessage, e); null
    }
  }

  override def displayName = {
    "Filing Data Contract[" + srcSymbol + "]"
  }
}
