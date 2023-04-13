/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.dataserver.ib

import com.ib.client.Contract;
import com.ib.client.EClientSocket;
import com.ib.client.TickType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Ticker
import scala.collection.immutable.TreeMap


/**
 *
 * @author Caoyuan Deng
 */
class IBWrapper extends EWrapperAdapter

object IBWrapper extends IBWrapper {

  private val TWS_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd HH:mm:ss")
  private val HISTORICAL_DATA_END = "finished"
  private val HIS_REQ_PROC_SPEED_THROTTLE = 1000 * 20 // 20 seconds

  val tickers = new ArrayList[Ticker]

  private val freqToBarSize = Map[TFreq, Int](
    TFreq.ONE_SEC      ->  1,
    TFreq.FIVE_SECS    ->  2,
    TFreq.FIFTEEN_SECS ->  3,
    TFreq.THREE_SECS   ->  4,
    TFreq.ONE_MIN      ->  5,
    TFreq.TWO_MINS     ->  6,
    TFreq.THREE_MINS   -> 16,
    TFreq.FIVE_MINS    ->  7,
    TFreq.FIFTEEN_MINS ->  8,
    TFreq.THIRTY_MINS  ->  9,
    TFreq.ONE_HOUR     -> 10,
    TFreq.DAILY        -> 11,
    TFreq.WEEKLY       -> 12,
    TFreq.MONTHLY      -> 13,
    TFreq.THREE_MONTHS -> 14,
    TFreq.ONE_YEAR     -> 15)

  private val secKindToName: Map[Sec.Kind, String] = Map(
    Sec.Kind.Stock        -> "STK",
    Sec.Kind.Stock        -> "STK",
    Sec.Kind.Option       -> "OPT",
    Sec.Kind.Future       -> "FUT",
    Sec.Kind.Index        -> "IND",
    Sec.Kind.FutureOption -> "FOP",
    Sec.Kind.Currency     -> "CASH",
    Sec.Kind.Bag          -> "BAG")

  private var singletonInstance: IBWrapper = this
  private var eclient: EClientSocket = new EClientSocket(this)

  private lazy val hisRequestServer = new HisRequestServer

  private val host = ""
  private val port = 7496
  private val clientId = 0
    
  /** in IB, the next valid id after connected should be 1 */
  private var nextReqId = 1
  private var reqIdToHisDataReq = new TreeMap[Int, HistoricalDataRequest]
  private var reqIdToMktDataReq = new TreeMap[Int, MarketDataRequest]
    
  private var serverVersion: Int = _
    
  def getBarSize(freq: TFreq) = {
    freqToBarSize.get(freq) getOrElse (1)
  }
    
  def getSupportedFreqs: Array[TFreq] = {
    freqToBarSize.keySet.toArray
  }
    
  def getSecKind(tpe: Sec.Kind) = {
    secKindToName.get(tpe)
  }
    
  private def askReqId: Int = synchronized {
    val reqId = nextReqId
    nextReqId += 1
        
    reqId
  }
    
  def quoteStorageOf(reqId: Int): ArrayList[Quote] = {
    reqIdToHisDataReq.get(reqId) match {
      case None => null
      case Some(hisReq) => hisReq.storage
    }
  }
    
  private def tickerStorageOf(reqId: Int): ArrayList[Ticker] = {
    reqIdToMktDataReq.get(reqId) match {
      case None => null
      case Some(mktReq) => mktReq.storage
    }
  }

  private def hisDataRequestorOf(reqId: Int): DataServer[Quote] = {
    reqIdToHisDataReq.get(reqId) match {
      case None => null
      case Some(hisReq) => hisReq.requestor
    }
  }
    
  def isHisDataReqPending(reqId: Int): Boolean = {
    reqIdToHisDataReq.contains(reqId)
  }
    
  def connect: Unit = synchronized {
    if (isConnected) {
      return
    }
        
    eclient.eConnect(host, port, clientId)
    var timeout = false
    var break = false
    while (!isConnected && !timeout && !break) {
      try {
        wait(TUnit.Second.interval * 5)
        timeout = true // whatever
      } catch {case ex: InterruptedException => break = true}
    }
        
    if (isConnected) {
      /**
       * IB Log levels: 1 = SYSTEM 2 = ERROR 3 = WARNING 4 = INFORMATION 5 = DETAIL
       */
      eclient.setServerLogLevel(2)
      eclient.reqNewsBulletins(true)
      serverVersion = eclient.serverVersion
            
      //WindowManager.getDefault.setStatusText("TWS connected. Server version: " + serverVersion)
    } else {
      //WindowManager.getDefault().setStatusText("Could not connect to TWS.")
    }
  }
    
