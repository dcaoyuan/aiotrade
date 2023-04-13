package org.aiotrade.lib.avro

import java.lang.reflect.GenericArrayType
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.Schema
import org.apache.avro.Protocol
import org.apache.avro.AvroRuntimeException
import org.apache.avro.AvroTypeException
import org.apache.avro.Schema.Type
import org.apache.avro.generic.GenericData
import scala.collection.mutable
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object SpecificData {
  private val INSTANCE = new SpecificData

  private val NO_CLASS = (new Object {}).getClass
  private val NULL_SCHEMA = Schema.create(Schema.Type.NULL)

  /** Return the singleton instance. */
  def get(): SpecificData = INSTANCE
}

/** Utilities for generated Java classes and interfaces. */
class SpecificData protected () extends GenericData {
  import ClassHelper._
  import SpecificData._
  import Schema.Type._

  private val log = Logger.getLogger(this.getClass().getName)
  private val classLoader = Thread.currentThread.getContextClassLoader
  
  private val classCache = new java.util.concurrent.ConcurrentHashMap[String, Class[_]](8, 0.9f, 1)
  private val schemaCache = new java.util.WeakHashMap[java.lang.reflect.Type, Schema]()

  override protected def isEnum(datum: Object): Boolean = {
    datum.isInstanceOf[Enum[_]] || super.isEnum(datum)
  }

  /** Called by {@link #resolveUnion(Schema,Object)}. @see #createSchema */
  override protected def instanceOf(schema: Schema, datum: AnyRef): Boolean = {
    schema.getType match {
      case INT => datum.isInstanceOf[java.lang.Integer] || datum.isInstanceOf[java.lang.Short] || datum.isInstanceOf[java.lang.Byte]
      case RECORD =>
        if (!isRecord(datum)) return false
        if (schema.getFullName == null) {
          getRecordSchema(datum).getFullName == null
        } else {
          val datumSchema = getRecordSchema(datum)
          if (schema.getFullName == datumSchema.getFullName) {
            true 
          } else {
            getClass(schema).isInstance(datum)
          }
        }
      case _ => super.instanceOf(schema, datum)
    }
  }
  
  /** Return the class that implements a schema, or null if none exists. */
  def getClass(schema: Schema): Class[_] = {
    schema.getType match {
      case FIXED | RECORD | ENUM =>
        val name = schema.getFullName
        val clz = classCache.get(name) match {
          case null =>
            val c = try {
              Class.forName(getClassName(schema), true, classLoader)
            } catch {
              case ex: ClassNotFoundException => log.warning(ex.getMessage); NO_CLASS
            }
            classCache.put(name, c)
            c
          case c => c
        }
        
        if (clz == NO_CLASS) null else clz
      case ARRAY => classOf[ArrayList[_]]
      case MAP => classOf[mutable.HashMap[_, _]]
      case UNION =>
        val types = schema.getTypes     // elide unions with null
        if ((types.size == 2) && types.contains(NULL_SCHEMA))
          getClass(types.get(if (types.get(0).equals(NULL_SCHEMA)) 1 else 0))
        else
          classOf[Object]
      case STRING =>  classOf[java.lang.CharSequence]
      case BYTES =>   classOf[java.nio.ByteBuffer]
      case INT =>     java.lang.Integer.TYPE
      case LONG =>    java.lang.Long.TYPE
      case FLOAT =>   java.lang.Float.TYPE
      case DOUBLE =>  java.lang.Double.TYPE
      case BOOLEAN => java.lang.Boolean.TYPE
      case NULL =>    java.lang.Void.TYPE
      case _ => throw new AvroRuntimeException("Unknown type: " + schema)
    }
  }

  /** Returns the Java class name indicated by a schema's name and namespace. */
  def getClassName(schema: Schema): String = {
    val namespace = schema.getNamespace
    val name = schema.getName
    if (namespace == null) {
      name
    } else {
      val dot = if (namespace.endsWith("$")) "" else "."
      namespace + dot + name
    }
  }

  /** Find the schema for a Java type. */
  def getSchema[T: ClassTag : TypeTag](tpe: Class[T]): Schema = {
    val tpeParams = Avro.tpeParams[T]
    if (tpeParams.isEmpty) { // don't cache Type that has type parameters
      schemaCache.get(tpe) match {
        case null =>
          val schema = createSchema(tpe, new java.util.LinkedHashMap[String, Schema]())
          schemaCache.put(tpe, schema)
          schema
        case x => x
      }
    } else {
      createSchema(tpe, new java.util.LinkedHashMap[String, Schema]())
    }
  }
  
