/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import java.awt.Image
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataContract

class PriceDistributionContract  extends DataContract[PriceDistributionServer]  {

  serviceClassName = null //"org.aiotrade.lib.dataserver.yahoo.YahooQuoteServer"

  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")

  def icon: Option[Image] =  {
    if (isServiceInstanceCreated) {
      createdServerInstance.icon
    } else {
      lookupServiceTemplate(classOf[PriceDistributionServer], "DataServers") match {
        case Some(x) => x.icon
        case None => None
      }
    }
  }

  def supportedFreqs: Array[TFreq] = Array(TFreq.DAILY)

  def isFreqSupported(freq: TFreq): Boolean = {
    freq == TFreq.DAILY
  }

  override def displayName = "Price Distribution Data Contract"
}
