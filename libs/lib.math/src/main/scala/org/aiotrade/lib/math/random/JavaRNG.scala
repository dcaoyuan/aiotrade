package org.aiotrade.lib.math.random

import java.util.Random

/**
 * <p>This is the default {@link Random JDK RNG} extended
 * to implement the {@link RepeatableRNG} interface (for consistency with
 * the other RNGs in this package).</p>
 *
 * <p>The {@link MersenneTwisterRNG} should be used in preference to this
 * class because it is statistically more random and performs slightly
 * better.</p>
 *
 * @author Daniel Dyer
 */
class JavaRNG private (seed: Array[Byte]) extends Random(JavaRNG.createLongSeed(seed)) with RepeatableRNG {
  def getSeed: Array[Byte] = seed.clone
}

object JavaRNG {
  private val SEED_SIZE_BYTES = 8
  
  /**
   * Creates a new RNG and seeds it using the default seeding strategy.
   */
  def apply(): JavaRNG = {
    apply(SeedGenerator.generateSeed(JavaRNG.SEED_SIZE_BYTES))
  }


  /**
   * Seed the RNG using the provided seed generation strategy.
   * @param seedGenerator The seed generation strategy that will provide
   * the seed value for this RNG.
   * @throws SeedException If there is a problem generating a seed.
   */
  @throws(classOf[SeedException])
  def apply(seedGenerator: SeedGenerator): JavaRNG = {
    apply(seedGenerator.generateSeed(JavaRNG.SEED_SIZE_BYTES))
  }

  def apply(seed: Array[Byte]) = new JavaRNG(seed.clone)
  
  /**
   * Helper method to convert seed bytes into the long value required by the
   * super class.
   */
  private def createLongSeed(seed: Array[Byte]): Long = {
    if (seed == null || seed.length != SEED_SIZE_BYTES) {
      throw new IllegalArgumentException("Java RNG requires a 64-bit (8-byte) seed.")
    }
    BinaryUtils.convertBytesToLong(seed, 0)
  }
}
