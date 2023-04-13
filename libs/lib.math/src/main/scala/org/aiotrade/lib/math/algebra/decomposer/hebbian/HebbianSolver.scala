package org.aiotrade.lib.math.algebra.decomposer.hebbian

import java.util.Properties

import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.Functions.PlusMult
import org.aiotrade.lib.math.Functions.TimesFunction
import org.aiotrade.lib.math.algebra.DenseMatrix
import org.aiotrade.lib.math.algebra.DenseVector
import org.aiotrade.lib.math.algebra.Matrix
import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.algebra.decomposer.AsyncEigenVerifier
import org.aiotrade.lib.math.algebra.decomposer.EigenStatus
import org.aiotrade.lib.math.algebra.decomposer.SingularVectorVerifier
import org.aiotrade.lib.math.random.RandomUtils

/**
 * The Hebbian solver is an iterative, sparse, singular value decomposition solver, based on the paper
 * <a href="http://www.dcs.shef.ac.uk/~genevieve/gorrell_webb.pdf">Generalized Hebbian Algorithm for
 * Latent Semantic Analysis</a> (2005) by Genevieve Gorrell and Brandyn Webb (a.k.a. Simon Funk).
 * TODO: more description here!  For now: read the inline comments, and the comments for the constructors.
 * 
 * @param updater
 *  {@link EigenUpdater} used to do the actual work of iteratively updating the current "best guess"
 *   singular vector one data-point presentation at a time.
 * @param verifier
 *  {@link SingularVectorVerifier } an object which perpetually tries to check how close to
 *   convergence the current singular vector is (typically is a
 *  {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } which does this
 *   in the background in another thread, while the main thread continues to converge)
 * @param convergenceTarget a small "epsilon" value which tells the solver how small you want the cosine of the
 *  angle between a proposed eigenvector and that same vector after being multiplied by the (square of the) input
 *  corpus
 * @param maxPassesPerEigen a cutoff which tells the solver after how many times of checking for convergence (done
 *  by the verifier) should the solver stop trying, even if it has not reached the convergenceTarget.
 */
