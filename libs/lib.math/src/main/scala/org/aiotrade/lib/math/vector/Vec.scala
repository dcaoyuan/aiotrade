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

/**
 * A vector of real numbers.
 *
 * @author Caoyuan Deng
 */
trait Vec extends Cloneable {
  /**
   * The elements of this <code>Vec</code> as an <code>Array[double]</code>.
   *
   * @return the <code>Array[double]</code>
   */
  def values: Array[Double]
  def values_=(values: Array[Double])
    
  /**
   * Element-wise addition.
   *
   * @param vec   the <code>Vec</code> to plus
   * @return the result of the operation
   */
  def plus(operand: Vec): Vec
    
  /**
   * Scalar addition.
   *
   * @param operand   the amount to plus
   * @return the result of the sum
   */
  def plus(operand: Double): Vec
    
  def minus(operand: Vec): Vec
    
  /**
   * Appends an element to the <code>Vec</code>.
   * @Notice
   * that is an inefficient operation and should be rarely used.
   *
   *
   * @param value   the value of the element to add
   */
  def add(value: Double)
    
  /**
   * Compute an Euclidean metric (or distance) from this <code>Vec</code> to
   * another.
   *
   * @param other   the <code>Vec</code> to measure the metric (or distance) with
   * @return the metric
   */
  def metric(other: Vec): Double
    
  /**
   * Compute the inner product of two <code>Vec</code>s.
   *
   * @param operand   the other <code>Vec</code>
   * @return the inner product
   */
  def innerProduct(operand: Vec): Double
    
  /**
   * <Xi dot Xi> the inner product of this vec itself
   */
  def square: Double
    
  /**
   * Scalar multipication.
   *
   * @param operand   the amount to times
   * @return the resulting <code>Vec</code>
   */
  def times(operand: Double): Vec
    
  /**
   * Compute a 1-norm (sum of absolute values) of the <code>Vec</code>.
   * norm (or length)
   *
   * @return the norm
   */
  def normOne: Double
    
  /**
   * Compute a 2-norm (square root of the sum of the squared values) of the
   * <code>Vec</code>.
   *
   *
   * @return the norm
   */
  def normTwo: Double
    
  /**
   * Returns the <i>idx </i>-nary element of the <code>Vec</code>.
   *
   * @param idx   the index of the desired element
   * @return the value of the element
   */
  def apply(idx: Int): Double
    
  /**
   * Sets element of index <code>i</code> to <code>value</code>.
   *
   * @param idx   index of the element to set
   * @param value    the value to set
   */
  def update(idx: Int, value: Double)
    
  /**
   * Sets all <code>Vec</code> elements to <code>value</code>.
   *
   * @param value   the value to set
   */
  def setAll(value: Double)
    
  /**
   * Sets elements to the ones of <code>orig</code>.
   *
   * @param orig   the <code>Vec</code> with the elements to set
   */
  def copy(orig: Vec)
    
  def copy(src: Vec, srcPos: Int, destPos: Int, length: Int)
    
  /**
   * @return the dimension of this <code>Vec</code>
   */
  def dimension :Int
    
    
  /**
   * Randomizes this <code>Vec</code> with values bounded by
   * <code>min</code> and <code>max</code>.
   *
   * @param min   lower bound
   * @param max   upper bound
   */
  def randomize(min: Double, max: Double)
    
  /**
   * Checks if a <code>Vec</code> has equal dimension of this <code>Vec</code>.
   *
   * @param comp   <code>Vec</code> to test with
   */
  def checkDimensionEquality(comp: Vec)
    
  def checkValidation: Boolean

  override 
  def clone: Vec = super.clone.asInstanceOf[Vec]
}
