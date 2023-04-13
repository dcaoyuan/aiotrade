package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException
import org.aiotrade.lib.math.Functions


/** Implementations of generic capabilities like sum of elements and dot products */
abstract class AbstractVector protected (private var _size: Int) extends Vector {
  protected var lengthSquared = -1.0

  def aggregate(aggregator: (Double, Double) => Double, map: Double => Double): Double = {
    if (size < 1) {
      throw new IllegalArgumentException("Cannot aggregate empty vector")
    }
    var result = map(this(0))
    var i = 1
    while (i < size) {
      result = aggregator(result, map(this(i)))
      i += 1
    }
    result
  }

  def aggregate(other: Vector, aggregator: (Double, Double) => Double, combiner: (Double, Double) => Double): Double = {
    if (size < 1) {
      throw new IllegalArgumentException("Cannot aggregate empty vector")
    }
    var result = combiner(this(0), other(0))
    var i = 1
    while (i < size) {
      result = aggregator(result, combiner(this(i), other(i)))
      i += 1
    }
    result
  }

  /**
   * Subclasses must override to return an appropriately sparse or dense result
   *
   * @param rows    the row cardinality
   * @param columns the column cardinality
   * @return a Matrix
   */
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix

  def viewPart(offset: Int, length: Int): Vector = {
    if (offset < 0) {
      throw new IndexException(offset, size)
    }
    if (offset + length > size) {
      throw new IndexException(offset + length, size)
    }
    VectorView(this, offset, length)
  }

  override 
  def clone: Vector = {
    try {
      val r = super.clone.asInstanceOf[AbstractVector]
      r._size = size
      r.lengthSquared = lengthSquared
      return r
    } catch {
      case e: CloneNotSupportedException =>
        throw new IllegalStateException("Can't happen")
    }
  }

  def divide(x: Double): Vector = {
    if (x == 1.0) {
      return like.assign(this)
    }
    val result = like.assign(this)
    val itr = result.iterateNonZero
    while (itr.hasNext) {
      val element = itr.next
      element.set(element.get / x)
    }
    result
  }

  def dot(x: Vector): Double = {
    if (size != x.size) {
      throw new CardinalityException(size, x.size)
    }
    if (this == x) {
      return dotSelf
    }

    // Crude rule of thumb: when a sequential-access vector, with O(log n) lookups, has about
    // 2^n elements, its lookups take longer than a dense / random access vector (with O(1) lookups) by
    // about a factor of (0.71n - 12.3). This holds pretty well from n=19 up to at least n=23 according to my tests;
    // below that lookups are so fast that this difference is near zero.

    val thisNumNonDefault = getNumNondefaultElements
    val thatNumNonDefault = x.getNumNondefaultElements
    // Default: dot from smaller vector to larger vector
    var reverseDot = thatNumNonDefault < thisNumNonDefault

    // But, see if we should override that -- is exactly one of them sequential access and so slower to lookup in?
    if (isSequentialAccess != x.isSequentialAccess) {
      val log2ThisSize = math.log(thisNumNonDefault) / AbstractVector.LOG2
      val log2ThatSize = math.log(thatNumNonDefault) / AbstractVector.LOG2
      // Only override when the O(log n) factor seems big enough to care about:
      if (log2ThisSize >= 19.0 && log2ThatSize >= 19.0) {
        var dotCost = thisNumNonDefault.toDouble
        if (x.isSequentialAccess) {
          dotCost *= 0.71 * log2ThatSize - 12.3
        }
        var reverseDotCost = thatNumNonDefault.toDouble
        if (isSequentialAccess) {
          reverseDotCost *= 0.71 * log2ThisSize - 12.3
        }
        reverseDot = reverseDotCost < dotCost
      }
    }

    if (reverseDot) {
      return x.dot(this)
    }

    var result = 0.0
    val itr = iterateNonZero
    while (itr.hasNext) {
      val element = itr.next
      result += element.get * x(element.index)
    }
    result
  }
  
