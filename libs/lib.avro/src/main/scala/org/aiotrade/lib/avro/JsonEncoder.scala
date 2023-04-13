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
import java.io.OutputStream
import java.util.BitSet

import org.apache.avro.AvroTypeException
import org.apache.avro.Schema
import org.apache.avro.io.ParsingEncoder
import org.apache.avro.io.parsing.JsonGrammarGenerator
import org.apache.avro.io.parsing.Parser
import org.apache.avro.io.parsing.Symbol
import org.apache.avro.util.Utf8
import org.codehaus.jackson.JsonEncoding
import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonGenerator

/**
 * 
 * @author Caoyuan Deng
 */
object JsonEncoder {
  val CHARSET = "ISO-8859-1"

  @throws(classOf[IOException])
  private def getJsonGenerator(out: OutputStream): JsonGenerator = {
    if (null == out)
      throw new NullPointerException("OutputStream cannot be null")
    new JsonFactory().createJsonGenerator(out, JsonEncoding.UTF8)
  }
  
  @throws(classOf[IOException])
  def apply(sc: Schema, _out: JsonGenerator): JsonEncoder = new JsonEncoder(sc, _out)

  @throws(classOf[IOException])
  def apply(sc: Schema, out: OutputStream): JsonEncoder = new JsonEncoder(sc, getJsonGenerator(out))

}

@throws(classOf[IOException])
class JsonEncoder(sc: Schema, _out: JsonGenerator) extends ParsingEncoder with Parser.ActionHandler {
  private var out: JsonGenerator = _
  protected val parser = new Parser(new JsonGrammarGenerator().generate(sc), this)
  
  configure(_out)
  
  /**
   * Has anything been written into the collections?
   */
  protected val isEmpty = new BitSet()

  @throws(classOf[IOException])
  override def flush() {
    parser.processImplicitActions()
    if (out != null) {
      out.flush()
    }
  }
  
  /**
   * Reconfigures this JsonEncoder to use the output stream provided.
   * <p/>
   * If the OutputStream provided is null, a NullPointerException is thrown.
   * <p/>
   * Otherwise, this JsonEncoder will flush its current output and then
   * reconfigure its output to use a default UTF8 JsonGenerator that writes
   * to the provided OutputStream.
   * 
   * @param out
   *          The OutputStream to direct output to. Cannot be null.
   * @throws IOException
   * @return this JsonEncoder
   */
   @throws(classOf[IOException])
   def configure(out: OutputStream): JsonEncoder = {
      this.configure(JsonEncoder.getJsonGenerator(out))
      this
    }
  
   /**
    * Reconfigures this JsonEncoder to output to the JsonGenerator provided.
    * <p/>
    * If the JsonGenerator provided is null, a NullPointerException is thrown.
    * <p/>
    * Otherwise, this JsonEncoder will flush its current output and then
    * reconfigure its output to use the provided JsonGenerator.
    * 
    * @param generator
    *          The JsonGenerator to direct output to. Cannot be null.
    * @throws IOException
    * @return this JsonEncoder
    */
   @throws(classOf[IOException])
   def configure(generator: JsonGenerator): JsonEncoder = {
      if (null == generator)
        throw new NullPointerException("JsonGenerator cannot be null")
      if (null != parser) {
        flush()
      }
      this.out = generator
      this
    }
  
   @throws(classOf[IOException])
   override def writeNull() {
      parser.advance(Symbol.NULL)
      out.writeNull()
    }

   @throws(classOf[IOException])
   override def writeBoolean(b: Boolean) {
      parser.advance(Symbol.BOOLEAN)
      out.writeBoolean(b)
    }

   @throws(classOf[IOException])
   override def writeInt(n: Int) {
      parser.advance(Symbol.INT)
      out.writeNumber(n)
    }

   @throws(classOf[IOException])
   override def writeLong(n: Long) {
      parser.advance(Symbol.LONG)
      out.writeNumber(n)
    }

  
   @throws(classOf[IOException])
   override def writeFloat(f: Float) {
      parser.advance(Symbol.FLOAT)
      out.writeNumber(f)
    }

  
   @throws(classOf[IOException])
   override def writeDouble(d: Double) {
      parser.advance(Symbol.DOUBLE)
      out.writeNumber(d)
    }

   @throws(classOf[IOException])
   override def writeString(utf8: Utf8) {
      writeString(utf8.toString())
    }
  
