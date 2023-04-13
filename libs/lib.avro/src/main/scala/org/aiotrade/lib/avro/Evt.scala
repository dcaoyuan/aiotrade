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

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Calendar
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.ClassHelper
import org.apache.avro.Schema
import org.apache.avro.io.BinaryData
import scala.collection.mutable
import scala.reflect._
import scala.reflect.runtime.universe._

/**
 * A sketch of business message protocals (APIs) design
 * 
 * 'Evt' is actually the evt definition, or the API definition
 * 'T' is the type of evt Msg's value
 * 'Msg[T](tag: Int, value: T)' is the type of each evt message, each type of Evt 
 *   may carray msgs with diffrent msg value, but these msgs have same tag.
 * 
 * Instead of using case class as each evt , the design here uses combination of 
 * object Evt and case class Msg, so:
 * 1. It's easier to keep off from possible serialization issue for lots concrete classes
 * 2. The meta data such as 'doc', 'tpeClass' are stored in evt definition for
 *    each type of evt. Only the message data is stored in each evt message
 * 3. The serialization size of evt message is smaller.
 * 4. We can pattern match it via named extract, or via regural Tuple match 
 * 
 * @param [T] the type of the evt msg's value. 
 *        For list, Although we support collection.Seq[_] type of T, but it's 
 *        better to use JVM type safed Array[_], since we here have to check each 
 *        elements of value to make sure the pattern match won't be cheated. 
 *        For varargs, use type safed Tuple instead of List.
 *        @see unapply
 * @param tag an unique int id for this type of Evt
 * @param doc the document of this Evt
 * @param schemaJson the custom schema 
 * 
 * @author Caoyuan Deng
 */

final case class Msg[T](tag: Int, value: T)

/**
 * We don't encourage to use 'object anApi extends Evt[T](..)' to define an Evt, 
 * instead, should use 'val anApi = Evt[T](..)' to define new api. The reason 
 * here is that object is something lazy val, which should be explicitly referred 
 * to invoke initializing code, that is, it may not be regirtered in Evt.tagToEvt 
 * yet when you call its static 'apply', 'unapply' methods.
 */
@throws(classOf[RuntimeException])
final class Evt[T: ClassTag : TypeTag] private (val tag: Int, val doc: String = "", schemaJson: String) {
  type ValType = T
  type MsgType = Msg[T]
  
  private val log = Logger.getLogger(this.getClass.getName)
  
  checkRegister
  
  /** class of msg value */
  val tpe: Class[T] = classTag[T].runtimeClass.asInstanceOf[Class[T]]
  /** typeParams of msg value */
  private val tpeParams: List[Class[_]] = Avro.tpeParams[T]
  
  @throws(classOf[RuntimeException])
  private def checkRegister {
    Evt.tagToEvt.get(tag) match {
      case Some(evt) =>
        if (evt ne this) {
          val ex = new RuntimeException("Tag: " + tag + " already existed!")
          log.log(Level.SEVERE, ex.getMessage, ex)
          throw ex
        }
      case None =>
        Evt.tagToEvt(tag) = this
    }
  }
  
  /**
   * Avro schema of evt value: we've implemented a reflect schema.
   * you can also override 'schemaJson' to get custom json
   */
  lazy val schema: Schema = {
    if (schemaJson != null) {
      Schema.parse(schemaJson)
    } else {
      ReflectData.AllowNull.getSchema(tpe)
    }
  }
  
  /**
   * Return the evt message that is to be passed to. the evt message is wrapped in
   * a tuple in form of (tag, evtValue)
   */
  def apply(msgVal: T): Msg[T] = Msg[T](tag, msgVal)

