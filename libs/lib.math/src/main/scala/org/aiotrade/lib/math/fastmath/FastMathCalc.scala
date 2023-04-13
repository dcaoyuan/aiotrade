package org.aiotrade.lib.math.fastmath


import java.io.PrintStream
import org.aiotrade.lib.math.CardinalityException

/**
 *
 * Ported by Caoyuan Deng from Java version at org.apache.commons.math3.util
 */
protected object FastMathCalc {

  /**
   * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
   * Equivalent to 2^30.
   */
  private val HEX_40000000 = 0x40000000L // 1073741824L

  /** Factorial table, for Taylor series expansions. 0!, 1!, 2!, ... 19! */
  private val FACT = Array[Double](
    +1.0d,                        // 0
    +1.0d,                        // 1
    +2.0d,                        // 2
    +6.0d,                        // 3
    +24.0d,                       // 4
    +120.0d,                      // 5
    +720.0d,                      // 6
    +5040.0d,                     // 7
    +40320.0d,                    // 8
    +362880.0d,                   // 9
    +3628800.0d,                  // 10
    +39916800.0d,                 // 11
    +479001600.0d,                // 12
    +6227020800.0d,               // 13
    +87178291200.0d,              // 14
    +1307674368000.0d,            // 15
    +20922789888000.0d,           // 16
    +355687428096000.0d,          // 17
    +6402373705728000.0d,         // 18
    +121645100408832000.0d        // 19
  )

  /** Coefficients for slowLog. */
  private val LN_SPLIT_COEF = Array[Array[Double]](
    Array(2.0, 0.0),
    Array(0.6666666269302368,   3.9736429850260626E-8),
    Array(0.3999999761581421,   2.3841857910019882E-8),
    Array(0.2857142686843872,   1.7029898543501842E-8),
    Array(0.2222222089767456,   1.3245471311735498E-8),
    Array(0.1818181574344635,   2.4384203044354907E-8),
    Array(0.1538461446762085,   9.140260083262505E-9 ),
    Array(0.13333332538604736,  9.220590270857665E-9 ),
    Array(0.11764700710773468,  1.2393345855018391E-8),
    Array(0.10526403784751892,  8.251545029714408E-9 ),
    Array(0.0952233225107193,   1.2675934823758863E-8),
    Array(0.08713622391223907,  1.1430250008909141E-8),
    Array(0.07842259109020233,  2.404307984052299E-9 ),
    Array(0.08371849358081818,  1.176342548272881E-8 ),
    Array(0.030589580535888672, 1.2958646899018938E-9),
    Array(0.14982303977012634,  1.225743062930824E-8 )
  )

  /** Table start declaration. */
  private val TABLE_START_DECL = "    {"

  /** Table end declaration. */
  private val TABLE_END_DECL   = "    };"

