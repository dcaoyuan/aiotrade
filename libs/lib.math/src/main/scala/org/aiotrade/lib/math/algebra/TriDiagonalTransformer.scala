package org.aiotrade.lib.math.algebra


/**
 * Class transforming a symmetrical matrix to tridiagonal shape.
 * <p>A symmetrical m &times; m matrix A can be written as the product of three matrices:
 * A = Q &times; T &times; Q<sup>T</sup> with Q an orthogonal matrix and T a symmetrical
 * tridiagonal matrix. Both Q and T are m &times; m matrices.</p>
 * <p>This implementation only uses the upper part of the matrix, the part below the
 * diagonal is not accessed at all.</p>
 * <p>Transformation to tridiagonal shape is often not a goal by itself, but it is
 * an intermediate step in more general decomposition algorithms like {@link
 * EigenDecomposition eigen decomposition}. This class is therefore intended for internal
 * use by the library and is not public. As a consequence of this explicitly limited scope,
 * many methods directly returns references to internal arrays, not copies.</p>
 * Build the transformation to tridiagonal shape of a symmetrical matrix.
 * <p>The specified matrix is assumed to be symmetrical without any check.
 * Only the upper triangular part of the matrix is used.</p>
 *
 * @param matrix Symmetrical matrix to transform.
 * @throws NonSquareMatrixException if the matrix is not square.
 */
protected[algebra] class TriDiagonalTransformer(matrix: Matrix) {
  Matrix.checkSquare(matrix)
  /** Householder vectors. */
  private val householderVectors = Matrix.toArray(matrix)
  
  private val m = matrix.numRows

  /** Main diagonal. */
  private val main = new Array[Double](m)
  /** Secondary diagonal. */
  private val secondary = new Array[Double](m - 1)
  /** Cached value of Q. */
  private var cachedQ: Matrix = _
  /** Cached value of Qt. */
  private var cachedQt: Matrix = _
  /** Cached value of T. */
  private var cachedT: Matrix = _
  
  transform()

  /**
   * Returns the matrix Q of the transform.
   * <p>Q is an orthogonal matrix, i.e. its transpose is also its inverse.</p>
   * @return the Q matrix
   */
  def getQ: Matrix = {
    if (cachedQ == null) {
      cachedQ = getQT.transpose
    }
    cachedQ
  }

  /**
   * Returns the transpose of the matrix Q of the transform.
   * <p>Q is an orthogonal matrix, i.e. its transpose is also its inverse.</p>
   * @return the Q matrix
   */
  def getQT: Matrix = {
    if (cachedQt == null) {
      val m = householderVectors.length
      val qta = Array.ofDim[Double](m, m)

      // build up first part of the matrix by applying Householder transforms
      var k = m - 1
      while (k >= 1) {
        val hK = householderVectors(k - 1)
        qta(k)(k) = 1
        if (hK(k) != 0.0) {
          val inv = 1.0 / (secondary(k - 1) * hK(k))
          var beta = 1.0 / secondary(k - 1)
          qta(k)(k) = 1 + beta * hK(k)
          var i = k + 1
          while (i < m) {
            qta(k)(i) = beta * hK(i)
            i += 1
          }
          var j = k + 1
          while (j < m) {
            beta = 0;
            var i = k + 1
            while (i < m) {
              beta += qta(j)(i) * hK(i)
              i += 1
            }
            beta *= inv
            qta(j)(k) = beta * hK(k)
            i = k + 1
            while (i < m) {
              qta(j)(i) += beta * hK(i)
              i += 1
            }
            j += 1
          }
        }
        k -= 1
      }
      qta(0)(0) = 1
      cachedQt = DenseMatrix(qta)
    }

    // return the cached matrix
    cachedQt
  }

  /**
   * Returns the tridiagonal matrix T of the transform.
   * @return the T matrix
   */
  def getT: Matrix = {
    if (cachedT == null) {
      val m = main.length;
      val ta = Array.ofDim[Double](m, m)
      var i = 0
      while (i < m) {
        ta(i)(i) = main(i)
        if (i > 0) {
          ta(i)(i - 1) = secondary(i - 1)
        }
        if (i < main.length - 1) {
          ta(i)(i + 1) = secondary(i)
        }
        i += 1
      }
      cachedT = DenseMatrix(ta)
    }

    // return the cached matrix
    cachedT
  }

  /**
   * Get the Householder vectors of the transform.
   * <p>Note that since this class is only intended for internal use,
   * it returns directly a reference to its internal arrays, not a copy.</p>
   * @return the main diagonal elements of the B matrix
   */
  protected[algebra] def getHouseholderVectorsRef = householderVectors

  /**
   * Get the main diagonal elements of the matrix T of the transform.
   * <p>Note that since this class is only intended for internal use,
   * it returns directly a reference to its internal arrays, not a copy.</p>
   * @return the main diagonal elements of the T matrix
   */
  protected[algebra] def getMainDiagonalRef = main

  /**
   * Get the secondary diagonal elements of the matrix T of the transform.
   * <p>Note that since this class is only intended for internal use,
   * it returns directly a reference to its internal arrays, not a copy.</p>
   * @return the secondary diagonal elements of the T matrix
   */
  protected[algebra] def getSecondaryDiagonalRef = secondary

  /**
   * Transform original matrix to tridiagonal form.
   * <p>Transformation is done using Householder transforms.</p>
   */
  private def transform() {
    val m = householderVectors.length
    val z = new Array[Double](m)
    var k = 0
    while (k < m - 1) {

      //zero-out a row and a column simultaneously
      val hK = householderVectors(k)
      main(k) = hK(k)
      var xNormSqr = 0.0
      var j = k + 1
      while (j < m) {
        val c = hK(j)
        xNormSqr += c * c
        j += 1
      }
      val a = if ((hK(k + 1) > 0)) -math.sqrt(xNormSqr) else math.sqrt(xNormSqr)
      secondary(k) = a
      if (a != 0.0) {
        // apply Householder transform from left and right simultaneously

        hK(k + 1) -= a
        val beta = -1.0 / (a * hK(k + 1))

        // compute a = beta A v, where v is the Householder vector
        // this loop is written in such a way
        //   1) only the upper triangular part of the matrix is accessed
        //   2) access is cache-friendly for a matrix stored in rows
        java.util.Arrays.fill(z, k + 1, m, 0)
        var i = k + 1
        while (i < m) {
          val hI = householderVectors(i)
          val hKI = hK(i)
          var zI = hI(i) * hKI;
          var j = i + 1
          while (j < m) {
            val hIJ = hI(j)
            zI   += hIJ * hK(j)
            z(j) += hIJ * hKI
            j += 1
          }
          z(i) = beta * (z(i) + zI)
          i += 1
        }

        // compute gamma = beta vT z / 2
        var gamma = 0.0
        i = k + 1
        while (i < m) {
          gamma += z(i) * hK(i)
          i += 1
        }
        gamma *= beta / 2

        // compute z = z - gamma v
        i = k + 1
        while (i < m) {
          z(i) -= gamma * hK(i)
          i += 1
        }

        // update matrix: A = A - v zT - z vT
        // only the upper triangular part of the matrix is updated
        i = k + 1
        while (i < m) {
          val hI = householderVectors(i)
          var j = i
          while (j < m) {
            hI(j) -= hK(i) * z(j) + z(i) * hK(j)
            j += 1
          }
          i += 1
        }
      }
      
      k += 1
    }
    main(m - 1) = householderVectors(m - 1)(m - 1)
  }
}

