package org.aiotrade.lib.math.algebra


/**
 * The basic interface including numerous convenience functions <p/> NOTE: All implementing classes must have a
 * constructor that takes an int for cardinality and a no-arg constructor that can be used for marshalling the Writable
 * instance <p/> NOTE: Implementations may choose to reuse the Vector.Element in the Iterable methods
 */
@cloneable
trait Vector extends Iterable[Vector.Element] {

  type Element = Vector.Element
  
  /** @return a formatted String suitable for output */
  def asFormatString: String

  /**
   * Assign the value to all elements of the receiver
   *
   * @param value a Double value
   * @return the modified receiver
   */
  def assign(value: Double): Vector

  /**
   * Assign the values to the receiver
   *
   * @param values a Double[] of values
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(values: Array[Double]): Vector

  /**
   * Assign the other vector values to the receiver
   *
   * @param other a Vector
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(other: Vector): Vector

  /**
   * Apply the function to each element of the receiver
   *
   * @param function a Double => Double to apply
   * @return the modified receiver
   */
  def assign(function: Double => Double): Vector

  /**
   * Apply the function to each element of the receiver and the corresponding element of the other argument
   *
   * @param other    a Vector containing the second arguments to the function
   * @param function a (Double, Double) => Double to apply
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(other: Vector, function: (Double, Double) => Double): Vector

  /**
   * Apply the function to each element of the receiver, using the y value as the second argument of the (Double, Double) => Double
   *
   * @param f a (Double, Double) => Double to be applied
   * @param y a Double value to be argument to the function
   * @return the modified receiver
   */
  def assign(f: (Double, Double) => Double, y: Double): Vector

  /**
   * Return the cardinality of the recipient (the maximum number of values)
   *
   * @return an int
   */
  def size: Int

  /**
   * @return true iff this implementation should be considered dense -- that it explicitly
   *  represents every value
   */
  def isDense: Boolean

  /**
   * @return true iff this implementation should be considered to be iterable in index order in an efficient way.
   *  In particular this implies that {@link #iterator()} and {@link #iterateNonZero()} return elements
   *  in ascending order by index.
   */
  def isSequentialAccess: Boolean

  /**
   * Return a copy of the recipient
   *
   * @return a new Vector
   */
  override 
  def clone: Vector = {
    // Scala's compiler seems to complain that the clone method is the protected 
    // one from Object instead of this overrided one when it's called outside the
    // protected scope. For instance: 
    //   method clone in class Object cannot be accessed in ....
    //   Access to protected method clone not permitted because
    // To bypass it, we need to implement it with following statement
    throw new CloneNotSupportedException
  }

  /**
   * Iterates over all elements <p/> * NOTE: Implementations may choose to reuse the Element returned for performance
   * reasons, so if you need a copy of it, you should call {@link #getElement(int)} for the given index
   *
   * @return An {@link Iterator} over all elements
   */
  def iterator: Iterator[Element]

  /**
   * Iterates over all non-zero elements. <p/> NOTE: Implementations may choose to reuse the Element returned for
   * performance reasons, so if you need a copy of it, you should call {@link #getElement(int)} for the given index
   *
   * @return An {@link Iterator} over all non-zero elements
   */
  def iterateNonZero: Iterator[Element]

  /**
   * Return an object of Vector.Element representing an element of this Vector. Useful when designing new iterator
   * types.
   *
   * @param index Index of the Vector.Element required
   * @return The Vector.Element Object
   */
  def getElement(index: Int): Element

  /**
   * Return a new vector containing the values of the recipient divided by the argument
   *
   * @param x a Double value
   * @return a new Vector
   */
  def divide(x: Double): Vector

  /**
   * Return the dot product of the recipient and the argument
   *
   * @param x a Vector
   * @return a new Vector
   * @throws CardinalityException if the cardinalities differ
   */
  def dot(x: Vector): Double

  /**
   * Return the value at the given index
   *
   * @param index an int index
   * @return the Double at the index
   * @throws IndexException if the index is out of bounds
   */
  def get(index: Int): Double

  /**
   * Return the value at the given index, without checking bounds
   *
   * @param index an int index
   * @return the Double at the index
   */
  def apply(index: Int): Double

  /**
   * Return an empty vector of the same underlying class as the receiver
   *
   * @return a Vector
   */
  def like(): Vector

  /**
   * Return a new vector containing the element by element difference of the recipient and the argument
   *
   * @param x a Vector
   * @return a new Vector
   * @throws CardinalityException if the cardinalities differ
   */
  def minus(x: Vector): Vector

  /**
   * Return a new vector containing the normalized (L_2 norm) values of the recipient
   *
   * @return a new Vector
   */
  def normalize: Vector

  /**
   * Return a new Vector containing the normalized (L_power norm) values of the recipient. <p/> See
   * http://en.wikipedia.org/wiki/Lp_space <p/> Technically, when 0 < power < 1, we don't have a norm, just a metric,
   * but we'll overload this here. <p/> Also supports power == 0 (number of non-zero elements) and power = {@link
   * Double#POSITIVE_INFINITY} (max element). Again, see the Wikipedia page for more info
   *
   * @param power The power to use. Must be >= 0. May also be {@link Double#POSITIVE_INFINITY}. See the Wikipedia link
   *              for more on this.
   * @return a new Vector x such that norm(x, power) == 1
   */
  def normalize(power: Double): Vector
  
