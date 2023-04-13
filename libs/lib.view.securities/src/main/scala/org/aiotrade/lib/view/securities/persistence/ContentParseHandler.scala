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

import java.text.NumberFormat
import java.text.ParseException
import java.util.Calendar
import org.aiotrade.lib.charting.chart.handledchart.HandledChart
import org.aiotrade.lib.charting.chart.segment.ValuePoint
import org.aiotrade.lib.charting.descriptor.DrawingDescriptor
import org.aiotrade.lib.math.indicator.IndicatorDescriptor
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.math.timeseries.TUnit
import org.aiotrade.lib.securities.dataserver.MoneyFlowContract
import org.aiotrade.lib.securities.dataserver.QuoteContract
import org.aiotrade.lib.securities.dataserver.RichInfoContract
import org.aiotrade.lib.securities.dataserver.RichInfoHisContract
import org.xml.sax.Attributes
import org.xml.sax.SAXException
import org.xml.sax.helpers.AttributesImpl
import org.xml.sax.helpers.DefaultHandler
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
class ContentParseHandler extends DefaultHandler {
  private val NUMBER_FORMAT = NumberFormat.getInstance
  
  private val classLoader = Thread.currentThread.getContextClassLoader

  private var content: Content = _
    
  private var indicatorDescriptor: IndicatorDescriptor = _
  private var factors: ArrayBuffer[Factor] = _
    
  private var drawingDescriptor: DrawingDescriptor = _
  private var handledChartMapPoints: mutable.Map[HandledChart, ArrayBuffer[ValuePoint]] = _
  private var handledChartClassName: String = _
  private var points: ArrayBuffer[ValuePoint] = _
    
  val DEBUG = false
    
  private var buffer: StringBuffer = new StringBuffer(500)
  private val context = mutable.Stack[Array[Object]]()
    
  private val calendar = Calendar.getInstance
    
  @throws(classOf[SAXException])
  final override def startElement(ns: String, name: String, qname: String, attrs: Attributes) {
    dispatch(true)
    context.push(Array(qname, new AttributesImpl(attrs)))
    if ("handle".equals(qname)) {
      handle_handle(attrs)
    } else if ("indicator".equals(qname)) {
      start_indicator(attrs)
    } else if ("chart".equals(qname)) {
      start_chart(attrs)
    } else if ("opt".equals(qname)) {
      handle_opt(attrs)
    } else if ("indicators".equals(qname)) {
      start_indicators(attrs)
    } else if ("drawings".equals(qname)) {
      start_drawings(attrs)
    } else if ("sec".equals(qname)) {
      start_sec(attrs)
    } else if ("layer".equals(qname)) {
      start_layer(attrs)
    } else if ("sources".equals(qname)) {
      start_sources(attrs)
    } else if ("source".equals(qname)) {
      start_source(attrs)
    } else if ("moneyflowsources".equals(qname)) {
      start_moneyflowsources(attrs)
    } else if ("moneyflowsource".equals(qname)) {
      start_moneyflowsource(attrs)
    } else if ("richinfosources".equals(qname)) {
      start_RichInfosources(attrs)
    } else if ("richinfosource".equals(qname)) {
      start_RichInfosource(attrs)
    } else if ("richinfohissources".equals(qname)) {
      start_RichInfoHissources(attrs)
    } else if ("richinfohissource".equals(qname)) {
      start_RichInfoHissource(attrs)
    }

  }
    
  /**
   *
   * This SAX interface method is implemented by the parser.
   */
  @throws(classOf[SAXException])
  final override def endElement(ns: String, name: String, qname: String) {
    dispatch(false);
    context.pop
    if ("indicator".equals(qname)) {
      end_indicator()
    } else if ("chart".equals(qname)) {
      end_chart()
    } else if ("indicators".equals(qname)) {
      end_indicators()
    } else if ("drawings".equals(qname)) {
      end_drawings()
    } else if ("sec".equals(qname)) {
      end_sec()
    } else if ("layer".equals(qname)) {
      end_layer()
    } else if ("sources".equals(qname)) {
      end_sources()
    }else if ("sources_richinfo".equals(qname)) {
      end_RichInfosources()
    }else if ("sources_richinfohis".equals(qname)) {
      end_RichInfoHissources()
    }
  }
    
    
  @throws(classOf[SAXException])
  private def dispatch(fireOnlyIfMixed: Boolean)  {
    if (fireOnlyIfMixed && buffer.length == 0) {
      return; //skip it
    }
    val ctx = context.top
    val here = ctx(0).asInstanceOf[String]
    val attrs = ctx(1).asInstanceOf[Attributes]
    buffer.delete(0, buffer.length)
  }
    
    
  @throws(classOf[SAXException])
  def handle_handle(meta: Attributes) {
    if (DEBUG) {
      System.err.println("handle_handle: " + meta)
    }
    try {
      val point = new ValuePoint
      point.t = NUMBER_FORMAT.parse(meta.getValue("t").trim).longValue
      point.v = NUMBER_FORMAT.parse(meta.getValue("v").trim).doubleValue
      points += point
    } catch {case ex: ParseException => ex.printStackTrace}
  }
    
