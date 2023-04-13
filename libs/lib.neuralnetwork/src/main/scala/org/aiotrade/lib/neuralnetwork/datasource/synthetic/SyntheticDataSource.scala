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

package org.aiotrade.lib.neuralnetwork.datasource.synthetic

import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.neuralnetwork.datasource.DataSource
import org.aiotrade.lib.neuralnetwork.math.ErrorUtils
import org.aiotrade.lib.math.vector.InputOutputPoint
import org.aiotrade.lib.math.vector.Vec

/**
 * An abstract class that represents a <i>Synthetic environment </i>. A
 * synthetic environment is one that produces the data by randomly sampling a priori
 * known function. These type of environments are most usefull for automated
 * testing.
 *
 * @author Caoyuan Deng 
 */
abstract class SyntheticDataSource extends DataSource {
  private val log = Logger.getLogger(getClass.getName)
  
  private var _trainSetSize: Int = _
  private var _testSetSize: Int = _
  private var _numberOfSystemRuns: Int = 1
  private var trainSetsList: ArrayList[Array[InputOutputPoint]] = _
  private var testSetsList:  ArrayList[Array[InputOutputPoint]] = _

  def testSetSize = _testSetSize
  def testSetSize_=(testSetSize: Int) {
    _testSetSize = testSetSize
  }

  def trainSetSize = _trainSetSize
  def trainSetSize_=(trainSetSize: Int) {
    _trainSetSize = trainSetSize
  }

  override
  def numNetworks = _numberOfSystemRuns
  override
  def numNetworks_=(numberOfRuns: Int) {
    _numberOfSystemRuns = numberOfRuns
  }

  def init() {
    trainSetsList = new ArrayList[Array[InputOutputPoint]]()
    testSetsList = new ArrayList[Array[InputOutputPoint]]()
    var i = 0
    while (i < _numberOfSystemRuns) {
      trainSetsList += generateSet(trainSetSize)
      testSetsList  += generateSet(testSetSize)
      i += 1
    }
  }

  @throws(classOf[Exception])
  def getTrainingPoints(runNumber: Int): Array[InputOutputPoint] = {
    try {
      trainSetsList(runNumber)
    } catch {
      case ex: Throwable => // shall we generate the set?
        init()
        getTrainingPoints(runNumber)
    }
  }

  protected def generateSet(setSize: Int): Array[InputOutputPoint] = {
    val res = Array.ofDim[InputOutputPoint](setSize)
    var i = 0
    while (i < setSize) {
      val input = generateRandomInput
      val output = synthetizeOutput(input)
      res(i) = InputOutputPoint(input, output)
      i += 1
    }
    res
  }

  protected def generateRandomInput(): Vec

  protected def synthetizeOutput(input: Vec): Vec

  @throws(classOf[Exception])
  def getValidatingInputs(runNumber: Int): Array[Vec] = {
    val iops = try {
      testSetsList(runNumber)
    } catch {
      case ex: Throwable => // shall we generate the set?
        init
        return getValidatingInputs(runNumber)
    }

    val res = Array.ofDim[Vec](iops.length)
    var i = 0
    while (i < iops.length) {
      res(i) = iops(i).input
      i += 1
    }
    res
  }

  @throws(classOf[Exception])
  def getTestDataset(runNumber: Int): Array[InputOutputPoint] = {
    try {
      testSetsList(runNumber)
    } catch {
      case ex: Throwable => // shall we generate the set?
        init
        getTestDataset(runNumber)
    }
  }

  @throws(classOf[Exception])
  def getTestExpectedOutputsSet(runNumber: Int): Array[Vec] = {
    val iops = try {
      testSetsList(runNumber)
    } catch {
      case ex: Throwable => // shall we generate the set?
        init
        return getTestExpectedOutputsSet(runNumber)
    }

    val res = Array.ofDim[Vec](iops.length)

    var i = 0
    while (i < iops.length) {
      res(i) = iops(i).output
      i += 1
    }

    res
  }

  def getFullTestDataset(runNumber: Int): Array[InputOutputPoint] = {
    try {
      testSetsList(runNumber)
    } catch {
      case ex: Throwable => // shall we generate the set?
        init
        getFullTestDataset(runNumber)
    }
  }

  @throws(classOf[Exception])
  def writeResults(results: Array[Vec], runNumber: Int) {
    val res = getTestExpectedOutputsSet(runNumber)

    log.log(Level.INFO, "Mean square test set error for run number " + runNumber + ": " + ErrorUtils.meanSquaredError(res, results) + ".");
  }

  @throws(classOf[Exception])
  def validate() {
    if (_trainSetSize < 0 || _testSetSize < 0) {
      throw new Exception("Wrong set sizes")
    }
  }

  override
  def inputDimension: Int = {
    if (trainSetsList == null) {
      init
    }
    trainSetsList(0)(0).input.dimension
  }

  override
  def outputDimension: Int = {
    if (trainSetsList == null) {
      init
    }
    trainSetsList(0)(0).output.dimension
  }
}