  /** Build the sine and cosine tables.
   * @param SINE_TABLE_A table of the most significant part of the sines
   * @param SINE_TABLE_B table of the least significant part of the sines
   * @param COSINE_TABLE_A table of the most significant part of the cosines
   * @param COSINE_TABLE_B table of the most significant part of the cosines
   * @param SINE_TABLE_LEN length of the tables
   * @param TANGENT_TABLE_A table of the most significant part of the tangents
   * @param TANGENT_TABLE_B table of the most significant part of the tangents
   */
  private def buildSinCosTables(SINE_TABLE_A: Array[Double], SINE_TABLE_B: Array[Double],
                                COSINE_TABLE_A: Array[Double], COSINE_TABLE_B: Array[Double],
                                SINE_TABLE_LEN: Int, TANGENT_TABLE_A: Array[Double], TANGENT_TABLE_B: Array[Double]) {
    val result = new Array[Double](2)

    /* Use taylor series for 0 <= x <= 6/8 */
    var i = 0
    while (i < 7) {
      val x = i / 8.0

      slowSin(x, result)
      SINE_TABLE_A(i) = result(0)
      SINE_TABLE_B(i) = result(1)

      slowCos(x, result)
      COSINE_TABLE_A(i) = result(0)
      COSINE_TABLE_B(i) = result(1)
      i += 1
    }

    /* Use angle addition formula to complete table to 13/8, just beyond pi/2 */
    i = 7
    while (i < SINE_TABLE_LEN) {
      val xs = new Array[Double](2)
      val ys = new Array[Double](2)
      val as = new Array[Double](2)
      val bs = new Array[Double](2)
      val temps = new Array[Double](2)

      if ( (i & 1) == 0) {
        // Even, use double angle
        xs(0) = SINE_TABLE_A(i / 2)
        xs(1) = SINE_TABLE_B(i / 2)
        ys(0) = COSINE_TABLE_A(i / 2)
        ys(1) = COSINE_TABLE_B(i / 2)

        /* compute sine */
        splitMult(xs, ys, result)
        SINE_TABLE_A(i) = result(0) * 2.0
        SINE_TABLE_B(i) = result(1) * 2.0

        /* Compute cosine */
        splitMult(ys, ys, as)
        splitMult(xs, xs, temps)
        temps(0) = -temps(0)
        temps(1) = -temps(1)
        splitAdd(as, temps, result)
        COSINE_TABLE_A(i) = result(0)
        COSINE_TABLE_B(i) = result(1)
      } else {
        xs(0) = SINE_TABLE_A(i / 2)
        xs(1) = SINE_TABLE_B(i / 2)
        ys(0) = COSINE_TABLE_A(i / 2)
        ys(1) = COSINE_TABLE_B(i / 2)
        as(0) = SINE_TABLE_A(i / 2 + 1)
        as(1) = SINE_TABLE_B(i / 2 + 1)
        bs(0) = COSINE_TABLE_A(i / 2 + 1)
        bs(1) = COSINE_TABLE_B(i / 2 + 1)

        /* compute sine */
        splitMult(xs, bs, temps)
        splitMult(ys, as, result)
        splitAdd(result, temps, result)
        SINE_TABLE_A(i) = result(0)
        SINE_TABLE_B(i) = result(1)

        /* Compute cosine */
        splitMult(ys, bs, result)
        splitMult(xs, as, temps)
        temps(0) = -temps(0)
        temps(1) = -temps(1)
        splitAdd(result, temps, result)
        COSINE_TABLE_A(i) = result(0)
        COSINE_TABLE_B(i) = result(1)
      }
      i += 1
    }

    /* Compute tangent = sine/cosine */
    i = 0
    while (i < SINE_TABLE_LEN) {
      val xs = new Array[Double](2)
      val ys = new Array[Double](2)
      val as = new Array[Double](2)

      as(0) = COSINE_TABLE_A(i)
      as(1) = COSINE_TABLE_B(i)

      splitReciprocal(as, ys)

      xs(0) = SINE_TABLE_A(i)
      xs(1) = SINE_TABLE_B(i)

      splitMult(xs, ys, as)

      TANGENT_TABLE_A(i) = as(0)
      TANGENT_TABLE_B(i) = as(1)
      i += 1
    }

  }

  /**
   *  For x between 0 and pi/4 compute cosine using Talor series
   *  cos(x) = 1 - x^2/2! + x^4/4! ...
   * @param x number from which cosine is requested
   * @param result placeholder where to put the result in extended precision
   * (may be null)
   * @return cos(x)
   */
  def slowCos(x: Double, result: Array[Double]): Double = {

    val xs = new Array[Double](2)
    val ys = new Array[Double](2)
    val facts = new Array[Double](2)
    val as = new Array[Double](2)
    split(x, xs)
    ys(0) = 0.0
    ys(1) = 0.0

    var i = FACT.length - 1
    while (i >= 0) {
      splitMult(xs, ys, as)
      ys(0) = as(0)
      ys(1) = as(1)

      if ( (i & 1) != 0) { // skip odd entries
        // continue
      } else {

        split(FACT(i), as)
        splitReciprocal(as, facts)

        if ( (i & 2) != 0 ) { // alternate terms are negative
          facts(0) = -facts(0)
          facts(1) = -facts(1)
        }

        splitAdd(ys, facts, as)
        ys(0) = as(0)
        ys(1) = as(1)
      }
      i -= 1
    }

    if (result != null) {
      result(0) = ys(0)
      result(1) = ys(1)
    }

    ys(0) + ys(1)
  }

