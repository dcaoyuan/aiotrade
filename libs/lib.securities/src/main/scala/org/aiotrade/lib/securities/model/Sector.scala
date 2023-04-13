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

import scala.collection
import scala.collection.mutable
import scala.collection.immutable
import java.util.Calendar
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.SectorMoneyFlowSer
import org.aiotrade.lib.util.ValidTime
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.SerProvider
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.dataserver.MoneyFlowContract
import org.aiotrade.lib.securities.PersistenceManager
import ru.circumflex.orm._

/**
 * Fullcode is defined as two parts:
 *   1st part is caterogy (6 chars),
 *   2nd part is code/subcategory (max 20 chars)
 *
 * @author Caoyuan Deng
 */
final class Sector extends LightSector with SerProvider with Ordered[Sector] {
  private val log = Logger.getLogger(getClass.getName)
  private val mutex = new AnyRef()

  lazy val sectorSnap = new SectorSnap(this)
  private var _realtimeMoneyFlowSer: SectorMoneyFlowSer = _
  private lazy val freqToMoneyFlowSer = mutable.Map[TFreq, SectorMoneyFlowSer]()

  override def hashCode = id.hashCode

  override def equals(that: Any) = that match {
    case x: Sector => this.crckey == x.crckey
    case _ => false
  }

  def getSectorSymbol = {
    if (crckey.startsWith(Sector.Category.TDXIndustries(0))){
      val ss = crckey.split('.')
      if (ss.length == 3) ss(1) + "." + ss(2) else crckey
    } else crckey
  }

  type T = SectorMoneyFlowSer
  type C = MoneyFlowContract

  def isSerCreated(freq: TFreq) = {
    freqToMoneyFlowSer.get(freq).isDefined
  }

  def updateSer(freq: TFreq, moneyFlow: MoneyFlow) {
    freq match {
      case TFreq.ONE_MIN =>
        if (isSerCreated(TFreq.ONE_MIN)) {
          serOf(TFreq.ONE_MIN) foreach (_.updateFrom(moneyFlow))
        }
      case TFreq.DAILY =>
        if (isSerCreated(TFreq.DAILY)) {
          serOf(TFreq.DAILY) foreach (_.updateFrom(moneyFlow))
        }
    }
  }

  def realtimeMoneyFlowSer = mutex synchronized {
    if (_realtimeMoneyFlowSer == null) {
      _realtimeMoneyFlowSer = new SectorMoneyFlowSer(this, TFreq.ONE_MIN)
      freqToMoneyFlowSer.put(TFreq.ONE_SEC, _realtimeMoneyFlowSer)
    }
    _realtimeMoneyFlowSer
  }

