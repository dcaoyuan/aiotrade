/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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

package org.aiotrade.lib.securities.model

import java.util.Calendar
import org.aiotrade.lib.info.model.Infos1m
import org.aiotrade.lib.info.model.Infos1d
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.securities.api
import org.aiotrade.lib.securities.InfoPointSer
import org.aiotrade.lib.securities.InfoSer
import org.aiotrade.lib.securities.MoneyFlowSer
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.PriceDistributionSer
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.QuoteSerCombiner
import org.aiotrade.lib.securities.dataserver.MoneyFlowContract
import org.aiotrade.lib.securities.dataserver.PriceDistributionContract
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.RichInfoHisContract
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.dataserver.RichInfo
import org.aiotrade.lib.securities.dataserver.RichInfoContract
import org.aiotrade.lib.securities.dataserver.RichInfoDataServer
import org.aiotrade.lib.util.actors.Reactions
import java.util.logging.Logger
import scala.collection.mutable
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.GeneralInfos
import org.aiotrade.lib.info.model.GeneralInfo
import org.aiotrade.lib.info.model.InfoSecs
import ru.circumflex.orm._


/**
 * Securities: Stock, Options, Futures, Index, Currency etc.
 *
 * An implement of Sec.
 * each sofic has a default quoteSer and a tickerSer which will be created in the
 * initialization. The quoteSer will be put in the freq-ser map, the tickerSer
 * won't be.
 * You may put ser from outside, to the freq-ser map, so each sofic may have multiple
 * freq sers, but only per freq pre ser is allowed.
 *
 * @author Caoyuan Deng
 */
class Sec extends SerProvider with CRCLongId with Ordered[Sec] {
  private val log = Logger.getLogger(this.getClass.getName)

  // --- database fields
  var exchange: Exchange = _

  var validFrom: Long = 0
  var validTo: Long = 0

  var company: Company = _
  var companyHists: List[Company] = Nil

  var secInfo: SecInfo = _
  var secInfoHists: List[SecInfo] = Nil
  var secStatus: SecStatus = _
  var secStatusHists: List[SecStatus] = Nil

  var secIssue: SecIssue = _
  var secDividends: List[SecDividend] = Nil

  var dailyQuotes: List[Quote] = Nil
  var dailyMoneyFlow: List[MoneyFlow] = Nil

  var minuteQuotes: List[Quote] = Nil
  var minuteMoneyFlow: List[MoneyFlow] = Nil

  // --- end of database fields

  type T = QuoteSer
  type C = QuoteContract

  private val mutex = new AnyRef()
  private var _realtimeSer: QuoteSer = _
  private var _realtimeMoneyFlowSer: MoneyFlowSer = _
  private var _realtimePriceDistributionSer: PriceDistributionSer = _
  private[securities] lazy val freqToQuoteSer = mutable.Map[TFreq, QuoteSer]()
  private lazy val freqToMoneyFlowSer = mutable.Map[TFreq, MoneyFlowSer]()
  private lazy val freqToPriceDistribuSer = mutable.Map[TFreq, PriceDistributionSer]()
  private lazy val freqToInfoSer = mutable.Map[TFreq, InfoSer]()
  private lazy val freqToInfoPointSer = mutable.Map[TFreq, InfoPointSer]()

  /**
   * @TODO, how about tickerServer switched?
   */
  private lazy val tickerServer: Option[TickerServer] = tickerContract.serviceInstance()
  private lazy val richInfoServer: Option[RichInfoDataServer] = richInfoContract.serviceInstance()
  private lazy val _content = PersistenceManager().restoreContent(uniSymbol)

  var description = ""
  private var _defaultFreq: TFreq = _
  private var _quoteContracts: Seq[QuoteContract] = Nil
  private var _moneyFlowContracts: Seq[MoneyFlowContract] = Nil
  private var _tickerContract: TickerContract = _
  private var _richInfoContract : RichInfoContract = _
  private var _richInfoHisContracts : Seq[RichInfoHisContract] = _
  
  def defaultFreq = if (_defaultFreq == null) TFreq.DAILY else _defaultFreq
  def content = _content // common content, all secs may share same content instance, @todo
  private lazy val selfContent = _content.clone
  
