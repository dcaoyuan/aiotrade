package org.aiotrade.lib.math.algebra

import org.aiotrade.lib.math.CardinalityException

object Algebra {
  def mult(m: Matrix, v: Vector): Vector = {
    if (m.numRows != v.size) {
      throw new CardinalityException(m.numRows, v.size)
    }
    // Use a Dense Vector for the moment,
    val result = DenseVector(m.numRows)
    
    var i = 0
    while (i < m.numRows) {
      result.set(i, m.viewRow(i).dot(v))
      i += 1
    }
    
    result
  }
}
