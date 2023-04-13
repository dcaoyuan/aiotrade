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
package org.aiotrade.lib.dataserver.yahoo

import java.io.{BufferedReader, InputStreamReader, InputStream}
import java.net.{HttpURLConnection, URL, SocketTimeoutException}
import java.text.ParseException
import java.util.{Calendar, TimeZone}
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.dataserver.TickerContract
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.Singleton
import scala.collection.mutable.ListBuffer

/**
 * @NOTICE
 * If the remote datafeed keeps only one inputstream for all subscriiebed
 * symbols, one singleton instance is enough. If each symbol need a separate
 * session, you may create new data server instance for each symbol.
 * 
 * @author Caoyuan Deng
 */
object YahooTickerServer extends TickerServer with Singleton {
  private val log = Logger.getLogger(this.getClass.getName)

  def getSingleton = this

  private val nSymbolsPerReq = 100

  // * "http://download.finance.yahoo.com/d/quotes.csv"
  private val BaseUrl = "http://quote.yahoo.com"
  private val UrlPath = "/download/javasoft.beans"

  //Dow Jones Index restrict yahoo publish ^DJI quote, reference:
  //http://developer.yahoo.net/forum/?showtopic=6943&endsession=1
  //http://stackoverflow.com/questions/3679870/yahoo-finance-csv-file-will-not-return-dow-jones-dji
  override def toSrcSymbol(uniSymbol: String): String = if(uniSymbol == "^DJI") "INDU" else uniSymbol
  override def toUniSymbol(srcSymbol: String): String = if(srcSymbol == "INDU") "^DJI" else srcSymbol

