package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.Functions
import java.util.Locale


/**
 For an <tt>m x n</tt> matrix <tt>A</tt> with <tt>m >= n</tt>, the QR decomposition is an <tt>m x n</tt>
 orthogonal matrix <tt>Q</tt> and an <tt>n x n</tt> upper triangular matrix <tt>R</tt> so that
 <tt>A = Q*R</tt>.
 <P>
 The QR decompostion always exists, even if the matrix does not have
 full rank, so the constructor will never fail.  The primary use of the
 QR decomposition is in the least squares solution of nonsquare systems
 of simultaneous linear equations.  This will fail if <tt>isFullRank()</tt>
 returns <tt>false</tt>.
 */

/** partially deprecated until unit tests are in place.  Until this time, this class/interface is unsupported. */
class QRDecomposition(a: Matrix) {

  /** Array for internal storage of decomposition. */
  private val qr: Matrix = a.clone

  /** Row and column dimensions. */
  private val originalRows = a.numRows
  private val originalColumns = a.numCols

  /** Array for internal storage of diagonal of R. */
  private val rDiag: Vector = DenseVector(originalColumns)

  precompute(a)
  /**
   * Constructs and returns a new QR decomposition object;  computed by Householder reflections; The decomposed matrices
   * can be retrieved via instance methods of the returned decomposition object.
   *
   * @param a A rectangular matrix.
   * @throws IllegalArgumentException if <tt>A.rows() < A.columns()</tt>.
   */
  def precompute(a: Matrix) {
    // precompute and cache some views to avoid regenerating them time and again
    val QRcolumnsPart = new Array[Vector](originalColumns)
    var k = 0
    while (k < originalColumns) {
      QRcolumnsPart(k) = qr.viewColumn(k).viewPart(k, originalRows - k)
      k += 1
    }

    // Main loop.
    k = 0
    while (k < originalColumns) {
      //DoubleMatrix1D QRcolk = QR.viewColumn(k).viewPart(k,m-k)
      // Compute 2-norm of k-th column without under/overflow.
      var nrm = 0.0
      //if (k<m) nrm = QRcolumnsPart[k].aggregate(hypot,F.identity)

      var i = k
      while (i < originalRows) { // fixes bug reported by hong.44@osu.edu
        nrm = java.lang.Math.hypot(nrm, qr(i, k))
        i += 1
      }


      if (nrm != 0.0) {
        // Form k-th Householder vector.
        if (qr(k, k) < 0) {
          nrm = -nrm
        }
        QRcolumnsPart(k).assign(Functions.div(nrm))
        /*
         for (Int i = k; i < m; i++) {
         QR[i][k] /= nrm;
         }
         */

        qr(k, k) = qr(k, k) + 1

        // Apply transformation to remaining columns.
        var j = k + 1
        while (j < originalColumns) {
          val QRcolj = qr.viewColumn(j).viewPart(k, originalRows - k)
          var s = QRcolumnsPart(k).dot(QRcolj)
          /*
           // fixes bug reported by John Chambers
           DoubleMatrix1D QRcolj = QR.viewColumn(j).viewPart(k,m-k)
           Double s = QRcolumnsPart[k].zDotProduct(QRcolumns[j])
           Double s = 0.0;
           for (Int i = k; i < m; i++) {
           s += QR[i][k]*QR[i][j]
           }
           */
          s = -s / qr(k, k)
          //QRcolumnsPart[j].assign(QRcolumns[k], F.plusMult(s))

          var i = k
          while (i < originalRows) {
            qr(i, j) = qr(i, j) + s * qr(i, k)
            i += 1
          }
          
          j += 1
        }
      }
      rDiag(k) = -nrm
      
      k += 1
    }
  }
  

  /**
   * Generates and returns the (economy-sized) orthogonal factor <tt>Q</tt>.
   *
   * @return <tt>Q</tt>
   */
  def getQ: Matrix = {
    val columns = math.min(originalColumns, originalRows)
    val q = qr.like(originalRows, columns)
    var k = columns - 1
    while (k >= 0) {
      val QRcolk = qr.viewColumn(k).viewPart(k, originalRows - k)
      q.set(k, k, 1)
      var j = k
      while (j < columns) {
        if (qr.get(k, k) != 0) {
          val Qcolj = q.viewColumn(j).viewPart(k, originalRows - k)
          val s = -QRcolk.dot(Qcolj) / qr.get(k, k)
          Qcolj.assign(QRcolk, Functions.plusMult(s))
        }
        j += 1
      }
      
      k -= 1
    }
    q
  }

  /**
   * Returns the upper triangular factor, <tt>R</tt>.
   *
   * @return <tt>R</tt>
   */
  def getR: Matrix = {
    val rows = math.min(originalRows, originalColumns)
    val r = qr.like(rows, originalColumns)
    var i = 0
    while (i < rows) {
      var j = 0
      while (j < originalColumns) {
        if (i < j) {
          r(i, j) = qr(i, j)
        } else if (i == j) {
          r(i, j) = rDiag(i)
        } else {
          r(i, j) = 0
        }
        j += 1
      }
      i += 1
    }
    r
  }

  /**
   * Returns whether the matrix <tt>A</tt> has full rank.
   *
   * @return true if <tt>R</tt>, and hence <tt>A</tt>, has full rank.
   */
  def hasFullRank: Boolean = {
    var j = 0
    while (j < originalColumns) {
      if (rDiag(j) == 0) {
        return false
      }
      j += 1
    }
    true
  }

  /**
   * Least squares solution of <tt>A*X = B</tt> <tt>returns X</tt>.
   *
   * @param B A matrix with as many rows as <tt>A</tt> and any number of columns.
   * @return <tt>X</tt> that minimizes the two norm of <tt>Q*R*X - B</tt>.
   * @throws IllegalArgumentException if <tt>B.rows() != A.rows()</tt>.
   */
  def solve(B: Matrix): Matrix = {
    if (B.numRows != originalRows) {
      throw new IllegalArgumentException("Matrix row dimensions must agree.")
    }

    val columns = B.numCols
    val x = B.like(originalColumns, columns)

    // this can all be done a bit more efficiently if we don't actually
    // form explicit versions of Q^T and R but this code isn't soo bad
    // and it is much easier to understand
    val qt = getQ.transpose
    val y = qt.times(B)

    val r = getR
    var k = math.min(originalColumns, originalRows) - 1
    while (k >= 0) {
      // X[k,] = Y[k,] / R[k,k], note that X[k,] starts with 0 so += is same as =
      x.viewRow(k).assign(y.viewRow(k), Functions.plusMult(1 / r.get(k, k)))

      // Y[0:(k-1),] -= R[0:(k-1),k] * X[k,]
      val rColumn = r.viewColumn(k).viewPart(0, k)
      var c = 0
      while (c < columns) {
        y.viewColumn(c).viewPart(0, k).assign(rColumn, Functions.plusMult(-x.get(k, c)))
        c += 1
      }
      k -= 1
    }
    return x;
  }

  /**
   * Returns a rough string rendition of a QR.
   */
  override
  def toString = {
    "QR(%d,%d,fullRank=%s)".format(Locale.ENGLISH, originalColumns, originalRows, hasFullRank)
  }
}