  private def dataContractOf[T <: DataContract[_]](tpe: Class[T], freq: TFreq): Option[T] = {
    val contracts = selfContent.lookupDescriptors(tpe)
    contracts.find(_.freq == freq) match {
      case None => 
        contracts.find(_.freq == defaultFreq) match {
          case Some(defaultOne) if defaultOne.isFreqSupported(freq) =>
            val x = defaultOne.clone//new QuoteContract
            x.freq = freq
            selfContent.addDescriptor(x)
            Some(x.asInstanceOf[T])
          case _ => None
        }
      case some => some
    }
  }

  def richInfoHisContracts = _richInfoHisContracts

  def richInfoContract = {
    if (_richInfoContract == null){
      _richInfoContract = new RichInfoContract()
    }
    _richInfoContract
  }

  def richInfoContract_= (contract : RichInfoContract) {
    _richInfoContract = contract
  }

  def realtimeSer = mutex synchronized {
    if (_realtimeSer == null) {
      _realtimeSer = new QuoteSer(this, TFreq.ONE_MIN)
      freqToQuoteSer.put(TFreq.ONE_SEC, _realtimeSer)
    }
    _realtimeSer
  }

  def realtimeMoneyFlowSer = mutex synchronized {
    if (_realtimeMoneyFlowSer == null) {
      _realtimeMoneyFlowSer = new MoneyFlowSer(this, TFreq.ONE_MIN)
      freqToMoneyFlowSer.put(TFreq.ONE_SEC, _realtimeMoneyFlowSer)
    }
    _realtimeMoneyFlowSer
  }

  def realtimePriceDistributionSer = mutex synchronized {
    if (_realtimePriceDistributionSer == null) {
      _realtimePriceDistributionSer = new PriceDistributionSer(this, TFreq.DAILY)
      freqToPriceDistribuSer.put(TFreq.ONE_SEC, _realtimePriceDistributionSer)
    }
    _realtimePriceDistributionSer
  }

  /** tickerContract will always be built according to quoteContrat ? */
  def tickerContract = {
    if (_tickerContract == null) {
      _tickerContract = new TickerContract
    }
    _tickerContract
  }
  def tickerContract_=(tickerContract: TickerContract) {
    _tickerContract = tickerContract
  }

  def serProviderOf(uniSymbol: String): Option[Sec] = {
    Exchange.secOf(uniSymbol)
  }

