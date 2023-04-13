package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.IndexException

/**
 * Matrix that allows transparent row and column permutation.
 */
class PivotedMatrix private (val base: Matrix, val rowPivot: Array[Int], val columnPivot: Array[Int]) extends AbstractMatrix(base.rowSize, base.columnSize) {
  import PivotedMatrix._
  
  val inverseRowPivot = invert(rowPivot)
  val inverseColumnPivot = invert(columnPivot)

  /**
   * Swaps indexes i and j.  This does both row and column permutation.
   *
   * @param i First index to swap.
   * @param j Second index to swap.
   */
  def swap(i: Int, j: Int) {
    swapRows(i, j)
    swapColumns(i, j)
  }

  /**
   * Swaps indexes i and j.  This does just row permutation.
   *
   * @param i First index to swap.
   * @param j Second index to swap.
   */
  def swapRows(i: Int, j: Int) {
    swap(rowPivot, inverseRowPivot, i, j)
  }


  /**
   * Swaps indexes i and j.  This does just row permutation.
   *
   * @param i First index to swap.
   * @param j Second index to swap.
   */
  def swapColumns(i: Int, j: Int) {
    swap(columnPivot, inverseColumnPivot, i, j)
  }

  private def swap(pivot: Array[Int], unpivot: Array[Int], i: Int, j: Int) {
    if (i < 0 || i > pivot.length) {
      throw new IndexException(i, pivot.length)
    }
    if (j < 0 || j > pivot.length) {
      throw new IndexException(j, pivot.length)
    }
    
    if (i != j) {
      val tmp = pivot(i)
      pivot(i) = pivot(j)
      pivot(j) = tmp

      unpivot(pivot(i)) = i
      unpivot(pivot(j)) = j
    }
  }

  /**
   * Assign the other vector values to the column of the receiver
   *
   * @param column the Int row to assign
   * @param other  a Vector
   * @return the modified receiver
   * @throws org.apache.mahout.math.CardinalityException
   *          if the cardinalities differ
   */
  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    // note the reversed pivoting for other
    base.assignColumn(columnPivot(column), PermutedVectorView(other, inverseRowPivot, rowPivot))
  }

  /**
   * Assign the other vector values to the row of the receiver
   *
   * @param row   the Int row to assign
   * @param other a Vector
   * @return the modified receiver
   * @throws org.apache.mahout.math.CardinalityException
   *          if the cardinalities differ
   */
  override
  def assignRow(row: Int, other: Vector): Matrix = {
    // note the reversed pivoting for other
    base.assignRow(rowPivot(row), PermutedVectorView(other, inverseColumnPivot, columnPivot))
  }

  /**
   * Return the column at the given index
   *
   * @param column an Int column index
   * @return a Vector at the index
   * @throws org.apache.mahout.math.IndexException
   *          if the index is out of bounds
   */
  override
  def viewColumn(column: Int): Vector = {
    if (column < 0 || column >= columnSize) {
      throw new IndexException(column, columnSize)
    }
    PermutedVectorView(base.viewColumn(columnPivot(column)), rowPivot, inverseRowPivot)
  }

  /**
   * Return the row at the given index
   *
   * @param row an Int row index
   * @return a Vector at the index
   * @throws org.apache.mahout.math.IndexException
   *          if the index is out of bounds
   */
  override
  def viewRow(row: Int): Vector = {
    if (row < 0 || row >= rowSize) {
      throw new IndexException(row, rowSize)
    }
    PermutedVectorView(base.viewRow(rowPivot(row)), columnPivot, inverseColumnPivot)
  }

  /**
   * Return the value at the given indexes, without checking bounds
   *
   * @param row    an Int row index
   * @param column an Int column index
   * @return the Double at the index
   */
  override
  def apply(row: Int, column: Int): Double = {
     base(rowPivot(row), columnPivot(column))
  }

  /**
   * Return an empty matrix of the same underlying class as the receiver
   *
   * @return a Matrix
   */
  override
  def like: Matrix = {
    PivotedMatrix(base.like)
  }

  /**
   * Returns an empty matrix of the same underlying class as the receiver and of the specified
   * size.
   *
   * @param rows    the Int number of rows
   * @param columns the Int number of columns
   */
  override
  def like(rows: Int, columns: Int): Matrix = {
    PivotedMatrix(base.like(rows, columns))
  }

  /**
   * Set the value at the given index, without checking bounds
   *
   * @param row    an Int row index into the receiver
   * @param column an Int column index into the receiver
   * @param value  a Double value to set
   */
  override
  def update(row: Int, column: Int, value: Double) {
    base(rowPivot(row), columnPivot(column)) = value
  }

  /**
   * Return the number of values in the recipient
   *
   * @return an Int[2] containing [row, column] count
   */
  override
  def getNumNondefaultElements: Array[Int] = {
    base.getNumNondefaultElements
  }

  /**
   * Return a new matrix containing the subset of the recipient
   *
   * @param offset an Int[2] offset into the receiver
   * @param size   the Int[2] size of the desired result
   * @return a new Matrix that is a view of the original
   * @throws org.apache.mahout.math.CardinalityException
   *          if the length is greater than the cardinality of the receiver
   * @throws org.apache.mahout.math.IndexException
   *          if the offset is negative or the offset+length is outside of the receiver
   */
  override
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
     MatrixView(this, offset, size)
  }
}

object PivotedMatrix {
  def apply(base: Matrix, rowPivot: Array[Int], columnPivot: Array[Int]) = {
    new PivotedMatrix(base, rowPivot, columnPivot)
  }
  
  def apply(base: Matrix, pivot: Array[Int]) = {
    new PivotedMatrix(base, pivot, java.util.Arrays.copyOf(pivot, pivot.length))
  }

  def apply(base: Matrix) = {
    new PivotedMatrix(base, identityPivot(base.rowSize), identityPivot(base.columnSize))
  }

  private def identityPivot(n: Int): Array[Int] = {
    val pivot = new Array[Int](n)
    var i = 0
    while (i < n) {
      pivot(i) = i
      i += 1
    }
    pivot
  }

  private def invert(pivot: Array[Int]): Array[Int] = {
    val x = new Array[Int](pivot.length)
    var i = 0
    while (i < pivot.length) {
      x(pivot(i)) = i
      i += 1
    }
    x
  }

}