package org.aiotrade.lib.collection

import scala.reflect.ClassTag

object WeakIdentityBiHashMapTest {
  def main(args: Array[String]) {
    test[String](
      Array(new String("newString1"),
            "literalString2",
            "literalString3",
            new String("newString4"),
            new String("newString5")
      ))

    testLots
  }

  def test[T: ClassTag](keys: Array[T]) {
    var k1 = keys(0)
    var k2 = keys(1)
    var k3 = keys(2)
    var k4 = keys(3)
    var k5 = keys(4)

    val map = new WeakIdentityBiHashMap[T, Long] //new WeakKeyHashMap[Any, Object]

    val v1 = 1
    map.put(k1, v1)
    val v2 = 2
    map.put(k2, v2)
    val v3 = 3
    map.put(k3, v3)
    val v4 = 4
    map.put(k4, v4)

    map foreach println

    assert(v1 == map.get(k1).get)
    assert(v2 == map.get(k2).get)
    assert(v3 == map.get(k3).get)
    assert(v4 == map.get(k4).get)

    assert(k1 == map.getByValue(v1).get)
    assert(k2 == map.getByValue(v2).get)
    assert(k3 == map.getByValue(v3).get)
    assert(k4 == map.getByValue(v4).get)

    // this put should remove k4, the pair is now: k5 <-> v4
    map.put(k5, v4)
    assert(None == map.get(k4))
    assert(k5 == map.getByValue(v4).get)
    assert(v4 == map.get(k5).get)

    /**
     * Discard the strong reference to all the keys.
     * @Note: shoule set elements in keys to null too.
     */
    k1 = null.asInstanceOf[T]
    k2 = null.asInstanceOf[T]
    k3 = null.asInstanceOf[T]
    k4 = null.asInstanceOf[T]
    k5 = null.asInstanceOf[T]
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
      println("map.size should be 2: " + map.size + "  " + map.mkString("[", ", ", "]"))
      println("key by value 1 should be None: " + map.getByValue(1))
      println("key by value 2 should be Some: " + map.getByValue(2))
      println("key by value 3 should be Some: " + map.getByValue(3))
      println("key by value 4 should be None: " + map.getByValue(4))
    }
  }

  def testLots {
    val size = 10000
    val map = new WeakIdentityBiHashMap[String, Long]
    var keys = for (i <- 0 until size) yield {
      val k = new String("String" + i)
      map.put(k, i)
      k
    }

    //map foreach println

    for (i <- 0 until size) {
      //println(i)
      assert(map.getByValue(i).get == ("String" + i), "Value " + i + " lost entry")
      assert(map.get("String" + i).isEmpty, "Identity Map error at " + i + ": new String does not identity equal")
      assert(map.get(keys(i)).get == i, "Key " + keys(i) + " value should be: " + i + ", but it's: " + map.get(keys(i)))
    }

    keys = null

    (0 until 20) foreach {_ =>
      System.gc
      /**
       * Verify Full GC with the -verbose:gc option
       * We expect the map to be emptied as the strong references to
       * all the keys are discarded. The map size should be 2 now
       */
      println("map.size should be 0: " + map.size + "  " + map.mkString("[", ", ", "]"))
    }
  }
}
