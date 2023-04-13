package org.aiotrade.lib.math

/**
 * Function objects to be passed to generic methods. Contains the functions of {@link java.lang.math} as function
 * objects, as well as a few more basic functions. <p>Function objects conveniently allow to express arbitrary functions
 * in a generic manner. Essentially, a function object is an object that can perform a function on some arguments. It
 * has a minimal interface: a method <tt>apply</tt> that takes the arguments, computes something and returns some result
 * value. Function objects are comparable to function pointers in C used for call-backs. <p>Unary functions are of type
 * {@link org.apache.mahout.math.function.DoubleFunction}, binary functions of type {@link
 * org.apache.mahout.math.function.DoubleDoubleFunction}. All can be retrieved via <tt>def</tt>
 * variables named after the function. Unary predicates are of type
 * {@link org.apache.mahout.math.function.DoubleProcedure},
 * binary predicates of type {@link org.apache.mahout.math.function.DoubleDoubleProcedure}. All can be retrieved via
 * <tt>def</tt> variables named <tt>isXXX</tt>.
 *
 * <p> Binary functions and predicates also exist as unary functions with the second argument being fixed to a constant.
 * These are generated and retrieved via factory methods (again with the same name as the function). Example: <ul>
 * <li><tt>Functions.pow</tt> gives the function <tt>a<sup>b</sup></tt>. <li><tt>Functions.pow.apply(2,3)==8</tt>.
 * <li><tt>Functions.pow(3)</tt> gives the function <tt>a<sup>3</sup></tt>. <li><tt>Functions.pow(3).apply(2)==8</tt>.
 * </ul> More general, any binary function can be made an unary functions by fixing either the first or the second
 * argument. See methods {@link #bindArg1(org.apache.mahout.math.function.DoubleDoubleFunction ,Double)} and {@link
 * #bindArg2(org.apache.mahout.math.function.DoubleDoubleFunction ,Double)}. The order of arguments can
 * be swapped so that the first argument becomes the
 * second and vice-versa. See method {@link #swapArgs(org.apache.mahout.math.function.DoubleDoubleFunction)}.
 * Example: <ul> <li><tt>Functions.pow</tt>
 * gives the function <tt>a<sup>b</sup></tt>. <li><tt>Functions.bindArg2(Functions.pow,3)</tt> gives the function
 * <tt>x<sup>3</sup></tt>. <li><tt>Functions.bindArg1(Functions.pow,3)</tt> gives the function <tt>3<sup>x</sup></tt>.
 * <li><tt>Functions.swapArgs(Functions.pow)</tt> gives the function <tt>b<sup>a</sup></tt>. </ul> <p> Even more
 * general, functions can be chained (composed, assembled). Assume we have two unary functions <tt>g</tt> and
 * <tt>h</tt>. The unary function <tt>g(h(a))</tt> applying both in sequence can be generated via {@link
 * #chain(org.apache.mahout.math.function.DoubleFunction , org.apache.mahout.math.function.DoubleFunction)}:
 * <ul> <li><tt>Functions.chain(g,h);</tt> </ul> Assume further we have a binary
 * function <tt>f</tt>. The binary function <tt>g(f(a,b))</tt> can be generated via {@link
 * #chain(org.apache.mahout.math.function.DoubleFunction , org.apache.mahout.math.function.DoubleDoubleFunction)}:
 * <ul> <li><tt>Functions.chain(g,f);</tt> </ul> The binary function
 * <tt>f(g(a),h(b))</tt> can be generated via
 * {@link #chain(org.apache.mahout.math.function.DoubleDoubleFunction , org.apache.mahout.math.function.DoubleFunction ,
 * org.apache.mahout.math.function.DoubleFunction)}: <ul>
 * <li><tt>Functions.chain(f,g,h);</tt> </ul> Arbitrarily complex functions can be composed from these building blocks.
 * For example <tt>sin(a) + cos<sup>2</sup>(b)</tt> can be specified as follows: <ul>
 * <li><tt>chain(plus,sin,chain(square,cos));</tt> </ul> or, of course, as
 * <pre>
 * 
 * &nbsp;&nbsp;&nbsp;public Double apply(a: Double, b: Double) { math.sin(a) + math.pow(math.cos(b),2); }
 * }
 * </pre>
 * <p> For aliasing see functions. Try this <table> <td class="PRE">
 * <pre>
 * // should yield 1.4399560356056456 in all cases
 * a: Double = 0.5;
 * b: Double = 0.2;
 * Double v = math.sin(a) + math.pow(math.cos(b),2);
 * log.info(v);
 * Functions F = Functions.functions;
 * DoubleDoubleFunction f = F.chain(F.plus,F.sin,F.chain(F.square,F.cos));
 * log.info(f.apply(a,b));
 * DoubleDoubleFunction g = 
 * &nbsp;&nbsp;&nbsp; = (a: Double, b: Double) => { math.sin(a) + math.pow(math.cos(b),2); }
 * };
 * log.info(g.apply(a,b));
 * </pre>
 * </td> </table>
 *
 * <p> <H3>Performance</H3>
 *
 * Surprise. Using modern non-adaptive JITs such as SunJDK 1.2.2 (java -classic) there seems to be no or only moderate
 * performance penalty in using function objects in a loop over traditional code in a loop. For complex nested function
 * objects (e.g. <tt>F.chain(F.abs,F.chain(F.plus,F.sin,F.chain(F.square,F.cos)))</tt>) the penalty is zero, for trivial
 * functions (e.g. <tt>F.plus</tt>) the penalty is often acceptable. <center> <table border cellpadding="3"
 * cellspacing="0" align="center"> <tr valign="middle" bgcolor="#33CC66" nowrap align="center"> <td nowrap colspan="7">
 * <font size="+2">Iteration Performance [million function evaluations per second]</font><br> <font size="-1">Pentium
 * Pro 200 Mhz, SunJDK 1.2.2, NT, java -classic, </font></td> </tr> <tr valign="middle" bgcolor="#66CCFF" nowrap
 * align="center"> <td nowrap bgcolor="#FF9966" rowspan="2">&nbsp;</td> <td bgcolor="#FF9966" colspan="2"> <p> 30000000
 * iterations</p> </td> <td bgcolor="#FF9966" colspan="2"> 3000000 iterations (10 times less)</td> <td bgcolor="#FF9966"
 * colspan="2">&nbsp;</td> </tr> <tr valign="middle" bgcolor="#66CCFF" nowrap align="center"> <td bgcolor="#FF9966">
 * <tt>F.plus</tt></td> <td bgcolor="#FF9966"><tt>a+b</tt></td> <td bgcolor="#FF9966">
 * <tt>F.chain(F.abs,F.chain(F.plus,F.sin,F.chain(F.square,F.cos)))</tt></td> <td bgcolor="#FF9966">
 * <tt>math.abs(math.sin(a) + math.pow(math.cos(b),2))</tt></td> <td bgcolor="#FF9966">&nbsp;</td> <td
 * bgcolor="#FF9966">&nbsp;</td> </tr> <tr valign="middle" bgcolor="#66CCFF" nowrap align="center"> <td nowrap
 * bgcolor="#FF9966">&nbsp;</td> <td nowrap>10.8</td> <td nowrap>29.6</td> <td nowrap>0.43</td> <td nowrap>0.35</td> <td
 * nowrap>&nbsp;</td> <td nowrap>&nbsp;</td> </tr> </table></center>
 */