  def serOf(freq: TFreq): Option[QuoteSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC => Some(realtimeSer)
      case _ => freqToQuoteSer.get(freq) match {
          case None => freq match {
              case TFreq.ONE_MIN | TFreq.DAILY =>
                val x = new QuoteSer(this, freq)
                freqToQuoteSer.put(freq, x)
                Some(x)
              case _ => createCombinedSer(freq)
            }
          case some => some
        }
    }
  }
  
  def setSer(ser: QuoteSer): Unit = mutex synchronized {
    freqToQuoteSer(ser.freq) = ser
  }

  def moneyFlowSerOf(freq: TFreq): Option[MoneyFlowSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToMoneyFlowSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new MoneyFlowSer(this, freq)
                freqToMoneyFlowSer.put(freq, x)
                Some(x)
              case None => None
            }
          case some => some
        }
      case _ => freqToMoneyFlowSer.get(freq) match {
          case None => None // @todo createCombinedSer(freq)
          case some => some
        }
    }
  }
  
  def updateQuoteSer(freq: TFreq, quote: Quote) {
    freq match {
      case TFreq.ONE_MIN =>
        if (!TickerServer.isServer && isSerCreated(TFreq.ONE_SEC)) {
          realtimeSer.updateFrom(quote)
        }
        if (isSerCreated(TFreq.ONE_MIN)) {
          serOf(TFreq.ONE_MIN) foreach {_.updateFrom(quote)}
        }
      case TFreq.DAILY =>
        if (isSerCreated(TFreq.DAILY)) {
          serOf(TFreq.DAILY) foreach {_.updateFrom(quote)}
        }
      case _ => // todo
    }
  }
  
  def updateMoneyFlowSer(freq: TFreq, moneyFlow: MoneyFlow) {
    freq match {
      case TFreq.ONE_MIN =>
        if (isSerCreated(TFreq.ONE_MIN)) {
          moneyFlowSerOf(TFreq.ONE_MIN) foreach (_.updateFrom(moneyFlow))
        }
      case TFreq.DAILY =>
        if (isSerCreated(TFreq.DAILY)) {
          moneyFlowSerOf(TFreq.DAILY) foreach (_.updateFrom(moneyFlow))
        }
      case _ => // todo
    }
  }

  def priceDistributionSerOf(freq: TFreq): Option[PriceDistributionSer] = mutex synchronized {
    freq match{
      case TFreq.DAILY => freqToPriceDistribuSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new PriceDistributionSer(this, freq)
                freqToPriceDistribuSer.put(freq, x)
                Some(x)
              case None => None
            }
          case _ => freqToPriceDistribuSer.get(freq)
        }
    }
  }

  def updatePriceDistributionSer(freq: TFreq, priceDistribution: PriceCollection) {
    freq match {
      case TFreq.DAILY =>
        if (isSerCreated(TFreq.DAILY)) {
          priceDistributionSerOf(TFreq.DAILY) foreach (_.updateFrom(priceDistribution))
        }
      case _ => // todo
    }
  }

  def infoPointSerOf(freq: TFreq): Option[InfoPointSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToInfoPointSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new InfoPointSer(this, freq)
                freqToInfoPointSer.put(freq, x)
                Some(x)
              case None => None
            }
          case some => some
        }
      case _ => freqToInfoPointSer.get(freq) match {
          case None => None // @todo createCombinedSer(freq)
          case some => some
        }
    }
  }

  def infoSerOf(freq: TFreq): Option[InfoSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC | TFreq.ONE_MIN | TFreq.DAILY => freqToInfoSer.get(freq) match {
          case None => serOf(freq) match {
              case Some(quoteSer) =>
                val x = new InfoSer(this, freq)
                freqToInfoSer.put(freq, x)
                Some(x)
              case None => None
            }
          case some => some
        }
      case _ => freqToInfoSer.get(freq) match {
          case None => None // @todo createCombinedSer(freq)
          case some => some
        }
    }
  }
  
  /**
   * @Note
   * here should be aware that if sec's ser has been loaded, no more
   * SerChangeEvent.Type.FinishedLoading will be fired, so if we create followed
   * viewContainers here, should make sure that the QuoteSerCombiner listen
   * to SeriesChangeEvent.FinishingCompute or SeriesChangeEvent.FinishingLoading from
   * sec's ser and computeFrom(0) at once.
   */
  private def createCombinedSer(freq: TFreq): Option[QuoteSer] = {
    (freq.unit match {
        case TUnit.Day | TUnit.Week | TUnit.Month | TUnit.Year => serOf(TFreq.DAILY)
        case _ => serOf(TFreq.ONE_MIN)
      }
    ) match {
      case Some(srcSer) =>
        if (!srcSer.isLoaded) loadSer(srcSer)

        val tarSer = new QuoteSer(this, freq)
        val combiner = new QuoteSerCombiner(srcSer, tarSer, exchange.timeZone)
        
        combiner.compute(0) // don't remove me, see notice above.
        freqToQuoteSer.put(tarSer.freq, tarSer)
        Some(tarSer)
      case None => None
    }
  }

  def putSer(ser: QuoteSer): Unit = mutex synchronized {
    freqToQuoteSer.put(ser.freq, ser)
  }

  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to varies maps.
   */
  def loadSer(ser: QuoteSer): Boolean = {
    if (ser.isInLoading) return true

    ser.isInLoading = true

    val isRealTime = ser eq realtimeSer
    // load from persistence
    val wantTime = loadSerFromPersistence(ser, isRealTime)
    // try to load from quote server
    loadFromQuoteServer(ser, wantTime, isRealTime)

    true
  }
  
  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to varies maps.
   */
  def loadMoneyFlowSer(ser: MoneyFlowSer): Boolean = {
    if (ser.isInLoading) return true

    ser.isInLoading = true

    val isRealTime = ser eq realtimeMoneyFlowSer
    // load from persistence
    val wantTime = loadMoneyFlowSerFromPersistence(ser, isRealTime)
    // try to load from quote server
    loadFromMoneyFlowServer(ser, wantTime, isRealTime)

    true
  }

  /**
   * synchronized this method to avoid conflict on variable: loadBeginning and
   * concurrent accessing to varies maps.
   */
  def loadPriceDistributionSer(ser: PriceDistributionSer): Boolean = {
    if (ser.isInLoading) return true

    ser.isInLoading = true

    val isRealTime = ser eq realtimePriceDistributionSer
    // load from persistence
    val wantTime = loadPriceDistributionSerFromPersistence(ser, isRealTime)
    // try to load from price distributions server
    loadFromPriceDistributionServer(ser, wantTime, isRealTime)

    true
  }

  def resetSers: Unit = mutex synchronized {
    _realtimeSer = null
    freqToQuoteSer.clear
    freqToMoneyFlowSer.clear
    freqToPriceDistribuSer.clear
    freqToInfoSer.clear
  }

  /**
   * @return quoteSer created and is loaded
   */
  def isSerLoaded(freq: TFreq) = {
    freqToQuoteSer.get(freq) match {
      case Some(x) => x.isLoaded
      case None => false
    }
  }

  def isSerCreated(freq: TFreq) = {
    freqToQuoteSer.get(freq).isDefined
  }

  /**
   * All quotes in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadSerFromPersistence(ser: QuoteSer, isRealTime: Boolean): Long = {
    val quotes = if (isRealTime) {

      val dailyRoundedTime = exchange.lastDailyRoundedTradingTime match {
        case Some(x) => x
        case None => TFreq.DAILY.round(System.currentTimeMillis, Calendar.getInstance(exchange.timeZone))
      }

      val cal = Calendar.getInstance(exchange.timeZone)
      cal.setTimeInMillis(dailyRoundedTime)
      log.info("Loading realtime ser from persistence of " + cal.getTime)
      Quotes1m.mintueQuotesOf(this, dailyRoundedTime)

    } else {

      ser.freq match {
        case TFreq.ONE_MIN => Quotes1m.quotesOf(this)
        case TFreq.DAILY   => Quotes1d.quotesOf(this)
        case _ => return 0L
      }

    }

    ser ++= quotes.toArray

    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!quotes.isEmpty) {
      val (first, last, isAscending) = if (quotes.head.time <= quotes.last.time)
        (quotes.head, quotes.last, true)
      else
        (quotes.last, quotes.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source? first.fromMe_? may means never load from data server
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: Quote = null
        var i = if (isAscending) 0 else quotes.length - 1
        while (i < quotes.length && i >= 0 && quotes(i).fromMe_?) {
          lastFromMe = quotes(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + (if (isRealTime) TFreq.ONE_SEC else ser.freq) + "): loaded from persistence, got quotes=" + quotes.length +
               ", loaded: time=" + last.time + ", ser size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )
      
      wantTime
    } else {
      log.info(uniSymbol + "(" + (if (isRealTime) TFreq.ONE_SEC else ser.freq) + "): loaded from persistence, got 0 quotes" + 
               ", ser size=" + ser.size + ", will try to load from data source from beginning")
      0L
    }
  }

  def loadInfoPointSer(ser : InfoPointSer) : Boolean = synchronized {
    //after resolve orm problem
    val wantTime = loadInfoPointSerFromPersistence(ser)
    loadInfoPointSerFromDataServer(ser,wantTime)
    true
  }

  private def loadInfoPointSerFromPersistence(ser: InfoPointSer): Long = {
    val id = Secs.idOf(this)
    val GI = GeneralInfos
    val IS = InfoSecs
    var time : Long = 0
    val infos = (SELECT (GI.*) FROM (GI JOIN IS) WHERE ( (GI.infoClass EQ GeneralInfo.RICH_INFO) AND (IS.sec.field EQ id) ) ORDER_BY (GI.publishTime DESC) list)
    infos map {
      info =>
      val RichInfo = new RichInfo
      RichInfo.time = info.publishTime
      RichInfo.generalInfo = info
//      RichInfo.summary = "info.summary"
//      RichInfo.content = "info.content"
//      RichInfo.content = info.content
      info.categories foreach ( cate => RichInfo.categories.append(cate))
      info.secs foreach (sec => RichInfo.secs.append(sec))
      ser.updateFrom(RichInfo)
      time = info.publishTime
    }
    time
  }

  private def loadInfoPointSerFromDataServer(ser: InfoPointSer, fromTime: Long) : Long = {
    val freq = ser.freq
    dataContractOf(classOf[RichInfoHisContract], freq) match {
      case Some(contract) =>  contract.serviceInstance() match {
          case Some(richInfoHisServer) =>
            contract.freq = if (ser eq realtimeSer) TFreq.ONE_SEC else freq
            if (contract.isRefreshable) {
              richInfoHisServer.subscribe(contract)
            }
            
            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: Reactions.Reaction = null
            reaction = {
              case TSerEvent.Loaded(ser, uniSymbol, frTime, toTime, _, _) =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            ser.isInLoading = true
            contract.fromTime = fromTime
            richInfoHisServer.loadData(List(contract))

          case _ => ser.isLoaded = true
        }
      case None => 
    }

    0L
  }

  /**
   * All values in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadMoneyFlowSerFromPersistence(ser: MoneyFlowSer, isRealTime: Boolean): Long = {
    val mfs = ser.freq match {
      case TFreq.DAILY   => MoneyFlows1d.closedMoneyFlowOf(this)
      case TFreq.ONE_MIN => MoneyFlows1m.closedMoneyFlowOf(this)
      case _ => return 0L
    }

    ser ++= mfs.toArray
    
    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!mfs.isEmpty) {
      val (first, last, isAscending) = if (mfs.head.time < mfs.last.time)
        (mfs.head, mfs.last, true)
      else
        (mfs.last, mfs.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: MoneyFlow = null
        var i = if (isAscending) 0 else mfs.length - 1
        while (i < mfs.length && i >= 0 && mfs(i).fromMe_?) {
          lastFromMe = mfs(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got MoneyFlows=" + mfs.length +
               ", loaded: time=" + last.time + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }

  /**
   * All values in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadPriceDistributionSerFromPersistence(ser: PriceDistributionSer, isRealTime: Boolean): Long = {
    val pcs = ser.freq match {
      case TFreq.DAILY   => PriceDistributions.closedDistribuOf(this)
      case _ => return 0L
    }

    ser ++= pcs.values.toArray

    /**
     * get the newest time which DataServer will load price distributions after this time
     * if price distributions is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!pcs.isEmpty) {
      val (max,min) = this.getMaxMinValue(pcs.keys.toArray)
      val first = pcs(min)
      val last = pcs(max)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: PriceCollection = null
        var maxTime = 0L
        pcs.values.foreach{pc =>
          if (pc.fromMe_? && pc.time >= maxTime){
            lastFromMe = pc;
            maxTime = pc.time
          }
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got Price distributions=" + pcs.size +
               ", loaded: time=" + last.time + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }

  private def getMaxMinValue(times: Array[Long]) = {
    var max = 0L
    var min = System.currentTimeMillis
    times foreach {value =>
      if (value > max) max = value
      if (value < min) min = value
    }

    (max, min)
  }

  def loadInfoSerFromPersistence(ser: InfoSer): Long = {
    val infos = ser.freq match {
      case TFreq.DAILY   => Infos1d.all()
      case TFreq.ONE_MIN => Infos1m.all()
      case _ => return 0L
    }

    ser ++= infos.toArray

    /**
     * get the newest time which DataServer will load quotes after this time
     * if quotes is empty, means no data in db, so, let newestTime = 0, which
     * will cause loadFromSource load from date: Jan 1, 1970 (timeInMills == 0)
     */
    if (!infos.isEmpty) {
      val (first, last, isAscending) = if (infos.head.time < infos.last.time)
        (infos.head, infos.last, true)
      else
        (infos.last, infos.head, false)

      ser.publish(TSerEvent.Refresh(ser, uniSymbol, first.time, last.time))

      // should load earlier quotes from data source?
      val wantTime = if (first.fromMe_?) 0 else {
        // search the lastFromMe one, if exist, should re-load quotes from data source to override them
        var lastFromMe: org.aiotrade.lib.info.model.Info = null
        var i = if (isAscending) 0 else infos.length - 1
        while (i < infos.length && i >= 0 && infos(i).fromMe_?) {
          lastFromMe = infos(i)
          if (isAscending) i += 1 else i -= 1
        }

        if (lastFromMe != null) lastFromMe.time - 1 else last.time
      }

      log.info(uniSymbol + "(" + ser.freq + "): loaded from persistence, got Infos=" + infos.length +
               ", loaded: time=" + last.time + ", size=" + ser.size +
               ", will try to load from data source from: " + wantTime
      )

      wantTime
    } else 0L
  }

  /**
   * @Note Since we use same quoteServer and contract to load varies freq data , we should guarantee that quoteServer is thread safe
   * 
   * @todo If there is no QuoteServer for this sec, who will fire the TSerEvent.Loaded to avoid evt chain broken?
   */
  private def loadFromQuoteServer(ser: QuoteSer, fromTime: Long, isRealTime: Boolean) {
    val freq = if (isRealTime) TFreq.ONE_SEC else ser.freq
    
    dataContractOf(classOf[QuoteContract], freq) match {
      case Some(contract) =>
        log.info("Quote Contract's identityHashCode=" + System.identityHashCode(contract))
        contract.serviceInstance() match {
          case Some(quoteServer) =>
            contract.srcSymbol = quoteServer.toSrcSymbol(uniSymbol)
            contract.freq = freq

            if (contract.isRefreshable) {
              quoteServer.subscribe(contract)
            }

            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: Reactions.Reaction = null
            reaction = {
              case TSerEvent.Loaded(serx, _, _, _, _, _) if serx eq ser =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            contract.fromTime = fromTime
            quoteServer.loadData(List(contract))

          case _ => ser.isLoaded = true
        }

      case _ => ser.isLoaded = true
    }
  }

  private def loadFromMoneyFlowServer(ser: MoneyFlowSer, fromTime: Long, isRealTime: Boolean) {
    val freq = if (isRealTime) TFreq.ONE_SEC else ser.freq
    
    dataContractOf(classOf[MoneyFlowContract], freq) match {
      case Some(contract) =>
        contract.serviceInstance() match {
          case Some(moneyFlowServer) =>
            contract.srcSymbol = moneyFlowServer.toSrcSymbol(uniSymbol)
            contract.freq = freq

            if (contract.isRefreshable) {
              moneyFlowServer.subscribe(contract)
            }

            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: Reactions.Reaction = null
            reaction = {
              case TSerEvent.Loaded(serx, uniSymbol, frTime, toTime, _, _) if serx eq ser =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            contract.fromTime = fromTime
            moneyFlowServer.loadData(List(contract))

          case _ => ser.isLoaded = true
        }

      case _ => ser.isLoaded = true
    }
  }

  def isIndex = Exchange.isIndex(this)
  def isStock = Exchange.isStock(this)

  private def loadFromPriceDistributionServer(ser: PriceDistributionSer, fromTime: Long, isRealTime: Boolean) {
    val freq = if (isRealTime) TFreq.ONE_SEC else ser.freq

    dataContractOf(classOf[PriceDistributionContract], freq) match {
      case Some(contract) =>
        contract.serviceInstance() match {
          case Some(priceDistributionServer) =>
            contract.srcSymbol = priceDistributionServer.toSrcSymbol(uniSymbol)
            contract.freq = freq

            if (contract.isRefreshable) {
              priceDistributionServer.subscribe(contract)
            }

            // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
            var reaction: Reactions.Reaction = null
            reaction = {
              case TSerEvent.Loaded(serx, uniSymbol, frTime, toTime, _, _) if serx eq ser =>
                reactions -= reaction
                deafTo(ser)
                ser.isLoaded = true
            }
            reactions += reaction
            listenTo(ser)

            contract.fromTime = fromTime
            priceDistributionServer.loadData(List(contract))

          case _ => ser.isLoaded = true
        }

      case _ => ser.isLoaded = true
    }
  }

  def uniSymbol: String = if (secInfo != null) secInfo.uniSymbol else ""
  def uniSymbol_=(uniSymbol: String) {
    if (secInfo != null) {
      secInfo.uniSymbol = uniSymbol
    }
  }

  def name: String = {
    if (secInfo != null) secInfo.name else uniSymbol
  }

  def stopAllDataServer {
    for (contract <- selfContent.lookupDescriptors(classOf[DataContract[DataServer[_]]]);
         server <- contract.serviceInstance()
    ) {
      server.stopRefresh
    }
  }

  override def toString: String = {
    "Sec(Company=" + company + ", info=" + secInfo + ")"
  }

  def subscribeTickerServer(startRefresh: Boolean = true): Option[TickerServer] = {
    if (tickerContract.serviceClassName == null) {
      for (quoteContract <- dataContractOf(classOf[QuoteContract], defaultFreq);
           quoteServer <- quoteContract.serviceInstance();
           klassName <- quoteServer.classNameOfTickerServer
      ) {
        tickerContract.serviceClassName = klassName
      }
    }

    tickerServer map {server =>
      if (!startRefresh) server.stopRefresh
      
      // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
      //this is not always true, for DJI, src code: DJI while unisymbol is ^DJI
      tickerContract.srcSymbol = server.toSrcSymbol(uniSymbol)
      if (!server.isContractSubsrcribed(tickerContract)) {
        server.subscribe(tickerContract)
      }

      if (startRefresh) server.startRefresh

      server
    }
  }

  def unSubscribeTickerServer {
    if (tickerServer.isDefined && tickerContract != null) {
      tickerServer.get.unsubscribe(tickerContract)
    }
  }

  def isTickerServerSubscribed: Boolean = {
    tickerServer.isDefined && tickerServer.get.isContractSubsrcribed(tickerContract)
  }

  def subscribeInfoDataServer(startRefresh: Boolean = true): Option[RichInfoDataServer] = {
    richInfoServer map {server =>
      // always set uniSymbol, since _tickerContract may be set before secInfo.uniSymbol
      richInfoContract.srcSymbol = uniSymbol
      if (!startRefresh) server.stopRefresh

      if (!server.isContractSubsrcribed(richInfoContract)) {
        server.subscribe(richInfoContract)
      }

      if (startRefresh) server.startRefresh

      server
    }
  }

  def unsubscribeInfoDataServer {
    if (richInfoServer.isDefined & richInfoContract != null){
      richInfoServer.get.unsubscribe(richInfoContract)
    }
  }

  def isInfoDataServerSubcribed : Boolean = {
    richInfoServer.isDefined && richInfoServer.get.isContractSubsrcribed(richInfoContract)
  }

  override def equals(that: Any) = that match {
    case x: Sec => this.id == x.id
    case _ => false
  }

  def compare(that: Sec): Int = {
    this.exchange.compare(that.exchange) match {
      case 0 => 
        val s1 = this.uniSymbol
        val s2 = that.uniSymbol
        if (s1 == "_" && s2 == "_") 0
        else if (s1 == "_") -1
        else if (s2 == "_") 1
        else {
          val s1s = s1.split('.')
          val s2s = s2.split('.')
          s1s(0).compareTo(s2s(0))
        }
      case x => x
    }
  }

  /**
   * store latest snap info
   */
  lazy val secSnap = new SecSnap(this)
}

class SecSnap(val sec: Sec) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val ONE_DAY = 24 * 60 * 60 * 1000

  var newTicker: Ticker = _
  var lastTicker: Ticker = _

  var dayQuote: Quote = _
  var minQuote: Quote = _

  var dayMoneyFlow: MoneyFlow = _
  var minMoneyFlow: MoneyFlow = _

  var priceCollection = new PriceCollection

  // it's not thread safe, but we know it won't be accessed parallel, @see sequenced accessing in setByTicker(ticker)
  private val cal = Calendar.getInstance(sec.exchange.timeZone) 

  final def setByTicker(ticker: Ticker): SecSnap = {
    newTicker = ticker
    
    val time = ticker.time
    checkLastTickerAt(time)
    checkDayQuoteAt(time)
    checkMinQuoteAt(time)
    checkDayMoneyFlowAt(time)
    checkMinMoneyFlowAt(time)
    checkPriceDistributionAt(time)
    this
  }

  private def checkDayQuoteAt(time: Long): Quote = {
    assert(Secs.idOf(sec).isDefined, "Sec: " + sec + " is transient")
    val rounded = TFreq.DAILY.round(time, cal)
    dayQuote match {
      case oldone: Quote if oldone.time == rounded =>
        oldone.lastModify = time
        oldone
      case _ => // day changes or null
        val newone = new Quote
        newone.time = rounded
        newone.lastModify = time
        newone.sec = sec
        newone.prevClose = newTicker.prevClose
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.DAILY, newone)
        log.info("Created new daily quote for " + sec.uniSymbol)

        dayQuote = newone
        newone
    }
  }

  private def checkDayMoneyFlowAt(time: Long): MoneyFlow = {
    assert(Secs.idOf(sec).isDefined, "Sec: " + sec + " is transient")
    val rounded = TFreq.DAILY.round(time, cal)
    dayMoneyFlow match {
      case oldone: MoneyFlow if oldone.time == rounded =>
        oldone
      case _ => // day changes or null
        val newone = new MoneyFlow
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.DAILY, newone)
        log.info("Created new daily moneyflow for " + sec.uniSymbol)

        dayMoneyFlow = newone
        newone
    }
  }

  private def checkPriceDistributionAt(time: Long): PriceCollection  = {
    assert(Secs.idOf(sec).isDefined, "Sec: " + sec + " is transient")
    val rounded = TFreq.DAILY.round(time, cal)
    priceCollection match {
      case oldOne: PriceCollection if oldOne.time == rounded => oldOne
      case _ =>
        val newone = new PriceCollection
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewPriceDistribution(TFreq.DAILY, newone)

        priceCollection = newone
        newone
    }
  }

  private def checkMinQuoteAt(time: Long): Quote = {
    val rounded = TFreq.ONE_MIN.round(time, cal)
    minQuote match {
      case oldone: Quote if oldone.time == rounded =>
        oldone.lastModify = time
        oldone
      case _ => // minute changes or null
        val newone = new Quote
        newone.time = rounded
        newone.lastModify = time
        newone.sec = sec
        newone.prevClose = newTicker.prevClose
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewQuote(TFreq.ONE_MIN, newone)

        minQuote = newone
        newone
    }
  }

  private def checkMinMoneyFlowAt(time: Long): MoneyFlow = {
    val rounded = TFreq.ONE_MIN.round(time, cal)
    minMoneyFlow match {
      case oldone: MoneyFlow if oldone.time == rounded =>
        oldone
      case _ => // minute changes or null
        val newone = new MoneyFlow
        newone.time = rounded
        newone.sec = sec
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        sec.exchange.addNewMoneyFlow(TFreq.ONE_MIN, newone)
        
        minMoneyFlow = newone
        newone
    }
  }

  /**
   * @return lastTicker of this day
   */
  private def checkLastTickerAt(time: Long): Ticker = {
    val rounded = TFreq.DAILY.round(time, cal)
    lastTicker match {
      case oldone: Ticker if oldone.time >= rounded && oldone.time < rounded + ONE_DAY =>
        newTicker.isDayFirst = false
        oldone
      case _ => // not today's one or null
        val newone = Tickers.lastTickerOf(sec, rounded)
        lastTicker = newone
        if (lastTicker.isTransient) {
          newTicker.isDayFirst = true
        }
        newone
    }
  }
}

