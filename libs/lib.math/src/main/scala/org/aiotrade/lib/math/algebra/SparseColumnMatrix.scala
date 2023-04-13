package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException

/**
 * sparse matrix with general element values whose columns are accessible quickly. Implemented as a column array of
 * SparseVectors.
 * Construct a matrix of the given cardinality with the given data columns
 *
 * @param rows
 * @param columns     a RandomAccessSparseVector[] array of columns
 * @param columnVectors
 */
class SparseColumnMatrix private (_rows: Int, _columns: Int, private var columnVectors: Array[RandomAccessSparseVector]) extends AbstractMatrix(_rows, _columns) {

  override
  def clone: Matrix = {
    val x = super.clone.asInstanceOf[SparseColumnMatrix]
    x.columnVectors = new Array[RandomAccessSparseVector](columnVectors.length)
    var i = 0
    while (i < columnVectors.length) {
      x.columnVectors(i) = columnVectors(i).clone
      i += 1
    }
    x
  }

  /**
   * Abstracted out for the iterator
   * @return {@link #numCols()} 
   */
  override
  def numSlices: Int = {
    numCols
  }

  override
  def apply(row: Int, column: Int): Double = {
    if (columnVectors(column) == null) 0.0 else columnVectors(column)(row)
  }

  override
  def like(): Matrix = {
    SparseColumnMatrix(rowSize, columnSize)
  }

  override
  def like(rows: Int, columns: Int): Matrix = {
    SparseColumnMatrix(rows, columns)
  }

  override
  def update(row: Int, column: Int, value: Double) {
    if (columnVectors(column) == null) {
      columnVectors(column) = RandomAccessSparseVector(rowSize)
    }
    columnVectors(column)(row) = value
  }

  override
  def getNumNondefaultElements: Array[Int] = {
    import AbstractMatrix._
    val result = new Array[Int](2)
    result(COL) = columnVectors.length;
    var col = 0
    while (col < columnSize) {
      result(ROW) = math.max(result(ROW), columnVectors(col).getNumNondefaultElements)
      col += 1
    }
    result
  }

  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
    import AbstractMatrix._
    if (offset(ROW) < 0) {
      throw new IndexException(offset(ROW), columnVectors(COL).size)
    }
    if (offset(ROW) + size(ROW) > columnVectors(COL).size) {
      throw new IndexException(offset(ROW) + size(ROW), columnVectors(COL).size)
    }
    if (offset(COL) < 0) {
      throw new IndexException(offset(COL), columnVectors.length)
    }
    if (offset(COL) + size(COL) > columnVectors.length) {
      throw new IndexException(offset(COL) + size(COL), columnVectors.length)
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
    columnVectors(column).assign(other)
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
    var col = 0
    while (col < columnSize) {
      columnVectors(col)(row) = other(col)
      col += 1
    }
    this
  }

  override
  def viewColumn(column: Int): Vector = {
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    columnVectors(column)
  }
}

object SparseColumnMatrix {
  def apply(rows: Int, columns: Int, columnVectors: Array[RandomAccessSparseVector]) = {
    val columnVectors1 = columnVectors.clone
    var col = 0
    while (col < columns) {
      columnVectors1(col) = columnVectors(col).clone
      col += 1
    }
    new SparseColumnMatrix(rows, columns, columnVectors)
  }
  
  /**
   * Construct a matrix of the given cardinality
   *
   * @param rows
   * @param columns
   */
  def apply(rows: Int, columns: Int) = {
    val columnVectors = new Array[RandomAccessSparseVector](columns)
    var col = 0
    while (col < columns) {
      columnVectors(col) =  RandomAccessSparseVector(rows)
      col += 1
    }
    new SparseColumnMatrix(rows, columns, columnVectors)
  }


}