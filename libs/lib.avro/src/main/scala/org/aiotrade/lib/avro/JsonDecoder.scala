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
package org.aiotrade.lib.avro

import java.io.IOException
import java.io.EOFException
import java.io.InputStream

import org.apache.avro.AvroTypeException
import org.apache.avro.Schema
import org.apache.avro.io.ParsingDecoder
import org.apache.avro.io.parsing.JsonGrammarGenerator
import org.apache.avro.io.parsing.Parser
import org.apache.avro.io.parsing.Symbol
import org.apache.avro.util.Utf8
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken

object JsonDecoder {
  private val CHARSET = "ISO-8859-1"
  private val jsonFactory = new JsonFactory()
  
  private def getSymbol(schema: Schema): Symbol = {
    if (null == schema) {
      throw new NullPointerException("Schema cannot be null!")
    }
    new JsonGrammarGenerator().generate(schema)
  }

  @throws(classOf[IOException])
  def apply(schema: Schema, in: InputStream) = new JsonDecoder(getSymbol(schema), Left(in))
  
  @throws(classOf[IOException])
  def apply(schema: Schema, in: String) = new JsonDecoder(JsonDecoder.getSymbol(schema), Right(in))
}

/** A {@link org.apache.avro.io.Decoder} for Avro's JSON data encoding. 
 * </p>
 * JsonDecoder is not thread-safe.
 */
@throws(classOf[IOException])
class JsonDecoder private (root: Symbol, _in: Either[InputStream, String]) extends ParsingDecoder(root) with Parser.ActionHandler {
  private var in: JsonParser = _

  _in match {
    case Left(x: InputStream) => configure(x)
    case Right(x: String) => configure(x)
  }
  
  /**
   * Reconfigures this JsonDecoder to use the InputStream provided.
   * <p/>
   * If the InputStream provided is null, a NullPointerException is thrown.
   * <p/>
   * Otherwise, this JsonDecoder will reset its state and then
   * reconfigure its input.
   * @param in
   *   The IntputStream to read from. Cannot be null.
   * @throws IOException
   * @return this JsonDecoder
   */
  @throws(classOf[IOException])
  def configure(in: InputStream): JsonDecoder = {
    if (null == in) {
      throw new NullPointerException("InputStream to read from cannot be null!")
    }
    parser.reset()
    this.in = JsonDecoder.jsonFactory.createJsonParser(in)
    this.in.nextToken()
    this
  }
  
  /**
   * Reconfigures this JsonDecoder to use the String provided for input.
   * <p/>
   * If the String provided is null, a NullPointerException is thrown.
   * <p/>
   * Otherwise, this JsonDecoder will reset its state and then
   * reconfigure its input.
   * @param in
   *   The String to read from. Cannot be null.
   * @throws IOException
   * @return this JsonDecoder
   */
  @throws(classOf[IOException])
  def configure(in: String): JsonDecoder = {
    if (null == in) {
      throw new NullPointerException("String to read from cannot be null!")
    }
    parser.reset()
    this.in = new JsonFactory().createJsonParser(in)
    this.in.nextToken()
    this
  }

  @throws(classOf[IOException])
  private def advance(symbol: Symbol) {
    this.parser.processTrailingImplicitActions()
    if (in.getCurrentToken() == null && this.parser.depth() == 1)
      throw new EOFException()
    parser.advance(symbol)
  }

  @throws(classOf[IOException])
  override
  def readNull() {
    advance(Symbol.NULL)
    if (in.getCurrentToken() == JsonToken.VALUE_NULL) {
      in.nextToken()
    } else {
      throw error("null")
    }
  }

  @throws(classOf[IOException])
  override
  def readBoolean(): Boolean = {
    advance(Symbol.BOOLEAN)
    val t = in.getCurrentToken() 
    if (t == JsonToken.VALUE_TRUE || t == JsonToken.VALUE_FALSE) {
      in.nextToken()
      return t == JsonToken.VALUE_TRUE
    } else {
      throw error("boolean")
    }
  }

  @throws(classOf[IOException])
  override
  def readInt(): Int = {
    advance(Symbol.INT)
    if (in.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
      val result = in.getIntValue()
      in.nextToken()
      return result
    } else {
      throw error("int")
    }
  }
    
  @throws(classOf[IOException])
  override
  def readLong(): Long = {
    advance(Symbol.LONG)
    if (in.getCurrentToken() == JsonToken.VALUE_NUMBER_INT) {
      val result = in.getLongValue()
      in.nextToken()
      return result
    } else {
      throw error("long")
    }
  }

  @throws(classOf[IOException])
  override
  def readFloat(): Float = {
    advance(Symbol.FLOAT)
    if (in.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
      val result = in.getFloatValue()
      in.nextToken()
      return result
    } else {
      throw error("float")
    }
  }

