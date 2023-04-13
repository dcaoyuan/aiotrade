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

import java.awt.Image
import java.io.{BufferedReader, File, InputStreamReader, InputStream}
import java.net.{HttpURLConnection, URL}
import java.text.{DateFormat, ParseException, SimpleDateFormat}
import java.util.{Calendar, Locale, TimeZone}
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.GZIPInputStream
import javax.imageio.ImageIO
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.QuoteServer
import org.aiotrade.lib.util.Singleton

/**
 *
 * @author Caoyuan Deng
 */
object YahooQuoteServer extends QuoteServer with Singleton {
  private val log = Logger.getLogger(this.getClass.getName)

  def getSingleton = this

  // * "http://table.finance.yahoo.com/table.csv"
  protected val BaseUrl = "http://table.finance.yahoo.com"
  protected val UrlPath = "/table.csv"
  protected val dateFormat: DateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US)

  def exchangeOf(srcSymbol: String): Exchange = {
    srcSymbol.split("\\.") match {
      case Array(head, exchange) => exchange.toUpperCase match {
          case "L"  => Exchange.L
          case "SS" => Exchange.SS
          case "SZ" => Exchange.SZ
          case _ => Exchange.N
        }
      case _ =>
        exchangeOfIndex(srcSymbol) match {
          case Some(x) => x
          case None => Exchange.N
        }
    }
  }

  private def exchangeOfIndex(srcSymbol: String): Option[Exchange] = {
    srcSymbol match {
      case "^DJI" => Some(Exchange.N)
      case "^HSI" => Some(Exchange.HK)
      case _ => None
    }
  }

  /**
   * Template:
   * http://table.finance.yahoo.com/table.csv?s=^HSI&a=01&b=20&c=1990&d=07&e=18&f=2005&g=d&ignore=.csv
   */
  @throws(classOf[Exception])
  protected def request(contract: QuoteContract): Option[InputStream] = {
    val fromTime = contract.fromTime
    val cal = Calendar.getInstance

    val (bTime, eTime) = if (fromTime <= ANCIENT_TIME /* @todo */) {
      (contract.fromTime, contract.toTime)
    } else {
      cal.setTimeInMillis(fromTime)
      (fromTime, cal.getTimeInMillis)
    }

    cal.setTimeInMillis(bTime)
    val a = cal.get(Calendar.MONTH)
    val b = cal.get(Calendar.DAY_OF_MONTH)
    val c = cal.get(Calendar.YEAR)

    cal.setTimeInMillis(eTime)
    val d = cal.get(Calendar.MONTH)
    val e = cal.get(Calendar.DAY_OF_MONTH)
    val f = cal.get(Calendar.YEAR)

    val urlStr = new StringBuilder(50)
    urlStr.append(BaseUrl).append(UrlPath)
    urlStr.append("?s=").append(contract.srcSymbol)

    /** a, d is month, which from 0 to 11 */
    urlStr.append("&a=" + a + "&b=" + b + "&c=" + c +
                  "&d=" + d + "&e=" + e + "&f=" + f)

    urlStr.append("&g=d&ignore=.csv")

    val url = new URL(urlStr.toString)

    log.info(url.toString)

    if (url != null) {
      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestProperty("Accept-Encoding", "gzip")
      conn.setAllowUserInteraction(true)
      conn.setRequestMethod("GET")
      conn.setInstanceFollowRedirects(true)
      conn.connect

      val encoding = conn.getContentEncoding
      val gzipped = encoding != null && encoding.indexOf("gzip") != -1

      val is = conn.getInputStream
      if (is == null) None else Some(if (gzipped) new GZIPInputStream(is) else is)
    } else None
  }

  /**
   * @return readed time
   */
  @throws(classOf[Exception])
  protected def read(contract: QuoteContract, is: InputStream): Array[Quote] = {
    val fromTime = contract.fromTime
    
    val reader = new BufferedReader(new InputStreamReader(is))
    /** skip first line */
    val s = reader.readLine

    val quotes = new ArrayList[Quote]
    val freq = contract.freq
    val symbol = contract.srcSymbol
    val exchange = exchangeOf(symbol)
    val timeZone = exchange.timeZone
    // * for daily quote, yahoo returns exchange's local date, so use exchange time zone
    val cal = Calendar.getInstance(timeZone)
    val dateFormat = new SimpleDateFormat(defaultDatePattern) //dateFormatOf(timeZone)
    dateFormat.setTimeZone(timeZone)
    
    def loop(newestTime: Long): Long = reader.readLine match {
      case null => newestTime // break now
      case line => line.split(",") match {
          case Array(dateX, openX, highX, lowX, closeX, volumeX, adjCloseX, _*) =>
            /**
             * !NOTICE
             * must catch the date parse exception, other wise, it's dangerous
             * for build a calendarTimes in BaseSer
             */
            try {
              val date = dateFormat.parse(dateX.trim)
              cal.clear
              cal.setTime(date)
            } catch {case _: ParseException => loop(newestTime)}

            // the time should be properly set to 00:00 of exchange location's local time, i.e. rounded to TFreq.DAILY
            val time = cal.getTimeInMillis
            if (time < fromTime) {
              loop(newestTime)
            }

            val quote = new Quote
            quote.time   = time
            quote.open   = openX.trim.toDouble
            quote.high   = highX.trim.toDouble
            quote.low    = lowX.trim.toDouble
            quote.close  = closeX.trim.toDouble
            quote.volume = volumeX.trim.toDouble
            quote.prevClose = adjCloseX.trim.toDouble
            quote.amount = -1
            //quote.adjWeight = adjCloseX.trim

            val newestTime1 = if (quote.high * quote.low * quote.close == 0) {
              newestTime
            } else {
              quotes += quote
              math.max(newestTime, time)
            }
                        
            loop(newestTime1)
          case _ => loop(newestTime)
        }
    }

    loop(Long.MinValue)

    quotes.toArray
  }

  protected def requestData(contracts: Iterable[QuoteContract]) {
    for (contract <- contracts) {
      try {
        request(contract) match {
          case Some(is) =>
            val quotes = read(contract, is)
            if (quotes.length > 0) {
              publishData(DataLoaded(quotes, contract))
            }
          case None =>
        }
      } catch {
        case ex: Exception => log.log(Level.WARNING, ex.getMessage, ex)
      }
    }
  }

  override def supportedFreqs: Array[TFreq] = Array(TFreq.DAILY)

  override def icon: Option[Image] = {
    try {
      Option(ImageIO.read(new File("org/aiotrade/lib/dataserver/yahoo/resources/favicon_yahoo.png")))
    } catch {case _: Throwable => None}
  }

  val displayName = "Yahoo! Finance Internet"
  val defaultDatePattern = "yyyy-MM-dd"
  val serialNumber = 1
  val sourceTimeZone = TimeZone.getTimeZone("America/New_York")
  val classNameOfTickerServer = Some(YahooTickerServer.getClass.getName)
}
