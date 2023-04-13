package org.aiotrade.lib.math.algebra.decomposer.hebbian

import org.aiotrade.lib.math.algebra.Vector


trait EigenUpdater {
  def update(pseudoEigen: Vector, trainingVector: Vector, currentState: TrainingState)
}
