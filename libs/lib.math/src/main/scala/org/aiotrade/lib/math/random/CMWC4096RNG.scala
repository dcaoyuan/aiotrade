package org.aiotrade.lib.math.random

import java.util.Random
import java.util.concurrent.locks.ReentrantLock

/**
 * <p>A Java version of George Marsaglia's
 * <a href="http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html">Complementary
 * Multiply With Carry (CMWC) RNG</a>.
 * This is a very fast PRNG with an extremely long period (2^131104).  It should be used
 * in preference to the {@link MersenneTwisterRNG} when a very long period is required.</p>
 *
 * <p>One potential drawback of this RNG is that it requires significantly more seed data than
 * the other RNGs provided by Uncommons Maths.  It requires just over 16 kilobytes, which may
 * be a problem if your are obtaining seed data from a slow or limited entropy source.
 * In contrast, the Mersenne Twister requires only 128 bits of seed data.</p>
 *  
 * @author Daniel Dyer
 * @since 1.2
 */
class CMWC4096RNG private (seed: Array[Byte], state: Array[Int]) extends Random with RepeatableRNG {

  private var carry = 362436 // TO DO: This should be randomly generated.
  private var index = 4095

  // Lock to prevent concurrent modification of the RNG's internal state.
  private val lock = new ReentrantLock

  def getSeed = seed.clone

  override
  protected def next(bits: Int): Int = {
    try {
      lock.lock
      index = (index + 1) & 4095
      val t = CMWC4096RNG.A * (state(index) & 0xFFFFFFFFL) + carry
      carry =  (t >> 32).toInt
      var x = t.toInt + carry
      if (x < carry) {
        x += 1
        carry += 1
      }
      state(index) = 0xFFFFFFFE - x
      state(index) >>> (32 - bits)
    } finally {
      lock.unlock
    }
  }
}

object CMWC4096RNG {
  private val SEED_SIZE_BYTES = 16384 // Needs 4,096 32-bit integers.
  private val A = 18782L

  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  def apply(): CMWC4096RNG = {
    apply(SeedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  @throws(classOf[SeedException])
  def apply(seedGenerator: SeedGenerator): CMWC4096RNG = {
    apply(seedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG.
   */
  def apply(seed: Array[Byte]): CMWC4096RNG = {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("CMWC RNG requires 16kb of seed data.")
    }
    new CMWC4096RNG(seed.clone, BinaryUtils.convertBytesToInts(seed))
  }
}