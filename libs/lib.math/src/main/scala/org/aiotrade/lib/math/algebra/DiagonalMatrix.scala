package org.aiotrade.lib.math.algebra

class DiagonalMatrix private (diagonal: Vector) extends AbstractMatrix(diagonal.size, diagonal.size) {


  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    throw new UnsupportedOperationException("Can't assign a column to a diagonal matrix")
  }

  /**
   * Assign the other vector values to the row of the receiver
   *
   * @param row   the Int row to assign
   * @param other a Vector
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  override
  def assignRow(row: Int, other: Vector): Matrix = {
    throw new UnsupportedOperationException("Can't assign a row to a diagonal matrix")
  }

  /**
   * Provides a view of the diagonal of a matrix.
   */
  override
  def viewDiagonal: Vector = {
    this.diagonal;
  }

  /**
   * Return the value at the given location, without checking bounds
   *
   * @param row    an Int row index
   * @param column an Int column index
   * @return the Double at the index
   */
  override
  def apply(row: Int, column: Int): Double = {
    if (row == column) {
      diagonal.get(row)
    } else {
      0
    }
  }

  /**
   * Return an empty matrix of the same underlying class as the receiver
   *
   * @return a Matrix
   */
  override
  def like(): Matrix = {
    SparseRowMatrix(rowSize, columnSize)
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
    SparseRowMatrix(rows, columns)
  }

  override
  def update(row: Int, column: Int, value: Double) {
    if (row == column) {
      diagonal.set(row, value)
    } else {
      throw new UnsupportedOperationException("Can't set off-diagonal element")
    }
  }

  /**
   * Return the number of values in the recipient
   *
   * @return an Int[2] containing [row, column] count
   */
  override
  def getNumNondefaultElements: Array[Int] = {
    throw new UnsupportedOperationException("Don't understand how to implement this")
  }

  /**
   * Return a new matrix containing the subset of the recipient
   *
   * @param offset an Int[2] offset into the receiver
   * @param size   the Int[2] size of the desired result
   * @return a new Matrix that is a view of the original
   * @throws CardinalityException if the length is greater than the cardinality of the receiver
   * @throws IndexException       if the offset is negative or the offset+length is outside of the
   *                              receiver
   */
   override
   def viewPart(offset: Array[Int], size: Array[Int]): Matrix = {
      MatrixView(this, offset, size)
    }
   }

   object DiagonalMatrix {
      def apply(diagonal: Vector) = new DiagonalMatrix(diagonal)
      def apply(values: Matrix) = new DiagonalMatrix(values.viewDiagonal)
      def apply(value: Double, size: Int) = new DiagonalMatrix(ConstantVector(value, size))
      def apply(values: Array[Double]) = new DiagonalMatrix(DenseVector(values))
  
      def identity(size: Int): DiagonalMatrix = DiagonalMatrix(1, size)
    }
