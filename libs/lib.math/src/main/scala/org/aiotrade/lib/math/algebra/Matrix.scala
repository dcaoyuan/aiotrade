package org.aiotrade.lib.math.algebra


import org.aiotrade.lib.math.Precision

/** 
 * The basic interface including numerous convenience functions 
 * 
 *             m x n matrix
 * a(i,j)
 *          n columns --> j+
 *  m
 * rows [ a(0,0) a(0,1) a(0,2) ... |
 *      [ a(1,0) a(1,1) a(1,2) ... |
 *  |   [ a(2,0) a(2,1) a(2,2) ... |
 *  |   [   .      .      .     .  |
 *  V   [   .      .      .     .  |
 *      [   .      .      .     .  |
 *  i+
 *      
 */
@cloneable
trait Matrix extends VectorIterable with Cloneable {

  /** @return a formatted String suitable for output */
  def asFormatString: String

  /**
   * Assign the value to all elements of the receiver
   *
   * @param value a Double value
   * @return the modified receiver
   */
  def assign(value: Double): Matrix

  /**
   * Assign the values to the receiver
   *
   * @param values a Double[] of values
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(values: Array[Array[Double]]): Matrix

  /**
   * Assign the other vector values to the receiver
   *
   * @param other a Matrix
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(other: Matrix): Matrix

  /**
   * Apply the function to each element of the receiver
   *
   * @param function a Double => Double to apply
   * @return the modified receiver
   */
  def assign(function: Double => Double): Matrix

  /**
   * Apply the function to each element of the receiver and the corresponding element of the other argument
   *
   * @param other    a Matrix containing the second arguments to the function
   * @param function a (Double, Double) => Double to apply
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assign(other: Matrix, function: (Double, Double) => Double): Matrix

  /**
   * Assign the other vector values to the column of the receiver
   *
   * @param column the Int row to assign
   * @param other  a Vector
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assignColumn(column: Int, other: Vector): Matrix

  /**
   * Assign the other vector values to the row of the receiver
   *
   * @param row   the Int row to assign
   * @param other a Vector
   * @return the modified receiver
   * @throws CardinalityException if the cardinalities differ
   */
  def assignRow(row: Int, other: Vector): Matrix

  /**
   * Collects the results of a function applied to each row of a matrix.
   * @param f  The function to be applied to each row.
   * @return  The vector of results.
   */
  def aggregateRows(f: Vector => Double): Vector

  /**
   * Collects the results of a function applied to each column of a matrix.
   * @param f  The function to be applied to each column.
   * @return  The vector of results.
   */
  def aggregateColumns(f: Vector => Double): Vector

  /**
   * Collects the results of a function applied to each element of a matrix and then
   * aggregated.
   * @param combiner  A function that combines the results of the mapper.
   * @param mapper  A function to apply to each element.
   * @return  The result.
   */
  def aggregate(combiner: (Double, Double) => Double, mapper: Double => Double): Double

  /**
   * @return The number of rows in the matrix.
   */
  def columnSize: Int

  /**
   * @return Returns the number of rows in the matrix.
   */
  def rowSize: Int

  /**
   * Return a copy of the recipient
   *
   * @return a new Matrix
   */
  override 
  def clone: Matrix = {
    // Scala's compiler seems to complain that the clone method is the protected 
    // one from Object instead of this overrided one when it's called outside the
    // protected scope. For instance: 
    //   method clone in class Object cannot be accessed in ....
    //   Access to protected method clone not permitted because
    // To bypass it, we need to implement it with following statement
    super.clone.asInstanceOf[Matrix]
  }
  
  /**
   * Returns matrix determinator using Laplace theorem
   *
   * @return a matrix determinator
   */
  def determinant: Double

  /**
   * Return a new matrix containing the values of the recipient divided by the argument
   *
   * @param x a Double value
   * @return a new Matrix
   */
  def divide(x: Double): Matrix

  /**
   * Return the value at the given indexes
   *
   * @param row    an Int row index
   * @param column an Int column index
   * @return the Double at the index
   * @throws IndexException if the index is out of bounds
   */
  def get(row: Int, column: Int): Double

  /**
   * Return the value at the given indexes, without checking bounds
   *
   * @param row    an Int row index
   * @param column an Int column index
   * @return the Double at the index
   */
  def apply(row: Int, column: Int): Double

  /**
   * Return an empty matrix of the same underlying class as the receiver
   * 
   * @note this method should be like() instead of like, other wise a call as
   * m.like(rows, columns) may be compiled to m.like.apply(rows, columns)
   * 
   * @return a Matrix
   */
  def like(): Matrix

