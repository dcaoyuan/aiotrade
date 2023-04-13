package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException
import org.aiotrade.lib.math.IndexException

/** Matrix of doubles implemented using a 2-d array */
class DenseMatrix protected (private var values: Array[Array[Double]]) extends AbstractMatrix(values.length, values(0).length) {
  
  override 
  def clone: Matrix = {
    val x = super.clone.asInstanceOf[DenseMatrix]
    x.values = new Array[Array[Double]](values.length)
    var i = 0
    while (i < values.length) {
      x.values(i) = values(i).clone
      i += 1
    }
    x
  }
  
  override
  def apply(row: Int, column: Int): Double = {
    values(row)(column)
  }
  
  override
  def like(): Matrix = {
    like(rowSize, columnSize)
  }
  
  override
  def like(rows: Int, columns: Int): Matrix = {
    DenseMatrix(rows, columns)
  }
  
  override
  def update(row: Int, column: Int, value: Double) {
    values(row)(column) = value
  }

  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
    val rowOffset = offset(AbstractMatrix.ROW)
    val rowsRequested = size(AbstractMatrix.ROW)
    val columnOffset = offset(AbstractMatrix.COL)
    val columnsRequested = size(AbstractMatrix.COL)

    viewPart(rowOffset, rowsRequested, columnOffset, columnsRequested)
  }

  override
  def viewPart(rowOffset: Int, rowsRequested: Int, columnOffset: Int, columnsRequested: Int): Matrix = {
    if (rowOffset < 0) {
      throw new IndexException(rowOffset, rowSize)
    }
    if (rowOffset + rowsRequested > rowSize) {
      throw new IndexException(rowOffset + rowsRequested, rowSize)
    }
    if (columnOffset < 0) {
      throw new IndexException(columnOffset, columnSize)
    }
    if (columnOffset + columnsRequested > columnSize) {
      throw new IndexException(columnOffset + columnsRequested, columnSize)
    }
    MatrixView(this, Array(rowOffset, columnOffset), Array(rowsRequested, columnsRequested))
  }

  override
  def assign(value: Double): Matrix = {
    var row = 0
    while (row < rowSize) {
      java.util.Arrays.fill(values(row), value)
      row += 1
    }
    this
  }
  
  def assign(matrix: DenseMatrix): Matrix = {
    // make sure the data field has the correct length
    if (matrix.values(0).length != this.values(0).length || matrix.values.length != this.values.length) {
      this.values = Array.ofDim[Double](matrix.values.length, matrix.values(0).length)
    }
    // now copy the values
    var i = 0
    while (i < this.values.length) {
      System.arraycopy(matrix.values(i), 0, this.values(i), 0, this.values(0).length)
      i += 1
    }
    this
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
      values(row)(column) = other(row)
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
    var col = 0
    while (col < columnSize) {
      values(row)(col) = other(col)
      col += 1
    }
    this
  }
  
  override
  def viewRow(row: Int): Vector = {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    DenseVector(values(row), true)
  }
  
}

object DenseMatrix {
  def apply(values: Array[Array[Double]], shallowCopy: Boolean) = {
    if (shallowCopy) {
      new DenseMatrix(values)
    } else {
      val xs = new Array[Array[Double]](values.length)
      // be careful, need to clone the columns too
      var i = 0
      while (i < xs.length) {
        xs(i) = values(i).clone
        i += 1
      }
      new DenseMatrix(xs)
    }
  }

  def apply(values: Array[Array[Double]]) = {
    new DenseMatrix(values)
  }

  def apply(rows: Int, columns: Int) = {
    new DenseMatrix(Array.ofDim[Double](rows,  columns))
  }
}
