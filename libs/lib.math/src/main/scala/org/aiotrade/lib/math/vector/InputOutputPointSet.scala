/*
 * Copyright (c) 2006-2013, AIOTrade Computing Co. and Contributors
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
import scala.reflect.ClassTag

/**
 * @author Caoyuan Deng
 */
class InputOutputPointSet[T <: InputOutputPoint: ClassTag] protected (val inputOutputPoints: Array[T]) {

  private val inputDimension = inputOutputPoints(0).input.dimension
  private var inputMeans = Array.ofDim[Double](inputDimension)
  private var inputStdDeviations = Array.ofDim[Double](inputDimension)
  private var inputNormalized = Array.ofDim[Boolean](inputDimension)

  private val outputDimension = inputOutputPoints(0).output.dimension
  private var outputMeans = Array.ofDim[Double](outputDimension)
  private var outputStdDeviations = Array.ofDim[Double](outputDimension)
  private var outputNormalized = Array.ofDim[Boolean](outputDimension)
    
  def apply(idx: Int): T = {
    inputOutputPoints(idx)
  }
    
  def update(idx: Int, iop: T) {
    inputOutputPoints(idx) = iop
  }
    
  def size: Int = {
    inputOutputPoints.length
  }
    
  def randomizeOrder {
    val random = new Random(System.currentTimeMillis)

    val n = size
    var i = 0
    while (i < n) {
      val next = random.nextInt(n - i)
            
      val iop = inputOutputPoints(next)
            
      inputOutputPoints(next) = inputOutputPoints(i)
      inputOutputPoints(i) = iop
      
      i += 1
    }
  }
    
