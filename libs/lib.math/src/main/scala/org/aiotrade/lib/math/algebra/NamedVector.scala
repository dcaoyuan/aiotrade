package org.aiotrade.lib.math.algebra

class NamedVector private (private val delegate: Vector, val name: String) extends Vector {

  /** For serialization only */
  def this() = this(null, null)
  
  override
  def hashCode: Int = {
    delegate.hashCode
  }

  /**
   * To not break transitivity with other {@link Vector}s, this does not compare name.
   */
  override
  def equals(other: Any): Boolean = {
    delegate.equals(other)
  }

  override
  def clone: NamedVector = {
    NamedVector(delegate.clone, name)
  }

  override
  def asFormatString: String = {
    toString
  }

  override
  def toString: String = {
    val sb = new StringBuilder()
    sb.append(name).append(':').append(delegate.toString)
    sb.toString
  }

  override
  def assign(value: Double): Vector = {
    delegate.assign(value)
  }

  override
  def assign(values: Array[Double]): Vector = {
    delegate.assign(values)
  }

  override
  def assign(other: Vector): Vector = {
    delegate.assign(other)
  }

  override
  def assign(function: Double => Double): Vector = {
    delegate.assign(function)
  }

  override
  def assign(other: Vector, function: (Double, Double) => Double): Vector = {
    delegate.assign(other, function)
  }

  override
  def assign(f: (Double, Double) => Double, y: Double): Vector = {
    delegate.assign(f, y)
  }

  override
  def size: Int = {
    delegate.size
  }

  override
  def isDense: Boolean = {
    delegate.isDense
  }

  override
  def isSequentialAccess: Boolean = {
    delegate.isSequentialAccess
  }

  override
  def iterator: Iterator[Element] = {
    delegate.iterator
  }

  override
  def iterateNonZero: Iterator[Element] = {
    delegate.iterateNonZero
  }

  override
  def getElement(index: Int): Element = {
    delegate.getElement(index)
  }

  override
  def divide(x: Double): Vector = {
    delegate.divide(x)
  }

  override
  def dot(x: Vector): Double = {
    delegate.dot(x)
  }

  override
  def get(index: Int): Double = {
    delegate.get(index)
  }

  override
  def apply(index: Int): Double = {
    delegate.apply(index)
  }

  override
  def like(): NamedVector = {
    NamedVector(delegate.like, name)
  }

  override
  def minus(x: Vector): Vector = {
    return delegate.minus(x)
  }

  override
  def normalize: Vector = {
    delegate.normalize
  }

  override
  def normalize(power: Double): Vector = {
    delegate.normalize(power)
  }

  override
  def logNormalize: Vector = {
    delegate.logNormalize
  }

  override
  def logNormalize(power: Double): Vector = {
    delegate.logNormalize(power)
  }

  override
  def norm(power: Double): Double = {
    delegate.norm(power)
  }

  override
  def maxValue: Double = {
    delegate.maxValue
  }

  override
  def maxValueIndex: Int = {
    delegate.maxValueIndex
  }

  override
  def minValue: Double = {
    delegate.minValue
  }

  override
  def minValueIndex: Int = {
    delegate.minValueIndex
  }

  override
  def plus(x: Double): Vector = {
    delegate.plus(x)
  }

  override
  def plus(x: Vector): Vector = {
    delegate.plus(x)
  }

  override
  def set(index: Int, value: Double) {
    delegate.set(index, value)
  }

  override
  def update(index: Int, value: Double) {
    delegate.update(index, value)
  }

  override
  def getNumNondefaultElements: Int = {
    delegate.getNumNondefaultElements
  }

  override
  def times(x: Double): Vector = {
    delegate.times(x)
  }

  override
  def times(x: Vector): Vector = {
    delegate.times(x)
  }

  override
  def viewPart(offset: Int, length: Int): Vector = {
    delegate.viewPart(offset, length)
  }

  override
  def zSum: Double = {
    delegate.zSum
  }

  override
  def cross(other: Vector): Matrix = {
    delegate.cross(other)
  }

  override
  def aggregate(aggregator: (Double, Double) => Double, map: Double => Double): Double = {
    delegate.aggregate(aggregator, map)
  }

  override
  def aggregate(other: Vector, aggregator: (Double, Double) => Double, combiner: (Double, Double) => Double): Double = {
    delegate.aggregate(other, aggregator, combiner)
  }

  override
  def getLengthSquared: Double = {
    delegate.getLengthSquared
  }

  override
  def getDistanceSquared(v: Vector): Double = {
    delegate.getDistanceSquared(v)
  }
}

object NamedVector {

  def apply(delegate: Vector, name: String) = {
    if (delegate == null || name == null) {
      throw new IllegalArgumentException()
    }
    new NamedVector(delegate, name)
  }

  def apply(other: NamedVector) = new NamedVector(other.delegate, other.name)
}
