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

import org.aiotrade.lib.collection.ArrayList
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object MainTest {

  def main(args: Array[String]) {    
    testArrayBuffer
    testAutoSchema
    
    for (contentType <- List(Avro.JSON, Avro.AVRO)) {
      testTuple(contentType)
      testJavaVMap(contentType)
      testScalaVMap(contentType)
      testReflectClass(contentType)
      testReflectArrayOfClass(contentType)
      testCaseObject(contentType)
    }
  }
  
  def testArrayBuffer {
    println("\n==== testArrayBuffer: debug on it to see type of field array ====")
    // debug on it to see type of field array
    val alist = new ArrayList[Double]()
    val primitiveArray = alist.toArray
    println("The type of field array in ArrayList should be double[]")
    println(getSchema(classOf[ArrayList[Double]]))
    
    val abuff = new collection.mutable.ArrayBuffer[Double]()
    val objectArray = abuff.toArray
    println("The type of field array in ArrayBuffer is still Object[]")
    println(getSchema(classOf[collection.mutable.ArrayBuffer[Double]]))
    
    
    val tickers = new ArrayList[Ticker]()
    tickers += new Ticker
    val arr = tickers.toArray
    var i = -1
    while ({i += 1; i < arr.length}) {
      val ticker = arr(i)
      println(ticker)
    }
    
    val schemaArrayList = """{"type":"array","items":"double","java-class":"org.aiotrade.lib.collection.ArrayList"}"""
    val schema = Schema.parse(schemaArrayList)
    println(schema)
    val xs = new ArrayList[Double](100)
    xs ++= Array(1.0, 2.0, 3.0)
    // encode
    val bytes = Avro.encode(xs, schema, Avro.JSON)
    println(new String(bytes, "UTF-8"))
    // decode
    val decoded = Avro.decode(bytes, schema, classOf[ArrayList[Double]], Avro.JSON).get
    println(decoded)
  }
  
  def testAutoSchema {
    println("\n==== test auto schema ====")

    println(getSchema(classOf[java.util.HashMap[String, Array[_]]]))
    val v = new java.util.HashMap[String, Array[_]]
    try {
      println(getSchema(v.getClass))
    } catch {
      case ex: Throwable => println("Error: " + ex.getMessage)
    }
    
    println(getSchema(classOf[(String, Long)]))
    
    println(getSchema(classOf[Kind]))
    
  }
  
  def testTuple(contentType: Int) {

    val schemaJson = """
    {"type": "record", "name": "tuple", "fields": [
      {"name":"_1", "type":"string"},
      {"name":"_2", "type":"long"},
      {"name":"_3", "type":"Int"},    
    ]}
    """
    
    val schema = getSchema(classOf[(String, Long, Int)]) //Schema.parse(schemaJson)
    println(schema)
    
    println("\n==== test record to tuple ====")
    // IndexedRecord will be converted to Tuple 
    val record = new GenericData.Record(schema)
    record.put(0, "a")
    record.put(1, 1L)
    record.put(2, 1)
    
    val recordBytes= Avro.encode(record, schema, contentType)
    println(new String(recordBytes, "UTF-8"))
    val decodedRecord = Avro.decode(recordBytes, schema, classOf[(String, Long, Int)], contentType).get
    println(decodedRecord)    

    println("\n==== test tuple to tuple ====")
    val tuple = ("a", 1L, 1)
    val tupleBytes= Avro.encode(tuple, schema, contentType)
    println(new String(tupleBytes, "UTF-8"))
    val decodedTuple = Avro.decode(tupleBytes, schema, classOf[(String, Long, Int)], contentType).get
    println(decodedTuple)
  }
  
  def testJavaVMap(contentType: Int) {
    println("\n==== test java vmap ====")

    val schemaJson = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double"]}}
    """
    
    val vmap = new java.util.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))

    testMap(schemaJson, vmap, contentType)
  }
  
  def testScalaVMap(contentType: Int) {
    println("\n==== test scala vmap ====")

    val schemaJson = """
    {"type": "map", "values": {"type": "array", "items": ["long", "double", "string"]}}
    """
    
    val vmap = new mutable.HashMap[String, Array[_]]
    vmap.put(".", Array(1L, 2L, 3L))
    vmap.put("a", Array(1.0, 2.0, 3.0))
    vmap.put("b", Array("a", "b", "c"))

    testMap(schemaJson, vmap, contentType)
  }
  
  def testMap[T](schemaDesc: String, vmap: T, contentType: Int) {
    println("\n========= Map ============= ")
    //val schema = ReflectData.get.getSchema(vmap.getClass)
    val schema = Schema.parse(schemaDesc)
    println(schema.toString)
    
    // encode a map
    val bytes = Avro.encode(vmap, schema, contentType)
    val json = new String(bytes, "UTF-8")
    println(json)
    
    // decode to scala map
    val map = Avro.decode(bytes, schema, classOf[collection.Map[String, Array[_]]], contentType).get
    map foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  def testReflectClass(contentType: Int) {
    println("\n==== test reflect class ====")

    val instance = new Ticker
    instance.flag = 0
    println("\n==== before ===")
    println(instance)

    val schema = getSchema(instance.getClass)
    println(schema.toString)
    
    // encode
    val bytes = Avro.encode(instance, schema, contentType)
    // decode
    val decoded = Avro.decode(bytes, schema, classOf[Ticker], contentType).get
    println(decoded)
  }
  
  def testReflectArrayOfClass(contentType: Int) {
    println("\n==== test array of reflect class ====")

    val instance = new Ticker
    instance.flag = 0
    println("\n==== before ===")
    println(instance)
    
    val instances = Array(instance)

    val schema = getSchema(instances.getClass)
    println(schema.toString)
    
    // encode
    val bytes = Avro.encode(instances, schema, contentType)
    println(new String(bytes, "UTF-8"))
    // decode
    val decoded = Avro.decode(bytes, schema, classOf[Array[Ticker]], contentType).get
    println(decoded)
  }
  
  def testCaseObject(contentType: Int) {
    println("\n==== test case object ====")
    
    val schema = getSchema(classOf[Corner])
    println(schema.toString)
    
    // encode
    val bytes = Avro.encode(Corner.Lower, schema, contentType)
    println(new String(bytes, "UTF-8"))
    // decode
    val decoded = Avro.decode(bytes, schema, classOf[Kind], contentType).get
    println(decoded)
    
    decoded match {
      case Corner.Lower => println("Sucess")
      case _ => println("Failure")
    }
  }
  
  
  private def getSchema[T: ClassTag: TypeTag](c: Class[T]) = {
    ReflectData.AllowNull.getSchema(c)
  }
  
  // test data --- ticker
  class Ticker {
    private val data = Array(1.0, 2.0)
    
    @transient var flag: Byte = 10
    
    val open = 8.0
    private val high = 10.0f
    
    var close = 10.1
    private var volumn = 100
    
    @transient private var corner: Corner = Corner.Lower // todo, it's difficult to avro scala singleton object
    
    override def toString = 
      "Ticker(data=" + data.mkString("[", ",", "]") + ", flag=" + flag + ", open=" + open + ", close=" + close + ", high=" + high + ", corner=" + corner + ")"
  }
  
  
  // test data --- case object
  object Kind {
    def withId(id: Byte): Kind = {
      if (isSign(id)) Side.withId(id) else Corner.withId(id)
    }

    def isSign(id: Byte): Boolean = id > 0
  }

  abstract class Kind {def id: Byte}

  class Side(val id: Byte) extends Kind {def this() = this(0) /* make it serializable */}
  object Side {
    case object EnterLong  extends Side(1)
    case object ExitLong   extends Side(2)
    case object EnterShort extends Side(3)
    case object ExitShort  extends Side(4)

    def withId(id: Byte): Side = id match {
      case 1 => Side.EnterLong
      case 2 => Side.ExitLong
      case 3 => Side.EnterShort
      case 4 => Side.ExitShort
    }
  }

  class Corner(val id: Byte) extends Kind {def this() = this(0) /* make it serializable */}
  object Corner {
    case object Upper extends Corner(-1)
    case object Lower extends Corner(-2)

    def withId(id: Byte): Corner = id match {
      case -1 => Corner.Upper
      case -2 => Corner.Lower
    }
  }
}