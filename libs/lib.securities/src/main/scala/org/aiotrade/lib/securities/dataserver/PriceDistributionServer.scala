/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.PriceCollection
import org.aiotrade.lib.securities.model.PriceDistribution
import org.aiotrade.lib.securities.model.PriceDistributions
import ru.circumflex.orm._

abstract class  PriceDistributionServer  extends DataServer[PriceCollection]{

  type C = PriceDistributionContract

  private val log = Logger.getLogger(this.getClass.getSimpleName)

  /**
   * All price-volume in storage should have been properly rounded to 00:00 of exchange's local time
   */
  protected def processData(pds: Array[PriceCollection], contract: PriceDistributionContract): Long = {
    val uniSymbol = toUniSymbol(contract.srcSymbol)
    val sec = Exchange.secOf(uniSymbol).getOrElse(return contract.loadedTime)
    log.info("Got price distributions from source of " + uniSymbol + "(" + contract.freq + "), size=" + pds.length)

    var frTime = contract.loadedTime
    var toTime = contract.loadedTime
    var i = 0
    while (i < pds.length) {
      val pd = pds(i)
      pd.values.foreach{value => value.sec = sec}
      frTime = math.min(pd.time, frTime)
      toTime = math.max(pd.time, toTime)

      i += 1
    }

    val ser = contract.freq match {
      case x => sec.priceDistributionSerOf(x).get
    }
    ser ++= pds

    ser.publish(TSerEvent.Loaded(ser, uniSymbol, frTime, toTime))

    // save to db after published TSerEvent, so the chart showing won't be blocked
    contract.freq match {
      case TFreq.DAILY =>
        PriceDistributions.saveBatch(sec, pds)
        COMMIT
      case _ =>
    }

    if (contract.isRefreshable) {
      startRefresh
    }

    contract.loadedTime = toTime
    toTime
  }

  def supportedFreqs = Array(TFreq.DAILY)
  def isFreqSupported(freq: TFreq) = supportedFreqs exists (_ == freq)

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}
