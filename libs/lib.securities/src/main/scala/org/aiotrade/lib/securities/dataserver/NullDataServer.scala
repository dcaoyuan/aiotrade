package org.aiotrade.lib.securities.dataserver

import java.util.TimeZone
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataServer
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.util.Singleton

object NullQuoteServer extends QuoteServer with Singleton {

  def getSingleton = this

  deafTo(DataServer) // disable HeartBeat

  protected def requestData(contracts: Iterable[QuoteContract]) {
    // should publish evt to enable evt chain
    publish(DataLoaded(this.EmptyValues, contracts.head))
  }

  override 
  def supportedFreqs: Array[TFreq] = Array()

  val displayName = "Null Quote Server"
  val defaultDatePattern = "MM/dd/yyyy h:mma"
  val serialNumber = 102
  val sourceTimeZone = TimeZone.getDefault
  val classNameOfTickerServer = Some(NullQuoteServer.getClass.getName)
}

object NullTickerServer extends TickerServer with Singleton {

  def getSingleton = this

  deafTo(DataServer) // disable HeartBeat
  
  // won't request tickers, tickers will be got passively via NodePubSub's ! sending 
  protected def requestData(contracts: Iterable[TickerContract]) {}
  // won't process tickers, quote/moneyflows will be got passively via NodePubSub's ! sending 
  override 
  protected def processData(values: Array[Ticker], contract: TickerContract): Long = -1
  
  val displayName = "Null Ticker Server"
  val defaultDatePattern = "MM/dd/yyyy h:mma"
  val serialNumber = 102
  val sourceTimeZone = TimeZone.getDefault
}
