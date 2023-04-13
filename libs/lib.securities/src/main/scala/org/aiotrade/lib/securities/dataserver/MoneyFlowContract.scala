package org.aiotrade.lib.securities.dataserver

import java.awt.Image
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataContract

class MoneyFlowContract extends DataContract[MoneyFlowServer] {

  serviceClassName = "org.aiotrade.lib.dataserver.yahoo.YahooQuoteServer"
  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")

  def icon: Option[Image] =  {
    if (isServiceInstanceCreated) {
      createdServerInstance.icon
    } else {
      lookupServiceTemplate(classOf[MoneyFlowServer], "DataServers") match {
        case Some(x) => x.icon
        case None => None
      }
    }
  }

  def supportedFreqs: Array[TFreq] = {
    if (isServiceInstanceCreated) {
      createdServerInstance.supportedFreqs
    } else {
      lookupServiceTemplate(classOf[MoneyFlowServer], "DataServers") match {
        case Some(x) => x.supportedFreqs
        case None => Array()
      }
    }
  }

  def isFreqSupported(freq: TFreq): Boolean = {
    if (isServiceInstanceCreated) {
      createdServerInstance.isFreqSupported(freq)
    } else {
      lookupServiceTemplate(classOf[MoneyFlowServer], "DataServers") match {
        case Some(x) => x.isFreqSupported(freq)
        case None => false
      }
    }
  }

  override def displayName = "MoneyFlow Data Contract"
}
