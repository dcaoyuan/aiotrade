package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq


class RichInfoHisContract extends DataContract[RichInfoHisDataServer] {
  serviceClassName = null
  freq = TFreq.DAILY
  isRefreshable = false

  def isFreqSupported(freq: TFreq): Boolean = true

  override def displayName = {
    "RichInfo His Data Contract[" + srcSymbol + "]"
  }

}
