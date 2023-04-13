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
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.MoneyFlows1d
import org.aiotrade.lib.securities.model.MoneyFlows1m
import ru.circumflex.orm._

abstract class MoneyFlowServer extends DataServer[MoneyFlow] {
  type C = MoneyFlowContract

  private val log = Logger.getLogger(this.getClass.getSimpleName)

  /**
   * All quotes in storage should have been properly rounded to 00:00 of exchange's local time
   */
  protected def processData(mfs: Array[MoneyFlow], contract: MoneyFlowContract): Long = {
    val uniSymbol = toUniSymbol(contract.srcSymbol)
    val sec = Exchange.secOf(uniSymbol).getOrElse(return contract.loadedTime)
    log.info("Got Moneyflows from source of " + uniSymbol + "(" + contract.freq + "), size=" + mfs.length)

    var frTime = contract.loadedTime
    var toTime = contract.loadedTime
    var i = -1
    while ({i += 1; i < mfs.length}) {
      val mf = mfs(i)
      mf.sec = sec
      mf.unfromMe_!
      frTime = math.min(mf.time, frTime)
      toTime = math.max(mf.time, toTime)
    }

    val ser = contract.freq match {
      case TFreq.ONE_SEC => sec.realtimeMoneyFlowSer
      case x => sec.moneyFlowSerOf(x).get
    }
    ser ++= mfs

    ser.publish(TSerEvent.Loaded(ser, uniSymbol, frTime, toTime))

    // save to db after published TSerEvent, so the chart showing won't be blocked
    contract.freq match {
      case TFreq.DAILY =>
        MoneyFlows1d.saveBatch(sec, mfs)
        COMMIT
      case TFreq.ONE_MIN =>
        MoneyFlows1m.saveBatch(sec, mfs)
        COMMIT
      case _ =>
    }

    if (contract.isRefreshable) {
      startRefresh
    }
    
    contract.loadedTime = toTime
    toTime
  }

  /**
   * Override to provide your options
   * @return supported frequency array.
   */
  def supportedFreqs: Array[TFreq] = Array()

  def isFreqSupported(freq: TFreq): Boolean = supportedFreqs exists (_ == freq)

  def toSrcSymbol(uniSymbol: String): String = uniSymbol
  def toUniSymbol(srcSymbol: String): String = srcSymbol
}