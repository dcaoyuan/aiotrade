package org.aiotrade.lib.math.algebra.decomposer.lanczos

import org.aiotrade.lib.math.algebra.DenseMatrix
import org.aiotrade.lib.math.algebra.Matrix
import org.aiotrade.lib.math.algebra.Vector
import org.aiotrade.lib.math.algebra.VectorIterable

import scala.collection.mutable

class LanczosState(val corpus: VectorIterable, desiredRank: Int, initialVector: Vector) {

  val diagonalMatrix: Matrix = DenseMatrix(desiredRank, desiredRank)
  var scaleFactor = 0.0
  var iterationNumber = 1
  protected val basis = new mutable.HashMap[Int, Vector]()
  protected val singularValues = new mutable.HashMap[Int, Double]()
  protected val singularVectors = new mutable.HashMap[Int, Vector]()
  setBasisVector(0, initialVector)

  def getRightSingularVector(i: Int ): Option[Vector] = {
    singularVectors.get(i)
  }

  def getSingularValue(i: Int): Option[Double] = {
    singularValues.get(i)
  }

  def getBasisVector(i: Int): Option[Vector] = {
    basis.get(i)
  }

  def getBasisSize: Int = {
    basis.size
  }

  def setBasisVector(i: Int, basisVector: Vector) {
    basis.put(i, basisVector)
  }

  def setRightSingularVector(i: Int, vector: Vector) {
    singularVectors.put(i, vector)
  }

  def setSingularValue(i: Int , value: Double) {
    singularValues.put(i, value)
  }
}
