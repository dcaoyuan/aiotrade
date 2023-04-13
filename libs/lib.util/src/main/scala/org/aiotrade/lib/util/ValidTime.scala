/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.util

/**
 * A class to get the valid time of something
 * 
 * @param ref       What the valid time is talking about
 * @param validFrom the time valid from, included
 * @param validTo   the time valid to, included.
 * 
 * @author Caoyuan Deng
 */
final case class ValidTime[T](ref: T, var validFrom: Long, var validTo: Long) extends Ordered[ValidTime[T]] {

  /** 
   * time >= validFrom && (validTo == 0 || time <= validTo) 
   */
  def isValid(time: Long): Boolean = time >= validFrom && (validTo == 0 || time <= validTo)
  def nonValid(time: Long): Boolean = !isValid(time)
  
  def isIn (prevTime: Long, time: Long): Boolean = nonValid(prevTime) && isValid(time)
  def isOut(prevTime: Long, time: Long): Boolean = isValid(prevTime) && nonValid(time)
  
  override 
  def hashCode = {
    var hash = 7
    hash = 59 * hash + ref.hashCode
    hash = 59 * hash + (validFrom ^ (validFrom >>> 32)).toInt
    hash = 59 * hash + (validTo ^ (validTo >>> 32)).toInt
    hash
  }
  
  override 
  def equals(a: Any) = a match {
    case that: ValidTime[T] => that.ref == this.ref && that.validFrom == this.validFrom && that.validTo == this.validTo
    case _ => false
  }
  
  def compare(that: ValidTime[T]): Int = {
    if (validFrom == that.validFrom) {
      validTo compare that.validTo
    } else if (validFrom < that.validFrom) {
      -1
    } else {
      1
    }
  }
  
  override 
  def toString = "" + validFrom + " - " + validTo + ": " + ref
}