  def dotSelf: Double = {
    var result = 0.0
    val itr = iterateNonZero
    while (itr.hasNext) {
      val value = itr.next.get
      result += value * value
    }
    result
  }

  def get(index: Int): Double = {
    if (index < 0 || index >= size) {
      throw new IndexException(index, size)
    }
    this(index)
  }

  def getElement(index: Int): Element = {
    new LocalElement(index)
  }

  def minus(that: Vector): Vector = {
    if (size != that.size) {
      throw new CardinalityException(size, that.size)
    }

    // TODO: check the numNonDefault elements to further optimize
    val result = like.assign(this)
    val itr = that.iterateNonZero
    while (itr.hasNext) {
      val thatElement = itr.next
      val index = thatElement.index
      result(index) = this(index) - thatElement.get
    }
    result
  }

  def normalize: Vector = {
    divide(math.sqrt(dotSelf))
  }

  def normalize(power: Double): Vector = {
    divide(norm(power))
  }
  
  def logNormalize: Vector = {
    logNormalize(2.0, math.sqrt(dotSelf))
  }
  
  def logNormalize(power: Double): Vector = {
    logNormalize(power, norm(power))
  }
  
  def logNormalize(power: Double, normLength: Double): Vector = {   
    // we can special case certain powers
    if (java.lang.Double.isInfinite(power) || power <= 1.0) {
      throw new IllegalArgumentException("Power must be > 1 and < infinity")
    } else {
      val denominator = normLength * math.log(power)
      val result = like.assign(this)
      val itr = result.iterateNonZero
      while (itr.hasNext) {
        val element = itr.next
        element.set(math.log1p(element.get) / denominator)
      }
      result
    }
  }

  def norm(power: Double): Double = {
    if (power < 0.0) {
      throw new IllegalArgumentException("Power must be >= 0")
    }
    // we can special case certain powers
    if (java.lang.Double.isInfinite(power)) {
      var v = 0.0
      val itr = this.iterateNonZero
      while (itr.hasNext) {
        v = math.max(v, math.abs(itr.next.get))
      }
      return v
    } else if (power == 2.0) {
      return math.sqrt(dotSelf)
    } else if (power == 1.0) {
      var v = 0.0
      val itr = this.iterateNonZero
      while (itr.hasNext) {
        v += math.abs(itr.next.get)
      }
      return v
    } else if (power == 0.0) {
      // this is the number of non-zero elements
      var v = 0.0
      val iter = this.iterateNonZero
      while (iter.hasNext) {
        v += (if (iter.next.get == 0) 0 else 1)
      }
      return v
    } else {
      var v = 0.0
      val itr = this.iterateNonZero
      while (itr.hasNext) {
        val element = itr.next
        v += math.pow(element.get, power)
      }
      return math.pow(v, 1.0 / power)
    }
  }

  def getLengthSquared: Double = {
    if (lengthSquared >= 0.0) {
      lengthSquared
    } else {
      lengthSquared = dotSelf
      lengthSquared
    }
  }

  def getDistanceSquared(v: Vector): Double = {
    if (size != v.size) {
      throw new CardinalityException(size, v.size)
    }
    // if this and v has a cached lengthSquared, dot product is quickest way to compute this.
    if (lengthSquared >= 0 && v.isInstanceOf[AbstractVector] && v.asInstanceOf[AbstractVector].lengthSquared >= 0) {
      return lengthSquared + v.getLengthSquared - 2 * this.dot(v)
    }
    var randomlyAccessed: Vector = null
    var d = 0.0
    val itr = if (lengthSquared >= 0.0) {
      randomlyAccessed = this
      d += lengthSquared
      v.iterateNonZero
    } else { // TODO: could be further optimized, figure out which one is smaller, etc
      randomlyAccessed = v
      d += v.getLengthSquared
      iterateNonZero
    }
    while (itr.hasNext) {
      val e = itr.next
      val value = e.get
      d += value * (value - 2.0 * randomlyAccessed(e.index))
    }
    //assert d > -1.0e-9; // round-off errors should never be too far off!
    math.abs(d)
  }