  /**
   * For x between 0 and pi/4 compute sine using Taylor expansion:
   * sin(x) = x - x^3/3! + x^5/5! - x^7/7! ...
   * @param x number from which sine is requested
   * @param result placeholder where to put the result in extended precision
   * (may be null)
   * @return sin(x)
   */
  def slowSin(x: Double, result: Array[Double]): Double = {
    val xs = new Array[Double](2)
    val ys = new Array[Double](2)
    val facts = new Array[Double](2)
    val as = new Array[Double](2)
    split(x, xs)
    ys(0) = 0.0
    ys(1) = 0.0

    var i = FACT.length - 1
    while (i >= 0) {
      splitMult(xs, ys, as)
      ys(0) = as(0)
      ys(1) = as(1)

      if ( (i & 1) == 0) { // Ignore even numbers
        // continue
      } else {

        split(FACT(i), as)
        splitReciprocal(as, facts)

        if ( (i & 2) != 0 ) { // alternate terms are negative
          facts(0) = -facts(0)
          facts(1) = -facts(1)
        }

        splitAdd(ys, facts, as)
        ys(0) = as(0)
        ys(1) = as(1)
      }
      i -= 1
    }

    if (result != null) {
      result(0) = ys(0)
      result(1) = ys(1)
    }

    ys(0) + ys(1)
  }


  /**
   *  For x between 0 and 1, returns exp(x), uses extended precision
   *  @param x argument of exponential
   *  @param result placeholder where to place exp(x) split in two terms
   *  for extra precision (i.e. exp(x) = result(0) + result(1)
   *  @return exp(x)
   */
  def slowexp(x: Double, result: Array[Double]): Double = {
    val xs = new Array[Double](2)
    val ys = new Array[Double](2)
    val facts = new Array[Double](2)
    val as = new Array[Double](2)
    split(x, xs)
    ys(0) = 0.0
    ys(1) = 0.0

    var i = FACT.length - 1
    while (i >= 0) {
      splitMult(xs, ys, as)
      ys(0) = as(0)
      ys(1) = as(1)

      split(FACT(i), as)
      splitReciprocal(as, facts)

      splitAdd(ys, facts, as)
      ys(0) = as(0)
      ys(1) = as(1)
      i -= 1
    }

    if (result != null) {
      result(0) = ys(0)
      result(1) = ys(1)
    }

    ys(0) + ys(1)
  }

  /** Compute split(0), split(1) such that their sum is equal to d,
   * and split(0) has its 30 least significant bits as zero.
   * @param d number to split
   * @param split placeholder where to place the result
   */
  private def split(d: Double, split: Array[Double]) {
    if (d < 8e298 && d > -8e298) {
      val a = d * HEX_40000000
      split(0) = (d + a) - a
      split(1) = d - split(0)
    } else {
      val a = d * 9.31322574615478515625E-10
      split(0) = (d + a - d) * HEX_40000000
      split(1) = d - split(0)
    }
  }

  /** Recompute a split.
   * @param a input/out array containing the split, changed
   * on output
   */
  private def resplit(a: Array[Double]) {
    val c = a(0) + a(1)
    val d = -(c - a(0) - a(1))

    if (c < 8e298 && c > -8e298) { // MAGIC NUMBER
      val z = c * HEX_40000000
      a(0) = (c + z) - z
      a(1) = c - a(0) + d
    } else {
      val z = c * 9.31322574615478515625E-10
      a(0) = (c + z - c) * HEX_40000000
      a(1) = c - a(0) + d
    }
  }

  /** Multiply two numbers in split form.
   * @param a first term of multiplication
   * @param b second term of multiplication
   * @param ans placeholder where to put the result
   */
  private def splitMult(a: Array[Double], b: Array[Double], ans: Array[Double]) {
    ans(0) = a(0) * b(0)
    ans(1) = a(0) * b(1) + a(1) * b(0) + a(1) * b(1)

    /* Resplit */
    resplit(ans)
  }

  /** Add two numbers in split form.
   * @param a first term of addition
   * @param b second term of addition
   * @param ans placeholder where to put the result
   */
  private def splitAdd(a: Array[Double], b: Array[Double], ans: Array[Double]) {
    ans(0) = a(0) + b(0)
    ans(1) = a(1) + b(1)

    resplit(ans)
  }

