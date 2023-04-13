/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.modules.ui.quicksearch

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.modules.ui.windows.AnalysisChartTopComponent
import org.aiotrade.spi.quicksearch.SearchProvider
import org.aiotrade.spi.quicksearch.SearchRequest
import org.aiotrade.spi.quicksearch.SearchResponse


class FreqSearchProvider extends SearchProvider {

  private val freqs = Map(TFreq.ONE_MIN -> "分钟线",
                          TFreq.THREE_MINS -> "3分钟",
                          TFreq.FIVE_MINS -> "5分钟",
                          TFreq.FIFTEEN_MINS -> "15分钟",
                          TFreq.THIRTY_MINS -> "30分钟",
                          TFreq.ONE_HOUR -> "小时线",
                          TFreq.DAILY -> "日线",
                          TFreq.THREE_DAYS -> "3日线",
                          TFreq.FIVE_DAYS -> "5日线",
                          TFreq.WEEKLY -> "周线",
                          TFreq.MONTHLY -> "月线",
                          TFreq.ONE_YEAR -> "年线",
                          TFreq.ONE_SEC -> "分时"
  )
  private val textToFreq = (freqs map (x => (x._1.shortName -> x._1))) ++ Map(
    "rt" -> TFreq.ONE_SEC,
    "fs" -> TFreq.ONE_SEC,
    "fz" -> TFreq.ONE_MIN,
    "3fz" -> TFreq.THREE_MINS,
    "5fz" -> TFreq.FIVE_MINS,
    "15fz" -> TFreq.FIFTEEN_MINS,
    "30fz" -> TFreq.THIRTY_MINS,
    "1xs" -> TFreq.ONE_HOUR,
    "sx" -> TFreq.ONE_HOUR,
    "zx" -> TFreq.WEEKLY,
    "yx" -> TFreq.MONTHLY,
    "nx"  -> TFreq.ONE_YEAR
  )

  /**
   * Method is called by infrastructure when search operation was requested.
   * Implementors should evaluate given request and fill response object with
   * apropriate results
   *
   * @param request Search request object that contains information what to search for
   * @param response Search response object that stores search results.
   *    Note that it's important to react to return value of SearchResponse.addResult(...) method
   *    and stop computation if false value is returned.
   */
  def evaluate(request: SearchRequest, response: SearchResponse) {
    val input = request.text.toUpperCase
    for ((text, freq) <- textToFreq if text.toUpperCase.startsWith(input)) {
      val name = freqs.get(freq) match {
        case Some(x) => text + " (" + x + ")"
        case None => text
      }
      if (!response.addResult(new FoundResult(freq), name)) return
    }
  }

  private class FoundResult(freq: TFreq) extends Runnable {
    def run {
      for (tc <- AnalysisChartTopComponent.selected;
           sec = tc.sec;
           content = sec.content;
           quoteContract <- content.lookupActiveDescriptor(classOf[QuoteContract])
      ) {
        quoteContract.freq = freq
        val tc = AnalysisChartTopComponent(sec)
        tc.requestActive
      }
    }
  }

}
