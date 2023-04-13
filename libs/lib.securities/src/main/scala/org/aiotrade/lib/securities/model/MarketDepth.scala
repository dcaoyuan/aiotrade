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
package org.aiotrade.lib.securities.model


/**
 * 0 - bid price
 * 1 - bid size
 * 2 - ask price
 * 3 - ask size
 */
@serializable @cloneable
final class MarketDepth(_bidAsks: Array[Double]) {
  @transient var isChanged: Boolean = _

  def this() = this(null)

  def depth = _bidAsks.length / 4
  
  def bidPrice(idx: Int) = _bidAsks(idx * 4)
  def bidSize (idx: Int) = _bidAsks(idx * 4 + 1)
  def askPrice(idx: Int) = _bidAsks(idx * 4 + 2)
  def askSize (idx: Int) = _bidAsks(idx * 4 + 3)

  def setBidPrice(idx: Int, v: Double) = updateDepthValue(idx * 4, v)
  def setBidSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 1, v)
  def setAskPrice(idx: Int, v: Double) = updateDepthValue(idx * 4 + 2, v)
  def setAskSize (idx: Int, v: Double) = updateDepthValue(idx * 4 + 3, v)

  def bidAsks = _bidAsks
  def bidAsks_=(that: Array[Double]) {
    isChanged = false
    if (that.length != _bidAsks.length) {
      return
    }
    var i = -1
    val length = that.length
    while ({i += 1; i < length}) {
      updateDepthValue(i, that(i))
    }
  }

  private def updateDepthValue(idx: Int, v: Double) {
    if (_bidAsks(idx) != v) {
      isChanged = true
    }
    _bidAsks(idx) = v
  }
  
  override def toString = {
    new StringBuilder().append("MarketDepth(").append(_bidAsks.mkString("[", ",", "]")).append(")").toString
  }

}


object MarketDepth {
  val Empty = new MarketDepth(Array[Double]())

  def apply(bidAsks: Array[Double], copy: Boolean = false) = {
    if (copy) {
      val x = new Array[Double](bidAsks.length)
      System.arraycopy(bidAsks, 0, x, 0, x.length)
      new MarketDepth(x)
    } else new MarketDepth(bidAsks)
  }
}