object Functions {

  /*
   * <H3>Unary functions</H3>
   */
  /** Function that returns <tt>math.abs(a)</tt>. */
  val ABS = (a: Double) => math.abs(a)

  /** Function that returns <tt>math.acos(a)</tt>. */
  val ACOS = (a: Double) => math.acos(a)

  /** Function that returns <tt>math.asin(a)</tt>. */
  val ASIN = (a: Double) => math.asin(a)

  /** Function that returns <tt>math.atan(a)</tt>. */
  val ATAN = (a: Double) => math.atan(a)

  /** Function that returns <tt>math.ceil(a)</tt>. */
  val CEIL = (a: Double) => math.ceil(a)

  /** Function that returns <tt>math.cos(a)</tt>. */
  val COS = (a: Double) => math.cos(a)

  /** Function that returns <tt>math.exp(a)</tt>. */
  val EXP = (a: Double) => math.exp(a)

  /** Function that returns <tt>math.floor(a)</tt>. */
  val FLOOR = (a: Double) => math.floor(a)

  /** Function that returns its argument. */
  val IDENTITY = (a: Double) => a

  /** Function that returns <tt>1.0 / a</tt>. */
  val INV = (a: Double) => 1.0 / a

  /** Function that returns <tt>math.log(a)</tt>. */
  val LOGARITHM = (a: Double) => math.log(a)