  /** 
   * @Note Since T is erasued after compiled, should check type of evt message 
   * via ClassTag instead of v.isInstanceOf[T]
   * 1. Don't write to unapply(evtMsg: Msg[T]), which will confuse the compiler 
   *    to generate wrong code for match {case .. case ..}
   * 2. Don't write to unapply(evtMsg: Msg[_]), which will cause sth like:
   *    msg match {case => StrEvt("") => ... } doesn't work
   * 3. Don't write to: case Msg(`tag`, value: T) if ClassHelper.isInstance(tpe, value), which
   *    will cause Primitives types of value match failure
   */
  def unapply(evtMsg: Any): Option[T] = evtMsg match {
    case Msg(`tag`, value) if ClassHelper.isInstance(tpe, value) =>
      // we will do 1-level type arguments check, and won't deep check it's type parameter anymore
      value match {
        case x: collection.Seq[_] =>
          val t = tpeParams.head
          val vs = x.iterator
          while (vs.hasNext) {
            if (!ClassHelper.isInstance(t, vs.next)) 
              return None
          }
          Some(value.asInstanceOf[T])
        case x: Product if ClassHelper.isTuple(x.asInstanceOf[AnyRef]) =>
          val vs = x.productIterator
          val ts = tpeParams.iterator
          while (vs.hasNext) {
            if (!ClassHelper.isInstance(ts.next, vs.next)) 
              return None
          }
          Some(value.asInstanceOf[T])
        case _ => Some(value.asInstanceOf[T])
      }
    case _ => None
  }
  
  override def toString = {
    "Evt(tag=" + tag + ", tpe=" + tpe + ", doc=\"" + doc + "\")"
  }
}

object Evt {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private val tagToEvt = new mutable.HashMap[Int, Evt[_]]()
  
  val NO_TAG = Int.MinValue
  val Error = Evt[String](Int.MaxValue)
  
  def exists(tag: Int): Boolean = tagToEvt.get(tag).isDefined
  def evtOf(tag: Int): Option[Evt[_]] = tagToEvt.get(tag)
  def typeOf(tag: Int): Option[Class[_]] = tagToEvt.get(tag) map (_.tpe)
  def schemaOf(tag: Int): Option[Schema] = tagToEvt.get(tag) map (_.schema)
  def tagToSchema = tagToEvt map {x => (x._1 -> schemaOf(x._1))}
 
  def toAvro[T](value: T, tag: Int): Array[Byte] = schemaOf(tag) match {
    case Some(schema) => Avro.encode(value, schema, Avro.AVRO)
    case None => Array[Byte]()
  }
  
  def toJson[T](value: T, tag: Int): Array[Byte] = schemaOf(tag) match {
    case Some(schema) => Avro.encode(value, schema, Avro.JSON)
    case None => Array[Byte]()
  }
  
  def fromAvro(bytes: Array[Byte], tag: Int): Option[_] = evtOf(tag) match {
    case Some(evt) => Avro.decode(bytes, evt.schema, evt.tpe, Avro.AVRO)
    case None => None
  }

  def fromJson(bytes: Array[Byte], tag: Int): Option[_] = evtOf(tag) match {
    case Some(evt) => Avro.decode(bytes, evt.schema, evt.tpe, Avro.JSON)
    case None => None
  }
  
  @throws(classOf[IOException])
  private def writeTag(tag: Int, out: OutputStream) {
    val buf = new Array[Byte](5) // max bytes is 5
    val len = BinaryData.encodeInt(tag, buf, 0)
    out.write(buf, 0, len)
  }
  
  /** @see org.apache.avro.io.DirectBinaryDecoder#readInt */
  @throws(classOf[IOException])
  private def readTag(in: InputStream): Int = {
    var n = 0
    var b = 0
    var shift = 0
    do {
      b = in.read()
      if (b >= 0) {
        n |= (b & 0x7F) << shift
        if ((b & 0x80) == 0) {
          return (n >>> 1) ^ -(n & 1) // back to two's-complement
        }
      } else {
        throw new EOFException()
      }
      shift += 7
    } while (shift < 32)
    
    throw new IOException("Invalid int encoding")
  }
  
  /**
   * A utility method to see the reflected schema of a class
   */
  def printSchema(x: Class[_]) {
    val schema = ReflectData.AllowNull.getSchema(x)
    println(schema)
  }
  
