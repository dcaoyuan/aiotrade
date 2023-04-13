package org.aiotrade.lib.svm

import org.aiotrade.lib.math.vector.SparseVec
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.math.vector.VecItem
import org.aiotrade.lib.svm.Param
import scala.collection.mutable

/**
 * Kernel Function
 */
abstract class Kernel {
    
  def k(X: Array[Vec], i: Int, j: Int, param: Param): Double = 
    k(X(i), X(j), param)
  
  def k(xi: Array[VecItem], xj: Array[VecItem], param: Param): Double = 
    k(new SparseVec(xi), new SparseVec(xj), param)
  
  def k(xi: Vec, xj: Vec, param: Param): Double
}

/** 
 *
 * Although the xSquare could be precomputed and be transfered in, but in my 
 * experience, there is rare difference on performance between precomputed and
 * compute on demand.
 */
case object GaussianKernel extends Kernel {
    
  /** 
   * by multiple 0.5, the Gaussian Kernel is a normalized kernel:
   *
   *                 exp(<xi, xj> / sigma^2)
   *   -------------------------------------------------
   *   sqrt(exp(||xi||^2 / sigma^2) * exp(||xj||^2 / sigma^2))
   *
   *         <xi, xj>   <xi, xi>   <xj, xj>
   *   = exp(-------- - -------- - --------)
   *         sigma^2    2sigma^2   2sigma^2
   *
   *           ||xi - xj||^2
   *   = exp(- -------------)
   *             2sigma^2
   */       
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    val sigmaSquare = param.sigmaSquare
    
    math.exp(-0.5 * (xi.square + xj.square - 2 * xi.innerProduct(xj)) / sigmaSquare)
  }
}

case object LinearKernel extends Kernel {

  def k(xi: Vec, xj: Vec, param: Param): Double = {
    xi.innerProduct(xj)
  }
}

case object PolynomialKernel extends Kernel {
    
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    powi(param.gamma * xi.innerProduct(xj) + param.coef0, param.degree)
  }
    
  private def powi(base: Double, times: Int): Double = {
    var tmp = base
    var ret = 1.0
        
    var t = times
    while (t > 0) {
      if (t % 2 == 1) ret *= tmp
      tmp = tmp * tmp
      t /= 2
    }
        
    ret
  }
    
}

case object PreComputedKernel extends Kernel {
    
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    xi(xj(0).toInt)
  }
}

case object RBFKernal extends Kernel {
 
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    math.exp(-param.gamma * (xi.square + xj.square - 2 * xi.innerProduct(xj)))
  }
}

case object SigmoidKernel extends Kernel {

  /**
   * If |x| = 1, and |xi| = 1, then 
   * should have: coefConst > 0, coefLinear > 0, coefConst >= coefLinear
   */
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    math.tanh(param.gamma * xi.innerProduct(xj) - param.coef0)
  }
}

/**
 *
 * Although the xSquare could be precomputed and be transfered in, but in my
 * experience, there is rare difference on performance between precomputed and
 * compute on demand.
 */
case object WeightedSigmaGaussianKernel extends Kernel {
    
  /**
   * by multiple 0.5, the Gaussian Kernel is a normalized kernel:
   *
   *                 exp(<xi, xj> / sigma^2)
   *   -------------------------------------------------
   *   sqrt(exp(||xi||^2 / sigma^2) * exp(||xj||^2 / sigma^2))
   *
   *         <xi, xj>   <xi, xi>   <xj, xj>
   *   = exp(-------- - -------- - --------)
   *         sigma^2    2sigma^2   2sigma^2
   *
   *           ||xi - xj||^2
   *   = exp(- -------------)
   *             2sigma^2
   */
  def k(xi: Vec, xj: Vec, param: Param): Double = {
    val sigmaSquare = param.sigmaSquare
    val sigmaSquareWeights = param.sigmaSquareWeights

    var sum = 0.0
        
    if (xi.isInstanceOf[SparseVec] && xj.isInstanceOf[SparseVec]) {
      /** A quick algorithm in case of both are SparseVec */
            
      val datasA = xi.asInstanceOf[SparseVec].compactData
      val datasB = xj.asInstanceOf[SparseVec].compactData
      var idxA = 0
      var idxB = 0
      val lenA = datasA.length
      val lenB = datasB.length
      while (idxA < lenA && idxB < lenB) {
        val dataA = datasA(idxA)
        val dataB = datasB(idxB)
                
        if (dataA.index == dataB.index) {
          val idx = dataA.index
          val valueA = dataA.value
          val valueB = dataB.value
                    
          sum += (valueA * valueA + valueB * valueB - 2 * valueA * valueB) / (sigmaSquareWeights(idx) * sigmaSquare)
                    
          idxA += 1
          idxB += 1
        } else if (dataA.index > dataB.index) {
          idxB += 1
        } else {
          idxA += 1
        }
      }
            
    } else {
            
      /** for inner product, we only need compute with those value != 0 */
      var i = 0
      val n = xi.dimension
      while (i < n) {
        val valueA = xi(i)
        val valueB = xj(i)
                
        sum += (valueA * valueA + valueB * valueB - 2 * valueA * valueB) / (sigmaSquareWeights(i) * sigmaSquare)
        
        i += 1
      }
            
    }
        
    math.exp(-0.5 * sum)
  }
}
