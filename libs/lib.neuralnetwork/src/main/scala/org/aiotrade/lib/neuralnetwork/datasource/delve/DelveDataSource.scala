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

package org.aiotrade.lib.neuralnetwork.datasource.delve


import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
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
class DelveDataSource extends DataSource {
  import DelveDataSource._
  
  class ProblemTrainFilenameFilter extends java.io.FilenameFilter {
    def accept(dir: File, name: String) = name.startsWith("train")
  }
    
  private var _inputDimension: Int = _
  private var _outputDimension: Int = _
    
  private var parsed = false
    
  private var _problemDirectory: String = _
  private var _resultsDirectory: String = _
    
  override
  def numNetworks: Int = {
    (new File(_problemDirectory)).list(new ProblemTrainFilenameFilter()).length;
  }
    
  override
  def inputDimension: Int = {
    if (parsed) {
      _inputDimension
    } else {
      throw new DelveNotInitializedException()
    }
  }
    
  override
  def outputDimension: Int = {
    if (parsed) {
      _outputDimension
    } else {
      throw new DelveNotInitializedException()
    }
  }
    
  def problemDirectory = _problemDirectory
  def problemDirectory_=(problemDirectory: String) {
    _problemDirectory = problemDirectory
  }
    
  def resultsDirectory = _resultsDirectory
  def resultsDirectory_=(resultsDirectory: String) {
    _resultsDirectory = resultsDirectory
  }
        
  /**
   * Reads a test data file from a DELVE directory. The test set is a
   * input-only file.
   * 
   * @param runNumber
   *            the number of the current system run
   * @return Test set
   * @throws DataSourceException
   */
  @throws(classOf[Exception])
  def getValidatingInputs(runNumber: Int): Array[Vec] = {
    val trueRunNumber = (runNumber + 1) % numNetworks
        
    val iop = loadInputOutputFile(problemDirectory + File.separator + 
                                  "test." + String.valueOf(trueRunNumber), 0)
        
    val ret = Array.ofDim[Vec](iop.length)
        
    var i = 0
    while (i < ret.length) {
      ret(i) = iop(i).input
      i += 1
    }
    
    ret
  }
    
  /**
   * Reads a train file from a DELVE directory.
   * 
   * @param runNumber
   *            the number of the current system run
   * @return the training set as an array of <code>InputOutputPoint</code>
   * @see yawn.envs.DataSource#getTrainingDataset(int)
   * @throws DataSourceException
   */
  @throws(classOf[Exception])
  def getTrainingPoints(runNumber: Int): Array[InputOutputPoint] = {
    val trueRunNumber = (runNumber + 1) % numNetworks
        
    loadInputOutputFile(problemDirectory + File.separator + "train."
                        + String.valueOf(trueRunNumber), _outputDimension)
  }
    
  @throws(classOf[Exception])
  protected def init() {
    val iops = loadInputOutputFile(TRAIN_FILE_NAME, 0)
    val spoi = loadInputOutputFile(TEST_FILE_NAME, 0)
    _inputDimension  = spoi(0).input.dimension
    _outputDimension = iops(0).input.dimension - _inputDimension
    parsed = true
  }
    
  /**
   * Reads a file in DELVE format. The last <code>numberOfOutpus</code>
   * columns are selected as outputs.
   * 
   * @return an array of <code>InputOutputPoint</code> loaded from the
   *         file.
   * @throws DataSourceException
   */
  @throws(classOf[Exception])
  protected def loadInputOutputFile(fileName: String, numberOfOutputs: Int): Array[InputOutputPoint] = {
    try {
      val in = new BufferedReader(new FileReader(fileName))
      val holder = new ArrayList[InputOutputPoint]()
      var break = false
      while (in.ready && !break) {
        val str = in.readLine.trim
        if (str.length == 0) {
          break = true
        } else {
          val st = new StringTokenizer(str)
          val patternSize = st.countTokens
          val iop = InputOutputPoint(patternSize - numberOfOutputs, numberOfOutputs)
          var i = 0
          while (i < patternSize - numberOfOutputs) {
            iop.input(i) = st.nextToken.toDouble
            i += 1
          }
          i = 0
          while (i < numberOfOutputs) {
            iop.output(i) = st.nextToken.toDouble
            i += 1
          }
          holder += iop
        }
      }
      in.close
      
      return holder.toArray
    } catch {
      case ex: IOException => throw ex
    }
  }
    
  override
  def numNetworks_=(numberOfRuns: Int) {
    throw new UnsupportedOperationException()
  }
    
  @throws(classOf[Exception])
  def checkValidation() {
    try {
      init
      if (!(new File(_problemDirectory)).exists) {
        throw new Exception("Problem directory not found.")
      }
    } catch {
      case ex: Exception => throw ex
    }
  }
    
  /**
   * Writes a DELVE cguess file representing the predictions of a neural
   * network.
   * 
   * @param results
   *            the results to be written
   * @param runNumber
   *            the number of the current system run
   * @throws DataSourceException
   */
  @throws(classOf[Exception])
  def writeResults(results: Array[Vec], runNumber: Int) {
    try {
      val trueRunNumber = (runNumber + 1) % numNetworks
      val bw = new BufferedWriter(new FileWriter(resultsDirectory + File.separator 
                                                 + "cguess." + String.valueOf(trueRunNumber)))
      var i = 0
      while (i < results.length) {
        var str = ""
        if ((results(i) == null) || (results(i).values == null)) { // if
          // pattern == null
          // means an "i don't know" answer
          var j = 0
          while (((results(j) == null) || (results(j).values == null)) && (j < results.length)) {
            // guessing the dimension of the pattern to be written
            j += 1
          }
          // System.out.println("Located pattern "+j);
          if ((results(j) != null) && (results(j).values != null)) {
            var k = 0
            while (k < results(j).dimension) {
              str = str + "? "
              k += 1
            }
          } else {
            throw new RuntimeException("The results about to be written are invalid.");
          }
        } else {
          str = results(i).toString
        }
        str.trim
        if (str.endsWith(".0")) {
          str = str.substring(0, str.length() - 2)
        }
        bw.write(str.replaceAll("NaN", "?"))
        bw.newLine
        
        i += 1
      }
      bw.close
    } catch {
      case ex: IOException => throw ex
    }
  }
    
  def createSampleInstance: DataSource = {
    val res = new DelveDataSource();
    //res.setNNetworks(5);
    res.problemDirectory = "<Directory where Delve has generated its problem files>"
    res.resultsDirectory = "<Directory where you want to write the results>"
    res
  }
}

object DelveDataSource {
  protected val GUESS_FILE_NAME = "cguess"
  protected val TEST_FILE_NAME = "test"
  protected val TRAIN_FILE_NAME = "train"
}