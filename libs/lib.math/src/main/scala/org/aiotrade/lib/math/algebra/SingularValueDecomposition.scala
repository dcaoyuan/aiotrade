package org.aiotrade.lib.math.algebra

/**
 * Constructs and returns a new singular value decomposition object; The
 * decomposed matrices can be retrieved via instance methods of the returned
 * decomposition object.
 * @see http://en.wikipedia.org/wiki/Singular_value_decomposition
 * also @see http://en.wikipedia.org/wiki/Eigenvalue,_eigenvector_and_eigenspace
 * 
 * @param arg
 *            A rectangular matrix.
 */
@serializable
class SingularValueDecomposition(arg: Matrix) {
  
  /** Row and column dimensions. */
  if (arg.numRows < arg.numCols) {
    transpositionNeeded = true
  }
    
  // Derived from LINPACK code.
  // Initialize.
  private val (m, n) = 
    if (transpositionNeeded) {
      (arg.numCols, arg.numRows) // use the transpose Matrix
    } else {
      (arg.numRows, arg.numCols)
    }
  val a = Array.ofDim[Double](m, n)

  /** Array for internal storage of singular values. */
  private val s = Array.ofDim[Double](math.min(m + 1, n))
  /** Arrays for internal storage of U and V. */
  private val u = Array.ofDim[Double](m, math.min(m, n))
  private val v = Array.ofDim[Double](n, n)
  
  /** To handle the case where numRows < numCols and to use the fact that SVD(A')=VSU'=> SVD(A')'=SVD(A)**/
  private var transpositionNeeded = false
  
