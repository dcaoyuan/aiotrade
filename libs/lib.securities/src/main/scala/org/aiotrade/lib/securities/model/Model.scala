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
import org.aiotrade.lib.info.model.ContentCategories
import org.aiotrade.lib.math.timeseries.TFreq
import ru.circumflex.orm._
import scala.collection.immutable.TreeMap
import scala.collection.mutable
import scala.actors.Scheduler

/**
 * -- Dump schema
 * mysqldump5 -uroot --no-data --database aiotrade > aiotrade.mysql
 * -- Create db
 * mysqladmin create target_db_name
 * mysql -uroot target_db_name < aiotrade.mysql
 *
 *  SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx)
 *  SELECT * FROM bid_ask AS a WHERE a.time = (SELECT max(time) FROM bid_ask WHERE isBid = a.isBid AND idx = a.idx AND intraDay = 2) AND intraDay = 2
 */
object Model {

  def warmUp {
    val companies = (SELECT (Companies.*) FROM Companies list)
    val exchanges = (SELECT (Exchanges.*) FROM Exchanges list)
    val categories = (SELECT (ContentCategories.*) FROM ContentCategories list)
    val secinfos = (SELECT (SecInfos.*, Secs.*) FROM (SecInfos JOIN Secs) list) // @todo got exception that secs primary key is null
  }

  val secs = mutable.Map[String, Sec]()
  // holding strong reference of exchange
  var exchanges: Seq[Exchange] = Nil
  
  def main(args: Array[String]) {
    try {
      val symbols = Set("000008.SZ", "600004.SS", "000004.SZ", "000024.SZ", "000010.SZ", "000015.SZ", "000023.SZ", "000019.SZ", "000030.SZ", "000002.SZ", "000012.SZ", "000013.SZ", "000006.SZ", "000018.SZ", "600028.SS", "600019.SS", "000009.SZ", "000001.SS", "600030.SS", "600005.SS", "600020.SS", "000025.SZ", "600016.SS"," 600010.SS", "600022.SS", "000003.SZ", "600003.SS", "000029.SZ")

      val a = Exchange.uniSymbolToSec
      println(a)
      val secs = (symbols map Exchange.secOf).flatten
      println(secs)
      //temporaryTest
      //test
      //createSamples

      System.exit(0)
    } catch {
      case ex: Throwable => ex.printStackTrace; System.exit(-1)
    }
  }

  private def temporaryTest {
    val xs = Exchanges.all()
    xs foreach (x => println(x.uniSymbolToLastTicker))

    xs foreach {x =>
      val cal = Calendar.getInstance(x.timeZone)
      val rounded = TFreq.DAILY.round(System.currentTimeMillis, cal)

      println(Tickers.lastTickersOf(rounded))
    }
  }

  private def test {
    schema
    sampleExchanges
    (0 until 5) foreach testSave
    testSelect
  }

  def createSamples = {
    schema
    sampleExchanges
    sampleSecs
    COMMIT
    Exchange.allExchanges map (x => 
      Exchange.symbolsOf(x).mkString(",")
    ) foreach println
  }

