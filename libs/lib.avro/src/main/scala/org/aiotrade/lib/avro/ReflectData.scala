package org.aiotrade.lib.avro

import java.lang.reflect.Field
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.lang.reflect.ParameterizedType
import java.lang.reflect.GenericArrayType

import java.lang.reflect.TypeVariable
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.AvroRemoteException
import org.apache.avro.AvroRuntimeException
import org.apache.avro.AvroTypeException
import org.apache.avro.Protocol
import org.apache.avro.Schema
import org.apache.avro.generic.IndexedRecord
import org.apache.avro.generic.GenericFixed
import org.apache.avro.specific.FixedSize
import org.apache.avro.reflect.Nullable
import org.apache.avro.reflect.Stringable
import org.apache.avro.reflect.Union
import org.apache.avro.io.BinaryData

import com.thoughtworks.paranamer.CachingParanamer
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

object ReflectData {
  /** {@link ReflectData} implementation that permits null field values.  The
   * schema generated for each field is a union of its declared type and
   * null. */
  object AllowNull extends ReflectData {
    override protected def createFieldSchema(field: Field, names: java.util.Map[String, Schema]): Schema = {
      val schema = super.createFieldSchema(field, names)
      makeNullable(schema)
    }
  }

  private val INSTANCE = new ReflectData()

  /** Return the singleton instance. */
  def get() = INSTANCE

  private val classLoader = Thread.currentThread.getContextClassLoader
  private val FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap[Class[_], java.util.Map[String, Field]](8, 0.9f, 1)

  /** Return the named field of the provided class.  Implementation caches
   * values, since this is used at runtime to get and set fields. */
  private def getField(c: Class[_], name: String): Field = {
    var fields = FIELD_CACHE.get(c)
    if (fields == null) {
      fields = new java.util.concurrent.ConcurrentHashMap[String, Field](8, 0.9f, 1)
      FIELD_CACHE.put(c, fields)
    }
    val realName = name.replace("_DOLLAR_", "$")
    var field = fields.get(realName)
    if (field == null) {
      field = findField(c, realName)
      fields.put(realName, field)
    }
    field
  }

  private def findField(c: Class[_], name: String): Field = {
    var clz = c
    do {
      try {
        val f = clz.getDeclaredField(name)
        f.setAccessible(true)
        return f
      } catch {
        case e: NoSuchFieldException =>
      }
      clz = clz.getSuperclass
    } while (clz != null)
    throw new AvroRuntimeException("No field named "+name+" in: "+c)
  }

  protected[avro] val CLASS_PROP = "java-class"
  protected[avro] val ELEMENT_PROP = "java-element-class"

  protected[avro] def getClassProp(schema: Schema, prop: String): Class[_] = {
    val name = schema.getProp(prop)
    if (name == null) return null
    try {
      return Class.forName(name, true, classLoader)
    } catch {
      case ex: ClassNotFoundException =>  throw new AvroRuntimeException(ex)
    }
  }

  private val BYTES_CLASS = Array[Byte]().getClass

  private val THROWABLE_MESSAGE = makeNullable(Schema.create(Schema.Type.STRING))

  /** Create and return a union of the null schema and the provided schema. */
  def makeNullable(schema: Schema) = {
    Schema.createUnion(java.util.Arrays.asList(Schema.create(Schema.Type.NULL), schema))
  }
}

/** Utilities to use existing Java classes and interfaces via reflection. */
class ReflectData protected() extends SpecificData {

  override
  def getClassName(schema: Schema): String = {
    super.getClassName(schema).replace("_DOLLAR_", "$")
  }

  override
  def setField(record: AnyRef, name: String, position: Int, v: AnyRef) {
    record match {
      case x: IndexedRecord => super.setField(record, name, position, v)
      case _ =>
        try {
          import ClassHelper._
          val field = ReflectData.getField(record.getClass, name)
          val value = if (v.isInstanceOf[java.lang.Integer] || v.isInstanceOf[Int]) {
            field.getGenericType match {
              case JByteClass  | ByteType  | ByteClass  => v.asInstanceOf[java.lang.Integer].byteValue
              case JShortClass | ShortType | ShortClass => v.asInstanceOf[java.lang.Integer].shortValue
              case _ => v
            }
          } else v

          field.set(record, value)
        } catch {
          case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
        }
    }
  }

