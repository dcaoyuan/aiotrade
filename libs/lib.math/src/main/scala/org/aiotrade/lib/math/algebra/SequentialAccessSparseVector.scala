package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.Functions
import scala.collection.mutable

/**
 * <p>
 * Implements vector that only stores non-zero Doubles as a pair of parallel arrays (OrderedIntDoubleMapping),
 * one Int[], one Double[].  If there are <b>k</b> non-zero elements in the vector, this implementation has
 * O(log(k)) random-access read performance, and O(k) random-access write performance, which is far below that
 * of the hashmap based {@link org.apache.mahout.math.RandomAccessSparseVector RandomAccessSparseVector}.  This
 * class is primarily used for operations where the all the elements will be accessed in a read-only fashion
 * sequentially: methods which operate not via get() or set(), but via iterateNonZero(), such as (but not limited
 * to) :</p>
 * <ul>
 *   <li>dot(Vector)</li>
 *   <li>addTo(Vector)</li>
 * </ul>
 * <p>
 * Note that the Vector passed to these above methods may (and currently, are) be used in a random access fashion,
 * so for example, calling SequentialAccessSparseVector.dot(SequentialAccessSparseVector) is slow.
 * TODO: this need not be the case - both are ordered, so this should be very fast if implmented in this class
 * </p>
 *
 * See {@link OrderedIntDoubleMapping}
 */
class SequentialAccessSparseVector private (cardinality: Int, private var values: OrderedIntDoubleMapping) extends AbstractVector(cardinality) {
  /** For serialization purposes only. */
  def this() = this(0, OrderedIntDoubleMapping(0))


  // Sorts a RandomAccessSparseVectors Elements before adding them to this
  private def copySortedRandomAccessSparseVector(other: Vector): Int = {
    val elementCount = other.getNumNondefaultElements
    val sortableElements = new Array[SequentialAccessSparseVector.OrderedElement](elementCount)
    val it = other.iterateNonZero
    var e: Vector.Element = null
    var s = 0
    while (it.hasNext && {e = it.next; e != null}) {
      sortableElements(s) = new SequentialAccessSparseVector.OrderedElement(e.index, e.get)
      s += 1
    }
    scala.util.Sorting.quickSort(sortableElements)
    
    var i = 0
    while (i < sortableElements.length) {
      values.indices(i) = sortableElements(i).index
      values.values(i) = sortableElements(i).value
      i += 1
    }
    values = OrderedIntDoubleMapping(values.indices, values.values, elementCount)
    elementCount
  }



  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    SparseRowMatrix(rows, columns)
  }

  override
  def clone: SequentialAccessSparseVector = {
    new SequentialAccessSparseVector(size, values.clone)
  }

  override
  def assign(other: Vector): Vector = {
    if (size != other.size) {
      throw new CardinalityException(size, other.size)
    }
    other match {
      case x: SequentialAccessSparseVector =>
        values = x.values.clone
      case _ =>
        values = new OrderedIntDoubleMapping()
        val itr = other.iterateNonZero
        while (itr.hasNext) {
          val e = itr.next
          this(e.index) = e.get
        }
    }
    this
  }

  override
  def toString = {
    val sb = new StringBuilder
    sb.append('{')
    val it = iterateNonZero
    while (it.hasNext) {
      val e = it.next
      sb.append(e.index)
      sb.append(':')
      sb.append(e.get)
      sb.append(',')
    }
    if (sb.length() > 1) {
      sb.setCharAt(sb.length() - 1, '}')
    }
    sb.toString
  }

  override
  def isDense = false

  override
  def isSequentialAccess = true

  override
  def apply(index: Int): Double = {
    values.get(index)
  }

  override
  def update(index: Int, value: Double) {
    lengthSquared = -1
    values.set(index, value)
  }

  override
  def getNumNondefaultElements: Int = {
    values.numMappings
  }

  override
  def like(): SequentialAccessSparseVector = {
    SequentialAccessSparseVector(size, values.numMappings)
  }

  override
  def iterateNonZero: Iterator[Element] = {
    new NonDefaultIterator()
  }

  override
  def iterator: Iterator[Element] = {
    new AllIterator()
  }

  override
  def minus(that: Vector): Vector = {
    if (size != that.size) {
      throw new CardinalityException(size, that.size)
    }
    // Here we compute "that - this" since it's not fast to randomly access "this"
    // and then invert at the end
    val result = that.clone
    val itr = this.iterateNonZero
    while (itr.hasNext) {
      val e = itr.next
      val index = e.index
      result(index) = that(index) - e.get
    }
    result.assign(Functions.NEGATE)
    result
  }


  private final class NonDefaultIterator extends Iterator[Element] {

    private val element = new NonDefaultElement

    def hasNext = values.numMappings > 0 && element.getNextOffset < values.numMappings
    def next = {
      if (values.numMappings <= 0 || element.getNextOffset >= values.numMappings) {
        null
      } else {
        element.advanceOffset
        element
      }
    }

  }

  private final class AllIterator extends Iterator[Element] {

    private final val element = new AllElement()

    def hasNext = values.numMappings > 0 && element.getNextIndex <= values.indices(values.numMappings - 1)
    def next = {
      if (values.numMappings <= 0 || element.getNextIndex > values.indices(values.numMappings - 1)) {
        null
      } else {
        element.advanceIndex
        element
      }
    }

  }

  private final class NonDefaultElement extends Element {
    private var offset = -1

    def advanceOffset {
      offset += 1
    }

    def getNextOffset = {
      offset + 1
    }

    def index = values.indices(offset)

    def get: Double = values.values(offset)
    def set(value: Double) {
      lengthSquared = -1
      values.values(offset) = value
    }
  }

  private final class AllElement extends Element {
    private var _index = -1
    private var nextOffset = 0

    def advanceIndex {
      _index += 1
      if (_index > values.indices(nextOffset)) {
        nextOffset += 1
      }
    }

    def getNextIndex = {
      _index + 1
    }

    def index = _index

    def get = {
      if (_index == values.indices(nextOffset)) {
        values.values(nextOffset)
      } else {
        OrderedIntDoubleMapping.DEFAULT_VALUE
      }
    }

    def set(value: Double) {
      lengthSquared = -1
      if (index == values.indices(nextOffset)) {
        values.values(nextOffset) = value
      } else {
        // Yes, this works; the offset Into indices of the new value's index will still be nextOffset
        values.set(index, value)
      }
    }
  }
  
}

