package org.aiotrade.lib.svm

import libsvm.svm_parameter

sealed abstract class SvmType(val ordinal: Int)
case object C_SVC       extends SvmType(0)
case object Mu_SVC      extends SvmType(1)
case object One_Class   extends SvmType(2)
case object Epsilon_SVR extends SvmType(3)
case object Nu_SVR      extends SvmType(4)

class Param extends svm_parameter {
  private var _svmType: SvmType = _
  def svmType = _svmType
  def svmType_=(svmType: SvmType) {
    _svmType = svmType
    svm_type = svmType.ordinal
  }
  
  private var _kernel: Kernel = _
  def kernel = _kernel
  def kernel_=(kernel: Kernel) {
    _kernel = kernel
    kernel_type = kernel match {
      case LinearKernel => 0
      case PolynomialKernel => 1
      case RBFKernal => 2
      case SigmoidKernel => 3
      case PreComputedKernel => 4
      case GaussianKernel => -1 // @todo
      case WeightedSigmaGaussianKernel => -1 // @todo
    }
  }
  
  val coefLinear = gamma  // for poly/rbf/sigmoid.
  val coefConst = coef0   // for poly/sigmoid
  val epsilonESVR = p     // for Epsilon_SVR
    
  var sigmaSquare: Double = _               // for Gaussion, coefLinear = 0.5 / sigmaSquare
  var sigmaSquareWeights: Array[Double] = _ // for WeightedSigmaGaussianKernel
  
  override
  def clone(): Param = {
    try {
      super.clone.asInstanceOf[Param]
    } catch {
      case ex: CloneNotSupportedException => null
    }
  }
    
  def processParamForNormalization(inputDimension: Int): Param = {
    val newParam = this.clone
        
    /** default value is 1.0 */
    if (gamma == 0) {
      newParam.gamma = 1.0
    }
    newParam.gamma /= inputDimension
        
    /** default value is 0.5 */
    if (sigmaSquare == 0) {
      newParam.sigmaSquare = 0.5
    }
    newParam.sigmaSquare *= inputDimension
        
    newParam
  }
}
