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

import com.ib.client.Contract
import com.ib.client.Order
import java.awt.Image
import java.io.File
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import java.util.logging.Level
import java.util.logging.Logger
import javax.imageio.ImageIO
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.QuoteServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.Singleton

/**
 * TWS demo user/password
 * For Individual Demo
 *   o User Name: edemo
 *     Password: demouser
 * For Advisor Demo
 *   o User Name: fdemo
 *     Password: demouser
 *
 * @author Caoyuan Deng
 */
object IBQuoteServer extends QuoteServer with Singleton {
  private val log = Logger.getLogger(this.getClass.getName)

  def getSingleton = this

  private val ibWrapper = IBWrapper

  private val maxDurationInSeconds = 86400 // 24 hours
  private val maxDurationInDays    = 365
  private val maxDurationInWeeks   = 54
  private val maxDurationInYears   = 1
    
  protected def connect: Boolean = {
    if (!ibWrapper.isConnected) {
      ibWrapper.connect
    }
        
    ibWrapper.isConnected
  }

  @throws(classOf[Exception])
  protected def request(contract: QuoteContract) {
    val cal = Calendar.getInstance
        
    val storage = new ArrayList[Quote]
        
    var bTime = contract.fromTime
    var eTime = contract.toTime
    if (contract.fromTime <= ANCIENT_TIME /* @todo */) {
    }
        
    var m_rc = false
    var m_backfillEndTime: String = null
    var m_backfillDuration: String = null
    var m_barSizeSetting = 0
    var m_useRTH = 0
    var m_formatDate = 0
    var m_marketDepthRows = 0
    var m_whatToShow: String = null
    val m_contract = new Contract
    val m_order = new Order
    var m_exerciseAction = 0
    var m_exerciseQuantity = 0
    var m_override = 0
        
    try {
            
      // set contract fields
      m_contract.m_symbol = contract.srcSymbol
      val sec = Exchange.secOf(contract.srcSymbol)
      val kind = Sec.Kind.Stock // @TODO
      m_contract.m_secType = IBWrapper.getSecKind(kind).get
      m_contract.m_expiry = ""
      m_contract.m_strike = 0
      m_contract.m_right = ""
      m_contract.m_multiplier = ""
      m_contract.m_exchange = "SMART"
      m_contract.m_primaryExch = "SUPERSOES"
      m_contract.m_currency = "USD"
      m_contract.m_localSymbol = ""
            
      // set order fields
      m_order.m_action = "BUY"
      m_order.m_totalQuantity = 10
      m_order.m_orderType = "LMT"
      m_order.m_lmtPrice = 40
      m_order.m_auxPrice = 0
      m_order.m_sharesAllocation = "FA Allocation Info..."
      m_order.m_goodAfterTime = ""
      m_order.m_goodTillDate = ""
            
      /** set historical data fields: */
      cal.setTimeInMillis(eTime)
      m_backfillEndTime = ibWrapper.getTwsDateFormart.format(cal.getTime)
            
      val freq = contract.freq
            
      /**
       * An integer followed by a space, followed by one of these units:
       * S (seconds), D (days), W (weeks), and Y (years)
       */
      var durationInt = 300
      var durationStr = "D"
      freq.unit match {
        case TUnit.Second =>
          durationInt = math.min(durationInt, maxDurationInSeconds)
          durationStr = "S"
        case TUnit.Minute =>
          durationInt *= 60
          durationInt = math.min(durationInt, maxDurationInSeconds)
          durationStr = "S"
        case TUnit.Hour =>
          durationInt *= 60 * 24
          durationInt = math.min(durationInt, maxDurationInSeconds)
          durationStr = "S"
        case TUnit.Day =>
          durationInt = math.min(durationInt, maxDurationInDays)
          durationStr = "D"
        case TUnit.Week =>
          durationInt = math.min(durationInt, maxDurationInWeeks)
          durationStr = "W"
        case TUnit.Month =>
          durationInt *= 30
          durationInt = math.min(durationInt, maxDurationInDays)
          durationStr = "D"
        case TUnit.Year =>
          durationInt = math.min(durationInt, maxDurationInYears)
          durationStr = "Y"
        case _ =>
      }
      m_backfillDuration = new StringBuilder(7).append(durationInt).append(" ").append(durationStr).toString
            
      m_barSizeSetting = IBWrapper.getBarSize(freq)
            
      m_useRTH = 1
      m_whatToShow = "MIDPOINT"
            
      /**
       * formatDate = 1, dates applying to bars are returned in a format ��yyyymmdd{space}{space}hh:mm:dd��
       *   - the same format already used when reporting executions.
       * formatDate = 2, dates are returned as a integer specifying the number of seconds since 1/1/1970 GMT.
       */
      m_formatDate = 2
      m_exerciseAction = 1
      m_exerciseQuantity = 1
      m_override = 0
            
      // set market depth rows
      m_marketDepthRows = 20
    } catch {
      case ex: Exception => log.log(Level.WARNING, ex.getMessage, ex); return
    }
    
    m_rc = true
        
    val reqId = ibWrapper.reqHistoricalData(
      this,
      storage,
      m_contract,
      m_backfillEndTime,
      m_backfillDuration,
      m_barSizeSetting,
      m_whatToShow,
      m_useRTH,
      m_formatDate
    )
    contract.reqId = reqId
  }

