package org.aiotrade.lib.math.random

import java.util.Random
import java.util.concurrent.locks.ReentrantLock

/**
 * <p>Random number generator based on the
 * <a href="http://www.math.sci.hiroshima-u.ac.jp/~m-mat/MT/emt.html" target="_top">Mersenne
 * Twister</a> algorithm developed by Makoto Matsumoto and Takuji Nishimura.</p>
 *
 * <p>This is a very fast random number generator with good statistical
 * properties (it passes the full DIEHARD suite).  This is the best RNG
 * for most experiments.  If a non-linear generator is required, use
 * the slower {@link AESCounterRNG} RNG.</p>
 *
 * <p>This PRNG is deterministic, which can be advantageous for testing purposes
 * since the output is repeatable.  If multiple instances of this class are created
 * with the same seed they will all have identical output.</p>
 *
 * <p>This code is translated from the original C version and assumes that we
 * will always seed from an array of bytes.  I don't pretend to know the
 * meanings of the magic numbers or how it works, it just does.</p>
 *
 * @author Makoto Matsumoto and Takuji Nishimura (original C version)
 * @author Daniel Dyer (Java port)
 * @author Caoyuan Deng (Scala port)
 */
class MersenneTwisterRNG private (seed: Array[Byte], mt: Array[Int]) extends Random with RepeatableRNG {
  import MersenneTwisterRNG._
  
  // Lock to prevent concurrent modification of the RNG's internal state.
  private val lock = new ReentrantLock
  private var mtIndex = N // Index into state vector.

  def getSeed = seed.clone

  override
  final protected def next(bits: Int): Int = {
    var y = 0
    try {
      lock.lock
      if (mtIndex >= N) { // Generate N ints at a time.
        var kk = 0
        while (kk < N - M) {
          y = (mt(kk) & UPPER_MASK) | (mt(kk + 1) & LOWER_MASK)
          mt(kk) = mt(kk + M) ^ (y >>> 1) ^ MAG01(y & 0x1)
          kk += 1
        }
        while (kk < N - 1) {
          y = (mt(kk) & UPPER_MASK) | (mt(kk + 1) & LOWER_MASK)
          mt(kk) = mt(kk + (M - N)) ^ (y >>> 1) ^ MAG01(y & 0x1)
          kk += 1
        }
        y = (mt(N - 1) & UPPER_MASK) | (mt(0) & LOWER_MASK)
        mt(N - 1) = mt(M - 1) ^ (y >>> 1) ^ MAG01(y & 0x1)

        mtIndex = 0
      }

      y = mt(mtIndex)
      mtIndex += 1
    } finally {
      lock.unlock
    }
    // Tempering
    y ^= (y >>> 11)
    y ^= (y << 7) & GENERATE_MASK1
    y ^= (y << 15) & GENERATE_MASK2
    y ^= (y >>> 18)

    y >>> (32 - bits)
  }
}

object MersenneTwisterRNG {
  // The actual seed size isn't that important, but it should be a multiple of 4.
  private val SEED_SIZE_BYTES = 16

  // Magic numbers from original C version.
  private val N = 624
  private val M = 397
  private val MAG01 = Array(0, 0x9908b0df)
  private val UPPER_MASK = 0x80000000
  private val LOWER_MASK = 0x7fffffff
  private val BOOTSTRAP_SEED = 19650218
  private val BOOTSTRAP_FACTOR = 1812433253
  private val SEED_FACTOR1 = 1664525
  private val SEED_FACTOR2 = 1566083941
  private val GENERATE_MASK1 = 0x9d2c5680
  private val GENERATE_MASK2 = 0xefc60000
    
  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  def apply(): MersenneTwisterRNG = {
    apply(SeedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  @throws(classOf[SeedException])
  def apply(seedGenerator: SeedGenerator): MersenneTwisterRNG = {
    apply(seedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG.
   */
  def apply(seed: Array[Byte]): MersenneTwisterRNG = {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Mersenne Twister RNG requires a 128-bit (16-byte) seed.")
    }

    val seedInts = BinaryUtils.convertBytesToInts(seed)
    val mt = new Array[Int](N)
    
    mt(0) = BOOTSTRAP_SEED
    var mtIndex = 1
    while (mtIndex < N) {
      mt(mtIndex) = (BOOTSTRAP_FACTOR * (mt(mtIndex - 1) ^ (mt(mtIndex - 1) >>> 30)) + mtIndex)
      mtIndex += 1
    }

    var i = 1
    var j = 0
    var k = math.max(N, seedInts.length)
    while (k > 0) {
      mt(i) = (mt(i) ^ ((mt(i - 1) ^ (mt(i - 1) >>> 30)) * SEED_FACTOR1)) + seedInts(j) + j
      i += 1
      j += 1
      if (i >= N) {
        mt(0) = mt(N - 1)
        i = 1
      }
      if (j >= seedInts.length) {
        j = 0
      }
      k -= 1
    }
    
    k = N - 1
    while (k > 0) {
      mt(i) = (mt(i) ^ ((mt(i - 1) ^ (mt(i - 1) >>> 30)) * SEED_FACTOR2)) - i
      i += 1
      if (i >= N) {
        mt(0) = mt(N - 1)
        i = 1
      }
      k -= 1
    }
    mt(0) = UPPER_MASK // Most significant bit is 1 - guarantees non-zero initial array.
    
    new MersenneTwisterRNG(seed.clone, mt)
  }

}