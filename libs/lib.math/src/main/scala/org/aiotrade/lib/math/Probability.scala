package org.aiotrade.lib.math

import java.util.Random

final class Probability private (private val probability: Double) extends Number with Ordered[Probability] {

  /**
   * Generates an event according the probability value {@literal p}.
   * @param rng A source of randomness for generating events.
   * @return True with a probability of {@literal p}, false with a probability of
   * {@literal 1 - p}.
   */
  def nextEvent(rng: Random): Boolean = {
    // Don't bother generating an random value if the result is guaranteed.
    probability == 1 || rng.nextDouble < probability
  }

  /**
   * The complement of a probability p is 1 - p.  If p = 0.75, then the complement is 0.25.
   * @return The complement of this probability.
   */
  def getComplement: Probability = Probability(1 - probability)

  /**
   * Converting a fractional probability into an integer is not meaningful since
   * all useful information is discarded. For this reason, this method is over-ridden
   * to thrown an {@link ArithmeticException}, except when the probability is exactly
   * zero or one.
   * @throws ArithmeticException Unless the probability is exactly zero or one.
   * @return An integer probability.
   */
  def intValue: Int = {
    if (probability % 1 == 0) {
      probability.toInt
    } else {
      throw new ArithmeticException("Cannot convert probability to integer due to loss of precision.");
    }
  }
  def longValue: Long = doubleValue.toLong
  def floatValue: Float = doubleValue.toFloat
  def doubleValue = probability

  override
  def equals(o: Any) = o match {
    case that: Probability => that.probability == probability
    case _ => false
  }

  override
  def hashCode = {
    val a = if (probability == 0.0) 0L else java.lang.Double.doubleToLongBits(probability)
    (a ^ (a >>> 32)).toInt
  }

  def compare(other: Probability): Int = {
    this.probability.compare(other.probability)
  }

  override
  def toString = {
    probability.toString
  }
}

object Probability {
  def apply(probability: Double): Probability = {
    if (probability < 0 || probability > 1) {
      throw new IllegalArgumentException("Probability must be in the range 0..1 inclusive.")
    }
    new Probability(probability)
  }

  val ZERO = Probability(0)
  val EVENS = Probability(0.5d)
  val ONE = Probability(1)
}