package org.aiotrade.lib.securities.dataserver

import java.util.TimeZone
import java.util.Calendar
import java.util.logging.Logger
import org.aiotrade.lib.info.model.News
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.Singleton

/**
 * @author Guibin Zhang
 */
object NewsDataServer extends NewsDataServer with Singleton {
  def getSingleton = this
}
class NewsDataServer extends DataServer[News] {
  type C = NewsContract

  private val log = Logger.getLogger(this.getClass.getName)
  protected val infoQueue = new java.util.concurrent.ConcurrentLinkedQueue[News]

  /**
   * Invoked by the InfoApiServer
   */
  def addNews(newses: List[News]){
    newses.foreach(infoQueue.add(_))
  }

  protected def requestData(contracts: Iterable[NewsContract]) {
    if (!infoQueue.isEmpty) {
      for (contract <- contracts) {
        publishData(DataLoaded(Array(infoQueue.poll), contract))
      }
    }
  }

  protected def processData(newses: Array[News], contract: NewsContract): Long = {
    var time = Long.MinValue
    for (news <- newses) {
      if (news.generalInfo != null && news.generalInfo.infoSecs.size > 0){
        time = math.max(news.time, time)
        val uniSymbol = news.generalInfo.infoSecs(0).sec.secInfo.uniSymbol

        Exchange.secOf(uniSymbol) match {
          case Some(sec) =>
            sec.infoSerOf(TFreq.DAILY) match {
              case Some(infoSer) =>
//              if(infoSer.exists(roundToDay(news.publishTime))){
//                news +=: infoSer.newses.values.asInstanceOf[ArrayList[News]]
//              }
//              else{
//                infoSer.newses(news.time) = ArrayList(news)
//              }
              case None => log.warning("Get None of DAILY infoSer of " + uniSymbol)
            }
          case None => log.warning("Get None of " + uniSymbol + " from Exchange")
        }
      }
    }
    time
  }

  def roundToDay(time: Long) = TFreq.DAILY.round(System.currentTimeMillis, Calendar.getInstance(Exchange.SS.timeZone))

  val displayName: String = "News Data Server"
  val defaultDatePattern: String = "MM/dd/yyyy hh:mm"
  val serialNumber = 11
  val sourceTimeZone = TimeZone.getTimeZone("Asia/Shanghai")
}