package org.aiotrade.lib.math.algebra

@serializable
final class EigenvalueDecomposition(V: Array[Array[Double]]) {

  if(V.length != V(0).length) {
    throw new IllegalArgumentException("Matrix must be square")
  }

  /** Row and column dimension (square matrix). */
  private val n = V.length

  /** Arrays for internal storage of eigenvalues. */
  private val d = new Array[Double](n)
  private val e = new Array[Double](n)


  /** Array for internal storage of nonsymmetric Hessenberg form. */
  private var H: Array[Array[Double]] = _

  /** Working storage for nonsymmetric algorithm. */
  private var ort: Array[Double] = _

  // Complex scalar division.

  private var cdivr: Double = _
  private var cdivi: Double = _
  
  if (Matrix.isSymmetric(V)) {
    // Tridiagonalize.
    tred2()

    // Diagonalize.
    tql2()

  } else {
    H = Array.ofDim[Double](n, n)
    ort = Array.ofDim[Double](n)

    var j = 0
    while (j < n) {
      var i = 0
      while (i < n) {
        H(i)(j) = V(i)(j)
        i += 1
      }
      j += 1
    }

    // Reduce to Hessenberg form.
    orthes()

    // Reduce Hessenberg to real Schur form.
    hqr2()
  }

  def this(A: Matrix) = {
    this(Matrix.toArray(A))
  }

  private def cdiv(xr: Double, xi: Double, yr: Double, yi: Double) {
    var r = 0.0
    var d = 0.0
    if (math.abs(yr) > math.abs(yi)) {
      r = yi / yr
      d = yr + r * yi
      cdivr = (xr + r * xi) / d
      cdivi = (xi - r * xr) / d
    } else {
      r = yr / yi
      d = yi + r * yr
      cdivr = (r * xr + xi) / d
      cdivi = (r * xi - xr) / d
    }
  }

  /**
   * Returns the block diagonal eigenvalue matrix, <tt>D</tt>.
   *
   * @return <tt>D</tt>
   */
  def getD: Matrix = {
    val D = Array.ofDim[Double](n, n)
    var i = 0
    while (i < n) {
      var j = 0
      while (j < n) {
        D(i)(j) = 0.0
        j += 1
      }
      D(i)(i) = d(i)
      if (e(i) > 0) {
        D(i)(i + 1) = e(i)
      } else if (e(i) < 0) {
        D(i)(i - 1) = e(i)
      }
      i += 1
    }
    DenseMatrix(D)
  }

  /**
   * Returns the imaginary parts of the eigenvalues.
   *
   * @return imag(diag(D))
   */
  def getImagEigenvalues: Vector = {
    DenseVector(e)
  }

  /**
   * Returns the real parts of the eigenvalues.
   *
   * @return real(diag(D))
   */
  def getRealEigenvalues: Vector = {
    DenseVector(d)
  }

  /**
   * Returns the eigenvector matrix, <tt>V</tt>
   *
   * @return <tt>V</tt>
   */
  def getV: Matrix = {
    DenseMatrix(V)
  }

