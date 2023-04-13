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
package org.aiotrade.lib.math.timeseries.datasource

import java.util.Calendar
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor
import scala.reflect.ClassTag

/**
 * Securities' data source request contract. It know how to find and invoke
 * server by call createBindInstance().
 *
 * We simplely inherit Descriptor, we may think the bindClass provides
 * service for descriptor.
 *
 * most fields' default value should be OK.
 * 
 * @param [S] DataServer
 * @author Caoyuan Deng
 */
abstract class DataContract[S: ClassTag] extends Descriptor[S] {
  private val log = Logger.getLogger(this.getClass.getName)
  
  @transient var reqId = 0

  /** symbol in source */
  var srcSymbol: String = _
   
  var datePattern: Option[String] = None
  var urlString: String = ""
  var isRefreshable: Boolean = false
  var refreshInterval: Int = 5000 // ms

  var toTime = Calendar.getInstance.getTimeInMillis
  var fromTime = 0L
  var loadedTime = 0L

  def isFreqSupported(freq: TFreq): Boolean
  
  /**
   * All dataserver will be implemented as singleton
   * @param none args are needed.
   */
  override 
  def createServiceInstance(args: Any*): Option[S] = {
    lookupServiceTemplate(m.runtimeClass.asInstanceOf[Class[S]], "DataServers")
  }

  override 
  def toString: String = displayName

  override 
  def clone: DataContract[S] = {
    try {
      super.clone.asInstanceOf[DataContract[S]]
    } catch {
      case ex: CloneNotSupportedException => log.log(Level.SEVERE, ex.getMessage, ex); null
    }
  }
}

