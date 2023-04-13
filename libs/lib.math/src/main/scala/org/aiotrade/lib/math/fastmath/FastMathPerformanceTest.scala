package org.aiotrade.lib.math.fastmath


/**
 * Performance tests for FastMath.
 * Not enabled by default, as the class does not end in Test.
 * 
 */
object FastMathPerformanceTest {
  private val RUNS = Integer.parseInt(System.getProperty("testRuns","10000000"))
  private val F1 = 1d / RUNS

  // Header format
  private val FMT_HDR = "%-5s %13s %13s %13s Runs=%d Java %s (%s) %s (%s)"
  // Detail format
  private val FMT_DTL = "%-5s\t %6d\t %6.1f\t %6d\t %6.4f\t %6d\t %6.4f"

  def main(args: Array[String]) {
    header
    this.testAbs
    this.testAcos
    this.testAsin
    this.testAtan
    this.testAtan2
    this.testCbrt
    this.testCos
    this.testCosh
    this.testExp
    this.testExpm1
    this.testHypot
    this.testLog
    this.testLog10
    this.testLog1p
    this.testPow
    this.testSin
    this.testSinh
    this.testSqrt
    this.testTan
    this.testTanh
  }
  
  def header {
    println(FMT_HDR.format(
        "Name","StrictMath","FastMath","Math",RUNS,
        System.getProperty("java.version"),
        System.getProperty("java.runtime.version","?"),
        System.getProperty("java.vm.name"),
        System.getProperty("java.vm.version")
      ))
  }

  private def report(name: String, strictMathTime: Long, fastMathTime: Long, mathTime: Long) {
    val unitTime = strictMathTime
    println(FMT_DTL.format(
        name,
        strictMathTime / RUNS, strictMathTime.toDouble / unitTime,
        fastMathTime / RUNS, fastMathTime.toDouble / unitTime,
        mathTime / RUNS, mathTime.toDouble / unitTime
      ))
  }

  def testLog {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.log(math.Pi + i/* 1.0 + i/1e9 */)
    val strictMath = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.log(math.Pi + i/* 1.0 + i/1e9 */)
    val fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.log(math.Pi + i/* 1.0 + i/1e9 */)
    val mathTime = System.nanoTime - time

    report("log",strictMath,fastTime,mathTime)
  }

  def testLog10 {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.log10(math.Pi + i/* 1.0 + i/1e9 */)
    var strictMath = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.log10(math.Pi + i/* 1.0 + i/1e9 */)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.log10(math.Pi + i/* 1.0 + i/1e9 */)
    var mathTime = System.nanoTime - time

    report("log10",strictMath,fastTime,mathTime)
  }

  
  def testLog1p {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.log1p(math.Pi + i/* 1.0 + i/1e9 */)
    var strictMath = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.log1p(math.Pi + i/* 1.0 + i/1e9 */)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.log1p(math.Pi + i/* 1.0 + i/1e9 */)
    var mathTime = System.nanoTime - time

    report("log1p",strictMath,fastTime,mathTime)
  }

  
  def testPow {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.pow(math.Pi + i * F1, i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.pow(math.Pi + i * F1, i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.pow(math.Pi + i * F1, i * F1)
    var mathTime = System.nanoTime - time
    report("pow",strictTime,fastTime,mathTime)
  }

  
  def testExp {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.exp(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.exp(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.exp(i * F1)
    var mathTime = System.nanoTime - time

    report("exp",strictTime,fastTime,mathTime)
  }

  
  def testSin {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.sin(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.sin(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.sin(i * F1)
    var mathTime = System.nanoTime - time

    report("sin",strictTime,fastTime,mathTime)
  }

  
  def testAsin {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.asin(i / 10000000.0)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.asin(i / 10000000.0)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.asin(i / 10000000.0)
    var mathTime = System.nanoTime - time

    report("asin",strictTime,fastTime,mathTime)
  }

  
  def testCos {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.cos(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.cos(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.cos(i * F1)
    var mathTime = System.nanoTime - time

    report("cos",strictTime,fastTime,mathTime)
  }
            
  
  def testAcos {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.acos(i / 10000000.0)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.acos(i / 10000000.0)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.acos(i / 10000000.0)
    var mathTime = System.nanoTime - time
    report("acos",strictTime,fastTime,mathTime)
  }

  
  def testTan {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.tan(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.tan(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.tan(i * F1)
    var mathTime = System.nanoTime - time

    report("tan",strictTime,fastTime,mathTime)
  }

  
  def testAtan {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.atan(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.atan(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.atan(i * F1)
    var mathTime = System.nanoTime - time

    report("atan",strictTime,fastTime,mathTime)
  }

  
  def testAtan2 {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.atan2(i * F1, i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.atan2(i * F1, i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.atan2(i * F1, i * F1)
    var mathTime = System.nanoTime - time

    report("atan2",strictTime,fastTime,mathTime)
  }

  
  def testHypot {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.hypot(i * F1, i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.hypot(i * F1, i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.hypot(i * F1, i * F1)
    var mathTime = System.nanoTime - time

    report("hypot",strictTime,fastTime,mathTime)
  }
     
  
  def testCbrt {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.cbrt(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.cbrt(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.cbrt(i * F1)
    var mathTime = System.nanoTime - time

    report("cbrt",strictTime,fastTime,mathTime)
  }

  
  def testSqrt {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.sqrt(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.sqrt(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.sqrt(i * F1)
    var mathTime = System.nanoTime - time

    report("sqrt",strictTime,fastTime,mathTime)
  }

  
  def testCosh {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.cosh(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.cosh(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.cosh(i * F1)
    var mathTime = System.nanoTime - time

    report("cosh",strictTime,fastTime,mathTime)
  }

  
  def testSinh {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.sinh(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.sinh(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.sinh(i * F1)
    var mathTime = System.nanoTime - time

    report("sinh",strictTime,fastTime,mathTime)
  }

  
  def testTanh {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.tanh(i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.tanh(i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.tanh(i * F1)
    var mathTime = System.nanoTime - time

    report("tanh",strictTime,fastTime,mathTime)
  }
     
  
  def testExpm1 {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.expm1(-i * F1)
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.expm1(-i * F1)
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.expm1(-i * F1)
    var mathTime = System.nanoTime - time
    report("expm1",strictTime,fastTime,mathTime)
  }

  
  def testAbs {
    var x = 0.0
    var time = System.nanoTime
    var i = -1; while ({i += 1; i < RUNS})
      x += StrictMath.abs(i * (1 - 0.5 * RUNS))
    var strictTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += FastMath.abs(i * (1 - 0.5 * RUNS))
    var fastTime = System.nanoTime - time

    x = 0
    time = System.nanoTime
    i = -1; while ({i += 1; i < RUNS})
      x += java.lang.Math.abs(i * (1 - 0.5 * RUNS))
    var mathTime = System.nanoTime - time

    report("abs",strictTime,fastTime,mathTime)
  }  
}

