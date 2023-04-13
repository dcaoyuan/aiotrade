package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.IndexException

/** Implements subset view of a Vector */
class VectorView private (private var vector: Vector, private var offset: Int, cardinality: Int) extends AbstractVector(cardinality) {

  /** For serialization purposes only */
  def this() = {
    this(null, 0, 0)
  }

  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    vector.asInstanceOf[AbstractVector].matrixLike(rows, columns)
  }

  override
  def clone: Vector = {
    val r = super.clone.asInstanceOf[VectorView]
    r.vector = vector.clone
    r.offset = offset
    r
  }

  override
  def isDense = vector.isDense

  override
  def isSequentialAccess = vector.isSequentialAccess

  override
  def like(): VectorView = {
    new VectorView(vector.like, offset, size)
  }
  
  override
  def apply(index: Int): Double = {
    vector.apply(offset + index)
  }

  override
  def update(index: Int, value: Double) {
    vector.update(offset + index, value)
  }

  override
  def getNumNondefaultElements: Int = {
     size
  }

  override
  def viewPart(offset: Int, length: Int): Vector = {
    if (offset < 0) {
      throw new IndexException(offset, size)
    }
    if (offset + length > size) {
      throw new IndexException(offset + length, size)
    }
    new VectorView(vector, offset + this.offset, length)
  }

  /** @return true if index is a valid index in the underlying Vector */
  private def isInView(index: Int): Boolean = {
    index >= offset && index < offset + size
  }

  override
  def iterateNonZero: Iterator[Element] = {
    new NonZeroIterator()
  }

  override
  def iterator: Iterator[Element] = {
     new AllIterator()
  }

  final class NonZeroIterator extends Iterator[Element] {
    private val itr = vector.iterateNonZero
    private var nextOne: Element = null

    def next = nextOne
    def hasNext = {
      nextOne = computeNext
      nextOne ne null
    }

    private def computeNext: Element = {
      while (itr.hasNext) {
        val el = itr.next
        if (isInView(el.index) && el.get != 0) {
          val decorated = vector.getElement(el.index)
          return new DecoratorElement(decorated)
        }
      }
      null
    }
  }

  final class AllIterator extends Iterator[Element] {
    private val itr = vector.iterator
    private var nextOne: Element = null

    def next = nextOne
    def hasNext = {
      nextOne = computeNext
      nextOne ne null
    }
    private def computeNext: Element = {
      while (itr.hasNext) {
        val el = itr.next
        if (isInView(el.index)) {
          val decorated = vector.getElement(el.index)
          return new DecoratorElement(decorated)
        }
      }
      null // No element was found
    }

  }

  private final class DecoratorElement(decorated: Element) extends Element {

    def index: Int = {
      decorated.index - offset
    }

    def get = decorated.get
    def set(value: Double) {
      decorated.set(value)
    }
  }

  override
  def getLengthSquared: Double = {
    var result = 0.0
    val n = size
    var i = 0
    while (i < n) {
      val value = this(i)
      result += value * value
      i += 1
    }
     result
  }

  override
  def getDistanceSquared(v: Vector): Double = {
    var result = 0.0
    val n = size
    var i = 0
    while (i < n) {
      val delta = this(i) - v(i)
      result += delta * delta
      i += 1
    }
    result
  }
}

object VectorView {
  def apply(vector: Vector, offset: Int, cardinality: Int) = new VectorView(vector, offset, cardinality)
}
