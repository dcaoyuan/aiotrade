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
package org.aiotrade.lib.math.signal

/**
 *
 * @author Caoyuan Deng
 */
class Kind(_id: Int) {
  def this() = this(0) /* for serializable */

  protected[signal] def id: Int = _id
  
  def isSide:   Boolean = id > 0
  def isCorner: Boolean = id < 0
  
  override 
  def hashCode = _id
  
  override 
  def equals(a: Any) = a match {
    case x: Kind => x.id == _id
    case _ => false
  }
  
  override 
  def toString = id match {
    case  1 => "Enter long"
    case  2 => "Exit long"
    case  3 => "Enter short"
    case  4 => "Exit short"
    case  5 => "Enter picking"
    case  6 => "Exit picking"
    case  7 => "Cut loss"
    case  8 => "Take profit"

    case -1 => "Upper"
    case -2 => "Lower"
  }
}

object Kind {
  def withId(id: Int): Kind = id match {
    case  1 => Side.EnterLong
    case  2 => Side.ExitLong
    case  3 => Side.EnterShort
    case  4 => Side.ExitShort
    case  5 => Side.EnterPicking
    case  6 => Side.ExitPicking
    case  7 => Side.CutLoss
    case  8 => Side.TakeProfit
      
    case -1 => Corner.Upper
    case -2 => Corner.Lower
  }  
}

class Side(_id: => Int) extends Kind(_id) {
  def this() = this(0) /* for serializable */  
}

object Side {
  val EnterLong    = new Side(1)
  val ExitLong     = new Side(2)
  val EnterShort   = new Side(3)
  val ExitShort    = new Side(4)
  val EnterPicking = new Side(5)
  val ExitPicking  = new Side(6)
  val CutLoss      = new Side(7)
  val TakeProfit   = new Side(8)

  def withId(id: Int): Side = Kind.withId(id).asInstanceOf[Side]
}

class Corner(_id: => Int) extends Kind(_id) {
  def this() = this(0) /* for serializable */  
}

object Corner {
  val Upper = new Corner(-1)
  val Lower = new Corner(-2)

  def withId(id: Int): Corner = Kind.withId(id).asInstanceOf[Corner]
}