  def maxValue: Double = {
    var result = java.lang.Double.NEGATIVE_INFINITY
    var nonZeroElements = 0
    val itr = this.iterateNonZero
    while (itr.hasNext) {
      nonZeroElements += 1
      val element = itr.next
      result = math.max(result, element.get)
    }
    if (nonZeroElements < size) {
      return math.max(result, 0.0)
    }
    result
  }
  
  def maxValueIndex: Int = {
    var result = -1
    var max = java.lang.Double.NEGATIVE_INFINITY;
    var nonZeroElements = 0
    val itr = this.iterateNonZero
    while (itr.hasNext) {
      nonZeroElements += 1
      val element = itr.next
      val tmp = element.get
      if (tmp > max) {
        max = tmp
        result = element.index
      }
    }
    // if the maxElement is negative and the vector is sparse then any
    // unfilled element(0.0) could be the maxValue hence we need to
    // find one of those elements
    if (nonZeroElements < size && max < 0.0) {
      for (element <- this) {
        if (element.get == 0.0) {
          return element.index
        }
      }
    }
    result
  }

  def minValue: Double = {
    var result = java.lang.Double.POSITIVE_INFINITY;
    var nonZeroElements = 0
    val itr = this.iterateNonZero
    while (itr.hasNext) {
      nonZeroElements += 1
      val element = itr.next
      result = math.min(result, element.get)
    }
    if (nonZeroElements < size) {
      return math.min(result, 0.0)
    }
    result
  }

  def minValueIndex: Int = {
    var result = -1
    var min = java.lang.Double.POSITIVE_INFINITY
    var nonZeroElements = 0
    val itr = this.iterateNonZero
    while (itr.hasNext) {
      nonZeroElements += 1
      val element = itr.next
      val tmp = element.get
      if (tmp < min) {
        min = tmp
        result = element.index
      }
    }
    // if the maxElement is positive and the vector is sparse then any
    // unfilled element(0.0) could be the maxValue hence we need to
    // find one of those elements
    if (nonZeroElements < size && min > 0.0) {
      for (element <- this) {
        if (element.get == 0.0) {
          return element.index
        }
      }
    }
    result
  }

  def plus(x: Double): Vector = {
    val result = like.assign(this)
    if (x == 0.0) {
      return result
    }
    val size = result.size
    var i = 0
    while (i < size) {
      result(i) = this(i) + x
      i += 1
    }
    result
  }

  def plus(x: Vector): Vector = {
    if (size != x.size) {
      throw new CardinalityException(size, x.size)
    }

    // prefer to have this be the denser than x
    if (!isDense && (x.isDense || x.getNumNondefaultElements > this.getNumNondefaultElements)) {
      return x.plus(this)
    }

    val result = like.assign(this)
    val itr = x.iterateNonZero
    while (itr.hasNext) {
      val e = itr.next
      val index = e.index
      result(index) = this(index) + e.get
    }
    result
  }

  def set(index: Int, value: Double) {
    if (index < 0 || index >= size) {
      throw new IndexException(index, size)
    }
    this(index) = value
  }

  def times(x: Double): Vector = {
    if (x == 0.0) {
      return like
    }

    val result = like.assign(this)
    if (x == 1.0) {
      return result
    }
    
    val itr = result.iterateNonZero
    while (itr.hasNext) {
      val element = itr.next
      element.set(element.get * x)
    }

    result
  }

  def times(x: Vector): Vector = {
    if (size != x.size) {
      throw new CardinalityException(size, x.size)
    }

    var to: Vector = this
    var from = x
    // Clone and edit to the sparse one; if both are sparse, edit the more sparse one (more zeroes)
    if (isDense || (!x.isDense && getNumNondefaultElements > x.getNumNondefaultElements)) {
      to = x
      from = this
    }

    val result = to.like.assign(to)
    val itr = result.iterateNonZero
    while (itr.hasNext) {
      val element = itr.next
      element.set(element.get * from(element.index))
    }

    result
  }