  def init(arg: Matrix) {
    var i = 0
    while (i < m) {
      var j = 0
      while (j < n) {
        a(i)(j) = if (transpositionNeeded) arg.get(j, i) else arg.get(i, j)
        j += 1
      }
      i += 1
    }
    
    val nu = math.min(m, n)
    val e = Array.ofDim[Double](n)
    val work = Array.ofDim[Double](m)
    var wantu = true
    var wantv = true
    
    // Reduce A to bidiagonal form, storing the diagonal elements
    // in s and the super-diagonal elements in e.
    
    val nct = math.min(m - 1, n)
    val nrt = math.max(0, math.min(n - 2, m))
    var k = 0
    while (k < math.max(nct, nrt)) {
      if (k < nct) {
        
        // Compute the transformation for the k-th column and
        // place the k-th diagonal in s(k).
        // Compute 2-norm of k-th column without under/overflow.
        s(k) = 0
        var i = k
        while (i < m) {
          s(k) = java.lang.Math.hypot(s(k), a(i)(k))
          i += 1
        }
        if (s(k) != 0.0) {
          if (a(k)(k) < 0.0) {
            s(k) = -s(k)
          }
          var i = k
          while (i < m) {
            a(i)(k) /= s(k)
            i += 1
          }
          a(k)(k) += 1.0
        }
        s(k) = -s(k)
      }
      var j = k + 1
      while (j < n) {
        if (k < nct && s(k) != 0.0) {
          // Apply the transformation.
          var t = 0.0
          var i = k
          while (i < m) {
            t += a(i)(k) * a(i)(j)
            i += 1
          }
          t = -t / a(k)(k)
          i = k
          while (i < m) {
            a(i)(j) += t * a(i)(k)
            i += 1
          }
        }
        
        // Place the k-th row of A into e for the
        // subsequent calculation of the row transformation.
        e(j) = a(k)(j)
        
        j += 1
      }
      
      if (wantu && k < nct) {
        // Place the transformation in U for subsequent back multiplication.
        var i = k
        while (i < m) {
          u(i)(k) = a(i)(k)
          i += 1
        }
      }
      
      if (k < nrt) {
        // Compute the k-th row transformation and place the
        // k-th super-diagonal in e(k).
        // Compute 2-norm without under/overflow.
        e(k) = 0
        var i = k + 1
        while (i < n) {
          e(k) = java.lang.Math.hypot(e(k), e(i))
          i += 1
        }
        if (e(k) != 0.0) {
          if (e(k + 1) < 0.0) {
            e(k) = -e(k)
          }
          var i = k + 1
          while (i < n) {
            e(i) /= e(k)
            i += 1
          }
          e(k + 1) += 1.0
        }
        e(k) = -e(k)
        if (k + 1 < m && e(k) != 0.0) {
          
          // Apply the transformation.
          
          var i = k + 1
          while (i < m) {
            work(i) = 0.0
            i += 1
          }
          var j = k + 1
          while (j < n) {
            var i = k + 1
            while (i < m) {
              work(i) += e(j) * a(i)(j)
              i += 1
            }
            j += 1
          }
          j = k + 1
          while (j < n) {
            val t = -e(j) / e(k + 1)
            var i = k + 1
            while (i < m) {
              a(i)(j) += t * work(i)
              i += 1
            }
            j += 1
          }
        }
        if (wantv) {
          
          // Place the transformation in V for subsequent
          // back multiplication.
          
          var i = k + 1
          while (i < n) {
            v(i)(k) = e(i)
            i += 1
          }
        }
      }
      
      k += 1
    }
    
    // Set up the final bidiagonal matrix or order p.
    
    var p = math.min(n, m + 1)
    if (nct < n) {
      s(nct) = a(nct)(nct)
    }
    if (m < p) {
      s(p - 1) = 0.0
    }
    if (nrt + 1 < p) {
      e(nrt) = a(nrt)(p - 1)
    }
    e(p - 1) = 0.0
    
    // If required, generate U.
    
    if (wantu) {
      var j = nct
      while (j < nu) {
        var i = 0
        while (i < m) {
          u(i)(j) = 0.0
          i += 1
        }
        u(j)(j) = 1.0
        j += 1
      }
      var k = nct - 1
      while (k >= 0) {
        if (s(k) != 0.0) {
          var j = k + 1
          while (j < nu) {
            var t = 0.0
            var i = k
            while (i < m) {
              t += u(i)(k) * u(i)(j)
              i += 1
            }
            t = -t / u(k)(k)
            
            i = k
            while (i < m) {
              u(i)(j) += t * u(i)(k)
              i += 1
            }
            j += 1
          }
          
          var i = k
          while (i < m) {
            u(i)(k) = -u(i)(k)
            i += 1
          }
          u(k)(k) = 1.0 + u(k)(k)
          
          i = 0
          while (i < k - 1) {
            u(i)(k) = 0.0
            i += 1
          }
        } else {
          var i = 0
          while (i < m) {
            u(i)(k) = 0.0
            i += 1
          }
          u(k)(k) = 1.0
        }
        k -= 1
      }
    }
    
    // If required, generate V.
    
    if (wantv) {
      var k = n - 1
      while (k >= 0) {
        if (k < nrt && e(k) != 0.0) {
          var j = k + 1
          while (j < nu) {
            var t = 0.0
            var i = k + 1
            while (i < n) {
              t += v(i)(k) * v(i)(j)
              i += 1
            }
            t = -t / v(k + 1)(k)
            
            i = k + 1
            while (i < n) {
              v(i)(j) += t * v(i)(k)
              i += 1
            }
            
            j += 1
          }
        }
        var i = 0
        while (i < n) {
          v(i)(k) = 0.0
          i += 1
        }
        v(k)(k) = 1.0
        k -= 1
      }
    }
    
    // Main iteration loop for the singular values.
    
    val pp = p - 1
    //Int iter = 0
    val eps = math.pow(2.0, -52.0)
    while (p > 0) {
      // Here is where a test for too many iterations would go.
      
      // This section of the program inspects for
      // negligible elements in the s and e arrays.  On
      // completion the variables kase and k are set as follows.
      
      // kase = 1     if s(p) and e[k-1] are negligible and k<p
      // kase = 2     if s(k) is negligible and k<p
      // kase = 3     if e[k-1] is negligible, k<p, and
      //              s(k), ..., s(p) are not negligible (qr step).
      // kase = 4     if e(p-1) is negligible (convergence).
      
      var k = p - 2
      var continue = true
      while (k >= -1 && continue) {
        if (k == -1) {
          continue = false
        } else {
          if (math.abs(e(k)) <= eps * (math.abs(s(k)) + math.abs(s(k + 1)))) {
            e(k) = 0.0
            continue = false
          } else {
            k -= 1
          }
        }
      }
      var kase = 0
      if (k == p - 2) {
        kase = 4
      } else {
        var ks = p - 1
        var continue = true
        while (ks >= k && continue) {
          if (ks == k) {
            continue = false
          } else {
            val t = (if (ks == p) 0.0 else math.abs(e(ks))) + (if (ks == k + 1) 0.0 else math.abs(e(ks - 1)))
            if (math.abs(s(ks)) <= eps * t) {
              s(ks) = 0.0
              continue = false
            } else {
              ks -= 1
            }
          }
        }
        if (ks == k) {
          kase = 3
        } else if (ks == p - 1) {
          kase = 1
        } else {
          kase = 2
          k = ks
        }
      }
      k += 1
      
      // Perform the task indicated by kase.
      
      kase match {
        
        // Deflate negligible s(p).
        
        case 1 =>
          var f = e(p - 2)
          e(p - 2) = 0.0
          var j = p - 2
          while (j >= k) {
            var t = java.lang.Math.hypot(s(j), f)
            val cs = s(j) / t
            val sn = f / t
            s(j) = t
            if (j != k) {
              f = -sn * e(j - 1)
              e(j - 1) = cs * e(j - 1)
            }
            if (wantv) {
              var i = 0
              while (i < n) {
                t = cs * v(i)(j) + sn * v(i)(p - 1)
                v(i)(p - 1) = -sn * v(i)(j) + cs * v(i)(p - 1)
                v(i)(j) = t
                i += 1
              }
            }
            j -= 1
          }
        
          // Split at negligible s(k).
        
        case 2 =>
          var f = e(k - 1)
          e(k - 1) = 0.0
          var j = k
          while (j < p) {
            var t = java.lang.Math.hypot(s(j), f)
            val cs = s(j) / t
            val sn = f / t
            s(j) = t
            f = -sn * e(j)
            e(j) = cs * e(j)
            if (wantu) {
              var i = 0
              while (i < m) {
                t = cs * u(i)(j) + sn * u(i)(k - 1)
                u(i)(k - 1) = -sn * u(i)(j) + cs * u(i)(k - 1)
                u(i)(j) = t
                i += 1
              }
            }
            j += 1
          }
        
          // Perform one qr step.
        
        case 3 =>
          
          // Calculate the shift.
          
          val scale = math.max(math.max(math.max(math.max(
                  math.abs(s(p - 1)), math.abs(s(p - 2))), math.abs(e(p - 2))),
              math.abs(s(k))), math.abs(e(k))
          )
          val sp = s(p - 1) / scale
          val spm1 = s(p - 2) / scale
          val epm1 = e(p - 2) / scale
          val sk = s(k) / scale
          val ek = e(k) / scale
          val b = ((spm1 + sp) * (spm1 - sp) + epm1 * epm1) / 2.0
          val c = sp * epm1 * sp * epm1
          var shift = 0.0
          if (b != 0.0 || c != 0.0) {
            shift = math.sqrt(b * b + c)
            if (b < 0.0) {
              shift = -shift
            }
            shift = c / (b + shift)
          }
          var f = (sk + sp) * (sk - sp) + shift
          var g = sk * ek
          
          // Chase zeros.
          
          var j = k
          while (j < p - 1) {
            var t = java.lang.Math.hypot(f, g)
            var cs = f / t
            var sn = g / t
            if (j != k) {
              e(j - 1) = t
            }
            f = cs * s(j) + sn * e(j)
            e(j) = cs * e(j) - sn * s(j)
            g = sn * s(j + 1)
            s(j + 1) = cs * s(j + 1)
            if (wantv) {
              var i = 0
              while (i < n) {
                t = cs * v(i)(j) + sn * v(i)(j + 1)
                v(i)(j + 1) = -sn * v(i)(j) + cs * v(i)(j + 1)
                v(i)(j) = t
                i += 1
              }
            }
            t = java.lang.Math.hypot(f, g)
            cs = f / t
            sn = g / t
            s(j) = t
            f = cs * e(j) + sn * s(j + 1)
            s(j + 1) = -sn * e(j) + cs * s(j + 1)
            g = sn * e(j + 1)
            e(j + 1) = cs * e(j + 1)
            if (wantu && j < m - 1) {
              var i = 0
              while (i < m) {
                t = cs * u(i)(j) + sn * u(i)(j + 1)
                u(i)(j + 1) = -sn * u(i)(j) + cs * u(i)(j + 1)
                u(i)(j) = t
                i += 1
              }
            }
            j += 1
          }
          e(p - 2) = f
          //iter += 1
        
          // Convergence.
        
        case 4 =>
          
          // Make the singular values positive.
          
          if (s(k) <= 0.0) {
            s(k) = if (s(k) < 0.0) -s(k) else 0.0
            if (wantv) {
              var i = 0
              while (i <= pp) {
                v(i)(k) = -v(i)(k)
                i += 1
              }
            }
          }
          
          // Order the singular values.
          var continue = true
          while (k < pp && continue) {
            if (s(k) >= s(k + 1)) {
              continue = false
            } else {
              var t = s(k)
              s(k) = s(k + 1)
              s(k + 1) = t
              if (wantv && k < n - 1) {
                var i = 0
                while (i < n) {
                  t = v(i)(k + 1)
                  v(i)(k + 1) = v(i)(k)
                  v(i)(k) = t
                  i += 1
                }
              }
              if (wantu && k < m - 1) {
                var i = 0
                while (i < m) {
                  t = u(i)(k + 1)
                  u(i)(k + 1) = u(i)(k)
                  u(i)(k) = t
                  i += 1
                }
              }
              k += 1
            }
          }
          //iter = 0
          p -= 1
        case _ =>
          throw new IllegalStateException()
      }
    }
  }
  
