package org.aiotrade.lib.math.random

/**
 * Deterministic random number generators are repeatable, which can prove
 * useful for testing and validation.  This interface defines an operation
 * to return the seed data from a repeatable RNG.  This seed value can then
 * be reused to create a random source with identical output.
 * @author Daniel Dyer
 */
trait RepeatableRNG {
  /**
   * @return The seed data used to initialise this pseudo-random
   * number generator.
   */
  def getSeed: Array[Byte]
}
