package org.aiotrade.lib.math.algebra


import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.CardinalityException
import scala.collection.mutable


/** Implements vector that only stores non-zero doubles */
class RandomAccessSparseVector(cardinality: Int, private val values: mutable.Map[Int, Double]) extends AbstractVector(cardinality) {
  /** For serialization purposes only. */
  def this() = this(0, null)

  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    SparseRowMatrix(rows, columns)
  }

  override
  def clone: RandomAccessSparseVector = {
    RandomAccessSparseVector(size, values.clone.asInstanceOf[mutable.Map[Int, Double]])
  }

  override
  def toString = {
    val sb = new StringBuilder()
    sb.append('{')
    val itr = iterateNonZero
    var first = true
    while (itr.hasNext) {
      if (first) {
        first = false
      } else {
        sb.append(',')
      }
      val e = itr.next
      sb.append(e.index)
      sb.append(':')
      sb.append(e.get)
    }
    sb.append('}')
    sb.toString
  }

  override
  def assign(other: Vector): Vector = {
    if (size != other.size) {
      throw new CardinalityException(size, other.size)
    }
    values.clear
    val itr = other.iterateNonZero
    while (itr.hasNext) {
      val e = itr.next
      if (e != null && e.get != 0) {
        this(e.index) = e.get
      }
    }
    this
  }

  /**
   * @return false
   */
  override
  def isDense = false

  /**
   * @return false
   */
  override
  def isSequentialAccess = false

  override
  def apply(index: Int): Double = {
    values.get(index).getOrElse(0.0)
  }

  override
  def update(index: Int, value: Double) {
    lengthSquared = -1.0
    if (value == 0.0) {
      values -= index
    } else {
      values += (index -> value)
    }
  }

  override
  def getNumNondefaultElements: Int = {
    values.size
  }

  override
  def like(): RandomAccessSparseVector = {
    RandomAccessSparseVector(size, values.size)
  }

  /**
   * NOTE: this implementation reuses the Vector.Element instance for each call of next(). If you need to preserve the
   * instance, you need to make a copy of it
   *
   * @return an {@link Iterator} over the Elements.
   * @see #getElement(Int)
   */
  override
  def iterateNonZero: Iterator[Element] = {
    new NonDefaultIterator()
  }
  
  override
  def iterator: Iterator[Element] = {
    new AllIterator()
  }

  private final class NonDefaultIterator extends Iterator[Element] {

    private final val element = new RandomAccessElement()
    private final val indices = new ArrayList[Int]() ++= values.keys
    private var offset: Int = 0

    def hasNext = offset < indices.size
    def next: Element = {
      if (offset >= indices.size) {
        return null
      }
      element.index = indices(offset)
      offset += 1
      element
    }

  }

  private final class AllIterator extends Iterator[Element] {

    private final val element = new RandomAccessElement()
    element.index = -1
    def hasNext = element.index + 1 < RandomAccessSparseVector.this.size
    def next: Element = {
      if (element.index + 1 < RandomAccessSparseVector.this.size) {
        element.index += 1
        element
      } else {
        null
      }
    }

  }

  private final class RandomAccessElement extends Element {
    var index: Int = _

    def get = values(index)
    def set(value: Double) {
      lengthSquared = -1
      if (value == 0.0) {
        values -= index
      } else {
        values += (index -> value)
      }
    }
  }
  
}

object RandomAccessSparseVector {
  private val INITIAL_CAPACITY = 11


  def apply(cardinality: Int): RandomAccessSparseVector = {
    apply(cardinality, math.min(cardinality, INITIAL_CAPACITY)) // arbitrary estimate of 'sparseness'
  }

  def apply(cardinality: Int, initialCapacity: Int) = {
    val values = new mutable.HashMap[Int, Double]() // initialCapacity ?
    new RandomAccessSparseVector(cardinality, values)
  }

  def apply(other: Vector) = {
    val values = new mutable.HashMap[Int, Double]() // other.getNumNondefaultElements
    val itr = other.iterateNonZero
    while (itr.hasNext) {
      val e = itr.next
      if (e != null && e.get != 0.0) {
        values += (e.index -> e.get)
      }
    }
    new RandomAccessSparseVector(other.size, values)
  }

  def apply(other: RandomAccessSparseVector, shallowCopy: Boolean) {
    val values = if (shallowCopy) other.values else other.values.clone.asInstanceOf[mutable.Map[Int, Double]]
    new RandomAccessSparseVector(other.size, values)
  }
  
  private def apply(cardinality: Int, values: mutable.Map[Int, Double]) = new RandomAccessSparseVector(cardinality, values)
}