  /** Compute the reciprocal of in.  Use the following algorithm.
   *  in = c + d.
   *  want to find x + y such that x+y = 1/(c+d) and x is much
   *  larger than y and x has several zero bits on the right.
   *
   *  Set b = 1/(2^22),  a = 1 - b.  Thus (a+b) = 1.
   *  Use following identity to compute (a+b)/(c+d)
   *
   *  (a+b)/(c+d)  =   a/c   +    (bc - ad) / (c^2 + cd)
   *  set x = a/c  and y = (bc - ad) / (c^2 + cd)
   *  This will be close to the right answer, but there will be
   *  some rounding in the calculation of X.  So by carefully
   *  computing 1 - (c+d)(x+y) we can compute an error and
   *  add that back in.   This is done carefully so that terms
   *  of similar size are subtracted first.
   *  @param in initial number, in split form
   *  @param result placeholder where to put the result
   */
  def splitReciprocal(in: Array[Double], result: Array[Double]) {
    val b = 1.0/4194304.0
    val a = 1.0 - b

    if (in(0) == 0.0) {
      in(0) = in(1)
      in(1) = 0.0
    }

    result(0) = a / in(0)
    result(1) = (b*in(0)-a*in(1)) / (in(0)*in(0) + in(0)*in(1))

    if (result(1) != result(1)) { // can happen if result(1) is NAN
      result(1) = 0.0
    }

    /* Resplit */
    resplit(result)

    var i = 0
    while (i < 2) {
      /* this may be overkill, probably once is enough */
      var err = 1.0 - result(0) * in(0) - result(0) * in(1) -
      result(1) * in(0) - result(1) * in(1)
      /*err = 1.0 - err */
      err = err * (result(0) + result(1))
      /*printf("err = %16e\n", err) */
      result(1) += err
      i += 1
    }
  }

  /** Compute (a(0) + a(1)) * (b(0) + b(1)) in extended precision.
   * @param a first term of the multiplication
   * @param b second term of the multiplication
   * @param result placeholder where to put the result
   */
  private def quadMult(a: Array[Double], b: Array[Double], result: Array[Double]) {
    val xs = new Array[Double](2)
    val ys = new Array[Double](2)
    val zs = new Array[Double](2)

    /* a(0) * b(0) */
    split(a(0), xs)
    split(b(0), ys)
    splitMult(xs, ys, zs)

    result(0) = zs(0)
    result(1) = zs(1)

    /* a(0) * b(1) */
    split(b(1), ys)
    splitMult(xs, ys, zs)

    var tmp = result(0) + zs(0)
    result(1) = result(1) - (tmp - result(0) - zs(0))
    result(0) = tmp
    tmp = result(0) + zs(1)
    result(1) = result(1) - (tmp - result(0) - zs(1))
    result(0) = tmp

    /* a(1) * b(0) */
    split(a(1), xs)
    split(b(0), ys)
    splitMult(xs, ys, zs)

    tmp = result(0) + zs(0)
    result(1) = result(1) - (tmp - result(0) - zs(0))
    result(0) = tmp
    tmp = result(0) + zs(1)
    result(1) = result(1) - (tmp - result(0) - zs(1))
    result(0) = tmp

    /* a(1) * b(0) */
    split(a(1), xs)
    split(b(1), ys)
    splitMult(xs, ys, zs)

    tmp = result(0) + zs(0)
    result(1) = result(1) - (tmp - result(0) - zs(0))
    result(0) = tmp
    tmp = result(0) + zs(1)
    result(1) = result(1) - (tmp - result(0) - zs(1))
    result(0) = tmp
  }

