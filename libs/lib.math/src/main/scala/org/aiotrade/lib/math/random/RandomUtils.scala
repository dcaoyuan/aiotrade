package org.aiotrade.lib.math.random

import java.util.Collections
import java.util.Random
import java.util.WeakHashMap
import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * <p>
 * The source of random stuff for the whole project. This lets us make all randomness in the project
 * predictable, if desired, for when we run unit tests, which should be repeatable.
 * </p>
 * 
 * <p>
 * This class is increasingly incorrectly named as it also includes other mathematical utility methods.
 * </p>
 */
object RandomUtils {

  /** The largest prime less than 2<sup>31</sup>-1 that is the smaller of a twin prime pair. */
  val MAX_INT_SMALLER_TWIN_PRIME = 2147482949

  private val INSTANCES = Collections.synchronizedMap(new WeakHashMap[RandomWrapper, Boolean]())
  
  private val SHUFFLE_THRESHOLD = 5
  
  def useTestSeed {
    RandomWrapper.useTestSeed
    INSTANCES synchronized {
      val itr = INSTANCES.keySet.iterator
      while (itr.hasNext) {
        val rng = itr.next
        rng.reset
      }
    }
  }
  
  def getRandom: Random = {
    val random = new RandomWrapper()
    INSTANCES.put(random, true)
    random
  }
  
  def getRandom(seed: Long): Random = {
    val random = new RandomWrapper(seed)
    INSTANCES.put(random, true)
    random
  }
  
  def longSeedtoBytes(seed: Long): Array[Byte] = {
    val seedBytes = new Array[Byte](16)
    seedBytes(0) = (seed >>> 56).toByte
    seedBytes(1) = (seed >>> 48).toByte
    seedBytes(2) = (seed >>> 40).toByte
    seedBytes(3) = (seed >>> 32).toByte
    seedBytes(4) = (seed >>> 24).toByte
    seedBytes(5) = (seed >>> 16).toByte
    seedBytes(6) = (seed >>> 8).toByte
    seedBytes(7) = seed.toByte
    System.arraycopy(seedBytes, 0, seedBytes, 8, 8)
    seedBytes
  }
  
  def seedBytesToLong(seed: Array[Byte]): Long = {
    var result = 0L
    var i = 0
    while (i < 8) {
      result |= (seed(i) & 0xFFL) << (8 * (7 - i)).toLong
      i += 1
    }
    result
  }
  
  /** @return what {@link Double#hashCode()} would return for the same value */
  def hashDouble(value: Double): Int = {
    val v = java.lang.Double.doubleToLongBits(value)
    (v ^ (v >>> 32)).toInt
  }

  /** @return what {@link Float#hashCode()} would return for the same value */
  def hashFloat(value: Float): Int = {
    java.lang.Float.floatToIntBits(value)
  }
  
  /**
   * <p>
   * Finds next-largest "twin primes": numbers p and p+2 such that both are prime. Finds the smallest such p
   * such that the smaller twin, p, is greater than or equal to n. Returns p+2, the larger of the two twins.
   * </p>
   */
  def nextTwinPrime(n: Int): Int = {
    if (n > MAX_INT_SMALLER_TWIN_PRIME) {
      throw new IllegalArgumentException()
    }
    if (n <= 3) {
      5
    } else {
      var next = nextPrime(n)
      while (isNotPrime(next + 2)) {
        next = nextPrime(next + 4)
      }
      next + 2
    }
  }
  
  /**
   * <p>
   * Finds smallest prime p such that p is greater than or equal to n.
   * </p>
   */
  def nextPrime(_n: Int): Int = {
    if (_n <= 2) {
      2
    } else {
      var n = _n
      // Make sure the number is odd. Is this too clever?
      n |= 0x1
      // There is no problem with overflow since Integer.MAX_INT is prime, as it happens
      while (isNotPrime(n)) {
        n += 2
      }
      n
    }
  }
  
  /** @return {@code true} iff n is not a prime */
  def isNotPrime(n: Int): Boolean = {
    if (n < 2 || (n & 0x1) == 0) { // < 2 or even
      n != 2
    } else {
      val max = 1 + math.sqrt(n).toInt
      var d = 3
      while (d <= max) {
        if (n % d == 0) {
          return true
        }
        d += 2
      }
      false
    }
  }
  
  def shuffle[T: ClassTag](seq: Seq[T]): Seq[T] = {
    val rnd = getRandom
    seq match {
      case xs: mutable.IndexedSeq[T] =>
        val n = xs.length
        var i = n
        while (i > 1) {
          swap(xs, i - 1, rnd.nextInt(i))
          i -= 1
        }
        xs
        
      case list: List[T] =>
        val xs = seq.toArray
        val n = xs.length
        var i = n
        while (i > 1) {
          swap(xs, i - 1, rnd.nextInt(i))
          i -= 1
        }
        
        var newSeq = List[T]()
        i = 0
        while (i < n) {
          newSeq ::= xs(i)
          i += 1
        }
        newSeq
        
      case _ => seq // @todo
    }
  }
  
  private def swap[T](list: mutable.IndexedSeq[T], i: Int, j: Int) {
    val tmp = list(i)
    list(i) = list(j)
    list(j) = tmp
  }
}
