package org.aiotrade.lib.math.random

import java.util.Random

/**
 * <a href="http://en.wikipedia.org/wiki/Normal_distribution" target="_top">Normally distributed</a>
 * random sequence.
 * <p>Creates a generator of normally-distributed values.  The mean and
 * standard deviation are determined by the provided
 * {@link NumberGenerator}s.  This means that the statistical parameters
 * of this generator may change over time.  One example of where this
 * is useful is if the mean and standard deviation generators are attached
 * to GUI controls that allow a user to tweak the parameters while a
 * program is running.</p>
 * <p>To create a Gaussian generator with a constant mean and standard
 * deviation, use the {@link #GaussianGenerator(double, double, Random)}
 * constructor instead.</p>
 * @param mean A {@link NumberGenerator} that provides the mean of the
 * Gaussian distribution used for the next generated value.
 * @param standardDeviation A {@link NumberGenerator} that provides the
 * standard deviation of the Gaussian distribution used for the next
 * generated value.
 * @param rng The source of randomness. 
 * 
 * @author Daniel Dyer
 */
class GaussianGenerator private (mean: NumberGenerator[Double], stdDeviation: NumberGenerator[Double], rng: Random) extends NumberGenerator[Double] {
  def nextValue: Double = rng.nextGaussian * stdDeviation.nextValue + mean.nextValue
}

object GaussianGenerator {
  /**
   * Creates a generator of normally-distributed values from a distribution
   * with the specified mean and standard deviation.
   * @param mean The mean of the values generated.
   * @param standardDeviation The standard deviation of the values generated.
   * @param rng The source of randomness.
   */
  def apply(mean: Double, stdDeviation: Double, rng: Random): GaussianGenerator = {
    new GaussianGenerator(new ConstantGenerator[Double](mean), new ConstantGenerator[Double](stdDeviation), rng)
  }
}
