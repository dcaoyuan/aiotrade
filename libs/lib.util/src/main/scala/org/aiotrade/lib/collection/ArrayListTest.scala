package org.aiotrade.lib.collection

/**
 * 
 * @author Caoyuan Deng
 */
import scala.reflect.ClassTag

object ArrayListTest {
  
  // --- simple test
  def main(args: Array[String]) {
    val test = new Test[Double]
    test.insertAll(1.0)
    test.insertOne(0.0)
    //test.insertOk(1.0, 2.0)
    //test.insertOk(1.0)
    //test.insertFailed(1.0)
  }
  
  private class Test[V: ClassTag] {
    val values = new ArrayList[V]()
    
    def insertOne(v: V) {
      values.insertOne(0, v)
      println(values)
    }

    def insertAll(v: V) {
      values.insertAll(0, Array(v))
      println(values)
    }

    def insertOk(v: V) {
      val xs = Array(v)
      values.insert(0, xs :_*)
      println(values)
    }

    def insertOk(v1: V, v2: V) {
      val xs = Array(v1, v2)
      values.insert(0, xs :_*)
      println(values)
    }

    def insertFailed(v: V) {
      // v will be boxed to java.lang.Object (java.lang.Double) due to ClassTag, 
      // then wrapped as Array[Object] and passed to function in scala.LowPriorityImplicits:
      //    implicit def genericWrapArray[T](xs: Array[T]): WrappedArray[T]
      // for insert(n: Int, elems: A*), and causes ArrayStoreException.
      values.insert(0, v)
      println(values)
    }

    def insertFailed(v1: V, v2: V) {
      values.insert(0, v1, v2)
      println(values)
    }

  }
}
