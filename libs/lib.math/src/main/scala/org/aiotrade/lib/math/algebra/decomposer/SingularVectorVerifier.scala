package org.aiotrade.lib.math.algebra.decomposer

import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.algebra.VectorIterable

trait SingularVectorVerifier {
  def verify(eigenMatrix: VectorIterable, vector: Vector): EigenStatus
}