  /**
   * Template:
   * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
   */
  protected def request(srcSymbol: Seq[String]): Option[InputStream] = {
    if (srcSymbol.isEmpty) return None

    val cal = Calendar.getInstance(sourceTimeZone)

    val urlStr = new StringBuilder(90)
    urlStr.append(BaseUrl).append(UrlPath)
    urlStr.append("?s=")

    urlStr.append(srcSymbol mkString("+"))

    urlStr.append("&d=t&f=sl1d1t1c1ohgvbap")

    /** s: symbol, n: name, x: stock exchange */
    val urlStrForName = urlStr.append("&d=t&f=snx").toString


    try {
      val url = new URL(urlStr.toString)
      log.info(url.toString)

      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "gzip")
      conn.setAllowUserInteraction(true)
      conn.setRequestMethod("GET")
      conn.setInstanceFollowRedirects(true)
      conn.setConnectTimeout(5000)
      conn.setReadTimeout(5000)
      conn.connect

      val encoding = conn.getContentEncoding
      val gzipped = encoding != null && encoding.indexOf("gzip") != -1

      val is = conn.getInputStream
      if (is == null) None else Some(if (gzipped) new GZIPInputStream(is) else is)
    } catch {
      case e: SocketTimeoutException => None
      case e: Throwable => None
    }
  }

  @throws(classOf[Exception])
  protected def read(is: InputStream): Array[Ticker] = {
    val reader = new BufferedReader(new InputStreamReader(is))
    
    val tickers = new ArrayList[Ticker]

    // time in Yahoo! tickers is in Yahoo! Inc's local time instead of exchange place
    // we need to convert them to UTC time
    val cal = Calendar.getInstance(sourceTimeZone)
    val dateFormat = dateFormatOf(sourceTimeZone)

    def loop(newestTime: Long): Long = reader.readLine match {
      case null => newestTime // break right now
      case line => 
        log.fine("line: " + line)
        line.split(",") match {
          case Array(symbolX, lastPriceX, dateX, timeX, dayChangeX, dayOpenX, dayHighX, dayLowX, dayVolumeX, bidPriceX1, askPriceX1, prevCloseX, _, _, _, nameX, marketX, _*)
            if !dateX.toUpperCase.contains("N/A") && !timeX.toUpperCase.contains("N/A") =>
            val symbol = symbolX.toUpperCase.replace('"', ' ').trim
            //val symbol  = toUniSymbol(symbolX.toUpperCase.replace('"', ' ').trim)
            val dateStr = dateX.replace('"', ' ').trim
            val timeStr = timeX.replace('"', ' ').trim

            val exchange = Exchange.exchangeOf(symbol)
            
            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in BaseTSer
             */
            try {
              val date = dateFormat.parse(dateStr + " " + timeStr)
              cal.clear
              cal.setTime(date)

              // fix dateStr bug for ".SS" and ".SZ" from yahoo ticker. The bug is like:
              // When apply: http://quote.yahoo.com/download/javasoft.beans?s=ORCL+BP.L+600000.SS+000002.SZ&d=t&f=sl1d1t1c1ohgvbap&d=t&f=snx
              // 1. At 8/17/2010 12:10pm CST, we got:
              // "ORCL",22.72,"8/16/2010","4:00pm",+0.06,22.50,23.00,22.35,19511716,19.00,N/A,22.66,"ORCL","ORCL","ORCL","Oracle Corporatio","NasdaqNM"
              // "BP.L",409.75,"8/16/2010","11:35am",-6.65,416.30,416.65,406.15,24275120,409.70,409.80,409.75,"BP.L","BP.L","BP.L","BP","London"
              // "600000.SS",14.39,"8/17/2010","11:30pm",+0.09,14.31,14.47,14.18,34159084,14.38,14.39,14.30,"600000.SS","600000.SS","600000.SS","S/PUDONG DEV BANK","Shanghai"
              // "000002.SZ",9.01,"8/17/2010","11:30pm",+0.12,8.85,9.01,8.81,61219112,9.00,9.01,8.89,"000002.SZ","000002.SZ","000002.SZ","VANKE-A","Shenzhen"
              // 2. Then at 8/17/2010 1:35pm CST, we got:
              // "ORCL",22.72,"8/16/2010","4:00pm",+0.06,22.50,23.00,22.35,19511716,19.00,N/A,22.66,"ORCL","ORCL","ORCL","Oracle Corporatio","NasdaqNM"
              // "BP.L",409.75,"8/16/2010","11:35am",-6.65,416.30,416.65,406.15,24275120,409.70,409.80,409.75,"BP.L","BP.L","BP.L","BP","London"
              // "600000.SS",14.36,"8/17/2010","1:20am",+0.06,14.31,14.47,14.18,36886816,14.36,14.37,14.30,"600000.SS","600000.SS","600000.SS","S/PUDONG DEV BANK","Shanghai"
              // "000002.SZ",8.96,"8/17/2010","1:20am",+0.07,8.85,9.02,8.81,68111120,8.96,8.97,8.89,"000002.SZ","000002.SZ","000002.SZ","VANKE-A","Shenzhen"
              if ((symbol.endsWith(".SS") || symbol.endsWith(".SZ")) && timeStr.endsWith("pm")) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
              }
            } catch {case _: ParseException => loop(newestTime)}

            val time = cal.getTimeInMillis
            if (time == 0) {
              /** for test and finding issues */
              log.warning("time of ticker: " + symbol + " is 0!")
            }

            val snapTicker = snapTickerOf(symbol)
            snapTicker.time = time
            snapTicker.isChanged = false
            snapTicker.prevClose = if (prevCloseX.equalsIgnoreCase("N/A")) 0 else prevCloseX.trim.toDouble
            snapTicker.lastPrice = if (lastPriceX.equalsIgnoreCase("N/A")) 0 else lastPriceX.trim.toDouble
            snapTicker.dayChange = if (dayChangeX.equalsIgnoreCase("N/A")) 0 else dayChangeX.trim.toDouble
            snapTicker.dayOpen   = if (dayOpenX.equalsIgnoreCase("N/A")) 0 else dayOpenX.trim.toDouble
            snapTicker.dayHigh   = if (dayHighX.equalsIgnoreCase("N/A")) 0 else dayHighX.trim.toDouble
            snapTicker.dayLow    = if (dayLowX.equalsIgnoreCase("N/A")) 0 else dayLowX.trim.toDouble
            snapTicker.dayVolume = if (dayVolumeX.equalsIgnoreCase("N/A")) 0 else dayVolumeX.trim.toDouble
            snapTicker.setBidPrice(0, if (bidPriceX1.equalsIgnoreCase("N/A")) 0 else bidPriceX1.trim.toDouble)
            snapTicker.setAskPrice(0, if (askPriceX1.equalsIgnoreCase("N/A")) 0 else askPriceX1.trim.toDouble)
            log.fine("tickerSnapshot : "+ snapTicker.toLightTicker.toString)
            log.fine("tickerSnapshot.isChanged : " + snapTicker.isChanged + ", subscribedSrcSymbols.contains " + symbol + ": " + this.subscribedSrcSymbols.contains(symbol))
            if (snapTicker.isChanged && this.subscribedSrcSymbols.contains(symbol)) {
              val ticker = new Ticker
              ticker.uniSymbol = symbol
              ticker.copyFrom(snapTicker)
              tickers += ticker
            }

            loop(math.max(newestTime, time))
          case _ => loop(newestTime)
        }
    }

    val newestTime = loop(Long.MinValue)
    
    tickers.toArray
  }

  /**
   * Retrive data from Yahoo finance website
   * Template:
   * http://quote.yahoo.com/download/javasoft.beans?symbols=^HSI+YHOO+SUMW&&format=sl1d1t1c1ohgvbap
   *
   * @param afterThisTime from time
   */
  protected def requestData(contracts: Iterable[TickerContract]) {
    if (contracts.isEmpty) return
    
    val symbols = contracts map (_.srcSymbol) toArray
    var i = 0
    while (i < symbols.length) {
      val toProcess = new ListBuffer[String]
      var j = 0
      while (j < nSymbolsPerReq && i < symbols.length) { // 1000: num of symbols per time
        toProcess += symbols(i)
        j += 1
        i += 1
      }
      if (!toProcess.isEmpty) {
        try {
          request(toProcess) match {
            case Some(is) =>
              val tickers = read(is)
              if (tickers.length > 0) {
                publishData(DataLoaded(tickers, null))
              }
            case None => log.info("no reponse for :" + toProcess.mkString(","))
          }
        } catch {case ex: Exception => log.log(Level.WARNING, ex.getMessage, ex)}
      }
    }
  }

  val displayName = "Yahoo! Finance Internet"
  val defaultDatePattern = "MM/dd/yyyy h:mma"
  val serialNumber = 1
  val sourceTimeZone = TimeZone.getTimeZone("America/New_York")
}



