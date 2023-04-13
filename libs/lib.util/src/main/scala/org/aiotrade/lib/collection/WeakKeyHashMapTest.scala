package org.aiotrade.lib.collection

import scala.reflect.ClassTag

object WeakKeyHashMapTest {

  def main(args: Array[String]) {
    test[String](
      Array(new String("newString1"),
            "literalString2",
            "literalString3",
            new String("newString4")))

    test[Long](
      Array(1L,
            2L,
            3L,
            4L))

  }

  def test[T: ClassTag](keys: Array[T]) {
    var k1 = keys(0)
    var k2 = keys(1)
    var k3 = keys(2)
    var k4 = keys(3)

    val map = new WeakIdentityHashMap[T, Object] //new WeakKeyHashMap[Any, Object]

    val v1 = new Object
    map.put(k1, v1)
    val v2 = new Object
    map.put(k2, v2)
    val v3 = new Object
    map.put(k3, v3)
    val v4 = new Object
    map.put(k4, v4)

    map foreach println

    assert(v1 eq map.get(k1).get)
    assert(v2 eq map.get(k2).get)
    assert(v3 eq map.get(k3).get)
    assert(v4 eq map.get(k4).get)

    /**
     * Discard the strong reference to all the keys.
     * @Note: shoule set elements in keys to null too.
     */
    k1 = null.asInstanceOf[T]
    k2 = null.asInstanceOf[T]
    k3 = null.asInstanceOf[T]
    k4 = null.asInstanceOf[T]
    var i = 0
    while (i < keys.length) {
      keys(i) = null.asInstanceOf[T]
      i += 1  
    }

    (0 until 20) foreach {_ =>
      System.gc
      /**
       * Verify Full GC with the -verbose:gc option
       * We expect the map to be emptied as the strong references to
       * all the keys are discarded. The map size should be 2 now
       */
      println("map.size = " + map.size + "  " + map.mkString("[", ", ", "]"))
    }
  }
}