  def isConnected: Boolean = {
    eclient.isConnected
  }
    
  def getTwsDateFormart: DateFormat = {
    TWS_DATE_FORMAT
  }
    
  def reqHistoricalData(requestor: DataServer[Quote], storage: ArrayList[Quote],
                        contract: Contract, endDateTime: String, durationStr: String,
                        barSizeSetting: Int, whatToShow: String, useRTH: Int, formatDate: Int): Int = {
        
    val reqId = askReqId
        
    val hisReq = HistoricalDataRequest(
      requestor,
      storage,
      contract,
      endDateTime,
      durationStr,
      barSizeSetting,
      whatToShow,
      useRTH,
      formatDate,
      reqId
    )
        
    reqIdToHisDataReq synchronized {
      reqIdToHisDataReq += (reqId -> hisReq)
    }
        
    if (!hisRequestServer.isInRunning) {
      new Thread(hisRequestServer).start
    }
        
    return reqId
  }
    
  def reqMktData(requestor: DataServer[_], contract: Contract, snapTicker: Ticker): Int = {
    val reqId = askReqId
        
    val mktReq = MarketDataRequest(
      contract,
      new ArrayList[Ticker](),
      snapTicker,
      reqId
    )
        
    reqIdToMktDataReq synchronized {
      reqIdToMktDataReq += (reqId -> mktReq)
    }
        
    eclient.reqMktData(reqId, contract)
        
    reqId
  }
    
  def cancelHisDataRequest(reqId: Int) {
    eclient.cancelHistoricalData(reqId);
    clearHisDataRequest(reqId);
  }
    
  def cancelMktDataRequest(reqId: Int) {
    eclient.cancelMktData(reqId);
    reqIdToMktDataReq synchronized {
      reqIdToMktDataReq -= reqId
    }
  }
    
  def isMktDataRequested(reqId: Int): Boolean = {
    reqIdToMktDataReq.contains(reqId)
  }
    
  private def snapTickerOf(reqId: Int): Ticker = {
    reqIdToMktDataReq.get(reqId) match {
      case None => null
      case Some(mktReq) => mktReq.snapTicker
    }
  }
    
  private def clearHisDataRequest(hisReqId: Int) {
    reqIdToHisDataReq synchronized {
      reqIdToHisDataReq -= hisReqId
    }
  }
    
  def getServerVersion: Int = {
    serverVersion
  }
    
  def disconnect {
    if (eclient != null && eclient.isConnected) {
      eclient.cancelNewsBulletins
      eclient.eDisconnect
    }
  }
    
  override def nextValidId(orderId: Int) {
    /**
     * this seems only called one when connected or re-connected. As we use
     * auto-increase id, we can just ignore it?
     */
  }
    
  /** A historical data arrived */
  override def historicalData(reqId: Int, date: String, open: Double, high: Double, low: Double, close: Double, prevClose: Double,
                              volume: Int, WAP: Double, hasGaps: Boolean) {
        
    val storage = quoteStorageOf(reqId)
    if (storage == null) {
      return
    }
        
    /** we only need lock storage here */
    storage synchronized {
      try {
        if (date.startsWith(HISTORICAL_DATA_END)) {
          val requstor = hisDataRequestorOf(reqId)
          if (requstor != null) {
            requstor synchronized {
              requstor.notifyAll
              System.out.println("requstor nofity all: finished")
            }
          }
          clearHisDataRequest(reqId)
        } else {
          val time = try {
            date.toLong * 1000
          } catch {case ex: NumberFormatException => return}
                    
          val quote = new Quote
                    
          quote.time   = time
          quote.open   = open
          quote.high   = high
          quote.low    = low
          quote.close  = close
          quote.volume = volume
          quote.prevClose = prevClose
                    
          quote.vwap = WAP
          quote.hasGaps = hasGaps
                    
          storage += quote
                    
          /** quote is still pending for process, don't return it */
        }
      } catch {case x: Throwable =>
          /**
           * Catch any Throwable to prevent them back to the eclient (will cause disconnect).
           * We don't need cancel this historical requset as the next data may be good.
           */
      }
    }
  }
    
  override def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
    // received price tick
    val snapshot = snapTickerOf(tickerId)
    if (snapshot == null) {
      return
    }
        
