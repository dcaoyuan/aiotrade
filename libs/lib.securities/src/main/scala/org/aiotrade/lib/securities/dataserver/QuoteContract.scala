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
package org.aiotrade.lib.securities.dataserver

import java.awt.Image
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataContract

/**
 * most fields' default value should be OK.
 *
 * @author Caoyuan Deng
 */
class QuoteContract extends DataContract[QuoteServer] {

  serviceClassName = "org.aiotrade.lib.dataserver.yahoo.YahooQuoteServer"
  /** default freq */
  freq = TFreq.DAILY
  datePattern = Some("yyyy-MM-dd")

  def icon: Option[Image] =  {
    if (isServiceInstanceCreated) {
      createdServerInstance.icon
    } else {
      lookupServiceTemplate(classOf[QuoteServer], "DataServers") match {
        case Some(x) => x.icon
        case None => None
      }
    }
  }

  def supportedFreqs: Array[TFreq] = {
    if (isServiceInstanceCreated) {
      createdServerInstance.supportedFreqs
    } else {
      lookupServiceTemplate(classOf[QuoteServer], "DataServers") match {
        case Some(x) => x.supportedFreqs
        case None => Array()
      }
    }
  }

  def isFreqSupported(freq: TFreq): Boolean = {
    if (isServiceInstanceCreated) {
      createdServerInstance.isFreqSupported(freq)
    } else {
      lookupServiceTemplate(classOf[QuoteServer], "DataServers") match {
        case Some(x) => x.isFreqSupported(freq)
        case None => false
      }
    }
  }

  override def displayName = "Quote Data Contract"
}
