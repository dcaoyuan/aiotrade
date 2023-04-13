/*
 * PersistenceManager.scala
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.aiotrade.platform.test

import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.dataserver.QuoteServer
import org.aiotrade.lib.securities.dataserver.TickerServer
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content
import java.util.Properties
import org.aiotrade.lib.indicator.Indicator
import org.aiotrade.lib.indicator.basic._

/**
 *
 * @author Caoyuan Deng
 */
class PlainPersistenceManager extends PersistenceManager {

  private lazy val quoteServers  = List[QuoteServer]()  // new YahooQuoteServer
  private lazy val tickerServers = List[TickerServer]() // new YahooTickerServer
  private lazy val indicators    = List(new ARBRIndicator,
                                        new BIASIndicator,
                                        new BOLLIndicator,
                                        new CCIIndicator,
                                        new DMIIndicator,
                                        new EMAIndicator,
                                        new GMMAIndicator,
                                        new HVDIndicator,
                                        new KDIndicator,
                                        new MACDIndicator,
                                        new MAIndicator,
                                        new MFIIndicator,
                                        new MTMIndicator,
                                        new OBVIndicator,
                                        new ROCIndicator,
                                        new RSIIndicator,
                                        new SARIndicator,
                                        new WMSIndicator,
                                        new ZIGZAGFAIndicator,
                                        new ZIGZAGIndicator
  )

  def saveQuotes(symbol: String, freq: TFreq, quotes: Array[Quote], sourceId: Long) {}
  def restoreQuotes(symbol: String, freq: TFreq): Array[Quote] = Array[Quote]()
  def deleteQuotes(symbol: String, freq: TFreq, fromTime: Long, toTime: Long) {}
  def dropAllQuoteTables(symbol: String) {}

  def saveRealTimeTickers(tickers: Array[Ticker], sourceId: Long) {}
  def restoreTickers(symbol: String): Array[Ticker] = Array[Ticker]()
  def restoreRealTimeTickersOverview: Array[Ticker] = Array[Ticker]()
  def deleteRealTimeTickers {}

  def shutdown {}

  def restoreProperties {}
  def saveProperties {}
  def properties = new Properties

  def saveContent(content: Content) {}
  def restoreContent(symbol: String): Content = new Content(symbol)
  def defaultContent: Content = new Content("<Default>")

  def lookupAllRegisteredServices[T](clz: Class[T], folderName: String): Seq[T] = {
    if (clz == classOf[QuoteServer]) {
      quoteServers.asInstanceOf[Seq[T]]
    } else if (clz == classOf[TickerServer]) {
      tickerServers.asInstanceOf[Seq[T]]
    } else if (clz == classOf[Indicator]) {
      indicators.asInstanceOf[Seq[T]]
    } else {
      Nil
    }
  }

}