  def prettyPrint(evts: collection.Iterable[Evt[_]]): String = {
    val sb = new StringBuffer
    
    sb.append("\n================ APIs ==============")
    evts foreach {evt =>
      sb.append("\n==============================")
      sb.append("\n\nName:       \n    ").append(evt.getClass.getName)
      sb.append("\n\nValue Class:\n    ").append(evt.tpe.getName)
      sb.append("\n\nParamters:  \n    ").append(evt.doc)
      sb.append("\n\nSchema:     \n    ").append(evt.schema.toString)
      sb.append("\n\n")
    }
    sb.append("\n================ End of APIs ==============")
    
    sb.toString
  }
  
  def apply[T: ClassTag : TypeTag](tag: Int, doc: String = "", schemaJson: String = null) = new Evt[T](tag, doc, schemaJson)
  
  // -- simple test
  def main(args: Array[String]) {
    testMatch
    testObject
    testTransientField
//    testPrimitives
    testVmap
    
//    println(prettyPrint(tagToEvt map (_._2)))
  }
  
  private def testMatch {
    import TestAPIs._

    println("\n==== apis: ")
    println(StringEvt.schema)
    println(IntEvt.schema)
    println(ArrayEvt.schema)
    println(ListEvt.schema)
    println(TupleEvt.schema)
    println(TupleWithPrimitiveArrayEvt.schema)
    
    val goodEvtMsgs = List(
      BadEmpEvt,
      EmpEvt(),
      StringEvt("a"),
      StringEvt("b"),
      IntEvt(8),
      ArrayEvt(Array("a", "b")),
      ListEvt(List("a", "b")),
      TupleEvt(8, "a", 8.0, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f)))),
      TupleEvt(8, "a", 8, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f)))),
      TupleWithPrimitiveArrayEvt("user", "abcdefghijk".getBytes("utf-8"), 1000000L)
    )

    val badEvtMsgs = List(
      Msg(-1, 8),
      Msg(-2, "a"),
      Msg(-3, Array(8, "a")),
      Msg(-3, Array(8, 8)),
      Msg(-4, List(1, "a")),
      Msg(-5, (8, "a")),
      Msg(-5, (8, 8, 8))
    )
    
    println("\n==== good evt messages: ")
    goodEvtMsgs map println
    println("\n==== bad evt messages: ")
    badEvtMsgs  map println
    
    println("\n==== regular matched: ")
    assert(!(goodEvtMsgs map regularMatch).contains(false), "Test failed")
    println("\n==== regular unmatched: ")
    //assert(!(badEvtMsgs  map regularMatch).contains(true),  "Test failed")
    
    println("\n==== advanced matched: ")
    assert(!(goodEvtMsgs map advancedMatch).contains(false), "Test failed")
    println("\n==== advanced unmatched: ")
    assert(!(badEvtMsgs  map advancedMatch).contains(true),  "Test failed") 
    
    /** 
     * @TODO bad match on ValType, need more research
     * The regular match on those evts look like: 
     */
    def regularMatch(v: Any) = v match {
      case BadEmpEvt => println("Matched emp evt"); true
      case Msg(EmpEvt.tag, aval: EmpEvt.ValType) => println("Matched emp evt2"); true
      case Msg(StringEvt.tag, aval: StringEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(IntEvt.tag, aval: IntEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(ArrayEvt.tag, aval: ArrayEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(ListEvt.tag, aval: ListEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(TupleEvt.tag, aval: TupleEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case Msg(TupleWithPrimitiveArrayEvt.tag, aval: TupleWithPrimitiveArrayEvt.ValType) => println("Matched: " + v + " => " + aval); true
      case _ => println("Unmatched: " + v); false
    }
    
    /** But we'd like a more concise approach: */
    def advancedMatch(v: Any): Boolean = v match {
      // @todo, for Scala 2.10.0: error: error during expansion of this match (this is a scalac bug).
      // The underlying error was: type mismatch;
      //  found   : Unit
      //  required: <notype>
      //    def advancedMatch(v: Any): Boolean = v match {
      //                                           ^
      // We have to change 'case EmpEvt(_)' to 'case EmpEvt()'
      //case EmpEvt(_) => println("Matched emp evt2"); true
      case EmpEvt() => println("Matched emp evt2"); true
      case StringEvt("a")  => println("Matched with value equals: " + v + " => " + "a"); true
      case StringEvt(aval) => println("Matched: " + v + " => " + aval); true
      case IntEvt(aval) => println("Matched: " + v + " => " + aval); true
      case ArrayEvt(aval) => println("Matched: " + v + " => " + aval); true
      case ListEvt(aval@List(a: String, b: String)) => println("Matched: " + v + " => " + aval); true
      case TupleEvt(aint: Int, astr: String, adou: Double, xs: Array[TestData]) => println("Matched: " + v + " => (" + aint + ", " + astr + ", " + adou + ")"); true
      case TupleWithPrimitiveArrayEvt(user: String, bytes: Array[Byte], productId: Long) => println("Matched: " + v + " => (" + user + ", " + new String(bytes) + ", " + productId + ")"); true
      case BadEmpEvt => println("Matched emp evt"); true 
      case _ => println("Unmatched: " + v); false
    }
  }
  
  private def testObject {
    import TestAPIs._
    
    printSchema(classOf[TestData])
    
    val data = TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))
    val msg = TestDataEvt(data)
    msg match {
      case TestDataEvt(data1) => println("matched: " + data1)
      case _ => error("Failed to match")
    }
    testMsg(TestDataEvt(data))
  }
  
  private def testPrimitives {
    import TestAPIs._
    
//    testMsg(EmpEvt())
//    testMsg(IntEvt(1))
//    testMsg(LongEvt(1L))
//    testMsg(FloatEvt(1.0f))
//    testMsg(DoubleEvt(1.0))
//    testMsg(BooleanEvt(true))
//    testMsg(StringEvt("abc"))
    println("test tuples")
    testMsg(PanelEvt(1000000, 35000L))
//    val map = mutable.Map[String, Array[Int]]()
//    map += "123" -> Array(12)
//    printSchema((1, "a", map).getClass)
//    testMsg(TEvt(1, "a", map))
//    testMsg(TupleEvt(1, "a", 100000L, Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f)))))
  }
  
  private def testMsg[T](msg: Msg[T]) = msg match {
    case Msg(tag, value) =>
      println(schemaOf(tag))

      val jsonBytes = toJson(value, tag)
      println(new String(jsonBytes, "UTF-8"))
      val jsonDatum = fromJson(jsonBytes, tag).get.asInstanceOf[T]
      println(jsonDatum)    

      val avroBytes = toAvro(value, tag)
      val avroDatum = fromAvro(avroBytes, tag).get.asInstanceOf[T]
      println(avroDatum)
  }

  private def testVmap {
    import TestAPIs._
    
//    val vmap = mutable.Map[String, Array[_]]()
//    vmap.put(".", Array(1L, 2L, 3L))
//    val map1 = Map() + "123" -> "abc"
//    val map2 = Map() + "123" -> "abc"
//    vmap.put("d", Array(map1, map2))
//    vmap.put("a", Array(1.0, 2.0, 3.0))
//    vmap.put("b", Array("a", "b", "c"))
//    vmap.put("c", Array(TestData("a", 1, 1.0, Array(1.0f, 2.0f, 3.0f))))

    val pc = new PriceCollection
    val pd = new PriceDistribution
    pd.price = 23.6
    pd.volumeDown=2334
    pd.volumeUp = 9803
    pc.put(pd.price.toString, pd)
    val msg = PCEvt(Array(pc))

//    val msg = TestVmapEvt(vmap)
//    println("print a map schema.")
    printSchema(Array(pc).getClass)
    
    val avroBytes = toAvro(msg.value, msg.tag)
//    val avroDatum = fromAvro(avroBytes, msg.tag).get.asInstanceOf[collection.Map[String, Array[_]]]
//    println("" + avroDatum(".")(0).asInstanceOf[Long])
//    println("" + avroDatum("d")(0).asInstanceOf[collection.Map[String, String]])
    val avroDatum = fromAvro(avroBytes, msg.tag).get.asInstanceOf[Array[PriceCollection]]
    println(avroDatum.mkString(","))
    //avroDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
    
    val jsonBytes = toJson(msg.value, msg.tag)
//    val jsonDatum = fromAvro(avroBytes, msg.tag).get.asInstanceOf[collection.Map[String, Array[_]]]
    val jsonDatum = fromJson(jsonBytes, msg.tag).get.asInstanceOf[Array[PriceCollection]]
    println(jsonDatum.mkString(","))    
    //jsonDatum foreach {case (k, v) => println(k + " -> " + v.mkString("[", ",", "]"))}
  }
  
  def testTransientField {
    import TestAPIs._
    
    printSchema(classOf[TestTransientField])
    
    val data = new TestTransientField(1, 1, 1, 1)
    val msg = TestTransientFieldEvt(data)
    msg match {
      case TestTransientFieldEvt(data1) => println("matched: " + data1)
      case _ => error("Failed to match")
    }
    testMsg(TestTransientFieldEvt(data))

  }
  
}

private[avro] object TestAPIs {

  val PanelEvt = Evt[(Int, Long)](-30)
  val EmpEvt = Evt[Unit](-1)
  val IntEvt = Evt[Int](-2)
  val LongEvt = Evt[Long](-3)
  val FloatEvt = Evt[Float](-4)
  val DoubleEvt = Evt[Double](-5)
  val BooleanEvt = Evt[Boolean](-6)
  val StringEvt = Evt[String](-7)

  val ListEvt = Evt[List[String]](-10)
  val ArrayEvt = Evt[Array[String]](-11)
  val TupleEvt = Evt[(Int, String, Double, Array[TestData])](-12, "id, name, value")
  val TEvt = Evt[(Int, String, collection.Map[String, Array[Int]])](-14, "")
  val TupleWithPrimitiveArrayEvt = Evt[(String, Array[Byte], Long)](-2002, "")

  val BadEmpEvt = Evt[AnyRef](-13) // T will be AnyRef or Nothing(scala 2.10.0)
  
  val TestDataEvt = Evt[TestData](-100)
  val TestTransientFieldEvt = Evt[TestTransientField](-101)
  val TestVmapEvt = Evt[collection.Map[String, Array[_]]](-102)//, schemaJson = """
//    {"type":"map","values":{"type":"array","items":["long","double","string",
//     {"type":"record","name":"TestData","namespace":"org.aiotrade.lib.avro.TestAPIs$",
//       "fields":[
//         {"name":"x1","type":"string"},
//         {"name":"x2","type":"int"},
//         {"name":"x3","type":"double"},
//         {"name":"x4","type":{"type":"array","items":"float"}}
//       ]}
//     ]}}
//  """)

  val PCEvt = Evt[Array[PriceCollection]](-103)//, "", """
  //{"type":"record","name":"PriceCollection","namespace":"org.aiotrade.lib.avro.TestAPIs$","fields":[{"name":"map","type":["null",{"type":"map","values":{"type":"record","name":"PriceDistribution","fields":[{"name":"_time","type":["null","long"]},{"name":"_flag","type":["null","int"]},{"name":"price","type":["null","double"]},{"name":"volumeUp","type":["null","double"]},{"name":"volumeDown","type":["null","double"]},{"name":"_uniSymbol","type":["null","string"]}]}}]},{"name":"isTransient","type":["null","boolean"]},{"name":"_time","type":["null","long"]},{"name":"_flag","type":["null","int"]},{"name":"_uniSymbol","type":["null","string"]}]}
  // """)

  final case class TestData(x1: String, x2: Int, x3: Double, x4: Array[Float]) {
    // An empty constructor is a must for evt serialization
    def this() = this(null, 0, 0.0, Array())
    override def toString = "TestData(" + x1 + ", " + x2 + ", " + x3 + ", " + x4.mkString("[", ", ", "]") + ")"
  }
  
  final class TestTransientField(
    var pubVar: Int,
    private var priVar: Double,
    @transient var pubTrans: Int,
    @transient private var priTrans: Double
  ) {
    def this() = this(0, 0, 0, 0)
    override def toString = "TestTransientField(" + pubVar + ", " + priVar + ", " + pubTrans + ", " + priTrans + ")" 
  }

  class PriceCollection extends BelongsToSec with TVal with Flag with Serializable  {
    @transient
    val cal = Calendar.getInstance
    private var map = mutable.Map[String, PriceDistribution]()

    var isTransient = true

    private var _time: Long = _
    def time = _time
    def time_=(time: Long) {this._time = time}

    private var _flag: Int = 1 // dafault is closed
    def flag = _flag
    def flag_=(flag: Int) {this._flag = flag}

    private val data = new Array[Double](2)

    def avgPrice = data(0)
    def totalVolume = data(1)

    def get(price: String) = map.get(price)

    def put(price: String, pd: PriceDistribution) = {
      if (map.isEmpty) this.time = pd.time

      map.put(price, pd)
    }

    def keys = map.keys

    def values = map.values

    def isEmpty = map.isEmpty
  }

  class PriceDistribution extends BelongsToSec with TVal with Flag with Serializable {

    private var _time: Long = _
    def time = _time
    def time_=(time: Long) {this._time = time}

    private var _flag: Int = 1 // dafault is closed
    def flag = _flag
    def flag_=(flag: Int) {this._flag = flag}

    private val data = new Array[Double](4)

    def price = data(0)
    def volumeUp = data(1)
    def volumeDown = data(2)
    def volumeEven = data(3)

    def price_= (value: Double){ data(0) = value}
    def volumeUp_= (value: Double){ data(1) = value}
    def volumeDown_= (value: Double){ data(2) = value}
    def volumeEven_= (value: Double){ data(3) = value}

    def copyFrom(another: PriceDistribution) {
      this.time = another.time
      this.flag = another.flag
      System.arraycopy(another.data, 0, data, 0, data.length)
    }
  }

  trait TVal extends Ordered[TVal] {
    def time: Long
    def time_=(time: Long)

    def compare(that: TVal): Int = {
      if (time > that.time) {
        1
      } else if (time < that.time) {
        -1
      } else {
        0
      }
    }
  }

  @serializable
  abstract class BelongsToSec {
  
    protected var _uniSymbol: String = _
    def uniSymbol = _uniSymbol
    def uniSymbol_=(uniSymbol: String) {
      this._uniSymbol = uniSymbol
    }
  }
  trait Flag {
    import Flag._

    /** dafault could be set to 1, which is closed_! */
    def flag: Int
    def flag_=(flag: Int)

    def closed_? : Boolean = (flag & MaskClosed) == MaskClosed
    def closed_!   {flag |=  MaskClosed}
    def unclosed_! {flag &= ~MaskClosed}

    def justOpen_? : Boolean = (flag & MaskJustOpen) == MaskJustOpen
    def justOpen_!   {flag |=  MaskJustOpen}
    def unjustOpen_! {flag &= ~MaskJustOpen}

    /** is this value created/composed by me or loaded from remote or other source */
    def fromMe_? : Boolean = (flag & MaskFromMe) == MaskFromMe
    def fromMe_!   {flag |=  MaskFromMe}
    def unfromMe_! {flag &= ~MaskFromMe}

  }

  object Flag {
    // bit masks for flag
    val MaskClosed    = 1 << 0   // 1   2^^0    000...00000001
    val MaskVerified  = 1 << 1   // 2   2^^1    000...00000010
    val MaskFromMe    = 1 << 2   // 4   2^^2    000...00000100
    val flagbit4      = 1 << 3   // 8   2^^3    000...00001000
    val flagbit5      = 1 << 4   // 16  2^^4    000...00010000
    val flagbit6      = 1 << 5   // 32  2^^5    000...00100000
    val flagbit7      = 1 << 6   // 64  2^^6    000...01000000
    val MaskJustOpen  = 1 << 7   // 128 2^^7    000...10000000
  }
}
