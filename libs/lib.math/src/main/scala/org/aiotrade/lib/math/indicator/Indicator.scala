/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.math.indicator

import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TSer


/**
 *
 * @author Caoyuan Deng
 */
trait Indicator extends TSer with WithFactors with Ordered[Indicator] {

  protected val Plot = org.aiotrade.lib.math.indicator.Plot
  
  reactions += {
    case ComputeFrom(time) => 
      if (baseSer != null) computeFrom(time)
    case FactorChanged => 
      if (baseSer != null) computeFrom(0)
  }

  def set(baseSer: BaseTSer)
  def baseSer: BaseTSer
  def baseSer_=(baseSer: BaseTSer)

  /**
   * If uniSymbol.isDefined, means the baseSer may belong to another one which is of this uniSymbol
   */
  def uniSymbol: Option[String]
  def uniSymbol_=(uniSymbol: String)

  /**
   * @param time to be computed from
   */
  def computeFrom(time: Long)
  def computedTime: Long

  def dispose
  
  def compare(another: Indicator): Int = {
    if (this.shortName.equalsIgnoreCase(another.shortName)) {
      if (this.hashCode < another.hashCode) -1 else (if (this.hashCode == another.hashCode) 0 else 1)
    } else {
      this.shortName.compareTo(another.shortName)
    }
  }

}

trait WithFactors {self: Indicator =>

  /**
   * factors of this instance, such as period long, period short etc,
   */
  private var _factors = Array[Factor]()
  
  def factorValues: Array[Double] = factors map {_.value}
  /**
   * if any value of factors changed, will publish FactorChanged
   */
  def factorValues_=(values: Array[Double]) {
    var valueChanged = false
    if (values != null) {
      if (factors.length == values.length) {
        var i = -1
        while ({i += 1; i < values.length}) {
          val myFactor = _factors(i)
          val inValue = values(i)
          /** check if changed happens before set myFactor */
          if (myFactor.value != inValue) {
            valueChanged = true
          }
          myFactor.value = inValue
        }
      }
    }

    if (valueChanged) publish(FactorChanged)
  }

  def factors = _factors
  def factors_=(factors: Array[Factor]) {
    if (factors != null) {
      val values = new Array[Double](factors.length)
      var i = -1
      while ({i += 1; i < factors.length}) {
        values(i) = factors(i).value
      }
      factorValues = values
    }
  }

  def replaceFactor(oldFactor: Factor, newFactor: Factor) {
    var idxOld = -1
    var i = -1
    var break = false
    while ({i += 1; i < factors.length} && !break) {
      val factor = factors(i)
      if (factor == oldFactor) {
        idxOld = i
        break = true
      }
    }

    if (idxOld != -1) {
      factors(idxOld) = newFactor
    }
  }

  private def addFactor(factor: Factor) {
    /** add factor reaction to this factor */
    val olds = _factors
    _factors = new Array[Factor](olds.length + 1)
    System.arraycopy(olds, 0, _factors, 0, olds.length)
    _factors(_factors.length - 1) = factor
  }

  /**
   * Inner Fac class that will be added to AbstractIndicator instance
   * automaticlly when new it.
   * Fac can only lives in AbstractIndicator
   *
   *
   * @see addFactor(..)
   * --------------------------------------------------------------------
   */
  protected class InnerFactor(name: => String, value: => Double, step: => Double, minValue: => Double, maxValue: => Double
  ) extends Factor(name, value, step, minValue, maxValue) {
    addFactor(this)
  }

  object Factor {
    def apply(name: String, value: Double) =
      new InnerFactor(name, value, 1.0, Double.MinValue, Double.MaxValue)
    
    def apply(name: String, value: Double, step: Double) =
      new InnerFactor(name, value, step, Double.MinValue, Double.MaxValue)
    
    def apply(name: String, value: Double, step: Double, minValue: Double, maxValue: Double) =
      new InnerFactor(name, value, step, minValue, maxValue)
  }
}

object Indicator {
  private val log = Logger.getLogger(this.getClass.getName)

  private val FAC_DECIMAL_FORMAT = new DecimalFormat("0.###")

  private val idToIndicator = new ConcurrentHashMap[Id[_ <: Indicator], Indicator](8, 0.9f, 1)

  def idOf[T <: Indicator](klass: Class[T], baseSer: BaseTSer, factors: Factor*) = Id[T](klass, baseSer, factors: _*)
  
  def apply[T <: Indicator](klass: Class[T], baseSer: BaseTSer, factors: Factor*): T = {
    val id = idOf(klass, baseSer, factors: _*)
    idToIndicator.get(id) match {
      case null =>
        /** if got none from idToIndicator, try to create new one */
        try {
          val indicator = klass.newInstance
          indicator.factors = factors.toArray // set factors first to avoid multiple computeFrom(0)
          /** don't forget to call set(baseSer) immediatley */
          indicator.set(baseSer)
          idToIndicator.putIfAbsent(id, indicator)
          indicator.computeFrom(0)
          indicator
        } catch {
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); null.asInstanceOf[T]
        }
      case x => x.asInstanceOf[T]
    }
  }

  def releaseAll() {
    idToIndicator.clear
  }
  
  def displayName(ser: TSer): String = ser match {
    case x: Indicator => displayName(ser.shortName, x.factors)
    case _ => ser.shortName
  }

  def displayName(name: String, factors: Array[Factor]): String = {
    if (factors.length == 0) name
    else factors map {x => FAC_DECIMAL_FORMAT.format(x.value)} mkString(name + "(", ",", ")")
  }
}

final case class ComputeFrom(time: Long)
