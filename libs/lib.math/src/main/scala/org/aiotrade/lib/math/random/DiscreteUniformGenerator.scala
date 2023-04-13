package org.aiotrade.lib.math.random

import java.util.Random

/**
 * Discrete, uniformly distributed random sequence.  Generates
 * values between the specified minimum and maximum values (inclusive).
 * @author Daniel Dyer
 */
class DiscreteUniformGenerator(minValue: Int, maxValue: Int, rng: Random) extends NumberGenerator[Int] {
  private val range = maxValue - minValue + 1

  def nextValue: Int = rng.nextInt(range) + minValue
}
