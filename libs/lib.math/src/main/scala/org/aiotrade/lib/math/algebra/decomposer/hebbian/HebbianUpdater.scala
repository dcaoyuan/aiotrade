package org.aiotrade.lib.math.algebra.decomposer.hebbian


import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.Functions.PlusMult

class HebbianUpdater extends EigenUpdater {

  def update(pseudoEigen: Vector, trainingVector: Vector, currentState: TrainingState) {
    val trainingVectorNorm = trainingVector.norm(2)
    val numPreviousEigens = currentState.numEigensProcessed
    if (numPreviousEigens > 0) {
      if (currentState.isFirstPass) {
        updateTrainingProjectionsVector(currentState, trainingVector, numPreviousEigens - 1)
      }
    }
    if (currentState.activationDenominatorSquared == 0 || trainingVectorNorm == 0) {
      if (currentState.activationDenominatorSquared == 0) {
        pseudoEigen.assign(trainingVector, new PlusMult(1))
        currentState.helperVector = (currentState.currentTrainingProjection.clone)
        val helperNorm = currentState.helperVector.norm(2)
        currentState.activationDenominatorSquared = trainingVectorNorm * trainingVectorNorm - helperNorm * helperNorm
      }
      return
    }
    currentState.activationNumerator = pseudoEigen.dot(trainingVector)
    currentState.activationNumerator = currentState.activationNumerator - currentState.helperVector.dot(currentState.currentTrainingProjection)

    val activation = currentState.activationNumerator / math.sqrt(currentState.activationDenominatorSquared)
    currentState.activationDenominatorSquared = (
      currentState.activationDenominatorSquared
      + 2 * activation * currentState.activationNumerator
      + activation * activation * (trainingVector.getLengthSquared - currentState.currentTrainingProjection.getLengthSquared))
    if (numPreviousEigens > 0) {
      currentState.helperVector.assign(currentState.currentTrainingProjection, new PlusMult(activation))
    }
    pseudoEigen.assign(trainingVector, new PlusMult(activation))
  }

  private def updateTrainingProjectionsVector(state: TrainingState,
                                              trainingVector: Vector,
                                              previousEigenIndex: Int) {
    val previousEigen = state.mostRecentEigen
    val currentTrainingVectorProjection = state.currentTrainingProjection
    val projection = previousEigen.dot(trainingVector)
    currentTrainingVectorProjection.set(previousEigenIndex, projection)
  }

}
