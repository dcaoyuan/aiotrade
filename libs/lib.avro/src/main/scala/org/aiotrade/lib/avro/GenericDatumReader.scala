package org.aiotrade.lib.avro

import java.io.IOException

import java.nio.ByteBuffer
import org.aiotrade.lib.collection.ArrayList
import org.apache.avro.AvroRuntimeException
import org.apache.avro.Schema
import org.apache.avro.generic.GenericArray
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericFixed
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.Decoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.io.ResolvingDecoder
import org.apache.avro.util.Utf8
import org.apache.avro.util.WeakIdentityHashMap

import scala.collection.mutable
import scala.reflect.ClassTag

object GenericDatumReader {
  private val RESOLVER_CACHE = new ThreadLocal[java.util.Map[Schema, java.util.Map[Schema, ResolvingDecoder]]]() {
    override protected def initialValue = {
      new WeakIdentityHashMap[Schema, java.util.Map[Schema, ResolvingDecoder]]()
    }
  }

  /** Skip an instance of a schema. */
  @throws(classOf[IOException])
  def skip(schema: Schema, in: Decoder) {
    import Schema.Type._
    schema.getType match {
      case RECORD =>
        val fields = schema.getFields.iterator
        while (fields.hasNext) {
          skip(fields.next.schema, in)
        }
      case ENUM =>
        in.readInt()
      case ARRAY => 
        val elementType = schema.getElementType
        var l = -1L
        while ({l = in.skipArray; l > 0}) {
          var i = -1
          while ({i += 1; i < l}) {
            skip(elementType, in)
          }
        }
      case MAP =>
        val valueType = schema.getValueType
        var l = -1L
        while ({l = in.skipMap; l > 0}) {
          var i = -1
          while ({i += 1; i < l}) {
            in.skipString()
            skip(valueType, in)
          }
        }
      case UNION =>
        skip(schema.getTypes.get(in.readIndex()), in)
      case FIXED =>
        in.skipFixed(schema.getFixedSize)
      case STRING =>
        in.skipString()
      case BYTES =>
        in.skipBytes()
      case INT =>     in.readInt()           
      case LONG =>    in.readLong()          
      case FLOAT =>   in.readFloat()         
      case DOUBLE =>  in.readDouble()
      case BOOLEAN => in.readBoolean()
      case NULL =>
      case _ => throw new RuntimeException("Unknown type: " + schema)
    }
  }
  
  def apply[T](actual: Schema, expected: Schema, data: GenericData): GenericDatumReader[T] = new GenericDatumReader[T](actual, expected, data)
  def apply[T](actual: Schema, expected: Schema): GenericDatumReader[T] = new GenericDatumReader[T](actual, expected, GenericData.get)
  /** Construct where the writer's and reader's schemas are the same. */
  def apply[T](schema: Schema): GenericDatumReader[T] = new GenericDatumReader[T](schema, schema, GenericData.get)
  def apply[T](): GenericDatumReader[T] = new GenericDatumReader[T](null, null, GenericData.get)
}
/** {@link DatumReader} for generic Java objects. */
class GenericDatumReader[T] protected (private var actual: Schema, private var expected: Schema, data: GenericData) extends DatumReader[T] {
  
  private var creatorResolver: ResolvingDecoder = _
  private val creator = Thread.currentThread

  /** Return the {@link GenericData} implementation. */
  def getData: GenericData = data

  override def setSchema(writer: Schema) {
    this.actual = writer
    if (expected == null) {
      expected = actual
    }
    creatorResolver = null
  }

  /** Set the reader's schema. */
  @throws(classOf[IOException])
  def setExpected(reader: Schema) {
    this.expected = reader
    creatorResolver = null
  }


  /** Gets a resolving decoder for use by this GenericDatumReader.
   *  Unstable API.
   *  Currently uses a thread local cache to prevent constructing the
   *  resolvers too often, because that is very expensive.
   */
  @throws(classOf[IOException])
  final protected def getResolver(actual: Schema, expected: Schema): ResolvingDecoder = {
    val currThread = Thread.currentThread
    var resolver: ResolvingDecoder = null
    if ((currThread eq creator) && creatorResolver != null) {
      return creatorResolver
    } 

    var cache = GenericDatumReader.RESOLVER_CACHE.get.get(actual)
    if (cache == null) {
      cache = new WeakIdentityHashMap[Schema, ResolvingDecoder]()
      GenericDatumReader.RESOLVER_CACHE.get.put(actual, cache)
    }
    resolver = cache.get(expected)
    if (resolver == null) {
      resolver = DecoderFactory.get.resolvingDecoder(Schema.applyAliases(actual, expected), expected, null)
      cache.put(expected, resolver)
    }
    
    if (currThread eq creator) {
      creatorResolver = resolver
    }

    resolver
  }

