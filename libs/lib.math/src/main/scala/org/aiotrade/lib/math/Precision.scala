package org.aiotrade.lib.math

import java.math.BigDecimal

/**
 * Utilities for comparing numbers.
 * 
 * Ported by Caoyuan Deng from Java version at org.apache.commons.math3.util
 */
object Precision {
  /**
   * Smallest positive number such that {@code 1 - EPSILON} is not
   * numerically equal to 1: {@value}.
   */
  val EPSILON: Double = math.pow(2, -53) //0x1.0p-53
  /**
   * Safe minimum, such that {@code 1 / SAFE_MIN} does not overflow.
   * In IEEE 754 arithmetic, this is also the smallest normalized
   * number 2<sup>-1022</sup>: {@value}.
   */
  val SAFE_MIN: Double = math.pow(2, -1022) //0x1.0p-1022
  /** Offset to order signed Double numbers lexicographically. */
  val SGN_MASK = 0x8000000000000000L
  /** Offset to order signed Double numbers lexicographically. */
  val SGN_MASK_FLOAT = 0x80000000

  /**
   * Compares two numbers given some amount of allowed error.
   *
   * @param x the first number
   * @param y the second number
   * @param eps the amount of error to allow when checking for equality
   * @return <ul><li>0 if  {@link #equals(Double, Double, Double) equals(x, y, eps)}</li>
   *       <li>&lt; 0 if !{@link #equals(Double, Double, Double) equals(x, y, eps)} &amp;&amp; x &lt; y</li>
   *       <li>> 0 if !{@link #equals(Double, Double, Double) equals(x, y, eps)} &amp;&amp; x > y</li></ul>
   */
  def compare(x: Double, y: Double, eps: Double): Int = {
    if (equals(x, y, eps)) {
      0
    } else if (x < y) {
      -1
    } else {
      1
    }
  }

  /**
   * Compares two numbers given some amount of allowed error.
   * Two Float numbers are considered equal if there are {@code (maxUlps - 1)}
   * (or fewer) floating point numbers between them, i.e. two adjacent floating
   * point numbers are considered equal.
   * Adapted from <a
   * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
   * Bruce Dawson</a>
   *
   * @param x first value
   * @param y second value
   * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
   * values between {@code x} and {@code y}.
   * @return <ul><li>0 if  {@link #equals(Double, Double, int) equals(x, y, maxUlps)}</li>
   *       <li>&lt; 0 if !{@link #equals(Double, Double, int) equals(x, y, maxUlps)} &amp;&amp; x &lt; y</li>
   *       <li>> 0 if !{@link #equals(Double, Double, int) equals(x, y, maxUlps)} &amp;&amp; x > y</li></ul>
   */
  def compare(x: Double, y: Double, maxUlps: Int): Int = {
    if (equals(x, y, maxUlps)) {
      0
    } else if (x < y) {
      -1
    } else {
      1
    }
  }

  /**
   * Returns true iff they are equal as defined by
   * {@link #equals(Float,Float,int) equals(x, y, 1)}.
   *
   * @param x first value
   * @param y second value
   * @return {@code true} if the values are equal.
   */
  def equals(x: Float, y: Float): Boolean = {
    equals(x, y, 1)
  }

  /**
   * Returns true if both arguments are NaN or neither is NaN and they are
   * equal as defined by {@link #equals(Float,Float) equals(x, y, 1)}.
   *
   * @param x first value
   * @param y second value
   * @return {@code true} if the values are equal or both are NaN.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Float, y: Float): Boolean = {
    (java.lang.Float.isNaN(x) && java.lang.Float.isNaN(y)) || equals(x, y, 1)
  }

  /**
   * Returns true if both arguments are equal or within the range of allowed
   * error (inclusive).
   *
   * @param x first value
   * @param y second value
   * @param eps the amount of absolute error to allow.
   * @return {@code true} if the values are equal or within range of each other.
   * @since 2.2
   */
  def equals(x: Float, y: Float, eps: Float): Boolean = {
    equals(x, y, 1) || math.abs(y - x) <= eps
  }

