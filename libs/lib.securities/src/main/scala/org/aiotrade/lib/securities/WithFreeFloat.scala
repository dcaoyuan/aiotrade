/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities

import java.util.logging.Logger
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.DefaultBaseTSer
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.Sec

trait WithFreeFloat {self: DefaultBaseTSer =>

  val freeFloat = TVar[Double]("FF", Plot.None)

  private val log = Logger.getLogger(getClass.getName)

  override protected def assignValue(tval: TVal) {
    val time = tval.time
    val idx = this.indexOfOccurredTime(time) - 1
    if (idx >= 0) freeFloat(time) = freeFloat(idx)
  }

  def doCalcRate

  protected def calcRateByFreeFloat(col: TVar[Double], volume: TVar[Double]){
    val infos = Exchanges.secInfosOf(serProvider.asInstanceOf[Sec])
    if (infos.isEmpty) {
      log.info("There is no secinfos of " + serProvider)
      return
    }

    val infoItr = infos.iterator
    var i = size - 1
    while (infoItr.hasNext) {
      val info = infoItr.next
      var stop = false
      while(i >= 0 && !stop){
        val time = timestamps(i)
        log.fine("Sec=" + info.uniSymbol + ",time = " + time + ", info.validFrom = " + info.validFrom + ", freefloat = " + info.freeFloat)
        if (time >= info.validFrom && info.freeFloat > 0){
          col(i) = volume(i) / info.freeFloat
          freeFloat(i) = info.freeFloat
          i -= 1
        }
        else{
          stop = true
        }
        log.fine("column (turnoverRate/netBuyPercent)=" + col(i) + ", volume =" + volume(i))
      }
    }

    while (i >= 0){
      col(i) = 0
      freeFloat(i) = 0
      i -= 1
    }
  }

}