  /** Nonsymmetric reduction from Hessenberg to real Schur form. */
  private def hqr2() {
    //  This is derived from the Algol procedure hqr2,
    //  by Martin and Wilkinson, Handbook for Auto. Comp.,
    //  Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutine in EISPACK.

    // Initialize

    var nn = this.n
    var n = nn - 1
    val low = 0
    val high = nn - 1
    val eps = math.pow(2.0, -52.0)

    // Store roots isolated by balanc and compute matrix norm

    var norm = 0.0
    var i = 0
    while (i < nn) {
      if (i < low || i > high) {
        d(i) = H(i)(i)
        e(i) = 0.0
      }
      var j = math.max(i - 1, 0)
      while (j < nn) {
        norm += math.abs(H(i)(j))
        j += 1
      }
      i += 1
    }

    // Outer loop over eigenvalue index

    var iter = 0
    var y = 0.0
    var x = 0.0
    var w = 0.0
    var z = 0.0
    var s = 0.0
    var r = 0.0
    var q = 0.0
    var p = 0.0
    var exshift = 0.0
    while (n >= low) {

      // Look for single small sub-diagonal element

      var l = n
      var continue = true
      while (l > low && continue) {
        s = math.abs(H(l - 1)(l - 1)) + math.abs(H(l)(l))
        if (s == 0.0) {
          s = norm
        }
        if (math.abs(H(l)(l - 1)) < eps * s) {
          continue = false
        } else {
          l -= 1
        }
      }

      // Check for convergence
      // One root found

      if (l == n) {
        H(n)(n) += exshift
        d(n) = H(n)(n)
        e(n) = 0.0
        n -= 1
        iter = 0

        // Two roots found

      } else if (l == n - 1) {
        w = H(n)(n - 1) * H(n - 1)(n)
        p = (H(n - 1)(n - 1) - H(n)(n)) / 2.0
        q = p * p + w
        z = math.sqrt(math.abs(q))
        H(n)(n) += exshift
        H(n - 1)(n - 1) += exshift
        x = H(n)(n)

        // Real pair

        if (q >= 0) {
          if (p >= 0) {
            z = p + z
          } else {
            z = p - z
          }
          d(n - 1) = x + z
          d(n) = d(n - 1)
          if (z != 0.0) {
            d(n) = x - w / z
          }
          e(n - 1) = 0.0
          e(n) = 0.0
          x = H(n)(n - 1)
          s = math.abs(x) + math.abs(z)
          p = x / s
          q = z / s
          r = math.sqrt(p * p + q * q)
          p /= r
          q /= r

          // Row modification

          var j = n - 1
          while (j < nn) {
            z = H(n - 1)(j)
            H(n - 1)(j) = q * z + p * H(n)(j)
            H(n)(j) = q * H(n)(j) - p * z
            j += 1
          }

          // Column modification

          var i = 0
          while (i <= n) {
            z = H(i)(n - 1)
            H(i)(n - 1) = q * z + p * H(i)(n)
            H(i)(n) = q * H(i)(n) - p * z
            i += 1
          }

          // Accumulate transformations

          i = low
          while (i <= high) {
            z = V(i)(n - 1)
            V(i)(n - 1) = q * z + p * V(i)(n)
            V(i)(n) = q * V(i)(n) - p * z
            i += 1
          }

          // Complex pair

        } else {
          d(n - 1) = x + p
          d(n) = x + p
          e(n - 1) = z
          e(n) = -z
        }
        n -= 2
        iter = 0

        // No convergence yet

      } else {

        // Form shift

        x = H(n)(n)
        y = 0.0
        w = 0.0
        if (l < n) {
          y = H(n - 1)(n - 1)
          w = H(n)(n - 1) * H(n - 1)(n)
        }

        // Wilkinson's original ad hoc shift

        if (iter == 10) {
          exshift += x
          var i = low
          while (i <= n) {
            H(i)(i) -= x
            i += 1
          }
          s = math.abs(H(n)(n - 1)) + math.abs(H(n - 1)(n - 2))
          x = 0.75 * s
          y = 0.75 * s
          w = -0.4375 * s * s
        }

        // MATLAB's new ad hoc shift

        if (iter == 30) {
          s = (y - x) / 2.0
          s = s * s + w
          if (s > 0) {
            s = math.sqrt(s)
            if (y < x) {
              s = -s
            }
            s = x - w / ((y - x) / 2.0 + s)
            var i = low
            while (i <= n) {
              H(i)(i) -= s
              i += 1
            }
            exshift += s
            x = 0.964
            y = 0.964
            w = 0.964
          }
        }

        iter += 1   // (Could check iteration count here.)

        // Look for two consecutive small sub-diagonal elements

        var m = n - 2
        var continue = true
        while (m >= l && continue) {
          z = H(m)(m)
          r = x - z
          s = y - z
          p = (r * s - w) / H(m + 1)(m) + H(m)(m + 1)
          q = H(m + 1)(m + 1) - z - r - s
          r = H(m + 2)(m + 1)
          s = math.abs(p) + math.abs(q) + math.abs(r)
          p /= s
          q /= s
          r /= s
          if (m == l) {
            continue = false
          } else if (math.abs(H(m)(m - 1)) * (math.abs(q) + math.abs(r)) < 
                     eps * math.abs(p) * (math.abs(H(m - 1)(m - 1)) + math.abs(z) + math.abs(H(m + 1)(m + 1)))) {
            continue = false
          } else {
            m -= 1
          }
        }

        var i = m + 2
        while (i <= n) {
          H(i)(i - 2) = 0.0
          if (i > m + 2) {
            H(i)(i - 3) = 0.0
          }
          i += 1
        }

        // Double QR step involving rows l:n and columns m:n

        var k = m
        continue = true
        while (k <= n - 1 && continue) {
          val notlast = k != n - 1
          if (k != m) {
            p = H(k)(k - 1)
            q = H(k + 1)(k - 1)
            r = if (notlast) H(k + 2)(k - 1) else 0.0
            x = math.abs(p) + math.abs(q) + math.abs(r)
            if (x != 0.0) {
              p /= x
              q /= x
              r /= x
            }
          }
          if (x == 0.0) {
            continue = false
          } else {
            s = math.sqrt(p * p + q * q + r * r)
            if (p < 0) {
              s = -s
            }
            if (s != 0) {
              if (k != m) {
                H(k)(k - 1) = -s * x
              } else if (l != m) {
                H(k)(k - 1) = -H(k)(k - 1)
              }
              p += s
              x = p / s
              y = q / s
              z = r / s
              q /= p
              r /= p

              // Row modification

              var j = k
              while (j < nn) {
                p = H(k)(j) + q * H(k + 1)(j)
                if (notlast) {
                  p += r * H(k + 2)(j)
                  H(k + 2)(j) -= p * z
                }
                H(k)(j) -= p * x
                H(k + 1)(j) -= p * y
                j += 1
              }

              // Column modification

              var i = 0
              while (i <= math.min(n, k + 3)) {
                p = x * H(i)(k) + y * H(i)(k + 1)
                if (notlast) {
                  p += z * H(i)(k + 2)
                  H(i)(k + 2) -= p * r
                }
                H(i)(k) -= p
                H(i)(k + 1) -= p * q
                i += 1
              }

              // Accumulate transformations

              i = low
              while (i <= high) {
                p = x * V(i)(k) + y * V(i)(k + 1)
                if (notlast) {
                  p += z * V(i)(k + 2)
                  V(i)(k + 2) -= p * r
                }
                V(i)(k) -= p
                V(i)(k + 1) -= p * q
                i += 1
              }
            }  // (s != 0)
          }
          k += 1
        }  // k loop
      }  // check convergence
    }  // while (n >= low)

    // Backsubstitute to find vectors of upper triangular form

    if (norm == 0.0) {
      return
    }

    n = nn - 1
    while (n >= 0) {
      p = d(n)
      q = e(n)

      // Real vector

      var t = 0.0
      if (q == 0) {
        var l = n
        H(n)(n) = 1.0
        var i = n - 1
        while (i >= 0) {
          w = H(i)(i) - p
          r = 0.0
          var j = l
          while (j <= n) {
            r += H(i)(j) * H(j)(n)
            j += 1
          }
          if (e(i) < 0.0) {
            z = w
            s = r
          } else {
            l = i
            if (e(i) == 0.0) {
              if (w != 0.0) {
                H(i)(n) = -r / w
              } else {
                H(i)(n) = -r / (eps * norm)
              }

              // Solve real equations

            } else {
              x = H(i)(i + 1)
              y = H(i + 1)(i)
              q = (d(i) - p) * (d(i) - p) + e(i) * e(i)
              t = (x * s - z * r) / q
              H(i)(n) = t
              if (math.abs(x) > math.abs(z)) {
                H(i + 1)(n) = (-r - w * t) / x
              } else {
                H(i + 1)(n) = (-s - y * t) / z
              }
            }

            // Overflow control

            t = math.abs(H(i)(n))
            if (eps * t * t > 1) {
              var j = i
              while (j <= n) {
                H(j)(n) /= t
                j += 1
              }
            }
          }
          i -= 1
        }

        // Complex vector

      } else if (q < 0) {
        var l = n - 1

        // Last vector component imaginary so matrix is triangular

        if (math.abs(H(n)(n - 1)) > math.abs(H(n - 1)(n))) {
          H(n - 1)(n - 1) = q / H(n)(n - 1)
          H(n - 1)(n) = -(H(n)(n) - p) / H(n)(n - 1)
        } else {
          cdiv(0.0, -H(n - 1)(n), H(n - 1)(n - 1) - p, q)
          H(n - 1)(n - 1) = cdivr
          H(n - 1)(n) = cdivi
        }
        H(n)(n - 1) = 0.0
        H(n)(n) = 1.0
        var i = n - 2
        while (i >= 0) {
          var ra = 0.0
          var sa = 0.0
          var j = l
          while (j <= n) {
            ra += H(i)(j) * H(j)(n - 1)
            sa += H(i)(j) * H(j)(n)
            j += 1
          }
          w = H(i)(i) - p

          if (e(i) < 0.0) {
            z = w
            r = ra
            s = sa
          } else {
            l = i
            if (e(i) == 0) {
              cdiv(-ra, -sa, w, q)
              H(i)(n - 1) = cdivr
              H(i)(n) = cdivi
            } else {

              // Solve complex equations

              x = H(i)(i + 1)
              y = H(i + 1)(i)
              var vr = (d(i) - p) * (d(i) - p) + e(i) * e(i) - q * q
              var vi = (d(i) - p) * 2.0 * q
              if (vr == 0.0 && vi == 0.0) {
                vr = eps * norm * (math.abs(w) + math.abs(q) + math.abs(x) + math.abs(y) + math.abs(z))
              }
              cdiv(x * r - z * ra + q * sa, x * s - z * sa - q * ra, vr, vi)
              H(i)(n - 1) = cdivr
              H(i)(n) = cdivi
              if (math.abs(x) > math.abs(z) + math.abs(q)) {
                H(i + 1)(n - 1) = (-ra - w * H(i)(n - 1) + q * H(i)(n)) / x
                H(i + 1)(n) = (-sa - w * H(i)(n) - q * H(i)(n - 1)) / x
              } else {
                cdiv(-r - y * H(i)(n - 1), -s - y * H(i)(n), z, q)
                H(i + 1)(n - 1) = cdivr
                H(i + 1)(n) = cdivi
              }
            }

            // Overflow control

            t = math.max(math.abs(H(i)(n - 1)), math.abs(H(i)(n)))
            if (eps * t * t > 1) {
              var j = i
              while (j <= n) {
                H(j)(n - 1) /= t
                H(j)(n) /= t
                j += 1
              }
            }
          }
          i -= 1
        }
      }
      n -= 1
    }

    // Vectors of isolated roots

    i = 0
    while (i < nn) {
      if (i < low || i > high) {
        System.arraycopy(H(i), i, V(i), i, nn - i)
      }
      i += 1
    }

    // Back transformation to get eigenvectors of original matrix

    var j = nn - 1
    while (j >= low) {
      var i = low
      while (i <= high) {
        z = 0.0
        var k = low
        while (k <= math.min(j, high)) {
          z += V(i)(k) * H(k)(j)
          k += 1
        }
        V(i)(j) = z
        i += 1
      }
      j -= 1
    }
  }