object Sec {
  trait Kind
  object Kind {
    case object Stock extends Kind
    case object Index extends Kind
    case object Option extends Kind
    case object Future extends Kind
    case object FutureOption extends Kind
    case object Currency extends Kind
    case object Bag extends Kind
    case object Bonds extends Kind
    case object Equity extends Kind

    def withName(name: String): Kind = {
      name match {
        case "Stock" => Stock
        case "Index" => Index
        case "Option" => Option
        case "Future" => Future
        case "FutureOption" => FutureOption
        case "Currency" => Currency
        case "Bag" => Bag
        case _ => null
      }
    }
  }  
}


// --- table
object Secs extends CRCLongPKTable[Sec] {
  val exchange = "exchanges_id" BIGINT() REFERENCES(Exchanges)

  val validFrom = "validFrom" BIGINT() 
  val validTo = "validTo" BIGINT()

  val company = "companies_id" BIGINT() REFERENCES(Companies)
  def companyHists = inverse(Companies.sec)

  val secInfo = "secInfos_id" BIGINT() REFERENCES(SecInfos)
  def secInfoHists = inverse(SecInfos.sec)
  val secStatus = "secStatuses_id" BIGINT() REFERENCES(SecStatuses)
  def secStatusHists = inverse(SecStatuses.sec)

  val secIssue = "secIssues_id" BIGINT() REFERENCES(SecIssues)
  def secDividends = inverse(SecDividends.sec)

  def dailyQuotes = inverse(Quotes1d.sec)
  def dailyMoneyFlow = inverse(MoneyFlows1d.sec)

  def minuteQuotes = inverse(Quotes1m.sec)
  def minuteMoneyFlow = inverse(MoneyFlows1m.sec)

  def priceDistribution = inverse(PriceDistributions.sec)

  def tickers = inverse(Tickers.sec)
  def executions = inverse(Executions.sec)
}

