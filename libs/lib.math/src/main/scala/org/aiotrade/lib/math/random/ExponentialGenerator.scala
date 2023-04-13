package org.aiotrade.lib.math.random

import java.util.Random

/**
 * Continuous random sequence that follows an
 * <a href="http://en.wikipedia.org/wiki/Exponential_distribution" target="_top">exponential
 * distribution</a>.
 * Creates a generator of exponentially-distributed values from a distribution
 * with a rate controlled by the specified generator parameter.  The mean of
 * this distribution is {@literal 1 / rate}
 * and the variance is {@literal 1 / rate^2}.
 * @param rate A number generator that provides values to use as the rate for
 * the exponential distribution.  This generator must only return non-zero, positive
 * values.
 * @param rng The source of randomness used to generate the exponential values.
 * @author Daniel Dyer
 * @since 1.0.2
 */
class ExponentialGenerator private (rate: NumberGenerator[Double], rng: Random) extends NumberGenerator[Double] {
  /**
   * Generate the next exponential value from the current value of
   * {@literal rate}.
   * @return The next exponentially-distributed value.
   */
  def nextValue: Double = {
    var u = 0.0
    do {
      // Get a uniformally-distributed random double between
      // zero (inclusive) and 1 (exclusive)
      u = rng.nextDouble
    } while (u == 0.0) // Reject zero, u must be positive for this to work.
    -math.log(u) / rate.nextValue
  }
}

object ExponentialGenerator {
  def apply(rate: NumberGenerator[Double], rng: Random) = new ExponentialGenerator(rate, rng)

  /**
   * Creates a generator of exponentially-distributed values from a distribution
   * with the specified rate.  The mean of this distribution is {@literal 1 / rate}
   * and the variance is {@literal 1 / rate^2}.
   * @param rate The rate (lamda) of the exponential distribution.
   * @param rng The source of randomness used to generate the exponential values.
   */
  def apply(rate: Double, rng: Random) = new ExponentialGenerator(new ConstantGenerator[Double](rate), rng)
}