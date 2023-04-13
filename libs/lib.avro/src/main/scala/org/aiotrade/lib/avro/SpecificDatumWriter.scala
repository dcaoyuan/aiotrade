package org.aiotrade.lib.avro

import java.io.IOException

import org.apache.avro.Schema
import org.apache.avro.io.Encoder

object SpecificDatumWriter {
  def apply[T](root: Schema, data: SpecificData): SpecificDatumWriter[T] = new SpecificDatumWriter[T](root, data)
  def apply[T](root: Schema): SpecificDatumWriter[T] = new SpecificDatumWriter[T](root, SpecificData.get)
  def apply[T](data: SpecificData): SpecificDatumWriter[T] = new SpecificDatumWriter[T](null, data)
  def apply[T](): SpecificDatumWriter[T] = new SpecificDatumWriter[T](null, SpecificData.get)
}

/** {@link org.apache.avro.io.DatumWriter DatumWriter} for generated Java classes. */
class SpecificDatumWriter[T] protected (root: Schema, specificData: SpecificData) extends GenericDatumWriter[T](root, specificData) {

  @throws(classOf[IOException])
  override protected def writeEnum(schema: Schema, datum: Any, out: Encoder) {
    datum match {
      case x: Enum[_] => out.writeEnum(x.ordinal)
      case _ => super.writeEnum(schema, datum, out)
    }
  }  
}
