package org.aiotrade.lib.math.stats

/**
 *
 * @author Caoyuan Deng
 */
object GeneralizedLeastSquares {
  
  def coefficients(xs: Array[Double], ys: Array[Double], order: Int, _weights: Array[Double] = null): Array[Double] = {
    val ws = if (_weights == null) Array.fill[Double](xs.length)(1.0) else _weights
    
    assert(
      xs != null && ys != null && ws != null && xs.length >= 2 && 
      xs.length == ys.length && xs.length == ws.length && order >= 2, 
      "Invald params: x.length(>=2)=%s, y.length(=x.length)=%s, order(>=2)=%s".format(xs.length, ys.length, order)
    )

    val s = Array.ofDim[Double]((order - 1) * 2 + 1)
    var i = 0
    while (i < s.length) {
      var j = 0
      while (j < xs.length) {
        s(i) += math.pow(xs(j), i) * ws(j)
        j += 1
      }
      i += 1
    }
    
    val f = Array.ofDim[Double](order)
    i = 0
    while (i < f.length) {
      var j = 0
      while (j < xs.length) {
        f(i) += math.pow(xs(j), i) * ys(j) * ws(j)
        j += 1
      }
      i += 1
    }
    
    val a = Array.ofDim[Double](order, order)
    i = 0
    while (i < order) {
      var j = 0
      while (j < order) {
        a(i)(j) = s(i + j)
        j += 1
      }
      i += 1
    }
    
    multiLinearEquationGroup(a, f)
  }
  
  def fit(coefficients: Array[Double])(x: Double): Double = {
    var y = 0.0
    var i = 0
    while (i < coefficients.length) {
      y += math.pow(x, i) * coefficients(i)
      i += 1
    }
    y
  }

  /**
   * @param left factors
   * @param right outputs
   * @return result
   */
  def multiLinearEquationGroup(factors: Array[Array[Double]], b: Array[Double]): Array[Double] = {
    val result = Array.ofDim[Double](factors.length)
		
    val len = factors.length - 1
    if (len == 0) {
      result(0) = b(0) / factors(0)(0)
      return result
    }
		
    val as = Array.ofDim[Double](len, len)
    val bs = Array.ofDim[Double](len)
    var xIdx = -1
    var yIdx = -1
    var i = 0
    var break_i = false
    while (i <= len && !break_i) {
      var j = 0
      var break_j = false
      while (j <= len && !break_j) {
        if (factors(i)(j) != 0.0) {
          yIdx = j
          break_j = true
        }
        j += 1
      }
      if (yIdx != -1) {
        xIdx = i
        break_i = true
      }
      i += 1
    }
    
    if (xIdx == -1) {
      return null
    }
		
    var count_i = 0
    i = 0
    while (i <= len) {
      if (i != xIdx) {
        bs(count_i) = b(i) * factors(xIdx)(yIdx) - b(xIdx) * factors(i)(yIdx)
        var count_j = 0
        var j = 0
        while (j <= len){
          if (j != yIdx) {
            as(count_i)(count_j) = factors(i)(j) * factors(xIdx)(yIdx) - factors(xIdx)(j) * factors(i)(yIdx)
            count_j += 1
          }        
          j += 1
        }
        count_i += 1
      }
      i += 1
    }
		
    val result2 = multiLinearEquationGroup(as, bs)
		
    var sum = b(xIdx)
    count_i = 0
    i = 0
    while (i <= len) {
      if (i != yIdx) {
        sum -= factors(xIdx)(i) * result2(count_i)
        result(i) = result2(count_i)
        count_i += 1
      }
      i += 1
    }
    result(yIdx) = sum / factors(xIdx)(yIdx)
		
    result
  }
  
  // --- simple test
  def main(args: Array[String]) {
    testCoefficients
    testMultiLinear
  }

  private def testCoefficients() {
    val xs1 = Array(187.1, 179.5, 157.0, 197.0, 239.4, 217.8, 227.1, 233.4, 242.0, 251.9, 230.0, 271.8)
    val ys1 = Array( 25.4,  22.8,  20.6,  21.8,  32.4,  24.4,  29.3,  27.9,  27.8,  34.2,  29.2,  30.0)
    val coefs1 = coefficients(xs1, ys1, 2)
    println("y = %s + %s * x".format(coefs1(0), coefs1(1))) // y = 3.412968396061506 + 0.10814137404983369 * x
    
    val xs2 = Array[Double]( 2000,  2001,  2002,  2003,  2004,   2005,   2006,   2007,   2008)
    val ys2 = Array[Double](37.84, 44.55, 45.74, 63.80, 76.67, 105.59, 178.48, 355.27, 409.92)
    val ws2 = Array[Double]( 11.0,  12.0,  13.0,  14.0,  15.0,   16.0,   17.0,   18.0,   19.0)
    val coefs2 = coefficients(xs2, ys2, 2, ws2)
    println("y = %s + %s * x".format(coefs2(0), coefs2(1)))
    val ys2Fit = List[Double](2000, 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010) map fit(coefs2)
    println(ys2Fit)
  }

  private def testMultiLinear() {
    val r = multiLinearEquationGroup(
      Array(
        Array(1, 2, 3, 1), 
        Array(2, 0, 1, 0), 
        Array(5, 2, 0, 0), 
        Array(7, 1, 1, 0)
      ),
      Array(18, 5, 9, 12)
    )
    println(r.mkString(","))
  }
  
}

