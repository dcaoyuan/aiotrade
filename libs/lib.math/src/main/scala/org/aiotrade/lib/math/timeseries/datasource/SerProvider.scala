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

import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.util.actors.Publisher

/**
 *
 * @author Caoyuan Deng
 */
trait SerProvider extends Publisher {
  type T <: BaseTSer
  type C <: DataContract[_]
    
  /**
   * Load sers, can be called to load ser whenever
   * If there is already a dataServer is running and not finished, don't load again.
   * @return boolean: if run sucessfully, ie. load begins, return true, else return false.
   */
  def loadSer(ser: T): Boolean
  def putSer(ser: T)
  def resetSers

  def uniSymbol: String
  def uniSymbol_=(symbol: String)
    
  def name: String
    
  def stopAllDataServer
    
  def serOf(freq: TFreq): Option[T]

  def description: String
  def description_=(description: String)

  /**
   * The content of each symbol should be got automatailly from PersistenceManager.restoreContent
   * and keep it there without being refered to another one, so, we only give getter without setter.
   */
  def content: Content
  
  /**
   * A helper method which can be overridden to get another ser provider from symbol
   */
  def serProviderOf(uniSymbol: String): Option[SerProvider]
}