  /**
   * Returns true if both arguments are NaN or are equal or within the range
   * of allowed error (inclusive).
   *
   * @param x first value
   * @param y second value
   * @param eps the amount of absolute error to allow.
   * @return {@code true} if the values are equal or within range of each other,
   * or both are NaN.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Float, y: Float, eps: Float): Boolean = {
    equalsIncludingNaN(x, y) || (math.abs(y - x) <= eps)
  }

  /**
   * Returns true if both arguments are equal or within the range of allowed
   * error (inclusive).
   * Two Float numbers are considered equal if there are {@code (maxUlps - 1)}
   * (or fewer) floating point numbers between them, i.e. two adjacent floating
   * point numbers are considered equal.
   * Adapted from <a
   * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
   * Bruce Dawson</a>
   *
   * @param x first value
   * @param y second value
   * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
   * values between {@code x} and {@code y}.
   * @return {@code true} if there are fewer than {@code maxUlps} floating
   * point values between {@code x} and {@code y}.
   * @since 2.2
   */
  def equals(x: Float, y: Float, maxUlps: Int): Boolean = {
    var xInt = java.lang.Float.floatToIntBits(x)
    var yInt = java.lang.Float.floatToIntBits(y)

    // Make lexicographically ordered as a two's-complement integer.
    if (xInt < 0) {
      xInt = SGN_MASK_FLOAT - xInt
    }
    if (yInt < 0) {
      yInt = SGN_MASK_FLOAT - yInt
    }

    val isEqual = math.abs(xInt - yInt) <= maxUlps

    isEqual && !java.lang.Float.isNaN(x) && !java.lang.Float.isNaN(y)
  }

  /**
   * Returns true if both arguments are NaN or if they are equal as defined
   * by {@link #equals(Float,Float,int) equals(x, y, maxUlps)}.
   *
   * @param x first value
   * @param y second value
   * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
   * values between {@code x} and {@code y}.
   * @return {@code true} if both arguments are NaN or if there are less than
   * {@code maxUlps} floating point values between {@code x} and {@code y}.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Float, y: Float, maxUlps: Int): Boolean = {
    (java.lang.Float.isNaN(x) && java.lang.Float.isNaN(y)) || equals(x, y, maxUlps)
  }

  /**
   * Returns true iff they are equal as defined by
   * {@link #equals(Double,Double,int) equals(x, y, 1)}.
   *
   * @param x first value
   * @param y second value
   * @return {@code true} if the values are equal.
   */
  def equals(x: Double, y: Double) {
    equals(x, y, 1)
  }

  /**
   * Returns true if both arguments are NaN or neither is NaN and they are
   * equal as defined by {@link #equals(Double,Double) equals(x, y, 1)}.
   *
   * @param x first value
   * @param y second value
   * @return {@code true} if the values are equal or both are NaN.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Double, y: Double): Boolean = {
    (java.lang.Double.isNaN(x) && java.lang.Double.isNaN(y)) || equals(x, y, 1)
  }

  /**
   * Returns {@code true} if there is no Double value strictly between the
   * arguments or the difference between them is within the range of allowed
   * error (inclusive).
   *
   * @param x First value.
   * @param y Second value.
   * @param eps Amount of allowed absolute error.
   * @return {@code true} if the values are two adjacent floating point
   * numbers or they are within range of each other.
   */
  def equals(x: Double, y: Double, eps: Double): Boolean = {
    equals(x, y, 1) || math.abs(y - x) <= eps
  }

  /**
   * Returns true if both arguments are NaN or are equal or within the range
   * of allowed error (inclusive).
   *
   * @param x first value
   * @param y second value
   * @param eps the amount of absolute error to allow.
   * @return {@code true} if the values are equal or within range of each other,
   * or both are NaN.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Double, y: Double, eps: Double): Boolean = {
    equalsIncludingNaN(x, y) || (math.abs(y - x) <= eps)
  }

  /**
   * Returns true if both arguments are equal or within the range of allowed
   * error (inclusive).
   * Two Float numbers are considered equal if there are {@code (maxUlps - 1)}
   * (or fewer) floating point numbers between them, i.e. two adjacent floating
   * point numbers are considered equal.
   * Adapted from <a
   * href="http://www.cygnus-software.com/papers/comparingfloats/comparingfloats.htm">
   * Bruce Dawson</a>
   *
   * @param x first value
   * @param y second value
   * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
   * values between {@code x} and {@code y}.
   * @return {@code true} if there are fewer than {@code maxUlps} floating
   * point values between {@code x} and {@code y}.
   */
  def equals(x: Double, y: Double, maxUlps: Int): Boolean = {
    var xInt = java.lang.Double.doubleToLongBits(x)
    var yInt = java.lang.Double.doubleToLongBits(y)

    // Make lexicographically ordered as a two's-complement integer.
    if (xInt < 0) {
      xInt = SGN_MASK - xInt
    }
    if (yInt < 0) {
      yInt = SGN_MASK - yInt
    }

    val isEqual = math.abs(xInt - yInt) <= maxUlps

    isEqual && !java.lang.Double.isNaN(x) && !java.lang.Double.isNaN(y)
  }