  def zSum: Double = {
    var result = 0.0
    val itr = iterateNonZero
    while (itr.hasNext) {
      result += itr.next.get
    }

    result
  }

  def assign(value: Double): Vector = {
    var i = 0
    while (i < size) {
      this(i) = value
      i += 1
    }
    this
  }

  def assign(values: Array[Double]): Vector = {
    if (size != values.length) {
      throw new CardinalityException(size, values.length)
    }
    var i = 0
    while (i < size) {
      this(i) = values(i)
      i += 1
    }
    this
  }

  def assign(other: Vector): Vector = {
    if (size != other.size) {
      throw new CardinalityException(size, other.size)
    }
    var i = 0
    while (i < size) {
      this(i) = other(i)
      i += 1
    }
    this
  }

  def assign(f: (Double, Double) => Double, y: Double): Vector = {
    val itr = if (f(0, y) == 0) iterateNonZero else iterator
    while (itr.hasNext) {
      val e = itr.next
      e.set(f(e.get, y))
    }
    this
  }

  def assign(function: Double => Double): Vector = {
    val itr = if (function(0) == 0) iterateNonZero else iterator
    while (itr.hasNext) {
      val e = itr.next
      e.set(function(e.get))
    }
    this
  }

  def assign(other: Vector, function: (Double, Double) => Double): Vector = {
    if (size != other.size) {
      throw new CardinalityException(size, other.size)
    }

    /* special case: we only need to iterate over the non-zero elements of the vector to add */
    if (function == Functions.PLUS) {
      val nonZeroElements = other.iterateNonZero
      while (nonZeroElements.hasNext) {
        val e = nonZeroElements.next
        this(e.index) = function(this(e.index), e.get)
      }
    } else {
      var i = 0
      while (i < size) {
        this(i) = function(this(i), other(i))
        i += 1
      }
    }
    this
  }

  def cross(other: Vector): Matrix = {
    val result = matrixLike(size, other.size)
    var row = 0
    while (row < size) {
      result.assignRow(row, other.times(this(row)))
      row += 1
    }
    result
  }

  override 
  final def size: Int = _size

  def asFormatString: String = {
    toString
  }

  override 
  def hashCode: Int = {
    var result = size
    val itr = iterateNonZero
    while (itr.hasNext) {
      val ele = itr.next
      val v = java.lang.Double.doubleToLongBits(ele.get)
      result += ele.index * (v ^ (v >>> 32)).toInt
    }
    result
  }

  /**
   * Determines whether this {@link Vector} represents the same logical vector as another
   * object. Two {@link Vector}s are equal (regardless of implementation) if the value at
   * each index is the same, and the cardinalities are the same.
   */
  override 
  def equals(o: Any): Boolean = o match {
    case that: Vector =>
      if (this eq that) {
        return true
      }
      if (size != that.size) {
        false
      } else {
        var index = 0
        while (index < size) {
          if (this(index) != that(index)) {
            return false
          }
          index += 1
        }
        true
      }
    case _ => false
  }

  override def toString: String = {
    toString(null)
  }

  def toString(dictionary: Array[String]): String = {
    val sb = new StringBuilder()
    sb.append('{')
    var index = 0
    while (index < size) {
      val value = this(index)
      if (value != 0.0) {
        sb.append(if (dictionary != null && dictionary.length > index) dictionary(index) else index)
        sb.append(':')
        sb.append(value)
        sb.append(',')
      }
      index += 1
    }
    if (sb.length > 1) {
      sb.setCharAt(sb.length - 1, '}')
    } else {
      sb.append('}')
    }
    sb.toString
  }


  protected final class LocalElement(var index: Int) extends Element {
    def get = AbstractVector.this(index)
    def set(value: Double) {
      AbstractVector.this(index) = value
    }
  }
}

object AbstractVector {
  private val LOG2 = math.log(2.0)
}
