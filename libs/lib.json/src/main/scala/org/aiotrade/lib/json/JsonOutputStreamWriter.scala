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
package org.aiotrade.lib.json

import java.io.OutputStream
import java.io.OutputStreamWriter

/**
 * 
 * @author Caoyuan Deng
 */
class JsonOutputStreamWriter(out: OutputStream, charsetName: String) extends OutputStreamWriter(out, charsetName) {
  def write(x: JsonSerializable) {
    write('{')

    jsonWrite(x.getClass.getName)

    write(':')

    write('{')
    x.writeJson(this)
    write('}')

    write('}')
  }

  def write(arr: Array[_]) {
    write('[')
    var i = 0
    while (i < arr.length) {
      val x = arr(i)
      jsonWrite(x)
      i += 1
      if (i < arr.length) {
        write(',')
      }
    }
    write(']')
  }

  def write(seq: collection.Seq[_]) {
    write('[')
    val xs = seq.iterator
    while (xs.hasNext) {
      val x = xs.next
      jsonWrite(x)
      if (xs.hasNext) {
        write(',')
      }
    }
    write(']')
  }

  def write(map: collection.Map[String, _]) {
    write('{')
    val xs = map.iterator
    while (xs.hasNext) {
      val (name, value) = xs.next
      jsonWrite(name, value)
      if (xs.hasNext) {
        write(',')
      }
    }
    write('}')
  }

  /**
   * write field or pair
   */
  def write(name: String, value: Any) {
    jsonWrite(name)

    write(':')

    jsonWrite(value)
  }

  def jsonWrite(value: Any) {
    value match {
      case x: String =>
        write('"')
        write(x)
        write('"')
      case x: Int => write(x.toString)
      case x: Long => write(x.toString)
      case x: Float => write(x.toString)
      case x: Double => write(x.toString)
      case x: Boolean => write(x.toString)
      case (k: String, v: Any) => write(k, v)
      case x: Array[_] => write(x)
      case x: collection.Seq[_] => write(x)
      case x: collection.Map[String, _] => write(x)
      case x: JsonSerializable => write(x)
      case _ => throw new UnsupportedOperationException(value + " cannot be json serialized")
    }
  }

}