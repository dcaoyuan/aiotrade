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


import java.io.Reader
import java.math.BigDecimal
import java.math.BigInteger
import org.aiotrade.lib.io.RestReader
import scala.collection.mutable.ListBuffer

/**
 * 
 * @author Caoyuan Deng
 */
object JsonBuilder {  
  def readJson(json: String): Any = {
    val parser = new JsonParser(new RestReader(json))
    new JsonBuilder(parser).getVal
  }

  def readJson(json: Reader): Any = {
    val parser = new JsonParser(new RestReader(json))
    new JsonBuilder(parser).getVal
  }
}

class JsonBuilder(parser: JsonParser) {
  import Json.Event._

  if (parser.lastEvent == 0) parser.nextEvent

  def getVal = parser.lastEvent match {
    case STRING       => getString
    case LONG         => getLong
    case NUMBER       => getNumber
    case BIGNUMBER    => getBigNumber
    case BOOLEAN      => getBoolean
    case NULL         => getNull
    case OBJECT_START => getObject
    case OBJECT_END   => () // or ERROR?
    case ARRAY_END    => () // or ERROR?
    case ARRAY_START  => getArray
    case EOF          => () // or ERROR?
    case _            => () // or ERROR?
  }

  def getObject: Map[String, _] = {
    var elems = Map[String, Any]()
    while (parser.nextEvent != OBJECT_END) {
      val key = getString
      parser.nextEvent
      elems += (key -> getVal)
    }
    elems
  }

  def getArray: List[_] = {
    val elems = new ListBuffer[Any]
    while (parser.nextEvent != ARRAY_END) {
      elems += getVal
    }
    elems.toList
  }
    
  def getString = parser.getString

  def getLong = parser.getLong

  def getNumber = {
    val str = parser.getNumberChars.toString
    val num = str.toDouble

    if (java.lang.Double.isInfinite(num)) new BigDecimal(str) else num
  }

  def getBigNumber: Any = {
    val chars = parser.getNumberChars.toCharArray
    var isBigDec = false
    var i = 0
    while (i < chars.length) {
      chars(i) match {
        case ',' | 'e' | 'E' => return new BigDecimal(chars)
        case _ => // go on
      }
      i += 1
    }

    new BigInteger(chars.toString)
  }

  def getBoolean: Boolean = parser.getBoolean
    
  def getNull = parser.getNull
}
