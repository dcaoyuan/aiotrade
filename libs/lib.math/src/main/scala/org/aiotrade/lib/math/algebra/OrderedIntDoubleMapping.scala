package org.aiotrade.lib.math.algebra

@serializable @cloneable
class OrderedIntDoubleMapping private (var indices: Array[Int], var values: Array[Double], var numMappings: Int) {

  private def this(capacity: Int) = this(new Array[Int](capacity), new Array[Double](capacity), 0)

  // no-arg constructor for deserializer
  def this() = this(11)
  
  private def growTo(newCapacity: Int) {
    if (newCapacity > indices.length) {
      val newIndices = new Array[Int](newCapacity)
      System.arraycopy(indices, 0, newIndices, 0, numMappings)
      indices = newIndices
      val newValues = new Array[Double](newCapacity)
      System.arraycopy(values, 0, newValues, 0, numMappings)
      values = newValues
    }
  }

  private def find(index: Int): Int = {
    var low = 0
    var high = numMappings - 1
    while (low <= high) {
      val mid = low + ((high - low) >>> 1)
      val midVal = indices(mid)
      if (midVal < index) {
        low = mid + 1
      } else if (midVal > index) {
        high = mid - 1
      } else {
        return mid
      }
    }
    -(low + 1)
  }

  def get(index: Int): Double = {
    val offset = find(index)
    if (offset >= 0) values(offset) else OrderedIntDoubleMapping.DEFAULT_VALUE
  }

  def set(index: Int, value: Double) {
    val offset = find(index)
    if (offset >= 0) {
      if (value == OrderedIntDoubleMapping.DEFAULT_VALUE) {
        var i = offset + 1; var j = offset
        while (i < numMappings) {
          indices(j) = indices(i)
          values(j) = values(i)
          i += 1; j += 1
        }
        numMappings -=1
      } else {
        values(offset) = value
      }
    } else {
      if (value != OrderedIntDoubleMapping.DEFAULT_VALUE) {
        if (numMappings >= indices.length) {
          growTo(math.max((1.2 * numMappings).toInt, numMappings + 1))
        }
        val at = -offset - 1
        if (numMappings > at) {
          var i = numMappings - 1; var j = numMappings
          while (i >= at) {
            indices(j) = indices(i)
            values(j) = values(i)
            i -= 1; j -= 1
          }
        }
        indices(at) = index
        values(at) = value
        numMappings += 1
      }
    }
  }

  override
  def hashCode: Int = {
    var result = 0
    var i = 0
    while (i < numMappings) {
      result = 31 * result + indices(i)
      result = 31 * result + java.lang.Double.doubleToRawLongBits(values(i)).toInt
      i += 1
    }
    result
  }

  override
  def equals(o: Any): Boolean = o match {
    case that: OrderedIntDoubleMapping =>
      if (numMappings == that.numMappings) {
        var i = 0
        while (i < numMappings) {
          if (indices(i) != that.indices(i) || values(i) != that.values(i)) {
            return false
          }
          i += 1
        }
        true
      } else {
        false
      }
    case _ => false
  }

  override
  def toString: String = {
    val result = new StringBuilder(10 * numMappings)
    var i = 0
    while (i < numMappings) {
      result.append('(')
      result.append(indices(i))
      result.append(',')
      result.append(values(i))
      result.append(')')
      i += 1
    }
    result.toString
  }

  override
  def clone: OrderedIntDoubleMapping = {
    OrderedIntDoubleMapping(indices.clone, values.clone, numMappings)
  }

}

object OrderedIntDoubleMapping {
  val DEFAULT_VALUE = 0.0

  def apply(capacity: Int) = new OrderedIntDoubleMapping(new Array[Int](capacity), new Array[Double](capacity), 0)
  
  def apply(indices: Array[Int], values: Array[Double], numMappings: Int) = new OrderedIntDoubleMapping(indices, values, numMappings)
}