  /** Nonsymmetric reduction to Hessenberg form. */
  private def orthes() {
    //  This is derived from the Algol procedures orthes and ortran,
    //  by Martin and Wilkinson, Handbook for Auto. Comp.,
    //  Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutines in EISPACK.

    val low = 0
    val high = n - 1

    var m = low + 1
    while (m <= high - 1) {

      // Scale column.

      var scale = 0.0
      var i = m
      while (i <= high) {
        scale += math.abs(H(i)(m - 1))
        i += 1
      }
      if (scale != 0.0) {

        // Compute Householder transformation.

        var h = 0.0
        var i = high
        while (i >= m) {
          ort(i) = H(i)(m - 1) / scale
          h += ort(i) * ort(i)
          i -= 1
        }
        var g = math.sqrt(h)
        if (ort(m) > 0) {
          g = -g
        }
        h -= ort(m) * g
        ort(m) -= g

        // Apply Householder similarity transformation
        // H = (I-u*u'/h)*H*(I-u*u')/h)

        var j = m
        while (j < n) {
          var f = 0.0
          var i = high
          while (i >= m) {
            f += ort(i) * H(i)(j)
            i -= 1
          }
          f /= h
          i = m
          while (i <= high) {
            H(i)(j) -= f * ort(i)
            i += 1
          }
          j += 1
        }

        i = 0
        while (i <= high) {
          var f = 0.0
          var j = high
          while (j >= m) {
            f += ort(j) * H(i)(j)
            j -= 1
          }
          f /= h
          j = m
          while (j <= high) {
            H(i)(j) -= f * ort(j)
            j += 1
          }
          i += 1
        }
        ort(m) = scale * ort(m)
        H(m)(m - 1) = scale * g
      }
      m += 1
    }

    // Accumulate transformations (Algol's ortran).

    var i = 0
    while (i < n) {
      var j = 0
      while (j < n) {
        V(i)(j) = if (i == j) 1.0 else 0.0
        j += 1
      }
      i += 1
    }

    m = high - 1
    while (m >= low + 1) {
      if (H(m)(m - 1) != 0.0) {
        var i = m + 1
        while (i <= high) {
          ort(i) = H(i)(m - 1)
          i += 1
        }
        var j = m
        while (j <= high) {
          var g = 0.0
          var i = m
          while (i <= high) {
            g += ort(i) * V(i)(j)
            i += 1
          }
          // Double division avoids possible underflow
          g = g / ort(m) / H(m)(m - 1)
          i = m
          while (i <= high) {
            V(i)(j) += g * ort(i)
            i += 1
          }
          j += 1
        }
      }
      m -= 1
    }
  }