  /**
   * All values in persistence should have been properly rounded to 00:00 of exchange's local time
   */
  def loadMoneyFlowSerFromPersistence(ser: SectorMoneyFlowSer, isRealTime: Boolean): Long = {
    val mfs = ser.freq match {
      case TFreq.DAILY   => SectorMoneyFlows1d.closedMoneyFlowOf(this)
      case TFreq.ONE_MIN => SectorMoneyFlows1m.closedMoneyFlowOf(this)
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
   * Load sers, can be called to load ser whenever
   * If there is already a dataServer is running and not finished, don't load again.
   * @return boolean: if run sucessfully, ie. load begins, return true, else return false.
   */
  def loadSer(ser: SectorMoneyFlowSer): Boolean = {
    if (ser.isInLoading) return true

    ser.isInLoading = true

    val isRealTime = ser eq realtimeMoneyFlowSer
    // load from persistence
    val wantTime = loadMoneyFlowSerFromPersistence(ser, isRealTime)

    true
  }

  def putSer(ser: SectorMoneyFlowSer) = mutex synchronized {
    freqToMoneyFlowSer.put(ser.freq, ser)
  }

  def resetSers: Unit = mutex synchronized {
    _realtimeMoneyFlowSer = null
    freqToMoneyFlowSer.clear
  }

  def uniSymbol: String = crckey
  def uniSymbol_=(uniSymbol: String) {crckey = uniSymbol}

  def stopAllDataServer {
    for (contract <- content.lookupDescriptors(classOf[DataContract[DataServer[_]]]);
         server <- contract.serviceInstance()
    ) {
      server.stopRefresh
    }
  }

  def serOf(freq: TFreq): Option[SectorMoneyFlowSer] = mutex synchronized {
    freq match {
      case TFreq.ONE_SEC => Some(_realtimeMoneyFlowSer)
      case _ => freqToMoneyFlowSer.get(freq) match {
          case None => freq match {
              case TFreq.ONE_MIN | TFreq.DAILY =>
                val x = new SectorMoneyFlowSer(this, freq)
                freqToMoneyFlowSer.put(freq, x)
                Some(x)
              case _ => None
            }
          case some => some
        }
    }
  }

  var description = ""
  private lazy val _content = PersistenceManager().restoreContent(uniSymbol)
  def content = _content

  def serProviderOf(uniSymbol: String): Option[Sector] = {
    Exchange.sectorOf(uniSymbol)
  }

  def compare(that: Sector): Int = {
    this.crckey.compare(that.crckey)
  }
}

object Sector {

  // max 6 chars
  object Category {

    // security kind
    val Kind = "KIND"

    // exchange
    val Exchange = "EXCHAN"

    // industries
    val IndustryA = "008001"
    val IndustryB = "008002"
    val IndustryC = "008003"
    val IndustryD = "008004"
    val IndustryE = "008005"

    // tdx industries
    val TDXIndustries = Array("008011","008012","008013","008014","008015","008018")

    // boards
    val Board = "BOARD"

    // joint
    val Joint = "JOINT"

    // custom
    val Custom = "CUSTOM"
  }

  // --- subcategories/code

  object Kind {
    val Index = "INDEX"                       // 指数
    val Stock = "STOCK"                       // 股票
    val Fund = "FUND"                         // 基金
    val Bond = "BOND"                         // 债券
    val Warrant = "WARRANT"                   // 权证
    val Future = "FUTURE"                     // 期货
    val Forex = "FOREX"                       // 外汇
    val Option = "OPTION"                     // 期权
    val Treasury = "TREASURY"                 // 国债
    val AdditionalShareOffer = "ADDSHAOFFER"  // 增发
    val ConvertibleBond = "CONVBOND"          // 可转换债券
    val TreasuryRepurchase = "TREASREP"       // 国债回购
  }

  // --- code of 'board'
  object Board {
    val Main = "MAIN"

    val AShare = "ASHARE"
    val BShare = "BSHARE"
    val HShare = "HSHARE"

    val SME = "SME" // Small and Medium-sized Enterprised board
    val GEM = "GEM" // Growth Enterprises Market board
  }

  lazy val sectorToSecValidTimes: collection.Map[String, collection.Seq[ValidTime[Sec]]] = {
    Sectors.sectorToSecValidTimes
  }

  lazy val secToSectorValidTimes: collection.Map[String, collection.Seq[ValidTime[Sector]]] = {
    Sectors.secToSectorValidTimes
  }

  def toKey(category: String, code: String): String = category + "." + code
  def toCategoryCode(key: String): (String, String) = {
    val sep = key.indexOf('.')
    if (sep > 0) {
      val category = key.substring(0, sep)
      val code = key.substring(sep + 1, key.length)
      (category, code)
    } else {
      (key, null)
    }
  }

  def setParent(sectors: collection.Map[String, Sector]) = {
    for((key, sector) <- sectors){
      if (sector.childrenString.trim != ""){
        for(key <- sector.childrenString.split('+')){
          sectors.get(key) match{
            case Some(sect) => sect.parent = sector
            case None =>
          }
        }
      }
    }
  }

  def createIndexSector(code: String, name: String): Sector = {
    val sector = new Sector
    val category = Category.TDXIndustries(0)
    val crckey = category + "." + code
    sector.category = category
    sector.code = code
    sector.name = name
    sector.crckey = crckey
    sector.id = sector.id
    sector
  }

  def allSectors = sectorToSecValidTimes.keys

  def sectorsOf(category: String) = Sectors.sectorsOf(category)
  def sectorsOf = Sectors.sectorsOf

  def secsOf(sector: Sector): Seq[Sec] = Sectors.secsOf(sector)
  def secsOf(key: String): Seq[Sec] = withKey(key) match {
    case Some(sector) => secsOf(sector)
    case None => Nil
  }

  def withKey(key: String): Option[Sector] = Sectors.withKey(key)
  def withCategoryCode(category: String, code: String): Option[Sector] = Sectors.withCategoryCode(category, code)

  def cnSymbolToSectorKey(uniSymbol: String): Seq[String] = {
    var sectorKeys = List[String]()
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol, "SS") =>
        sectorKeys ::= toKey(Category.Exchange, "SS")

        if (symbol.startsWith("000")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Index)
        } else if (symbol.startsWith("009") || symbol.startsWith("010") || symbol.startsWith("020")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury)
        } else if (symbol.startsWith("60")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("90")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("500") || symbol.startsWith("510")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Fund)
        } else if (symbol.startsWith("580")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
        } else if (symbol.startsWith("100") || symbol.startsWith("110") || symbol.startsWith("112") || symbol.startsWith("113")) {
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("120") || symbol.startsWith("129")) { // enterprised bond
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("1")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        }

      case Array(symbol, "SZ") =>
        sectorKeys ::= toKey(Category.Exchange, "SZ")

        if (symbol.startsWith("00")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("03")) { // 认购或认沽权证
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("07")) {
          sectorKeys ::= toKey(Category.Kind, Kind.AdditionalShareOffer)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("08")) { // 配股权证
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.AShare)
        } else if (symbol.startsWith("101")) { // 国债券挂牌分销
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury)
        } else if (symbol.startsWith("109")) { // 地方政府债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("10")) {  // 国债现货
          sectorKeys ::= toKey(Category.Kind, Kind.Treasury)
        } else if (symbol.startsWith("111")) { // 企业债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("112")) { // 公司债券
          sectorKeys ::= toKey(Category.Kind, Kind.Bond)
        } else if (symbol.startsWith("115")) { // 分离交易型可转债
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("12")) {
          sectorKeys ::= toKey(Category.Kind, Kind.ConvertibleBond)
        } else if (symbol.startsWith("13")) {
          sectorKeys ::= toKey(Category.Kind, Kind.TreasuryRepurchase)
        } else if (symbol.startsWith("15") || symbol.startsWith("16") || symbol.startsWith("18")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Fund)
        } else if (symbol.startsWith("20")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("28")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.BShare)
        } else if (symbol.startsWith("30")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Stock)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("37")) {
          sectorKeys ::= toKey(Category.Kind, Kind.AdditionalShareOffer)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("38")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Warrant)
          sectorKeys ::= toKey(Category.Board, Board.GEM)
        } else if (symbol.startsWith("39")) {
          sectorKeys ::= toKey(Category.Kind, Kind.Index)
        }
      case _ =>
    }
    sectorKeys
  }

  def apply(category: String, code: String) = new Sector()

  // --- simple test
  def main(args: Array[String]) {
    try {
//      val secsHolder = SELECT(Secs.*) FROM (Secs) list()
//      val t0 = System.currentTimeMillis
//      val sectorToSecValidTimes = Sectors.sectorToSecValidTimes
//      sectorToSecValidTimes foreach println

//      val secsHolder = SELECT(Secs.*) FROM (Secs) list()
//      val sectorsHolder = SELECT(Sectors.*) FROM (Sectors) list()
      val t0 = System.currentTimeMillis
      val secToSectorValidTimes = Sectors.secToSectorValidTimes
      secToSectorValidTimes foreach println

      println("Finished in " + (System.currentTimeMillis - t0) / 1000.0 + "s")
      System.exit(0)
    } catch {
      case ex: Throwable => ex.printStackTrace; System.exit(1)
    }
  }
}


