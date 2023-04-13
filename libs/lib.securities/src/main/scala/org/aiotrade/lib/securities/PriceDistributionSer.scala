/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.{TVal, TSerEvent, DefaultBaseTSer, TFreq}
import org.aiotrade.lib.securities.model.PriceCollection
import org.aiotrade.lib.securities.model.PriceDistribution
import org.aiotrade.lib.securities.model.Sec

class PriceDistributionSer($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq)  {

  private var _shortName: String = ""

  val priceCollection = TVar[PriceCollection]("PR", Plot.None)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    tval match {
      case pd: PriceCollection =>
        priceCollection(time) = pd
      case _ =>
    }
  }

  def valueOf(time: Long): Option[PriceCollection] = {
    if (exists(time)) {
      Some(priceCollection(time))
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(pd: PriceCollection) {
    val time = pd.time
    createOrReset(time)
    priceCollection(time) = pd

    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }

  /**
   * This function adjusts linear according to a norm
   */
  private def linearAdjust(value: Double, prevNorm: Double, postNorm: Double): Double = {
    ((value - prevNorm) / prevNorm) * postNorm + postNorm
  }

  override def shortName =  _shortName
  override def shortName_=(name: String) {
    this._shortName = name
  }

}
