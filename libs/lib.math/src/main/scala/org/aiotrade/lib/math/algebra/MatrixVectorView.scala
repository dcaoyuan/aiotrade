package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.IndexException
import java.util.NoSuchElementException

/**
 * Provides a virtual vector that is really a row or column or diagonal of a matrix.
 */
class MatrixVectorView protected (private var matrix: Matrix, 
                                  private var row: Int, private var column: Int, 
                                  private var rowStride: Int, 
                                  private var columnStride: Int) extends AbstractVector(MatrixVectorView.viewSize(matrix, row, column, rowStride, columnStride)) {
 
  if (row < 0 || row > matrix.rowSize) {
    throw new IndexException(row, matrix.rowSize)
  }
  if (column < 0 || column > matrix.columnSize) {
    throw new IndexException(column, matrix.columnSize)
  }

  /**
   * @return true iff the {@link Vector} implementation should be considered
   *         dense -- that it explicitly represents every value
   */
  override
  def isDense = true

  /**
   * @return true iff {@link Vector} should be considered to be iterable in
   *         index order in an efficient way. In particular this implies that {@link #iterator()} and
   *         {@link #iterateNonZero()} return elements in ascending order by index.
   */
  override
  def isSequentialAccess = true

  /**
   * Iterates over all elements <p/> * NOTE: Implementations may choose to reuse the Element returned
   * for performance reasons, so if you need a copy of it, you should call {@link #getElement(Int)} for
   * the given index
   *
   * @return An {@link java.util.Iterator} over all elements
   */
  override
  def iterator: Iterator[Element] = {
    new Iterator[Element]() {
      private val r = new LocalElement(0)
      private var i: Int = 0

      def hasNext = i < MatrixVectorView.this.size
      def next: Element = {
        if (i >= MatrixVectorView.this.size) {
          throw new NoSuchElementException
        }
        r.index = i
        i += 1
        r
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
    matrix(row + rowStride * index, column + columnStride * index)
  }

  /**
   * Return an empty vector of the same underlying class as the receiver
   *
   * @return a Vector
   */
   override
   def like(): Vector = {
      matrix.like(size, 1).viewColumn(0)
    }

   /**
    * Set the value at the given index, without checking bounds
    *
    * @param index an Int index into the receiver
    * @param value a Double value to set
    */
   override
   def update(index: Int, value: Double) {
      matrix(row + rowStride * index, column + columnStride * index) = value
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

   /**
    * Subclasses must override to return an appropriately sparse or dense result
    *
    * @param rows    the row cardinality
    * @param columns the column cardinality
    * @return a Matrix
    */
   override
   protected[algebra] def matrixLike(rows: Int, columns: Int): Matrix = {
      val offset = Array(row, column)
      val size = Array(if (rowStride == 0) 1 else rowStride, if (columnStride == 0) 1 else columnStride)
      matrix.viewPart(offset, size)
    }

   override
   def clone: Vector = {
      val r = super.clone.asInstanceOf[MatrixVectorView]
      r.matrix = matrix.clone
      r.row = row
      r.column = column
      r.rowStride = rowStride
      r.columnStride = columnStride
      r
    }
   }

   object MatrixVectorView {
      def apply(matrix: Matrix, row: Int, column: Int, rowStride: Int, columnStride: Int) = new MatrixVectorView(matrix, row, column, rowStride, columnStride)
  
      private def viewSize(matrix: Matrix, row: Int, column: Int, rowStride: Int, columnStride: Int): Int = {
        if (rowStride != 0 && columnStride != 0) {
          val n1 = (matrix.numRows - row) / rowStride
          val n2 = (matrix.numCols - column) / columnStride
          return math.min(n1, n2)
        } else if (rowStride > 0) {
          return (matrix.numRows - row) / rowStride
        } else {
          return (matrix.numCols - column) / columnStride
        }
      }
    }
