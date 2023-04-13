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

package org.aiotrade.lib.neuralnetwork.datasource

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.Date
import java.util.StringTokenizer
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TSer
import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.Vec

/**
 *
 * @author Caoyuan Deng
 */
class DefaultDataSource(_nNetworks: Int, _inputDimension: Int, _outputDimension: Int
) extends DataSource(_nNetworks, _inputDimension, _outputDimension) {
    
  private var series: TSer = _
  private var endDate: Date = _
    
  private var _resultsFileName: String = _
  private var _overwriteResults = true
  private var _elementSeparator = " "
    
  private var _validatingSize: Int = _
  private var _trainingSize: Int = _
  
  private var normOutput: Double = _
  private var deviationOutput: Double = _
    
  protected var validatingPoints: Array[Array[InputOutputPoint]] = _
  protected var trainingPoints:   Array[Array[InputOutputPoint]] = _
    
  def isOverwriteResults = _overwriteResults
  def isOverwriteResults_=(overwriteResults: Boolean) {
    _overwriteResults = overwriteResults
  }
    
  def resultsFileName = _resultsFileName
  def resultsFileName_=(resultsFileName: String) {
    _resultsFileName = resultsFileName
  }
    
  def elementSeparator = _elementSeparator
  def elementSeparator_=(elementSeparator: String) {
    _elementSeparator = elementSeparator
  }
    
  @throws(classOf[Exception])
  def getValidatingInputs(networkIdx: Int): Array[Vec] = {
    if ((trainingPoints == null) || (validatingPoints == null)) {
      init
    }
    val atest = Array.ofDim[Vec](validatingPoints(networkIdx).length)
        
    var i = 0
    while (i < validatingPoints(networkIdx).length) {
      atest(i) = validatingPoints(networkIdx)(i).input
      i += 1
    }
        
    atest
  }
    
  @throws(classOf[Exception])
  def getTrainingPoints(networkIdx: Int): Array[InputOutputPoint] = {
    if ((trainingPoints == null) || (validatingPoints == null)) {
      init
    }
        
    trainingPoints(networkIdx)
  }
    
  def validatingSize = _validatingSize    
  def validatingSize_=(validatingSize: Int) {
    _validatingSize = validatingSize
  }
    
  def trainingSize = _trainingSize    
  def trainingSize_=(trainingSize: Int) {
    _trainingSize = trainingSize
  }
    
  @throws(classOf[Exception])
  def checkValidation() {
    if (inputDimension <= 0) {
      throw new Exception("Input size must be greater than zero.");
    }
    if (outputDimension <= 0) {
      throw new Exception("Input size must be greater than zero.");
    }
        
    if (_overwriteResults) {
      if ((new File(resultsFileName)).exists) {
        throw new Exception(
          "Results file already exists and configuration requieres not to overwrite it.");
      }
    } else {
      if (!(new File(resultsFileName)).canWrite) {
        throw new Exception("Results file can not be written.")
      }
    }
        
  }
    
  /**
   * Writes the results to a an ascii file.
   *
   * @see PlainTextEnvironment#getElementSeparator()
   */
  @throws(classOf[Exception])
  def writeResults(results: Array[Vec], runNumber: Int) {
    try {
      val bw = new BufferedWriter(new FileWriter(resultsFileName))
            
      var i = 0
      while (i < results.length) {
        val sb = new StringBuffer()
                
        var j = 0
        while (j < results(i).dimension) {
          if (j != 0) { // if its not the first, add an element
            // separator space
            sb.append(elementSeparator)
          }
          sb.append(results(i)(j))
          j += 1
        }
                
        bw.write(sb.toString)
        bw.newLine
        i += 1
      }
            
      bw.close
    } catch {
      case ex: IOException  => throw ex
    }
        
  }
    
  @throws(classOf[Exception])
  protected def init() {
    if (series == null) {
      throw new Exception("series not set")
    }
        
    val iops = loadInputOutputDataset_fromEndDate
    val trainSetSize = trainingSize
        
    trainingPoints = Array.ofDim[InputOutputPoint](numNetworks, trainSetSize)
    validatingPoints = Array.ofDim[InputOutputPoint](numNetworks, iops.length - trainSetSize)
        
    var i = 0
    while (i < numNetworks) {
      val aset = InputOutputPoint.randomizeOrder_createNew(iops)
      System.arraycopy(aset, 0, trainingPoints(i), 0, trainSetSize)
      //System.arraycopy(aset, trainSetSize + 1, validatingPoints(i), 0, aset.length - trainSetSize);
      i += 1
    }
  }
    
  @throws(classOf[Exception])
  protected def loadInputOutputDataset_fromEndDate(): Array[InputOutputPoint] = {
    /** @TODO */
    null
  }
    
  @throws(classOf[Exception])
  protected def loadInputOutputFile(fileName: String): Array[InputOutputPoint] = {
    try {
      val in = new BufferedReader(new FileReader(fileName))
      val holder = new ArrayList[InputOutputPoint]()
      
      var break = false
      while (in.ready && !break) {
        val str = in.readLine.trim
        if (str.length == 0) {
          break = true
        } else {
          val st = new StringTokenizer(str, _elementSeparator)
          val vecSize = st.countTokens
          if (vecSize != inputDimension + outputDimension) {
            throw new Exception("The data file is not properly formated.")
          }
          val iop = InputOutputPoint(inputDimension, outputDimension)
                
          var i = 0
          while (i < inputDimension) {
            iop.input(i) = st.nextToken.toDouble
            i += 1
          }
                
          i = 0
          while (i < outputDimension) {
            iop.output(i) = st.nextToken.toDouble
            i += 1
          }
                
          holder += iop
        
        }
      }
      in.close
      
      holder.toArray
    } catch {
      case ex: FileNotFoundException => throw ex
      case ex: IOException => throw ex
    }
  }
    
  def createSampleInstance(): DataSource = {
    val instance = new DefaultDataSource(1, 200, 10)
        
    instance.resultsFileName = "<name of the file where the results will be written>"
    instance.elementSeparator = " "
    instance.isOverwriteResults = true
    instance.numNetworks = 5
        
    instance
  }
    
  final def mapToFeature(value: Double): Double = {
    value
    //return Math.sqrt(value)
    //return Math.log(value)
  }
    
  final def mapToOrigin(value: Double): Double = {
    value
    //return value * value
    //return Math.pow(Math.E, value)
  }
    
  final def reinstate(normalizedOutput: Double): Double = {
    mapToOrigin(normalizedOutput * deviationOutput + normOutput)
  }
    
  private def normalize(outputs: Array[Double]): Array[Double] = {
    normalize_MinMax(outputs)
  }
    
  private def normalize_ZScore(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** do scaling */
    var i = 0
    while (i < num) {
      values(i) = mapToFeature(values(i))
      i += 1
    }
        
    /** compute mean value */
    var sum = 0.0
    i = 0
    while (i < num) {
      sum += values(i)
      i += 1
    }
    normOutput = sum / num // mean value
        
    /** compute standard deviation */
    var deviation_square_sum = 0.0
    i = 0
    while (i < num) {
      val deviation = values(i) - normOutput
      deviation_square_sum += deviation * deviation
      i += 1
    }
    deviationOutput = Math.sqrt(deviation_square_sum / num) // standard deviation
        
    println("normOutput: " + normOutput + " deviationOutput: " + deviationOutput)
        
    /** do 'Z Score' normalization */
    val result = Array.ofDim[Double](values.length)
    i = 0
    while (i < num) {
      result(i) = (values(i) - normOutput) / deviationOutput
      i += 1
    }
        
    result
  }
    
  /**
   * y = (0.9 - 0.1) / (xmax - xmin) * x + (0.9 - (0.9 - 0.1) / (xmax - xmin) * xmax)
   *   = 0.8 / (xmax - xmin) * x + (0.9 - 0.8 / (xmax - xmin) * xmax)
   */
  private def normalize_MinMax(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** do scaling */
    var i = 0
    while (i < num) {
      values(i) = mapToFeature(values(i))
      i += 1
    }
        
    /** compute min max value */
    var min = Double.MaxValue
    var max = Double.MinValue
    i = 0
    while (i < num) {
      val value = values(i)
      if (value < min) {
        min = value
      }
      if (value > max) {
        max = value
      }
      i += 1
    }
        
    normOutput = min
    deviationOutput = max - min
        
    println("normOutput: " + normOutput + " deviationOutput: " + deviationOutput)
        
    /** do 'min max' normalization */
    val result = Array.ofDim[Double](values.length)
    i = 0
    while (i < num) {
      result(i) = (values(i) - normOutput) / deviationOutput
      i += 1
    }
        
    result
  }
    
  private def normalize_CustomMinMax(values: Array[Double]): Array[Double] = {
    val num = values.length
        
    /** do scaling */
    var i = 0
    while (i < num) {
      values(i) = mapToFeature(values(i))
      i += 1
    }
        
    /** compute max min value */
    val max = 30000
    val min = 0
        
    normOutput = min
    deviationOutput = max - min
        
    println("normOutput: " + normOutput + " deviationOutput: " + deviationOutput)
        
    /** do 'maxmin' standardization */
    val result = Array.ofDim[Double](values.length)
    i = 0
    while (i < num) {
      result(i) = (values(i) - normOutput) / deviationOutput
      i += 1
    }
        
    result
  }
}