  @throws(classOf[IOException])
  override
  def readDouble(): Double = {
    advance(Symbol.DOUBLE)
    if (in.getCurrentToken() == JsonToken.VALUE_NUMBER_FLOAT) {
      val result = in.getDoubleValue()
      in.nextToken()
      return result
    } else {
      throw error("double")
    }
  }
    
  @throws(classOf[IOException])
  override
  def readString(old: Utf8): Utf8 = {
    advance(Symbol.STRING)
    if (parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
      parser.advance(Symbol.MAP_KEY_MARKER)
      if (in.getCurrentToken() != JsonToken.FIELD_NAME) {
        throw error("map-key")
      }
    } else {
      if (in.getCurrentToken() != JsonToken.VALUE_STRING) {
        throw error("string")
      }
    }
    val result = in.getText()
    in.nextToken()
    return new Utf8(result)
  }

  @throws(classOf[IOException])
  override
  def skipString() {
    advance(Symbol.STRING)
    if (parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
      parser.advance(Symbol.MAP_KEY_MARKER)
      if (in.getCurrentToken() != JsonToken.FIELD_NAME) {
        throw error("map-key")
      }
    } else {
      if (in.getCurrentToken() != JsonToken.VALUE_STRING) {
        throw error("string")
      }
    }
    in.nextToken()
  }

  @throws(classOf[IOException])
  override
  def readBytes(old: java.nio.ByteBuffer): java.nio.ByteBuffer = {
    advance(Symbol.BYTES)
    if (in.getCurrentToken() == JsonToken.VALUE_STRING) {
      val result = readByteArray()
      in.nextToken()
      return java.nio.ByteBuffer.wrap(result)
    } else {
      throw error("bytes")
    }
  }

  @throws(classOf[IOException])
  private def readByteArray(): Array[Byte] = {
    in.getText().getBytes(JsonDecoder.CHARSET)
  }

  @throws(classOf[IOException])
  override
  def skipBytes() {
    advance(Symbol.BYTES)
    if (in.getCurrentToken() == JsonToken.VALUE_STRING) {
      in.nextToken()
    } else {
      throw error("bytes")
    }
  }

  @throws(classOf[IOException])
  private def checkFixed(size: Int) {
    advance(Symbol.FIXED)
    val top = parser.popSymbol().asInstanceOf[Symbol.IntCheckAction]
    if (size != top.size) {
      throw error(
        "Incorrect length for fixed binary: expected " +
        top.size + " but received " + size + " bytes.")
    }
  }
    
  @throws(classOf[IOException])
  override
  def readFixed(bytes: Array[Byte], start: Int, len: Int) {
    checkFixed(len)
    if (in.getCurrentToken() == JsonToken.VALUE_STRING) {
      val result = readByteArray()
      in.nextToken()
      if (result.length != len) {
        throw error("Expected fixed length " + len
                    + ", but got" + result.length)
      }
      System.arraycopy(result, 0, bytes, start, len)
    } else {
      throw error("fixed")
    }
  }

  @throws(classOf[IOException])
  override
  def skipFixed(length: Int) {
    checkFixed(length)
    doSkipFixed(length)
  }

  @throws(classOf[IOException])
  private def doSkipFixed(length: Int) {
    if (in.getCurrentToken() == JsonToken.VALUE_STRING) {
      val result = readByteArray()
      in.nextToken()
      if (result.length != length) {
        throw error("Expected fixed length " + length
                    + ", but got" + result.length)
      }
    } else {
      throw error("fixed")
    }
  }

  @throws(classOf[IOException])
  override
  protected def skipFixed() {
    advance(Symbol.FIXED)
    val top = parser.popSymbol().asInstanceOf[Symbol.IntCheckAction]
    doSkipFixed(top.size)
  }

  @throws(classOf[IOException])
  override
  def readEnum(): Int = {
    advance(Symbol.ENUM)
    val top = parser.popSymbol().asInstanceOf[Symbol.EnumLabelsAction]
    if (in.getCurrentToken() == JsonToken.VALUE_STRING) {
      in.getText()
      val n = top.findLabel(in.getText())
      if (n >= 0) {
        in.nextToken()
        return n
      }
      throw error("Unknown symbol in enum " + in.getText())
    } else {
      throw error("fixed")
    }
  }

  @throws(classOf[IOException])
  override
  def readArrayStart(): Long = {
    advance(Symbol.ARRAY_START)
    if (in.getCurrentToken() == JsonToken.START_ARRAY) {
      in.nextToken()
      return doArrayNext()
    } else {
      throw error("array-start")
    }
  }

