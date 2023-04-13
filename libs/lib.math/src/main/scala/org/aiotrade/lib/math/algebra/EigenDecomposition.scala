package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException

import org.aiotrade.lib.math.Precision

/**
 * Calculates the eigen decomposition of a real <strong>symmetric</strong>
 * matrix.
 * <p>The eigen decomposition of matrix A is a set of two matrices:
 * V and D such that A = V &times; D &times; V<sup>T</sup>.
 * A, V and D are all m &times; m matrices.</p>
 * <p>This class is similar in spirit to the <code>EigenvalueDecomposition</code>
 * class from the <a href="http://math.nist.gov/javanumerics/jama/">JAMA</a>
 * library, with the following changes:</p>
 * <ul>
 *   <li>a {@link #getVT() getVt} method has been added,</li>
 *   <li>two {@link #getRealEigenvalue(int) getRealEigenvalue} and {@link #getImagEigenvalue(int)
 *   getImagEigenvalue} methods to pick up a single eigenvalue have been added,</li>
 *   <li>a {@link #getEigenvector(int) getEigenvector} method to pick up a single
 *   eigenvector has been added,</li>
 *   <li>a {@link #getDeterminant() getDeterminant} method has been added.</li>
 *   <li>a {@link #getSolver() getSolver} method has been added.</li>
 * </ul>
 * <p>
 * As of 2.0, this class supports only <strong>symmetric</strong> matrices, and
 * hence computes only real realEigenvalues. This implies the D matrix returned
 * by {@link #getD()} is always diagonal and the imaginary values returned
 * {@link #getImagEigenvalue(int)} and {@link #getImagEigenvalues()} are always
 * null.
 * </p>
 * <p>
 * When called with a {@link RealMatrix} argument, this implementation only uses
 * the upper part of the matrix, the part below the diagonal is not accessed at
 * all.
 * </p>
 * <p>
 * This implementation is based on the paper by A. Drubrulle, R.S. Martin and
 * J.H. Wilkinson "The Implicit QL Algorithm" in Wilksinson and Reinsch (1971)
 * Handbook for automatic computation, vol. 2, Linear algebra, Springer-Verlag,
 * New-York
 * </p>
 * @see <a href="http://mathworld.wolfram.com/EigenDecomposition.html">MathWorld</a>
 * @see <a href="http://en.wikipedia.org/wiki/Eigendecomposition_of_a_matrix">Wikipedia</a>
 * 
 * @param main        Main diagonal of the tridiagonal matrix
 * @param secondary   Secondary diagonal of the tridiagonal matrix
 * 
 * Ported by Caoyuan Deng from Java version at org.apache.commons.math3.linear
 */
class EigenDecomposition private (main: Array[Double], secondary: Array[Double], householder: Array[Array[Double]], splitTolerance: Double) {
  /** Maximum number of iterations accepted in the implicit QL transformation */
  private val maxIter = 30.toByte
  /** Real part of the realEigenvalues. */
  private var realEigenvalues: Array[Double] = _
  /** Imaginary part of the realEigenvalues. */
  private var imagEigenvalues: Array[Double] = _
  /** Eigenvectors. */
  private var eigenvectors: Array[Vector] = _
  /** Cached value of V. */
  private var cachedV: Matrix = _
  /** Cached value of D. */
  private var cachedD: Matrix = _
  /** Cached value of Vt. */
  private var cachedVt: Matrix = _

  /**
   * Gets the matrix V of the decomposition.
   * V is an orthogonal matrix, i.e. its transpose is also its inverse.
   * The columns of V are the eigenvectors of the original matrix.
   * No assumption is made about the orientation of the system axes formed
   * by the columns of V (e.g. in a 3-dimension space, V can form a left-
   * or right-handed system).
   *
   * @return the V matrix.
   */
  def getV: Matrix = {

    if (cachedV == null) {
      val m = eigenvectors.length
      cachedV = DenseMatrix(m, m)
      var k = 0
      while (k < m) {
        cachedV.assignColumn(k, eigenvectors(k))
        k += 1
      }
    }
    // return the cached matrix
    cachedV
  }

  /**
   * Gets the block diagonal matrix D of the decomposition.
   * D is a block diagonal matrix.
   * Real eigenvalues are on the diagonal while complex values are on
   * 2x2 blocks { {real +imaginary}, {-imaginary, real} }.
   *
   * @return the D matrix.
   *
   * @see #getRealEigenvalues()
   * @see #getImagEigenvalues()
   */
  def getD: Matrix = {
    if (cachedD == null) {
      // cache the matrix for subsequent calls
      cachedD = DiagonalMatrix(realEigenvalues)
    }
    cachedD
  }