  /** Function that returns <tt>math.log(a) / math.log(2)</tt>. */
  val LOG2 = (a: Double) => math.log(a) * 1.4426950408889634

  /** Function that returns <tt>-a</tt>. */
  val NEGATE = (a: Double) => -a

  /** Function that returns <tt>math.rint(a)</tt>. */
  val RINT = (a: Double) => math.rint(a)

  /** Function that returns <tt>a < 0 ? -1 : a > 0 ? 1 : 0</tt>. */
  val SIGN = (a: Double) => if (a < 0) -1 else if (a > 0) 1.0 else 0.0

  /** Function that returns <tt>math.sin(a)</tt>. */
  val SIN = (a: Double) => math.sin(a)

  /** Function that returns <tt>math.sqrt(a)</tt>. */
  val SQRT = (a: Double) => math.sqrt(a)

  /** Function that returns <tt>a * a</tt>. */
  val SQUARE = (a: Double) => a * a

  /** Function that returns <tt> 1 / (1 + exp(-a) </tt> */
  val SIGMOID = (a: Double) => 1.0 / (1.0 + math.exp(-a))

  /** Function that returns <tt> a * (1-a) </tt> */
  val SIGMOIDGRADIENT = (a: Double) => a * (1.0 - a)

  /** Function that returns <tt>math.tan(a)</tt>. */
  val TAN = (a: Double) => math.tan(a)


  /*
   * <H3>Binary functions</H3>
   */

  /** Function that returns <tt>math.atan2(a,b)</tt>. */
  val ATAN2 = (a: Double, b: Double) => math.atan2(a, b)

  /** Function that returns <tt>a < b ? -1 : a > b ? 1 : 0</tt>. */
  val COMPARE = (a: Double, b: Double) => if (a < b) -1 else if (a > b) 1.0 else 0.0

  /** Function that returns <tt>a / b</tt>. */
  val DIV = (a: Double, b: Double) => a / b

  /** Function that returns <tt>a == b ? 1 : 0</tt>. */
  val EQUALS = (a: Double, b: Double) => if (a == b) 1.0 else 0.0

  /** Function that returns <tt>a > b ? 1 : 0</tt>. */
  val GREATER = (a: Double, b: Double) => if (a > b) 1.0 else 0.0

  /** Function that returns <tt>math.IEEEremainder(a,b)</tt>. */
  val IEEE_REMAINDER = (a: Double, b: Double) => math.IEEEremainder(a, b)

  /** Function that returns <tt>a == b</tt>. */
  val IS_EQUAL = (a: Double, b: Double) => a == b

  /** Function that returns <tt>a < b</tt>. */
  val IS_LESS = (a: Double, b: Double) => a < b

  /** Function that returns <tt>a > b</tt>. */
  val IS_GREATER = (a: Double, b: Double) => a > b

  /** Function that returns <tt>a < b ? 1 : 0</tt>. */
  val LESS = (a: Double, b: Double) => if (a < b) 1.0 else 0.0

  /** Function that returns <tt>math.log(a) / math.log(b)</tt>. */
  val LG = (a: Double, b: Double) => math.log(a) / math.log(b)

  /** Function that returns <tt>math.max(a,b)</tt>. */
  val MAX = (a: Double, b: Double) =>  math.max(a, b)

  /** Function that returns <tt>math.min(a,b)</tt>. */
  val MIN = (a: Double, b: Double) => math.min(a, b)

  /** Function that returns <tt>a - b</tt>. */
  val MINUS = plusMult(-1)

  /** Function that returns <tt>a % b</tt>. */
  val MOD = (a: Double, b: Double) => a % b

  /** Function that returns <tt>a * b</tt>. */
  val MULT = (a: Double, b: Double) => a * b
  
  /** Function that returns <tt>a + b</tt>. */
  val PLUS = (a: Double, b: Double) => a + b

  /** Function that returns <tt>math.abs(a) + math.abs(b)</tt>. */
  val PLUS_ABS = (a: Double, b: Double) => math.abs(a) + math.abs(b)

