package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException

/**
 * sparse matrix with general element values whose rows are accessible quickly. Implemented as a row array of
 * either SequentialAccessSparseVectors or RandomAccessSparseVectors.
 */
class SparseRowMatrix private (rows: Int, columns: Int, private var rowVectors: Array[Vector], randomAccessRows: Boolean) extends AbstractMatrix(rows, columns) {

  override
  def clone: Matrix = {
    val x = super.clone.asInstanceOf[SparseRowMatrix]
    x.rowVectors = new Array[Vector](rowVectors.length)
    var i = 0
    while (i < rowVectors.length) {
      x.rowVectors(i) = rowVectors(i).clone
      i += 1
    }
    x
  }

  override
  def apply(row: Int, column: Int): Double = {
    if (rowVectors(row) == null) 0.0 else rowVectors(row)(column)
  }

  override
  def like(): Matrix = {
    SparseRowMatrix(rowSize, columnSize, randomAccessRows)
  }

  override
  def like(rows: Int, columns: Int): Matrix = {
    SparseRowMatrix(rows, columns, randomAccessRows)
  }

  override
  def update(row: Int, column: Int, value: Double) {
    rowVectors(row)(column) = value
  }

  override
  def getNumNondefaultElements: Array[Int] = {
    import AbstractMatrix._
    val result = new Array[Int](2)
    result(ROW) = rowVectors.length
    var row = 0
    while (row < rowSize) {
      result(COL) = math.max(result(COL), rowVectors(row).getNumNondefaultElements)
      row += 1
    }
    result
  }

  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
    import AbstractMatrix._
    if (offset(ROW) < 0) {
      throw new IndexException(offset(ROW), rowVectors.length)
    }
    if (offset(ROW) + size(ROW) > rowVectors.length) {
      throw new IndexException(offset(ROW) + size(ROW), rowVectors.length)
    }
    if (offset(COL) < 0) {
      throw new IndexException(offset(COL), rowVectors(ROW).size)
    }
    if (offset(COL) + size(COL) > rowVectors(ROW).size) {
      throw new IndexException(offset(COL) + size(COL), rowVectors(ROW).size)
    }
    MatrixView(this, offset, size)
  }

  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    if (rowSize != other.size) {
      throw new CardinalityException(rowSize, other.size)
    }
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    var row = 0
    while (row < rowSize) {
      rowVectors(row)(column) = other(row)
      row += 1
    }
    this
  }

  override
  def assignRow(row: Int, other: Vector): Matrix = {
    if (columnSize != other.size) {
      throw new CardinalityException(columnSize, other.size)
    }
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    rowVectors(row).assign(other)
    this
  }

  /**
   *
   * @param row an Int row index
   * @return a shallow view of the Vector at specified row (ie you may mutate the original matrix using this row)
   */
  override
  def viewRow(row: Int): Vector = {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    rowVectors(row)
  }

}

object SparseRowMatrix {
  def apply(rows: Int, columns: Int, rowVectors: Array[Vector], shallowCopy: Boolean, randomAccessRows: Boolean) = {
    val rowVectors1 = rowVectors.clone
    var row = 0
    while (row < rows) {
      if (rowVectors(row) == null) {
        // TODO: this can't be right to change the argument
        rowVectors(row) = if (randomAccessRows) RandomAccessSparseVector(columns, 10) else SequentialAccessSparseVector(columns, 10)
      }
      rowVectors1(row) = if (shallowCopy) rowVectors(row) else rowVectors(row).clone
      row += 1
    }
    new SparseRowMatrix(rows, columns, rowVectors1, randomAccessRows)
  }
  
  /**
   * Construct a sparse matrix starting with the provided row vectors.
   *
   * @param rows              The number of rows in the result
   * @param columns           The number of columns in the result
   * @param rowVectors        a Vector[] array of rows
   */
  def apply(rows: Int, columns: Int, rowVectors: Array[Vector]): SparseRowMatrix = {
    apply(rows, columns, rowVectors, false, rowVectors.isInstanceOf[Array[RandomAccessSparseVector]])
  }

  def apply(rows: Int, columns: Int, randomAccessRows: Boolean): SparseRowMatrix = {
    val rowVectors = if (randomAccessRows) new Array[RandomAccessSparseVector](rows) else new Array[SequentialAccessSparseVector](rows)
    // scala's Array is invariant, we have to let rowVectors.asInstanceOf[Array[Vector]], and it's safe here, since they all are Array[Object] in java
    apply(rows, columns, rowVectors.asInstanceOf[Array[Vector]], true, randomAccessRows)
  }
  
  /**
   * Construct a matrix of the given cardinality, with rows defaulting to RandomAccessSparseVector implementation
   *
   * @param rows
   * @param columns
   */
  def apply(rows: Int, columns: Int): SparseRowMatrix = {
    apply(rows, columns, true)
  }

}