  override
  def getField(record: AnyRef, name: String, position: Int): AnyRef = {
    record match {
      case x: IndexedRecord => super.getField(record, name, position)
      case _ =>
        try {
          ReflectData.getField(record.getClass, name).get(record)
        } catch {
          case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
        }
    }
  }

  override
  protected def isRecord(datum: AnyRef): Boolean = {
    if (datum == null) return false
    getSchema(datum.getClass).getType == Schema.Type.RECORD
  }

  override
  protected def isArray(datum: AnyRef): Boolean = {
    if (datum == null) return false
    datum.isInstanceOf[java.util.Collection[_]] || datum.isInstanceOf[collection.Seq[_]] || datum.getClass.isArray()
  }

  override
  protected def isMap(datum: AnyRef): Boolean = {
    datum.isInstanceOf[java.util.Map[_, _]] || datum.isInstanceOf[collection.Map[_, _]]
  }

  override
  protected def isBytes(datum: AnyRef): Boolean = {
    if (datum == null) return false
    if (super.isBytes(datum)) return true
    val c = datum.getClass
    c.isArray && c.getComponentType == java.lang.Byte.TYPE
  }

  override
  protected def getRecordSchema(record: AnyRef): Schema = {
    getSchema(record.getClass)
  }

  override
  def validate(schema: Schema, datum: AnyRef): Boolean = {
    import Schema.Type._
    schema.getType match {
      case RECORD =>
        if (datum == null) return false
        val c = datum.getClass
        val fields = schema.getFields.iterator
        while (fields.hasNext) {
          val f = fields.next
          try {
            if (!validate(f.schema, ReflectData.getField(c, f.name).get(datum))) return false
          } catch {
            case ex: IllegalAccessException => throw new AvroRuntimeException(ex)
          }
        }
        true

      case ARRAY =>
        if (datum.getClass.isArray) {    // array
          val length = java.lang.reflect.Array.getLength(datum)
          var i = -1
          while ({i += 1; i < length}) {
            if (!validate(schema.getElementType, java.lang.reflect.Array.get(datum, i))) return false
          }
          return true
        }

        datum match {
          case xs: java.util.Collection[_] =>          // collection
            val itr = xs.iterator
            while (itr.hasNext) {
              val element = itr.next.asInstanceOf[AnyRef]
              if (!validate(schema.getElementType, element)) return false
            }
            true
          case xs: collection.Seq[_] =>          // collection
            val itr = xs.iterator
            while (itr.hasNext) {
              val element = itr.next.asInstanceOf[AnyRef]
              if (!validate(schema.getElementType, element)) return false
            }
            true
          case _ => false
        }

      case MAP =>
        datum match {
          case map: java.util.Map[_, _] =>
            val itr = map.entrySet.iterator
            while (itr.hasNext) {
              val entry = itr.next
              if (!validate(schema.getValueType, entry.getValue.asInstanceOf[AnyRef])) return false
            }
            true
          case map: collection.Map[_, _] =>
            val itr = map.iterator
            while (itr.hasNext) {
              val entry = itr.next
              if (!validate(schema.getValueType, entry._2.asInstanceOf[AnyRef])) return false
            }
            true
          case _ => true
        }
      case _ => super.validate(schema, datum)
    }
  }

  override
  def getClass(schema: Schema): Class[_] = {
    import Schema.Type._
    schema.getType match {
      case ARRAY => ReflectData.getClassProp(schema, ReflectData.CLASS_PROP) match {
          case null => java.lang.reflect.Array.newInstance(getClass(schema.getElementType), 0).getClass
          case collectionClass => collectionClass
        }
      case MAP => classOf[collection.Map[_, _]]
      case STRING => classOf[String]
      case BYTES => ReflectData.BYTES_CLASS
      case INT =>
        val classProp = schema.getProp(ReflectData.CLASS_PROP)
        if (classOf[Short].getName == classProp || classOf[java.lang.Short].getName == classProp)
          java.lang.Short.TYPE
        else
          super.getClass(schema)
      case _ => super.getClass(schema)
    }
  }

