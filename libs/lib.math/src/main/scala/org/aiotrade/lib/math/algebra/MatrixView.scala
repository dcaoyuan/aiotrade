package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException

/** Implements subset view of a Matrix */
/**
 * Construct a view of the matrix with given offset and cardinality
 *
 * @param matrix      an underlying Matrix
 * @param offset      the Int[2] offset into the underlying matrix
 * @param size        the Int[2] size of the view
 */
class MatrixView private (private var matrix: Matrix, private var offset: Array[Int], _size: Array[Int]) extends AbstractMatrix(_size(AbstractMatrix.ROW), _size(AbstractMatrix.COL)) {
  {
    val rowOffset = offset(AbstractMatrix.ROW)
    if (rowOffset < 0) {
      throw new IndexException(rowOffset, rowSize)
    }

    val rowsRequested = _size(AbstractMatrix.ROW)
    if (rowOffset + rowsRequested > matrix.rowSize) {
      throw new IndexException(rowOffset + rowsRequested, matrix.rowSize)
    }

    val columnOffset = offset(AbstractMatrix.COL)
    if (columnOffset < 0) {
      throw new IndexException(columnOffset, columnSize)
    }

    val columnsRequested = _size(AbstractMatrix.COL)
    if (columnOffset + columnsRequested > matrix.columnSize) {
      throw new IndexException(columnOffset + columnsRequested, matrix.columnSize)
    }
  }

  override
  def clone: Matrix = {
    val x = super.clone.asInstanceOf[MatrixView]
    x.matrix = matrix.clone
    x.offset = offset.clone
    x
  }

  override
  def apply(row: Int, column: Int): Double = {
    import AbstractMatrix._
    matrix(offset(ROW) + row, offset(COL) + column)
  }

  override
  def like(): Matrix = {
    matrix.like(rowSize, columnSize)
  }

  override
  def like(rows: Int, columns: Int): Matrix = {
    matrix.like(rows, columns)
  }

  override
  def update(row: Int, column: Int, value: Double) {
    import AbstractMatrix._
    matrix(offset(ROW) + row, offset(COL) + column) = value
  }

  override
  def getNumNondefaultElements: Array[Int] = {
    Array(rowSize, columnSize)

  }

  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
    import AbstractMatrix._
    if (offset(ROW) < ROW) {
      throw new IndexException(offset(ROW), ROW)
    }
    if (offset(ROW) + size(ROW) > rowSize) {
      throw new IndexException(offset(ROW) + size(ROW), rowSize)
    }
    if (offset(COL) < ROW) {
      throw new IndexException(offset(COL), ROW)
    }
    if (offset(COL) + size(COL) > columnSize) {
      throw new IndexException(offset(COL) + size(COL), columnSize)
    }
    val origin = offset.clone
    origin(ROW) += offset(ROW)
    origin(COL) += offset(COL)
    new MatrixView(matrix, origin, size)
  }

  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    import AbstractMatrix._
    if (rowSize != other.size) {
      throw new CardinalityException(rowSize, other.size)
    }
    var row = 0
    while (row < rowSize) {
      matrix(row + offset(ROW), column + offset(COL)) = other(row)
      row += 1
    }
    this
  }

  override
  def assignRow(row: Int, other: Vector): Matrix = {
    import AbstractMatrix._
    if (columnSize != other.size) {
      throw new CardinalityException(columnSize, other.size)
    }
    var col = 0
    while (col < columnSize) {
      matrix(row + offset(ROW), col + offset(COL)) = other(col)
      col += 1
    }
    this
  }

  override
  def viewColumn(column: Int): Vector = {
    import AbstractMatrix._
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    VectorView(matrix.viewColumn(column + offset(COL)), offset(ROW), rowSize)
  }

  override
  def viewRow(row: Int): Vector = {
    import AbstractMatrix._
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    VectorView(matrix.viewRow(row + offset(ROW)), offset(COL), columnSize)
  }

}

object MatrixView {
  def apply(matrix: Matrix, offset: Array[Int], size: Array[Int]) = new MatrixView(matrix, offset, size)
}