  /**
   * Returns an empty matrix of the same underlying class as the receiver and of the specified size.
   *
   * @param rows    the Int number of rows
   * @param columns the Int number of columns
   */
  def like(rows: Int, columns: Int): Matrix

  /**
   * Return a new matrix containing the element by element difference of the recipient and the argument
   *
   * @param x a Matrix
   * @return a new Matrix
   * @throws CardinalityException if the cardinalities differ
   */
  def minus(x: Matrix): Matrix

  /**
   * Return a new matrix containing the sum of each value of the recipient and the argument
   *
   * @param x a Double
   * @return a new Matrix
   */
  def plus(x: Double): Matrix

  /**
   * Return a new matrix containing the element by element sum of the recipient and the argument
   *
   * @param x a Matrix
   * @return a new Matrix
   * @throws CardinalityException if the cardinalities differ
   */
  def plus(x: Matrix): Matrix

  /**
   * Set the value at the given index
   *
   * @param row    an Int row index into the receiver
   * @param column an Int column index into the receiver
   * @param value  a Double value to set
   * @throws IndexException if the index is out of bounds
   */
  def set(row: Int, column: Int, value: Double)

  def set(row: Int, data: Array[Double])

  /**
   * Set the value at the given index, without checking bounds
   *
   * @param row    an Int row index into the receiver
   * @param column an Int column index into the receiver
   * @param value  a Double value to set
   */
  def update(row: Int, column: Int, value: Double)

  /**
   * Return the number of values in the recipient
   *
   * @return an Int[2] containing [row, column] count
   */
  def getNumNondefaultElements: Array[Int]

  /**
   * Return a new matrix containing the product of each value of the recipient and the argument
   *
   * @param x a Double argument
   * @return a new Matrix
   */
  def times(x: Double): Matrix

  /**
   * Return a new matrix containing the product of the recipient and the argument
   *
   * @param x a Matrix argument
   * @return a new Matrix
   * @throws CardinalityException if the cardinalities are incompatible
   */
  def times(x: Matrix): Matrix

  /**
   * Return a new matrix that is the transpose of the receiver
   *
   * @return the transpose
   */
  def transpose: Matrix

  /**
   * Return the sum of all the elements of the receiver
   *
   * @return a Double
   */
  def zSum: Double

  /**
   * Return a map of the current column label bindings of the receiver
   *
   * @return a Map<String, Integer>
   */
  def getColumnLabelBindings: collection.Map[String, Int]

  /**
   * Return a map of the current row label bindings of the receiver
   *
   * @return a Map<String, Integer>
   */
  def getRowLabelBindings: collection.Map[String, Int]

  /**
   * Sets a map of column label bindings in the receiver
   *
   * @param bindings a Map<String, Integer> of label bindings
   */
  def setColumnLabelBindings(bindings: collection.Map[String, Int])

  /**
   * Sets a map of row label bindings in the receiver
   *
   * @param bindings a Map<String, Integer> of label bindings
   */
  def setRowLabelBindings(bindings: collection.Map[String, Int])

  /**
   * Return the value at the given labels
   *
   * @param rowLabel    a String row label
   * @param columnLabel a String column label
   * @return the Double at the index
   * 
   * @throws IndexException if the index is out of bounds
   */
  def get(rowLabel: String, columnLabel: String): Double

  /**
   * Set the value at the given index
   *
   * @param rowLabel    a String row label
   * @param columnLabel a String column label
   * @param value       a Double value to set
   * @throws IndexException if the index is out of bounds
   */
  def set(rowLabel: String, columnLabel: String, value: Double)

  /**
   * Set the value at the given index, updating the row and column label bindings
   *
   * @param rowLabel    a String row label
   * @param columnLabel a String column label
   * @param row         an Int row index
   * @param column      an Int column index
   * @param value       a Double value
   */
  def set(rowLabel: String, columnLabel: String, row: Int, column: Int, value: Double)

  /**
   * Sets the row values at the given row label
   *
   * @param rowLabel a String row label
   * @param rowData  a Double[] array of row data
   */
  def set(rowLabel: String, rowData: Array[Double])

  /**
   * Sets the row values at the given row index and updates the row labels
   *
   * @param rowLabel the String row label
   * @param row      an Int the row index
   * @param rowData  a Double[] array of row data
   */
  def set(rowLabel: String, row: Int, rowData: Array[Double])