  /**
   * Return a new vector containing the log(1 + entry)/ L_2 norm  values of the recipient
   *
   * @return a new Vector
   */
  def logNormalize: Vector

  /**
   * Return a new Vector with a normalized value calculated as log_power(1 + entry)/ L_power norm. <p/>
   *
   * @param power The power to use. Must be > 1. Cannot be {@link Double#POSITIVE_INFINITY}.
   * @return a new Vector
   */
  def logNormalize(power: Double): Vector

  /**
   * Return the k-norm of the vector. <p/> See http://en.wikipedia.org/wiki/Lp_space <p/> Technically, when 0 &gt; power
   * &lt; 1, we don't have a norm, just a metric, but we'll overload this here. Also supports power == 0 (number of
   * non-zero elements) and power = {@link Double#POSITIVE_INFINITY} (max element). Again, see the Wikipedia page for
   * more info.
   *
   * @param power The power to use.
   * @see #normalize(Double)
   */
  def norm(power: Double): Double

  /** @return The minimum value in the Vector */
  def minValue: Double

  /** @return The index of the minimum value */
  def minValueIndex: Int

  /** @return The maximum value in the Vector */
  def maxValue: Double

  /** @return The index of the maximum value */
  def maxValueIndex: Int

  /**
   * Return a new vector containing the sum of each value of the recipient and the argument
   *
   * @param x a Double
   * @return a new Vector
   */
  def plus(x: Double): Vector

  /**
   * Return a new vector containing the element by element sum of the recipient and the argument
   *
   * @param x a Vector
   * @return a new Vector
   * @throws CardinalityException if the cardinalities differ
   */
  def plus(x: Vector): Vector

  /**
   * Set the value at the given index
   *
   * @param index an int index into the receiver
   * @param value a Double value to set
   * @throws IndexException if the index is out of bounds
   */
  def set(index: Int, value: Double)

  /**
   * Set the value at the given index, without checking bounds
   *
   * @param index an int index into the receiver
   * @param value a Double value to set
   */
  def update(index: Int, value: Double)

  /**
   * Return the number of values in the recipient which are not the default value.  For instance, for a
   * sparse vector, this would be the number of non-zero values.
   *
   * @return an int
   */
  def getNumNondefaultElements: Int

  /**
   * Return a new vector containing the product of each value of the recipient and the argument
   *
   * @param x a Double argument
   * @return a new Vector
   */
  def times(x: Double): Vector

  /**
   * Return a new vector containing the element-wise product of the recipient and the argument
   *
   * @param x a Vector argument
   * @return a new Vector
   * @throws CardinalityException if the cardinalities differ
   */
  def times(x: Vector): Vector

  /**
   * Return a new vector containing the subset of the recipient
   *
   * @param offset an int offset into the receiver
   * @param length the cardinality of the desired result
   * @return a new Vector
   * @throws CardinalityException if the length is greater than the cardinality of the receiver
   * @throws IndexException       if the offset is negative or the offset+length is outside of the receiver
   */
  def viewPart(offset: Int, length: Int): Vector

  /**
   * Return the sum of all the elements of the receiver
   *
   * @return a Double
   */
  def zSum: Double

  /**
   * Return the cross product of the receiver and the other vector
   *
   * @param other another Vector
   * @return a Matrix
   */
  def cross(other: Vector): Matrix

  /*
   * Need stories for these but keeping them here for now.
   */
  // void getNonZeros(IntArrayList jx, DoubleArrayList values)
  // void foreachNonZero(IntDoubleFunction f)
  // (Double, Double) => Double map)
  // NewVector assign(Vector y, (Double, Double) => Double function, IntArrayList
  // nonZeroIndexes)

  /**
   * Examples speak louder than words:  aggregate(plus, pow(2)) is another way to say
   * getLengthSquared(), aggregate(max, abs) is norm(Double.POSITIVE_INFINITY).  To sum all of the postive values,
   * aggregate(plus, max(0)).  
   * @param aggregator used to combine the current value of the aggregation with the result of map.apply(nextValue)
   * @param map a function to apply to each element of the vector in turn before passing to the aggregator
   * @return the final aggregation
   */
  def aggregate(aggregator: (Double, Double) => Double, map: Double => Double): Double

  /**
   * <p>Generalized inner product - take two vectors, iterate over them both, using the combiner to combine together
   * (and possibly map in some way) each pair of values, which are then aggregated with the previous accumulated
   * value in the combiner.</p>
   * <p>
   * Example: dot(other) could be expressed as aggregate(other, Plus, Times), and kernelized inner products (which
   * are symmetric on the indices) work similarly.
   * @param other a vector to aggregate in combination with
   * @param aggregator
   * @param combiner
   * @return the final aggregation
   */
  def aggregate(other: Vector, aggregator: (Double, Double) => Double, combiner: (Double, Double) => Double): Double

  /** Return the sum of squares of all elements in the vector. Square root of this value is the length of the vector. */
  def getLengthSquared: Double

  /** Get the square of the distance between this vector and the other vector. */
  def getDistanceSquared(v: Vector): Double
}

object Vector {
  /**
   * A holder for information about a specific item in the Vector. <p/> When using with an Iterator, the implementation
   * may choose to reuse this element, so you may need to make a copy if you want to keep it
   */
  trait Element {

    /** @return the value of this vector element. */
    def get: Double

    /** @return the index of this vector element. */
    def index: Int

    /** @param value Set the current element to value. */
    def set(value: Double)
  }

}