  override
  protected def createSchema(tpe: GenericArrayType, names: java.util.Map[String, Schema]): Schema = {
    import ClassHelper._
    tpe.getGenericComponentType match {
      case ByteType => Schema.create(Schema.Type.BYTES) // byte array
      case c: Class[_] =>
        val c1 = c.asInstanceOf[Class[_]] // cheat createSchema for lack of ClassTag
        val schema = Schema.createArray(createSchema(c1, names))
        setElement(schema, c)
        schema
      case ptype: ParameterizedType =>
        val schema = Schema.createArray(createSchema(ptype, names))
        setElement(schema, ptype)
        schema
    }
  }

  override
  protected def createSchema(tpe: ParameterizedType, names: java.util.Map[String, Schema]): Schema = {
    val raw = tpe.getRawType.asInstanceOf[Class[_]]
    val params = tpe.getActualTypeArguments
    if (classOf[java.util.Map[_, _]].isAssignableFrom(raw) || classOf[collection.Map[_, _]].isAssignableFrom(raw)) { // Map
      val key = params(0)
      val value = params(1).asInstanceOf[Class[_]]
      if (key != classOf[String]) throw new AvroTypeException("Map key class not String: "+key)
      Schema.createMap(createSchema(value, names))
    } else if (classOf[java.util.Collection[_]].isAssignableFrom(raw) || classOf[collection.Seq[_]].isAssignableFrom(raw)) { // Collection
      if (params.length != 1) throw new AvroTypeException("No array type specified.")
      val element = params(0).asInstanceOf[Class[_]]
      val schema = Schema.createArray(createSchema(element, names))
      schema.addProp(ReflectData.CLASS_PROP, raw.getName)
      schema
    } else {
      super.createSchema(tpe, names)
    }
  }

  override
  protected def createSchema[T <: GenericDeclaration](tpe: TypeVariable[T], names: java.util.Map[String, Schema]): Schema = {
    tpe.getGenericDeclaration match {
      case c: Class[_] => val c1 = c.asInstanceOf[Class[_]]; createSchema(c1, names)
      case gtype => throw new AvroTypeException("Unknown type: " + gtype)
    }
  }