  /**
   * Returns true if both arguments are NaN or if they are equal as defined
   * by {@link #equals(Double,Double,int) equals(x, y, maxUlps)}.
   *
   * @param x first value
   * @param y second value
   * @param maxUlps {@code (maxUlps - 1)} is the number of floating point
   * values between {@code x} and {@code y}.
   * @return {@code true} if both arguments are NaN or if there are less than
   * {@code maxUlps} floating point values between {@code x} and {@code y}.
   * @since 2.2
   */
  def equalsIncludingNaN(x: Double, y: Double, maxUlps: Int): Boolean = {
    (java.lang.Double.isNaN(x) && java.lang.Double.isNaN(y)) || equals(x, y, maxUlps)
  }

  /**
   * Rounds the given value to the specified number of decimal places.
   * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
   *
   * @param x Value to round.
   * @param scale Number of digits to the right of the decimal point.
   * @return the rounded value.
   * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  def round(x: Double, scale: Int): Double = {
    round(x, scale, BigDecimal.ROUND_HALF_UP)
  }

  /**
   * Rounds the given value to the specified number of decimal places.
   * The value is rounded using the given method which is any method defined
   * in {@link BigDecimal}.
   * If {@code x} is infinite or {@code NaN}, then the value of {@code x} is
   * returned unchanged, regardless of the other parameters.
   *
   * @param x Value to round.
   * @param scale Number of digits to the right of the decimal point.
   * @param roundingMethod Rounding method as defined in {@link BigDecimal}.
   * @return the rounded value.
   * @throws ArithmeticException if {@code roundingMethod == ROUND_UNNECESSARY}
   * and the specified scaling operation would require rounding.
   * @throws IllegalArgumentException if {@code roundingMethod} does not
   * represent a valid rounding mode.
   * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  def round(x: Double, scale: Int, roundingMethod: Int): Double = {
    try {
      (new BigDecimal(java.lang.Double.toString(x)).setScale(scale, roundingMethod)).doubleValue
    } catch {
      case ex: NumberFormatException =>
        if (java.lang.Double.isInfinite(x)) {
          x
        } else {
          Double.NaN
        }
    }
  }

  /**
   * Rounds the given value to the specified number of decimal places.
   * The value is rounded using the {@link BigDecimal#ROUND_HALF_UP} method.
   *
   * @param x Value to round.
   * @param scale Number of digits to the right of the decimal point.
   * @return the rounded value.
   * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  def round(x: Float, scale: Int): Float = {
    round(x, scale, BigDecimal.ROUND_HALF_UP)
  }

  /**
   * Rounds the given value to the specified number of decimal places.
   * The value is rounded using the given method which is any method defined
   * in {@link BigDecimal}.
   *
   * @param x Value to round.
   * @param scale Number of digits to the right of the decimal point.
   * @param roundingMethod Rounding method as defined in {@link BigDecimal}.
   * @return the rounded value.
   * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  def round(x: Float, scale: Int, roundingMethod: Int): Float = {
    val sign = java.lang.Math.copySign(1f, x)
    val factor = math.pow(10.0f, scale).toFloat * sign
    roundUnscaled(x * factor, sign, roundingMethod).toFloat / factor
  }

  /**
   * Rounds the given non-negative value to the "nearest" integer. Nearest is
   * determined by the rounding method specified. Rounding methods are defined
   * in {@link BigDecimal}.
   *
   * @param unscaled Value to round.
   * @param sign Sign of the original, scaled value.
   * @param roundingMethod Rounding method, as defined in {@link BigDecimal}.
   * @return the rounded value.
   * @throws MathIllegalArgumentException if {@code roundingMethod} is not a valid rounding method.
   * @since 1.1 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  private def roundUnscaled(unscaled: Double, sign: Double, roundingMethod: Int): Double = {
    val roundedUnscaled = roundingMethod match {
      case BigDecimal.ROUND_CEILING =>
        if (sign == -1) {
          math.floor(java.lang.Math.nextAfter(unscaled, Double.NegativeInfinity))
        } else {
          math.ceil(java.lang.Math.nextAfter(unscaled, Double.PositiveInfinity))
        }
      case BigDecimal.ROUND_DOWN =>
        math.floor(java.lang.Math.nextAfter(unscaled, Double.NegativeInfinity))
      case BigDecimal.ROUND_FLOOR =>
        if (sign == -1) {
          math.ceil(java.lang.Math.nextAfter(unscaled, Double.PositiveInfinity))
        } else {
          math.floor(java.lang.Math.nextAfter(unscaled, Double.NegativeInfinity))
        }
      case BigDecimal.ROUND_HALF_DOWN => 
        val unscaledTmp = java.lang.Math.nextAfter(unscaled, Double.NegativeInfinity)
        val fraction = unscaledTmp - math.floor(unscaledTmp)
        if (fraction > 0.5) {
          math.ceil(unscaledTmp)
        } else {
          math.floor(unscaledTmp)
        }
      case BigDecimal.ROUND_HALF_EVEN =>
        val fraction = unscaled - math.floor(unscaled)
        if (fraction > 0.5) {
          math.ceil(unscaled)
        } else if (fraction < 0.5) {
          math.floor(unscaled)
        } else {
          // The following equality test is intentional and needed for rounding purposes
          if (math.floor(unscaled) / 2.0 == math.floor(math.floor(unscaled) / 2.0)) { // even
            math.floor(unscaled)
          } else { // odd
            math.ceil(unscaled)
          }
        }
      case BigDecimal.ROUND_HALF_UP =>
        val unscaledTmp = java.lang.Math.nextAfter(unscaled, Double.PositiveInfinity)
        val fraction = unscaledTmp - math.floor(unscaledTmp)
        if (fraction >= 0.5) {
          math.ceil(unscaled)
        } else {
          math.floor(unscaled)
        }
      case BigDecimal.ROUND_UNNECESSARY =>
        if (unscaled != math.floor(unscaled)) {
          throw new ArithmeticException()
        } else unscaled
      case BigDecimal.ROUND_UP =>
        math.ceil(java.lang.Math.nextAfter(unscaled,  Double.PositiveInfinity))
      case _ =>
        throw new IllegalArgumentException("invalid rounding method {0}, valid methods: {1} ({2}), {3} ({4}), {5} ({6}), {7} ({8}), {9} ({10}), {11} ({12}), {13} ({14}), {15} ({16})".format(
            roundingMethod,
            "ROUND_CEILING", BigDecimal.ROUND_CEILING,
            "ROUND_DOWN", BigDecimal.ROUND_DOWN,
            "ROUND_FLOOR", BigDecimal.ROUND_FLOOR,
            "ROUND_HALF_DOWN", BigDecimal.ROUND_HALF_DOWN,
            "ROUND_HALF_EVEN", BigDecimal.ROUND_HALF_EVEN,
            "ROUND_HALF_UP", BigDecimal.ROUND_HALF_UP,
            "ROUND_UNNECESSARY", BigDecimal.ROUND_UNNECESSARY,
            "ROUND_UP", BigDecimal.ROUND_UP))
    }
    roundedUnscaled
  }


  /**
   * Computes a number {@code delta} close to {@code originalDelta} with
   * the property that <pre><code>
   *   x + delta - x
   * </code></pre>
   * is exactly machine-representable.
   * This is useful when computing numerical derivatives, in order to reduce
   * roundoff errors.
   *
   * @param x Value.
   * @param originalDelta Offset value.
   * @return a number {@code delta} so that {@code x + delta} and {@code x}
   * differ by a representable floating number.
   */
  def representableDelta(x: Double, originalDelta: Double): Double = {
    x + originalDelta - x
  }

  // --- simple test
  def main(args: Array[String]) {
    println("EPSILON = " + Precision.EPSILON)
    println("SAFE_MIN = " + Precision.SAFE_MIN)
    println("Math.hypot(8.5, 11.19) = " + java.lang.Math.hypot(8.5, 11.19))
  }
  
}