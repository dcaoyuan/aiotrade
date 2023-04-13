package org.aiotrade.lib.math.algebra.decomposer

class EigenStatus(val eigenValue: Double, val cosAngle: Double, @volatile var inProgress: Boolean) {
  def this(eigenValue: Double, cosAngle: Double) = {
    this(eigenValue, cosAngle, true)
  }
}