  def cloneWithRandomizedOrder: InputOutputPointSet[T] = {
    val size1 = size
        
    val newPoints = Array.ofDim[T](size1)
    System.arraycopy(inputOutputPoints, 0, newPoints, 0, size1)
        
    val newSet = new InputOutputPointSet(newPoints)
        
    newSet.inputMeans = Array.ofDim[Double](inputMeans.length)
    System.arraycopy(inputMeans, 0, newSet.inputMeans, 0, inputMeans.length)
        
    newSet.inputStdDeviations = Array.ofDim[Double](inputStdDeviations.length)
    System.arraycopy(inputStdDeviations, 0, newSet.inputStdDeviations, 0, inputStdDeviations.length)
        
    newSet.outputMeans = Array.ofDim[Double](outputMeans.length)
    System.arraycopy(outputMeans, 0, newSet.outputMeans, 0, outputMeans.length)
        
    newSet.outputStdDeviations = Array.ofDim[Double](outputStdDeviations.length)
    System.arraycopy(outputStdDeviations, 0, newSet.outputStdDeviations, 0, outputStdDeviations.length)
        
    newSet.randomizeOrder
        
    newSet
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   */
  def normalizeInputs(dimensionIdx: Int) {
    val n = inputOutputPoints.length
        
    val values = Array.ofDim[Double](n)
    var i = 0
    while (i < n) {
      values(i) = inputOutputPoints(i).input(dimensionIdx)
      i += 1
    }
        
    val normalized = normalize_ZScore(values)
    
    i = 0
    while (i < n) {
      inputOutputPoints(i).input(dimensionIdx) = normalized(i)
      i += 1
    }
        
    inputMeans(dimensionIdx) = normalized(n)
    inputStdDeviations(dimensionIdx) = normalized(n + 1)
    inputNormalized(dimensionIdx) = true
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   *
   * @NOTICE
   * If the output layer uses linear neurons as y = x, the y will be 0 symmetry.
   * the output can be < 0 in same probabilty as > 0, so we should also normalize
   * outputs to [-1, 1] instead of positively?
   *
   * 1. If the hidden neurons' outputs are positive-polarity (such as: LogiSigmoidNeuron)
   * when the mean of initial weights is about 0, the output will be around 0.5,
   * so, we'd better to normalize the outputs to [0, 1], or, with 0.5 mean and 0.5 stdDeviation
   *
   * 2. If the hidden neurons' outputs are double-polarity (such as: TanhSigmoidNeuron)
   * when the mean of initial weights is about 0, the output will be around 0,
   * so, we'd better to normalize the outputs to [-1, 1], or, with 0 mean and 1 stdDeviation
   *
   * Experience: If normalize ouput to [-1, 1], will cause a slower convergence.
   */
  def normalizeOutputs(dimensionIdx: Int) {
    val n = inputOutputPoints.length
        
    val values = Array.ofDim[Double](n)
    var i = 0
    while (i < n) {
      values(i) = inputOutputPoints(i).output(dimensionIdx)
      i += 1
    }
        
    val normalized = normalize_ZScore(values)
    
    i = 0
    while (i < n) {
      inputOutputPoints(i).output(dimensionIdx) = normalized(i)
      i += 1
    }
        
    val mu = normalized(n)
    val sigma = normalized(n + 1)
    outputMeans(dimensionIdx) = normalized(n)
    outputStdDeviations(dimensionIdx) = normalized(n + 1)
    outputNormalized(dimensionIdx) = true
  }
    
  /**
   * Normalize values to:
   *   mean: 0.5
   *   standard deviation: 0.5
   *   range: about [0, 1]
   */
  def normalizeOutputsPositively(dimensionIdx: Int) {
    val n = inputOutputPoints.length
        
    val values = Array.ofDim[Double](n)
    var i = 0
    while (i < n) {
      values(i) = inputOutputPoints(i).output(dimensionIdx)
      i += 1
    }
        
    val normalized = normalize_ZScore(values)
    
    i = 0
    while (i < n) {
      /** transform to mean: 0.5, standar deviation: 0.5 */
      inputOutputPoints(i).output(dimensionIdx) = normalized(i) * 0.5 + 0.5
      i += 1
    }
        
    /**
     * When doing normalize_ZScore(),
     *   y = (x - mu) / sigma
     * Here, we again,
     *   v = y * 0.5 + 0.5
     * So,
     *   v = ((x - mu) / sigma) * 0.5 + 0.5
     *     = ((x - mu) + 0.5 * sigma / 0.5) / (sigma / 0.5)
     *     = (x - (mu - sigma)) / (sigma / 0.5)
     *     = (x - mu') / sigma'
     * where
     *   mu' = mu - sigma
     *   sigma' = sigma / 0.5
     */
    val mu = normalized(n)
    val sigma = normalized(n + 1)
    outputMeans(dimensionIdx) = mu - sigma
    outputStdDeviations(dimensionIdx) = sigma / 0.5
    outputNormalized(dimensionIdx) = true
  }
    
  def normalizeAllInputs {
    val n = inputOutputPoints(0).input.dimension
    var i = 0
    while (i < n) {
      normalizeInputs(i)
      i += 1
    }
  }
    
  def normalizeAllOutputs {
    val n = inputOutputPoints(0).output.dimension
    var i = 0
    while (i < n) {
      normalizeOutputs(i)
      i += 1
    }
  }
    
  def normalizePositivelyAllOutputs {
    val n = inputOutputPoints(0).output.dimension
    var i = 0
    while (i < n) {
      normalizeOutputsPositively(i)
      i += 1
    }
  }
    
  def normalizeInput(input: Vec) {
    val n = input.dimension
    var i = 0
    while (i < n) {
      val value = input(i)
      input(i) = normalizeInput(i, value)
      i += 1
    }
  }
    
  def normalizeOutput(output: Vec) {
    val n = output.dimension
    var i = 0
    while (i < n) {
      val value = output(i)
      output(i) = normalizeOutput(i, value)
      i += 1
    }
  }
    
  def normalizePositivelyOutput(output: Vec) {
    /** as we have considered the mean and stdDeviation in positive case, it's same as: */
    normalizeOutput(output)
  }
    
  def  normalizeInput(dimensionIdx: Int, value: Double): Double = {
    if (inputNormalized(dimensionIdx)) {
      (value - inputMeans(dimensionIdx)) / inputStdDeviations(dimensionIdx)
    } else {
      /** the mean and stdDeviation of this dimensionIdx are not computed yet */
      value
    }
  }
    
  def normalizeOutput(dimensionIdx: Int, value: Double): Double = {
    if (outputNormalized(dimensionIdx)) {
      (value - outputMeans(dimensionIdx)) / outputStdDeviations(dimensionIdx)
    } else {
      /** the mean and stdDeviation of this dimensionIdx are not computed yet */
      value
    }
  }
    
  def normalizePositivelyOutput(dimensionIdx: Int, value: Double): Double = {
    /** as we have considered the mean and stdDeviation in positive case, it's same as: */
    normalizeOutput(dimensionIdx, value)
  }
    
  def reinstateInput(input: Vec) {
    val n = input.dimension
    var i = 0
    while (i < n) {
      val value = input(i) * inputStdDeviations(i) + inputMeans(i)
      input(i) = value
      i += 1
    }
  }
    
  def reinstateOutput(output: Vec) {
    val n = output.dimension
    var i = 0
    while (i < n) {
      val value = output(i) * outputStdDeviations(i) + outputMeans(i)
      output(i) = value
      i += 1
    }
  }
    
    
  def reinstateInput(dimensionIdx: Int, value: Double): Double = {
    value * inputStdDeviations(dimensionIdx) + inputMeans(dimensionIdx)
  }
    
    
  def reinstateOutput(dimensionIdx: Int, value: Double): Double = {
    value * outputStdDeviations(dimensionIdx) + outputMeans(dimensionIdx)
  }
    
  /**
   * Normalize values to:
   *   mean: 0
   *   standard deviation: 1
   *   range: about [-1, 1]
   */
  private def normalize_ZScore(values: Array[Double]): Array[Double] = {
    val n = values.length
        
    /** compute mean value */
    var sum = 0.0
    var i = 0
    while (i < n) {
      sum += values(i)
      i += 1
    }
    val mean = sum / (n * 1d)
        
    /** compute standard deviation */
    var deviation_square_sum = 0d
    i = 0
    while (i < n) {
      val deviation = values(i) - mean
      deviation_square_sum += deviation * deviation
      i += 1
    }
        
    var stdDeviation = math.sqrt(deviation_square_sum / (n * 1d))
        
    //("Mean: " + mean + " Standard Deviation: " + stdDeviation)
        
    if (stdDeviation == 0) {
      stdDeviation = 1
    }
        
    /**
     * do 'Z Score' normalization.
     * 2 more dimensions are added to store mean and stdDeviation
     */
    val normalized = Array.ofDim[Double](n + 2)
    i = 0
    while (i < n) {
      normalized(i) = (values(i) - mean) / stdDeviation
      i += 1
    }
        
    normalized(n) = mean
    normalized(n + 1) = stdDeviation
        
    normalized
  }
    
  /**
   * y = (0.9 - 0.1) / (xmax - xmin) * x + (0.9 - (0.9 - 0.1) / (xmax - xmin) * xmax)
   *   = 0.8 / (xmax - xmin) * x + (0.9 - 0.8 / (xmax - xmin) * xmax)
   */
  private def normalizePositively_MinMax(values: Array[Double]): Array[Double] = {
    val n = values.length
        
    /** compute min max value */
    var min = Double.MaxValue
    var max = Double.MinValue
    var i = 0
    while (i < n) {
      val value = values(i)
      if (value < min) {
        min = value
      }
      if (value > max) {
        max = value
      }
      i += 1
    }
        
    val mean = min
        
    val stdDeviation = max - min
        
    //println("normOutput: " + mean + " deviationOutput: " + stdDeviation)
        
    /** do 'min max' normalization */
    val normalized = Array.ofDim[Double](n + 2)
    i = 0
    while (i < n) {
      normalized(i) = (values(i) - mean) / stdDeviation
      i += 1
    }
        
    normalized(n) = mean
    normalized(n + 1) = stdDeviation
        
    normalized
  }
    
  private def normalizePositively_CustomMinMax(values: Array[Double]): Array[Double] = {
    val n = values.length
        
    /** compute max min value */
    val max = 30000
    val min = 0
        
    val mean = min
        
    val stdDeviation = max - min
        
    /** do 'maxmin' standardization */
    val normalized = Array.ofDim[Double](n + 2)
    var i = 0
    while (i < n) {
      normalized(i) = (values(i) - mean) / stdDeviation
      i += 1
    }
        
    normalized(n) = mean
    normalized(n + 1) = stdDeviation
        
    normalized
  }
}

object InputOutputPointSet {
  def apply[T <: InputOutputPoint: ClassTag](inputOutputPoints: Array[T]) = new InputOutputPointSet(inputOutputPoints)
}