  def schema {
    val tables = List(
      Companies, Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, Executions
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))
  }

  private def testSave(i: Int) {
    val com = new Company
    com.listDate = System.currentTimeMillis
    com.shortName = "abc"
    Companies.save(com)

    println("company's listDate: " + com.listDate)

    val secInfo = new SecInfo
    secInfo.uniSymbol = "000001.SS"
    secInfo.name = "???A"
    SecInfos.save(secInfo)

    val sec = new Sec
    sec.company = com
    sec.secInfo = secInfo
    sec.exchange = Exchange.SS
    Secs.save_!(sec)

    com.sec = sec
    secInfo.sec = sec
    Companies.update(com)
    SecInfos.update(secInfo)

    val cal = Calendar.getInstance
    val quote1d = new Quote
    //quote1d.id := 10000L
    quote1d.sec = sec
    quote1d.time = cal.getTimeInMillis
    quote1d.open = 1
    Quotes1d.save(quote1d)

    cal.add(Calendar.DAY_OF_YEAR, 1)
    val quote1da = new Quote
    quote1da.sec = sec
    quote1da.time = cal.getTimeInMillis
    quote1da.open = 1
    Quotes1d.save(quote1da)

    val quote1m = new Quote
    quote1m.sec = sec
    quote1m.open = 1
    quote1m.time = System.currentTimeMillis
    Quotes1m.save(quote1m)

    def makeTicker = {
      val ticker = new Ticker
      ticker.sec = sec
      ticker.time = System.currentTimeMillis
      val bidAskDepth = 10
      val bidAsks = new Array[Double](bidAskDepth * 4)
      ticker.bidAsks = bidAsks
      Tickers.save(ticker)
    }

    for (i <- 0 until 10) makeTicker
  }

  private def testSelect {
    val ticker = Tickers.get(1).get
    val decodedBidAsks = ticker.bidAsks
    val depth = decodedBidAsks.length / 4
    println("Depth of bid ask: " + depth)
    
    val quote1d = Quotes1d.get(1).get

    val co = Companies

    co.criteria.add(co.shortName like "a%").list foreach (c =>
      println(c.shortName)
    )


//    val ci = CompanyIndustries
//    val s1 = SELECT (co.*, ci.*) FROM (co JOIN ci) WHERE (co.shortName LIKE "a%") ORDER_BY (co.shortName ASC) list
//    
//    s1 foreach println

    val com = Companies.get(1).get
    Companies.sec(com) // fetch com.sec
    println("com: " + com.shortName + ", com.sec: " + com.sec)
//    println("com's industries: " + (Companies.industries(com) map (CompanyIndustries.industry(_).getOrElse(null))))
    
    val sec = Secs.get(1).get

    val quotes = Secs.dailyQuotes(sec)
    println("sec's Quote: " + quotes)
    com.sec.dailyQuotes ++= quotes
    println("sec's Quote: " + com.sec.dailyQuotes)

    fetchAllSecs
  }

  private def fetchAllSecs {
    /* .prefetch(Sec.secInfo.asInstanceOf[Association[Any, Any]]) */
    val s = Secs
    val i = SecInfos
    (SELECT (s.*, i.*) FROM (s JOIN i) list) foreach {case (sec, info) =>
        if (info != null) {
          println("sec's info: " + sec.secInfo)
          println("secInfo's sec: " + info.sec)
          secs += (info.uniSymbol -> sec)
        }
    }
//    Sec.all() foreach {sec =>
//      Sec.secInfo(sec) match {
//        case None =>
//        case Some(info) => secs += (info.symbol -> sec)
//      }
//    }
  }

  def sampleExchanges = {
    exchanges = List(N, SS, SZ, L)
    exchanges foreach println
    exchanges foreach Exchanges.save
    COMMIT
  }

  def sampleSecs = {
    assert(Exchanges.idOf(N ).isDefined, N  + " with none id")
    assert(Exchanges.idOf(L ).isDefined, L  + " with none id")
    assert(Exchanges.idOf(SS).isDefined, SS + " with none id")
    assert(Exchanges.idOf(SZ).isDefined, SZ + " with none id")
    
    for (symbol <- List("GOOG", "YHOO", "ORCL")) {
      Exchanges.createSimpleSec(symbol, symbol, false)
    }

    for (symbol <- List("BP.L", "VOD.L", "BT-A.L", "BARC.L", "BAY.L", "TSCO.L", "HSBA.L")) {
      Exchanges.createSimpleSec(symbol, symbol, false)
    }

    for ((symbol, name) <- SSSymToName) {
      Exchanges.createSimpleSec(symbol + ".SS", name, false)
    }

    for (symbol <- List("000001.SZ", "000002.SZ", "000003.SZ", "000004.SZ")) {
      Exchanges.createSimpleSec(symbol, symbol, false)
    }
  }

  lazy val N   = Exchange("N",  "America/New_York", Array(9, 30, 16, 00))  // New York
  lazy val SS  = Exchange("SS", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shanghai
  lazy val SZ  = Exchange("SZ", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shenzhen
  lazy val L   = Exchange("L",  "UTC", Array(8, 00, 15, 30)) // London

  val SSSymToName = TreeMap(
    "600000" -> "浦发银行",
    "600001" -> "邯郸钢铁",
    "600002" -> "齐鲁石化",
    "600003" -> "东北高速",
    "600004" -> "白云机场",
    "600005" -> "武钢股份",
    "600006" -> "东风汽车",
    "600007" -> "中国国贸",
    "600008" -> "首创股份",
    "600009" -> "上海机场",
    "600010" -> "钢联股份",
    "600011" -> "华能国际",
    "600012" -> "皖通高速",
    "600015" -> "华夏银行",
    "600016" -> "民生银行",
    "600018" -> "上港集箱",
    "600019" -> "宝钢股份",
    "600020" -> "中原高速",
    "600021" -> "上海电力",
    "600026" -> "中海发展",
    "600028" -> "中国石化",
    "600029" -> "南方航空",
    "600030" -> "中信证券",
    "600031" -> "三一重工",
    "600033" -> "福建高速",
    "600036" -> "招商银行",
    "600037" -> "歌华有线",
    "600038" -> "哈飞股份",
    "600039" -> "四川路桥",
    "600050" -> "中国联通",
    "600051" -> "宁波联合",
    "600052" -> "浙江广厦",
    "600053" -> "*ST江纸",
    "600054" -> "黄山旅游",
    "600055" -> "万东医疗",
    "600056" -> "中技贸易",
    "600057" -> "夏新电子",
    "600058" -> "五矿发展",
    "600059" -> "古越龙山",
    "600060" -> "海信电器",
    "600061" -> "中纺投资",
    "600062" -> "双鹤药业",
    "600063" -> "皖维高新",
    "600064" -> "南京高科",
    "600065" -> "大庆联谊",
    "600066" -> "宇通客车",
    "600067" -> "冠城大通",
    "600068" -> "葛洲坝",
    "600069" -> "银鸽投资",
    "600070" -> "浙江富润",
    "600071" -> "凤凰光学",
    "600072" -> "江南重工",
    "600073" -> "上海梅林"
  )

  val exchangeCodes = Map(
    "CI"  -> "Abidjan Stock Exchange",
    "E"   -> "AEX Options and Futures Exchange",
    "AS"  -> "AEX Stock Exchange",
    "AL"  -> "Alpha Trading Systems",
    "A"   -> "American Stock Exchange",
    "AM"  -> "Amman Stock Exchange",
    "AX"  -> "Australian Stock Exchange",
    "BH"  -> "Bahrain Stock Exchange",
    "MC"  -> "Barcelona Stock Exchange - CATS Feed",
    "BC"  -> "Barcelona Stock Exchange - Floor Trading",
    "BY"  -> "Beirut Stock Exchange",
    "b"   -> "Belfox",
    "BE"  -> "Berlin Stock Exchange",
    "BN"  -> "Berne Stock Exchange",
    "BI"  -> "Bilbao Stock Exchange",
    "BBK" -> "BlockBook ATS",
    "BO"  -> "Bombay Stock Exchange",
    "B"   -> "Boston Stock Exchange",
    "BT"  -> "Botswana Share Market",
    "BM"  -> "Bremen Stock Exchange",
    "BR"  -> "Brussels Stock Exchange",
    "CA"  -> "Cairo and Alexandria Stock Exchange",
    "CL"  -> "Calcutta Stock Exchange",
    "V"   -> "Canadian Ventures Exchange",
    "CH"  -> "Channel Islands",
    "W"   -> "Chicago Board Options Exchange",
    "MW"  -> "Chicago Stock Exchange",
    "CE"  -> "Chile Electronic Exchange",
    "INS" -> "CHI-X Exchange",
    "C"   -> "Cincinnati Stock Exchange",
    "CM"  -> "Colombo Stock Exchange",
    "CO"  -> "Copenhagen Stock Exchange",
    "DL"  -> "Dehli Stock Exchange",
    "QA"  -> "Doha Securities Market",
    "DU"  -> "Dubai Financial Market",
    "DI"  -> "Dubai International Financial Exchange",
    "D"   -> "Dusseldorf Stock Exchange",
    "EB"  -> "Electronic Stock Exchange  of Venezuela",
    "F"   -> "Frankfurt Stock Exchange",
    "FU"  -> "Fukuoka Stock Exchange",
    "GH"  -> "Ghana Stock Exchange",
    "H"   -> "Hamburg Stock Exchange",
    "HA"  -> "Hanover Stock Exchange",
    "HE"  -> "Helsinki Stock Exchange",
    "HK"  -> "Hong Kong Stock Exchange",
    "IC"  -> "Iceland Stock Exchange",
    "IN"  -> "Interbolsa (Portugal)",
    "Y"   -> "International Securities Exchange (ISE)",
    "I"   -> "Irish Stock Exchange",
    "IS"  -> "Istanbul Stock Exchange",
    "JK"  -> "Jakarta Stock Exchange",
    "Q"   -> "Japanese Securities Dealers Association (JASDAQ)",
    "J"   -> "Johannesburg Stock Exchange",
    "KAB" -> "Kabu.com PTS",
    "KA"  -> "Karachi Stock Exchange",
    "KZ"  -> "Kazakhstan Stock Exchange",
    "KFE" -> "Korean Futures Exchange",
    "KS"  -> "Korea Stock Exchange",
    "KQ"  -> "KOSDAQ (Korea)",
    "KL"  -> "Kuala Lumpur Stock Exchange",
    "KW"  -> "Kuwait Stock Exchange",
    "KY"  -> "Kyoto Stock Exchange",
    "LG"  -> "Lagos Stock Exchange",
    "LA"  -> "Latin American Market in Spain (LATIBEX)",
    "LN"  -> "Le Nouveau Marche",
    "LM"  -> "Lima Stock Exchange",
    "LS"  -> "Lisbon Stock Exchange (Portugal)",
    "L"   -> "London Stock Exchange",
    "LZ"  -> "Lusaka Stock Exchange",
    "LU"  -> "Luxembourg Stock Exchange",
    "MD"  -> "Madras Stock Exchange",
    "MA"  -> "Madrid Stock Exchange - Floor Trading",
    "MT"  -> "Malta Stock Exchange",
    "MZ"  -> "Mauritius Stock Exchange",
    "ML"  -> "Medellin Stock Excahnge",
    "MX"  -> "Mexican Stock Exchange",
    "MI"  -> "Milan Stock Exchange",
    "p"   -> "MONEP Paris Stock Options",
    "M"   -> "Montreal Exchange",
    "MM"  -> "Moscow Inter Bank Currency Exchange",
    "MO"  -> "Moscow Stock Exchange",
    "MU"  -> "Munich Stock Exchange",
    "OM"  -> "Muscat Stock Exchange",
    "NG"  -> "Nagoya Stock Exchange",
    "NR"  -> "Nairobi Stock Exchange",
    "NM"  -> "Namibia Stock Exchange",
    "OQ"  -> "NASDAQ",
    "OB"  -> "NASDAQ Dealers - Bulletin Board",
    "OJ"  -> "NASDAQ Japan",
    "NS"  -> "National Stock Exchange of India",
    "NW"  -> "NewEx (Austria)",
    "N"   -> "New York Stock Exchange",
    "NZ"  -> "New Zealand Stock Exchange",
    "MP"  -> "NYSE MatchPoint",
    "OD"  -> "Occidente Stock Exchange",
    "OS"  -> "Osaka Stock Exchange",
    "OL"  -> "Oslo Stock Exchange",
    "P"   -> "Pacific Stock Exchange",
    "PA"  -> "Paris Stock Exchange",
    "PH"  -> "Philadelphia Stock Exchange",
    "X"   -> "Philadelphia Stock Exchange Options",
    "PS"  -> "Phillipine Stock Exchange",
    "PNK" -> "Pink Sheets (National Quotation Bureau)",
    "PR"  -> "Prague Stock Exchange",
    "PT"  -> "Pure Trading",
    "RQ"  -> "RASDAQ (Romania)",
    "RI"  -> "Riga Stock Exchange",
    "SO"  -> "Rio de Janeiro OTC Stock Exchange (SOMA)",
    "RTS" -> "Russian Trading System",
    "SN"  -> "Santiago Stock Exchange",
    "SA"  -> "Sao Paulo Stock Exchange",
    "SP"  -> "Sapporo Stock Exchange",
    "SE"  -> "Saudi Stock Exchange",
    "JNX" -> "SBI Japannext",
    "SBI" -> "SBI Stock Exchange (Sweden)",
    "SS"  -> "Shanghai Stock Exchange",
    "SZ"  -> "Shenzhen Stock Exchange",
    "SIM" -> "Singapore Exchange - Derivatives",
    "SI"  -> "Singapore Stock Exchange",
    "ST"  -> "Stockholm Stock Exchange",
    "PE"  -> "St. Petersburg Stock Exchange",
    "SG"  -> "Stuttgart Stock Exchange",
    "SU"  -> "Surabaya Stock Exchange",
    "QMH" -> "SWX Quotematch AG",
    "S"   -> "SWX Swiss Exchange",
    "SFE" -> "Sydney Futures Exchange",
    "TWO" -> "Taiwan OTC Securities Exchange",
    "TW"  -> "Taiwan Stock Exchange",
    "TL"  -> "Tallinn Stock Exchange",
    "TA"  -> "Tel Aviv Stock Exchange",
    "BK"  -> "Thailand Stock Exchange",
    "TH"  -> "Third Market",
    "TCE" -> "Tokyo Commodity Exchange",
    "TFF" -> "Tokyo Financial Futures Exchange",
    "T"   -> "Tokyo Stock Exchange",
    "K"   -> "Toronto Options Exchange",
    "TO"  -> "Toronto Stock Exchange",
    "TP"  -> "Tradepoint Stock Exchange",
    "TN"  -> "Tunis Stock Exchange",
    "TQ"  -> "Turquoise",
    "PFT" -> "Ukraine PFTS",
    "VA"  -> "Valencia Stock Exchange",
    "VI"  -> "Vienna Stock Exchange",
    "VL"  -> "Vilnus Stock Exchange",
    "VX"  -> "virt-x",
    "DE"  -> "Xetra",
    "ZA"  -> "Zagreb Stock Exchange",
    "ZI"  -> "Zimbabwe Stock Exchange"
  )


}
