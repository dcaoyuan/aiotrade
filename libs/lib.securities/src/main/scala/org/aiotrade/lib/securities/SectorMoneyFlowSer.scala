/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.securities.model._
import org.aiotrade.lib.math.timeseries.{TVal, TFreq}

class SectorMoneyFlowSer(_sector: Sector, $freq: TFreq) extends MoneyFlowSer(null, $freq) {

  override def serProvider: Sector = _sector

  val volumnPercentOfMarket = TVar[Double]("P", Plot.None)

  override protected def assignValue(tval: TVal){
    super.assignValue(tval)

    tval match {
      case mf: MoneyFlow =>
        volumnPercentOfMarket(mf.time) = mf.volumnPercentOfMarket
        netBuyPercent(mf.time) = mf.netBuyPercent
      case _ =>
    }
  }

  override def valueOf(time: Long): Option[MoneyFlow] = {
    super.valueOf(time) match{
      case Some(mf) =>
        mf.volumnPercentOfMarket = volumnPercentOfMarket(time)
        Some(mf)
      case None => None
    }

  }
}
