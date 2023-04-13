package org.aiotrade.lib.math.algebra


trait VectorIterable extends Iterable[MatrixSlice] {

  def iterateAll: Iterator[MatrixSlice] 

  def numSlices: Int

  def numRows: Int

  def numCols: Int

  /**
   * Return a new vector with cardinality equal to getNumRows() of this matrix which is the matrix product of the
   * recipient and the argument
   *
   * @param v a vector with cardinality equal to getNumCols() of the recipient
   * @return a new vector (typically a DenseVector)
   * @throws CardinalityException if this.getNumRows() != v.size()
   */
  def times(v: Vector): Vector

  /**
   * Convenience method for producing this.transpose().times(this.times(v)), which can be implemented with only one pass
   * over the matrix, without making the transpose() call (which can be expensive if the matrix is sparse)
   *
   * @param v a vector with cardinality equal to getNumCols() of the recipient
   * @return a new vector (typically a DenseVector) with cardinality equal to that of the argument.
   * @throws CardinalityException if this.getNumCols() != v.size()
   */
  def timesSquared(v: Vector): Vector

}