class HebbianSolver(updater: EigenUpdater,
                    verifier: SingularVectorVerifier,
                    convergenceTarget: Double,
                    maxPassesPerEigen: Int) {

  private val log = Logger.getLogger(this.getClass.getName)
  private val DEBUG = false

  private final val rng = RandomUtils.getRandom

  private var numPasses = 0

  /**
   * Creates a new HebbianSolver
   *
   * @param updater
   *  {@link EigenUpdater} used to do the actual work of iteratively updating the current "best guess"
   *   singular vector one data-point presentation at a time.
   * @param verifier
   *  {@link SingularVectorVerifier } an object which perpetually tries to check how close to
   *   convergence the current singular vector is (typically is a
   *  {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } which does this
   *   in the background in another thread, while the main thread continues to converge)
   * @param convergenceTarget a small "epsilon" value which tells the solver how small you want the cosine of the
   *  angle between a proposed eigenvector and that same vector after being multiplied by the (square of the) input
   *  corpus
   * @param maxPassesPerEigen a cutoff which tells the solver after how many times of checking for convergence (done
   *  by the verifier) should the solver stop trying, even if it has not reached the convergenceTarget.
   */

  /**
   * Creates a new HebbianSolver with maxPassesPerEigen = Integer.MAX_VALUE (i.e. keep on iterating until
   * convergenceTarget is reached).  <b>Not recommended</b> unless only looking for
   * the first few (5, maybe 10?) singular
   * vectors, as small errors which compound early on quickly put a minimum error on subsequent vectors.
   *
   * @param updater {@link EigenUpdater} used to do the actual work of iteratively updating the current "best guess"
   *  singular vector one data-point presentation at a time.
   * @param verifier {@link org.apache.mahout.math.decomposer.SingularVectorVerifier }
   * an object which perpetually tries to check how close to
   *  convergence the current singular vector is (typically is a
   * {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } which does this
   *  in the background in another thread, while the main thread continues to converge)
   * @param convergenceTarget a small "epsilon" value which tells the solver how small you want the cosine of the
   *  angle between a proposed eigenvector and that same vector after being multiplied by the (square of the) input
   *  corpus
   */
  def this(updater: EigenUpdater, verifier: SingularVectorVerifier, convergenceTarget: Double) = {
    this(updater, verifier, convergenceTarget, java.lang.Integer.MAX_VALUE)
  }

  /**
   * <b>This is the recommended constructor to use if you're not sure</b>
   * Creates a new HebbianSolver with the default {@link HebbianUpdater } to do the updating work, and the default
   * {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } to check for convergence in a
   * (single) background thread.
   *
   * @param convergenceTarget a small "epsilon" value which tells the solver how small you want the cosine of the
   *  angle between a proposed eigenvector and that same vector after being multiplied by the (square of the) input
   *  corpus
   * @param maxPassesPerEigen a cutoff which tells the solver after how many times of checking for convergence (done
   *  by the verifier) should the solver stop trying, even if it has not reached the convergenceTarget.
   */
  def this(convergenceTarget: Double, maxPassesPerEigen: Int) = {
    this(new HebbianUpdater(), new AsyncEigenVerifier(), convergenceTarget, maxPassesPerEigen)
  }

  /**
   * Creates a new HebbianSolver with the default {@link HebbianUpdater } to do the updating work, and the default
   * {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } to check for convergence in a (single)
   * background thread, with
   * maxPassesPerEigen set to Integer.MAX_VALUE.  <b>Not recommended</b> unless only looking
   * for the first few (5, maybe 10?) singular
   * vectors, as small errors which compound early on quickly put a minimum error on subsequent vectors.
   *
   * @param convergenceTarget a small "epsilon" value which tells the solver how small you want the cosine of the
   *  angle between a proposed eigenvector and that same vector after being multiplied by the (square of the) input
   *  corpus
   */
  def this(convergenceTarget: Double) = {
    this(convergenceTarget, java.lang.Integer.MAX_VALUE)
  }

  /**
   * Creates a new HebbianSolver with the default {@link HebbianUpdater } to do the updating work, and the default
   * {@link org.apache.mahout.math.decomposer.AsyncEigenVerifier } to check for convergence in a (single)
   * background thread, with
   * convergenceTarget set to 0, which means that the solver will not really care about convergence as a loop-exiting
   * criterion (but will be checking for convergence anyways, so it will be logged and singular values will be
   * saved).
   *
   * @param numPassesPerEigen the exact number of times the verifier will check convergence status in the background
   *                          before the solver will move on to the next eigen-vector.
   */
  def this(numPassesPerEigen: Int) = {
    this(0.0, numPassesPerEigen)
  }

  /**
   * Primary singular vector solving method.
   *
   * @param corpus input matrix to find singular vectors of.  Needs not be symmetric, should probably be sparse (in
   *   fact the input vectors are not mutated, and accessed only via dot-products and sums, so they should be
   *   {@link org.apache.mahout.math.SequentialAccessSparseVector }
   * @param desiredRank the number of singular vectors to find (in roughly decreasing order by singular value)
   * @return the final {@link TrainingState } of the solver, after desiredRank singular vectors (and approximate
   *         singular values) have been found.
   */
  def solve(corpus: Matrix, desiredRank: Int): TrainingState = {
    val cols = corpus.numCols
    val eigens = DenseMatrix(desiredRank, cols)
    val eigenValues = new ArrayList[Double]()
    log.info("Finding {} singular vectors of matrix with {} rows, via Hebbian".format(desiredRank, corpus.numRows))
    /*
     * The corpusProjections matrix is a running cache of the residual projection of each corpus vector against all
     * of the previously found singular vectors.  Without this, if multiple passes over the data is made (per
     * singular vector), recalculating these projections eventually dominates the computational complexity of the
     * solver.
     */
    val corpusProjections = DenseMatrix(corpus.numRows, desiredRank)
    val state = new TrainingState(eigens, corpusProjections)
    var i = 0
    while (i < desiredRank) {
      val currentEigen = DenseVector(cols)
      var previousEigen: Vector = null
      while (hasNotConverged(currentEigen, corpus, state)) {
        val randomStartingIndex = getRandomStartingIndex(corpus, eigens)
        val initialTrainingVector = corpus.viewRow(randomStartingIndex)
        state.trainingIndex = randomStartingIndex
        updater.update(currentEigen, initialTrainingVector, state)
        var corpusRow = 0
        while (corpusRow < corpus.numRows) {
          state.trainingIndex = corpusRow
          if (corpusRow != randomStartingIndex) {
            updater.update(currentEigen, corpus.viewRow(corpusRow), state)
          }
          corpusRow += 1
        }
        state.isFirstPass = false
        if (DEBUG) {
          if (previousEigen == null) {
            previousEigen = currentEigen.clone
          } else {
            var dot = currentEigen.dot(previousEigen)
            if (dot > 0.0) {
              dot /= currentEigen.norm(2) * previousEigen.norm(2)
            }
            // log.info("Current pass * previous pass = {}", dot)
          }
        }
        i += 1
      }
      // converged!
      val eigenValue = state.statusProgress(state.statusProgress.size - 1).eigenValue
      // it's actually more efficient to do this to normalize than to call currentEigen = currentEigen.normalize(),
      // because the latter does a clone, which isn't necessary here.
      currentEigen.assign(new TimesFunction(), 1 / currentEigen.norm(2))
      eigens.assignRow(i, currentEigen)
      eigenValues += eigenValue
      state.currentEigenValues = eigenValues
      log.info("Found eigenvector {}, eigenvalue: {}".format(i, eigenValue))

      /**
       *  TODO: Persist intermediate output!
       */
      state.isFirstPass = true
      state.numEigensProcessed = state.numEigensProcessed + 1
      state.activationDenominatorSquared = 0
      state.activationNumerator = 0
      state.statusProgress.clear
      numPasses = 0
    }
    state
  }

  /**
   * You have to start somewhere...
   * TODO: start instead wherever you find a vector with maximum residual length after subtracting off the projection
   * TODO: onto all previous eigenvectors.
   *
   * @param corpus the corpus matrix
   * @param eigens not currently used, but should be (see above TODO)
   * @return the index into the corpus where the "starting seed" input vector lies.
   */
  private def getRandomStartingIndex(corpus: Matrix, eigens: Matrix): Int = {
    var index = 0
    var v: Vector = null
    do {
      val r = rng.nextDouble
      index = (r * corpus.numRows).toInt
      v = corpus.viewRow(index)
    } while (v == null || v.norm(2) == 0 || v.getNumNondefaultElements < 5)
    index
  }

  /**
   * Uses the {@link SingularVectorVerifier } to check for convergence
   *
   * @param currentPseudoEigen the purported singular vector whose convergence is being checked
   * @param corpus             the corpus to check against
   * @param state              contains the previous eigens, various other solving state {@link TrainingState}
   * @return true if <em>either</em> we have converged, <em>or</em> maxPassesPerEigen has been exceeded.
   */
  protected def hasNotConverged(currentPseudoEigen: Vector, corpus: Matrix, state: TrainingState): Boolean = {
    numPasses += 1
    if (state.isFirstPass) {
      log.info("First pass through the corpus, no need to check convergence...")
      return true
    }
    val previousEigens = state.currentEigens
    log.info("Have made {} passes through the corpus, checking convergence...".format(numPasses))
    /*
     * Step 1: orthogonalize currentPseudoEigen by subtracting off eigen(i) * helper.get(i)
     * Step 2: zero-out the helper vector because it has already helped.
     */
    var i = 0
    while (i < state.numEigensProcessed) {
      val previousEigen = previousEigens.viewRow(i)
      currentPseudoEigen.assign(previousEigen, new PlusMult(-state.helperVector.get(i)))
      state.helperVector.set(i, 0)
      i += 1
    }
    if (DEBUG && currentPseudoEigen.norm(2) > 0) {
      var i = 0
      while (i < state.numEigensProcessed) {
        val previousEigen = previousEigens.viewRow(i)
        log.info("dot with previous: {}".format(previousEigen.dot(currentPseudoEigen) / currentPseudoEigen.norm(2)))
        i += 1
      }
    }
    /*
     * Step 3: verify how eigen-like the prospective eigen is.  This is potentially asynchronous.
     */
    val status = verify(corpus, currentPseudoEigen)
    if (status.inProgress) {
      log.info("Verifier not finished, making another pass...")
    } else {
      log.info("Has 1 - cosAngle: {}, convergence target is: {}".format(1.0 - status.cosAngle, convergenceTarget))
      state.statusProgress += status
    }
    state.statusProgress.size <= maxPassesPerEigen && 1.0 - status.cosAngle > convergenceTarget
  }

  protected def verify(corpus: Matrix, currentPseudoEigen: Vector): EigenStatus = {
    verifier.verify(corpus, currentPseudoEigen)
  }
  
}