  /*
   * Need stories for these but keeping them here for now.
   * 
   */
  // void getNonZeros(IntArrayList jx, DoubleArrayList values)
  // void foreachNonZero(IntDoubleFunction f)
  // Double aggregate((Double, Double) => Double aggregator, Double => Double map)
  // Double aggregate(Matrix other, (Double, Double) => Double aggregator,
  // (Double, Double) => Double map)
  // NewMatrix assign(Matrix y, (Double, Double) => Double function, IntArrayList
  // nonZeroIndexes)

  /**
   * Return a view into part of a matrix.  Changes to the view will change the
   * original matrix.
   *
   * @param offset an Int[2] offset into the receiver
   * @param size   the Int[2] size of the desired result
   * @return a matrix that shares storage with part of the original matrix.
   * @throws CardinalityException if the length is greater than the cardinality of the receiver
   * @throws IndexException       if the offset is negative or the offset+length is outside of the receiver
   */
  def viewPart(offset: Array[Int], size: Array[Int]): Matrix

  /**
   * Return a view into part of a matrix.  Changes to the view will change the
   * original matrix.
   *
   * @param rowOffset           The first row of the view
   * @param rowsRequested       The number of rows in the view
   * @param columnOffset        The first column in the view
   * @param columnsRequested    The number of columns in the view
   * @return a matrix that shares storage with part of the original matrix.
   * @throws CardinalityException if the length is greater than the cardinality of the receiver
   * @throws IndexException       if the offset is negative or the offset+length is outside of the
   *                              receiver
   */
  def viewPart(rowOffset: Int, rowsRequested: Int, columnOffset: Int, columnsRequested: Int): Matrix

  /**
   * Return a reference to a row.  Changes to the view will change the original matrix.
   * @param row  The index of the row to return.
   * @return A vector that shares storage with the original.
   */
  def viewRow(row: Int): Vector

  /**
   * Return a reference to a column.  Changes to the view will change the original matrix.
   * @param column  The index of the column to return.
   * @return A vector that shares storage with the original.
   */
  def viewColumn(column: Int): Vector

  /**
   * Returns a reference to the diagonal of a matrix. Changes to the view will change
   * the original matrix.
   * @return A vector that shares storage with the original matrix.
   */
  def viewDiagonal: Vector
}

object Matrix {
  
  /**
   * Compute Maximum Absolute Row Sum Norm of input Matrix m
   * http://mathworld.wolfram.com/MaximumAbsoluteRowSumNorm.html 
   */
  def getNorm(m: Matrix): Double = {
    var max = 0.0;
    var i = 0
    while (i < m.numRows) {
      var sum = 0;
      val cv = m.viewRow(i)
      var j = 0
      while (j < cv.size) {
        sum += math.abs(cv(j)).toInt
        j += 1
      }
      if (sum > max) {
        max = sum
      }
      i += 1
    }
    max
  }
  
  def toArray(m: Matrix): Array[Array[Double]] = {
    checkSquare(m)
    val n = m.numCols
    val V = Array.ofDim[Double](n, n)
    for (slice <- m) {
      val row = slice.index
      for (element <- slice.vector) {
        V(row)(element.index) = element.get
      }
    }
    V
  }

  def isSymmetric(matrix: Array[Array[Double]]): Boolean = {
    var i = 0
    while (i < matrix.length) {
      var j = 0 
      while (j < i) {
        if (matrix(i)(j) != matrix(j)(i)) {
          return false
        }
        j += 1
      }
      i += 1
    }
    true
  }
  
  /**
   * Check if a matrix is symmetric.
   *
   * @param matrix Matrix to check.
   * @param raiseException If {@code true}, the method will throw an
   * exception if {@code matrix} is not symmetric.
   * @return {@code true} if {@code matrix} is symmetric.
   * @throws NonSymmetricMatrixException if the matrix is not symmetric and
   * {@code raiseException} is {@code true}.
   */
  def isSymmetric(matrix: Matrix): Boolean = {
    val rows = matrix.numRows
    val columns = matrix.numCols
    val eps = 10 * rows * columns * Precision.EPSILON
    var i = 0
    while (i < rows) {
      var j = i + 1
      while (j < columns) {
        val mij = matrix(i, j)
        val mji = matrix(j, i)
        if (math.abs(mij - mji) > (math.max(math.abs(mij), math.abs(mji)) * eps)) {
          return false
        }
        j += 1
      }
      i += 1
    }
    true
  }
  
  def isSquare(m: Matrix): Boolean = {
    m.numCols == m.numRows
  }
  
  def checkSquare(m: Matrix) = {
    if (!isSquare(m)) {
      throw new IllegalArgumentException("Matrix must be square")     
    }
  }
}
