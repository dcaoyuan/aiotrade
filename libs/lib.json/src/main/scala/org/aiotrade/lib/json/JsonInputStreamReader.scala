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

import java.io.InputStream
import java.io.InputStreamReader
import scala.collection.mutable.ArrayBuffer

/**
 * 
 * @author Caoyuan Deng
 */
class JsonInputStreamReader(in: InputStream, charsetName: String)  extends InputStreamReader(in, charsetName) {
  private val classLoader = Thread.currentThread.getContextClassLoader

  private lazy val ret = {
    JsonBuilder.readJson(new InputStreamReader(in)) match {
      case map: Json.Object if map.size == 1 =>
        val (name, fields) = map.iterator.next
        readObject(name, fields)
      case seq: collection.Seq[_] =>
        val ret = new ArrayBuffer[Any]
        val xs = seq.iterator
        while (xs.hasNext) {
          val obj = xs.next match {
            case map: Json.Object if map.size == 1 =>
              val (name, fields) = map.iterator.next
              readObject(name, fields)
          }
          ret += obj
        }
        ret.toArray
      case x =>
        println(x)
        x
    }
  }

  private def readObject(clzName: String, fields: collection.Map[String, _]): Any = {
    try {
      Class.forName(clzName, true, classLoader).newInstance match {
        case x: JsonSerializable => x.readJson(fields); x
        case _ => null
      }
    } catch {
      case ex: ClassNotFoundException => null
    }
  }

  def readObject: Any = ret
}
