package org.aiotrade.lib.math.fastmath

import org.aiotrade.lib.math.Precision

/**
 * Faster, more accurate, portable alternative to {@link Math} and
 * {@link StrictMath} for large scale computation.
 * <p>
 * FastMath is a drop-in replacement for both Math and StrictMath. This
 * means that for any method in Math (say {@code Math.sin(x)} or
 * {@code Math.cbrt(y)}), user can directly change the class and use the
 * methods as is (using {@code FastMath.sin(x)} or {@code FastMath.cbrt(y)}
 * in the previous example).
 * </p>
 * <p>
 * FastMath speed is achieved by relying heavily on optimizing compilers
 * to native code present in many JVMs today and use of large tables.
 * The larger tables are lazily initialised on first use, so that the setup
 * time does not penalise methods that don't need them.
 * </p>
 * <p>
 * Note that FastMath is
 * extensively used inside Apache Commons Math, so by calling some algorithms,
 * the overhead when the the tables need to be intialised will occur
 * regardless of the end-user calling FastMath methods directly or not.
 * Performance figures for a specific JVM and hardware can be evaluated by
 * running the FastMathTestPerformance tests in the test directory of the source
 * distribution.
 * </p>
 * <p>
 * FastMath accuracy should be mostly independent of the JVM as it relies only
 * on IEEE-754 basic operations and on embedded tables. Almost all operations
 * are accurate to about 0.5 ulp throughout the domain range. This statement,
 * of course is only a rough global observed behavior, it is <em>not</em> a
 * guarantee for <em>every</em> Double numbers input (see William Kahan's <a
 * href="http://en.wikipedia.org/wiki/Rounding#The_table-maker.27s_dilemma">Table
 * Maker's Dilemma</a>).
 * </p>
 * <p>
 * FastMath additionally implements the following methods not found in Math/StrictMath:
 * <ul>
 * <li>{@link #asinh(Double)}</li>
 * <li>{@link #acosh(Double)}</li>
 * <li>{@link #atanh(Double)}</li>
 * </ul>
 * The following methods are found in Math/StrictMath since 1.6 only, they are provided
 * by FastMath even in 1.5 Java virtual machines
 * <ul>
 * <li>{@link #copySign(Double, Double)}</li>
 * <li>{@link #getExponent(Double)}</li>
 * <li>{@link #nextAfter(Double,Double)}</li>
 * <li>{@link #nextUp(Double)}</li>
 * <li>{@link #scalb(Double, int)}</li>
 * <li>{@link #copySign(Float, Float)}</li>
 * <li>{@link #getExponent(Float)}</li>
 * <li>{@link #nextAfter(Float,Double)}</li>
 * <li>{@link #nextUp(Float)}</li>
 * <li>{@link #scalb(Float, int)}</li>
 * </ul>
 * </p>
 * 
 * Ported by Caoyuan Deng from Java version at org.apache.commons.math3.util
 */
object FastMath {

  /** Archimede's constant PI, ratio of circle circumference to diameter. */
  val PI = 105414357.0 / 33554432.0 + 1.984187159361080883e-9

  /** Napier's constant e, base of the natural logarithm. */
  val E = 2850325.0 / 1048576.0 + 8.254840070411028747e-8

  /** Index of exp(0) in the array of integer exponentials. */
  protected[math] val EXP_INT_TABLE_MAX_INDEX = 750
  /** Length of the array of integer exponentials. */
  protected[math] val EXP_INT_TABLE_LEN = EXP_INT_TABLE_MAX_INDEX * 2
  /** Logarithm table length. */
  protected[math] val LN_MANT_LEN = 1024
  /** Exponential fractions table length. */
  protected[math] val EXP_FRAC_TABLE_LEN = 1025 // 0, 1/1024, ... 1024/1024

  /** Indicator for tables initialization.
   * <p>
   * This compile-time constant should be set to true only if one explicitly
   * wants to compute the tables at class loading time instead of using the
   * already computed ones provided as literal arrays below.
   * </p>
   */
  private val RECOMPUTE_TABLES_AT_RUNTIME = false
  /** Indicator for loading big tables from "resource" files. */
  private val LOAD_RESOURCES = false

  /** log(2) (high bits). */
  private val LN_2_A = 0.693147063255310059

  /** log(2) (low bits). */
  private val LN_2_B = 1.17304635250823482e-7

  /** Coefficients for log, when input 0.99 < x < 1.01. */
  private val LN_QUICK_COEF = Array(
    Array(1.0, 5.669184079525E-24),
    Array(-0.25, -0.25),
    Array(0.3333333134651184, 1.986821492305628E-8),
    Array(-0.25, -6.663542893624021E-14),
    Array(0.19999998807907104, 1.1921056801463227E-8),
    Array(-0.1666666567325592, -7.800414592973399E-9),
    Array(0.1428571343421936, 5.650007086920087E-9),
    Array(-0.12502530217170715, -7.44321345601866E-11),
    Array(0.11113807559013367, 9.219544613762692E-9)
  )

  /** Coefficients for log in the range of 1.0 < x < 1.0 + 2^-10. */
  private val LN_HI_PREC_COEF = Array(
    Array(1.0, -6.032174644509064E-23),
    Array(-0.25, -0.25),
    Array(0.3333333134651184, 1.9868161777724352E-8),
    Array(-0.2499999701976776, -2.957007209750105E-8),
    Array(0.19999954104423523, 1.5830993332061267E-10),
    Array(-0.16624879837036133, -2.6033824355191673E-8)
  )

  /** Sine, Cosine, Tangent tables are for 0, 1/8, 2/8, ... 13/8 = PI/2 approx. */
  private val SINE_TABLE_LEN = 14

  /** Sine table (high bits). */
  private val SINE_TABLE_A = Array(
    +0.0d,
    +0.1246747374534607d,
    +0.24740394949913025d,
    +0.366272509098053d,
    +0.4794255495071411d,
    +0.5850973129272461d,
    +0.6816387176513672d,
    +0.7675435543060303d,
    +0.8414709568023682d,
    +0.902267575263977d,
    +0.9489846229553223d,
    +0.9808930158615112d,
    +0.9974949359893799d,
    +0.9985313415527344d
  )

  /** Sine table (low bits). */
  private val SINE_TABLE_B = Array(
    +0.0d,
    -4.068233003401932E-9d,
    +9.755392680573412E-9d,
    +1.9987994582857286E-8d,
    -1.0902938113007961E-8d,
    -3.9986783938944604E-8d,
    +4.23719669792332E-8d,
    -5.207000323380292E-8d,
    +2.800552834259E-8d,
    +1.883511811213715E-8d,
    -3.5997360512765566E-9d,
    +4.116164446561962E-8d,
    +5.0614674548127384E-8d,
    -1.0129027912496858E-9d
  )

  /** Cosine table (high bits). */
  private val COSINE_TABLE_A = Array(
    +1.0d,
    +0.9921976327896118d,
    +0.9689123630523682d,
    +0.9305076599121094d,
    +0.8775825500488281d,
    +0.8109631538391113d,
    +0.7316888570785522d,
    +0.6409968137741089d,
    +0.5403022766113281d,
    +0.4311765432357788d,
    +0.3153223395347595d,
    +0.19454771280288696d,
    +0.07073719799518585d,
    -0.05417713522911072d
  )

  /** Cosine table (low bits). */
  private val COSINE_TABLE_B = Array(
    +0.0d,
    +3.4439717236742845E-8d,
    +5.865827662008209E-8d,
    -3.7999795083850525E-8d,
    +1.184154459111628E-8d,
    -3.43338934259355E-8d,
    +1.1795268640216787E-8d,
    +4.438921624363781E-8d,
    +2.925681159240093E-8d,
    -2.6437112632041807E-8d,
    +2.2860509143963117E-8d,
    -4.813899778443457E-9d,
    +3.6725170580355583E-9d,
    +2.0217439756338078E-10d
  )


  /** Tangent table, used by atan() (high bits). */
  private val TANGENT_TABLE_A = Array(
    +0.0d,
    +0.1256551444530487d,
    +0.25534194707870483d,
    +0.3936265707015991d,
    +0.5463024377822876d,
    +0.7214844226837158d,
    +0.9315965175628662d,
    +1.1974215507507324d,
    +1.5574076175689697d,
    +2.092571258544922d,
    +3.0095696449279785d,
    +5.041914939880371d,
    +14.101419448852539d,
    -18.430862426757812d
  )

  /** Tangent table, used by atan() (low bits). */
  private val TANGENT_TABLE_B = Array(
    +0.0d,
    -7.877917738262007E-9d,
    -2.5857668567479893E-8d,
    +5.2240336371356666E-9d,
    +5.206150291559893E-8d,
    +1.8307188599677033E-8d,
    -5.7618793749770706E-8d,
    +7.848361555046424E-8d,
    +1.0708593250394448E-7d,
    +1.7827257129423813E-8d,
    +2.893485277253286E-8d,
    +3.1660099222737955E-7d,
    +4.983191803254889E-7d,
    -3.356118100840571E-7d
  )

  /** Bits of 1/(2*pi), need for reducePayneHanek(). */
  private val RECIP_2PI = Array(
    (0x28be60dbL << 32) | 0x9391054aL,
    (0x7f09d5f4L << 32) | 0x7d4d3770L,
    (0x36d8a566L << 32) | 0x4f10e410L,
    (0x7f9458eaL << 32) | 0xf7aef158L,
    (0x6dc91b8eL << 32) | 0x909374b8L,
    (0x01924bbaL << 32) | 0x82746487L,
    (0x3f877ac7L << 32) | 0x2c4a69cfL,
    (0xba208d7dL << 32) | 0x4baed121L,
    (0x3a671c09L << 32) | 0xad17df90L,
    (0x4e64758eL << 32) | 0x60d4ce7dL,
    (0x272117e2L << 32) | 0xef7e4a0eL,
    (0xc7fe25ffL << 32) | 0xf7816603L,
    (0xfbcbc462L << 32) | 0xd6829b47L,
    (0xdb4d9fb3L << 32) | 0xc9f2c26dL,
    (0xd3d18fd9L << 32) | 0xa797fa8bL,
    (0x5d49eeb1L << 32) | 0xfaf97c5eL,
    (0xcf41ce7dL << 32) | 0xe294a4baL,
    0x9afed7ecL << 32  )

  /** Bits of pi/4, need for reducePayneHanek(). */
  private val PI_O_4_BITS = Array(
    (0xc90fdaa2L << 32) | 0x2168c234L,
    (0xc4c6628bL << 32) | 0x80dc1cd1L )

  /** Eighths.
   * This is used by sinQ, because its faster to do a table lookup than
   * a multiply in this time-critical routine
   */
  private val EIGHTHS = Array(0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.5, 1.625)

  /** Table of 2^((n+2)/3) */
  private val CBRTTWO = Array( 0.6299605249474366,
                              0.7937005259840998,
                              1.0,
                              1.2599210498948732,
                              1.5874010519681994 )

  /*
   *  There are 52 bits in the mantissa of a java.lang.Double.
   *  For additional precision, the code splits Double numbers into two parts,
   *  by clearing the low order 30 bits if possible, and then performs the arithmetic
   *  on each half separately.
   */

  /**
   * 0x40000000 - used to split a Double into two parts, both with the low order bits cleared.
   * Equivalent to 2^30.
   */
  private val HEX_40000000 = 0x40000000L // 1073741824L

  /** Mask used to clear low order 30 bits */
  private val MASK_30BITS = -1L - (HEX_40000000 -1) // 0xFFFFFFFFC0000000L

  /** 2^52 - Double numbers this large must be integral (no fraction) or NaN or Infinite */
  private val TWO_POWER_52 = 4503599627370496.0

  /** Constant: {@value}. */
  private val F_1_3 = 1d / 3d
  /** Constant: {@value}. */
  private val F_1_5 = 1d / 5d
  /** Constant: {@value}. */
  private val F_1_7 = 1d / 7d
  /** Constant: {@value}. */
  private val F_1_9 = 1d / 9d
  /** Constant: {@value}. */
  private val F_1_11 = 1d / 11d
  /** Constant: {@value}. */
  private val F_1_13 = 1d / 13d
  /** Constant: {@value}. */
  private val F_1_15 = 1d / 15d
  /** Constant: {@value}. */
  private val F_1_17 = 1d / 17d
  /** Constant: {@value}. */
  private val F_3_4 = 3d / 4d
  /** Constant: {@value}. */
  private val F_15_16 = 15d / 16d
  /** Constant: {@value}. */
  private val F_13_14 = 13d / 14d
  /** Constant: {@value}. */
  private val F_11_12 = 11d / 12d
  /** Constant: {@value}. */
  private val F_9_10 = 9d / 10d
  /** Constant: {@value}. */
  private val F_7_8 = 7d / 8d
  /** Constant: {@value}. */
  private val F_5_6 = 5d / 6d
  /** Constant: {@value}. */
  private val F_1_2 = 1d / 2d
  /** Constant: {@value}. */
  private val F_1_4 = 1d / 4d

  // Generic helper methods

  /**
   * Get the high order bits from the mantissa.
   * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
   *
   * @param d the value to split
   * @return the high order part of the mantissa
   */
  private def doubleHighPart(d: Double): Double = {
    if (d > -Precision.SAFE_MIN && d < Precision.SAFE_MIN){
      return d // These are un-normalised - don't try to convert
    }
    var xl = java.lang.Double.doubleToLongBits(d)
    xl = xl & MASK_30BITS // Drop low order bits
    java.lang.Double.longBitsToDouble(xl)
  }

  /** Compute the square root of a number.
   * <p><b>Note:</b> this implementation currently delegates to {@link Math#sqrt}
   * @param a number on which evaluation is done
   * @return square root of a
   */
  def sqrt(a: Double): Double = {
    math.sqrt(a)
  }

