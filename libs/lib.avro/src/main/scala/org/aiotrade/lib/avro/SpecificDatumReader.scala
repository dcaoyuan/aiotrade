package org.aiotrade.lib.avro

import java.io.IOException
import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap;

import org.apache.avro.Schema;import org.apache.avro.generic.IndexedRecord
import org.apache.avro.io.ResolvingDecoder
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._


object SpecificDatumReader {
  /** 
   * Tag interface that indicates that a class has a one-argument constructor
   * that accepts a Schema.
   * @see SpecificDatumReader#newInstance
   */
  trait SchemaConstructable

  private val NO_ARG = Array[Class[_]]()
  private val SCHEMA_ARG = Array[Class[_]](classOf[Schema])
  private val CTOR_CACHE = new ConcurrentHashMap[Class[_], Constructor[_]](8, 0.9f, 1)

  /** Create an instance of a class.  If the class implements {@link
   * SchemaConstructable}, call a constructor with a {@link
   * org.apache.avro.Schema} parameter, otherwise use a no-arg constructor. */
  protected[avro] def newInstance[T](c: Class[T], s: Schema): T = {
    val useSchema = classOf[SchemaConstructable].isAssignableFrom(c)
    try {
      var constructor = CTOR_CACHE.get(c)
      if (constructor == null) {
        val args = if (useSchema) SCHEMA_ARG else NO_ARG
        constructor = c.getDeclaredConstructor(args :_*)
        constructor.setAccessible(true)
        CTOR_CACHE.put(c, constructor)
      }
      val args = if (useSchema) Array(s) else NO_ARG
      constructor.newInstance(args :_*).asInstanceOf[T]
    } catch {
      case ex: Exception => throw new RuntimeException(ex); null.asInstanceOf[T]
    }
  }
  
  /**
   * @Note Enum.valueOf(c.asInstanceOf[Class[_ <: Enum[_]]], name) doesn't work in Scala
   */
  def enumValueOf[T <: Enum[T]](c: Class[_], name: String): Enum[_] =
    Enum.valueOf(c.asInstanceOf[Class[T]], name).asInstanceOf[Enum[_]]

  def toTuple(arity: Int, r: IndexedRecord) = arity match {
    case 1  => Tuple1(r.get(0))
    case 2  => (r.get(0), r.get(1))
    case 3  => (r.get(0), r.get(1), r.get(2))
    case 4  => (r.get(0), r.get(1), r.get(2), r.get(3))
    case 5  => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4))
    case 6  => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5))
    case 7  => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6))
    case 8  => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7))
    case 9  => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8))
    case 10 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9))
    case 11 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10))
    case 12 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11))
    case 13 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12))
    case 14 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13))
    case 15 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14))
    case 16 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15))
    case 17 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16))
    case 18 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16), r.get(17))
    case 19 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16), r.get(17), r.get(18))
    case 20 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16), r.get(17), r.get(18), r.get(19))
    case 21 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16), r.get(17), r.get(18), r.get(19), r.get(20))
    case 22 => (r.get(0), r.get(1), r.get(2), r.get(3), r.get(4), r.get(5), r.get(6), r.get(7), r.get(8), r.get(9), r.get(10), r.get(11), r.get(12), r.get(13), r.get(14), r.get(15), r.get(16), r.get(17), r.get(18), r.get(19), r.get(20), r.get(21))
    case _ => throw new RuntimeException("Arity of record should be less than 23")
  }
  
  def apply[T](writer: Schema, reader: Schema, data: SpecificData): SpecificDatumReader[T] = new SpecificDatumReader[T](writer, reader, data) 
  def apply[T](writer: Schema, reader: Schema): SpecificDatumReader[T] = new SpecificDatumReader[T](writer, reader, SpecificData.get)
  /** Construct where the writer's and reader's schemas are the same. */
  def apply[T](schema: Schema): SpecificDatumReader[T] = new SpecificDatumReader[T](schema, schema, SpecificData.get)
  def apply[T: ClassTag : TypeTag](c: Class[T]): SpecificDatumReader[T] = apply[T](SpecificData.get.getSchema(c))
  def apply[T](): SpecificDatumReader[T] = new SpecificDatumReader[T](null, null, SpecificData.get)
}

/** {@link org.apache.avro.io.DatumReader DatumReader} for generated Java classes. */
class SpecificDatumReader[T] protected (writer: Schema, reader: Schema, data: SpecificData) extends GenericDatumReader[T](writer, reader, data) {
  import SpecificDatumReader._
  
  @throws(classOf[IOException])
  override protected def readRecord(old: Any, expected: Schema, in: ResolvingDecoder): Any = {
    super.readRecord(old, expected, in) match {
      case record: IndexedRecord => // a GenericData.Record, convert it to Tuple
        val arity = record.getSchema.getFields.size
        if (arity > 0 && arity < 23) {
          toTuple(arity, record)
        } else {
          record
        }
      case x => x
    }
  }
  
  override protected def newRecord(old: Any, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null) {
      super.newRecord(old, schema) // punt to generic
    } else {
      if (c.isInstance(old)) old else newInstance(c.asInstanceOf[Class[AnyRef]], schema)
    }
  }

  override protected def createEnum(symbol: String, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null) {
      super.createEnum(symbol, schema)
    } else {
      enumValueOf(c, symbol)
    }
  }

  override protected def createFixed(old: Any, schema: Schema): Any = {
    val c = SpecificData.get.getClass(schema)
    if (c == null){
      super.createFixed(old, schema)
    } else {
      if (c.isInstance(old)) old else newInstance(c.asInstanceOf[Class[AnyRef]], schema)
    }
  }

}