  /** Function that returns <tt>math.pow(a,b)</tt>. */
  val POW = (a: Double, b: Double) => math.pow(a, b)

  /**
   * Constructs a function that returns <tt>(from<=a && a<=to) ? 1 : 0</tt>. <tt>a</tt> is a variable, <tt>from</tt> and
   * <tt>to</tt> are fixed.
   */
  def between(from: Double, to: Double) = (a: Double) => if (from <= a && a <= to) 1.0 else 0.0

  /**
   * Constructs a unary function from a binary function with the first operand (argument) fixed to the given constant
   * <tt>c</tt>. The second operand is variable (free).
   *
   * @param function a binary function taking operands in the form <tt>function.apply(c,var)</tt>.
   * @the unary function <tt>function(c,var)</tt>.
   */
  def bindArg1(function: (Double, Double) => Double, c: Double) = (v: Double) => function(c, v)

  /**
   * Constructs a unary function from a binary function with the second operand (argument) fixed to the given constant
   * <tt>c</tt>. The first operand is variable (free).
   *
   * @param function a binary function taking operands in the form <tt>function.apply(var,c)</tt>.
   * @the unary function <tt>function(var,c)</tt>.
   */
  def bindArg2(function: (Double, Double) => Double, c: Double) = (v: Double) => function(v, c)

  /**
   * Constructs the function <tt>f( g(a), h(b) )</tt>.
   *
   * @param f a binary function.
   * @param g a unary function.
   * @param h a unary function.
   * @the binary function <tt>f( g(a), h(b) )</tt>.
   */
  def chain(f: (Double, Double) => Double, g: Double => Double, h: Double => Double) = (a: Double, b: Double) => f(g(a), h(b))

  /**
   * Constructs the function <tt>g( h(a,b) )</tt>.
   *
   * @param g a unary function.
   * @param h a binary function.
   * @the unary function <tt>g( h(a,b) )</tt>.
   */
  def chain(g: Double => Double, h: (Double, Double) => Double) = (a: Double, b: Double) => g(h(a, b))

  /**
   * Constructs the function <tt>g( h(a) )</tt>.
   *
   * @param g a unary function.
   * @param h a unary function.
   * @the unary function <tt>g( h(a) )</tt>.
   */
  def chain(g: Double => Double, h: Double => Double) = (a: Double) => g(h(a))

  /**
   * Constructs a function that returns <tt>a < b ? -1 : a > b ? 1 : 0</tt>. <tt>a</tt> is a variable, <tt>b</tt> is
   * fixed.
   */
  def compare(b: Double) = (a: Double) => if (a < b) -1 else if (a > b) 1.0 else 0.0

  /** Constructs a function that returns the constant <tt>c</tt>. */
  def constant(c: Double) = (a: Double) => c

  /** Constructs a function that returns <tt>a / b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def div(b: Double) = mult(1 / b)

  /** Constructs a function that returns <tt>a == b ? 1 : 0</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def equals(b: Double) = (a: Double) => if (a == b) 1.0 else 0.0

  /** Constructs a function that returns <tt>a > b ? 1 : 0</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def greater(b: Double) = (a: Double) => if (a > b) 1.0 else 0.0

  /**
   * Constructs a function that returns <tt>math.IEEEremainder(a,b)</tt>. <tt>a</tt> is a variable, <tt>b</tt> is
   * fixed.
   */
  def mathIEEEremainder(b: Double) = (a: Double) => math.IEEEremainder(a, b)

  /**
   * Constructs a function that returns <tt>from<=a && a<=to</tt>. <tt>a</tt> is a variable, <tt>from</tt> and
   * <tt>to</tt> are fixed.
   */
  def isBetween(from: Double, to: Double) = (a: Double) => from <= a && a <= to

  /** Constructs a function that returns <tt>a == b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def isEqual(b: Double) = (a: Double) => a == b

  /** Constructs a function that returns <tt>a > b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def isGreater(b: Double) = (a: Double) => a > b

  /** Constructs a function that returns <tt>a < b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def isLess(b: Double) = (a: Double) => a < b

  /** Constructs a function that returns <tt>a < b ? 1 : 0</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def less(b: Double) = (a: Double) => if (a < b) 1.0 else 0.0

  /**
   * Constructs a function that returns <tt><tt>math.log(a) / math.log(b)</tt></tt>. <tt>a</tt> is a variable,
   * <tt>b</tt> is fixed.
   */
  def lg(b: Double) = (a: Double) => {
    def logInv = 1.0 / math.log(b) // cached for speed
    math.log(a) * logInv
  }