  /**
   * Returns a String with (propertyName, propertyValue) pairs. Useful for debugging or to quickly get the rough
   * picture. For example,
   * <pre>
   * rank          : 3
   * trace         : 0
   * </pre>
   */
  override
  def toString = {
    val sb = new StringBuilder()

    sb.append("---------------------------------------------------------------------\n")
    sb.append("EigenvalueDecomposition(A) --> D, V, realEigenvalues, imagEigenvalues\n")
    sb.append("---------------------------------------------------------------------\n")

    sb.append("realEigenvalues = ")
    val unknown = "Illegal operation or error: "
    try {
      sb.append(String.valueOf(this.getRealEigenvalues))
    } catch {
      case ex: IllegalArgumentException =>
        sb.append(unknown).append(ex.getMessage)
    }

    sb.append("\nimagEigenvalues = ")
    try {
      sb.append(String.valueOf(this.getImagEigenvalues))
    } catch {
      case ex: IllegalArgumentException =>
        sb.append(unknown).append(ex.getMessage)
    }

    sb.append("\n\nD = ")
    try {
      sb.append(String.valueOf(this.getD))
    } catch {
      case ex: IllegalArgumentException =>
        sb.append(unknown).append(ex.getMessage)
    }

    sb.append("\n\nV = ")
    try {
      sb.append(String.valueOf(this.getV))
    } catch {
      case ex: IllegalArgumentException =>
        sb.append(unknown).append(ex.getMessage)
    }

    sb.toString
  }