// --- table
object Sectors extends CRCLongPKTable[Sector] {
  private val log = Logger.getLogger(this.getClass.getName)

  val category = "category" VARCHAR(6) DEFAULT("''")
  val code = "code" VARCHAR(20) DEFAULT("''")
  val name = "name" VARCHAR(60) DEFAULT("''")
  val childrenString = "children" VARCHAR(2048) DEFAULT("''")

  def secs = inverse(SectorSecs.sector)

  val categoryIdx = getClass.getSimpleName + "_category_idx" INDEX(category.name)
  val codeIdx = getClass.getSimpleName + "_code_idx" INDEX(code.name)


  // --- helpers:

  private[model] def allSectors: Seq[String] = {
    val res = try {
      SELECT (Sectors.*) FROM (Sectors) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res map (_.key)
  }

  private[model] def sectorsOf(category: String): Seq[Sector] = {
    try {
      SELECT (Sectors.*) FROM (Sectors) WHERE (Sectors.category EQ category) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  private[model] def sectorsOf(): Seq[Sector] = {
    SELECT (Sectors.*) FROM (Sectors) list()
  }

  private[model] def secsOf(sector: Sector): Seq[Sec] = {
    try {
      SELECT (Secs.*) FROM (SectorSecs JOIN Secs) WHERE (SectorSecs.sector.field EQ Sectors.idOf(sector)) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
  }

  private[model] def withKey(key: String): Option[Sector] = {
    val (category, code) = Sector.toCategoryCode(key)
    withCategoryCode(category, code)
  }

  private[model] def withCategoryCode(category: String, code: String): Option[Sector] = {
    try {
      SELECT (Sectors.*) FROM (Sectors) WHERE ((Sectors.category EQ category) AND (Sectors.code EQ code)) unique()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); None
    }
  }

  /**
   * @Note: This method can only be called after all sectors have been selected and holded in Memory
   */
  private[model] def secToSectorValidTimes = {
    val result = mutable.HashMap[String, mutable.ListBuffer[ValidTime[Sector]]]()

    val sectorsHolder = try {
      SELECT(Sectors.*) FROM (Sectors) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val secsHolder = try {
      SELECT(Secs.*) FROM (Secs) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val sectorSecs = try {
      SELECT (SectorSecs.*) FROM (SectorSecs) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    for (sectorSec <- sectorSecs) {
      if (sectorSec.sector != null && sectorSec.sec != null) {
        val key = sectorSec.sec.crckey
        val validTime = sectorSec.toSectorValidTime
        val validTimes = result.get(key) match {
          case None =>
            val validTimes = mutable.ListBuffer[ValidTime[Sector]]()
            result += (key -> validTimes)
            validTimes
          case Some(x) => x
        }

        validTimes += validTime
      } else {
        log.warning("SectorSec: " + sectorSec + " has null sec. The id of this sectorSec is: " + SectorSecs.idOf(sectorSec))
      }
    }

    result
  }

  /**
   * @Note: This method can only be called after all secs have been selected and holded in Memory
   */
  private[model] def sectorToSecValidTimes = {
    val result = mutable.HashMap[String, mutable.ListBuffer[ValidTime[Sec]]]()

    val secsHolder = try {
      SELECT(Secs.*) FROM (Secs) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val sectorsHolder = try {
      SELECT(Sectors.*) FROM (Sectors) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    val sectorSecs = try {
      SELECT (SectorSecs.*) FROM (SectorSecs) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    for (sectorSec <- sectorSecs) {
      if (sectorSec.sec ne null) {
        val key = sectorSec.sector.key
        val validTime = sectorSec.toSecValidTime
        val validTimes = result.get(key) match {
          case None =>
            val validTimes = mutable.ListBuffer[ValidTime[Sec]]()
            result += (key -> validTimes)
            validTimes
          case Some(x) => x
        }

        validTimes += validTime
      } else {
        log.warning("SectorSec: " + sectorSec + " has null sec. The id of this sectorSec is: " + SectorSecs.idOf(sectorSec))
      }
    }

    result
  }
}

class SectorSnap(val sector: Sector, exchange: Exchange = Exchange.SS) {
  private val log = Logger.getLogger(this.getClass.getName)

  var dayMoneyFlow: MoneyFlow = _
  var minMoneyFlow: MoneyFlow = _

  // it's not thread safe, but we know it won't be accessed parallel, @see sequenced accessing in setByTicker(ticker)
  private val cal = Calendar.getInstance(exchange.timeZone)

  final def setByMoneyFlow(mf: MoneyFlow): SectorSnap = {
    val time = mf.lastModify
    checkDayMoneyFlowAt(time)
    checkMinMoneyFlowAt(time)
    this
  }

  private def checkDayMoneyFlowAt(time: Long): MoneyFlow = {
    val rounded = TFreq.DAILY.round(time, cal)
    dayMoneyFlow match {
      case oldone: MoneyFlow if oldone.time == rounded =>
        oldone
      case _ => // day changes or null
        val newone = new MoneyFlow
        newone.time = rounded
        newone.lastModify = time
        newone.sector = sector
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        exchange.addNewSectorMoneyFlow(TFreq.DAILY, newone)
        log.info("Created new daily moneyflow for " + sector.uniSymbol)

        dayMoneyFlow = newone
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
        newone.lastModify = time
        newone.sector = sector
        newone.unclosed_!
        newone.justOpen_!
        newone.fromMe_!
        newone.isTransient = true
        exchange.addNewSectorMoneyFlow(TFreq.ONE_MIN, newone)
        log.info("Created new minute moneyflow for " + sector.uniSymbol)

        minMoneyFlow = newone
        newone
    }
  }
}
