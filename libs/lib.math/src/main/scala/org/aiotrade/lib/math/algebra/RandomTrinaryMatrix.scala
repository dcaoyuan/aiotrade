package org.aiotrade.lib.math.algebra

import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger

/**
 * Random matrix.  Each value is taken from {-1,0,1} with roughly equal probability.  Note
 * that by default, the value is determined by a relatively simple hash of the coordinates.
 * Such a hash is not usable where real randomness is required, but suffices nicely for
 * random projection methods.
 *
 * If the simple hash method is not satisfactory, an optional high quality mode is available
 * which uses a murmur hash of the coordinates.
 */
class RandomTrinaryMatrix private (seed: Int, _rows: Int, _columns: Int, highQuality: Boolean) extends AbstractMatrix(_rows, _columns) {

  override
  def assignColumn(column: Int, other: Vector): Matrix = {
    throw new UnsupportedOperationException("Can't assign to read-only matrix")
  }

  override
  def assignRow(row: Int, other: Vector): Matrix = {
    throw new UnsupportedOperationException("Can't assign to read-only matrix")
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
    import RandomTrinaryMatrix._

    if (highQuality) {
      val buf = ByteBuffer.allocate(8)
      buf.putInt(row)
      buf.putInt(column)
      buf.flip
      (MurmurHash.hash64A(buf, seed) & (SCALE - 1)) / SCALE.toDouble
    } else {
      // this isn't a fantastic random number generator, but it is just fine for random projections
      ((((row * PRIME1) + column * PRIME2 + row * column * PRIME3) & 8) * 0.25) - 1
    }
  }


  /**
   * Return an empty matrix of the same underlying class as the receiver
   *
   * @return a Matrix
   */
  override
  def like(): Matrix = {
    DenseMatrix(rowSize, columnSize)
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
    DenseMatrix(rows, columns)
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
    throw new UnsupportedOperationException("Can't assign to read-only matrix")
  }

  /**
   * Return the number of values in the recipient
   *
   * @return an Int[2] containing [row, column] count
   */
  override
  def getNumNondefaultElements: Array[Int] = {
    throw new UnsupportedOperationException("Can't assign to read-only matrix")
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

object RandomTrinaryMatrix {
  def apply(seed: Int, rows: Int, columns: Int, highQuality: Boolean) = new RandomTrinaryMatrix(seed, rows, columns, highQuality)
  def apply(rows: Int, columns: Int) = new RandomTrinaryMatrix(id.incrementAndGet, rows, columns, false)
  
  private val id = new AtomicInteger()
  private val PRIME1 = 104047
  private val PRIME2 = 101377
  private val PRIME3 = 64661
  private val SCALE = 1L << 32
}