   @throws(classOf[IOException])
   override def writeString(str: String) {
      parser.advance(Symbol.STRING)
      if (parser.topSymbol() == Symbol.MAP_KEY_MARKER) {
        parser.advance(Symbol.MAP_KEY_MARKER)
        out.writeFieldName(str)
      } else {
        out.writeString(str)
      }
    }

   @throws(classOf[IOException])
   override def writeBytes(bytes: java.nio.ByteBuffer) {
      if (bytes.hasArray()) {
        writeBytes(bytes.array(), bytes.position(), bytes.remaining())
      } else {
        val b = new Array[Byte](bytes.remaining())
        var i = -1
        while ({i += 1; i < b.length}) {
          b(i) = bytes.get()
        }
        writeBytes(b)
      }
    }

   @throws(classOf[IOException])
   override def writeBytes(bytes: Array[Byte], start: Int, len: Int) {
      parser.advance(Symbol.BYTES)
      writeByteArray(bytes, start, len)
    }

   @throws(classOf[IOException])
   private def writeByteArray(bytes: Array[Byte], start: Int, len: Int) {
      out.writeString(new String(bytes, start, len, JsonEncoder.CHARSET))
    }

   @throws(classOf[IOException])
   override def writeFixed(bytes: Array[Byte], start: Int, len: Int) {
      parser.advance(Symbol.FIXED)
      val top = parser.popSymbol().asInstanceOf[Symbol.IntCheckAction]
      if (len != top.size) {
        throw new AvroTypeException(
          "Incorrect length for fixed binary: expected " +
          top.size + " but received " + len + " bytes.")
      }
      writeByteArray(bytes, start, len)
    }

   @throws(classOf[IOException])
   override def writeEnum(e: Int) {
      parser.advance(Symbol.ENUM)
      val top = parser.popSymbol().asInstanceOf[Symbol.EnumLabelsAction]
      if (e < 0 || e >= top.size) {
        throw new AvroTypeException(
          "Enumeration out of range: max is " +
          top.size + " but received " + e)
      }
      out.writeString(top.getLabel(e))
    }

   @throws(classOf[IOException])
   override def writeArrayStart() {
      parser.advance(Symbol.ARRAY_START)
      out.writeStartArray()
      push()
      isEmpty.set(depth())
    }

   @throws(classOf[IOException])
   override def writeArrayEnd() {
      if (! isEmpty.get(pos)) {
        parser.advance(Symbol.ITEM_END)
      }
      pop()
      parser.advance(Symbol.ARRAY_END)
      out.writeEndArray()
    }

   @throws(classOf[IOException])
   override def writeMapStart() {
      push()
      isEmpty.set(depth())

      parser.advance(Symbol.MAP_START)
      out.writeStartObject()
    }

   @throws(classOf[IOException])
   override def writeMapEnd() {
      if (! isEmpty.get(pos)) {
        parser.advance(Symbol.ITEM_END)
      }
      pop()

      parser.advance(Symbol.MAP_END)
      out.writeEndObject()
    }

   @throws(classOf[IOException])
   override def startItem() {
      if (! isEmpty.get(pos)) {
        parser.advance(Symbol.ITEM_END)
      }
      super.startItem()
      isEmpty.clear(depth())
    }

   @throws(classOf[IOException])
   override def writeIndex(unionIndex: Int) {
      parser.advance(Symbol.UNION)
      val top = parser.popSymbol().asInstanceOf[Symbol.Alternative]
      val symbol = top.getSymbol(unionIndex)
      symbol match {
        case Symbol.NULL =>
        case Symbol.STRING | Symbol.BYTES | Symbol.INT | Symbol.LONG | Symbol.FLOAT | Symbol.DOUBLE | Symbol.BOOLEAN => // primitives
        case _ =>
          out.writeStartObject()
          out.writeFieldName(top.getLabel(unionIndex))
          parser.pushSymbol(Symbol.UNION_END)
      }
      parser.pushSymbol(symbol)
    }

   @throws(classOf[IOException])
   override def doAction(input: Symbol, top: Symbol): Symbol = {
      if (top.isInstanceOf[Symbol.FieldAdjustAction]) {
        val fa = top.asInstanceOf[Symbol.FieldAdjustAction]
        out.writeFieldName(fa.fname)
      } else if (top == Symbol.RECORD_START) {
        out.writeStartObject()
      } else if (top == Symbol.RECORD_END || top == Symbol.UNION_END) {
        out.writeEndObject()
      } else {
        throw new AvroTypeException("Unknown action symbol " + top)
      }
      return null;
    }
   }