  override
  protected def createSchema[T: ClassTag : TypeTag](tpe: Class[T], names: java.util.Map[String, Schema]): Schema = {
    import ClassHelper._
    tpe match {
      case ShortClass | JShortClass | ShortType =>
        val schema = Schema.create(Schema.Type.INT)
        schema.addProp(ReflectData.CLASS_PROP, classOf[Short].getName)
        schema

      case c: Class[_] if c.isArray =>
        c.getComponentType match {
          case ByteType =>
            Schema.create(Schema.Type.BYTES) // byte array
          case component =>
            val schema = Schema.createArray(createSchema(component, names))
            setElement(schema, component)
            schema
        }

      case c: Class[T] if classOf[CharSequence].isAssignableFrom(c) => // String
        Schema.create(Schema.Type.STRING) // String

      case c: Class[T] if c.isPrimitive || classOf[Number].isAssignableFrom(c) || c == JVoidClass || c == JBooleanClass || c == BooleanClass => // primitive
        super.createSchema(tpe, names)

      case c: Class[T] if isTupleClass(c) || isCollectionClass(c) || isMapClass(c) =>
        super.createSchema(tpe, names)

      case c: Class[T] => // other Class
        val fullName = c.getName
        names.get(fullName) match {
          case null =>
            val name = if (isScalaSingletonObject(c)) { // scala singleton object
              c.getSimpleName.replace("$", "_DOLLAR_")
            } else c.getSimpleName

            val space = if (c.getEnclosingClass != null) { // nested class
              c.getEnclosingClass.getName + "$"
            } else {
              if (c.getPackage == null) "" else c.getPackage.getName
            }

            val union = c.getAnnotation(classOf[Union])
            if (union != null) { // union annotated
              return getAnnotatedUnion(union, names)
            } else if (c.isAnnotationPresent(classOf[Stringable])) { // Stringable
              val result = Schema.create(Schema.Type.STRING)
              result.addProp(ReflectData.CLASS_PROP, c.getName)
              return result
            } else if (classOf[IndexedRecord].isAssignableFrom(c)) { // specific
              return super.createSchema(tpe, names)
            }

            val schema =
              if (c.isEnum) { // Enum
                val symbols = new java.util.ArrayList[String]()
                val constants = c.getEnumConstants.asInstanceOf[Array[Enum[_]]]
                var i = -1
                while ({i += 1; i < constants.length}) {
                  symbols.add(constants(i).name)
                }
                Schema.createEnum(name, null /* doc */, space, symbols)
              } else if (classOf[GenericFixed].isAssignableFrom(c)) { // fixed
                val size = c.getAnnotation(classOf[FixedSize]).value
                Schema.createFixed(name, null /* doc */, space, size)
              } else { // record
                val fields = new java.util.ArrayList[Schema.Field]()
                val error = classOf[Throwable].isAssignableFrom(c)
                val schema = Schema.createRecord(name, null /* doc */, space, error)
                names.put(c.getName, schema)
                val clzFields = getFields(c)
                for (field <- clzFields) {
                  if ((field.getModifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0){
                    val fieldSchema = createFieldSchema(field, names)
                    // schema name only allows leffter and digit
                    val fieleName = field.getName.replace("$", "_DOLLAR_")
                    fields.add(new Schema.Field(fieleName, fieldSchema, null /* doc */, null))
                  }
                }
                if (error) { // add Throwable message
                  fields.add(new Schema.Field("detailMessage", ReflectData.THROWABLE_MESSAGE, null, null))
                }
                schema.setFields(fields)
                schema
              }

            names.put(fullName, schema)
            schema
          case schema => schema
        }

      case _ => super.createSchema(tpe, names)
    }
  }


  // if array element type is a class with a union annotation, note it
  // this is required because we cannot set a property on the union itself
  private def setElement(schema: Schema, element: Type) {
    element match {
      case c: Class[_] =>
        val union = c.getAnnotation(classOf[Union])
        if (union != null) // element is annotated union
          schema.addProp(ReflectData.ELEMENT_PROP, c.getName)
      case _ =>
    }
  }

  // construct a schema from a union annotation
  private def getAnnotatedUnion(union: Union, names: java.util.Map[String, Schema]): Schema = {
    val branches = new java.util.ArrayList[Schema]()
    for (branch <- union.value)
      branches.add(createSchema(branch, names))
    Schema.createUnion(branches)
  }

  // Return of this class and its superclasses to serialize.
  // Not cached, since this is only used to create schemas, which are cached.
  private def getFields(recordClass: Class[_]) = {
    val nameToFields = collection.mutable.LinkedHashMap[String, Field]()
    var c = recordClass
    var break = false
    do {
      if (c.getPackage != null && c.getPackage.getName.startsWith("java.")) {
        break = true // skip java built-in classes
      } else {
        val fields = c.getDeclaredFields
        var  i = -1
        while ({i += 1; i < fields.length}) {
          val field = fields(i)
          if ((field.getModifiers & (Modifier.TRANSIENT | Modifier.STATIC)) == 0) {
            val name = field.toString // the sub-class and the super-class may have fields with same name, do not use the method  "getName".
            if (nameToFields.contains(name)) {
              throw new AvroTypeException(c+" contains two fields named: "+field)
            } else {
              nameToFields.put(name, field)
            }
          }
        }
        c = c.getSuperclass
      }
    } while (c != null && !break)
    nameToFields.values
  }

  /** Create a schema for a field. */
  protected def createFieldSchema(field: Field, names: java.util.Map[String, Schema]): Schema = {
    val schema = field.getGenericType match {
      case x: GenericArrayType => createSchema(x, names)
      case x: ParameterizedType => createSchema(x, names)
      case x: TypeVariable[_] => createSchema(x, names)
      case x: Class[_] => val c = x.asInstanceOf[Class[_]]; createSchema(c, names)
    }

    if (field.isAnnotationPresent(classOf[Nullable])) // nullable
      ReflectData.makeNullable(schema)
    else
      schema
  }

  /** Return the protocol for a Java interface.
   * <p>Note that this requires that <a
   * href="http://paranamer.codehaus.org/">Paranamer</a> is run over compiled
   * interface declarations, since Java 6 reflection does not provide access to
   * method parameter names.  See Avro's build.xml for an example. */
  override
  def getProtocol(iface: Class[_]): Protocol = {
    val protocol = new Protocol(iface.getSimpleName, if (iface.getPackage == null) "" else iface.getPackage.getName)
    val names = new java.util.LinkedHashMap[String, Schema]()
    val messages = protocol.getMessages
    val methods = iface.getMethods
    var i = -1
    while ({i += 1; i < methods.length}) {
      val method = methods(i)
      if ((method.getModifiers & Modifier.STATIC) == 0) {
        val name = method.getName()
        if (messages.containsKey(name)) throw new AvroTypeException("Two methods with same name: "+name)
        messages.put(name, getMessage(method, protocol, names))
      }
    }

    // reverse types, since they were defined in reference order
    val types = new java.util.ArrayList[Schema]()
    types.addAll(names.values)
    java.util.Collections.reverse(types)
    protocol.setTypes(types)

    protocol
  }

  private val paranamer = new CachingParanamer

  private def getMessage(method: Method, protocol: Protocol, names: java.util.Map[String, Schema]): Protocol#Message = {
    val fields = new java.util.ArrayList[Schema.Field]()
    val paramNames = paranamer.lookupParameterNames(method)
    val paramTypes = method.getGenericParameterTypes
    val annotations = method.getParameterAnnotations
    var i = -1
    while ({i += 1; i < paramTypes.length}) {
      var paramSchema = getSchema(paramTypes(i), names)
      var j = -1
      while ({j += 1; j < annotations(i).length}) {
        annotations(i)(j) match {
          case x: Union => paramSchema = getAnnotatedUnion(x, names)
          case x: Nullable => paramSchema = ReflectData.makeNullable(paramSchema)
          case _ =>
        }
      }
      val paramName = if (paramNames.length == paramTypes.length) paramNames(i) else (paramSchema.getName + i)
      fields.add(new Schema.Field(paramName, paramSchema, null /* doc */, null))
    }

    val request = Schema.createRecord(fields)

    val union = method.getAnnotation(classOf[Union])
    var response = if (union == null) getSchema(method.getGenericReturnType, names) else getAnnotatedUnion(union, names)
    if (method.isAnnotationPresent(classOf[Nullable])) {         // nullable
      response = ReflectData.makeNullable(response)
    }

    val errs = new java.util.ArrayList[Schema]()
    errs.add(Protocol.SYSTEM_ERROR) // every method can throw
    val excps = method.getGenericExceptionTypes
    var k = -1
    while ({k += 1; k < excps.length}) {
      val err = excps(k)
      if (err != classOf[AvroRemoteException]) {
        errs.add(getSchema(err, names))
      }
    }
    val errors = Schema.createUnion(errs)
    protocol.createMessage(method.getName, null /* doc */, request, response, errors)
  }