  /**
   * Returns the two norm condition number, which is <tt>max(S) / min(S)</tt>.
   */
  def cond: Double = {
    s(0) / s(math.min(m, n) - 1)
  }
  
  /**
   * @return the diagonal matrix of singular values.
   */
  def getS: Matrix = {
    val s = Array.ofDim[Double](n, n)
    var i = 0
    while (i < n) {
      var j = 0
      while (j < n) {
        s(i)(j) = 0.0
        j += 1
      }
      s(i)(i) = this.s(i)
      i += 1
    }
    
    DenseMatrix(s)
  }
  
  /**
   * Returns the diagonal of <tt>S</tt>, which is a one-dimensional array of
   * singular values
   * 
   * @return diagonal of <tt>S</tt>.
   */
  def getSingularValues: Array[Double] = {
    s
  }
  
  /**
   * Returns the left singular vectors <tt>U</tt>.
   * 
   * @return <tt>U</tt>
   */
  def getU: Matrix = {
    if (transpositionNeeded) { //case numRows() < numCols()
      DenseMatrix(v)
    } else {
      val numCols = math.min(m + 1, n)
      val r = DenseMatrix(m, numCols)
      var i = 0
      while (i < m) {
        var j = 0
        while (j < numCols) {
          r.set(i, j, u(i)(j))
          j += 1
        }
        i += 1
      }

      r
    }
  }
  
