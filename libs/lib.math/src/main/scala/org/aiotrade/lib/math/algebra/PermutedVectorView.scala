package org.aiotrade.lib.math.algebra


/**
 * Provides a permuted view of a vector.
 * @param vector;            // the vector containing the data
 * @param pivot;              // convert from external index to internal
 * @param unpivot;            // convert from internal index to external
 */
class PermutedVectorView private (vector: Vector, pivot: Array[Int], unpivot: Array[Int]) extends AbstractVector(vector.size) {


  /**
   * Subclasses must override to return an appropriately sparse or dense result
   *
   * @param rows    the row cardinality
   * @param columns the column cardinality
   * @return a Matrix
   */
  override
  protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
    if (vector.isDense) {
      DenseMatrix(rows, columns)
    } else {
      SparseRowMatrix(rows, columns)
    }
  }

  /**
   * @return true iff this implementation should be considered dense -- that it explicitly
   *         represents every value
   */
  override
  def isDense = vector.isDense

  /**
   * @return true iff this implementation should be considered to be iterable in index order in an
   *         efficient way. In particular this implies that {@link #iterator()} and {@link
   *         #iterateNonZero()} return elements in ascending order by index.
   */
  override
  def isSequentialAccess = vector.isSequentialAccess

  /**
   * Iterates over all elements <p/> * NOTE: Implementations may choose to reuse the Element
   * returned for performance reasons, so if you need a copy of it, you should call {@link
   * #getElement(Int)} for the given index
   *
   * @return An {@link java.util.Iterator} over all elements
   */
  override
  def  iterator: Iterator[Element] = {
    new Iterator[Element]() {
      private val i = vector.iterator

      def hasNext = i.hasNext
      def next: Vector.Element = {
        if (i.hasNext) {
          val x = i.next
          new Element() {
            private val _index = unpivot(x.index)

            def index = _index
            def get = x.get
            def set(value: Double) {
              x.set(value)
            }
          }
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
    new Iterator[Element]() {
      private val i = vector.iterateNonZero

      def hasNext = i.hasNext
      def next: Vector.Element = {
        if (i.hasNext) {
          val x = i.next
          new Element() {
            private val _index = unpivot(x.index)

            def index = _index
            def get = x.get
            def set(value: Double) {
              x.set(value)
            }
          }
        } else {
          null
        }
      }
    }
  }

  /**
   * Return the value at the given index, without checking bounds
   *
   * @param index an Int index
   * @return the Double at the index
   */
  override
  def apply(index: Int): Double = {
    vector.apply(pivot(index))
  }

  /**
   * Return an empty vector of the same underlying class as the receiver
   *
   * @return a Vector
   */
  override
  def like(): Vector = {
    vector.like()
  }

  /**
   * Set the value at the given index, without checking bounds
   *
   * @param index an Int index into the receiver
   * @param value a Double value to set
   */
  override
  def update(index: Int, value: Double) {
    vector.update(pivot(index), value)
  }

  /**
   * Return the number of values in the recipient
   *
   * @return an Int
   */
  override
  def getNumNondefaultElements: Int = {
    vector.getNumNondefaultElements
  }
}

object PermutedVectorView {
  def apply(vector: Vector, pivot: Array[Int], unpivot: Array[Int]) = new PermutedVectorView(vector, pivot, unpivot)
  def apply(vector: Vector, pivot: Array[Int]) = new PermutedVectorView(vector, pivot, reversePivotPermutation(pivot))

  private def reversePivotPermutation(pivot: Array[Int]): Array[Int] = {
    val unpivot1 = new Array[Int](pivot.length)
    var i = 0
    while (i < pivot.length) {
      unpivot1(pivot(i)) = i
      i += 1
    }
    unpivot1
  }
}