  /** Compute the hyperbolic cosine of a number.
   * @param x number on which evaluation is done
   * @return hyperbolic cosine of x
   */
  def cosh(_x: Double): Double = {
    var x = _x
    if (x != x) {
      return x
    }
    
    // cosh[z] = (exp(z) + exp(-z))/2

    // for numbers with magnitude 20 or so,
    // exp(-z) can be ignored in comparison with exp(z)

    if (x > 20.0) {
      return exp(x)/2.0
    }

    if (x < -20) {
      return exp(-x)/2.0
    }

    val hiPrec = new Array[Double](2)
    if (x < 0.0) {
      x = -x
    }
    exp(x, 0.0, hiPrec)

    var ya = hiPrec(0) + hiPrec(1)
    var yb = -(ya - hiPrec(0) - hiPrec(1))

    var temp = ya * HEX_40000000
    val yaa = ya + temp - temp
    val yab = ya - yaa

    // recip = 1/y
    val recip = 1.0/ya
    temp = recip * HEX_40000000
    var recipa = recip + temp - temp
    var recipb = recip - recipa

    // Correct for rounding in division
    recipb += (1.0 - yaa*recipa - yaa*recipb - yab*recipa - yab*recipb) * recip
    // Account for yb
    recipb += -yb * recip * recip

    // y = y + 1/y
    temp = ya + recipa
    yb += -(temp - ya - recipa)
    ya = temp
    temp = ya + recipb
    yb += -(temp - ya - recipb)
    ya = temp

    var result = ya + yb
    result *= 0.5
    result
  }

  /** Compute the hyperbolic sine of a number.
   * @param x number on which evaluation is done
   * @return hyperbolic sine of x
   */
  def sinh(_x: Double): Double = {
    var x = _x
    if (x != x) {
      return x
    }

    // sinh[z] = (exp(z) - exp(-z) / 2

    // for values of z larger than about 20,
    // exp(-z) can be ignored in comparison with exp(z)

    if (x > 20.0) {
      return exp(x)/2.0
    }

    if (_x < -20) {
      return -exp(-x)/2.0
    }

    if (x == 0) {
      return x
    }

    var negate = false
    if (x < 0.0) {
      x = -x
      negate = true
    }

    var result = 0.0

    if (x > 0.25) {
      val hiPrec = new Array[Double](2)
      exp(x, 0.0, hiPrec)

      var ya = hiPrec(0) + hiPrec(1)
      var yb = -(ya - hiPrec(0) - hiPrec(1))

      var temp = ya * HEX_40000000
      val yaa = ya + temp - temp
      val yab = ya - yaa

      // recip = 1/y
      val recip = 1.0/ya
      temp = recip * HEX_40000000
      var recipa = recip + temp - temp
      var recipb = recip - recipa

      // Correct for rounding in division
      recipb += (1.0 - yaa*recipa - yaa*recipb - yab*recipa - yab*recipb) * recip
      // Account for yb
      recipb += -yb * recip * recip

      recipa = -recipa
      recipb = -recipb

      // y = y + 1/y
      temp = ya + recipa
      yb += -(temp - ya - recipa)
      ya = temp
      temp = ya + recipb
      yb += -(temp - ya - recipb)
      ya = temp

      result = ya + yb
      result *= 0.5
    } else {
      val hiPrec = new Array[Double](2)
      expm1(x, hiPrec)

      var ya = hiPrec(0) + hiPrec(1)
      var yb = -(ya - hiPrec(0) - hiPrec(1))

      /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
      val denom = 1.0 + ya
      val denomr = 1.0 / denom
      val denomb = -(denom - 1.0 - ya) + yb
      val ratio = ya * denomr
      var temp = ratio * HEX_40000000
      var ra = ratio + temp - temp
      var rb = ratio - ra

      temp = denom * HEX_40000000
      val za = denom + temp - temp
      val zb = denom - za

      rb += (ya - za*ra - za*rb - zb*ra - zb*rb) * denomr

      // Adjust for yb
      rb += yb*denomr                        // numerator
      rb += -ya * denomb * denomr * denomr   // denominator

      // y = y - 1/y
      temp = ya + ra
      yb += -(temp - ya - ra)
      ya = temp
      temp = ya + rb
      yb += -(temp - ya - rb)
      ya = temp

      result = ya + yb
      result *= 0.5
    }

    if (negate) {
      result = -result
    }

    result
  }

  /** Compute the hyperbolic tangent of a number.
   * @param x number on which evaluation is done
   * @return hyperbolic tangent of x
   */
  def tanh(_x: Double): Double = {
    var x = _x
    if (x != x) {
      return x
    }

    // tanh[z] = sinh[z] / cosh[z]
    // = (exp(z) - exp(-z)) / (exp(z) + exp(-z))
    // = (exp(2x) - 1) / (exp(2x) + 1)

    // for magnitude > 20, sinh[z] == cosh[z] in Double precision

    if (x > 20.0) {
      return 1.0
    }

    if (x < -20) {
      return -1.0
    }

    if (x == 0) {
      return x
    }

    var negate = false

    if (x < 0.0) {
      x = -x
      negate = true
    }

    var result = 0.0
    if (x >= 0.5) {
      val hiPrec = new Array[Double](2)
      // tanh(x) = (exp(2x) - 1) / (exp(2x) + 1)
      exp(x*2.0, 0.0, hiPrec)

      val ya = hiPrec(0) + hiPrec(1)
      val yb = -(ya - hiPrec(0) - hiPrec(1))

      /* Numerator */
      var na = -1.0 + ya
      var nb = -(na + 1.0 - ya)
      var temp = na + yb
      nb += -(temp - na - yb)
      na = temp

      /* Denominator */
      var da = 1.0 + ya
      var db = -(da - 1.0 - ya)
      temp = da + yb
      db += -(temp - da - yb)
      da = temp

      temp = da * HEX_40000000
      val daa = da + temp - temp
      val dab = da - daa

      // ratio = na/da
      val ratio = na/da
      temp = ratio * HEX_40000000
      var ratioa = ratio + temp - temp
      var ratiob = ratio - ratioa

      // Correct for rounding in division
      ratiob += (na - daa*ratioa - daa*ratiob - dab*ratioa - dab*ratiob) / da

      // Account for nb
      ratiob += nb / da
      // Account for db
      ratiob += -db * na / da / da

      result = ratioa + ratiob
    }
    else {
      val hiPrec = new Array[Double](2)
      // tanh(x) = expm1(2x) / (expm1(2x) + 2)
      expm1(x*2.0, hiPrec)

      val ya = hiPrec(0) + hiPrec(1)
      val yb = -(ya - hiPrec(0) - hiPrec(1))

      /* Numerator */
      val na = ya
      val nb = yb

      /* Denominator */
      var da = 2.0 + ya
      var db = -(da - 2.0 - ya)
      var temp = da + yb
      db += -(temp - da - yb)
      da = temp

      temp = da * HEX_40000000
      val daa = da + temp - temp
      val dab = da - daa

      // ratio = na/da
      val ratio = na/da
      temp = ratio * HEX_40000000
      var ratioa = ratio + temp - temp
      var ratiob = ratio - ratioa

      // Correct for rounding in division
      ratiob += (na - daa*ratioa - daa*ratiob - dab*ratioa - dab*ratiob) / da

      // Account for nb
      ratiob += nb / da
      // Account for db
      ratiob += -db * na / da / da

      result = ratioa + ratiob
    }

    if (negate) {
      result = -result
    }

    result
  }

  /** Compute the inverse hyperbolic cosine of a number.
   * @param a number on which evaluation is done
   * @return inverse hyperbolic cosine of a
   */
  def acosh(a: Double): Double = {
    FastMath.log(a + FastMath.sqrt(a * a - 1))
  }

  /** Compute the inverse hyperbolic sine of a number.
   * @param a number on which evaluation is done
   * @return inverse hyperbolic sine of a
   */
  def asinh(_a: Double): Double = {
    var a = _a
    var negative = false
    if (a < 0) {
      negative = true
      a = -a
    }

    val absAsinh = if (a > 0.167) {
      FastMath.log(FastMath.sqrt(a * a + 1) + a)
    } else {
      val a2 = a * a
      if (a > 0.097) {
        a * (1 - a2 * (F_1_3 - a2 * (F_1_5 - a2 * (F_1_7 - a2 * (F_1_9 - a2 * (F_1_11 - a2 * (F_1_13 - a2 * (F_1_15 - a2 * F_1_17 * F_15_16) * F_13_14) * F_11_12) * F_9_10) * F_7_8) * F_5_6) * F_3_4) * F_1_2)
      } else if (a > 0.036) {
        a * (1 - a2 * (F_1_3 - a2 * (F_1_5 - a2 * (F_1_7 - a2 * (F_1_9 - a2 * (F_1_11 - a2 * F_1_13 * F_11_12) * F_9_10) * F_7_8) * F_5_6) * F_3_4) * F_1_2)
      } else if (a > 0.0036) {
        a * (1 - a2 * (F_1_3 - a2 * (F_1_5 - a2 * (F_1_7 - a2 * F_1_9 * F_7_8) * F_5_6) * F_3_4) * F_1_2)
      } else {
        a * (1 - a2 * (F_1_3 - a2 * F_1_5 * F_3_4) * F_1_2)
      }
    }

    if (negative) -absAsinh else absAsinh
  }