  @throws(classOf[IOException])
  override def read(reuse: T, in: Decoder): T = {
    val resolver = getResolver(actual, expected)
    resolver.configure(in)
    val result = read(reuse, expected, resolver).asInstanceOf[T]
    resolver.drain()
    result
  }
  
  /** Called to read data.*/
  @throws(classOf[IOException])
  protected def read(old: Any, expected: Schema, in: ResolvingDecoder): Any = {
    import Schema.Type._
    expected.getType match {
      case UNION =>   read(old, expected.getTypes().get(in.readIndex()), in)
      case LONG =>    in.readLong()
      case FLOAT =>   in.readFloat()
      case DOUBLE =>  in.readDouble()
      case BOOLEAN => in.readBoolean()
      case NULL =>    in.readNull(); null
      case INT =>     readInt(old, expected, in)
      case STRING =>  readString(old, expected, in)
      case ARRAY =>   readArray(old, expected, in)
      case RECORD =>  readRecord(old, expected, in)
      case MAP =>     readMap(old, expected, in)
      case ENUM =>    readEnum(expected, in)
      case FIXED =>   readFixed(old, expected, in)
      case BYTES =>   readBytes(old, in)
      case _ => throw new AvroRuntimeException("Unknown type: " + expected)
    }
  }

  /** Called to read a record instance. May be overridden for alternate record
   * representations.*/
  @throws(classOf[IOException])
  protected def readRecord(old: Any, expected: Schema, in: ResolvingDecoder): Any = {
    val record = newRecord(old, expected)
    val fieldOrder = in.readFieldOrder
    var i = -1
    while ({i += 1; i < fieldOrder.length}) {
      val field = fieldOrder(i)
      val pos = field.pos
      val name = field.name
      val oldDatum = if (old != null) data.getField(record, name, pos) else null
      val value = read(oldDatum, field.schema, in)
      data.setField(record, name, pos, value)
    }

    record
  }
  
  /** Called to read an enum value. May be overridden for alternate enum
   * representations.  By default, returns a GenericEnumSymbol. */
  @throws(classOf[IOException])
  protected def readEnum(expected: Schema, in: Decoder): Any = {
    createEnum(expected.getEnumSymbols().get(in.readEnum()), expected)
  }

  /** Called to create an enum value. May be overridden for alternate enum
   * representations.  By default, returns a GenericEnumSymbol. */
  protected def createEnum(symbol: String, schema: Schema): Any = {
    new GenericData.EnumSymbol(schema, symbol)
  }

  /** Called to read an array instance.  May be overridden for alternate array
   * representations.*/
  @throws(classOf[IOException])
  protected def readArray(old: Any, expected: Schema, in: ResolvingDecoder): AnyRef = {
    doReadArray(old, expected, in).asInstanceOf[ArrayList[_]].toArray
  }
  
  final 
  protected def doReadArray(old: Any, expected: Schema, in: ResolvingDecoder): AnyRef = {
    val expectedType = expected.getElementType
    var l = in.readArrayStart()
    var base = 0L
    if (l > 0) {
      var array = newArray(old, l.toInt, expected)
      do {
        var i = -1
        while ({i += 1; i < l}) {
          array = addToArray(array, base + i, read(peekArray(array), expectedType, in))
        }
        base += l
      } while ({l = in.arrayNext; l > 0})
      array
    } else {
      newArray(old, 0, expected)
    }
  }
  
  
  /** Called by the default implementation of {@link #readArray} to retrieve a
   * value from a reused instance.  The default implementation is for {@link
   * GenericArray}.*/
  protected def peekArray(array: Any): Any = {
    array match {
      case x: GenericArray[_] => x.peek
      case _ => null
    }
  }

  /** 
   * Called by the default implementation of {@link #readArray} to add a
   * value.  The default implementation is for {@link Array}.
   * 
   * @Note for JsonEncoder, since the array length cannot be got in advance,  
   * we have to use appendable collection instead of a native array.
   * 
   * The input array may be immutable one, so we should return the new/old one
   */
  protected def addToArray[T](array: AnyRef, pos: Long, e: T): AnyRef = {
    array.asInstanceOf[ArrayList[T]] += e
  }

  /** Called to read a map instance.  May be overridden for alternate map
   * representations.*/
  @throws(classOf[IOException])
  protected def readMap(old: Any, expected: Schema, in: ResolvingDecoder): Any = {
    val eValue = expected.getValueType
    var l = in.readMapStart
    val map = newMap(old, l.toInt)
    if (l > 0) {
      do {
        var i = -1
        while ({i += 1; i < l}) {
          addToMap(map, readString(null, in), read(null, eValue, in))
        }
      } while ({l = in.mapNext; l > 0})
    }
    map
  }

  /** Called by the default implementation of {@link #readMap} to add a
   * key/value pair.  The default implementation is for {@link Map}.*/
  protected def addToMap(map: Any, key: Any, value: Any) {
    map.asInstanceOf[mutable.Map[Any, Any]].put(key, value)
  }
  
