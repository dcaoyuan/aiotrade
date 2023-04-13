package org.aiotrade.lib.math.random

import java.util.Random
import java.util.concurrent.locks.ReentrantLock

/**
 * Very fast pseudo random number generator.  See
 * <a href="http://school.anhb.uwa.edu.au/personalpages/kwessen/shared/Marsaglia03.html">this
 * page</a> for a description.  This RNG has a period of about 2^160, which is not as long
 * as the {@link MersenneTwisterRNG} but it is faster.
 * @author Daniel Dyer
 * @since 1.2
 */
class XORShiftRNG private (seed: Array[Byte], state: Array[Int]) extends Random with RepeatableRNG {

  // Previously used an array for state but using separate fields proved to be
  // faster.
  private var state1 = state(0)
  private var state2 = state(1)
  private var state3 = state(2)
  private var state4 = state(3)
  private var state5 = state(4)

  // Lock to prevent concurrent modification of the RNG's internal state.
  private val lock = new ReentrantLock

  def getSeed = seed.clone

  override
  protected def next(bits: Int): Int = {
    try {
      lock.lock
      var t = (state1 ^ (state1 >> 7))
      state1 = state2
      state2 = state3
      state3 = state4
      state4 = state5
      state5 = (state5 ^ (state5 << 6)) ^ (t ^ (t << 13))
      val value = (state2 + state2 + 1) * state5
      value >>> (32 - bits)
    } finally {
      lock.unlock
    }
  }
}


object XORShiftRNG {
  private val SEED_SIZE_BYTES = 20 // Needs 5 32-bit integers.
  
  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  def apply(): XORShiftRNG = {
    apply(SeedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  @throws(classOf[SeedException])
  def apply(seedGenerator: SeedGenerator): XORShiftRNG = {
    apply(seedGenerator.generateSeed(SEED_SIZE_BYTES))
  }

  /**
   * Creates an RNG and seeds it with the specified seed data.
   * @param seed The seed data used to initialise the RNG.
   */
  def apply(seed: Array[Byte]): XORShiftRNG = {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("XOR shift RNG requires 160 bits of seed data.")
    }
    val state = BinaryUtils.convertBytesToInts(seed)
    new XORShiftRNG(seed.clone, state)
  }
}