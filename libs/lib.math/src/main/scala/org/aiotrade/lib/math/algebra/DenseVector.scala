package org.aiotrade.lib.math.algebra


/** Implements vector as an array of doubles */
import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.Functions
import org.aiotrade.lib.math.IndexException


class DenseVector protected (private var values: Array[Double]) extends AbstractVector(values.length) {

  /** For serialization purposes only */
  def this() = {
    this(new Array[Double](0))
  }

  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    DenseMatrix(rows, columns)
  }

  override
  def clone: DenseVector = {
    DenseVector(values.clone)
  }

  /**
   * @return true
   */
  override
  def isDense = true

  /**
   * @return true
   */
  override
  def isSequentialAccess = true

  override
  def dotSelf: Double = {
    var result = 0.0
    val max = size
    var i = 0
    while (i < max) {
      val value = this.apply(i)
      result += value * value
      i += 1
    }
    result
  }

  override
  def apply(index: Int): Double = {
    values(index)
  }

  override
  def like(): DenseVector = {
    DenseVector(size)
  }

  override
  def update(index: Int, value: Double) {
    lengthSquared = -1
    values(index) = value
  }
  
  override
  def assign(value: Double): Vector = {
    lengthSquared = -1
    java.util.Arrays.fill(values, value)
    this
  }
  
  override
  def assign(other: Vector, function: (Double, Double) => Double): Vector = {
    if (size != other.size) {
      throw new CardinalityException(size, other.size)
    }
    // is there some other way to know if function(0, x) = x for all x?
    function match {
      case f: Functions.PlusMult =>
        val itr = other.iterateNonZero
        var e: Element = null
        while (itr.hasNext && {e = itr.next; e != null}) {
          values(e.index) = function(values(e.index), e.get)
        }
      case _ =>
        var i = 0
        while (i < size) {
          values(i) = function(values(i), other(i))
          i += 1
        }
    }
    lengthSquared = -1
    this
  }

  def assign(vector: DenseVector): Vector = {
    // make sure the data field has the correct length
    if (vector.values.length != values.length) {
      values = new Array[Double](vector.values.length)
    }
    // now copy the values
    System.arraycopy(vector.values, 0, values, 0, values.length)
    this
  }

  override
  def getNumNondefaultElements: Int = {
    values.length
  }

  override
  def viewPart(offset: Int, length: Int): Vector = {
    if (offset < 0) {
      throw new IndexException(offset, size)
    }
    if (offset + length > size) {
      throw new IndexException(offset + length, size)
    }
    VectorView(this, offset, length)
  }

  /**
   * Returns an iterator that traverses this Vector from 0 to cardinality-1, in that order.
   */
  override
  def iterateNonZero: Iterator[Element] = {
    new NonDefaultIterator()
  }

  override
  def iterator: Iterator[Element] = {
    new AllIterator()
  }

  override
  def equals(o: Any): Boolean = o match {
    case that: DenseVector =>
      java.util.Arrays.equals(values, that.values) // Speedup for DenseVectors
    case _ =>
      super.equals(o)
  }

  override
  def getLengthSquared: Double = {
    if (lengthSquared >= 0.0) {
      return lengthSquared
    }

    var result = 0.0
    var i = 0
    while (i < values.length) {
      val value = values(i)
      result += value * value
      i += 1
    }
    lengthSquared = result
    result
  }

  def addAll(v: Vector) {
    if (size != v.size) {
      throw new CardinalityException(size, v.size)
    }
    
    val itr = v.iterateNonZero
    while (itr.hasNext) {
      val element = itr.next
      values(element.index) += element.get
    }
  }

  private final class NonDefaultIterator extends Iterator[Element] {
    private val element = new DenseElement()
    private var index = 0

    def hasNext = {
      while (index < DenseVector.this.size && values(index) == 0.0) {
        index += 1
      }
      index < DenseVector.this.size
    }
    def next = {
      if (hasNext) {
        element.index = index
        index += 1
        element
      } else {
        null
      }
    }

  }

  private final class AllIterator extends Iterator[Element] {
    private val element = new DenseElement()
    element.index = -1

    def hasNext = element.index + 1 < DenseVector.this.size
    def next = {
      if (hasNext) {
        element.index += 1
        element
      } else {
        null
      }
    }
  }

  private final class DenseElement extends Element {
    var index: Int = _

    def get = values(index)
    def set(value: Double) {
      lengthSquared = -1
      values(index) = value
    }
  }

}

object DenseVector {
  /** Construct a new instance using provided values */
  def apply(values: Array[Double]) = new DenseVector(values)

  def apply(values: Array[Double], shallowCopy: Boolean) = new DenseVector(if (shallowCopy) values else values.clone)

  def apply(values: DenseVector, shallowCopy: Boolean): DenseVector = apply(values.values, shallowCopy)

  /** Construct a new instance of the given cardinality */
  def apply(cardinality: Int) = new DenseVector(new Array[Double](cardinality))

  /**
   * Copy-constructor (for use in turning a sparse vector into a dense one, for example)
   * @param vector
   */
  def apply(vector: Vector) = {
    val values = new Array[Double](vector.size)
    val itr = vector.iterateNonZero
    while (itr.hasNext) {
      val e = itr.next
      values(e.index) = e.get
    }
    new DenseVector(values)
  }
}