  /** Compute the inverse hyperbolic tangent of a number.
   * @param a number on which evaluation is done
   * @return inverse hyperbolic tangent of a
   */
  def atanh(_a: Double): Double = {
    var a = _a
    var negative = false
    if (a < 0) {
      negative = true
      a = -a
    }

    val absAtanh = if (a > 0.15) {
      0.5 * FastMath.log((1 + a) / (1 - a))
    } else {
      val a2 = a * a
      if (a > 0.087) {
        a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * (F_1_9 + a2 * (F_1_11 + a2 * (F_1_13 + a2 * (F_1_15 + a2 * F_1_17))))))))
      } else if (a > 0.031) {
        a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * (F_1_9 + a2 * (F_1_11 + a2 * F_1_13))))))
      } else if (a > 0.003) {
        a * (1 + a2 * (F_1_3 + a2 * (F_1_5 + a2 * (F_1_7 + a2 * F_1_9))))
      } else {
        a * (1 + a2 * (F_1_3 + a2 * F_1_5))
      }
    }

    if (negative) -absAtanh else absAtanh
  }

  /** Compute the signum of a number.
   * The signum is -1 for negative numbers, +1 for positive numbers and 0 otherwise
   * @param a number on which evaluation is done
   * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
   */
  def signum(a: Double): Double = {
    if (a < 0.0) -1.0 else (if (a > 0.0) 1.0 else a) // return +0.0/-0.0/NaN depending on a
  }

  /** Compute the signum of a number.
   * The signum is -1 for negative numbers, +1 for positive numbers and 0 otherwise
   * @param a number on which evaluation is done
   * @return -1.0, -0.0, +0.0, +1.0 or NaN depending on sign of a
   */
  def signum(a: Float): Float = {
    if (a < 0.0f) -1.0f else (if (a > 0.0f) 1.0f else a) // return +0.0/-0.0/NaN depending on a
  }

  /** Compute next number towards positive infinity.
   * @param a number to which neighbor should be computed
   * @return neighbor of a towards positive infinity
   */
  def nextUp(a: Double): Double = {
    nextAfter(a, Double.PositiveInfinity)
  }

  /** Compute next number towards positive infinity.
   * @param a number to which neighbor should be computed
   * @return neighbor of a towards positive infinity
   */
  def nextUp(a: Float): Float = {
    nextAfter(a, Float.PositiveInfinity)
  }

  /** Returns a pseudo-random number between 0.0 and 1.0.
   * <p><b>Note:</b> this implementation currently delegates to {@link Math#random}
   * @return a random number between 0.0 and 1.0
   */
  def random: Double = {
    math.random
  }

  /**
   * Exponential function.
   *
   * Computes exp(x), function result is nearly rounded.   It will be correctly
   * rounded to the theoretical value for 99.9% of input values, otherwise it will
   * have a 1 UPL error.
   *
   * Method:
   *    Lookup intVal = exp(int(x))
   *    Lookup fracVal = exp(int(x-int(x) / 1024.0) * 1024.0 )
   *    Compute z as the exponential of the remaining bits by a polynomial minus one
   *    exp(x) = intVal * fracVal * (1 + z)
   *
   * Accuracy:
   *    Calculation is done with 63 bits of precision, so result should be correctly
   *    rounded for 99.9% of input values, with less than 1 ULP error otherwise.
   *
   * @param x   a Double
   * @return Double e<sup>x</sup>
   */
  def exp(x: Double): Double = {
    exp(x, 0.0, null)
  }

  /**
   * Internal helper method for exponential function.
   * @param x original argument of the exponential function
   * @param extra extra bits of precision on input (To Be Confirmed)
   * @param hiPrec extra bits of precision on output (To Be Confirmed)
   * @return exp(x)
   */
  private def exp(x: Double, extra: Double, hiPrec: Array[Double]): Double = {
    var intPartA = 0.0
    var intPartB = 0.0
    var intVal = 0

    /* Lookup exp(floor(x)).
     * intPartA will have the upper 22 bits, intPartB will have the lower
     * 52 bits.
     */
    if (x < 0.0) {
      intVal = -x.toInt

      if (intVal > 746) {
        if (hiPrec != null) {
          hiPrec(0) = 0.0
          hiPrec(1) = 0.0
        }
        return 0.0
      }

      if (intVal > 709) {
        /* This will produce a subnormal output */
        val result = exp(x+40.19140625, extra, hiPrec) / 285040095144011776.0
        if (hiPrec != null) {
          hiPrec(0) /= 285040095144011776.0
          hiPrec(1) /= 285040095144011776.0
        }
        return result
      }

      if (intVal == 709) {
        /* exp(1.494140625) is nearly a machine number... */
        val result = exp(x+1.494140625, extra, hiPrec) / 4.455505956692756620
        if (hiPrec != null) {
          hiPrec(0) /= 4.455505956692756620
          hiPrec(1) /= 4.455505956692756620
        }
        return result
      }

      intVal += 1

      intPartA = ExpIntTable.EXP_INT_TABLE_A(EXP_INT_TABLE_MAX_INDEX-intVal)
      intPartB = ExpIntTable.EXP_INT_TABLE_B(EXP_INT_TABLE_MAX_INDEX-intVal)

      intVal = -intVal
    } else {
      intVal = x.toInt

      if (intVal > 709) {
        if (hiPrec != null) {
          hiPrec(0) = Double.PositiveInfinity
          hiPrec(1) = 0.0
        }
        return Double.PositiveInfinity
      }

      intPartA = ExpIntTable.EXP_INT_TABLE_A(EXP_INT_TABLE_MAX_INDEX+intVal)
      intPartB = ExpIntTable.EXP_INT_TABLE_B(EXP_INT_TABLE_MAX_INDEX+intVal)
    }

    /* Get the fractional part of x, find the greatest multiple of 2^-10 less than
     * x and look up the exp function of it.
     * fracPartA will have the upper 22 bits, fracPartB the lower 52 bits.
     */
    val intFrac = ((x - intVal) * 1024.0).toInt
    val fracPartA = ExpFracTable.EXP_FRAC_TABLE_A(intFrac)
    val fracPartB = ExpFracTable.EXP_FRAC_TABLE_B(intFrac)

    /* epsilon is the difference in x from the nearest multiple of 2^-10.  It
     * has a value in the range 0 <= epsilon < 2^-10.
     * Do the subtraction from x as the last step to avoid possible loss of percison.
     */
    val epsilon = x - (intVal + intFrac / 1024.0)

    /* Compute z = exp(epsilon) - 1.0 via a minimax polynomial.  z has
     full Double precision (52 bits).  Since z < 2^-10, we will have
     62 bits of precision when combined with the contant 1.  This will be
     used in the last addition below to get proper rounding. */

    /* Remez generated polynomial.  Converges on the interval [0, 2^-10], error
     is less than 0.5 ULP */
    var z = 0.04168701738764507
    z = z * epsilon + 0.1666666505023083
    z = z * epsilon + 0.5000000000042687
    z = z * epsilon + 1.0
    z = z * epsilon + -3.940510424527919E-20

    /* Compute (intPartA+intPartB) * (fracPartA+fracPartB) by binomial
     expansion.
     tempA is exact since intPartA and intPartB only have 22 bits each.
     tempB will have 52 bits of precision.
     */
    val tempA = intPartA * fracPartA
    val tempB = intPartA * fracPartB + intPartB * fracPartA + intPartB * fracPartB

    /* Compute the result.  (1+z)(tempA+tempB).  Order of operations is
     important.  For accuracy add by increasing size.  tempA is exact and
     much larger than the others.  If there are extra bits specified from the
     pow() function, use them. */
    val tempC = tempB + tempA
    val result = if (extra != 0.0) {
      tempC*extra*z + tempC*extra + tempC*z + tempB + tempA
    } else {
      tempC*z + tempB + tempA
    }

    if (hiPrec != null) {
      // If requesting high precision
      hiPrec(0) = tempA
      hiPrec(1) = tempC*extra*z + tempC*extra + tempC*z + tempB
    }

    result
  }

  /** Compute exp(x) - 1
   * @param x number to compute shifted exponential
   * @return exp(x) - 1
   */
  def expm1(x: Double): Double = {
    expm1(x, null)
  }

  /** Internal helper method for expm1
   * @param x number to compute shifted exponential
   * @param hiPrecOut receive high precision result for -1.0 < x < 1.0
   * @return exp(x) - 1
   */
  private def expm1(_x: Double, hiPrecOut: Array[Double]): Double = {
    var x = _x
    if (x != x || x == 0.0) { // NaN or zero
      return x
    }

    if (x <= -1.0 || x >= 1.0) {
      // If not between +/- 1.0
      //return exp(x) - 1.0
      val hiPrec = new Array[Double](2)
      exp(x, 0.0, hiPrec)
      if (x > 0.0) {
        return -1.0 + hiPrec(0) + hiPrec(1)
      } else {
        val ra = -1.0 + hiPrec(0)
        var rb = -(ra + 1.0 - hiPrec(0))
        rb += hiPrec(1)
        return ra + rb
      }
    }

    var baseA = 0.0
    var baseB = 0.0
    var epsilon = 0.0
    var negative = false

    if (x < 0.0) {
      x = -x
      negative = true
    }

    {
      val intFrac = (x * 1024.0).toInt
      var tempA = ExpFracTable.EXP_FRAC_TABLE_A(intFrac) - 1.0
      var tempB = ExpFracTable.EXP_FRAC_TABLE_B(intFrac)

      var temp = tempA + tempB
      tempB = -(temp - tempA - tempB)
      tempA = temp

      temp = tempA * HEX_40000000
      baseA = tempA + temp - temp
      baseB = tempB + (tempA - baseA)

      epsilon = x - intFrac/1024.0
    }


    /* Compute expm1(epsilon) */
    var zb = 0.008336750013465571
    zb = zb * epsilon + 0.041666663879186654
    zb = zb * epsilon + 0.16666666666745392
    zb = zb * epsilon + 0.49999999999999994
    zb = zb * epsilon
    zb = zb * epsilon

    var za = epsilon
    var temp = za + zb
    zb = -(temp - za - zb)
    za = temp

    temp = za * HEX_40000000
    temp = za + temp - temp
    zb += za - temp
    za = temp

    /* Combine the parts.   expm1(a+b) = expm1(a) + expm1(b) + expm1(a)*expm1(b) */
    var ya = za * baseA
    //Double yb = za*baseB + zb*baseA + zb*baseB
    temp = ya + za * baseB
    var yb = -(temp - ya - za * baseB)
    ya = temp

    temp = ya + zb * baseA
    yb += -(temp - ya - zb * baseA)
    ya = temp

    temp = ya + zb * baseB
    yb += -(temp - ya - zb*baseB)
    ya = temp

    //ya = ya + za + baseA
    //yb = yb + zb + baseB
    temp = ya + baseA
    yb += -(temp - baseA - ya)
    ya = temp

    temp = ya + za
    //yb += (ya > za) ? -(temp - ya - za) : -(temp - za - ya)
    yb += -(temp - ya - za)
    ya = temp

    temp = ya + baseB
    //yb += (ya > baseB) ? -(temp - ya - baseB) : -(temp - baseB - ya)
    yb += -(temp - ya - baseB)
    ya = temp

    temp = ya + zb
    //yb += (ya > zb) ? -(temp - ya - zb) : -(temp - zb - ya)
    yb += -(temp - ya - zb)
    ya = temp

    if (negative) {
      /* Compute expm1(-x) = -expm1(x) / (expm1(x) + 1) */
      val denom = 1.0 + ya
      val denomr = 1.0 / denom
      val denomb = -(denom - 1.0 - ya) + yb
      val ratio = ya * denomr
      temp = ratio * HEX_40000000
      val ra = ratio + temp - temp
      var rb = ratio - ra

      temp = denom * HEX_40000000
      za = denom + temp - temp
      zb = denom - za

      rb += (ya - za * ra - za * rb - zb * ra - zb * rb) * denomr

      // f(x) = x/1+x
      // Compute f'(x)
      // Product rule:  d(uv) = du*v + u*dv
      // Chain rule:  d(f(g(x)) = f'(g(x))*f(g'(x))
      // d(1/x) = -1/(x*x)
      // d(1/1+x) = -1/( (1+x)^2) *  1 =  -1/((1+x)*(1+x))
      // d(x/1+x) = -x/((1+x)(1+x)) + 1/1+x = 1 / ((1+x)(1+x))

      // Adjust for yb
      rb += yb * denomr                      // numerator
      rb += -ya * denomb * denomr * denomr   // denominator

      // negate
      ya = -ra
      yb = -rb
    }

    if (hiPrecOut != null) {
      hiPrecOut(0) = ya
      hiPrecOut(1) = yb
    }

    ya + yb
  }

  /**
   * Natural logarithm.
   *
   * @param x   a Double
   * @return log(x)
   */
  def log(x: Double): Double = {
    log(x, null)
  }

  /**
   * Internal helper method for natural logarithm function.
   * @param x original argument of the natural logarithm function
   * @param hiPrec extra bits of precision on output (To Be Confirmed)
   * @return log(x)
   */
  private def log(x: Double, hiPrec: Array[Double]): Double = {
    if (x==0) { // Handle special case of +0/-0
      return Double.NegativeInfinity
    }
    var bits = java.lang.Double.doubleToLongBits(x)

    /* Handle special cases of negative input, and NaN */
    if ((bits & 0x8000000000000000L) != 0 || x != x) {
      if (x != 0.0) {
        if (hiPrec != null) {
          hiPrec(0) = Double.NaN
        }

        return Double.NaN
      }
    }

    /* Handle special cases of Positive infinity. */
    if (x == Double.PositiveInfinity) {
      if (hiPrec != null) {
        hiPrec(0) = Double.PositiveInfinity
      }

      return Double.PositiveInfinity
    }

    /* Extract the exponent */
    var exp = (bits >> 52).toInt - 1023

    if ((bits & 0x7ff0000000000000L) == 0) {
      // Subnormal!
      if (x == 0) {
        // Zero
        if (hiPrec != null) {
          hiPrec(0) = Double.NegativeInfinity
        }

        return Double.NegativeInfinity
      }

      /* Normalize the subnormal number. */
      bits <<= 1
      while ( (bits & 0x0010000000000000L) == 0) {
        exp -= 1
        bits <<= 1
      }
    }


    if (exp == -1 || exp == 0) {
      if (x < 1.01 && x > 0.99 && hiPrec == null) {
        /* The normal method doesn't work well in the range [0.99, 1.01], so call do a straight
         polynomial expansion in higer precision. */

        /* Compute x - 1.0 and split it */
        var xa = x - 1.0
        var xb = xa - x + 1.0
        var tmp = xa * HEX_40000000
        var aa = xa + tmp - tmp
        var ab = xa - aa
        xa = aa
        xb = ab

        var ya = LN_QUICK_COEF(LN_QUICK_COEF.length-1)(0)
        var yb = LN_QUICK_COEF(LN_QUICK_COEF.length-1)(1)

        var i = LN_QUICK_COEF.length - 2
        while (i >= 0) {
          /* Multiply a = y * x */
          aa = ya * xa
          ab = ya * xb + yb * xa + yb * xb
          /* split, so now y = a */
          tmp = aa * HEX_40000000
          ya = aa + tmp - tmp
          yb = aa - ya + ab

          /* Add  a = y + lnQuickCoef */
          aa = ya + LN_QUICK_COEF(i)(0)
          ab = yb + LN_QUICK_COEF(i)(1)
          /* Split y = a */
          tmp = aa * HEX_40000000
          ya = aa + tmp - tmp
          yb = aa - ya + ab
          i -= 1
        }

        /* Multiply a = y * x */
        aa = ya * xa
        ab = ya * xb + yb * xa + yb * xb
        /* split, so now y = a */
        tmp = aa * HEX_40000000
        ya = aa + tmp - tmp
        yb = aa - ya + ab

        return ya + yb
      }
    }

    // lnm is a log of a number in the range of 1.0 - 2.0, so 0 <= lnm < ln(2)
    val lnm = lnMant.LN_MANT(((bits & 0x000ffc0000000000L) >> 42).toInt)

    /*
     Double epsilon = x / java.lang.Double.longBitsToDouble(bits & 0xfffffc0000000000L)

     epsilon -= 1.0
     */

    // y is the most significant 10 bits of the mantissa
    //Double y = java.lang.Double.longBitsToDouble(bits & 0xfffffc0000000000L)
    //Double epsilon = (x - y) / y
    val epsilon = (bits & 0x3ffffffffffL) / (TWO_POWER_52 + (bits & 0x000ffc0000000000L))

    var lnza = 0.0
    var lnzb = 0.0

    if (hiPrec != null) {
      /* split epsilon -> x */
      var tmp = epsilon * HEX_40000000
      var aa = epsilon + tmp - tmp
      var ab = epsilon - aa
      var xa = aa
      var xb = ab

      /* Need a more accurate epsilon, so adjust the division. */
      val numer = bits & 0x3ffffffffffL
      val denom = TWO_POWER_52 + (bits & 0x000ffc0000000000L)
      aa = numer - xa*denom - xb * denom
      xb += aa / denom

      /* Remez polynomial evaluation */
      var ya = LN_HI_PREC_COEF(LN_HI_PREC_COEF.length - 1)(0)
      var yb = LN_HI_PREC_COEF(LN_HI_PREC_COEF.length - 1)(1)

      var i = LN_HI_PREC_COEF.length - 2
      while (i >= 0) {
        /* Multiply a = y * x */
        aa = ya * xa
        ab = ya * xb + yb * xa + yb * xb
        /* split, so now y = a */
        tmp = aa * HEX_40000000
        ya = aa + tmp - tmp
        yb = aa - ya + ab

        /* Add  a = y + lnHiPrecCoef */
        aa = ya + LN_HI_PREC_COEF(i)(0)
        ab = yb + LN_HI_PREC_COEF(i)(1)
        /* Split y = a */
        tmp = aa * HEX_40000000
        ya = aa + tmp - tmp
        yb = aa - ya + ab
        i -= 1
      }

      /* Multiply a = y * x */
      aa = ya * xa
      ab = ya * xb + yb * xa + yb * xb

      /* split, so now lnz = a */
      /*
       tmp = aa * 1073741824.0
       lnza = aa + tmp - tmp
       lnzb = aa - lnza + ab
       */
      lnza = aa + ab
      lnzb = -(lnza - aa - ab)
    } else {
      /* High precision not required.  Eval Remez polynomial
       using standard Double precision */
      lnza = -0.16624882440418567
      lnza = lnza * epsilon + 0.19999954120254515
      lnza = lnza * epsilon + -0.2499999997677497
      lnza = lnza * epsilon + 0.3333333333332802
      lnza = lnza * epsilon + -0.5
      lnza = lnza * epsilon + 1.0
      lnza = lnza * epsilon
    }

    /* Relative sizes:
     * lnzb     [0, 2.33E-10]
     * lnm(1)   [0, 1.17E-7]
     * ln2B*exp [0, 1.12E-4]
     * lnza      [0, 9.7E-4]
     * lnm(0)   [0, 0.692]
     * ln2A*exp [0, 709]
     */

    /* Compute the following sum:
     * lnzb + lnm(1) + ln2B*exp + lnza + lnm(0) + ln2A*exp
     */

    //return lnzb + lnm(1) + ln2B*exp + lnza + lnm(0) + ln2A*exp
    var a = LN_2_A*exp
    var b = 0.0
    var c = a+lnm(0)
    var d = -(c-a-lnm(0))
    a = c
    b = b + d

    c = a + lnza
    d = -(c - a - lnza)
    a = c
    b = b + d

    c = a + LN_2_B*exp
    d = -(c - a - LN_2_B*exp)
    a = c
    b = b + d

    c = a + lnm(1)
    d = -(c - a - lnm(1))
    a = c
    b = b + d

    c = a + lnzb
    d = -(c - a - lnzb)
    a = c
    b = b + d

    if (hiPrec != null) {
      hiPrec(0) = a
      hiPrec(1) = b
    }

    return a + b
  }

  /** Compute log(1 + x).
   * @param x a number
   * @return log(1 + x)
   */
  def log1p(x: Double): Double = {

    if (x == -1) {
      return x/0.0   // -Infinity
    }

    if (x > 0 && 1/x == 0) { // x = Infinity
      return x
    }

    if (x > 1e-6 || x < -1e-6) {
      val xpa = 1.0 + x
      val xpb = -(xpa - 1.0 - x)

      val hiPrec = new Array[Double](2)

      val lores = log(xpa, hiPrec)
      if (java.lang.Double.isInfinite(lores)){ // don't allow this to be converted to NaN
        return lores
      }

      /* Do a taylor series expansion around xpa */
      /* f(x+y) = f(x) + f'(x)*y + f''(x)/2 y^2 */
      val fx1 = xpb/xpa

      var epsilon = 0.5 * fx1 + 1.0
      epsilon = epsilon * fx1

      return epsilon + hiPrec(1) + hiPrec(0)
    }

    /* Value is small |x| < 1e6, do a Taylor series centered on 1.0 */
    var y = x * F_1_3 - F_1_2
    y = y * x + 1.0
    y = y * x

    return y
  }

  /** Compute the base 10 logarithm.
   * @param x a number
   * @return log10(x)
   */
  def log10(x: Double): Double = {
    val hiPrec = new Array[Double](2)

    val lores = log(x, hiPrec)
    if (java.lang.Double.isInfinite(lores)) { // don't allow this to be converted to NaN
      return lores
    }

    val tmp = hiPrec(0) * HEX_40000000
    val lna = hiPrec(0) + tmp - tmp
    val lnb = hiPrec(0) - lna + hiPrec(1)

    val rln10a = 0.4342944622039795
    val rln10b = 1.9699272335463627E-8

    return rln10b * lnb + rln10b * lna + rln10a * lnb + rln10a * lna
  }

  /**
   * Computes the <a href="http://mathworld.wolfram.com/Logarithm.html">
   * logarithm</a> in a given base.
   *
   * Returns {@code NaN} if either argument is negative.
   * If {@code base} is 0 and {@code x} is positive, 0 is returned.
   * If {@code base} is positive and {@code x} is 0,
   * {@code Double.NegativeInfinity} is returned.
   * If both arguments are 0, the result is {@code NaN}.
   *
   * @param base Base of the logarithm, must be greater than 0.
   * @param x Argument, must be greater than 0.
   * @return the value of the logarithm, i.e. the number {@code y} such that
   * <code>base<sup>y</sup> = x</code>.
   * @since 1.2 (previously in {@code MathUtils}, moved as of version 3.0)
   */
  def log(base: Double, x: Double): Double = {
    log(x) / log(base)
  }

  /**
   * Power function.  Compute x^y.
   *
   * @param x   a Double
   * @param y   a Double
   * @return Double
   */
  def pow(x: Double, y: Double): Double = {
    val lns = new Array[Double](2)

    if (y == 0.0) {
      return 1.0
    }

    if (x != x) { // X is NaN
      return x
    }


    if (x == 0) {
      val bits = java.lang.Double.doubleToLongBits(x)
      if ((bits & 0x8000000000000000L) != 0) {
        // -zero
        val yi = y.toLong

        if (y < 0 && y == yi && (yi & 1) == 1) {
          return Double.NegativeInfinity
        }

        if (y > 0 && y == yi && (yi & 1) == 1) {
          return -0.0
        }
      }

      if (y < 0) {
        return Double.PositiveInfinity
      }
      if (y > 0) {
        return 0.0
      }

      return Double.NaN
    }

    if (x == Double.PositiveInfinity) {
      if (y != y) { // y is NaN
        return y
      }
      if (y < 0.0) {
        return 0.0
      } else {
        return Double.PositiveInfinity
      }
    }

    if (y == Double.PositiveInfinity) {
      if (x * x == 1.0) {
        return Double.NaN
      }

      if (x * x > 1.0) {
        return Double.PositiveInfinity
      } else {
        return 0.0
      }
    }

    if (x == Double.NegativeInfinity) {
      if (y != y) { // y is NaN
        return y
      }

      if (y < 0) {
        val yi = y.toLong
        if (y == yi && (yi & 1) == 1) {
          return -0.0
        }

        return 0.0
      }

      if (y > 0)  {
        val yi = y.toLong
        if (y == yi && (yi & 1) == 1) {
          return Double.NegativeInfinity
        }

        return Double.PositiveInfinity
      }
    }

    if (y == Double.NegativeInfinity) {

      if (x * x == 1.0) {
        return Double.NaN
      }

      if (x * x < 1.0) {
        return Double.PositiveInfinity
      } else {
        return 0.0
      }
    }

    /* Handle special case x<0 */
    if (x < 0) {
      // y is an even integer in this case
      if (y >= TWO_POWER_52 || y <= -TWO_POWER_52) {
        return pow(-x, y)
      }

      if (y == y.toLong) {
        // If y is an integer
        return if ((y.toLong & 1) == 0) pow(-x, y) else -pow(-x, y)
      } else {
        return Double.NaN
      }
    }

    /* Split y into ya and yb such that y = ya+yb */
    var ya = 0.0
    var yb = 0.0
    if (y < 8e298 && y > -8e298) {
      val tmp1 = y * HEX_40000000
      ya = y + tmp1 - tmp1
      yb = y - ya
    } else {
      val tmp1 = y * 9.31322574615478515625E-10
      val tmp2 = tmp1 * 9.31322574615478515625E-10
      ya = (tmp1 + tmp2 - tmp1) * HEX_40000000 * HEX_40000000
      yb = y - ya
    }

    /* Compute ln(x) */
    val lores = log(x, lns)
    if (java.lang.Double.isInfinite(lores)) { // don't allow this to be converted to NaN
      return lores
    }

    var lna = lns(0)
    var lnb = lns(1)

    /* resplit lns */
    val tmp1 = lna * HEX_40000000
    val tmp2 = lna + tmp1 - tmp1
    lnb += lna - tmp2
    lna = tmp2

    // y*ln(x) = (aa+ab)
    val aa = lna * ya
    val ab = lna * yb + lnb * ya + lnb * yb

    lna = aa+ab
    lnb = -(lna - aa - ab)

    var z = 1.0 / 120.0
    z = z * lnb + (1.0 / 24.0)
    z = z * lnb + (1.0 / 6.0)
    z = z * lnb + 0.5
    z = z * lnb + 1.0
    z = z * lnb

    val result = exp(lna, z, null)
    //result = result + result * z
    return result
  }


  /**
   *  Computes sin(x) - x, where |x| < 1/16.
   *  Use a Remez polynomial approximation.
   *  @param x a number smaller than 1/16
   *  @return sin(x) - x
   */
  private def polySine(x: Double): Double = {
    val x2 = x*x

    var p = 2.7553817452272217E-6
    p = p * x2 + -1.9841269659586505E-4
    p = p * x2 + 0.008333333333329196
    p = p * x2 + -0.16666666666666666
    //p *= x2
    //p *= x
    p = p * x2 * x

    return p
  }

  /**
   *  Computes cos(x) - 1, where |x| < 1/16.
   *  Use a Remez polynomial approximation.
   *  @param x a number smaller than 1/16
   *  @return cos(x) - 1
   */
  private def polyCosine(x: Double): Double = {
    val x2 = x*x

    var p = 2.479773539153719E-5
    p = p * x2 + -0.0013888888689039883
    p = p * x2 + 0.041666666666621166
    p = p * x2 + -0.49999999999999994
    p *= x2

    return p
  }

  /**
   *  Compute sine over the first quadrant (0 < x < pi/2).
   *  Use combination of table lookup and rational polynomial expansion.
   *  @param xa number from which sine is requested
   *  @param xb extra bits for x (may be 0.0)
   *  @return sin(xa + xb)
   */
  private def sinQ(xa: Double, xb: Double): Double = {
    val idx = ((xa * 8.0) + 0.5).toInt
    val epsilon = xa - EIGHTHS(idx) //idx*0.125

    // Table lookups
    val sintA = SINE_TABLE_A(idx)
    val sintB = SINE_TABLE_B(idx)
    val costA = COSINE_TABLE_A(idx)
    val costB = COSINE_TABLE_B(idx)

    // Polynomial eval of sin(epsilon), cos(epsilon)
    var sinEpsA = epsilon
    var sinEpsB = polySine(epsilon)
    val cosEpsA = 1.0
    val cosEpsB = polyCosine(epsilon)

    // Split epsilon   xa + xb = x
    val temp = sinEpsA * HEX_40000000
    val temp2 = (sinEpsA + temp) - temp
    sinEpsB +=  sinEpsA - temp2
    sinEpsA = temp2

    /* Compute sin(x) by angle addition formula */
    var result = 0.0

    /* Compute the following sum:
     *
     * result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
     *          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB
     *
     * Ranges of elements
     *
     * xxxtA   0            PI/2
     * xxxtB   -1.5e-9      1.5e-9
     * sinEpsA -0.0625      0.0625
     * sinEpsB -6e-11       6e-11
     * cosEpsA  1.0
     * cosEpsB  0           -0.0625
     *
     */

    //result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
    //          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB

    //result = sintA + sintA*cosEpsB + sintB + sintB * cosEpsB
    //result += costA*sinEpsA + costA*sinEpsB + costB*sinEpsA + costB * sinEpsB
    var a = 0.0
    var b = 0.0

    var t = sintA
    var c = a + t
    var d = -(c - a - t)
    a = c
    b = b + d

    t = costA * sinEpsA
    c = a + t
    d = -(c - a - t)
    a = c
    b = b + d

    b = b + sintA * cosEpsB + costA * sinEpsB
    /*
     t = sintA*cosEpsB
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d

     t = costA*sinEpsB
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d
     */

    b = b + sintB + costB * sinEpsA + sintB * cosEpsB + costB * sinEpsB
    /*
     t = sintB
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d

     t = costB*sinEpsA
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d

     t = sintB*cosEpsB
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d

     t = costB*sinEpsB
     c = a + t
     d = -(c - a - t)
     a = c
     b = b + d
     */

    if (xb != 0.0) {
      t = ((costA + costB) * (cosEpsA + cosEpsB) -
           (sintA + sintB) * (sinEpsA + sinEpsB)) * xb  // approximate cosine*xb
      c = a + t
      d = -(c - a - t)
      a = c
      b = b + d
    }

    result = a + b

    return result
  }

  /**
   * Compute cosine in the first quadrant by subtracting input from PI/2 and
   * then calling sinQ.  This is more accurate as the input approaches PI/2.
   *  @param xa number from which cosine is requested
   *  @param xb extra bits for x (may be 0.0)
   *  @return cos(xa + xb)
   */
  private def cosQ(xa: Double, xb: Double): Double = {
    val pi2a = 1.5707963267948966
    val pi2b = 6.123233995736766E-17

    val a = pi2a - xa
    var b = -(a - pi2a + xa)
    b += pi2b - xb

    sinQ(a, b)
  }

  /**
   *  Compute tangent (or cotangent) over the first quadrant.   0 < x < pi/2
   *  Use combination of table lookup and rational polynomial expansion.
   *  @param xa number from which sine is requested
   *  @param xb extra bits for x (may be 0.0)
   *  @param cotanFlag if true, compute the cotangent instead of the tangent
   *  @return tan(xa+xb) (or cotangent, depending on cotanFlag)
   */
  private def tanQ(xa: Double, xb: Double, cotanFlag: Boolean): Double = {

    val idx = ((xa * 8.0) + 0.5).toInt
    val epsilon = xa - EIGHTHS(idx) //idx*0.125

    // Table lookups
    val sintA = SINE_TABLE_A(idx)
    val sintB = SINE_TABLE_B(idx)
    val costA = COSINE_TABLE_A(idx)
    val costB = COSINE_TABLE_B(idx)

    // Polynomial eval of sin(epsilon), cos(epsilon)
    var sinEpsA = epsilon
    var sinEpsB = polySine(epsilon)
    val cosEpsA = 1.0
    val cosEpsB = polyCosine(epsilon)

    // Split epsilon   xa + xb = x
    var temp = sinEpsA * HEX_40000000
    val temp2 = (sinEpsA + temp) - temp
    sinEpsB +=  sinEpsA - temp2
    sinEpsA = temp2

    /* Compute sin(x) by angle addition formula */

    /* Compute the following sum:
     *
     * result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
     *          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB
     *
     * Ranges of elements
     *
     * xxxtA   0            PI/2
     * xxxtB   -1.5e-9      1.5e-9
     * sinEpsA -0.0625      0.0625
     * sinEpsB -6e-11       6e-11
     * cosEpsA  1.0
     * cosEpsB  0           -0.0625
     *
     */

    //result = sintA + costA*sinEpsA + sintA*cosEpsB + costA*sinEpsB +
    //          sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB

    //result = sintA + sintA*cosEpsB + sintB + sintB * cosEpsB
    //result += costA*sinEpsA + costA*sinEpsB + costB*sinEpsA + costB * sinEpsB
    var a = 0.0
    var b = 0.0

    // Compute sine
    var t = sintA
    var c = a + t
    var d = -(c - a - t)
    a = c
    b = b + d

    t = costA*sinEpsA
    c = a + t
    d = -(c - a - t)
    a = c
    b = b + d

    b = b + sintA*cosEpsB + costA*sinEpsB
    b = b + sintB + costB*sinEpsA + sintB*cosEpsB + costB*sinEpsB

    var sina = a + b
    var sinb = -(sina - a - b)

    // Compute cosine

    a = 0.0 
    b = 0.0
    c = 0.0
    d = 0.0

    t = costA*cosEpsA
    c = a + t
    d = -(c - a - t)
    a = c
    b = b + d

    t = -sintA*sinEpsA
    c = a + t
    d = -(c - a - t)
    a = c
    b = b + d

    b = b + costB*cosEpsA + costA*cosEpsB + costB*cosEpsB
    b = b - (sintB*sinEpsA + sintA*sinEpsB + sintB*sinEpsB)

    var cosa = a + b
    var cosb = -(cosa - a - b)

    if (cotanFlag) {
      var tmp = cosa; cosa = sina; sina = tmp
      tmp = cosb; cosb = sinb; sinb = tmp
    }


    /* estimate and correct, compute 1.0/(cosa+cosb) */
    /*
     Double est = (sina+sinb)/(cosa+cosb)
     Double err = (sina - cosa*est) + (sinb - cosb*est)
     est += err/(cosa+cosb)
     err = (sina - cosa*est) + (sinb - cosb*est)
     */

    // f(x) = 1/x,   f'(x) = -1/x^2

    val est = sina/cosa

    /* Split the estimate to get more accurate read on division rounding */
    temp = est * HEX_40000000
    val esta = (est + temp) - temp
    val estb =  est - esta

    temp = cosa * HEX_40000000
    val cosaa = (cosa + temp) - temp
    val cosab =  cosa - cosaa

    //Double err = (sina - est*cosa)/cosa  // Correction for division rounding
    var err = (sina - esta*cosaa - esta*cosab - estb*cosaa - estb*cosab)/cosa  // Correction for division rounding
    err += sinb/cosa                     // Change in est due to sinb
    err += -sina * cosb / cosa / cosa    // Change in est due to cosb

    if (xb != 0.0) {
      // tan' = 1 + tan^2      cot' = -(1 + cot^2)
      // Approximate impact of xb
      var xbadj = xb + est*est*xb
      if (cotanFlag) {
        xbadj = -xbadj
      }

      err += xbadj
    }

    est+err
  }

  /** Reduce the input argument using the Payne and Hanek method.
   *  This is good for all inputs 0.0 < x < inf
   *  Output is remainder after dividing by PI/2
   *  The result array should contain 3 numbers.
   *  result(0) is the integer portion, so mod 4 this gives the quadrant.
   *  result(1) is the upper bits of the remainder
   *  result[2] is the lower bits of the remainder
   *
   * @param x number to reduce
   * @param result placeholder where to put the result
   */
  private def reducePayneHanek(x: Double, result: Array[Double]) {
    /* Convert input Double to bits */
    var inbits = java.lang.Double.doubleToLongBits(x)
    var exponent = ((inbits >> 52) & 0x7ff).toInt - 1023

    /* Convert to fixed point representation */
    inbits &= 0x000fffffffffffffL
    inbits |= 0x0010000000000000L

    /* Normalize input to be between 0.5 and 1.0 */
    exponent += 1
    inbits <<= 11

    /* Based on the exponent, get a shifted copy of recip2pi */
    var shpi0 = 0L
    var shpiA = 0L
    var shpiB = 0L
    val idx = exponent >> 6
    val shift = exponent - (idx << 6)

    if (shift != 0) {
      shpi0 = if (idx == 0) 0 else (RECIP_2PI(idx-1) << shift)
      shpi0 |= RECIP_2PI(idx) >>> (64-shift)
      shpiA = (RECIP_2PI(idx) << shift) | (RECIP_2PI(idx+1) >>> (64-shift))
      shpiB = (RECIP_2PI(idx+1) << shift) | (RECIP_2PI(idx+2) >>> (64-shift))
    } else {
      shpi0 = if (idx == 0) 0 else RECIP_2PI(idx-1)
      shpiA = RECIP_2PI(idx)
      shpiB = RECIP_2PI(idx+1)
    }

    /* Multiply input by shpiA */
    var a = inbits >>> 32
    var b = inbits & 0xffffffffL

    var c = shpiA >>> 32
    var d = shpiA & 0xffffffffL

    var ac = a * c
    var bd = b * d
    var bc = b * c
    var ad = a * d

    var prodB = bd + (ad << 32)
    var prodA = ac + (ad >>> 32)

    var bita = (bd & 0x8000000000000000L) != 0
    var bitb = (ad & 0x80000000L ) != 0
    var bitsum = (prodB & 0x8000000000000000L) != 0

    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prodA += 1
    }

    bita = (prodB & 0x8000000000000000L) != 0
    bitb = (bc & 0x80000000L ) != 0

    prodB = prodB + (bc << 32)
    prodA = prodA + (bc >>> 32)

    bitsum = (prodB & 0x8000000000000000L) != 0

    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prodA += 1
    }

    /* Multiply input by shpiB */
    c = shpiB >>> 32
    d = shpiB & 0xffffffffL
    ac = a * c
    bc = b * c
    ad = a * d

    /* Collect terms */
    ac = ac + ((bc + ad) >>> 32)

    bita = (prodB & 0x8000000000000000L) != 0
    bitb = (ac & 0x8000000000000000L ) != 0
    prodB += ac
    bitsum = (prodB & 0x8000000000000000L) != 0
    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prodA += 1
    }

    /* Multiply by shpi0 */
    c = shpi0 >>> 32
    d = shpi0 & 0xffffffffL

    bd = b * d
    bc = b * c
    ad = a * d

    prodA += bd + ((bc + ad) << 32)

    /*
     * prodA, prodB now contain the remainder as a fraction of PI.  We want this as a fraction of
     * PI/2, so use the following steps:
     * 1.) multiply by 4.
     * 2.) do a fixed point muliply by PI/4.
     * 3.) Convert to floating point.
     * 4.) Multiply by 2
     */

    /* This identifies the quadrant */
    val intPart = (prodA >>> 62).toInt

    /* Multiply by 4 */
    prodA <<= 2
    prodA |= prodB >>> 62
    prodB <<= 2

    /* Multiply by PI/4 */
    a = prodA >>> 32
    b = prodA & 0xffffffffL

    c = PI_O_4_BITS(0) >>> 32
    d = PI_O_4_BITS(0) & 0xffffffffL

    ac = a * c
    bd = b * d
    bc = b * c
    ad = a * d

    var prod2B = bd + (ad << 32)
    var prod2A = ac + (ad >>> 32)

    bita = (bd & 0x8000000000000000L) != 0
    bitb = (ad & 0x80000000L ) != 0
    bitsum = (prod2B & 0x8000000000000000L) != 0

    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prod2A += 1
    }

    bita = (prod2B & 0x8000000000000000L) != 0
    bitb = (bc & 0x80000000L ) != 0

    prod2B = prod2B + (bc << 32)
    prod2A = prod2A + (bc >>> 32)

    bitsum = (prod2B & 0x8000000000000000L) != 0

    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prod2A += 1
    }

    /* Multiply input by pio4bits(1) */
    c = PI_O_4_BITS(1) >>> 32
    d = PI_O_4_BITS(1) & 0xffffffffL
    ac = a * c
    bc = b * c
    ad = a * d

    /* Collect terms */
    ac = ac + ((bc + ad) >>> 32)

    bita = (prod2B & 0x8000000000000000L) != 0
    bitb = (ac & 0x8000000000000000L ) != 0
    prod2B += ac
    bitsum = (prod2B & 0x8000000000000000L) != 0
    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prod2A += 1
    }

    /* Multiply inputB by pio4bits(0) */
    a = prodB >>> 32
    b = prodB & 0xffffffffL
    c = PI_O_4_BITS(0) >>> 32
    d = PI_O_4_BITS(0) & 0xffffffffL
    ac = a * c
    bc = b * c
    ad = a * d

    /* Collect terms */
    ac = ac + ((bc + ad) >>> 32)

    bita = (prod2B & 0x8000000000000000L) != 0
    bitb = (ac & 0x8000000000000000L ) != 0
    prod2B += ac
    bitsum = (prod2B & 0x8000000000000000L) != 0
    /* Carry */
    if ( (bita && bitb) ||
        ((bita || bitb) && !bitsum) ) {
      prod2A == 1
    }

    /* Convert to Double */
    val tmpA = (prod2A >>> 12) / TWO_POWER_52  // High order 52 bits
    val tmpB = (((prod2A & 0xfffL) << 40) + (prod2B >>> 24)) / TWO_POWER_52 / TWO_POWER_52 // Low bits

    val sumA = tmpA + tmpB
    val sumB = -(sumA - tmpA - tmpB)

    /* Multiply by PI/2 and return */
    result(0) = intPart
    result(1) = sumA * 2.0
    result(2) = sumB * 2.0
  }

  /**
   *  Sine function.
   *  @param x a number
   *  @return sin(x)
   */
  def sin(x: Double): Double = {
    var negative = false
    var quadrant = 0
    var xa = x
    var xb = 0.0

    /* Take absolute value of the input */
    if (x < 0) {
      negative = true
      xa = -xa
    }

    /* Check for zero and negative zero */
    if (xa == 0.0) {
      val bits = java.lang.Double.doubleToLongBits(x)
      if (bits < 0) {
        return -0.0
      }
      return 0.0
    }

    if (xa != xa || xa == Double.PositiveInfinity) {
      return Double.NaN
    }

    /* Perform any argument reduction */
    if (xa > 3294198.0) {
      // PI * (2**20)
      // Argument too big for CodyWaite reduction.  Must use
      // PayneHanek.
      val reduceResults = new Array[Double](3)
      reducePayneHanek(xa, reduceResults)
      quadrant = reduceResults(0).toInt & 3
      xa = reduceResults(1)
      xb = reduceResults(2)
    } else if (xa > 1.5707963267948966) {
      /* Inline the Cody/Waite reduction for performance */

      // Estimate k
      //k = (int)(xa / 1.5707963267948966)
      var k = (xa * 0.6366197723675814).toInt

      // Compute remainder
      var remA = 0.0
      var remB = 0.0
      var continue = true
      while (continue) {
        var a = -k * 1.570796251296997
        remA = xa + a
        remB = -(remA - xa - a)

        a = -k * 7.549789948768648E-8
        var b = remA
        remA = a + b
        remB += -(remA - b - a)

        a = -k * 6.123233995736766E-17
        b = remA
        remA = a + b
        remB += -(remA - b - a)

        if (remA > 0.0) {
          continue = false
        } else {

          // Remainder is negative, so decrement k and try again.
          // This should only happen if the input is very close
          // to an even multiple of pi/2
          k -= 1
        }
      }
      quadrant = k & 3
      xa = remA
      xb = remB
    }

    if (negative) {
      quadrant ^= 2  // Flip bit 1
    }

    quadrant match {
      case 0 => sinQ(xa, xb)
      case 1 => cosQ(xa, xb)
      case 2 => -sinQ(xa, xb)
      case 3 => -cosQ(xa, xb)
      case _ => Double.NaN
    }
  }

  /**
   *  Cosine function
   *  @param x a number
   *  @return cos(x)
   */
  def cos(x: Double): Double = {
    var quadrant = 0

    /* Take absolute value of the input */
    var xa = x
    if (x < 0) {
      xa = -xa
    }

    if (xa != xa || xa == Double.PositiveInfinity) {
      return Double.NaN
    }

    /* Perform any argument reduction */
    var xb = 0.0
    if (xa > 3294198.0) {
      // PI * (2**20)
      // Argument too big for CodyWaite reduction.  Must use
      // PayneHanek.
      val reduceResults = new Array[Double](3)
      reducePayneHanek(xa, reduceResults)
      quadrant = reduceResults(0).toInt & 3
      xa = reduceResults(1)
      xb = reduceResults(2)
    } else if (xa > 1.5707963267948966) {
      /* Inline the Cody/Waite reduction for performance */

      // Estimate k
      //k = (int)(xa / 1.5707963267948966)
      var k = (xa * 0.6366197723675814).toInt

      // Compute remainder
      var remA = 0.0
      var remB = 0.0
      var continue = true
      while (continue) {
        var a = -k * 1.570796251296997
        remA = xa + a
        remB = -(remA - xa - a)

        a = -k * 7.549789948768648E-8
        var b = remA
        remA = a + b
        remB += -(remA - b - a)

        a = -k * 6.123233995736766E-17
        b = remA
        remA = a + b
        remB += -(remA - b - a)

        if (remA > 0.0) {
          continue = false
        } else {

          // Remainder is negative, so decrement k and try again.
          // This should only happen if the input is very close
          // to an even multiple of pi/2
          k -= 1
        }
      }
      quadrant = k & 3
      xa = remA
      xb = remB
    }

    //if (negative)
    //  quadrant = (quadrant + 2) % 4

    quadrant match {
      case 0 => cosQ(xa, xb)
      case 1 => -sinQ(xa, xb)
      case 2 => -cosQ(xa, xb)
      case 3 => sinQ(xa, xb)
      case _ => Double.NaN
    }
  }

  /**
   *   Tangent function
   *  @param x a number
   *  @return tan(x)
   */
  def tan(x: Double): Double = {
    var negative = false
    var quadrant = 0

    /* Take absolute value of the input */
    var xa = x
    if (x < 0) {
      negative = true
      xa = -xa
    }

    /* Check for zero and negative zero */
    if (xa == 0.0) {
      val bits = java.lang.Double.doubleToLongBits(x)
      if (bits < 0) {
        return -0.0
      }
      return 0.0
    }

    if (xa != xa || xa == Double.PositiveInfinity) {
      return Double.NaN
    }

    /* Perform any argument reduction */
    var xb = 0.0
    if (xa > 3294198.0) {
      // PI * (2**20)
      // Argument too big for CodyWaite reduction.  Must use
      // PayneHanek.
      val reduceResults = new Array[Double](3)
      reducePayneHanek(xa, reduceResults)
      quadrant = (reduceResults(0).toInt) & 3
      xa = reduceResults(1)
      xb = reduceResults(2)
    } else if (xa > 1.5707963267948966) {
      /* Inline the Cody/Waite reduction for performance */

      // Estimate k
      //k = (int)(xa / 1.5707963267948966)
      var k = (xa * 0.6366197723675814).toInt

      // Compute remainder
      var remA = 0.0
      var remB = 0.0
      var continue = true
      while (continue) {
        var a = -k * 1.570796251296997
        remA = xa + a
        remB = -(remA - xa - a)

        a = -k * 7.549789948768648E-8
        var b = remA
        remA = a + b
        remB += -(remA - b - a)

        a = -k * 6.123233995736766E-17
        b = remA
        remA = a + b
        remB += -(remA - b - a)

        if (remA > 0.0) {
          continue = false
        } else {

          // Remainder is negative, so decrement k and try again.
          // This should only happen if the input is very close
          // to an even multiple of pi/2
          k -= 1
        }
      }
      quadrant = k & 3
      xa = remA
      xb = remB
    }

    if (xa > 1.5) {
      // Accurracy suffers between 1.5 and PI/2
      val pi2a = 1.5707963267948966
      val pi2b = 6.123233995736766E-17

      val a = pi2a - xa
      var b = -(a - pi2a + xa)
      b += pi2b - xb

      xa = a + b
      xb = -(xa - a - b)
      quadrant ^= 1
      negative ^= true
    }

    var result = 0.0
    if ((quadrant & 1) == 0) {
      result = tanQ(xa, xb, false)
    } else {
      result = -tanQ(xa, xb, true)
    }

    if (negative) {
      result = -result
    }

    return result
  }

  /**
   * Arctangent function
   *  @param x a number
   *  @return atan(x)
   */
  def atan(x: Double): Double = {
    atan(x, 0.0, false)
  }

  /** Internal helper function to compute arctangent.
   * @param xa number from which arctangent is requested
   * @param xb extra bits for x (may be 0.0)
   * @param leftPlane if true, result angle must be put in the left half plane
   * @return atan(xa + xb) (or angle shifted by {@code PI} if leftPlane is true)
   */
  private def atan(_xa: Double, _xb: Double, leftPlane: Boolean): Double = {
    var xa = _xa
    var xb = _xb
    var negate = false
    var idx = 0

    if (xa == 0.0) { // Matches +/- 0.0 return correct sign
      return if (leftPlane) copySign(math.Pi, xa) else xa
    }

    if (xa < 0) {
      // negative
      xa = -xa
      xb = -xb
      negate = true
    }

    if (xa > 1.633123935319537E16) { // Very large input
      return if (negate ^ leftPlane) (-math.Pi * F_1_2) else (math.Pi * F_1_2)
    }

    /* Estimate the closest tabulated arctan value, compute eps = xa-tangentTable */
    if (xa < 1) {
      idx =  (((-1.7168146928204136 * xa * xa + 8.0) * xa) + 0.5).toInt
    } else {
      val oneOverXa = 1 / xa
      idx = (-((-1.7168146928204136 * oneOverXa * oneOverXa + 8.0) * oneOverXa) + 13.07).toInt
    }
    var epsA = xa - TANGENT_TABLE_A(idx)
    var epsB = -(epsA - xa + TANGENT_TABLE_A(idx))
    epsB += xb - TANGENT_TABLE_B(idx)

    var temp = epsA + epsB
    epsB = -(temp - epsA - epsB)
    epsA = temp

    /* Compute eps = eps / (1.0 + xa*tangent) */
    temp = xa * HEX_40000000
    var ya = xa + temp - temp
    var yb = xb + xa - ya
    xa = ya
    xb += yb

    //if (idx > 8 || idx == 0)
    if (idx == 0) {
      /* If the slope of the arctan is gentle enough (< 0.45), this approximation will suffice */
      //Double denom = 1.0 / (1.0 + xa*tangentTableA(idx) + xb*tangentTableA(idx) + xa*tangentTableB(idx) + xb*tangentTableB(idx))
      val denom = 1d / (1d + (xa + xb) * (TANGENT_TABLE_A(idx) + TANGENT_TABLE_B(idx)))
      //Double denom = 1.0 / (1.0 + xa*tangentTableA(idx))
      ya = epsA * denom
      yb = epsB * denom
    } else {
      var temp2 = xa * TANGENT_TABLE_A(idx)
      var za = 1d + temp2
      var zb = -(za - 1d - temp2)
      temp2 = xb * TANGENT_TABLE_A(idx) + xa * TANGENT_TABLE_B(idx)
      temp = za + temp2
      zb += -(temp - za - temp2)
      za = temp

      zb += xb * TANGENT_TABLE_B(idx)
      ya = epsA / za

      temp = ya * HEX_40000000
      val yaa = (ya + temp) - temp
      val yab = ya - yaa

      temp = za * HEX_40000000
      val zaa = (za + temp) - temp
      val zab = za - zaa

      /* Correct for rounding in division */
      yb = (epsA - yaa * zaa - yaa * zab - yab * zaa - yab * zab) / za

      yb += -epsA * zb / za / za
      yb += epsB / za
    }


    epsA = ya
    epsB = yb

    /* Evaluate polynomial */
    val epsA2 = epsA * epsA

    /*
     yb = -0.09001346640161823
     yb = yb * epsA2 + 0.11110718400605211
     yb = yb * epsA2 + -0.1428571349122913
     yb = yb * epsA2 + 0.19999999999273194
     yb = yb * epsA2 + -0.33333333333333093
     yb = yb * epsA2 * epsA
     */

    yb = 0.07490822288864472
    yb = yb * epsA2 + -0.09088450866185192
    yb = yb * epsA2 + 0.11111095942313305
    yb = yb * epsA2 + -0.1428571423679182
    yb = yb * epsA2 + 0.19999999999923582
    yb = yb * epsA2 + -0.33333333333333287
    yb = yb * epsA2 * epsA


    ya = epsA

    temp = ya + yb
    yb = -(temp - ya - yb)
    ya = temp

    /* Add in effect of epsB.   atan'(x) = 1/(1+x^2) */
    yb += epsB / (1d + epsA * epsA)

    //result = yb + eighths(idx) + ya
    var za = EIGHTHS(idx) + ya
    var zb = -(za - EIGHTHS(idx) - ya)
    temp = za + yb
    zb += -(temp - za - yb)
    za = temp

    var result = za + zb
    var resultb = -(result - za - zb)

    if (leftPlane) {
      // Result is in the left plane
      val pia = 1.5707963267948966 * 2
      val pib = 6.123233995736766E-17 * 2

      za = pia - result
      zb = -(za - pia + result)
      zb += pib - resultb

      result = za + zb
      resultb = -(result - za - zb)
    }


    if (negate ^ leftPlane) {
      result = -result
    }

    result
  }

  /**
   * Two arguments arctangent function
   * @param y ordinate
   * @param x abscissa
   * @return phase angle of point (x,y) between {@code -PI} and {@code PI}
   */
  def atan2(y: Double,  x: Double): Double = {
    if (x != x || y != y) {
      return Double.NaN
    }

    if (y == 0) {
      val result = x * y
      val invx = 1d / x
      val invy = 1d / y

      if (invx == 0) { // X is infinite
        if (x > 0) {
          return y // return +/- 0.0
        } else {
          return copySign(math.Pi, y)
        }
      }

      if (x < 0 || invx < 0) {
        if (y < 0 || invy < 0) {
          return -math.Pi
        } else {
          return math.Pi
        }
      } else {
        return result
      }
    }

    // y cannot now be zero

    if (y == Double.PositiveInfinity) {
      if (x == Double.PositiveInfinity) {
        return math.Pi * F_1_4
      }

      if (x == Double.NegativeInfinity) {
        return math.Pi * F_3_4
      }

      return math.Pi * F_1_2
    }

    if (y == Double.NegativeInfinity) {
      if (x == Double.PositiveInfinity) {
        return -math.Pi * F_1_4
      }

      if (x == Double.NegativeInfinity) {
        return -math.Pi * F_3_4
      }

      return -math.Pi * F_1_2
    }

    if (x == Double.PositiveInfinity) {
      if (y > 0 || 1 / y > 0) {
        return 0d
      }

      if (y < 0 || 1 / y < 0) {
        return -0d
      }
    }

    if (x == Double.NegativeInfinity)
    {
      if (y > 0.0 || 1 / y > 0.0) {
        return math.Pi
      }

      if (y < 0 || 1 / y < 0) {
        return -math.Pi
      }
    }

    // Neither y nor x can be infinite or NAN here

    if (x == 0) {
      if (y > 0 || 1 / y > 0) {
        return math.Pi * F_1_2
      }

      if (y < 0 || 1 / y < 0) {
        return -math.Pi * F_1_2
      }
    }

    // Compute ratio r = y/x
    val r = y / x
    if (java.lang.Double.isInfinite(r)) { // bypass calculations that can create NaN
      return atan(r, 0, x < 0)
    }

    var ra = doubleHighPart(r)
    var rb = r - ra

    // Split x
    val xa = doubleHighPart(x)
    val xb = x - xa

    rb += (y - ra * xa - ra * xb - rb * xa - rb * xb) / x

    val temp = ra + rb
    rb = -(temp - ra - rb)
    ra = temp

    if (ra == 0) { // Fix up the sign so atan works correctly
      ra = copySign(0d, y)
    }

    // Call atan
    val result = atan(ra, rb, x < 0)

    return result
  }

  /** Compute the arc sine of a number.
   * @param x number on which evaluation is done
   * @return arc sine of x
   */
  def asin(x: Double): Double = {
    if (x != x) {
      return Double.NaN
    }

    if (x > 1.0 || x < -1.0) {
      return Double.NaN
    }

    if (x == 1.0) {
      return math.Pi/2.0
    }

    if (x == -1.0) {
      return -math.Pi/2.0
    }

    if (x == 0.0) { // Matches +/- 0.0 return correct sign
      return x
    }

    /* Compute asin(x) = atan(x/sqrt(1-x*x)) */

    /* Split x */
    var temp = x * HEX_40000000
    val xa = x + temp - temp
    val xb = x - xa

    /* Square it */
    var ya = xa*xa
    var yb = xa*xb*2.0 + xb*xb

    /* Subtract from 1 */
    ya = -ya
    yb = -yb

    var za = 1.0 + ya
    var zb = -(za - 1.0 - ya)

    temp = za + yb
    zb += -(temp - za - yb)
    za = temp

    /* Square root */
    val y = sqrt(za)
    temp = y * HEX_40000000
    ya = y + temp - temp
    yb = y - ya

    /* Extend precision of sqrt */
    yb += (za - ya*ya - 2*ya*yb - yb*yb) / (2.0*y)

    /* Contribution of zb to sqrt */
    val dx = zb / (2.0*y)

    // Compute ratio r = x/y
    val r = x/y
    temp = r * HEX_40000000
    var ra = r + temp - temp
    var rb = r - ra

    rb += (x - ra*ya - ra*yb - rb*ya - rb*yb) / y  // Correct for rounding in division
    rb += -x * dx / y / y  // Add in effect additional bits of sqrt.

    temp = ra + rb
    rb = -(temp - ra - rb)
    ra = temp

    return atan(ra, rb, false)
  }

  /** Compute the arc cosine of a number.
   * @param x number on which evaluation is done
   * @return arc cosine of x
   */
  def acos(x: Double): Double = {
    if (x != x) {
      return Double.NaN
    }

    if (x > 1.0 || x < -1.0) {
      return Double.NaN
    }

    if (x == -1.0) {
      return math.Pi
    }

    if (x == 1.0) {
      return 0.0
    }

    if (x == 0) {
      return math.Pi/2.0
    }

    /* Compute acos(x) = atan(sqrt(1-x*x)/x) */

    /* Split x */
    var temp = x * HEX_40000000
    val xa = x + temp - temp
    val xb = x - xa

    /* Square it */
    var ya = xa*xa
    var yb = xa*xb*2.0 + xb*xb

    /* Subtract from 1 */
    ya = -ya
    yb = -yb

    var za = 1.0 + ya
    var zb = -(za - 1.0 - ya)

    temp = za + yb
    zb += -(temp - za - yb)
    za = temp

    /* Square root */
    var y = sqrt(za)
    temp = y * HEX_40000000
    ya = y + temp - temp
    yb = y - ya

    /* Extend precision of sqrt */
    yb += (za - ya*ya - 2*ya*yb - yb*yb) / (2.0*y)

    /* Contribution of zb to sqrt */
    yb += zb / (2.0*y)
    y = ya+yb
    yb = -(y - ya - yb)

    // Compute ratio r = y/x
    val r = y/x

    // Did r overflow?
    if (java.lang.Double.isInfinite(r)) { // x is effectively zero
      return math.Pi/2 // so return the appropriate value
    }

    var ra = doubleHighPart(r)
    var rb = r - ra

    rb += (y - ra*xa - ra*xb - rb*xa - rb*xb) / x  // Correct for rounding in division
    rb += yb / x  // Add in effect additional bits of sqrt.

    temp = ra + rb
    rb = -(temp - ra - rb)
    ra = temp

    return atan(ra, rb, x<0)
  }

  /** Compute the cubic root of a number.
   * @param x number on which evaluation is done
   * @return cubic root of x
   */
  def cbrt(_x: Double): Double = {
    var x = _x
    /* Convert input Double to bits */
    var inbits = java.lang.Double.doubleToLongBits(x)
    var exponent = ((inbits >> 52) & 0x7ff).toInt - 1023
    var subnormal = false

    if (exponent == -1023) {
      if (x == 0) {
        return x
      }

      /* Subnormal, so normalize */
      subnormal = true
      x *= 1.8014398509481984E16  // 2^54
      inbits = java.lang.Double.doubleToLongBits(x)
      exponent = ((inbits >> 52) & 0x7ff).toInt - 1023
    }

    if (exponent == 1024) {
      // Nan or infinity.  Don't care which.
      return x
    }

    /* Divide the exponent by 3 */
    val exp3 = exponent / 3

    /* p2 will be the nearest power of 2 to x with its exponent divided by 3 */
    val p2 = java.lang.Double.longBitsToDouble((inbits & 0x8000000000000000L) |
                                               ((exp3 + 1023) & 0x7ff).toLong << 52)

    /* This will be a number between 1 and 2 */
    val mant = java.lang.Double.longBitsToDouble((inbits & 0x000fffffffffffffL) | 0x3ff0000000000000L)

    /* Estimate the cube root of mant by polynomial */
    var est = -0.010714690733195933
    est = est * mant + 0.0875862700108075
    est = est * mant + -0.3058015757857271
    est = est * mant + 0.7249995199969751
    est = est * mant + 0.5039018405998233

    est *= CBRTTWO(exponent % 3 + 2)

    // est should now be good to about 15 bits of precision.   Do 2 rounds of
    // Newton's method to get closer,  this should get us full Double precision
    // Scale down x for the purpose of doing newtons method.  This avoids over/under flows.
    val xs = x / (p2*p2*p2)
    est += (xs - est*est*est) / (3*est*est)
    est += (xs - est*est*est) / (3*est*est)

    // Do one round of Newton's method in extended precision to get the last bit right.
    var temp = est * HEX_40000000
    val ya = est + temp - temp
    val yb = est - ya

    var za = ya * ya
    var zb = ya * yb * 2.0 + yb * yb
    temp = za * HEX_40000000
    val temp2 = za + temp - temp
    zb += za - temp2
    za = temp2

    zb = za * yb + ya * zb + zb * yb
    za = za * ya

    val na = xs - za
    var nb = -(na - xs + za)
    nb -= zb

    est += (na+nb)/(3*est*est)

    /* Scale by a power of two, so this is exact. */
    est *= p2

    if (subnormal) {
      est *= 3.814697265625E-6  // 2^-18
    }

    return est
  }

  /**
   *  Convert degrees to radians, with error of less than 0.5 ULP
   *  @param x angle in degrees
   *  @return x converted into radians
   */
  def toRadians(x: Double): Double = {
    if (java.lang.Double.isInfinite(x) || x == 0.0) { // Matches +/- 0.0 return correct sign
      return x
    }

    // These are PI/180 split into high and low order bits
    val facta = 0.01745329052209854
    val factb = 1.997844754509471E-9

    val xa = doubleHighPart(x)
    val xb = x - xa

    var result = xb * factb + xb * facta + xa * factb + xa * facta
    if (result == 0) {
      result = result * x // ensure correct sign if calculation underflows
    }
    return result
  }

  /**
   *  Convert radians to degrees, with error of less than 0.5 ULP
   *  @param x angle in radians
   *  @return x converted into degrees
   */
  def toDegrees(x: Double): Double = {
    if (java.lang.Double.isInfinite(x) || x == 0.0) { // Matches +/- 0.0 return correct sign
      return x
    }

    // These are 180/PI split into high and low order bits
    val facta = 57.2957763671875
    val factb = 3.145894820876798E-6

    val xa = doubleHighPart(x)
    val xb = x - xa

    return xb * factb + xb * facta + xa * factb + xa * facta
  }

  /**
   * Absolute value.
   * @param x number from which absolute value is requested
   * @return abs(x)
   */
  def abs(x: Int): Int = {
    if (x < 0) -x else x
  }

  /**
   * Absolute value.
   * @param x number from which absolute value is requested
   * @return abs(x)
   */
  def abs(x: Long): Long = {
    if (x < 0l) -x else x
  }

  /**
   * Absolute value.
   * @param x number from which absolute value is requested
   * @return abs(x)
   */
  def abs(x: Float): Float = {
    if (x < 0.0f) -x else if (x == 0.0f) 0.0f else x // -0.0 => +0.0
  }

  /**
   * Absolute value.
   * @param x number from which absolute value is requested
   * @return abs(x)
   */
  def abs(x: Double): Double = {
    if (x < 0.0) -x else if (x == 0.0) 0.0 else x // -0.0 => +0.0
  }

  /**
   * Compute least significant bit (Unit in Last Position) for a number.
   * @param x number from which ulp is requested
   * @return ulp(x)
   */
  def ulp(x: Double): Double = {
    if (java.lang.Double.isInfinite(x)) {
      return Double.PositiveInfinity
    }
    abs(x - java.lang.Double.longBitsToDouble(java.lang.Double.doubleToLongBits(x) ^ 1))
  }

  /**
   * Compute least significant bit (Unit in Last Position) for a number.
   * @param x number from which ulp is requested
   * @return ulp(x)
   */
  def ulp(x: Float): Float = {
    if (java.lang.Float.isInfinite(x)) {
      return Float.PositiveInfinity
    }
    return abs(x - java.lang.Float.intBitsToFloat(java.lang.Float.floatToIntBits(x) ^ 1))
  }

  /**
   * Multiply a Double number by a power of 2.
   * @param d number to multiply
   * @param n power of 2
   * @return d &times; 2<sup>n</sup>
   */
  def scalb(d: Double, n: Int): Double = {

    // first simple and fast handling when 2^n can be represented using normal numbers
    if ((n > -1023) && (n < 1024)) {
      return d * java.lang.Double.longBitsToDouble(((n + 1023).toLong) << 52)
    }

    // handle special cases
    if (java.lang.Double.isNaN(d) || java.lang.Double.isInfinite(d) || (d == 0)) {
      return d
    }
    if (n < -2098) {
      return if (d > 0) 0.0 else -0.0
    }
    if (n > 2097) {
      return if (d > 0) Double.PositiveInfinity else Double.NegativeInfinity
    }

    // decompose d
    val bits = java.lang.Double.doubleToLongBits(d)
    val sign = bits & 0x8000000000000000L
    val  exponent   = (bits >>> 52).toInt & 0x7ff
    var mantissa   = bits & 0x000fffffffffffffL

    // compute scaled exponent
    var scaledExponent = exponent + n

    if (n < 0) {
      // we are really in the case n <= -1023
      if (scaledExponent > 0) {
        // both the input and the result are normal numbers, we only adjust the exponent
        return java.lang.Double.longBitsToDouble(sign | ((scaledExponent).toLong << 52) | mantissa)
      } else if (scaledExponent > -53) {
        // the input is a normal number and the result is a subnormal number

        // recover the hidden mantissa bit
        mantissa = mantissa | (1L << 52)

        // scales down complete mantissa, hence losing least significant bits
        val mostSignificantLostBit = mantissa & (1L << (-scaledExponent))
        mantissa = mantissa >>> (1 - scaledExponent)
        if (mostSignificantLostBit != 0) {
          // we need to add 1 bit to round up the result
          mantissa += 1
        }
        return java.lang.Double.longBitsToDouble(sign | mantissa)

      } else {
        // no need to compute the mantissa, the number scales down to 0
        return if (sign == 0L) 0.0 else -0.0
      }
    } else {
      // we are really in the case n >= 1024
      if (exponent == 0) {

        // the input number is subnormal, normalize it
        while ((mantissa >>> 52) != 1) {
          mantissa = mantissa << 1
          scaledExponent -= 1
        }
        scaledExponent += 1
        mantissa = mantissa & 0x000fffffffffffffL

        if (scaledExponent < 2047) {
          return java.lang.Double.longBitsToDouble(sign | (scaledExponent.toLong << 52) | mantissa)
        } else {
          return if (sign == 0L) Double.PositiveInfinity else Double.NegativeInfinity
        }

      } else if (scaledExponent < 2047) {
        return java.lang.Double.longBitsToDouble(sign | (scaledExponent.toLong << 52) | mantissa)
      } else {
        return if (sign == 0L) Double.PositiveInfinity else Double.NegativeInfinity
      }
    }

  }

  /**
   * Multiply a Float number by a power of 2.
   * @param f number to multiply
   * @param n power of 2
   * @return f &times; 2<sup>n</sup>
   */
  def scalb(f: Float, n: Int): Float = {
    // first simple and fast handling when 2^n can be represented using normal numbers
    if ((n > -127) && (n < 128)) {
      f * java.lang.Float.intBitsToFloat((n + 127) << 23)
    }

    // handle special cases
    if (java.lang.Float.isNaN(f) || java.lang.Float.isInfinite(f) || (f == 0f)) {
      return f
    }
    if (n < -277) {
      return if (f > 0) 0.0f else -0.0f
    }
    if (n > 276) {
      return if (f > 0) Float.PositiveInfinity else Float.NegativeInfinity
    }

    // decompose f
    val bits = java.lang.Float.floatToIntBits(f)
    val sign = bits & 0x80000000
    val exponent  = (bits >>> 23) & 0xff
    var mantissa   = bits & 0x007fffff

    // compute scaled exponent
    var scaledExponent = exponent + n

    if (n < 0) {
      // we are really in the case n <= -127
      if (scaledExponent > 0) {
        // both the input and the result are normal numbers, we only adjust the exponent
        return java.lang.Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa)
      } else if (scaledExponent > -24) {
        // the input is a normal number and the result is a subnormal number

        // recover the hidden mantissa bit
        mantissa = mantissa | (1 << 23)

        // scales down complete mantissa, hence losing least significant bits
        val mostSignificantLostBit = mantissa & (1 << (-scaledExponent))
        mantissa = mantissa >>> (1 - scaledExponent)
        if (mostSignificantLostBit != 0) {
          // we need to add 1 bit to round up the result
          mantissa += 1
        }
        return java.lang.Float.intBitsToFloat(sign | mantissa)

      } else {
        // no need to compute the mantissa, the number scales down to 0
        return if (sign == 0) 0.0f else -0.0f
      }
    } else {
      // we are really in the case n >= 128
      if (exponent == 0) {

        // the input number is subnormal, normalize it
        while ((mantissa >>> 23) != 1) {
          mantissa = mantissa << 1
          scaledExponent -= 1
        }
        scaledExponent += 1
        mantissa = mantissa & 0x007fffff

        if (scaledExponent < 255) {
          return java.lang.Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa)
        } else {
          return if (sign == 0) Float.PositiveInfinity else Float.NegativeInfinity
        }

      } else if (scaledExponent < 255) {
        return java.lang.Float.intBitsToFloat(sign | (scaledExponent << 23) | mantissa)
      } else {
        return if (sign == 0) Float.PositiveInfinity else Float.NegativeInfinity
      }
    }

  }

  /**
   * Get the next machine representable number after a number, moving
   * in the direction of another number.
   * <p>
   * The ordering is as follows (increasing):
   * <ul>
   * <li>-INFINITY</li>
   * <li>-MAX_VALUE</li>
   * <li>-MIN_VALUE</li>
   * <li>-0.0</li>
   * <li>+0.0</li>
   * <li>+MIN_VALUE</li>
   * <li>+MAX_VALUE</li>
   * <li>+INFINITY</li>
   * <li></li>
   * <p>
   * If arguments compare equal, then the second argument is returned.
   * <p>
   * If {@code direction} is greater than {@code d},
   * the smallest machine representable number strictly greater than
   * {@code d} is returned; if less, then the largest representable number
   * strictly less than {@code d} is returned.</p>
   * <p>
   * If {@code d} is infinite and direction does not
   * bring it back to finite numbers, it is returned unchanged.</p>
   *
   * @param d base number
   * @param direction (the only important thing is whether
   * {@code direction} is greater or smaller than {@code d})
   * @return the next machine representable number in the specified direction
   */
  def nextAfter(d: Double, direction: Double): Double = {

    // handling of some important special cases
    if (java.lang.Double.isNaN(d) || java.lang.Double.isNaN(direction)) {
      return Double.NaN
    } else if (d == direction) {
      return direction
    } else if (java.lang.Double.isInfinite(d)) {
      if (d < 0) -Double.MaxValue else Double.MaxValue
    } else if (d == 0) {
      if (direction < 0) -Double.MinValue else Double.MinValue
    }
    // special cases MAX_VALUE to infinity and  MIN_VALUE to 0
    // are handled just as normal numbers

    val bits = java.lang.Double.doubleToLongBits(d)
    val sign = bits & 0x8000000000000000L
    if ((direction < d) ^ (sign == 0L)) {
      return java.lang.Double.longBitsToDouble(sign | ((bits & 0x7fffffffffffffffL) + 1))
    } else {
      return java.lang.Double.longBitsToDouble(sign | ((bits & 0x7fffffffffffffffL) - 1))
    }

  }

  /**
   * Get the next machine representable number after a number, moving
   * in the direction of another number.
   * <p>
   * The ordering is as follows (increasing):
   * <ul>
   * <li>-INFINITY</li>
   * <li>-MAX_VALUE</li>
   * <li>-MIN_VALUE</li>
   * <li>-0.0</li>
   * <li>+0.0</li>
   * <li>+MIN_VALUE</li>
   * <li>+MAX_VALUE</li>
   * <li>+INFINITY</li>
   * <li></li>
   * <p>
   * If arguments compare equal, then the second argument is returned.
   * <p>
   * If {@code direction} is greater than {@code f},
   * the smallest machine representable number strictly greater than
   * {@code f} is returned; if less, then the largest representable number
   * strictly less than {@code f} is returned.</p>
   * <p>
   * If {@code f} is infinite and direction does not
   * bring it back to finite numbers, it is returned unchanged.</p>
   *
   * @param f base number
   * @param direction (the only important thing is whether
   * {@code direction} is greater or smaller than {@code f})
   * @return the next machine representable number in the specified direction
   */
  def nextAfter(f: Float, direction: Double): Float = {

    // handling of some important special cases
    if (java.lang.Double.isNaN(f) || java.lang.Double.isNaN(direction)) {
      return Float.NaN
    } else if (f == direction) {
      return direction.toFloat
    } else if (java.lang.Float.isInfinite(f)) {
      return if (f < 0f) -Float.MaxValue else Float.MaxValue
    } else if (f == 0f) {
      return if (direction < 0) -Float.MinValue else Float.MinValue
    }
    // special cases MAX_VALUE to infinity and  MIN_VALUE to 0
    // are handled just as normal numbers

    val bits = java.lang.Float.floatToIntBits(f)
    val sign = bits & 0x80000000
    if ((direction < f) ^ (sign == 0)) {
      java.lang.Float.intBitsToFloat(sign | ((bits & 0x7fffffff) + 1))
    } else {
      java.lang.Float.intBitsToFloat(sign | ((bits & 0x7fffffff) - 1))
    }

  }

  /** Get the largest whole number smaller than x.
   * @param x number from which floor is requested
   * @return a Double number f such that f is an integer f <= x < f + 1.0
   */
  def floor(x: Double): Double = {
    var y = 0L

    if (x != x) { // NaN
      return x
    }

    if (x >= TWO_POWER_52 || x <= -TWO_POWER_52) {
      return x
    }

    y =  x.toLong
    if (x < 0 && y != x) {
      y -= 1
    }

    if (y == 0) {
      return x*y
    }

    return y
  }

  /** Get the smallest whole number larger than x.
   * @param x number from which ceil is requested
   * @return a Double number c such that c is an integer c - 1.0 < x <= c
   */
  def ceil(x: Double): Double = {
    var y = 0D

    if (x != x) { // NaN
      return x
    }

    y = floor(x)
    if (y == x) {
      return y
    }

    y += 1.0

    if (y == 0) {
      return x*y
    }

    return y
  }

  /** Get the whole number that is the nearest to x, or the even one if x is exactly half way between two integers.
   * @param x number from which nearest whole number is requested
   * @return a Double number r such that r is an integer r - 0.5 <= x <= r + 0.5
   */
  def rint(x: Double): Double = {
    val y = floor(x)
    val d = x - y

    if (d > 0.5) {
      if (y == -1.0) {
        return -0.0 // Preserve sign of operand
      }
      return y+1.0
    }
    if (d < 0.5) {
      return y
    }

    /* half way, round to even */
    val z = y.toLong
    return if ((z & 1) == 0) y else y + 1.0
  }

  /** Get the closest long to x.
   * @param x number from which closest long is requested
   * @return closest long to x
   */
  def round(x: Double): Long = {
    floor(x + 0.5).toLong
  }

  /** Get the closest int to x.
   * @param x number from which closest int is requested
   * @return closest int to x
   */
  def round(x: Float): Int = {
    floor(x + 0.5f).toInt
  }

  /** Compute the minimum of two values
   * @param a first value
   * @param b second value
   * @return a if a is lesser or equal to b, b otherwise
   */
  def min(a: Int, b: Int): Int = {
    if (a <= b) a else b
  }

  /** Compute the minimum of two values
   * @param a first value
   * @param b second value
   * @return a if a is lesser or equal to b, b otherwise
   */
  def min(a: Long, b: Long): Long = {
    if (a <= b) a else b
  }

  /** Compute the minimum of two values
   * @param a first value
   * @param b second value
   * @return a if a is lesser or equal to b, b otherwise
   */
  def min(a: Float, b: Float): Float = {
    if (a > b) {
      return b
    }
    if (a < b) {
      return a
    }
    /* if either arg is NaN, return NaN */
    if (a != b) {
      return Float.NaN
    }
    /* min(+0.0,-0.0) == -0.0 */
    /* 0x80000000 == Float.floatToRawIntBits(-0.0d) */
    val bits = java.lang.Float.floatToRawIntBits(a)
    if (bits == 0x80000000) {
      return a
    }
    return b
  }

  /** Compute the minimum of two values
   * @param a first value
   * @param b second value
   * @return a if a is lesser or equal to b, b otherwise
   */
  def min(a: Double, b: Double): Double = {
    if (a > b) {
      return b
    }
    if (a < b) {
      return a
    }
    /* if either arg is NaN, return NaN */
    if (a != b) {
      return Double.NaN
    }
    /* min(+0.0,-0.0) == -0.0 */
    /* 0x8000000000000000L == java.lang.Double.doubleToRawLongBits(-0.0d) */
    val bits = java.lang.Double.doubleToRawLongBits(a)
    if (bits == 0x8000000000000000L) {
      return a
    }
    return b
  }

  /** Compute the maximum of two values
   * @param a first value
   * @param b second value
   * @return b if a is lesser or equal to b, a otherwise
   */
  def max(a: Int, b: Int): Int = {
    if (a <= b) b else a
  }

  /** Compute the maximum of two values
   * @param a first value
   * @param b second value
   * @return b if a is lesser or equal to b, a otherwise
   */
  def max(a: Long, b: Long): Long = {
    if (a <= b) b else a
  }

  /** Compute the maximum of two values
   * @param a first value
   * @param b second value
   * @return b if a is lesser or equal to b, a otherwise
   */
  def max(a: Float, b: Float): Float = {
    if (a > b) {
      return a
    }
    if (a < b) {
      return b
    }
    /* if either arg is NaN, return NaN */
    if (a != b) {
      return Float.NaN
    }
    /* min(+0.0,-0.0) == -0.0 */
    /* 0x80000000 == Float.floatToRawIntBits(-0.0d) */
    val bits = java.lang.Float.floatToRawIntBits(a)
    if (bits == 0x80000000) {
      return b
    }
    return a
  }

  /** Compute the maximum of two values
   * @param a first value
   * @param b second value
   * @return b if a is lesser or equal to b, a otherwise
   */
  def max(a: Double, b: Double): Double = {
    if (a > b) {
      return a
    }
    if (a < b) {
      return b
    }
    /* if either arg is NaN, return NaN */
    if (a != b) {
      return Double.NaN
    }
    /* min(+0.0,-0.0) == -0.0 */
    /* 0x8000000000000000L == java.lang.Double.doubleToRawLongBits(-0.0d) */
    val bits = java.lang.Double.doubleToRawLongBits(a)
    if (bits == 0x8000000000000000L) {
      return b
    }
    return a
  }

  /**
   * Returns the hypotenuse of a triangle with sides {@code x} and {@code y}
   * - sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)<br/>
   * avoiding intermediate overflow or underflow.
   *
   * <ul>
   * <li> If either argument is infinite, then the result is positive infinity.</li>
   * <li> else, if either argument is NaN then the result is NaN.</li>
   * </ul>
   *
   * @param x a value
   * @param y a value
   * @return sqrt(<i>x</i><sup>2</sup>&nbsp;+<i>y</i><sup>2</sup>)
   */
  def hypot(x: Double, y: Double): Double = {
    if (java.lang.Double.isInfinite(x) || java.lang.Double.isInfinite(y)) {
      Double.PositiveInfinity
    } else if (java.lang.Double.isNaN(x) || java.lang.Double.isNaN(y)) {
      Double.NaN
    } else {

      val expX = getExponent(x)
      val expY = getExponent(y)
      if (expX > expY + 27) {
        // y is neglectible with respect to x
        abs(x)
      } else if (expY > expX + 27) {
        // x is neglectible with respect to y
        abs(y)
      } else {

        // find an intermediate scale to avoid both overflow and underflow
        val middleExp = (expX + expY) / 2

        // scale parameters without losing precision
        val scaledX = scalb(x, -middleExp)
        val scaledY = scalb(y, -middleExp)

        // compute scaled hypotenuse
        val scaledH = sqrt(scaledX * scaledX + scaledY * scaledY)

        // remove scaling
        scalb(scaledH, middleExp)
      }
    }
  }

  /**
   * Computes the remainder as prescribed by the IEEE 754 standard.
   * The remainder value is mathematically equal to {@code x - y*n}
   * where {@code n} is the mathematical integer closest to the exact mathematical value
   * of the quotient {@code x/y}.
   * If two mathematical integers are equally close to {@code x/y} then
   * {@code n} is the integer that is even.
   * <p>
   * <ul>
   * <li>If either operand is NaN, the result is NaN.</li>
   * <li>If the result is not NaN, the sign of the result equals the sign of the dividend.</li>
   * <li>If the dividend is an infinity, or the divisor is a zero, or both, the result is NaN.</li>
   * <li>If the dividend is finite and the divisor is an infinity, the result equals the dividend.</li>
   * <li>If the dividend is a zero and the divisor is finite, the result equals the dividend.</li>
   * </ul>
   * <p><b>Note:</b> this implementation currently delegates to {@link StrictMath#IEEEremainder}
   * @param dividend the number to be divided
   * @param divisor the number by which to divide
   * @return the remainder, rounded
   */
   def IEEEremainder(dividend: Double, divisor: Double): Double = {
      return StrictMath.IEEEremainder(dividend, divisor) // TODO provide our own implementation
    }

   /**
    * Returns the first argument with the sign of the second argument.
    * A NaN {@code sign} argument is treated as positive.
    *
    * @param magnitude the value to return
    * @param sign the sign for the returned value
    * @return the magnitude with the same sign as the {@code sign} argument
    */
   def copySign(magnitude: Double, sign: Double): Double = {
      val m = java.lang.Double.doubleToLongBits(magnitude)
      val s = java.lang.Double.doubleToLongBits(sign)
      if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
        return magnitude
      }
      return -magnitude // flip sign
    }

   /**
    * Returns the first argument with the sign of the second argument.
    * A NaN {@code sign} argument is treated as positive.
    *
    * @param magnitude the value to return
    * @param sign the sign for the returned value
    * @return the magnitude with the same sign as the {@code sign} argument
    */
   def copySign(magnitude: Float, sign: Float): Float ={
      val m = java.lang.Float.floatToIntBits(magnitude)
      val s = java.lang.Float.floatToIntBits(sign)
      if ((m >= 0 && s >= 0) || (m < 0 && s < 0)) { // Sign is currently OK
        magnitude
      } else {
        -magnitude // flip sign
      }
    }

   /**
    * Return the exponent of a Double number, removing the bias.
    * <p>
    * For Double numbers of the form 2<sup>x</sup>, the unbiased
    * exponent is exactly x.
    * </p>
    * @param d number from which exponent is requested
    * @return exponent for d in IEEE754 representation, without bias
    */
   def getExponent(d: Double): Int = {
      ((java.lang.Double.doubleToLongBits(d) >>> 52) & 0x7ff).toInt - 1023
    }

   /**
    * Return the exponent of a Float number, removing the bias.
    * <p>
    * For Float numbers of the form 2<sup>x</sup>, the unbiased
    * exponent is exactly x.
    * </p>
    * @param f number from which exponent is requested
    * @return exponent for d in IEEE754 representation, without bias
    */
   def getExponent(f: Float): Int = {
      ((java.lang.Float.floatToIntBits(f) >>> 23) & 0xff) - 127
    }

   /**
    * Print out contents of arrays, and check the length.
    * <p>used to generate the preset arrays originally.</p>
    * @param a unused
    */
   def main(a: Array[String]) {
      val out = System.out
      FastMathCalc.printarray(out, "EXP_INT_TABLE_A", EXP_INT_TABLE_LEN, ExpIntTable.EXP_INT_TABLE_A)
      FastMathCalc.printarray(out, "EXP_INT_TABLE_B", EXP_INT_TABLE_LEN, ExpIntTable.EXP_INT_TABLE_B)
      FastMathCalc.printarray(out, "EXP_FRAC_TABLE_A", EXP_FRAC_TABLE_LEN, ExpFracTable.EXP_FRAC_TABLE_A)
      FastMathCalc.printarray(out, "EXP_FRAC_TABLE_B", EXP_FRAC_TABLE_LEN, ExpFracTable.EXP_FRAC_TABLE_B)
      FastMathCalc.printarray(out, "LN_MANT",LN_MANT_LEN, lnMant.LN_MANT)
      FastMathCalc.printarray(out, "SINE_TABLE_A", SINE_TABLE_LEN, SINE_TABLE_A)
      FastMathCalc.printarray(out, "SINE_TABLE_B", SINE_TABLE_LEN, SINE_TABLE_B)
      FastMathCalc.printarray(out, "COSINE_TABLE_A", SINE_TABLE_LEN, COSINE_TABLE_A)
      FastMathCalc.printarray(out, "COSINE_TABLE_B", SINE_TABLE_LEN, COSINE_TABLE_B)
      FastMathCalc.printarray(out, "TANGENT_TABLE_A", SINE_TABLE_LEN, TANGENT_TABLE_A)
      FastMathCalc.printarray(out, "TANGENT_TABLE_B", SINE_TABLE_LEN, TANGENT_TABLE_B)
    }

   /** Enclose large data table in nested static class so it's only loaded on first access. */
   private object ExpIntTable {
      /** Exponential evaluated at integer values,
       * exp(x) =  expIntTableA[x + EXP_INT_TABLE_MAX_INDEX] + expIntTableB[x+EXP_INT_TABLE_MAX_INDEX].
       */
      var EXP_INT_TABLE_A: Array[Double] = _
      /** Exponential evaluated at integer values,
       * exp(x) =  expIntTableA[x + EXP_INT_TABLE_MAX_INDEX] + expIntTableB[x+EXP_INT_TABLE_MAX_INDEX]
       */
      var EXP_INT_TABLE_B: Array[Double] = _
    
      if (RECOMPUTE_TABLES_AT_RUNTIME) {
        EXP_INT_TABLE_A = new Array[Double](FastMath.EXP_INT_TABLE_LEN)
        EXP_INT_TABLE_B = new Array[Double](FastMath.EXP_INT_TABLE_LEN)

        val tmp = new Array[Double](2)
        val recip = new Array[Double](2)

        // Populate expIntTable
        var i = 0
        while (i < FastMath.EXP_INT_TABLE_MAX_INDEX) {
          FastMathCalc.expint(i, tmp)
          EXP_INT_TABLE_A(i + FastMath.EXP_INT_TABLE_MAX_INDEX) = tmp(0)
          EXP_INT_TABLE_B(i + FastMath.EXP_INT_TABLE_MAX_INDEX) = tmp(1)

          if (i != 0) {
            // Negative integer powers
            FastMathCalc.splitReciprocal(tmp, recip)
            EXP_INT_TABLE_A(FastMath.EXP_INT_TABLE_MAX_INDEX - i) = recip(0)
            EXP_INT_TABLE_B(FastMath.EXP_INT_TABLE_MAX_INDEX - i) = recip(1)
          }
          i += 1
        }
      } else if (LOAD_RESOURCES) {
        val expInt: Array[Array[Double]] = FastMathResources.loadExpInt
        EXP_INT_TABLE_A = expInt(0)
        EXP_INT_TABLE_B = expInt(1)
      } else {
        EXP_INT_TABLE_A = FastMathLiteralArrays.loadExpIntA
        EXP_INT_TABLE_B = FastMathLiteralArrays.loadExpIntB
      }
    }

   /** Enclose large data table in nested static class so it's only loaded on first access. */
   private object ExpFracTable {
      /** Exponential over the range of 0 - 1 in increments of 2^-10
       * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
       * 1024 = 2^10
       */
      var EXP_FRAC_TABLE_A: Array[Double] = _
      /** Exponential over the range of 0 - 1 in increments of 2^-10
       * exp(x/1024) =  expFracTableA[x] + expFracTableB[x].
       */
      var EXP_FRAC_TABLE_B: Array[Double] = _

      if (RECOMPUTE_TABLES_AT_RUNTIME) {
        EXP_FRAC_TABLE_A = new Array[Double](FastMath.EXP_FRAC_TABLE_LEN)
        EXP_FRAC_TABLE_B = new Array[Double](FastMath.EXP_FRAC_TABLE_LEN)

        val tmp = new Array[Double](2)

        // Populate expFracTable
        val factor = 1d / (EXP_FRAC_TABLE_LEN - 1)
        var i = 0
        while (i < EXP_FRAC_TABLE_A.length) {
          FastMathCalc.slowexp(i * factor, tmp)
          EXP_FRAC_TABLE_A(i) = tmp(0)
          EXP_FRAC_TABLE_B(i) = tmp(1)
          i += 1
        }
      } else if (LOAD_RESOURCES) {
        val expFrac: Array[Array[Double]] = FastMathResources.loadExpFrac
        EXP_FRAC_TABLE_A = expFrac(0)
        EXP_FRAC_TABLE_B = expFrac(1)
      } else {
        EXP_FRAC_TABLE_A = FastMathLiteralArrays.loadExpFracA
        EXP_FRAC_TABLE_B = FastMathLiteralArrays.loadExpFracB
      }
    }

   /** Enclose large data table in nested static class so it's only loaded on first access. */
   private object lnMant {
      /** Extended precision logarithm table over the range 1 - 2 in increments of 2^-10. */
      var LN_MANT: Array[Array[Double]] = _

      if (RECOMPUTE_TABLES_AT_RUNTIME) {
        LN_MANT = new Array[Array[Double]](FastMath.LN_MANT_LEN)

        // Populate lnMant table
        var i = 0
        while (i < LN_MANT.length) {
          val d = java.lang.Double.longBitsToDouble((i.toLong << 42) | 0x3ff0000000000000L)
          LN_MANT(i) = FastMathCalc.slowLog(d)
          i += 1
        }
      } else if (LOAD_RESOURCES) {
        LN_MANT = FastMathResources.loadLnMant
      } else {
        LN_MANT = FastMathLiteralArrays.loadLnMant
      }
    }
   }