  /** Symmetric tridiagonal QL algorithm. */
  private def tql2() {

    //  This is derived from the Algol procedures tql2, by
    //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutine in EISPACK.

    System.arraycopy(e, 1, e, 0, n - 1)
    e(n - 1) = 0.0

    var f = 0.0
    var tst1 = 0.0
    val eps = math.pow(2.0, -52.0)
    var l = 0
    while (l < n) {

      // Find small subdiagonal element

      tst1 = math.max(tst1, math.abs(d(l)) + math.abs(e(l)))
      var m = l
      var continue = true
      while (m < n && continue) {
        if (math.abs(e(m)) <= eps * tst1) {
          continue = false
        } else {
          m += 1
        }
      }

      // If m == l, d(l) is an eigenvalue,
      // otherwise, iterate.

      if (m > l) {
        var iter = 0
        do {
          iter += 1  // (Could check iteration count here.)

          // Compute implicit shift

          var g = d(l)
          var p = (d(l + 1) - g) / (2.0 * e(l))
          var r = java.lang.Math.hypot(p, 1.0)
          if (p < 0) {
            r = -r
          }
          d(l) = e(l) / (p + r)
          d(l + 1) = e(l) * (p + r)
          val dl1 = d(l + 1)
          var h = g - d(l)
          var i = l + 2
          while (i < n) {
            d(i) -= h
            i += 1
          }
          f += h

          // Implicit QL transformation.

          p = d(m)
          var c = 1.0
          var c2 = c
          var c3 = c
          var el1 = e(l + 1)
          var s = 0.0
          var s2 = 0.0
          i = m - 1
          while (i >= l) {
            c3 = c2
            c2 = c
            s2 = s
            g = c * e(i)
            h = c * p
            r = java.lang.Math.hypot(p, e(i))
            e(i + 1) = s * r
            s = e(i) / r
            c = p / r
            p = c * d(i) - s * g
            d(i + 1) = h + s * (c * g + s * d(i))

            // Accumulate transformation.

            var k = 0
            while (k < n) {
              h = V(k)(i + 1)
              V(k)(i + 1) = s * V(k)(i) + c * h
              V(k)(i) = c * V(k)(i) - s * h
              k += 1
            }
            i -= 1
          }
          p = -s * s2 * c3 * el1 * e(l) / dl1
          e(l) = s * p
          d(l) = c * p

          // Check for convergence.

        } while (math.abs(e(l)) > eps * tst1)
      }
      d(l) += f
      e(l) = 0.0
      l += 1
    }

    // Sort eigenvalues and corresponding vectors.

    var i = 0
    while (i < n - 1) {
      var k = i
      var p = d(i)
      var j = i + 1
      while (j < n) {
        if (d(j) < p) {
          k = j
          p = d(j)
        }
        j += 1
      }
      if (k != i) {
        d(k) = d(i)
        d(i) = p
        var j = 0
        while (j < n) {
          p = V(j)(i)
          V(j)(i) = V(j)(k)
          V(j)(k) = p
          j += 1
        }
      }
      i += 1
    }
  }

