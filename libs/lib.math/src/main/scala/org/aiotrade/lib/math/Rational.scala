package org.aiotrade.lib.math

import java.math.BigDecimal
import java.math.BigInteger

final class Rational private[Rational] (val numerator: Long, val denominator: Long) extends Number with Ordered[Rational] {
  def add(value: Rational): Rational = {
    if (denominator == value.denominator) {
      Rational(numerator + value.numerator, denominator)
    } else {
      Rational(numerator * value.denominator + value.numerator * denominator, denominator * value.denominator)
    }
  }

  def subtract(value: Rational): Rational = {
    if (denominator == value.denominator) {
      Rational(numerator - value.numerator, denominator)
    } else {
      Rational(numerator * value.denominator - value.numerator * denominator, denominator * value.denominator)
    }
  }

  def multiply(value: Rational): Rational = {
    Rational(numerator * value.numerator, denominator * value.denominator)
  }

  def divide(value: Rational): Rational = {
    Rational(numerator * value.denominator, denominator * value.numerator)
  }
  
  def intValue: Int = longValue.toInt
  def longValue: Long = doubleValue.toLong
  def floatValue: Float = doubleValue.toFloat
  def doubleValue: Double = numerator.toDouble / denominator.toDouble

  override
  def equals(o: Any) = o match {
    case that: Rational => denominator == that.denominator && numerator == that.numerator
    case that: AnyRef => this eq that
    case _ => false
  }

  override
  def hashCode: Int = {
    val a = (numerator ^ (numerator >>> 32)).toInt
    31 * a +  (denominator ^ (denominator >>> 32)).toInt
  }

  override
  def toString = {
    val sb = new StringBuilder()
    sb.append(numerator)
    if (denominator != 1) {
      sb.append('/')
      sb.append(denominator)
    }
    sb.toString
  }

  def compare(other: Rational): Int = {
    if (denominator == other.denominator) {
      numerator.compare(other.numerator)
    } else {
      val adjustedNumerator = numerator * other.denominator
      val otherAdjustedNumerator = other.numerator * denominator
      adjustedNumerator.compare(otherAdjustedNumerator)
    }
  }
}

object Rational {    
  def apply(numerator: Long, denominator: Long) = {
    if (denominator < 1) {
      throw new IllegalArgumentException("Denominator must be non-zero and positive.")
    }
  
    val gcd = greatestCommonDivisor(numerator, denominator)
    new Rational(numerator / gcd, denominator / gcd)
  }
    
  /**
   * Creates a rational value equivalent to the specified decimal value.
   * @param value The value of this rational as a fractional decimal.
   * @throws ArithmeticException If the BigDecimal value is too large to be
   * represented as a Rational.
   */
  def apply(value: BigDecimal): Rational = {
    val trimmedValue = value.stripTrailingZeros
    val denominator = BigInteger.TEN.pow(trimmedValue.scale)
    val numerator = trimmedValue.unscaledValue
    val gcd = numerator.gcd(denominator)
    apply(numerator.divide(gcd).longValue, denominator.divide(gcd).longValue)
  }
  
  /**
   * Creates a rational value equivalent to the specified integer value.
   * @param value The value of this rational as an integer.
   */
  def apply(value: Long): Rational = {
    apply(value, 1)
  }
  
  val ZERO = Rational(0)
  val QUARTER = Rational(1, 4)
  val THIRD = Rational(1, 3)
  val HALF = Rational(1, 2)
  val TWO_THIRDS = Rational(2, 3)
  val THREE_QUARTERS = Rational(3, 4)
  val ONE = Rational(1)
    
  def greatestCommonDivisor(_a: Long, _b: Long): Long = {
    var a = math.abs(_a)
    var b = math.abs(_b)
    while (b != 0) {
      val temp = b
      b = a % b
      a = temp
    }
    a
  }
}

