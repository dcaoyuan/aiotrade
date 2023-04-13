/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities
import org.aiotrade.lib.securities.model.Sec
import java.util.Calendar
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.dataserver.RichInfo
import org.aiotrade.lib.math.timeseries.{DefaultBaseTSer, TFreq, TSerEvent, TVal}
import org.aiotrade.lib.math.indicator.Plot
import scala.collection.mutable
import java.util.logging.Logger

class InfoPointSer ($sec: Sec, $freq: TFreq) extends DefaultBaseTSer($sec, $freq) {
  val infos   = TVar[ArrayList[RichInfo]]("I", Plot.None)
  private val log = Logger.getLogger(this.getClass.getName)

  def updateFromNoFire(info : RichInfo) : TSerEvent = {
    try {
      writeLock.lock
      val cal = Calendar.getInstance($sec.exchange.timeZone)
      //log.info(("publishtime:" + info.generalInfo.publishTime + " title : " + info.generalInfo.title))
      val time = $freq.round(info.generalInfo.publishTime, cal)
      createWhenNonExist(time)
      Option(infos(time)) match {
        case Some(x) => x += info
        case None =>
          val ifs = ArrayList[RichInfo]()
          ifs.append(info)
          infos(time) = ifs

      }
      TSerEvent.Updated(this, "", time, time)
    } finally {
      writeLock.unlock
    }
  }

  def exportTo(fromTime : Long, toTime : Long) : List[mutable.Map[String, Any]] =  {
    try {
      readLock.lock
      timestamps.readLock.lock

      val vs = vars filter (v => v.name != "" && v.name != null)
      val frIdx = timestamps.indexOrNextIndexOfOccurredTime(fromTime)
      var toIdx = timestamps.indexOrPrevIndexOfOccurredTime(toTime)
      toIdx = vs.foldLeft(toIdx){(acc, v) => math.min(acc, v.values.length)}
      val RichInfos = ArrayList[mutable.Map[String, Any]]()
      for(i : Int <- 0 to infos.size) {
        if(infos(i) != null ){
          for (RichInfo <- infos(i)) {
            if(RichInfo != null) RichInfos.append(RichInfo.export)
          }
        }
      }
      RichInfos.toList   
    } finally {
      timestamps.readLock.unlock
      readLock.unlock
    }
  }
  def updateFrom(info : RichInfo) {
    publish(updateFromNoFire(info))
  }


}
