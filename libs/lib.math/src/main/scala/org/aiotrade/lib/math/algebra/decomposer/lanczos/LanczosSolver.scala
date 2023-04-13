package org.aiotrade.lib.math.algebra.decomposer.lanczos


import org.aiotrade.lib.math.algebra.EigenvalueDecomposition
import org.aiotrade.lib.math.algebra.Vector
import java.util.logging.Logger
import org.aiotrade.lib.math.Functions.PlusMult
import scala.collection.mutable

/**
 * <p>Simple implementation of the <a href="http://en.wikipedia.org/wiki/Lanczos_algorithm">Lanczos algorithm</a> for
 * finding eigenvalues of a symmetric matrix, applied to non-symmetric matrices by applying Matrix.timesSquared(vector)
 * as the "matrix-multiplication" method.</p>
 * <p>
 * To avoid floating point overflow problems which arise in power-methods like Lanczos, an initial pass is made
 * through the input matrix to
 * <ul>
 *   <li>generate a good starting seed vector by summing all the rows of the input matrix, and</li>
 *   <li>compute the trace(inputMatrix<sup>t</sup>*matrix)
 * </ul>
 * </p>
 * <p>
 * This latter value, being the sum of all of the singular values, is used to rescale the entire matrix, effectively
 * forcing the largest singular value to be strictly less than one, and transforming floating point <em>overflow</em>
 * problems into floating point <em>underflow</em> (ie, very small singular values will become invisible, as they
 * will appear to be zero and the algorithm will terminate).
 * </p>
 * <p>This implementation uses {@link org.apache.mahout.math.matrix.linalg.EigenvalueDecomposition} to do the
 * eigenvalue extraction from the small (desiredRank x desiredRank) tridiagonal matrix.  Numerical stability is
 * achieved via brute-force: re-orthogonalization against all previous eigenvectors is computed after every pass.
 * This can be made smarter if (when!) this proves to be a major bottleneck.  Of course, this step can be parallelized
 * as well.
 * </p>
 */
class LanczosSolver {
  import LanczosSolver._
  
  private val log = Logger.getLogger(this.getClass.getName)

  trait TimingSection
  object TimingSection {
    case object ITERATE extends TimingSection
    case object ORTHOGANLIZE extends TimingSection
    case object TRIDIAG_DECOMP extends TimingSection
    case object FINAL_EIGEN_CREATE extends TimingSection
  }

  private val startTimes = new mutable.HashMap[TimingSection, Long]()
  private val times = new mutable.HashMap[TimingSection, Long]()

  def solve(state: LanczosState, desiredRank: Int) {
    solve(state, desiredRank, false)
  }