  private def getSchema(tpe: Type, names: java.util.Map[String, Schema]): Schema = {
    try {
      tpe match {
        case x: GenericArrayType => createSchema(x, names)
        case x: ParameterizedType => createSchema(x, names)
        case x: TypeVariable[_] => createSchema(x, names)
        case x: Class[_] => val c = x.asInstanceOf[Class[_]]; createSchema(c, names)
      }
    } catch {
      case ex: AvroTypeException => throw new AvroTypeException("Error getting schema for "+tpe+": " +ex.getMessage, ex)
    }
  }

  override
  protected def compare(o1: Object, o2: Object, s: Schema, equals: Boolean): Int = {
    import Schema.Type._
    s.getType match {
      case ARRAY if o1.getClass.isArray =>
        val elementType = s.getElementType()
        val l1 = java.lang.reflect.Array.getLength(o1)
        val l2 = java.lang.reflect.Array.getLength(o2)
        val l = math.min(l1, l2)
        var i = -1
        while ({i += 1; i < l}) {
          val comp = compare(java.lang.reflect.Array.get(o1, i),
                             java.lang.reflect.Array.get(o2, i),
                             elementType, equals)
          if (comp != 0) return comp
        }
        l1 - l2
      case BYTES if o1.getClass.isArray =>
        val b1 = o1.asInstanceOf[Array[Byte]]
        val b2 = o2.asInstanceOf[Array[Byte]]
        BinaryData.compareBytes(b1, 0, b1.length, b2, 0, b2.length)
      case _ => super.compare(o1, o2, s, equals)
    }
  }

}