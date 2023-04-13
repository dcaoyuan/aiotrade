package org.aiotrade.lib.avro

import java.io.IOException
import java.lang.reflect.InvocationTargetException

import org.aiotrade.lib.collection.ArrayList
import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.io.Decoder
import org.apache.avro.io.ResolvingDecoder

import scala.collection.mutable
import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object ReflectDatumReader {
  def apply[T](writer: Schema, reader: Schema, data: ReflectData): ReflectDatumReader[T] = new ReflectDatumReader[T](writer, reader, data)
  def apply[T](writer: Schema, reader: Schema): ReflectDatumReader[T] = new ReflectDatumReader[T](writer, reader, ReflectData.get)
  def apply[T](root: Schema): ReflectDatumReader[T] = new ReflectDatumReader[T](root, root, ReflectData.get)
  def apply[T: ClassTag : TypeTag](c: Class[T]): ReflectDatumReader[T] = apply[T](ReflectData.get.getSchema(c))
  def apply[T](): ReflectDatumReader[T] = new ReflectDatumReader[T](null, null, ReflectData.get)
}

/**
 * {@link org.apache.avro.io.DatumReader DatumReader} for existing classes via
 * Java reflection.
 */
class ReflectDatumReader[T] protected (writer: Schema, reader: Schema, data: ReflectData) extends SpecificDatumReader[T](writer, reader, data) {

  @throws(classOf[IOException])
  override protected def readArray(old: Any, expected: Schema, in: ResolvingDecoder): AnyRef = {
    super.doReadArray(old, expected, in) match {
      case xs: ArrayList[_] => 
        if (ReflectData.getClassProp(expected, ReflectData.CLASS_PROP) == null) {
          // expected native array @see GenericDatumReader#newArray
          xs.toArray
        } else {
          xs
        }
      case xs: immutable.Seq[_] => xs.reverse
      case xs => xs
    }
  }
  
  override protected def newArray(old: Any, size: Int, schema: Schema): AnyRef = {
    ReflectData.getClassProp(schema, ReflectData.CLASS_PROP) match {
      case null => // use native array
        val elementClass = ReflectData.getClassProp(schema, ReflectData.ELEMENT_PROP) match {
          case null => ReflectData.get.getClass(schema.getElementType)
          case x => x
        }
        super.newArray(old, size, schema, elementClass)
      case collectionClass => 
        old match {
          case xs: ArrayList[_] => xs.clear; xs
          case xs: java.util.Collection[_] => xs.clear; xs
          case xs: mutable.ListBuffer[_] => xs.clear; xs
          case _ =>
            if (collectionClass.isAssignableFrom(classOf[java.util.ArrayList[_]])) {
              new java.util.ArrayList()
            } else if (collectionClass.isAssignableFrom(classOf[mutable.ListBuffer[_]])) {
              new mutable.ListBuffer()
            } else if (collectionClass.isAssignableFrom(classOf[List[_]])) {
              Nil
            } else {
              super.newArray(old, size, schema)
            }
        }
    }
  }

  override protected def addToArray[T](array: AnyRef, pos: Long, e: T): AnyRef = {
    array match {
      case xs: ArrayList[T] => xs += e; xs
      case xs: java.util.Collection[T] => xs.add(e.asInstanceOf[T]); xs
      case xs: mutable.Seq[T]   => xs.:+(e); xs // append to end
      case xs: immutable.Seq[T] => xs.+:(e); xs // insert in front
      case xs => java.lang.reflect.Array.set(array, pos.toInt, e); xs // it's better not use it (for json)
    }
  }

  override protected def peekArray(array: Any): Any = null

  @throws(classOf[IOException])
  override protected def readString(old: Any, s: Schema, in: Decoder): Any = {
    val value = readString(null, in).asInstanceOf[String]
    val c = ReflectData.getClassProp(s, ReflectData.CLASS_PROP)
    if (c != null)                                // Stringable annotated class
      try {                                       // use String-arg ctor
        return c.getConstructor(classOf[String]).newInstance(value)
      } catch {
        case ex: NoSuchMethodException => throw new AvroRuntimeException(ex)
        case ex: InstantiationException => throw new AvroRuntimeException(ex)
        case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
        case ex: InvocationTargetException => throw new AvroRuntimeException(ex)
      }
    
    value
  }

  @throws(classOf[IOException])
  override protected def readString(old: Any, in: Decoder): Any = {
    super.readString(null, in).toString
  }

  override protected def createString(value: String): Any = value

  @throws(classOf[IOException])
  override protected def readBytes(old: Any, in: Decoder): Any = {
    val bytes = in.readBytes(null)
    val result = Array.ofDim[Byte](bytes.remaining)
    bytes.get(result)
    result
  }

  @throws(classOf[IOException])
  override protected def readInt(old: Any, expected: Schema, in: Decoder): Int = {
    val value = in.readInt()
    val classProp = expected.getProp(ReflectData.CLASS_PROP)
    if (classOf[Short].getName == classProp || classOf[java.lang.Short].getName == classProp)
      value.toShort
    else
      value
  }

}