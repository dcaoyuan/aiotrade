package org.aiotrade.lib.math.random

import java.util.Random

/**
 * Continuous, uniformly distributed random sequence.  Generates
 * values in the range {@literal mininum (inclusive) ... maximum (exclusive)}.
 * @author Daniel Dyer
 * @author Caoyuan Deng
 */
class ContinuousUniformGenerator private (minValue: Double, maxValue: Double, rng: Random) extends NumberGenerator[Double] {
  private val range = maxValue - minValue
  
  def nextValue = rng.nextDouble * range + minValue
}

object ContinuousUniformGenerator {
  def apply(minValue: Double, maxValue: Double, rng: Random) = new ContinuousUniformGenerator(minValue, maxValue, rng)
}