  /** Called to read a fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  @throws(classOf[IOException])
  protected def readFixed(old: Any, expected: Schema, in: Decoder): Any = {
    val fixed = createFixed(old, expected).asInstanceOf[GenericFixed]
    in.readFixed(fixed.bytes(), 0, expected.getFixedSize())
    return fixed;
  }

  /** Called to create an fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  protected def createFixed(old: Any, schema: Schema): Any = {
    old match {
      case x: GenericFixed if x.bytes.length == schema.getFixedSize => old
      case _ => new GenericData.Fixed(schema)
    }
  }

  /** Called to create an fixed value. May be overridden for alternate fixed
   * representations.  By default, returns {@link GenericFixed}. */
  protected def createFixed(old: Any, bytes: Array[Byte], schema: Schema): Any = {
    val fixed = createFixed(old, schema).asInstanceOf[GenericFixed]
    System.arraycopy(bytes, 0, fixed.bytes, 0, schema.getFixedSize)
    fixed
  }
  /**
   * Called to create new record instances. Subclasses may override to use a
   * different record implementation. The returned instance must conform to the
   * schema provided. If the old object contains fields not present in the
   * schema, they should either be removed from the old object, or it should
   * create a new instance that conforms to the schema. By default, this returns
   * a {@link GenericData.Record}.
   */
  protected def newRecord(old: Any, schema: Schema): Any = {
    old match {
      case x: IndexedRecord if x.getSchema == schema => x
      case _ => new GenericData.Record(schema)
    }
  }

  /** Called to create new array instances.  Subclasses may override to use a
   * different array implementation.  By default, this returns a {@link Array}
   */
  protected def newArray(old: Any, size: Int, schema: Schema): AnyRef = {
    newArray(old, size, schema, classOf[Any])
  }
  
  /**
   * We need 'elementClass: Class[T]' to get the ClassTag[T]
   */
  protected def newArray[T: ClassTag](old: Any, size: Int, schema: Schema, elementClass: Class[T]): AnyRef = {
    import Schema.Type._
    schema.getElementType.getType match {
      case INT =>     new ArrayList[Int](size)
      case LONG =>    new ArrayList[Long](size)
      case FLOAT =>   new ArrayList[Float](size)
      case DOUBLE =>  new ArrayList[Double](size)
      case BOOLEAN => new ArrayList[Boolean](size)
      case ENUM =>    new ArrayList[Int](size)
      case RECORD | ARRAY | MAP | UNION | FIXED | STRING | BYTES | NULL => new ArrayList[T](size, Some(elementClass))
      case _ => throw new AvroRuntimeException("Unknown type: " + expected)
    }
  }

  /** Called to create new array instances.  Subclasses may override to use a
   * different map implementation.  By default, this returns a {@link
   * HashMap}.*/
  protected def newMap(old: Any, size: Int): Any = {
    old match {
      case x: mutable.HashMap[_, _] => x.clear; old
      case _ => new mutable.HashMap[Any, Any]
    }
  }

  /** Called to read strings.  Subclasses may override to use a different
   * string representation.  By default, this calls {@link
   * #readString(AnyRef,Decoder)}.*/
  @throws(classOf[IOException])
  protected def readString(old: Any, expected: Schema, in: Decoder): Any = {
    readString(old, in)
  }
  /** Called to read strings.  Subclasses may override to use a different
   * string representation.  By default, this calls {@link
   * Decoder#readString(Utf8)}.*/
  @throws(classOf[IOException])
  protected def readString(old: Any, in: Decoder): Any = {
    in.readString(if (old.isInstanceOf[Utf8]) old.asInstanceOf[Utf8] else null)
  }

  /** Called to create a string from a default value.  Subclasses may override
   * to use a different string representation.  By default, this calls {@link
   * Utf8#Utf8(String)}.*/
  protected def createString(value: String): Any = { 
    new Utf8(value)
  }

  /** Called to read byte arrays.  Subclasses may override to use a different
   * byte array representation.  By default, this calls {@link
   * Decoder#readBytes(ByteBuffer)}.*/
  @throws(classOf[IOException])
  protected def readBytes(old: Any, in: Decoder): Any = {
    in.readBytes(if (old.isInstanceOf[ByteBuffer]) old.asInstanceOf[ByteBuffer] else null)
  }

  /** Called to read integers.  Subclasses may override to use a different
   * integer representation.  By default, this calls {@link
   * Decoder#readInt()}.*/
  @throws(classOf[IOException])
  protected def readInt(old: Any, expected: Schema, in: Decoder): Int = {
    in.readInt
  }

  /** Called to create byte arrays from default values.  Subclasses may
   * override to use a different byte array representation.  By default, this
   * calls {@link ByteBuffer#wrap(byte[])}.*/
  protected def createBytes(value: Array[Byte]): Any = { 
    ByteBuffer.wrap(value)
  }

}