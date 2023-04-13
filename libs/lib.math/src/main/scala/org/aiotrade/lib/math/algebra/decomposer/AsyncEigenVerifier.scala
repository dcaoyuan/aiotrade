package org.aiotrade.lib.math.algebra.decomposer

import java.util.concurrent.Executors

import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.algebra.VectorIterable

class AsyncEigenVerifier extends SimpleEigenVerifier {

  private val threadPool = Executors.newFixedThreadPool(1)
  private var status = new EigenStatus(-1, 0)
  private var finished: Boolean = _
  private var started: Boolean = _

  override 
  def verify(corpus: VectorIterable, vector: Vector): EigenStatus = synchronized {
    if (!finished && !started) { // not yet started or finished, so start!
      status = new EigenStatus(-1, 0)
      val vectorCopy = vector.clone
      threadPool.execute(new VerifierRunnable(corpus, vectorCopy))
      started = true
    }
    if (finished) {
      finished = false
    }
    status
  }

  protected def innerVerify(corpus: VectorIterable, vector: Vector): EigenStatus = {
    super.verify(corpus, vector)
  }

  private class VerifierRunnable(corpus: VectorIterable, vector: Vector) extends Runnable {
    def run {
      val status = innerVerify(corpus, vector)
      AsyncEigenVerifier.this synchronized {
        AsyncEigenVerifier.this.status = status
        finished = true
        started = false
      }
    }
  }
}
