/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.collection.ArrayList


/*                        Sington           deaf to heart beat    function
 * RichInfoHisDataServer  one per contract   yes                 history quote info only
 * InfoDataServer      yes               no                  incremental quote info only
 */
abstract class RichInfoHisDataServer extends DataServer[RichInfo] {
  type C = RichInfoHisContract
  private val log = Logger.getLogger(this.getClass.getName)

  private val updatedEvents = new ArrayList[TSerEvent]

  deafTo(DataServer)

  protected def processData(values: Array[RichInfo], contract: RichInfoHisContract): Long = {
    updatedEvents.clear
    var count = 0

    for (info <- values ; sec <- info.secs) {
      sec.infoPointSerOf(TFreq.ONE_MIN) match {
        case Some(minuteSer) =>
          val event = minuteSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
      sec.infoPointSerOf(TFreq.DAILY) match {
        case Some(dailyeSer) =>
          val event = dailyeSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
    }
    
    var lastTime = Long.MinValue
    updatedEvents foreach {
      case event@TSerEvent.Updated(source, symbol, fromTime, toTime, lastObject, callback) =>
        source.publish(event)
        //log.info(symbol + ": " + count + ", data loaded, load RichInfo server finished")
        lastTime = toTime
      case _ =>
    }
    lastTime
  }

}

