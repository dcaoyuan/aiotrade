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

/**
 * 
 * @author Caoyuan Deng
 */
import java.io.ByteArrayOutputStream
import java.util.logging.Level
import java.util.logging.Logger

object Json {
  private val log = Logger.getLogger(this.getClass.getName)
  
  type Json = Either[collection.Map[String, _], collection.Seq[_]]
  type Object = collection.Map[String, collection.Map[String, _]]

  val TRUE_CHARS      = Array('t', 'r', 'u', 'e')
  val FALSE_CHARS     = Array('f', 'a', 'l', 's', 'e')
  val NULL_CHARS      = Array('n', 'u', 'l', 'l')
  val HEX_CHARS       = Array('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')
  val VALUE_SEPARATOR = ','
  val NAME_SEPARATOR  = ':'
  val OBJECT_START    = '{'
  val OBJECT_END      = '}'
  val ARRAY_START     = '['
  val ARRAY_END       = ']'

  object Event {
    /** Event indicating a JSON string value, including member names of objects */
    val STRING = 1
    /** Event indicating a JSON number value which fits into a signed 64 bit integer */
    val LONG = 2
    /**
     * Event indicating a JSON number value which has a fractional part or an exponent
     * and with string length <= 23 chars not including sign.  This covers
     * all representations of normal values for Double.toString.
     */
    val NUMBER = 3
    /**
     * Event indicating a JSON number value that was not produced by toString of any
     * Java primitive numerics such as Double or Long.  It is either
     * an integer outside the range of a 64 bit signed integer, or a floating
     * point value with a string representation of more than 23 chars.
     */
    val BIGNUMBER = 4
    /** Event indicating a JSON boolean */
    val BOOLEAN = 5
    /** Event indicating a JSON null */
    val NULL = 6
    /** Event indicating the start of a JSON object */
    val OBJECT_START = 7
    /** Event indicating the end of a JSON object */
    val OBJECT_END = 8
    /** Event indicating the start of a JSON array */
    val ARRAY_START = 9
    /** Event indicating the end of a JSON array */
    val ARRAY_END = 10
    /** Event indicating the end of input has been reached */
    val EOF = 11
  }
  
  def encode(x: Any): Array[Byte] = {
    val out = new ByteArrayOutputStream
    val jsonout = new JsonOutputStreamWriter(out, "utf-8")

    try {
      jsonout.jsonWrite(x)
      jsonout.close
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
    }

    out.toByteArray
  }
  
  def decode(json: String): Any = {
    JsonBuilder.readJson(json)
  }
  
  def decode(json: Array[Byte]): Any = {
    
  }

}