  /**
   * Gets the transpose of the matrix V of the decomposition.
   * V is an orthogonal matrix, i.e. its transpose is also its inverse.
   * The columns of V are the eigenvectors of the original matrix.
   * No assumption is made about the orientation of the system axes formed
   * by the columns of V (e.g. in a 3-dimension space, V can form a left-
   * or right-handed system).
   *
   * @return the transpose of the V matrix.
   */
  def getVT: Matrix = {

    if (cachedVt == null) {
      val m = eigenvectors.length
      cachedVt = DenseMatrix(m, m)
      var k = 0
      while (k < m) {
        cachedVt.assignRow(k, eigenvectors(k))
        k += 1
      }

    }

    // return the cached matrix
    cachedVt
  }

  /**
   * Gets a copy of the real parts of the eigenvalues of the original matrix.
   *
   * @return a copy of the real parts of the eigenvalues of the original matrix.
   *
   * @see #getD()
   * @see #getRealEigenvalue(int)
   * @see #getImagEigenvalues()
   */
  def getRealEigenvalues: Array[Double] = {
    realEigenvalues.clone
  }

  /**
   * Returns the real part of the i<sup>th</sup> eigenvalue of the original
   * matrix.
   *
   * @param i index of the eigenvalue (counting from 0)
   * @return real part of the i<sup>th</sup> eigenvalue of the original
   * matrix.
   *
   * @see #getD()
   * @see #getRealEigenvalues()
   * @see #getImagEigenvalue(int)
   */
  def getRealEigenvalue(i: Int): Double = {
    realEigenvalues(i)
  }

  /**
   * Gets a copy of the imaginary parts of the eigenvalues of the original
   * matrix.
   *
   * @return a copy of the imaginary parts of the eigenvalues of the original
   * matrix.
   *
   * @see #getD()
   * @see #getImagEigenvalue(int)
   * @see #getRealEigenvalues()
   */
  def getImagEigenvalues: Array[Double] = {
    imagEigenvalues.clone
  }

  /**
   * Gets the imaginary part of the i<sup>th</sup> eigenvalue of the original
   * matrix.
   *
   * @param i Index of the eigenvalue (counting from 0).
   * @return the imaginary part of the i<sup>th</sup> eigenvalue of the original
   * matrix.
   *
   * @see #getD()
   * @see #getImagEigenvalues()
   * @see #getRealEigenvalue(int)
   */
  def getImagEigenvalue(i: Int): Double = {
    imagEigenvalues(i)
  }

  /**
   * Gets a copy of the i<sup>th</sup> eigenvector of the original matrix.
   *
   * @param i Index of the eigenvector (counting from 0).
   * @return a copy of the i<sup>th</sup> eigenvector of the original matrix.
   * @see #getD()
   */
  def getEigenvector(i: Int): Vector = {
    eigenvectors(i).clone
  }

  /**
   * Computes the determinant of the matrix.
   *
   * @return the determinant of the matrix.
   */
  def getDeterminant: Double = {
    var determinant = 1.0
    var i = 0
    while (i < realEigenvalues.length) {
      val lambda = realEigenvalues(i)
      determinant *= lambda
      i += 1
    }
    determinant
  }

  /**
   * Gets a solver for finding the A &times; X = B solution in exact
   * linear sense.
   *
   * @return a solver.
   */
  def getSolver: EigenDecomposition.Solver = {
    new EigenDecomposition.Solver(realEigenvalues, imagEigenvalues, eigenvectors)
  }