object HebbianSolver {
  private val log = Logger.getLogger(this.getClass.getName)

  def main(args: Array[String]) {
    val props = new Properties()
    val propertiesFile = if (args.length > 0) args(0) else "config/solver.properties"
    //  props.load(new FileInputStream(propertiesFile))

    val corpusDir = props.getProperty("solver.input.dir")
    val outputDir = props.getProperty("solver.output.dir")
    if (corpusDir == null || corpusDir.isEmpty || outputDir == null || outputDir.isEmpty) {
      log.severe("{} must contain values for solver.input.dir and solver.output.dir".format(propertiesFile))
      return
    }
    //Int inBufferSize = Integer.parseInt(props.getProperty("solver.input.bufferSize"))
    val rank = props.getProperty("solver.output.desiredRank").toInt
    val convergence = props.getProperty("solver.convergence").toDouble
    val maxPasses = props.getProperty("solver.maxPasses").toInt
    //Int numThreads = Integer.parseInt(props.getProperty("solver.verifier.numThreads"))

    val updater = new HebbianUpdater()
    val verifier = new AsyncEigenVerifier()
    val solver = new HebbianSolver(updater, verifier, convergence, maxPasses)
    var corpus: Matrix = null
    /*
     if (numThreads <= 1) {
     //  corpus = new DiskBufferedDoubleMatrix(new File(corpusDir), inBufferSize)
     } else {
     //  corpus = new ParallelMultiplyingDiskBufferedDoubleMatrix(new File(corpusDir), inBufferSize, numThreads)
     }
     */
    val now = System.currentTimeMillis
    val finalState = solver.solve(corpus, rank)
    val time = (System.currentTimeMillis - now) / 1000
    log.info("Solved {} eigenVectors in {} seconds.  Persisted to {}".format(finalState.currentEigens.rowSize, time, outputDir))
  }
}
