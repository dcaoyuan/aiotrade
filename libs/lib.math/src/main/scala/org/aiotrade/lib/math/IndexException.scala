package org.aiotrade.lib.math

/**
 * Exception thrown when a matrix or vector is accessed at an index, or dimension,
 * which does not logically exist in the entity.
 */
class IndexException(index: Int, cardinality: Int) extends IllegalArgumentException("Index " + index + " is outside allowable range of [0," + cardinality + ')')
