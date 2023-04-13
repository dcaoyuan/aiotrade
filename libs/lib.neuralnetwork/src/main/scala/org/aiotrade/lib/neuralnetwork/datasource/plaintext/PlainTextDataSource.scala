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

package org.aiotrade.lib.neuralnetwork.datasource.plaintext

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.util.StringTokenizer
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.Vec
import org.aiotrade.lib.neuralnetwork.datasource.DataSource

/**
 * 
 * @author Caoyuan Deng 
 */
class PlainTextDataSource extends DataSource {
    
  private val ELEMENT_SEPARATOR_SPACE = " "
    
  private var _dataFileName: String = _
  private var _resultsFileName: String = _
  private var _overwriteResults = true
  private var _elementSeparator = ELEMENT_SEPARATOR_SPACE
  private var _inputDimension: Int = _
  private var _outputDimension: Int = _
  private var _numberOfSystemRuns: Int = _
  private var _trainingToGeneralizingRatio: Double = _
  private var generalizingDataset: Array[Array[InputOutputPoint]] = _
  private var trainingDataset: Array[Array[InputOutputPoint]] = _
    
  def isOverwriteResults = _overwriteResults    
  def isOverwriteResults_=(overwriteResults: Boolean) {
    _overwriteResults = overwriteResults
  }
 
  def resultsFileName = _resultsFileName
  def resultsFileName_=(resultsFileName: String) {
    _resultsFileName = resultsFileName
  }

  def dataFileName = _dataFileName
  def dataFileName_=(dataFileName: String) {
    _dataFileName = dataFileName
  }
    
  def elementSeparator = _elementSeparator
  def elementSeparator_=(elementSeparator: String) {
    _elementSeparator = elementSeparator
  }
    
  override
  def numNetworks = _numberOfSystemRuns 
  override
  def numNetworks_=(numberOfSystemRuns: Int) {
    _numberOfSystemRuns = numberOfSystemRuns
  }
  
  def trainingToGeneralizingRatio = _trainingToGeneralizingRatio    
  def trainingToGeneralizingRatio_=(trainingToGeneralizingRatio: Double) {
    _trainingToGeneralizingRatio = trainingToGeneralizingRatio
  }
    
  override 
  def inputDimension = _inputDimension    
  override 
  def inputDimension_=(inputDimension: Int) {
    _inputDimension = inputDimension
  }
  
  override 
  def outputDimension = _outputDimension
  override 
  def outputDimension_=(outputDimension: Int) {
    _outputDimension = outputDimension
  }

  @throws(classOf[Exception])
  def getValidatingInputs(runNumber: Int): Array[Vec] = {
    if ((trainingDataset == null) || (generalizingDataset == null)) {
      init
    }
    val atest = Array.ofDim[Vec](generalizingDataset(runNumber).length)
        
    var i = 0
    while (i < generalizingDataset(runNumber).length) {
      atest(i) = generalizingDataset(runNumber)(i).input
      i += 1
    }
        
    atest
  }
    
  @throws(classOf[Exception])
  def getTrainingPoints(runNumber: Int ): Array[InputOutputPoint] = {
    if ((trainingDataset == null) || (generalizingDataset == null)) {
      init
    }
        
    trainingDataset(runNumber)
  }
    
  @throws(classOf[Exception])
  def checkValidation() {
    if (_inputDimension <= 0) {
      throw new Exception("Input size must be greater than zero.")
    }
    if (_outputDimension <= 0) {
      throw new Exception("Input size must be greater than zero.")
    }
        
    if (_overwriteResults) {
      if ((new File(resultsFileName)).exists) {
        throw new Exception(
          "Results file already exists and configuration requieres not to overwrite it.")
      }
    } else {
      if (!(new File(resultsFileName)).canWrite) {
        throw new Exception("Results file can not be written.")
      }
    }
        
    if (!(new File(dataFileName)).exists) {
      throw new Exception("Data file not found.")
    }
        
    if (!(new File(dataFileName)).canRead) {
      throw new Exception("Data file can not be read.")
    }
  }
    
  /**
   * Writes the results to a an ascii file.
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
    val set = loadInputOutputFile(dataFileName)
        
    val trainSetSize = math.round(trainingToGeneralizingRatio * set.length).toInt
        
    trainingDataset = Array.ofDim[InputOutputPoint](numNetworks, trainSetSize)
    generalizingDataset = Array.ofDim[InputOutputPoint](numNetworks, set.length - trainSetSize)
        
    var i = 0
    while (i < numNetworks) {
      val aset = InputOutputPoint.randomizeOrder_createNew(set)
      System.arraycopy(aset, 0, trainingDataset(i), 0, trainSetSize)
      System.arraycopy(aset, trainSetSize + 1, generalizingDataset(i), 0, aset.length - trainSetSize)
      
      i += 1
    }
  }
    
  @throws(classOf[Exception])
  protected def loadInputOutputFile(fileName: String): Array[InputOutputPoint] = {
    try {
      val in = new BufferedReader(new FileReader(fileName))
      val holder = new ArrayList[InputOutputPoint]()
      var break = false
      while (in.ready && !break) {
        val str = in.readLine.trim
                
        if (str.length() == 0) {
          break = true
        } else {
          val st = new StringTokenizer(str, _elementSeparator)
                
          val patternSize = st.countTokens
          if (patternSize != _inputDimension + _outputDimension) {
            throw new Exception("The data file is not properly formated.")
          }
          val iop = InputOutputPoint(_inputDimension, _outputDimension)
                
          var i = 0
          while (i < _inputDimension) {
            iop.input(i) = st.nextToken.toDouble
            i += 1
          }
                
          i = 0
          while (i < _outputDimension) {
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
    
  def createSampleInstance: DataSource = {
    val res = new PlainTextDataSource()
    res.dataFileName = "<the name of the data source file>"
    res.resultsFileName = "<name of the file where the results will be written>"
    res.elementSeparator = " "
    res.isOverwriteResults = true
    res.trainingToGeneralizingRatio = 0.8
    res.numNetworks = 5
    res
  }
}