  /** Compute exp(p) for a integer p in extended precision.
   * @param p integer whose exponential is requested
   * @param result placeholder where to put the result in extended precision
   * @return exp(p) in standard precision (equal to result(0) + result(1))
   */
  def expint(_p: Int, result: Array[Double]): Double = {
    var p = _p
    //double x = M_E
    val xs = new Array[Double](2)
    val as = new Array[Double](2)
    val ys = new Array[Double](2)
    //split(x, xs)
    //xs(1) = (double)(2.7182818284590452353602874713526625L - xs(0))
    //xs(0) = 2.71827697753906250000
    //xs(1) = 4.85091998273542816811e-06
    //xs(0) = Double.longBitsToDouble(0x4005bf0800000000L)
    //xs(1) = Double.longBitsToDouble(0x3ed458a2bb4a9b00L)

    /* E */
    xs(0) = 2.718281828459045
    xs(1) = 1.4456468917292502E-16

    split(1.0, ys)

    while (p > 0) {
      if ((p & 1) != 0) {
        quadMult(ys, xs, as)
        ys(0) = as(0)
        ys(1) = as(1)
      }

      quadMult(xs, xs, as)
      xs(0) = as(0)
      xs(1) = as(1)

      p >>= 1
    }

    if (result != null) {
      result(0) = ys(0)
      result(1) = ys(1)

      resplit(result)
    }

    ys(0) + ys(1)
  }
  /** xi in the range of [1, 2].
   *                                3        5        7
   *      x+1           /          x        x        x          \
   *  ln ----- =   2 *  |  x  +   ----  +  ----  +  ---- + ...  |
   *      1-x           \          3        5        7          /
   *
   * So, compute a Remez approximation of the following function
   *
   *  ln ((sqrt(x)+1)/(1-sqrt(x)))  /  x
   *
   * This will be an even function with only positive coefficents.
   * x is in the range [0 - 1/3].
   *
   * Transform xi for input to the above function by setting
   * x = (xi-1)/(xi+1).   Input to the polynomial is x^2, then
   * the result is multiplied by x.
   * @param xi number from which log is requested
   * @return log(xi)
   */
  def slowLog(xi: Double): Array[Double] = {
    val x = new Array[Double](2)
    val x2 = new Array[Double](2)
    val y = new Array[Double](2)
    val a = new Array[Double](2)

    split(xi, x)

    /* Set X = (x-1)/(x+1) */
    x(0) += 1.0
    resplit(x)
    splitReciprocal(x, a)
    x(0) -= 2.0
    resplit(x)
    splitMult(x, a, y)
    x(0) = y(0)
    x(1) = y(1)

    /* Square X -> X2*/
    splitMult(x, x, x2)


    //x(0) -= 1.0
    //resplit(x)

    y(0) = LN_SPLIT_COEF(LN_SPLIT_COEF.length-1)(0)
    y(1) = LN_SPLIT_COEF(LN_SPLIT_COEF.length-1)(1)

    var i = LN_SPLIT_COEF.length - 2
    while (i >= 0) {
      splitMult(y, x2, a)
      y(0) = a(0)
      y(1) = a(1)
      splitAdd(y, LN_SPLIT_COEF(i), a)
      y(0) = a(0)
      y(1) = a(1)
      i -= 1
    }

    splitMult(y, x, a)
    y(0) = a(0)
    y(1) = a(1)

    y
  }


  /**
   * Print an array.
   * @param out text output stream where output should be printed
   * @param name array name
   * @param expectedLen expected length of the array
   * @param array2d array data
   */
  def printarray(out: PrintStream, name: String, expectedLen: Int, array2d: Array[Array[Double]]) {
    out.println(name)
    checkLen(expectedLen, array2d.length)
    out.println(TABLE_START_DECL + " ")
    var i = 0
    while (i < array2d.length) {
      val array = array2d(i)
      out.print("        {")
      var j = 0
      while (j < array.length) { // assume inner array has very few entries
        val d = array(j) 
        out.printf("%-25.25s", format(d)) // multiple entries per line
        j += 1
      }
      out.println("}, // " + i)
      i += 1
    }
    out.println(TABLE_END_DECL)
  }

  /**
   * Print an array.
   * @param out text output stream where output should be printed
   * @param name array name
   * @param expectedLen expected length of the array
   * @param array array data
   */
  def printarray(out: PrintStream, name: String, expectedLen: Int, array: Array[Double]) {
    out.println(name + "=")
    checkLen(expectedLen, array.length)
    out.println(TABLE_START_DECL)
    var i = 0
    while (i < array.length) {
      val d = array(i)
      out.printf("        %s%n", format(d)) // one entry per line
      i += 1
    }
    out.println(TABLE_END_DECL)
  }

  /** Format a double.
   * @param d double number to format
   * @return formatted number
   */
  def format(d: Double): String = {
    if (d != d) {
      "Double.NaN,"
    } else {
      (if (d >= 0) "+" else "") + d.toString + "d,"
    }
  }

  /**
   * Check two lengths are equal.
   * @param expectedLen expected length
   * @param actual actual length
   * @exception DimensionMismatchException if the two lengths are not equal
   */
  @throws(classOf[CardinalityException])
  private def checkLen(expectedLen: Int, actual: Int) {
    if (expectedLen != actual) {
      throw new CardinalityException(expectedLen, actual)
    }
  }

}
