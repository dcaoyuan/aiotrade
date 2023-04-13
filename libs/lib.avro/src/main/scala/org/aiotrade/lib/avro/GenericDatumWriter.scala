package org.aiotrade.lib.avro

import java.io.IOException

import org.apache.avro.AvroTypeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericFixed
import org.apache.avro.io.DatumWriter
import org.apache.avro.io.Encoder

object GenericDatumWriter {
  def apply[T](root: Schema, data: GenericData): GenericDatumWriter[T] = new GenericDatumWriter[T](root, data)
  def apply[T](root: Schema): GenericDatumWriter[T] = new GenericDatumWriter[T](root, GenericData.get)
  def apply[T](data: GenericData): GenericDatumWriter[T] = new GenericDatumWriter[T](null, data)
  def apply[T](): GenericDatumWriter[T] = new GenericDatumWriter[T](null, GenericData.get)
}

/** {@link DatumWriter} for generic Java objects. */
class GenericDatumWriter[T] protected (private var root: Schema, data: GenericData) extends DatumWriter[T] {

  /** Return the {@link GenericData} implementation. */
  def getData: GenericData = data

  def setSchema(root: Schema) { 
    this.root = root
  }

  @throws(classOf[IOException])
  def write(datum: T, out: Encoder) {
    write(root, datum, out)
  }
  
  /** Called to write data.*/
  @throws(classOf[IOException])
  protected def write(schema: Schema, datum: Any, out: Encoder) {
    try {
      import Schema.Type._
      schema.getType match {
        case RECORD =>  writeRecord(schema, datum, out)
        case ENUM =>    writeEnum(schema, datum, out)
        case ARRAY =>   writeArray(schema, datum, out)
        case MAP =>     writeMap(schema, datum, out)
        case UNION =>   writeUnion(schema, datum, out)
        case FIXED =>   writeFixed(schema, datum, out)
        case STRING =>  writeString(schema, datum, out)
        case BYTES =>   writeBytes(datum, out)
        case INT =>     out.writeInt(datum.asInstanceOf[Int])
        case LONG =>    out.writeLong(datum.asInstanceOf[Long])
        case FLOAT =>   out.writeFloat(datum.asInstanceOf[Float])
        case DOUBLE=>   out.writeDouble(datum.asInstanceOf[Double])
        case BOOLEAN => out.writeBoolean(datum.asInstanceOf[Boolean])
        case NULL    => out.writeNull()
        case _ => error(schema, datum)
      }
    } catch {
      case ex: NullPointerException => throw npe(ex, " of "+schema.getFullName)
    }
  }

  /** Helper method for adding a message to an NPE. */
  protected def npe(e: NullPointerException, s: String): NullPointerException = {
    val result = new NullPointerException(e.getMessage + s)
    result.initCause(if (e.getCause == null) e else e.getCause)
    result
  }

  /** Called to write a record.  May be overridden for alternate record
   * representations.
   */
  @throws(classOf[IOException])
  protected def writeRecord(schema: Schema, datum: Any, out: Encoder) {
    val fields = schema.getFields.iterator
    while (fields.hasNext) {
      val field = fields.next
      val value = data.getField(datum, field.name, field.pos)
      try {
        write(field.schema, value, out)
      } catch {
        case ex: NullPointerException => throw npe(ex, " in field " + field.name)
      }
    }
  }
  
  @throws(classOf[IOException])
  protected def writeUnion(schema: Schema, datum: Any, out: Encoder) {
    val index = data.resolveUnion(schema, datum)
    out.writeIndex(index)
    write(schema.getTypes.get(index), datum, out)
  }
  
  /** Called to write an enum value.  May be overridden for alternate enum
   * representations.*/
  @throws(classOf[IOException])
  protected def writeEnum(schema: Schema, datum: Any, out: Encoder) {
    out.writeEnum(schema.getEnumOrdinal(datum.toString))
  }
  
  /** Called to write a array.  May be overridden for alternate array
   * representations.*/
  @throws(classOf[IOException])
  protected def writeArray(schema: Schema, datum: Any, out: Encoder) {
    val element = schema.getElementType
    val size = getArraySize(datum)
    out.writeArrayStart()
    out.setItemCount(size)
    datum match {
      case xs: Array[_] =>
        var i = -1
        while ({i += 1; i < xs.length}) {
          out.startItem
          write(element, xs(i), out)
        }
      case xs: java.util.Collection[_] =>
        val itr = xs.iterator
        while (itr.hasNext) {
          out.startItem
          write(element, itr.next, out)
        }
      case xs: collection.Seq[_] =>
        val itr = xs.iterator
        while (itr.hasNext) {
          out.startItem
          write(element, itr.next, out)
        }
    }
    out.writeArrayEnd()
  }

  /** Called by the default implementation of {@link #writeArray} to get the
   * size of an array.  The default implementation is for {@link Collection}.*/
  protected def getArraySize(datum: Any): Long = {
    datum match {
      case xs: Array[_] => xs.length
      case xs: java.util.Collection[_] => xs.size
      case xs: collection.Seq[_] => xs.size
    }
  }

  /** Called to write a map.  May be overridden for alternate map
   * representations.*/
  @throws(classOf[IOException])
  protected def writeMap(schema: Schema, datum: Any, out: Encoder) {
    val value = schema.getValueType()
    val size = getMapSize(datum)
    out.writeMapStart()
    out.setItemCount(size)
    datum match {
      case map: java.util.Map[_, _] => 
        val itr = map.entrySet.iterator
        while (itr.hasNext) {
          val entry = itr.next
          out.startItem
          writeString(entry.getKey, out)
          write(value, entry.getValue, out)
        }
      case map: collection.Map[_, _] => map.size
        val itr = map.iterator
        while (itr.hasNext) {
          val entry = itr.next
          out.startItem
          writeString(entry._1, out)
          write(value, entry._2, out)
        }
    }
    out.writeMapEnd()
  }

  /** Called by the default implementation of {@link #writeMap} to get the size
   * of a map.  The default implementation is for {@link Map}.*/
  protected def getMapSize(datum: Any): Int = {
    datum match {
      case map: java.util.Map[_, _] => map.size
      case map: collection.Map[_, _] => map.size
    }
  }

  /** Called to write a string.  May be overridden for alternate string
   * representations.*/
  @throws(classOf[IOException])
  protected def writeString(schema: Schema, datum: Any, out: Encoder) {
    writeString(datum, out)
  }
  /** Called to write a string.  May be overridden for alternate string
   * representations.*/
  @throws(classOf[IOException])
  protected def writeString(datum: Any, out: Encoder) {
    out.writeString(datum.asInstanceOf[CharSequence])
  }

  /** Called to write a bytes.  May be overridden for alternate bytes
   * representations.*/
  @throws(classOf[IOException])
  protected def writeBytes(datum: Any, out: Encoder) {
    out.writeBytes(datum.asInstanceOf[java.nio.ByteBuffer])
  }

  /** Called to write a fixed value.  May be overridden for alternate fixed
   * representations.*/
  @throws(classOf[IOException])
  protected def writeFixed(schema: Schema, datum: Any, out: Encoder) {
    out.writeFixed((datum.asInstanceOf[GenericFixed]).bytes, 0, schema.getFixedSize)
  }
  
  private def error(schema: Schema, datum: Any) {
    throw new AvroTypeException("Not a "+schema+": "+datum)
  }

}