  @throws(classOf[Exception])
  protected def read(contract: QuoteContract): Array[Quote] = {
    /**
     * Don't try <code>synchronized (storage) {}<code>, synchronized (this)
     * instead. Otherwise, the ibWrapper can not process storage during the
     * storage waiting period which will be whthin the synchronized block.
     */
    this synchronized {
      var timeout = false
      while (ibWrapper.isHisDataReqPending(contract.reqId) && !timeout) {
        try {
          //System.out.println("dataserver is waiting: " + getSer(contract).getFreq());
          wait(TUnit.Minute.interval * 1)
          //System.out.println("dataserver is woke up: " + getSer(contract).getFreq());
          timeout = true // whatever
        } catch {case ex: InterruptedException =>
            if (ibWrapper.isHisDataReqPending(contract.reqId)) {
              ibWrapper.cancelHisDataRequest(contract.reqId)
            }
            return EmptyValues
        }
      }
    }
        
    val storage = ibWrapper.quoteStorageOf(contract.reqId)
    storage synchronized {
      val res = storage.toArray
      storage.clear
      res
    }
  }
    
  override protected def cancelRequest(contract: QuoteContract) {
    ibWrapper.cancelHisDataRequest(contract.reqId)
  }
    
  protected def requestData(contracts: Iterable[QuoteContract]) {
    if (!connect) return

    for (contract <- contracts) {
      try {
        request(contract)
        val quotes = read(contract)
        if (quotes.length > 0) {
          publishData(DataLoaded(quotes, contract))
        }
      } catch {
        case ex: Exception => println("Error in loading from source: " + ex.getMessage)
      }
    }
  }
    
  override def supportedFreqs = IBWrapper.getSupportedFreqs

  override def icon: Option[Image] = {
    try {
      Option(ImageIO.read(new File("org/aiotrade/platform/modules/dataserver/ib/resources/favicon_ib.png")))
    } catch {case ex: IOException => None}
  }

  val displayName = "IB TWS"
  val defaultDatePattern = "yyyyMMdd HH:mm:ss"
  val serialNumber = 6
  val sourceTimeZone = TimeZone.getTimeZone("America/New_York")
  val classNameOfTickerServer = Some(IBTickerServer.getClass.getName)

  /**
   * 1 1sec "<30;2000> S"
   * 2 5sec "<30;10000> S"
   * 3 15sec "<30;30000> S" (returns 2 days of data for "30000 S"!)
   * 4 30sec "<30;86400> S", "1 D" (returns 4 days of data for "86400 S"!)
   * 5 1min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
   * 6 2min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
   * 7 5min "<30;86400> S", "<1;6> D" (returns 4 days of data for "86400 S"!)
   * 8 15min "<30;86400> S", "<1;20> D", "<1,2> W" (returns 4 days of data for "86400 S"!)
   * 9 30min "<30;86400> S", "<1;34> D", "<1,4> W" (returns 4 days of data for "86400 S"!)
   * 10 1h "<30;86400> S", "<1;34> D", "<1,4> W" (returns 4 days of data for "86400 S"!)
   * 11 1d "<30;86400> S", "<1;60> D", "<1,52> W" (returns 4 days of data for "86400 S"!)
   */
}