  def solve(state: LanczosState, desiredRank: Int, isSymmetric: Boolean) {
    val t0 = System.currentTimeMillis
    val corpus = state.corpus
    log.info("Finding %s singular vectors of matrix with %s rows, via Lanczos".format(desiredRank, corpus.numRows))
    var i = state.iterationNumber
    var currentVector = state.getBasisVector(i - 1).getOrElse(null)
    var previousVector = state.getBasisVector(i - 2).getOrElse(null)
    var beta = 0.0
    val triDiag = state.diagonalMatrix
    var continue = true
    while (i < desiredRank && continue) {
      startTime(TimingSection.ITERATE)
      val nextVector = if (isSymmetric) corpus.times(currentVector) else corpus.timesSquared(currentVector)
      log.fine("%s passes through the corpus so far...".format(i))
      if (state.scaleFactor <= 0) {
        state.scaleFactor = calculateScaleFactor(nextVector)
      }
      nextVector.assign(new Scale(1.0 / state.scaleFactor))
      if (previousVector != null) {
        nextVector.assign(previousVector, new PlusMult(-beta))
      }
      // now orthogonalize
      val alpha = currentVector.dot(nextVector)
      nextVector.assign(currentVector, new PlusMult(-alpha))
      endTime(TimingSection.ITERATE);
      startTime(TimingSection.ORTHOGANLIZE)
      orthoganalizeAgainstAllButLast(nextVector, state)
      endTime(TimingSection.ORTHOGANLIZE)
      // and normalize
      beta = nextVector.norm(2)
      if (outOfRange(beta) || outOfRange(alpha)) {
        log.warning("Lanczos parameters out of range: alpha = %s, beta = %s.  Bailing out early!".format(alpha, beta))
        continue = false
      } else {
        nextVector.assign(new Scale(1 / beta))
        state.setBasisVector(i, nextVector)
        previousVector = currentVector
        currentVector = nextVector
        // save the projections and norms!
        triDiag.set(i - 1, i - 1, alpha)
        if (i < desiredRank - 1) {
          triDiag.set(i - 1, i, beta)
          triDiag.set(i, i - 1, beta)
        }
        i += 1
        state.iterationNumber = i
      }
    }
    startTime(TimingSection.TRIDIAG_DECOMP)

    log.info("Lanczos iteration complete - now to diagonalize the tri-diagonal auxiliary matrix.")
    // at this point, have tridiag all filled out, and basis is all filled out, and orthonormalized
    val decomp = new EigenvalueDecomposition(triDiag)

    val eigenVects = decomp.getV
    val eigenVals = decomp.getRealEigenvalues
    endTime(TimingSection.TRIDIAG_DECOMP)
    startTime(TimingSection.FINAL_EIGEN_CREATE)
    var row = 0
    while (row < i) {
      var realEigen: Vector = null
      // the eigenvectors live as columns of V, in reverse order.  Weird but true.
      val ejCol = eigenVects.viewColumn(i - row - 1)
      val size = math.min(ejCol.size, state.getBasisSize)
      var j = 0
      while (j < size) {
        val d = ejCol.get(j)
        val rowJ = state.getBasisVector(j).getOrElse(null)
        if(realEigen == null) {
          realEigen = rowJ.like
        }
        realEigen.assign(rowJ, new PlusMult(d))
        j += 1
      }
      realEigen = realEigen.normalize
      state.setRightSingularVector(row, realEigen)
      var e = eigenVals.get(row) * state.scaleFactor
      if(!isSymmetric) {
        e = math.sqrt(e)
      }
      log.fine("Eigenvector %s found with eigenvalue %s".format(row, e))
      state.setSingularValue(row, e)
      row += 1
    }
    log.info("LanczosSolver finished in %ss.".format((System.currentTimeMillis - t0) / 1000))
    endTime(TimingSection.FINAL_EIGEN_CREATE)
  }

  protected def calculateScaleFactor(nextVector: Vector) = {
    nextVector.norm(2)
  }

  protected def orthoganalizeAgainstAllButLast(nextVector: Vector, state: LanczosState) {
    var i = 0
    while (i < state.iterationNumber) {
      val basisVector = state.getBasisVector(i).getOrElse(null)
      var alpha = 0.0
      if (basisVector == null || {alpha = nextVector.dot(basisVector); alpha == 0.0}) {
        // continue
      } else {
        nextVector.assign(basisVector, new PlusMult(-alpha))
      }
      i += 1
    }
  }

  private def startTime(section: TimingSection) {
    startTimes += (section -> System.nanoTime)
  }

  private def endTime(section: TimingSection) {
    if (!times.contains(section)) {
      times += (section -> 0L)
    }
    times += (section -> (times(section) + System.nanoTime - startTimes(section)))
  }

}

object LanczosSolver {
  val SAFE_MAX = 1.0e150
  
  private def outOfRange(d: Double): Boolean = {
    java.lang.Double.isNaN(d) || d > SAFE_MAX || -d > SAFE_MAX
  }

  private final class Scale(d: Double) extends Function1[Double, Double] {
    def apply(arg1: Double): Double = arg1 * d
  }
}