  @throws(classOf[SAXException])
  def start_indicator(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_indicator: " + meta.getValue("class"))
    }
    indicatorDescriptor = new IndicatorDescriptor
    indicatorDescriptor.active = (meta.getValue("active").trim).toBoolean
    indicatorDescriptor.serviceClassName = meta.getValue("class")
    indicatorDescriptor.uniSymbol = meta.getValue("symbol")
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt)
    indicatorDescriptor.freq = freq
        
    factors = new ArrayBuffer
  }
    
  @throws(classOf[SAXException])
  def end_indicator() {
    if (DEBUG) {
      System.err.println("end_indicator()")
    }
    indicatorDescriptor.factors = factors.toArray
    content.addDescriptor(indicatorDescriptor)
  }
    
  @throws(classOf[SAXException])
  def start_chart(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_chart: " + meta)
    }
    handledChartClassName = meta.getValue("class")
    points = new ArrayBuffer[ValuePoint]
  }
    
  @throws(classOf[SAXException])
  def end_chart() {
    if (DEBUG) {
      System.err.println("end_chart()")
    }
        
    val handledChart = try {
      Class.forName(handledChartClassName, true, classLoader).newInstance.asInstanceOf[HandledChart]
    } catch {case ex: Exception => ex.printStackTrace; null}
    
    if (handledChart !=null) {
      handledChartMapPoints.put(handledChart, points)
    }
  }
    
  @throws(classOf[SAXException])
  def handle_opt(meta: Attributes) {
    if (DEBUG) {
      System.err.println("handle_opt: " + meta.getValue("value"))
    }
        
    val nameStr     = meta.getValue("name")
    val valueStr    = meta.getValue("value")
    val stepStr     = meta.getValue("step")
    val minValueStr = meta.getValue("minvalue")
    val maxValueStr = meta.getValue("maxvalue")
        
    try {
      val value    = NUMBER_FORMAT.parse(valueStr.trim).doubleValue
      val step     = if (stepStr     == null) 1.0 else NUMBER_FORMAT.parse(stepStr.trim).doubleValue
      val minValue = if (minValueStr == null) Double.MinValue else NUMBER_FORMAT.parse(minValueStr.trim).doubleValue
      val maxValue = if (maxValueStr == null) Double.MaxValue else NUMBER_FORMAT.parse(maxValueStr.trim).doubleValue
            
      val factor = new Factor(nameStr, value, step, minValue, maxValue)
      factors += factor
    } catch {case ex: ParseException => ex.printStackTrace}
  }
    
  @throws(classOf[SAXException])
  def start_indicators(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_indicators: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_indicators() {
    if (DEBUG) {
      System.err.println("end_indicators()")
    }
        
  }

  @throws(classOf[SAXException])
  def start_drawings(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_drawings: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_drawings() {
    if (DEBUG)
      System.err.println("end_drawings()")
  }
    
  @throws(classOf[SAXException])
  def start_sec(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_sofic: " + meta.getValue("unisymbol"))
    }
    val uniSymbol = meta.getValue("unisymbol")
    content = new Content(uniSymbol)
  }
    
  @throws(classOf[SAXException])
  def end_sec()  {
    if (DEBUG) {
      System.err.println("end_sofic()")
    }
  }
    
  @throws(classOf[SAXException])
  def start_source(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_source: " + meta)
    }
        
    val dataContract = new QuoteContract
        
    dataContract.active = meta.getValue("active").trim.toBoolean
    dataContract.serviceClassName = meta.getValue("class")
    dataContract.srcSymbol = meta.getValue("symbol")
    dataContract.datePattern = Option(meta.getValue("dateformat"))
        
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt
    )
    dataContract.freq = freq
        
    dataContract.isRefreshable = meta.getValue("refreshable").trim.toBoolean
    dataContract.refreshInterval = meta.getValue("refreshinterval").trim.toInt
        
    try {
      dataContract.fromTime = meta.getValue("fromTime").trim.toLong
      dataContract.toTime = meta.getValue("toTime").trim.toLong
    } catch {case ex: Exception => ex.printStackTrace}
        
    dataContract.urlString = meta.getValue("url")
        
    content.addDescriptor(dataContract)
  }
    
  @throws(classOf[SAXException])
  def start_sources(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_sources: " + meta)
    }
  }
    
  @throws(classOf[SAXException])
  def end_sources() {
    if (DEBUG) {
      System.err.println("end_sources()")
    }
  }
  
  @throws(classOf[SAXException])
  def start_moneyflowsources(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_moneyflowsources: " + meta)
    }
  }

  @throws(classOf[SAXException])
  def start_moneyflowsource(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_moneyflowsource: " + meta)
    }

    val dataContract = new MoneyFlowContract

    dataContract.active = meta.getValue("active").trim.toBoolean
    dataContract.serviceClassName = meta.getValue("class")
    dataContract.srcSymbol = meta.getValue("symbol")
    dataContract.datePattern = Option(meta.getValue("dateformat"))

    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt
    )
    dataContract.freq = freq

    dataContract.isRefreshable = meta.getValue("refreshable").trim.toBoolean
    dataContract.refreshInterval = meta.getValue("refreshinterval").trim.toInt

    try {
      dataContract.fromTime = meta.getValue("fromTime").trim.toLong
      dataContract.toTime = meta.getValue("toTime").trim.toLong
    } catch {case ex: Exception => ex.printStackTrace}

    dataContract.urlString = meta.getValue("url")

    content.addDescriptor(dataContract)
  }
  
  @throws(classOf[SAXException])
  def start_RichInfosource(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_RichInfosource: " + meta)
    }

    val dataContract = new RichInfoContract

    dataContract.active = meta.getValue("active").trim.toBoolean
    dataContract.serviceClassName = meta.getValue("class")
    dataContract.srcSymbol = meta.getValue("symbol")
    dataContract.datePattern = Option(meta.getValue("dateformat"))

    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt
    )
    dataContract.freq = freq

    dataContract.isRefreshable = meta.getValue("refreshable").trim.toBoolean
    dataContract.refreshInterval = meta.getValue("refreshinterval").trim.toInt

    try {
      dataContract.fromTime = meta.getValue("fromTime").trim.toLong
      dataContract.toTime = meta.getValue("toTime").trim.toLong
    } catch {case ex: Exception => ex.printStackTrace}

    dataContract.urlString = meta.getValue("url")

    content.addDescriptor(dataContract)
  }

  @throws(classOf[SAXException])
  def start_RichInfosources(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_RichInfosources: " + meta)
    }
  }

  @throws(classOf[SAXException])
  def end_RichInfosources() {
    if (DEBUG) {
      System.err.println("end_RichInfosources()")
    }

  }

  @throws(classOf[SAXException])
  def start_RichInfoHissource(meta: Attributes)  {
    if (DEBUG) {
      System.err.println("start_RichInfoHissource: " + meta)
    }

    val dataContract = new RichInfoHisContract

    dataContract.active = meta.getValue("active").trim.toBoolean
    dataContract.serviceClassName = meta.getValue("class")
    dataContract.srcSymbol = meta.getValue("symbol")
    dataContract.datePattern = Option(meta.getValue("dateformat"))

    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      meta.getValue("nunits").trim.toInt
    )
    dataContract.freq = freq

    dataContract.isRefreshable = meta.getValue("refreshable").trim.toBoolean
    dataContract.refreshInterval = meta.getValue("refreshinterval").trim.toInt

    try {
      dataContract.fromTime = meta.getValue("fromTime").trim.toLong
      dataContract.toTime = meta.getValue("toTime").trim.toLong
    } catch {case ex: Exception => ex.printStackTrace}

    dataContract.urlString = meta.getValue("url")

    content.addDescriptor(dataContract)
  }

  @throws(classOf[SAXException])
  def start_RichInfoHissources(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_RichInfoHissources: " + meta)
    }
  }

  @throws(classOf[SAXException])
  def end_RichInfoHissources() {
    if (DEBUG) {
      System.err.println("end_RichInfoHissources")
    }

  }


  @throws(classOf[SAXException])
  def start_layer(meta: Attributes) {
    if (DEBUG) {
      System.err.println("start_layer: " + meta)
    }
    drawingDescriptor = new DrawingDescriptor
    drawingDescriptor.serviceClassName = meta.getValue("name")
    val freq = new TFreq(
      TUnit.withName(meta.getValue("unit")).asInstanceOf[TUnit],
      Integer.parseInt(meta.getValue("nunits").trim))
    drawingDescriptor.freq = freq
        
    handledChartMapPoints = mutable.Map()
  }
    
  @throws(classOf[SAXException])
  def end_layer()  {
    if (DEBUG) {
      System.err.println("end_layer()")
    }
    drawingDescriptor.setHandledChartMapPoints(handledChartMapPoints)
    content.addDescriptor(drawingDescriptor)
  }
    
  def getContent: Content = {
    content
  }
        
}