  /**
   * Returns the right singular vectors <tt>V</tt>.
   * 
   * @return <tt>V</tt>
   */
  def getV: Matrix = {
    if (transpositionNeeded) { // case numRows < numCols
      val numCols = math.min(m + 1, n)
      val r = DenseMatrix(m, numCols)
      var i = 0
      while (i < m) {
        var j = 0
        while (j < numCols) {
          r.set(i, j, u(i)(j))
          j += 1
        }
        i += 1
      }

      r
    } else {
      DenseMatrix(v)
    }
  }
  
  /** Returns the two norm, which is <tt>max(S)</tt>. */
  def norm2: Double = {
    s(0)
  }
  
  /**
   * Returns the effective numerical matrix rank, which is the number of
   * nonnegligible singular values.
   */
  def rank: Int = {
    val eps = math.pow(2.0, -52.0)
    val tol = math.max(m, n) * s(0) * eps
    var r = 0
    for (value <- s) {
      if (value > tol) {
        r += 1
      }
    }
    r
  }
  
  /**
   * @parameter minSingularValue
   * minSingularValue - value below which singular values are ignored (a 0 or negative
   * value implies all singular value will be used)
   * @return Returns the n × n covariance matrix.
   * The covariance matrix is V × J × Vt where J is the diagonal matrix of the inverse
   *  of the squares of the singular values.
   */
  def getCovariance(minSingularValue: Double): Matrix = {
    val j = DenseMatrix(s.length,s.length)
    val vMat = DenseMatrix(this.v)
    var i = 0
    while (i < s.length) {
      j.set(i, i, if (s(i) >= minSingularValue) 1 / (s(i) * s(i)) else 0.0)
      i += 1
    }
    vMat.times(j).times(vMat.transpose)
  }
  
  /**
   * Returns a String with (propertyName, propertyValue) pairs. Useful for
   * debugging or to quickly get the rough picture. For example,
   * 
   * <pre>
   * rank          : 3
   * trace         : 0
   * </pre>
   */
  override
  def toString = {
    val sb = new StringBuilder
    sb.append("---------------------------------------------------------------------\n")
    sb.append("SingularValueDecomposition(A) --> cond(A), rank(A), norm2(A), U, S, V\n")
    sb.append("---------------------------------------------------------------------\n")
    
    sb.append("cond = ")
    val unknown = "Illegal operation or error: "
    try {
      sb.append(String.valueOf(this.cond))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.append("\nrank = ")
    try {
      sb.append(String.valueOf(this.rank))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.append("\nnorm2 = ")
    try {
      sb.append(String.valueOf(this.norm2))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.append("\n\nU = ")
    try {
      sb.append(String.valueOf(this.getU))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.append("\n\nS = ")
    try {
      sb.append(String.valueOf(this.getS))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.append("\n\nV = ")
    try {
      sb.append(String.valueOf(this.getV))
    } catch {
      case e: IllegalArgumentException => sb.append(unknown).append(e.getMessage)
    }
    
    sb.toString
  }
}
