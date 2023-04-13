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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.Schema
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.EncoderFactory
import scala.reflect._
import scala.reflect.runtime.universe._

/**
 * 
 * @author Caoyuan Deng
 */
object Avro {
  private val log = Logger.getLogger(this.getClass.getName)
  
  protected[avro] val AVRO = 0
  protected[avro] val JSON = 1

  def encode[T](value: T, schema: Schema, contentType: Int): Array[Byte] = {
    var out: ByteArrayOutputStream = null
    try {
      out = new ByteArrayOutputStream()
    
      val encoder = contentType match {
        case JSON => JsonEncoder(schema, out)
        case AVRO => EncoderFactory.get.binaryEncoder(out, null)
      }
      
      val writer = ReflectDatumWriter[T](schema)
      writer.write(value, encoder)
      encoder.flush()
      
      out.toByteArray
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex); Array[Byte]()
    } finally {
      if (out != null) try {out.close} catch {case _: Throwable =>}
    }
  }
  
  def decode[T](bytes: Array[Byte], schema: Schema, valueType: Class[T], contentType: Int): Option[T] = {
    var in: InputStream = null
    try {
      in = new ByteArrayInputStream(bytes)
      
      val decoder = contentType match {
        case JSON => JsonDecoder(schema, in)
        case AVRO => DecoderFactory.get.binaryDecoder(in, null)
      }
      
      val reader = ReflectDatumReader[T](schema)
      reader.read(null.asInstanceOf[T], decoder) match {
        case null => 
          import ClassHelper._
          valueType match {
            case UnitClass | JVoidClass  => Some(().asInstanceOf[T])
            case NullClass => Some(null.asInstanceOf[T])
            case _ => None
          }
        case value => Some(value)
      }
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex); None
    } finally {
      if (in != null) try {in.close} catch {case _: Throwable =>}
    }
  }
  
  protected[avro] def tpeParams[T: TypeTag : ClassTag]: List[Class[_]] = tpeParams(typeOf[T])
  protected[avro] def tpeParams(tp: Type): List[Class[_]] = {
    tp match {
      case TypeRef(_, _, args) => 
        val m = runtimeMirror(Thread.currentThread.getContextClassLoader) // must use currentThread's classLoader
        args map {tpParam => 
          if (tpParam.typeSymbol == definitions.ArrayClass) { 
            // m.runtimeClass(..): If the Scala symbol is ArrayClass, a ClassNotFound exception is thrown
            // because there is no unique Java class corresponding to a Scala generic array
            tpeParams(tpParam) match {
              case Nil => 
                // @Note: classOf[Array[_]] returns: class java.lang.Object, meanwhile
                //   classOf[Array[AnyRef]] returns: class [Ljava.lang.Object;
                // We choose class java.lang.Object here, why? 
                classOf[Array[_]] 
              case componentTp :: _ =>
                java.lang.reflect.Array.newInstance(componentTp, 0).getClass
            }
          } else {
            m.runtimeClass(tpParam.typeSymbol.asClass)
          }
        }
      case _ => Nil
    }
  }
}
