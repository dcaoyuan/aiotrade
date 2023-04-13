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
package org.aiotrade.lib.view.securities.persistence

import java.text.ParseException
import java.text.SimpleDateFormat
import javax.swing.text.DateFormatter
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.securities.dataserver.MoneyFlowContract
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.RichInfoContract
import org.aiotrade.lib.securities.dataserver.RichInfoHisContract
import org.aiotrade.lib.util.serialization.BeansDocument

/**
 * @author Caoyuan Deng
 */

object ContentPersistenceHandler {
    
  def dumpContent(content: Content): String = {
    val buffer = new StringBuilder(500)
    val beans = new BeansDocument
        
    buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    //buffer.append("<!DOCTYPE settings PUBLIC \"-//AIOTrade//DTD Content settings 1.0//EN\" >\n")
    buffer.append("<sec unisymbol=\"" + content.uniSymbol + "\">\n")

    val df = new DateFormatter(new SimpleDateFormat("yyyy-MM-dd"))
  
    val dataContracts = ("sources", "source", content.lookupDescriptors(classOf[QuoteContract]))
    val moneyflowContracts = ("moneyflowsources", "moneyflowsource", content.lookupDescriptors(classOf[MoneyFlowContract]))
    val infoContracts = ("richinfosources", "richinfosource", content.lookupDescriptors(classOf[RichInfoContract]))
    val infoHisContracts = ("richinfohissources", "richinfohissource", content.lookupDescriptors(classOf[RichInfoHisContract]))

    for ((sources, source, contracts) <- List(dataContracts, moneyflowContracts, infoContracts, infoHisContracts)) {
      if (contracts.size > 0) {
        buffer.append("    <").append(sources).append(">\n")
        for (contract <- contracts) {
          buffer.append("        <").append(source).append(" ")
          buffer.append("active=\"" + contract.active + "\" ")
          buffer.append("class=\"" + contract.serviceClassName + "\" ")
          buffer.append("symbol=\"" + contract.srcSymbol + "\" ")
          contract.datePattern foreach {x => buffer.append("dateformat=\"" + x + "\" ")}
          // always store daily freq for datacontract
          buffer.append("nunits=\"" + TFreq.DAILY.nUnits + "\" ")
          buffer.append("unit=\"" + TFreq.DAILY.unit + "\" ")
          buffer.append("refreshable=\"" + contract.isRefreshable + "\" ")
          buffer.append("refreshinterval=\"" + contract.refreshInterval + "\" ")
          try {
            buffer.append("fromTime=\"" + contract.fromTime + "\" ")
            buffer.append("toTime=\"" + contract.toTime + "\" ")
          } catch {case ex: ParseException => ex.printStackTrace}
          buffer.append("url=\"" + contract.urlString + "\"")
          buffer.append(">\n")
          buffer.append("        </").append(source).append(">\n")
        }
        buffer.append("    </").append(sources).append(">\n")
      }
    }
        
    val indicatorDescriptors = content.lookupDescriptors(classOf[IndicatorDescriptor])
    if (indicatorDescriptors.size > 0) {
      buffer.append("    <indicators>\n")
      for (descriptor <- indicatorDescriptors) {
        buffer.append("        <indicator ")
        buffer.append("active=\"" + descriptor.active + "\" ")
        buffer.append("class=\"" + descriptor.serviceClassName + "\" ")
        for (s <- descriptor.uniSymbol) buffer.append("symbol=\"" + s + "\" ")
        buffer.append("nunits=\"" + descriptor.freq.nUnits + "\" ")
        buffer.append("unit=\"" + descriptor.freq.unit + "\">\n")
                
        val factors = descriptor.factors
        for (factor <- factors) {
          buffer.append("            <opt name=\"").append(factor.name)
          .append("\" value=\"").append(factor.value)
          .append("\" step=\"").append(factor.step)
          .append("\" minvalue=\"").append(factor.minValue)
          .append("\" maxvalue=\"").append(factor.maxValue)
          .append("\"/>\n")
        }
                
        buffer.append("        </indicator>\n")
      }
      buffer.append("    </indicators>\n")
    }
        
    val drawingDescriptors = content.lookupDescriptors(classOf[DrawingDescriptor])
    if (drawingDescriptors.size > 0) {
      buffer.append("    <drawings>\n")
      for (descriptor <- drawingDescriptors) {
        buffer.append("        <layer ")
        buffer.append("name=\"" + descriptor.serviceClassName + "\" ")
        buffer.append("nunits=\"" + descriptor.freq.nUnits + "\" ")
        buffer.append("unit=\"" + descriptor.freq.unit + "\">\n ")
        val chartMapPoints = descriptor.getHandledChartMapPoints
        for (chart <- chartMapPoints.keysIterator) {
          buffer.append("            <chart class=\"" + chart.getClass.getName + "\">\n")
          for (point <- chartMapPoints.get(chart).get) {
            buffer.append("                <handle t=\"" + point.t + "\" v=\"" + point.v + "\"/>\n")
          }
          buffer.append("            </chart>\n")
        }
        buffer.append("        </layer>\n")
      }
      buffer.append("    </drawings>\n")
    }
        
    buffer.append("</sec>")
        
    //beans.saveDoc();
        
    return buffer.toString
  }
    
  def loadContent {
  }
    
}
