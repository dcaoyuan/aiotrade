package org.aiotrade.lib.math.random


/**
 * Implements an uncommons math compatible generator that avoids /dev/random's tendency to block
 * due to entropy underflow.
 */
object FastRandomSeedGenerator extends SeedGenerator {

  private final val generators = List[SeedGenerator](DevURandomSeedGenerator, SecureRandomSeedGenerator)

  /**
   * Generate a seed value for a random number generator.  Try the /dev/urandom generator
   * first, and then fall back to SecureRandomSeedGenerator to guarantee a result.  On
   * platforms with /dev/random, /dev/urandom should exist and thus be fast and pretty good.
   * On platforms without /dev/random, the fallback strategies should also be pretty fast.
   *
   * @param length The length of the seed to generate (in bytes).
   * @return A byte array containing the seed data.
   * @throws org.uncommons.maths.random.SeedException
   *          If a seed cannot be generated for any reason.
   */
  @throws(classOf[SeedException])
  override
  def generateSeed(length: Int): Array[Byte] = {
    var savedException: SeedException = null
    for (generator <- generators) {
      try {
        return generator.generateSeed(length)
      } catch {
        case ex: SeedException =>
          if (savedException != null) {
            savedException.initCause(ex)
          }
          savedException = ex
      }
    }
    
    if (savedException != null) {
      throw savedException
    } else {
      throw new IllegalStateException("Couldn't generate seed, but didn't find an exception. Can't happen.")
    }
  }
}