  /** Constructs a function that returns <tt>math.max(a,b)</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def max(b: Double) = (a: Double) => math.max(a, b)

  /** Constructs a function that returns <tt>math.min(a,b)</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def min(b: Double) = (a: Double) =>  math.min(a, b)

  /** Constructs a function that returns <tt>a - b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def minus(b: Double) = (a: Double) => a - b

  /**
   * Constructs a function that returns <tt>a - b*constant</tt>. <tt>a</tt> and <tt>b</tt> are variables,
   * <tt>constant</tt> is fixed.
   */
  def minusMult(constant: Double) = plusMult(-constant)

  /** Constructs a function that returns <tt>a % b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def mod(b: Double) = (a: Double) => a % b

  /** Constructs a function that returns <tt>a * b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def mult(b: Double) = (a: Double) => a * b

  /** Constructs a function that returns <tt>a + b</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def plus(b: Double) = (a: Double) => a + b

  /**
   * Constructs a function that returns <tt>a + b*constant</tt>. <tt>a</tt> and <tt>b</tt> are variables,
   * <tt>constant</tt> is fixed.
   */
  def plusMult(constant: Double) = new PlusMult(constant)

  /** Constructs a function that returns <tt>math.pow(a,b)</tt>. <tt>a</tt> is a variable, <tt>b</tt> is fixed. */
  def pow(b: Double) = (a: Double) => math.pow(a, b)

  /**
   * Constructs a function that returns a new uniform random number in the open unit interval {@code (0.0,1.0)}
   * (excluding 0.0 and 1.0). Currently the engine is {@link MersenneTwister} and is
   * seeded with the current time. <p> Note that any random engine derived from {@link
   * org.apache.mahout.math.jet.random.engine.RandomEngine} and any random distribution derived from {@link
   * org.apache.mahout.math.jet.random.AbstractDistribution} are function objects, because they implement the proper
   * interfaces. Thus, if you are not happy with the default, just pass your favourite random generator to function
   * evaluating methods.
   */
  //def random() = new MersenneTwister(new Date())

  /**
   * Constructs a function that returns the number rounded to the given precision;
   * <tt>math.rint(a/precision)*precision</tt>. Examples:
   * <pre>
   * precision = 0.01 rounds 0.012 --> 0.01, 0.018 --> 0.02
   * precision = 10   rounds 123   --> 120 , 127   --> 130
   * </pre>
   */
  def round(precision: Double) = (a: Double) => math.rint(a / precision) * precision

  /**
   * Constructs a function that returns <tt>function.apply(b,a)</tt>, i.e. applies the function with the first operand
   * as second operand and the second operand as first operand.
   *
   * @param function a function taking operands in the form <tt>function.apply(a,b)</tt>.
   * @the binary function <tt>function(b,a)</tt>.
   */
  def swapArgs(function: (Double, Double) => Double) = (a: Double, b: Double) => function(b, a)

  
  // --- Named function classes, usally when we need to know the class type of the function
  
  final class Mult(var multiplicator: Double) extends Function1[Double, Double] {
    def apply(a: Double): Double = a * multiplicator
  }
  object Mult {
    def apply(multiplicator: Double) = new Mult(multiplicator)
  }
  
  final class PlusMult(var multiplicator: Double) extends Function2[Double, Double, Double] {
    def apply(a: Double, b: Double): Double = a + b * multiplicator
  }
  object PlusMult {
    def apply(multiplicator: Double) = new PlusMult(multiplicator)
  }
  
  final class SquareRootFunction extends Function1[Double, Double] {
    def apply(a: Double): Double = math.sqrt(a)
  }
  object SquareRootFunction {
    def apply() = new SquareRootFunction()
  }
  
  final class TimesFunction extends Function2[Double, Double, Double] {
    def apply(x: Double, y: Double): Double = x * y
  }
  object TimesFunction {
    def apply() = new TimesFunction()
  }
}