    snapshot synchronized {
      val value = price
      snapshot.time = System.currentTimeMillis
      snapshot.isChanged = false
      field match {
        case TickType.ASK =>
          snapshot.setAskPrice(0, value)
        case TickType.ASK_SIZE =>
          snapshot.setAskSize (0, value)
        case TickType.BID =>
          snapshot.setBidPrice(0, value)
        case TickType.BID_SIZE =>
          snapshot.setBidSize (0, value)
        case TickType.CLOSE =>
          snapshot.prevClose = value
        case TickType.HIGH =>
          snapshot.dayHigh = value
        case TickType.LAST =>
          snapshot.lastPrice = value
        case TickType.LAST_SIZE =>
        case TickType.LOW =>
          snapshot.dayLow = value
        case TickType.VOLUME =>
          snapshot.dayVolume = value
        case _ =>
      }
    }

    if (snapshot.isChanged) {
      val ticker = new Ticker
      ticker.uniSymbol = tickerId.toString // @TODO
      ticker.copyFrom(snapshot)
      tickers += ticker
    }

    // @todo who is observe it
    //snapshot.notifyChanged
        
    //System.out.println("id=" + tickerId + "  " + TickType.getField( field) + "=" + price + " " +
    //(canAutoExecute != 0 ? " canAutoExecute" : " noAutoExecute"));
  }
    
  override def tickSize(tickerId: Int, field: Int, size: Int) {
    // received size tick
    tickPrice(tickerId, field, size, 0);
  }
    
  override def tickOptionComputation(tickerId: Int, field: Int, impliedVol: Double, delta: Double) {
    // received price tick
    //        System.out.println( "id=" + tickerId + "  " + TickType.getField( field) + ": vol = " +
    //                ((impliedVol >= 0 && impliedVol != Double.MAX_VALUE) ? Double.toString(impliedVol) : "N/A") + " delta = " +
    //                ((Math.abs(delta) <= 1) ? Double.toString(delta) : "N/A") );
  }
    
  override def error(error: String) {
    //WindowManager.getDefault().setStatusText(error);
  }
    
  private val msg = new StringBuilder(40)
  override def error(id: Int, errorCode: Int, errorMsg: String) {
    msg.delete(0, msg.length)
    if (id < 0) {
      /** connected or not connected msg, notify connect() waiting */
      this synchronized {
        notifyAll
      }
    } else {
      msg.append("Error: reqId = ")
    }
    msg.append(id).append(" | ").append(errorCode).append(" : ").append(errorMsg).toString
        
    System.out.println(msg.toString)
    //WindowManager.getDefault().setStatusText(msg.toString)
        
    /** process error concerns with hisReq */
    val shouldResetAllHisReqs = (
      (errorCode == 1102) ||
      (errorCode == 165 && msg.toString.contains("HMDS connection attempt failed")) ||
      (errorCode == 165 && msg.toString.contains("HMDS server disconnect occurred")))
        
    if (shouldResetAllHisReqs) {
      for (reqId <- reqIdToHisDataReq.keySet) {
        resetHisReq(reqId)
      }
    } else {
      if (reqIdToHisDataReq.contains(id)) {
        resetHisReq(id)
      }
    }
        
  }
    
  private def resetHisReq(reqId: Int) {
    val requstor = hisDataRequestorOf(reqId)
    if (requstor != null) {
      requstor synchronized {
        requstor.notifyAll
        //System.out.println("requstor nofity all on error");
      }
      /** Don't do this before requstor has been fetched from map ! */
      cancelHisDataRequest(reqId)
    }
  }
    
  override def connectionClosed {
  }
        
  private class HisRequestServer extends Runnable {
        
    private var inRunning: Boolean = false
        
    def isInRunning = {
      inRunning
    }
        
    def run {
      inRunning = true
            
      var inRoundProcessing = false
      while (!inRoundProcessing) {
        try {
          Thread.sleep(HIS_REQ_PROC_SPEED_THROTTLE)
        } catch {
          case ex: InterruptedException =>
            ex.printStackTrace
            inRunning = false
            return
        }
                
        inRoundProcessing = true

        /** just fetch the first one to process */
        val hisReq = reqIdToHisDataReq(reqIdToHisDataReq.firstKey)
        eclient.reqHistoricalData(
          hisReq.reqId,
          hisReq.contract,
          hisReq.endDateTime,
          hisReq.durationStr,
          hisReq.barSizeSetting,
          hisReq.whatToShow,
          hisReq.useRTH,
          hisReq.formatDate
        )
                    
        inRoundProcessing = false
      }
    }
  }

  private case class MarketDataRequest(
    contract: Contract,
    storage: ArrayList[Ticker],
    snapTicker: Ticker,
    reqId: Int
  )

  private case class HistoricalDataRequest(
    requestor: DataServer[Quote],
    storage: ArrayList[Quote],
    contract: Contract,
    endDateTime: String,
    durationStr: String,
    barSizeSetting: Int,
    whatToShow: String,
    useRTH: Int,
    formatDate: Int,
    reqId: Int
  )

}