  /**
   * Find eigenvalues and eigenvectors (Dubrulle et al., 1971)
   *
   * @param householderMatrix Householder matrix of the transformation
   * to tridiagonal form.
   */
  private def findEigenVectors() {
    val z = householder.clone
    val n = main.length
    realEigenvalues = new Array[Double](n)
    imagEigenvalues = new Array[Double](n)
    val e = new Array[Double](n)
    var i = 0
    while (i < n - 1) {
      realEigenvalues(i) = main(i)
      e(i) = secondary(i)
      i += 1
    }
    realEigenvalues(n - 1) = main(n - 1)
    e(n - 1) = 0

    // Determine the largest main and secondary value in absolute term.
    var maxAbsoluteValue = 0.0
    i = 0
    while (i < n) {
      if (math.abs(realEigenvalues(i)) > maxAbsoluteValue) {
        maxAbsoluteValue = math.abs(realEigenvalues(i))
      }
      if (math.abs(e(i)) > maxAbsoluteValue) {
        maxAbsoluteValue = math.abs(e(i))
      }
      i += 1
    }
    // Make null any main and secondary value too small to be significant
    if (maxAbsoluteValue != 0) {
      var i = 0
      while (i < n) {
        if (math.abs(realEigenvalues(i)) <= Precision.EPSILON * maxAbsoluteValue) {
          realEigenvalues(i) = 0
        }
        if (math.abs(e(i)) <= Precision.EPSILON * maxAbsoluteValue) {
          e(i) = 0
        }
        i += 1
      }
    }

    var j = 0
    while (j < n) {
      var its = 0
      var m = 0
      do {
        m = j
        var continue = true
        while (m < n - 1 && continue) {
          val delta = math.abs(realEigenvalues(m)) + math.abs(realEigenvalues(m + 1))
          if (math.abs(e(m)) + delta == delta) {
            continue = false
          } else {
            m += 1
          }
        }
        if (m != j) {
          if (its == maxIter) {
            throw new IllegalStateException("convergence failed")
          }
          its += 1
          var q = (realEigenvalues(j + 1) - realEigenvalues(j)) / (2 * e(j))
          var t = math.sqrt(1 + q * q)
          if (q < 0.0) {
            q = realEigenvalues(m) - realEigenvalues(j) + e(j) / (q - t)
          } else {
            q = realEigenvalues(m) - realEigenvalues(j) + e(j) / (q + t)
          }
          var u = 0.0
          var s = 1.0
          var c = 1.0
          var i = m - 1
          var continue = true
          while (i >= j && continue) {
            var p = s * e(i)
            val h = c * e(i)
            if (math.abs(p) >= math.abs(q)) {
              c = q / p
              t = math.sqrt(c * c + 1.0)
              e(i + 1) = p * t
              s = 1.0 / t
              c = c * s
            } else {
              s = p / q
              t = math.sqrt(s * s + 1.0)
              e(i + 1) = q * t
              c = 1.0 / t
              s = s * c
            }
            if (e(i + 1) == 0.0) {
              realEigenvalues(i + 1) -= u
              e(m) = 0.0
              continue = false
            } else {
              q = realEigenvalues(i + 1) - u
              t = (realEigenvalues(i) - q) * s + 2.0 * c * h
              u = s * t
              realEigenvalues(i + 1) = q + u
              q = c * t - h
              var ia = 0
              while (ia < n) {
                p = z(ia)(i + 1)
                z(ia)(i + 1) = s * z(ia)(i) + c * p
                z(ia)(i) = c * z(ia)(i) - s * p
                ia += 1
              }
              i -= 1
            }
          }
          if (t == 0.0 && i >= j) {
            // continue
          } else {
            realEigenvalues(j) -= u
            e(j) = q
            e(m) = 0.0
          }
        }
      } while (m != j)
      
      j += 1
    }

    //Sort the eigen values (and vectors) in increase order
    i = 0
    while (i < n) {
      var k = i
      var p = realEigenvalues(i)
      var j = i + 1
      while (j < n) {
        if (realEigenvalues(j) > p) {
          k = j
          p = realEigenvalues(j)
        }
        j += 1
      }
      if (k != i) {
        realEigenvalues(k) = realEigenvalues(i)
        realEigenvalues(i) = p
        var j = 0
        while (j < n) {
          p = z(j)(i)
          z(j)(i) = z(j)(k)
          z(j)(k) = p
          j += 1
        }
      }
      i += 1
    }

    // Determine the largest eigen value in absolute term.
    maxAbsoluteValue = 0
    i = 0
    while (i < n) {
      if (math.abs(realEigenvalues(i)) > maxAbsoluteValue) {
        maxAbsoluteValue = math.abs(realEigenvalues(i))
      }
      i += 1
    }
    // Make null any eigen value too small to be significant
    if (maxAbsoluteValue!=0.0) {
      var i=0
      while (i < n) {
        if (math.abs(realEigenvalues(i)) < Precision.EPSILON * maxAbsoluteValue) {
          realEigenvalues(i) = 0
        }
        i += 1
      }
    }
    eigenvectors = new Array[Vector](n)
    val tmp = new Array[Double](n)
    i = 0
    while (i < n) {
      var j = 0
      while (j < n) {
        tmp(j) = z(j)(i)
        j += 1
      }
      eigenvectors(i) = DenseVector(tmp)
      i += 1
    }
  }
}

object EigenDecomposition {
  /**
   * Calculates the eigen decomposition of the given symmetric matrix.
   *
   * @param matrix Matrix to decompose. It <em>must</em> be symmetric.
   * @param splitTolerance Dummy parameter (present for backward
   * compatibility only).
   * @throws NonSymmetricMatrixException if the matrix is not symmetric.
   * @throws MaxCountExceededException if the algorithm fails to converge.
   */
  def apply(matrix: Matrix, splitTolerance: Double): EigenDecomposition =  {
    if (Matrix.isSymmetric(matrix)) {
      val transformer = new TriDiagonalTransformer(matrix)
      val main = transformer.getMainDiagonalRef
      val secondary = transformer.getSecondaryDiagonalRef
      val householder = Matrix.toArray(transformer.getQ).clone
      new EigenDecomposition(main, secondary, householder, splitTolerance)
      //findEigenVectors(householderMatrix)
    } else {
      throw new IllegalArgumentException("Matrix must be symmetric")
    }
  }

