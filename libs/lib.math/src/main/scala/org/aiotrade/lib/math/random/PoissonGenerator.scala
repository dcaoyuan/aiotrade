package org.aiotrade.lib.math.random

import java.util.Random

/**
 * Discrete random sequence that follows a
 * <a href="http://en.wikipedia.org/wiki/Poisson_distribution" target="_top">Poisson
 * distribution</a>.
 * @author Daniel Dyer
 */
class PoissonGenerator private (mean: NumberGenerator[Double], rng: Random) extends NumberGenerator[Int] {

  /**
   * {@inheritDoc}
   */
  def nextValue: Int = {
    var x = 0
    var t = 0.0
    var continue = true
    while (continue) {
      t -= math.log(rng.nextDouble) / mean.nextValue
      if (t > 1.0) {
        continue = false
      } else {
        x += 1
      }
    }
    x
  }
}

object PoissonGenerator {
  
  /**
   * <p>Creates a generator of Poisson-distributed values.  The mean is
   * determined by the provided {@link org.uncommons.maths.number.NumberGenerator}.  This means that
   * the statistical parameters of this generator may change over time.
   * One example of where this is useful is if the mean generator is attached
   * to a GUI control that allows a user to tweak the parameters while a
   * program is running.</p>
   * <p>To create a Poisson generator with a constant mean, use the
   * {@link #PoissonGenerator(double, Random)} constructor instead.</p>
   * @param mean A {@link NumberGenerator} that provides the mean of the
   * Poisson distribution used for the next generated value.
   * @param rng The source of randomness.
   */
  def apply(mean: NumberGenerator[Double], rng: Random ): PoissonGenerator = {
    new PoissonGenerator(mean, rng)
  }

  /**
   * Creates a generator of Poisson-distributed values from a distribution
   * with the specified mean.
   * @param mean The mean of the values generated.
   * @param rng The source of randomness.
   */
  def apply(mean: Double, rng: Random ): PoissonGenerator = {
    if (mean <= 0) {
      throw new IllegalArgumentException("Mean must be a positive value.")
    }
    apply(new ConstantGenerator[Double](mean), rng)
  }
}