/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.math.vector

import java.util.Random
import java.util.StringTokenizer
import org.aiotrade.lib.math.timeseries.Null

/**
 * Default implement of Vec.
 *
 * @author Caoyuan Deng
 */
class DefaultVec(underlying: Array[Double]) extends Vec {
  import DefaultVec._
    
  private var _values: Array[Double] = underlying
    
  /**
   * Create a zero values <code>DefaultVec</code>.
   */
  def this() = {
    this(new Array[Double](0))
  }
    
  /**
   * Create a <code>DefaultVec</code> of the desired dimension and initialized to zero.
   *
   * @param dimension   the dimension of the new <code>DefaultVec</code>
   */
  def this(dimension: Int) = {
    this(new Array[Double](dimension))
  }
    
  /**
   * Create a <code>DefaultVec</code> whose values are copied from
   * <code>source</code>.
   *
   * @param source   the <code>DefaultVec</code> to be used as source
   */
  def this(source: Vec) {
    this(source.values)
  }
    
  def add(value: Double) {
    val size = if (_values == null) 0 else _values.length
        
    val newValues = new Array[Double](size + 1)
        
    if (size > 0) {
      System.arraycopy(_values, 0, newValues, 0, size)
    }
    newValues(newValues.length - 1) = value
        
    _values = newValues
  }
    
  def values = _values
  def values_=(values: Array[Double]) {
    _values = values
  }
    
  def checkDimensionEquality(comp: Vec) {
    if (comp.dimension != this.dimension) {
      throw new ArrayIndexOutOfBoundsException(
        "Doing operations with DefaultVec instances of different sizes. this.dimension=%s, that.dimension=%s".format(this.dimension, comp.dimension)
      )
    }
  }

  override 
  def clone: DefaultVec = {
    new DefaultVec(this)
  }
    
  def metric(other: Vec): Double = {
    minus(other).normTwo
  }
    
  def equals(other: Vec): Boolean = {
    if (dimension != other.dimension) {
      return false
    }

    var i = 0
    while (i < dimension) {
      if (apply(i) != other(i)) {
        return false
      }
      i += 1
    }
        
    true
  }
    
  def apply(dimensionIdx: Int): Double = {
    _values(dimensionIdx)
  }
    
  def update(dimensionIdx: Int, value: Double) {
    _values(dimensionIdx) = value
  }
    
  def setAll(value: Double) {
    val n = _values.length
    var i = 0
    while (i < n) {
      _values(i) = value
      i += 1
    }
  }
    
  def copy(src: Vec) {
    checkDimensionEquality(src)
    System.arraycopy(src.values, 0, _values, 0, _values.length)
  }
    
  def copy(src: Vec, srcPos: Int, destPos: Int, length: Int) {
    System.arraycopy(src.values, srcPos, _values, destPos, length)
  }
    
  def dimension: Int = {
    _values.length
  }
    
  def plus(operand: Vec): Vec = {
    checkDimensionEquality(operand)

    val result = new DefaultVec(dimension)
    val n = dimension
    var i = 0
    while (i < n) {
      result(i) =  apply(i) + operand(i)
      i += 1
    }
    
    result
  }
    
  def plus(operand: Double): Vec = {
    val result = new DefaultVec(dimension)
    val n = dimension
    var i = 0
    while (i < n) {
      result(i) = apply(i) + operand
      i += 1
    }
    
    result
  }
    
  def minus(operand: Vec): Vec = {
    checkDimensionEquality(operand)
        
    val result = new DefaultVec(dimension)
    val n = dimension
    var i = 0
    while (i < n) {
      result(i) = apply(i) - operand(i)
      i += 1
    }
    
    result
  }
    
  def innerProduct(operand: Vec): Double = {
    checkDimensionEquality(operand)
        
    var result = 0.0
    val n = dimension
    var i = 0
    while (i < n) {
      result += apply(i) * operand(i)
      i += 1
    }
    
    result
  }
    
  def square: Double = {
    var result = 0.0
    val n = dimension
    var i = 0
    while (i < n) {
      val value = apply(i)
      result += value * value
      i += 1
    }
    
    result
  }
    
  def times(operand: Double): Vec = {
    val result = new DefaultVec(dimension)
    val n = dimension
    var i = 0
    while (i < n) {
      result(i) = apply(i) * operand
      i += 1
    }
    
    result
  }
    
  def normOne: Double = {
    var result = 0.0
    val n = dimension
    var i = 0
    while (i < n) {
      result += math.abs(apply(i))
      i += 1
    }
    
    result
  }
    
  def normTwo: Double = {
    var result = 0.0
    val n = dimension
    var i = 0
    while (i < n) {
      result += math.pow(apply(i), 2.0)
      i += 1
    }
    
    math.sqrt(result)
  }
    
  def checkValidation: Boolean = {
    val n = dimension
    var i = 0
    while (i < n) {
      if (Null.is(_values(i))) {
        return false
      }
      i += 1
    }

    true
  }

  def randomize(min: Double, max: Double) {
    val source = new Random(System.currentTimeMillis + Runtime.getRuntime.freeMemory)

    val n = dimension
    var i = 0
    while (i < n) {
      /**
       * @NOTICE
       * source.nextDouble() returns a pseudorandom value between 0.0 and 1.0
       */
      update(i, source.nextDouble * (max - min) + min)
      i += 1
    }
  }

  override 
  def toString: String = {
    val result = new StringBuilder
    result.append("[")
    val n = dimension
    var i = 0
    while (i < n) {
      result.append(apply(i)).append(ITEM_SEPARATOR)
      i += 1
    }
    result.append("]")
    
    result.toString
  }
}

object DefaultVec {
  def apply(underlying: Array[Double]) = new DefaultVec(underlying)
  def apply(source: Vec) = new DefaultVec(source)
  
  val ITEM_SEPARATOR = " "
    
  /**
   * Parses a String into a <code>DefaultVec</code>.
   * Elements are separated by <code>DefaultVec.ITEM_SEPARATOR</code>
   *
   * @param str   the String to parse
   * @return the resulting <code>DefaultVec</code>
   * @see DefaultVec#ITEM_SEPARATOR
   */
  def parseVec(str: String): Vec = {
    val st = new StringTokenizer(str, ITEM_SEPARATOR)

    val dimension = st.countTokens
    val result = new DefaultVec(dimension)
    val n = dimension
    var i = 0
    while (i < n) {
      result(i) = st.nextToken.toDouble
      i += 1
    }
    
    result
  }

}
