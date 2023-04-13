package org.aiotrade.lib.math.algebra


import org.aiotrade.lib.collection.ArrayList

object OrthonormalityVerifier {

  def pairwiseInnerProducts(basis: Iterable[MatrixSlice]): VectorIterable = {
    var out: DenseMatrix = null
    for (slice1 <- basis) {
      val dots = new ArrayList[Double]()
      for (slice2 <- basis) {
        dots += (slice1.vector.dot(slice2.vector))
      }
      if (out == null) {
        out = DenseMatrix(dots.size, dots.size)
      }
      var i = 0
      while (i < dots.size) {
        out.set(slice1.index, i, dots(i))
        i += 1
      }
    }
    out
  }

}
