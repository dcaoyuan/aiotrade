/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.avro

import java.io.File
import java.util.HashMap
import java.util.Random
import org.apache.avro.Schema
import org.apache.avro.file.DataFileWriter
import org.apache.avro.generic.GenericData
import org.apache.avro.util.Utf8
import scala.collection.JavaConversions._

object RandomData {
  private def generate(schema: Schema, random: Random, d: Int): Object = {
    import Schema.Type._
    schema.getType match {
      case RECORD =>
        val record = new GenericData.Record(schema)
        for (field <- schema.getFields)
          record.put(field.name, generate(field.schema, random, d + 1))
        record
      case ENUM =>
        val symbols = schema.getEnumSymbols
        new GenericData.EnumSymbol(schema, symbols.get(random.nextInt(symbols.size)))
      case ARRAY =>
        val length = (random.nextInt(5)+2)-d
        val array = new GenericData.Array[Object](if (length <= 0) 0 else length, schema)
        for (i <- 0 until length)
          array.add(generate(schema.getElementType(), random, d+1));
        array
      case MAP =>
        val length = (random.nextInt(5)+2)-d;
        val map = new HashMap[Object,Object](if (length <= 0) 0 else length)
        for (i <- 0 until length) {
          map.put(randomUtf8(random, 40),
                  generate(schema.getValueType(), random, d+1));
        }
        map
      case UNION =>
        val types = schema.getTypes
        generate(types.get(random.nextInt(types.size)), random, d)
      case FIXED =>
        val bytes = new Array[Byte](schema.getFixedSize)
        random.nextBytes(bytes)
        new GenericData.Fixed(schema, bytes)
      case STRING =>  return randomUtf8(random, 40)
      case BYTES =>   return randomBytes(random, 40)
      case INT =>     return random.nextInt.asInstanceOf[AnyRef]
      case LONG =>    return random.nextLong.asInstanceOf[AnyRef]
      case FLOAT =>   return random.nextFloat.asInstanceOf[AnyRef]
      case DOUBLE =>  return random.nextDouble.asInstanceOf[AnyRef]
      case BOOLEAN => return random.nextBoolean.asInstanceOf[AnyRef]
      case NULL =>    return null
      case _ => throw new RuntimeException("Unknown type: "+schema)
    }
  }

  private def randomUtf8(rand: Random, maxLength: Int): Utf8 =  {
    val utf8 = new Utf8().setLength(rand.nextInt(maxLength));
    for (i <- 0 until utf8.getLength) {
      utf8.getBytes()(i) = ('a'+rand.nextInt('z'-'a')).toByte
    }
    utf8
  }

  private def randomBytes(rand: Random, maxLength: Int): java.nio.ByteBuffer = {
    val bytes = java.nio.ByteBuffer.allocate(rand.nextInt(maxLength))
    bytes.limit(bytes.capacity)
    rand.nextBytes(bytes.array)
    bytes
  }
}

import RandomData._
class RandomData(root: Schema, count: Int, seed: Long) extends java.lang.Iterable[Object] {
  def this(root: Schema, count: Int) = this(root, count, System.currentTimeMillis())

  def iterator = {
    new java.util.Iterator[Object] {
      private val random = new Random(seed)
      private var n = 0
      def hasNext = { n < count }
      def next: Object = {
        n += 1
        generate(root, random, 0)
      }
      def remove { throw new UnsupportedOperationException }
    }
  }

  @throws(classOf[Exception])
  def main(args: Array[String]) {
    if (args.length != 3) {
      println("Usage: RandomData <schemafile> <outputfile> <count>");
      System.exit(-1)
    }
    val sch = Schema.parse(new File(args(0)))
    val writer = new DataFileWriter[Object](GenericDatumWriter[Object]()).create(sch, new File(args(1)))
    try {
      for (datum <- new RandomData(sch, Integer.parseInt(args(2)))) {
        writer.append(datum)
      }
    } finally {
      writer.close
    }
  }
}
