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
package org.aiotrade.modules.ui

import java.util.logging.LogManager
import org.openide.ErrorManager

/**
 *
 * @author Caoyuan Deng
 */
class NetBeansLogManager extends LogManager {
    
  val errorManager = ErrorManager.getDefault
    
  def log(severity: Int, message: String) {
    errorManager.log(severity, message)
  }
    
  def log(message: String) {
    errorManager.log(message)
  }
    
  def info(message: String) {
    errorManager.log(ErrorManager.INFORMATIONAL, message)
  }
    
  def isDebugEnabled = {
    true
  }
    
  def debug(message: String) {
    errorManager.log(ErrorManager.INFORMATIONAL, message)
  }
    
  def debug(t: Throwable) {
    errorManager.notify(ErrorManager.INFORMATIONAL, t)
  }
    
  def error(message: String) {
    errorManager.log(ErrorManager.ERROR, message)
  }
    
  def error(t: Throwable) {
    errorManager.notify(ErrorManager.ERROR, t)
  }
    
  def notify(severity: Int, t: Throwable) {
    errorManager.notify(severity, t)
  }
    
  def	notify(t: Throwable) {
    errorManager.notify(t)
  }
    
}