  /** Create the schema for a Java type. */
  protected def createSchema(tpe: ParameterizedType, names: java.util.Map[String, Schema]): Schema = {
    val raw = tpe.getRawType.asInstanceOf[Class[_]]
    val params = tpe.getActualTypeArguments
    if (isCollectionClass(raw)) { 
      // array
      if (params.length != 1) throw new AvroTypeException("No array type specified.")
      val etype = params(0).asInstanceOf[Class[_]]
      Schema.createArray(createSchema(etype, names))
    } else if (isMapClass(raw)) {   
      // map
      val key = params(0).asInstanceOf[Class[_]]
      val value = params(1).asInstanceOf[Class[_]]
      if (CharSequenceClass.isAssignableFrom(key)) {
        Schema.createMap(createSchema(value, names))
      } else {
        throw new AvroTypeException("Map key class not CharSequence: " + key)
      }
    } else {
      createSchema(raw, names)
    }
  }

  protected def createSchema(tpe: GenericArrayType, names: java.util.Map[String, Schema]): Schema = {
    throw new AvroTypeException("Unknown type: " + tpe)
  }
  
  protected def createSchema[T <: GenericDeclaration](tpe: TypeVariable[T], names: java.util.Map[String, Schema]): Schema = {
    throw new AvroTypeException("Unknown type: " + tpe)
  }
  
  /** Create the schema for a Java class. */
  protected def createSchema[T: ClassTag : TypeTag](tpe: Class[T], names: java.util.Map[String, Schema]): Schema = {
    tpe match {
      case VoidType    | JVoidClass                   => Schema.create(Type.NULL)
      case ByteType    | ByteClass    | JByteClass    => Schema.create(Type.INT)
      case ShortType   | ShortClass   | JShortClass   => Schema.create(Type.INT)
      case IntegerType | IntClass     | JIntegerClass => Schema.create(Type.INT)
      case LongType    | LongClass    | JLongClass    => Schema.create(Type.LONG)
      case FloatType   | FloatClass   | JFloatClass   => Schema.create(Type.FLOAT)
      case DoubleType  | DoubleClass  | JDoubleClass  => Schema.create(Type.DOUBLE)
      case BooleanType | BooleanClass | JBooleanClass => Schema.create(Type.BOOLEAN)
      case ByteBufferClass => Schema.create(Type.BYTES)
      case c: Class[T] if CharSequenceClass.isAssignableFrom(c) => Schema.create(Type.STRING)
      case c: Class[T] if isTupleClass(c) => // tuple
        val schema = Schema.createRecord("tuple", null, "", false)
        val fields = new java.util.ArrayList[Schema.Field]()
        var i = 1 // tuple indexed from 1
        for (param <- Avro.tpeParams[T]) {
          val fieldSchema = createSchema(param, names)
          // make nullable
          Schema.createUnion(java.util.Arrays.asList(Schema.create(Schema.Type.NULL), fieldSchema))
          fields.add(new Schema.Field("_" + i, fieldSchema, null /* doc */, null))
          i += 1
        }
        schema.setFields(fields)
        schema
      case c: Class[T] if isCollectionClass(c) => // array
        Avro.tpeParams[T].headOption match {
          case Some(etype) => 
            Schema.createArray(createSchema(etype, names))
          case None => throw new AvroTypeException("No array type specified.")
        }
      case c: Class[T] if isMapClass(c) => // map
        Avro.tpeParams[T] match {
          case List(key, value) => 
            if (CharSequenceClass.isAssignableFrom(key)) {
              Schema.createMap(createSchema(value, names))
            } else {
              throw new AvroTypeException("Map key class not CharSequence: " + key)
            }
          case _ => throw new AvroTypeException("No map type paramters specified.")
        }
      case c: Class[T] =>
        val fullName = c.getName
        val schema = names.get(fullName) match {
          case null =>
            try {
              c.getDeclaredField("SCHEMA$").get(null).asInstanceOf[Schema]
            } catch {
              case e: NoSuchFieldException => throw new AvroRuntimeException(e)
              case e: IllegalAccessException => throw new AvroRuntimeException(e)
            }
          case schema => schema
        }
        names.put(fullName, schema)
        schema
      case _ => throw new AvroTypeException("Unknown type: " + tpe)
    }
  }



  /** Return the protocol for a Java interface. */
  def getProtocol(iface: Class[_]): Protocol = {
    try {
      iface.getDeclaredField("PROTOCOL").get(null).asInstanceOf[Protocol]
    } catch {
      case e: NoSuchFieldException=> throw new AvroRuntimeException(e);
      case e: IllegalAccessException => throw new AvroRuntimeException(e);
    }
  }

  override def compare(o1: Object, o2: Object, s: Schema): Int = {
    s.getType match {
      case ENUM if !(o1.isInstanceOf[String]) =>               // not generic
        (o1.asInstanceOf[Enum[_]]).ordinal - (o2.asInstanceOf[Enum[_]]).ordinal
      case _ => super.compare(o1, o2, s)
    }
  }
}