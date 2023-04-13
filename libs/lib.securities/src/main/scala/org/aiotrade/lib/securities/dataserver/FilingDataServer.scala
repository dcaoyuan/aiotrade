package org.aiotrade.lib.securities.dataserver

import java.util.Calendar
import java.util.TimeZone
import java.util.logging.Logger
import org.aiotrade.lib.info.model.Filing
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.Singleton

/**
 * @author Guibin Zhang
 */
object FilingDataServer extends FilingDataServer with Singleton {
  def getSingleton = this
}
class FilingDataServer extends DataServer[Filing] {
  type C = FilingContract

  private val log = Logger.getLogger(this.getClass.getName)
  protected val infoQueue = new java.util.concurrent.ConcurrentLinkedQueue[Filing]

  /**
   * Invoked by the InfoApiServer
   */
  def addFilings(filings: List[Filing]) {
    filings.foreach(infoQueue.add(_))
  }

  protected def requestData(contracts: Iterable[FilingContract]) {
    if (!infoQueue.isEmpty) {
      for (contract <- contracts) {
        publishData(DataLoaded(Array(infoQueue.poll), contract))
      }
    }
  }

  protected def processData(filings: Array[Filing], contract: FilingContract): Long = {
    var time = Long.MinValue
    for (filing <- filings) {
      if (filing.generalInfo != null && filing.generalInfo.infoSecs.size > 0) {
        time = math.max(filing.time, time)
        val uniSymbol = filing.generalInfo.infoSecs(0).sec.secInfo.uniSymbol

        Exchange.secOf(uniSymbol) match {
          case Some(sec) =>
            sec.infoSerOf(TFreq.DAILY) match {
              case Some(infoSer) =>
//              if(infoSer.exists(roundToDay(filing.publishTime))){
//                filing +=: infoSer.filings.values.asInstanceOf[ArrayList[Filing]]
//              }
//              else{
//                infoSer.filings(filing.time) = ArrayList(filing)
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

  val displayName: String = "Filing Data Server"
  val defaultDatePattern: String = "MM/dd/yyyy hh:mm"
  val serialNumber = 12
  val sourceTimeZone = TimeZone.getTimeZone("Asia/Shanghai")
}