object SequentialAccessSparseVector {
  
  def apply(cardinality: Int): SequentialAccessSparseVector = apply(cardinality, cardinality / 8) // arbitrary estimate of 'sparseness'

  def apply(other: Vector): SequentialAccessSparseVector = {
    val instance = apply(other.size, other.getNumNondefaultElements)

    if (other.isSequentialAccess) {
      val it = other.iterateNonZero
      var e: Vector.Element = null
      while (it.hasNext && {e = it.next; e != null}) {
        instance.set(e.index, e.get)
      }
    } else {
      // If the incoming Vector to copy is random, then adding items
      // from the Iterator can degrade performance dramatically if
      // the number of elements is large as this Vector tries to stay
      // in order as items are added, so it's better to sort the other
      // Vector's elements by index and then add them to this
      instance.copySortedRandomAccessSparseVector(other)
    }
    
    instance
  }

  def apply(other: SequentialAccessSparseVector, shallowCopy: Boolean) = new SequentialAccessSparseVector(other.size, if (shallowCopy) other.values else other.values.clone)

  def apply(cardinality: Int, size: Int) = new SequentialAccessSparseVector(cardinality, OrderedIntDoubleMapping(size))

  def apply(other: SequentialAccessSparseVector) = new SequentialAccessSparseVector(other.size, other.values.clone)

  
  // Comparable Element for sorting Elements by index
  protected final class OrderedElement(val index: Int, val value: Double) extends  Ordered[OrderedElement] {

    override
    def compare(that: OrderedElement): Int = {
      // both indexes are positive, and neither can be Integer.MAX_VALUE (otherwise there would be
      // an array somewhere with Integer.MAX_VALUE + 1 elements)
      this.index - that.index
    }

    override
    def hashCode: Int = {
      index ^ value.hashCode
    }

    override
    def equals(o: Any) = o match {
      case that: OrderedElement =>
        index == that.index && value == that.value
      case _ => false
    }
  }
}
