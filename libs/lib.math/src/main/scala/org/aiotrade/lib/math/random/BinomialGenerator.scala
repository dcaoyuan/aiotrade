package org.aiotrade.lib.math.random

import java.util.Random
import org.aiotrade.lib.math.BitString


/**
 * Discrete random sequence that follows a
 * <a href="http://en.wikipedia.org/wiki/Binomial_distribution" target="_top">binomial
 * distribution</a>.
 * 
 * <p>Creates a generator of binomially-distributed values.  The number of
 * trials ({@literal n}) and the probability of success in each trial
 * ({@literal p}) are determined by the provided {@link NumberGenerator}s.
 * This means that the statistical parameters of this generator may change
 * over time.  One example of where this is useful is if the {@literal n}
 * and {@literal p} generators are attached to GUI controls that allow a
 * user to tweak the parameters while a program is running.</p>
 * <p>To create a Binomial generator with a constant {@literal n} and
 * {@literal p}, use the {@link #BinomialGenerator(int, double, Random)}
 * constructor instead.</p>
 * @param n A {@link NumberGenerator} that provides the number of trials for
 * the Binomial distribution used for the next generated value.  This generator
 * must produce only positive values.
 * @param p A {@link NumberGenerator} that provides the probability of succes
 * in a single trial for the Binomial distribution used for the next
 * generated value.  This generator must produce only values in the range 0 - 1.
 * @param rng The source of randomness.
 * 
 * @author Daniel Dyer
 */
class BinomialGenerator private (n: NumberGenerator[Int], p: NumberGenerator[Double], rng: Random) extends NumberGenerator[Int] {
  // Cache the fixed-point representation of p to avoid having to
  // recalculate it for each value generated.  Only calculate it
  // if and when p changes.
  @transient private var pBits: BitString = _
  @transient private var lastP: Double = _


  /**
   * Generate the next binomial value from the current values of
   * {@literal n} and {@literal p}.  The algorithm used is from
   * The Art of Computer Programming Volume 2 (Seminumerical Algorithms)
   * by Donald Knuth (page 589 in the Third Edition) where it is
   * credited to J.H. Ahrens. 
   */
  def nextValue: Int = {
    // Regenerate the fixed point representation of p if it has changed.
    val newP = p.nextValue
    if (pBits == null || newP != lastP) {
      lastP = newP
      pBits = BinaryUtils.convertDoubleToFixedPointBits(newP)
    }

    var totalSuccesses = 0
    var trials = n.nextValue
    var pIndex = pBits.length - 1
    while (trials > 0 && pIndex >= 0) {
      val successes = binomialWithEvenProbability(trials)
      trials -= successes
      if (pBits.getBit(pIndex)) {
        totalSuccesses += successes
      }
      pIndex -= 1
    }

    totalSuccesses
  }


  /**
   * Generating binomial values when {@literal p = 0.5} is straightforward.
   * It simply a case of generating {@literal n} random bits and
   * counting how many are 1s.
   * @param n The number of trials.
   * @return The number of successful outcomes from {@literal n} trials.
   */
  private def binomialWithEvenProbability(n: Int): Int = {
    val bits = BitString(n, rng)
    bits.countSetBits
  }
}

object BinomialGenerator {
  def apply(n: NumberGenerator[Int], p: NumberGenerator[Double], rng: Random) = new BinomialGenerator(n, p, rng)
  
  /**
   * Creates a generator of binomially-distributed values from a distribution
   * with the specified parameters.
   * @param n The number of trials (and therefore the maximum possible value returned
   * by this sequence).
   * @param p The probability (between 0 and 1) of success in any one trial.
   * @param rng The source of randomness used to generate the binomial values.
   */
  def apply(n: Int, p: Double, rng: Random) = {
    if (n <= 0) {
      throw new IllegalArgumentException("n must be a positive integer.")
    }
    if (p <= 0 || p >= 1) {
      throw new IllegalArgumentException("p must be between 0 and 1.")
    }
    new BinomialGenerator(new ConstantGenerator[Int](n), new ConstantGenerator[Double](p), rng)
  }
}