  @throws(classOf[IOException])
  override
  def arrayNext(): Long = {
    advance(Symbol.ITEM_END)
    doArrayNext()
  }

  @throws(classOf[IOException])
  private def doArrayNext(): Long = {
    if (in.getCurrentToken() == JsonToken.END_ARRAY) {
      parser.advance(Symbol.ARRAY_END)
      in.nextToken()
      0
    } else {
      1
    }
  }

  @throws(classOf[IOException])
  override
  def skipArray(): Long = {
    advance(Symbol.ARRAY_START)
    if (in.getCurrentToken() == JsonToken.START_ARRAY) {
      in.skipChildren()
      in.nextToken()
      advance(Symbol.ARRAY_END)    
    } else {
      throw error("array-start")
    }
    0
  }

  @throws(classOf[IOException])
  override
  def readMapStart(): Long = {
    advance(Symbol.MAP_START)
    if (in.getCurrentToken() == JsonToken.START_OBJECT) {
      in.nextToken()
      return doMapNext()
    } else {
      throw error("map-start")
    }
  }

  @throws(classOf[IOException])
  override
  def mapNext(): Long = {
    advance(Symbol.ITEM_END)
    doMapNext()
  }

  @throws(classOf[IOException])
  private def doMapNext(): Long = {
    if (in.getCurrentToken() == JsonToken.END_OBJECT) {
      in.nextToken()
      advance(Symbol.MAP_END)
      0
    } else {
      1
    }
  }

  @throws(classOf[IOException])
  override
  def skipMap(): Long = {
    advance(Symbol.MAP_START)
    if (in.getCurrentToken() == JsonToken.START_OBJECT) {
      in.skipChildren()
      in.nextToken()
      advance(Symbol.MAP_END)    
    } else {
      throw error("map-start")
    }
    0
  }

  @throws(classOf[IOException])
  override
  def readIndex(): Int = {
    advance(Symbol.UNION)
    val a = parser.popSymbol().asInstanceOf[Symbol.Alternative]
    
    var n: Int = -1
    in.getCurrentToken match {
      case JsonToken.VALUE_NULL =>
        val label = "null"
        n = a.findLabel(label)
        if (n < 0)
          throw error("Unknown union branch " + label)
      case JsonToken.START_OBJECT =>
        in.nextToken() match {
          case JsonToken.FIELD_NAME =>
            val label = in.getText()
            in.nextToken()
            parser.pushSymbol(Symbol.UNION_END)
            n = a.findLabel(label)
            if (n < 0)
              throw error("Unknown union branch " + label)
          case _ => throw error("start-union")
        }
      case JsonToken.VALUE_NUMBER_FLOAT =>
        n = a.findLabel("float")
        if (n < 0) {
          n = a.findLabel("double")
        }
        if (n < 0)
          throw error("Lack union branch float or double")
      case JsonToken.VALUE_NUMBER_INT =>
        n = a.findLabel("int")
        if (n < 0) {
          n = a.findLabel("long")
        }
        if (n < 0)
          throw error("Lack union branch int or long")
      case JsonToken.VALUE_FALSE | JsonToken.VALUE_TRUE =>
        n = a.findLabel("boolean")
        if (n < 0)
          throw error("Lack union branch boolean")
      case JsonToken.VALUE_STRING =>
        n = a.findLabel("string")
        if (n < 0)
          throw error("Lack union branch string")
      case _ => throw error("start-union")
    }
    
    parser.pushSymbol(a.getSymbol(n))
    n
  }

  @throws(classOf[IOException])
  override
  def doAction(input: Symbol, top: Symbol): Symbol = {
    if (top.isInstanceOf[Symbol.FieldAdjustAction]) {
      val fa = top.asInstanceOf[Symbol.FieldAdjustAction];
      if (in.getCurrentToken() == JsonToken.FIELD_NAME) {
        val fn = in.getCurrentName()
        if (fa.fname.equals(fn)) {
          in.nextToken()
          return null;
        } else {
          error("Expected field name " + fa.fname +
                " got " + in.getCurrentName())
        }
      }
    } else if (top == Symbol.RECORD_START) {
      if (in.getCurrentToken() == JsonToken.START_OBJECT) {
        in.nextToken()
      } else {
        throw error("record-start")
      }
    } else if (top == Symbol.RECORD_END || top == Symbol.UNION_END) {
      if (in.getCurrentToken() == JsonToken.END_OBJECT) {
        in.nextToken()
      } else {
        throw error(if (top == Symbol.RECORD_END) "record-end" else "union-end")
      }
    } else {
      throw error("Unknown action symbol " + top)
    }
    null
  }

  private def error(tpe: String): AvroTypeException = {
    new AvroTypeException("Expected " + tpe + ". Got " + in.getCurrentToken())
  }

}
