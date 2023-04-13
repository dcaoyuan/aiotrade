package org.aiotrade.lib.util


import org.aiotrade.lib.collection.ArrayList
import scala.collection.mutable.ArrayBuffer


/**
 * http://www.roseindia.net/javatutorials/determining_memory_usage_in_java.shtml
 *
 * Memory usage of a single-dimension array

 A single-dimension array is a single object. As expected, the array has the usual object header. However, this object head is 12 bytes to accommodate a four-byte array length. Then comes the actual array data which, as you might expect, consists of the number of elements multiplied by the number of bytes required for one element, depending on its type. The memory usage for one element is 4 bytes for an object reference; for a list of the memory usage of primitive types, see the page on Java data types. If the total memory usage of the array is not a multiple of 8 bytes, then the size is rounded up to the next mutlitple of 8 (just as for any other object).

 Note that a boolean array requires one byte per element, even though each element actually only stores a single bit of useful information. (If you need to store a series of bits more compactly, see the BitSet class.)

 Memory usage of a two-dimensional array

 In a language such as C, a two-dimensional array (or indeed any multidimensional array) is essentially a one-dimensional array with judicious pointer manipulation. This is not the case in Java, where a multidimensional array is actually a set of nested arrays. This means that every row of a two-dimensional array has the overhead of an object, since it actually is a separate object!

 For example, let's consider a 10x10 int array. Firstly, the "outer" array has its 12-byte object header followed by space for the 10 elements. Those elements are object references to the 10 arrays making up the rows. That comes to 12+4*10=52 bytes, which must then be rounded up to the next multiple of 8, giving 56. Then, each of the 10 rows has its own 12-byte object header, 4*10=40 bytes for the actual row of ints, and again, 4 bytes of padding to bring the total for that row to a multiple of 8. So in total, that gives 11*56=616 bytes. That's a bit bigger than if you'd just counted on 10*10*4=400 bytes for the hundred "raw" ints themselves.

 Multidimensional arrays

 For arrays of more than 2 dimensions, the above logic repeats: each row of the "outer" array is now an array of references to a further array, which contains the actual primitive data (or references if it is an object array).
 */
object MemoryBench {

  trait ObjectFactory {
    def makeObject: Object
  }

  private def calculateMemoryUsage(factory: ObjectFactory): Long = {
    var handle = factory.makeObject
    var mem0 = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    var mem1 = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory

    handle = null
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc

    mem0 = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory
    
    handle = factory.makeObject
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc
    System.gc; System.gc; System.gc; System.gc
    mem1 = Runtime.getRuntime.totalMemory - Runtime.getRuntime.freeMemory

    mem1 - mem0
  }

  def showMemoryUsage(factory: ObjectFactory) {
    val mem = calculateMemoryUsage(factory)
    println(factory.makeObject.getClass.getName + " which took " + mem + " bytes")
  }


  // --- simple object factories

  object BasicObjectFactory extends ObjectFactory {
    def makeObject = new Object
  }

  object ByteFactory extends ObjectFactory {
    def makeObject = new java.lang.Byte(33.toByte)
  }

  object ThreeByteFactory extends ObjectFactory {
    class ThreeBytes {
      var b0, b1, b2 : Byte = _
    }

    def makeObject = new ThreeBytes
  }

  object SixtyFourBooleanFactory extends ObjectFactory {
    class SixtyFourBooleans {
      var a0, a1, a2, a3, a4, a5, a6, a7 : Boolean = _
      var b0, b1, b2, b3, b4, b5, b6, b7 : Boolean = _
      var c0, c1, c2, c3, c4, c5, c6, c7 : Boolean = _
      var d0, d1, d2, d3, d4, d5, d6, d7 : Boolean = _
      var e0, e1, e2, e3, e4, e5, e6, e7 : Boolean = _
      var f0, f1, f2, f3, f4, f5, f6, f7 : Boolean = _
      var g0, g1, g2, g3, g4, g5, g6, g7 : Boolean = _
      var h0, h1, h2, h3, h4, h5, h6, h7 : Boolean = _
    }

    def makeObject = new SixtyFourBooleans
  }

  object BooleanArrayFactory extends ObjectFactory {
    def makeObject = {
      val objs = new Array[java.lang.Boolean](1000)
      var i = 0
      while (i < objs.length) {
        objs(i) = new java.lang.Boolean(true)
        i += 1
      }
      objs
    }
  }


  object PrimitiveByteArrayFactory extends ObjectFactory {
    def makeObject = new Array[Byte](1000)
  }


  object StringFactory extends ObjectFactory {
    def makeObject = {
      val buf = new StringBuffer(12)
      buf.append("Hello ")
      buf.append("World!")
      buf.toString
    }
  }


  object VectorFactory extends ObjectFactory {
    def makeObject = new java.util.Vector(10)
  }

  object FullArrayListFactory extends ObjectFactory {
    def makeObject = {
      val result = new ArrayList[Float](10000)
      var i = 0
      while (i < 10000) {
        result += 1.0f
        i += 1
      }
      result
    }
  }

  object FullLArrayBufferFactory extends ObjectFactory {
    def makeObject = {
      val result = new ArrayBuffer[Float]
      var i = 0
      while (i < 10000) {
        result += 1.0f
        i += 1
      }
      result
    }
  }


  // --- simple test
  def main(args: Array[String]) {
    showMemoryUsage(BasicObjectFactory)
    showMemoryUsage(ByteFactory)
    showMemoryUsage(ThreeByteFactory)
    showMemoryUsage(SixtyFourBooleanFactory)
    showMemoryUsage(BooleanArrayFactory)
    showMemoryUsage(PrimitiveByteArrayFactory)
    showMemoryUsage(StringFactory)
    showMemoryUsage(VectorFactory)
    showMemoryUsage(FullArrayListFactory)
    showMemoryUsage(FullLArrayBufferFactory)
  }
}