  /** Symmetric Householder reduction to tridiagonal form. */
  private def tred2() {
    //  This is derived from the Algol procedures tred2 by
    //  Bowdler, Martin, Reinsch, and Wilkinson, Handbook for
    //  Auto. Comp., Vol.ii-Linear Algebra, and the corresponding
    //  Fortran subroutine in EISPACK.


    System.arraycopy(V(n - 1), 0, d, 0, n)


    // Householder reduction to tridiagonal form.

    var i = n - 1
    while (i > 0) {

      // Scale to avoid under/overflow.

      var scale = 0.0
      var k = 0
      while (k < i) {
        scale += math.abs(d(k))
        k += 1
      }
      var h = 0.0
      if (scale == 0.0) {
        e(i) = d(i - 1)
        var j = 0
        while (j < i) {
          d(j) = V(i - 1)(j)
          V(i)(j) = 0.0
          V(j)(i) = 0.0
          j += 1
        }
      } else {

        // Generate Householder vector.

        var k = 0
        while (k < i) {
          d(k) /= scale
          h += d(k) * d(k)
          k += 1
        }
        var f = d(i - 1)
        var g = math.sqrt(h)
        if (f > 0) {
          g = -g
        }
        e(i) = scale * g
        h -= f * g
        d(i - 1) = f - g
        var j = 0
        while (j < i) {
          e(j) = 0.0
          j += 1
        }

        // Apply similarity transformation to remaining columns.

        j = 0
        while (j < i) {
          f = d(j)
          V(j)(i) = f
          g = e(j) + V(j)(j) * f
          var k = j + 1
          while (k <= i - 1) {
            g += V(k)(j) * d(k)
            e(k) += V(k)(j) * f
            k += 1
          }
          e(j) = g
          j += 1
        }
        f = 0.0
        j = 0
        while (j < i) {
          e(j) /= h
          f += e(j) * d(j)
          j += 1
        }
        val hh = f / (h + h)
        j = 0
        while (j < i) {
          e(j) -= hh * d(j)
          j += 1
        }
        j = 0
        while (j < i) {
          f = d(j)
          g = e(j)
          var k = j
          while (k <= i - 1) {
            V(k)(j) -= f * e(k) + g * d(k)
            k += 1
          }
          d(j) = V(i - 1)(j)
          V(i)(j) = 0.0
          j += 1
        }
      }
      d(i) = h
      
      i -= 1
    }

    // Accumulate transformations.

    i = 0
    while (i < n - 1) {
      V(n - 1)(i) = V(i)(i)
      V(i)(i) = 1.0
      val h = d(i + 1)
      if (h != 0.0) {
        var k = 0
        while (k <= i) {
          d(k) = V(k)(i + 1) / h
          k += 1
        }
        var j = 0
        while (j <= i) {
          var g = 0.0
          var k = 0
          while (k <= i) {
            g += V(k)(i + 1) * V(k)(j)
            k += 1
          }
          k = 0
          while (k <= i) {
            V(k)(j) -= g * d(k)
            k += 1
          }
          j += 1
        }
      }
      var k = 0
      while (k <= i) {
        V(k)(i + 1) = 0.0
        k += 1
      }
      i += 1
    }
    var j = 0
    while (j < n) {
      d(j) = V(n - 1)(j)
      V(n - 1)(j) = 0.0
      j += 1
    }
    V(n - 1)(n - 1) = 1.0
    e(0) = 0.0
  }
}
