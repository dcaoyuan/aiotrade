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
package org.aiotrade.lib.indicator.function

import org.aiotrade.lib.math.timeseries.{DefaultTSer, BaseTSer,TVar, Null}
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.signal.Side
import org.aiotrade.lib.securities.QuoteSer

/**
 *
 * @author Caoyuan Deng
 */
object Function {
  /**
   * a helper function for keeping the same functin form as Function, don't be
   * puzzled by the name, it actully will return funcion instance
   */
  protected def apply[T <: org.aiotrade.lib.math.indicator.Function](clazz: Class[T], baseSer: BaseTSer, args: Any*): T = {
    org.aiotrade.lib.math.indicator.Function(clazz, baseSer, args: _*)
  }
}

import Function._
abstract class Function extends DefaultTSer
                           with org.aiotrade.lib.math.indicator.Function {

  /**
   * Use computing session to avoid redundant computation on same idx of same
   * function instance by different callers.
   *
   * A session is a series of continuant computing usally called by Indicator
   * It may contains a couple of functions that being called during it.
   *
   * The sessionId is injected in by the caller.
   */
  private var sessionId = Long.MinValue
  protected var computedIdx = Int.MinValue

  /** base series to compute this. */
  protected var baseSer: BaseTSer = _
    
  /** To store values of open, high, low, close, volume: */
  protected var O: TVar[Double] = _
  protected var H: TVar[Double] = _
  protected var L: TVar[Double] = _
  protected var C: TVar[Double] = _
  protected var V: TVar[Double] = _
  protected var E: TVar[Double] = _

  def set(baseSer: BaseTSer, args: Any*) {
    super.set(baseSer.freq)

    this.baseSer = baseSer
    this.attach(baseSer.timestamps)

    initPredefinedVarsOfBaseSer
  }
    
  /** override this method to define your own pre-defined vars if necessary */
  protected def initPredefinedVarsOfBaseSer {
    baseSer match {
      case x: QuoteSer =>
        O = x.open
        H = x.high
        L = x.low
        C = x.close
        V = x.volume
        E = x.execCount
      case _ =>
    }
  }
        
  /**
   * This method will compute from computedIdx <b>to</b> idx.
   *
   * and AbstractIndicator.compute(final long begTime) will compute <b>from</b>
   * begTime to last data
   *
   * @param sessionId, the sessionId usally is controlled by outside caller,
   *        such as an indicator
   * @param idx, the idx to be computed to
   */
  def computeTo(sessionId: Long, idx: Int) {
    try {
      timestamps.readLock.lock

      preComputeTo(sessionId, idx)
        
      /**
       * if in same session and idx has just been computed, do not do
       * redundance computation
       */
      if (this.sessionId == sessionId && idx <= computedIdx) {
        return
      }
        
      this.sessionId = sessionId
        
      // computedIdx itself has been computed, so, compare computedIdx + 1 with idx */
      var fromIdx = math.min(computedIdx + 1, idx)
      if (fromIdx < 0) {
        fromIdx = 0
      }

      // fill with clear data from fromIdx
      if (this ne baseSer) {
        validate
      }

      // call computeSpot(i)
      val size = timestamps.size
      val toIdx = math.min(idx, size - 1)
      var i = fromIdx
      while (i <= toIdx) {
        computeSpot(i)
        i += 1
      }
        
      computedIdx = toIdx
        
      postComputeTo(sessionId, toIdx)
      
    } finally {
      timestamps.readLock.unlock
    }
  }
    
  /**
   * override this method to do something before computeTo, such as set computedIdx etc.
   */
  protected def preComputeTo(sessionId: Long, idx: Int) {
  }
    
  /**
   * override this method to do something post computeTo
   */
  protected def postComputeTo(sessionId: Long, idx: Int) {
  }
    
  /**
   * @param i, idx of spot
   */
  protected def computeSpot(i: Int)

  /**
   * Define functions
   * --------------------------------------------------------------------
   */
    
  /**
   * Functions of helper
   * ----------------------------------------------------------------------
   */
    
  protected def indexOfLastValidValue(var1: TVar[_]): Int = {
    val values = var1.values
    var i = values.size - 1
    while (i > 0) {
      val value = values(i)
      if (value != null && !(value.isInstanceOf[Double] && Null.is(value.asInstanceOf[Double]))) {
        return baseSer.indexOfOccurredTime(timestamps(i))
      }

      i -= 1
    }
    -1
  }
    
  /**
   * ---------------------------------------------------------------------
   * End of functions of helper
   */
    
    
    
    
  /**
   * Functions from FunctionSereis
   * ----------------------------------------------------------------------
   */
    
  final protected def sum(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[SUMFunction], baseSer, baseVar, period).sum(sessionId, idx)
  }
    
  final protected def max(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[MAXFunction], baseSer, baseVar, period).max(sessionId, idx)
  }
    
  final protected def min(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[MINFunction], baseSer, baseVar, period).min(sessionId, idx)
  }
    
  final protected def ma(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[MAFunction], baseSer, baseVar, period).ma(sessionId, idx)
  }
    
  final protected def ema(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[EMAFunction], baseSer, baseVar, period).ema(sessionId, idx)
  }
    
  final protected def stdDev(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[STDDEVFunction], baseSer, baseVar, period).stdDev(sessionId, idx)
  }
    
  final protected def probMass(idx: Int, baseVar: TVar[Double], period: Factor, nInterval: Factor): Array[Array[Double]] = {
    Function(classOf[PROBMASSFunction], baseSer, baseVar, null, period, nInterval).probMass(sessionId, idx)
  }
    
  final protected def probMass(idx: Int, baseVar: TVar[Double], weight: TVar[Double] , period: Factor, nInterval: Factor): Array[Array[Double]] = {
    Function(classOf[PROBMASSFunction], baseSer, baseVar, weight, period, nInterval).probMass(sessionId, idx)
  }
    
  final protected def tr(idx: Int): Double = {
    Function(classOf[TRFunction], baseSer).tr(sessionId, idx)
  }
    
  final protected def dmPlus(idx: Int): Double = {
    Function(classOf[DMFunction], baseSer).dmPlus(sessionId, idx)
  }
    
  final protected def dmMinus(idx: Int): Double = {
    Function(classOf[DMFunction], baseSer).dmMinus(sessionId, idx)
  }
    
  final protected def diPlus(idx: Int, period: Factor): Double = {
    Function(classOf[DIFunction], baseSer, period).diPlus(sessionId, idx)
  }
    
  final protected def diMinus(idx: Int, period: Factor): Double = {
    Function(classOf[DIFunction], baseSer, period).diMinus(sessionId, idx)
  }
    
  final protected def dx(idx: Int, period: Factor): Double = {
    Function(classOf[DXFunction], baseSer, period).dx(sessionId, idx)
  }
    
  final protected def adx(idx: Int, periodDi: Factor, periodAdx: Factor): Double = {
    Function(classOf[ADXFunction], baseSer, periodDi, periodAdx).adx(sessionId, idx)
  }
    
  final protected def adxr(idx: Int, periodDi: Factor, periodAdx: Factor): Double = {
    Function(classOf[ADXRFunction], baseSer, periodDi, periodAdx).adxr(sessionId, idx)
  }
    
  final protected def bollMiddle(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    Function(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollMiddle(sessionId, idx)
  }
    
  final protected def bollUpper(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    Function(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollUpper(sessionId, idx)
  }
    
  final protected def bollLower(idx: Int, baseVar: TVar[_], period: Factor, alpha: Factor): Double = {
    Function(classOf[BOLLFunction], baseSer, baseVar, period, alpha).bollLower(sessionId, idx)
  }
    
  final protected def cci(idx: Int, period: Factor, alpha: Factor): Double = {
    Function(classOf[CCIFunction], baseSer, period, alpha).cci(sessionId, idx)
  }
    
  final protected def macd(idx: Int, baseVar: TVar[_], periodSlow: Factor, periodFast: Factor): Double = {
    Function(classOf[MACDFunction], baseSer, baseVar, periodSlow, periodFast).macd(sessionId, idx)
  }
    
  final protected def mfi(idx: Int, period: Factor): Double = {
    Function(classOf[MFIFunction], baseSer, period).mfi(sessionId, idx)
  }
    
  final protected def mtm(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[MTMFunction], baseSer, baseVar, period).mtm(sessionId, idx)
  }
    
  final protected def obv(idx: Int): Double = {
    Function(classOf[OBVFunction], baseSer).obv(sessionId, idx)
  }
    
  final protected def roc(idx: Int, baseVar: TVar[_], period: Factor): Double = {
    Function(classOf[ROCFunction], baseSer, baseVar, period).roc(sessionId, idx)
  }
    
  final protected def rsi(idx: Int, period: Factor): Double = {
    Function(classOf[RSIFunction], baseSer, period).rsi(sessionId, idx)
  }
    
  final protected def sar(idx: Int, initial: Factor, step: Factor, maximum: Factor): Double = {
    Function(classOf[SARFunction], baseSer, initial, step, maximum).sar(sessionId, idx)
  }
    
  final protected def sarSide(idx: Int, initial: Factor, step: Factor, maximum: Factor): Side = {
    Function(classOf[SARFunction], baseSer, initial, step, maximum).sarSide(sessionId, idx)
  }
    
  final protected def stochK(idx: Int, period: Factor, periodK: Factor): Double = {
    Function(classOf[STOCHKFunction], baseSer, period, periodK).stochK(sessionId, idx)
  }
    
  final protected def stochD(idx: Int, period: Factor, periodK: Factor, periodD: Factor): Double = {
    Function(classOf[STOCHDFunction], baseSer, period, periodK, periodD).stochD(sessionId, idx)
  }
    
  final protected def stochJ(idx: Int, period: Factor, periodK: Factor, periodD: Factor): Double = {
    Function(classOf[STOCHJFunction], baseSer, period, periodK, periodD).stochJ(sessionId, idx)
  }
    
  final protected def wms(idx: Int, period: Factor): Double = {
    Function(classOf[WMSFunction], baseSer, period).wms(sessionId, idx)
  }
    
  final protected def zigzag(idx: Int, percent: Factor): Double = {
    Function(classOf[ZIGZAGFunction], baseSer, percent).zigzag(sessionId, idx)
  }
    
  final protected def pseudoZigzag(idx: Int, percent: Factor): Double = {
    Function(classOf[ZIGZAGFunction], baseSer, percent).pseudoZigzag(sessionId, idx)
  }
    
  final protected def zigzagSide(idx: Int, percent: Factor): Side = {
    Function(classOf[ZIGZAGFunction], baseSer, percent).zigzagSide(sessionId, idx)
  }
    
    
  /**
   * ----------------------------------------------------------------------
   * End of Functions from FunctionSereis
   */
}
