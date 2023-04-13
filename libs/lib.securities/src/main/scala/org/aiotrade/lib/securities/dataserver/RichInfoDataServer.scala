/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.dataserver

import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.ContentCategory
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.collection.ArrayList
import ru.circumflex.orm._

class RichInfo extends TVal {
  
  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  var generalInfo : GeneralInfo =  new GeneralInfo()
  var content : String = ""
  var summary : String = ""
  var categories : ListBuffer[ContentCategory] = new ListBuffer[ContentCategory]()
  var secs : ListBuffer[Sec] = new ListBuffer[Sec]()
  def export: mutable.Map[String, Any]= {
    mutable.Map[String, Any]("publishTime" -> generalInfo.publishTime,
                             "title" -> generalInfo.title,
                             "url" -> generalInfo.url,
                             "combinValue" -> generalInfo.combinValue,
                             "content" -> content,
                             "summary" -> summary,
                             "category" -> {for(cate <- categories if cate != null) yield cate.code}.toList,
                             "symbol" -> {for(sec <- secs if sec != null) yield sec.uniSymbol}.toList)
  }
}

final case class RichInfoSnapshot(publishTime : Long, title: String, url : String,
                                  combinValue : Long, content : String, summary : String,
                                  category : List[ContentCategory], secs : List[Sec] ) {

  def export: mutable.Map[String, Any]= {
    mutable.Map[String, Any]("publishTime" -> publishTime,
                             "title" -> title,
                             "url" -> url,
                             "combinValue" -> combinValue,
                             "content" -> content,
                             "summary" -> summary,
                             "category" -> {for(cate <- category if cate != null) yield cate.code},
                             "symbol" -> {for(sec <- secs if sec != null) yield sec.uniSymbol})
  }
}

final case class RichInfoSnapshots(events : List[RichInfoSnapshot]) {
  def export: List[mutable.Map[String, Any]] = for(event <- events) yield event.export
}

object RichInfoDataServer extends Publisher

abstract class RichInfoDataServer extends DataServer[RichInfo] {
  type C = RichInfoContract
  private val log = Logger.getLogger(this.getClass.getName)

  private val updatedEvents = new ArrayList[TSerEvent]
  private val allRichInfo = new ArrayList[RichInfoSnapshot]

  protected def processData(values: Array[RichInfo], contract: RichInfoContract): Long = {
    updatedEvents.clear
    allRichInfo.clear
    var count = 0

    for (info <- values; sec <- info.secs) {
      sec.infoPointSerOf(TFreq.ONE_MIN) match {
        case Some(minuteSer) => val event = minuteSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
      sec.infoPointSerOf(TFreq.DAILY) match {
        case Some(dailyeSer) => val event = dailyeSer.updateFromNoFire(info)
          updatedEvents += event
          count = count + 1
        case _ =>
      }
      val category = info.categories.headOption match {
        case Some(x) => x
        case None => null
      }
      val RichInfo = RichInfoSnapshot(info.generalInfo.publishTime, info.generalInfo.title,
                                      info.generalInfo.url, info.generalInfo.combinValue,
                                      info.content, info.summary,info.categories.toList,
                                      info.secs.toList)
      allRichInfo += RichInfo
    }

    values foreach (value => GeneralInfo.save(value))
    COMMIT
    
    if (allRichInfo.length > 0) {
      log.info("Publish RichInfoSnapshots :" + allRichInfo.size)
      RichInfoDataServer.publish(RichInfoSnapshots(allRichInfo.toList))
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

