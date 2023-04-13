/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.TFreq

class RichInfoContract extends DataContract[RichInfoDataServer] {

  serviceClassName = null
  freq = TFreq.ONE_MIN
  isRefreshable = true

  def isFreqSupported(freq: TFreq): Boolean = true

  override def displayName = {
    "RichInfo Data Contract[" + srcSymbol + "]"
  }
}