  /**
   * Calculates the eigen decomposition of the symmetric tridiagonal
   * matrix.  The Householder matrix is assumed to be the identity matrix.
   *
   * @param main Main diagonal of the symmetric tridiagonal form.
   * @param secondary Secondary of the tridiagonal form.
   * @param splitTolerance Dummy parameter (present for backward
   * compatibility only).
   * @throws MaxCountExceededException if the algorithm fails to converge.
   */
  def apply(main: Array[Double], secondary: Array[Double], splitTolerance: Double): EigenDecomposition = {
    val size = main.length
    val householder = Array.ofDim[Double](size, size)
    var i = 0
    while (i < size) {
      householder(i)(i) = 1.0
      i += 1
    }
    //findEigenVectors(z)
    new EigenDecomposition(main.clone, secondary.clone, householder, splitTolerance)
  }
  
  /** Specialized solver. */
  class Solver(realEigenvalues: Array[Double], imagEigenvalues: Array[Double], eigenvectors: Array[Vector]) {

    /**
     * Solves the linear equation A &times; X = B for symmetric matrices A.
     * <p>
     * This method only finds exact linear solutions, i.e. solutions for
     * which ||A &times; X - B|| is exactly 0.
     * </p>
     *
     * @param b Right-hand side of the equation A &times; X = B.
     * @return a Vector X that minimizes the two norm of A &times; X - B.
     *
     * @throws DimensionMismatchException if the matrices dimensions do not match.
     * @throws SingularMatrixException if the decomposed matrix is singular.
     */
    def solve(b: Vector): Vector = {
      if (!isNonSingular) {
        throw new IllegalArgumentException("Matrix must be singular")
      }

      val m = realEigenvalues.length
      if (b.size != m) {
        throw new CardinalityException(m, b.size)
      }

      val bp = new Array[Double](m)
      var i = 0
      while (i < m) {
        val v = eigenvectors(i)
        val s: Double = v.dot(b) / realEigenvalues(i)
        var j = 0
        while (j < m) {
          bp(j) += s * v(j)
          j += 1
        }
        i += 1
      }

      DenseVector(bp, false)
    }

    /** {@inheritDoc} */
    def solve(b: Matrix): Matrix = {
      if (!isNonSingular) {
        throw new IllegalArgumentException("Matrix must be singular")
      }

      val m = realEigenvalues.length
      if (b.numRows != m) {
        throw new CardinalityException(m, b.numRows)
      }

      val nColB = b.numCols
      val bp = Array.ofDim[Double](m, nColB)
      val tmpCol = Array.ofDim[Double](m)
      var k = 0
      while (k < nColB) {
        var i = 0
        while (i < m) {
          tmpCol(i) = b(i, k)
          bp(i)(k)  = 0
          i += 1
        }
        i = 0
        while (i < m) {
          val v = eigenvectors(i)
          var s = 0.0
          var j = 0
          while (j < m) {
            s += v(j) * tmpCol(j)
            j += 1
          }
          s /= realEigenvalues(i)
          j = 0
          while (j < m) {
            bp(j)(k) += s * v(j)
            j += 1
          }
          i += 1
        }
        k += 1
      }

      DenseMatrix(bp)
    }

    /**
     * Checks whether the decomposed matrix is non-singular.
     *
     * @return true if the decomposed matrix is non-singular.
     */
    def isNonSingular: Boolean = {
      var i = 0
      while (i < realEigenvalues.length) {
        if (realEigenvalues(i) == 0 && imagEigenvalues(i) == 0) {
          return false
        }
        i += 1
      }
      true
    }

    /**
     * Get the inverse of the decomposed matrix.
     *
     * @return the inverse matrix.
     * @throws SingularMatrixException if the decomposed matrix is singular.
     */
    def getInverse: Matrix = {
      if (!isNonSingular) {
        throw new IllegalArgumentException("Matrix must be singular")
      }

      val m = realEigenvalues.length
      val invData = Array.ofDim[Double](m, m)

      var i = 0
      while (i < m) {
        var invI = invData(i)
        var j = 0
        while (j < m) {
          var invIJ = 0.0
          var k = 0
          while (k < m) {
            val vK = eigenvectors(k)
            invIJ += vK(i) * vK(j) / realEigenvalues(k)
            k += 1
          }
          invI(j) = invIJ
          j += 1
        }
        i += 1
      }
      DenseMatrix(invData)
    }
  }
}