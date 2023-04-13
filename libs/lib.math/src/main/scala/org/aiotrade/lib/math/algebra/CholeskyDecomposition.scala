package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.Functions

/**
 * Cholesky decomposition shamelessly ported from JAMA.
 * <p/>
 * A Cholesky decomposition of a semi-positive definite matrix A is a lower triangular matrix L such
 * that L L^* = A.  If A is full rank, L is unique.  If A is real, then it must be symmetric and R
 * will also be real.
 */
class CholeskyDecomposition private (private val L: PivotedMatrix, val isPositiveDefinite: Boolean) {

  def getL: Matrix = {
    L.base
  }

  def getPermutedL: PivotedMatrix = {
    L
  }

  /**
   * @return Returns the permutation of rows and columns that was applied to L
   */
  def getPivot: Array[Int] = {
    L.rowPivot
  }

  def getInversePivot: Array[Int] = {
    L.inverseRowPivot
  }

  /**
   * Compute inv(L) * z efficiently.
   *
   * @param z
   */
  def solveLeft(z: Matrix): Matrix = {
    val n = L.columnSize
    val nx = z.columnSize

    val x = DenseMatrix(n, z.columnSize)
    x.assign(z)

    // Solve L*Y = Z using back-substitution
    // note that k and i have to go in a funny order because L is pivoted
    var internalK = 0
    while (internalK < n) {
      val k = L.inverseRowPivot(internalK)
      var j = 0
      while (j < nx) {
        var internalI = 0
        while (internalI < internalK) {
          val i = L.inverseRowPivot(internalI)
          x.set(k, j, x.get(k, j) - x.get(i, j) * L.get(k, i))
          internalI += 1
        }
        if (L.get(k, k) != 0) {
          x.set(k, j, x.get(k, j) / L.get(k, k))
        } else {
          x.set(k, j, 0)
        }
        j += 1
      }
      internalK += 1
    }
    x
  }

  /**
   * Compute z * inv(L') efficiently
   */
  def solveRight(z: Matrix): Matrix = {
    val n = z.columnSize
    val nx = z.rowSize

    val x = DenseMatrix(z.rowSize, z.columnSize)
    x.assign(z)

    // Solve Y*L' = Z using back-substitution
    var internalK = 0
    while (internalK < n) {
      val k = L.inverseRowPivot(internalK)
      var j = 0
      while (j < nx) {
        var internalI = 0
        while (internalI < k) {
          val i = L.inverseRowPivot(internalI)
          x.set(j, k, x.get(j, k) - x.get(j, i) * L.get(k, i))
          if (java.lang.Double.isInfinite(x.get(j, k)) || java.lang.Double.isNaN(x.get(j, k))) {
            throw new IllegalStateException("Invalid value found at" + j + ", " + k + " (should not be possible)")
          }
          internalI += 1
        }
        if (L.get(k, k) != 0) {
          x.set(j, k, x.get(j, k) / L.get(k, k))
        } else {
          x.set(j, k, 0)
        }
        if (java.lang.Double.isInfinite(x.get(j, k)) || java.lang.Double.isNaN(x.get(j, k))) {
          throw new IllegalStateException("Invalid value found at" + j + ", " + k + " (should not be possible)")
        }
        j += 1
      }
      internalK += 1
    }
    x
  }

}

object CholeskyDecomposition {  
  def apply(a: Matrix): CholeskyDecomposition = apply(a, true)
  def apply(a: Matrix, pivot: Boolean): CholeskyDecomposition = {
    val rows = a.rowSize
    // must be square
    if (rows != a.columnSize) {
      throw new IllegalArgumentException()
    }
    
    val L = PivotedMatrix(DenseMatrix(rows, rows))
    if (pivot) {
      decomposeWithPivoting(L, a)
    } else {
      decompose(L, a)
    }
  }
  
  private def decomposeWithPivoting(L: PivotedMatrix, a: Matrix): CholeskyDecomposition = {
    var isPositiveDefinite = true
    L.assign(a)

    // pivoted column-wise submatrix cholesky with simple pivoting
    var uberMax = L.viewDiagonal.aggregate(Functions.MAX, Functions.ABS)
    val n = a.rowSize
    var k = 0
    while (k < n) {
      var max = 0.0
      var pivot = k
      var j = k
      while (j < n) {
        if (L.get(j, j) > max) {
          max = L.get(j, j)
          pivot = j
          if (uberMax < math.abs(max)) {
            uberMax = math.abs(max)
          }
        }
        j += 1
      }
      L.swap(k, pivot)

      var akk = L.get(k, k)
      val epsilon = 1.0e-10 * math.max(uberMax, L.viewColumn(k).aggregate(Functions.MAX, Functions.ABS))

      if (akk < -epsilon) {
        // can't have decidedly negative element on diagonal
        throw new IllegalArgumentException("Matrix is not positive semi-definite")
      } else if (akk <= epsilon) {
        // degenerate column case.  Set all to zero
        L.viewColumn(k).assign(0)
        isPositiveDefinite = false

        // no need to subtract from remaining sub-matrix
      } else {
        // normalize column by diagonal element
        akk = math.sqrt(math.max(0, akk))
        L.viewColumn(k).viewPart(k, n - k).assign(Functions.div(akk))
        L.viewColumn(k).viewPart(0, k).assign(0)

        // subtract off scaled version of this column to the right
        var j = k + 1
        while (j < n) {
          val columnJ = L.viewColumn(j).viewPart(k, n - k)
          val columnK = L.viewColumn(k).viewPart(k, n - k)
          columnJ.assign(columnK, Functions.minusMult(columnK.get(j - k)))
          j += 1
        }

      }
      k += 1
    }
    
    new CholeskyDecomposition(L, isPositiveDefinite)
  }

  private def decompose(L: PivotedMatrix, a: Matrix): CholeskyDecomposition = {
    var isPositiveDefinite = true
    L.assign(a)

    val n = a.rowSize
    // column-wise submatrix cholesky with simple pivoting
    var k = 0
    while (k < n) {
      var akk = L.get(k, k)

      // set upper part of column to 0.
      L.viewColumn(k).viewPart(0, k).assign(0)

      val epsilon = 1.0e-10 * L.viewColumn(k).aggregate(Functions.MAX, Functions.ABS)
      if (akk <= epsilon) {
        // degenerate column case.  Set diagonal to 1, all others to zero
        L.viewColumn(k).viewPart(k, n - k).assign(0)

        isPositiveDefinite = false

        // no need to subtract from remaining sub-matrix
      } else {
        // normalize column by diagonal element
        akk = math.sqrt(math.max(0, akk))
        L.set(k, k, akk)
        L.viewColumn(k).viewPart(k + 1, n - k - 1).assign(Functions.div(akk))

        // now subtract scaled version of column
        var j = k + 1
        while (j < n) {
          val columnJ = L.viewColumn(j).viewPart(j, n - j)
          val columnK = L.viewColumn(k).viewPart(j, n - j)
          columnJ.assign(columnK, Functions.minusMult(L.get(j, k)))
          j += 1
        }
      }
      k += 1
    }
    
    new CholeskyDecomposition(L, isPositiveDefinite)
  }
}