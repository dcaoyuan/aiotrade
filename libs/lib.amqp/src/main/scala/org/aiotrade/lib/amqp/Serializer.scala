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
package org.aiotrade.lib.amqp

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.aiotrade.lib.avro.ReflectData
import org.aiotrade.lib.avro.ReflectDatumWriter
import org.aiotrade.lib.avro.Evt
import org.aiotrade.lib.avro.Msg
import org.apache.avro.io.EncoderFactory

/**
 * 
 * @author Caoyuan Deng
 */
object Serializer {
  /**
   * lzma properties with:
   * encoder.SetEndMarkerMode(true)     // must set true
   * encoder.SetDictionarySize(1 << 20) // 1048576
   */
  val lzmaProps = Array[Byte](93, 0, 0, 16, 0)
  
  def encodeJava(content: Any): Array[Byte] = {
    val out = new ByteArrayOutputStream
    val oout = new ObjectOutputStream(out)
    oout.writeObject(content)
    oout.close

    out.toByteArray
  }

  def decodeJava(body: Array[Byte]): Any = {
    val in = new ObjectInputStream(new ByteArrayInputStream(body))
    in.readObject
  }

  def encodeAvro(content: Any): Array[Byte] = {
    content match {
      case Msg(tag, value) => Evt.toAvro(value, tag)
      case _ =>
        // best trying
        val schema = ReflectData.get.getSchema(content.asInstanceOf[AnyRef].getClass)
        val bao = new ByteArrayOutputStream()
        val encoder = EncoderFactory.get.binaryEncoder(bao, null)
        val writer = ReflectDatumWriter[Any](schema)
        writer.write(content, encoder)
        encoder.flush()
        val body = bao.toByteArray
        bao.close
        body
    }
  }

  def decodeAvro(body: Array[Byte], tag: Int = Evt.NO_TAG): Any = {
    Evt.fromAvro(body, tag) match {
      case Some(x) => x
      case None => null
    }
  }
  
  def encodeJson(content: Any): Array[Byte] = {
    content match {
      case Msg(tag, value) => Evt.toJson(value, tag)
      case _ => Array[Byte]()
    }
  }

  def decodeJson(body: Array[Byte], tag: Int = Evt.NO_TAG): Any = {
    Evt.fromJson(body, tag) match {
      case Some(x) => x
      case None => null
    }
  }

  @throws(classOf[IOException])
  def gzip(input: Array[Byte]): Array[Byte] = {
    val out = new ByteArrayOutputStream()
    val bout = new BufferedOutputStream(new GZIPOutputStream(out))
    bout.write(input)
    bout.close

    val body = out.toByteArray

    out.close
    body
  }

  @throws(classOf[IOException])
  def ungzip(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val bin = new BufferedInputStream(new GZIPInputStream(in))
    val out = new ByteArrayOutputStream()

    val buf = new Array[Byte](1024)
    var len = -1
    while ({len = bin.read(buf); len > 0}) {
      out.write(buf, 0, len)
    }

    val body = out.toByteArray

    in.close
    bin.close
    out.close
    body
  }

  @throws(classOf[IOException])
  def lzma(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val out = new ByteArrayOutputStream

    val encoder = new SevenZip.Compression.LZMA.Encoder
    encoder.SetEndMarkerMode(true)     // must set true
    encoder.SetDictionarySize(1 << 20) // 1048576

    encoder.Code(in, out, -1, -1, null)

    val body = out.toByteArray

    in.close
    out.close
    body
  }

  @throws(classOf[IOException])
  def unlzma(input: Array[Byte]): Array[Byte] = {
    val in = new ByteArrayInputStream(input)
    val out = new ByteArrayOutputStream

    val decoder = new SevenZip.Compression.LZMA.Decoder
    decoder.SetDecoderProperties(lzmaProps)
    decoder.Code(in, out, -1)

    val body = out.toByteArray

    in.close
    out.close
    body
  }

}
