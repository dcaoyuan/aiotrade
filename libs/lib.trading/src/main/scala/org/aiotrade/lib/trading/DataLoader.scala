package org.aiotrade.lib.trading

import java.util.logging.Logger
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.dataserver.NullQuoteServer
import org.aiotrade.lib.securities.dataserver.NullTickerServer
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content


/**
 * @Note If we implement DataLoader as actor to async load data from db, the db performance 
 * may become the bottleneck, and the worse, the db connections may be exhausted, and causes
 * series problems.
 * 
 * @author Caoyuan Deng
 */
class DataLoader(quoteFreqs: List[TFreq], indFreqs: List[TFreq],
                 quoteServerClass: String = NullQuoteServer.getClass.getName, 
                 tickerServerClass: String = NullTickerServer.getClass.getName
) {
  private val log = Logger.getLogger(this.getClass.getName)

  def load(sec: Sec) {
    val symbol = sec.uniSymbol
    
    val content = sec.content

    for (freq <- quoteFreqs) {
      val quoteContract = securities.createQuoteContract(symbol, "", "", freq, false, quoteServerClass)
      content.addDescriptor(quoteContract)
    }
    val tickerContract = securities.createTickerContract(symbol, "", "", TFreq.ONE_MIN, tickerServerClass)
    sec.tickerContract = tickerContract

    indFreqs foreach createAndAddIndicatorDescritors(content)

    // * init indicators before loadSer, so, they can receive the Loaded evt
    val inds = for (freq <- indFreqs; ser <- sec.serOf(freq)) yield securities.initIndicators(content, ser) 

    //for (freq <- quoteFreqs; ser <- sec.serOf(freq)) {
    //  sec.loadSer(ser)
    //  ser.adjust()
    //}
    // @todo above code seems will miss adjust() call due to lost of TSer.Loaded evt, need to dig later, just use a plain securities.loadSer(sec, freq)
    for (freq <- quoteFreqs) {
      log.info("Loading " + symbol + " " + freq)
      securities.loadSer(sec, freq, true)
    }

    // * Here, we test two possible conditions:
    // * 1. inds may have been computed by Loaded evt,
    // * 2. data loading may not finish yet
    // * For what ever condiction, we force to compute it again to test concurrent
    inds flatMap (x => x) foreach securities.computeSync
  }
    
  /**
   * Added indicators that want to load here. @Todo specify indicator via config ?
   */
  def createAndAddIndicatorDescritors(content: Content)(freq: TFreq) {}
}
