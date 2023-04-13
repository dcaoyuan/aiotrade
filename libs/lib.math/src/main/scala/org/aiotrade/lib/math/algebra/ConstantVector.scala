package org.aiotrade.lib.math.algebra

/**
 * Implements a vector with all the same values.
 */
class ConstantVector private (value: Double, _size: Int) extends AbstractVector(_size) {

  /**
   * Subclasses must override to return an appropriately sparse or dense result
   *
   * @param rows    the row cardinality
   * @param columns the column cardinality
   * @return a Matrix
   */
  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    DenseMatrix(rows, columns)
  }

  /**
   * @return true iff this implementation should be considered dense -- that it explicitly represents
   *         every value
   */
  override
  def isDense = true

  /**
   * @return true iff this implementation should be considered to be iterable in index order in an
   *         efficient way. In particular this implies that {@link #iterator()} and {@link
   *         #iterateNonZero()} return elements in ascending order by index.
   */
  override
  def isSequentialAccess = true

  /**
   * Iterates over all elements <p/> * NOTE: Implementations may choose to reuse the Element returned
   * for performance reasons, so if you need a copy of it, you should call {@link #getElement(Int)}
   * for the given index
   *
   * @return An {@link java.util.Iterator} over all elements
   */
  override
  def iterator: Iterator[Element] = {
    new Iterator[Element]() {
      private var i: Int = 0
      private val n = size
      def hasNext = i < n
      def next = {
        if (i < n) {
          val x = new LocalElement(i)
          i += 1
          x
        } else {
          null
        }
      }
    }
  }

  /**
   * Iterates over all non-zero elements. <p/> NOTE: Implementations may choose to reuse the Element
   * returned for performance reasons, so if you need a copy of it, you should call {@link
   * #getElement(Int)} for the given index
   *
   * @return An {@link java.util.Iterator} over all non-zero elements
   */
  override
  def iterateNonZero: Iterator[Element] = {
    iterator
  }

  /**
   * Return the value at the given index, without checking bounds
   *
   * @param index an Int index
   * @return the Double at the index
   */
  override
  def apply(index: Int): Double = {
    value;
  }

  /**
   * Return an empty vector of the same underlying class as the receiver
   *
   * @return a Vector
   */
  override
  def like(): Vector = {
    DenseVector(size)
  }

  /**
   * Set the value at the given index, without checking bounds
   *
   * @param index an Int index into the receiver
   * @param value a Double value to set
   */
  override
  def update(index: Int, value: Double) {
    throw new UnsupportedOperationException("Can't set a value in a constant matrix")
  }

  /**
   * Return the number of values in the recipient
   *
   * @return an Int
   */
  override
  def getNumNondefaultElements: Int = {
    size
  }
}

object ConstantVector {
  def apply(value: Double, _size: Int) = new ConstantVector(value, _size)
}