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
package org.aiotrade.lib.math.indicator

import javax.swing.Action
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.PersistenceManager
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.descriptor.Descriptor

/**
 *
 * @author Caoyuan Deng
 */
class IndicatorDescriptor($serviceClassName: => String, $freq: => TFreq, $factors: => Array[Factor], $active: => Boolean
) extends Descriptor[Indicator]($serviceClassName, $freq, $active) {

  def this() = this(null, TFreq.DAILY, Array[Factor](), false)

  val folderName = "Indicators"

  private var _factors = new ArrayList[Factor] ++= $factors

  private var _uniSymbol: Option[String] = None


  /**
   * You specify the indictor is from another symbols
   */
  def uniSymbol = _uniSymbol
  def uniSymbol_=(uniSymbol: String) {
     _uniSymbol = uniSymbol match {
      case null | "" => None
      case _ => Some(uniSymbol)
    }
  }

  override 
  def set(serviceClassName: String, freq: TFreq): Unit = {
    super.set(serviceClassName, freq)

    setFacsToDefault
  }

  def factors: Array[Factor] = _factors.toArray
  def factors_=(factors: Array[Factor]) {
    /**
     * @NOTICE:
     * always create a new copy of in factors to seperate the factors of this
     * and that transfered in (we don't know who transfer it in, so, be more
     * carefule is always good)
     */
    val mySize = this._factors.length
    if (factors != null) {
      var i = -1
      while ({i += 1; i < factors.length}) {
        val newFac = factors(i).clone
        if (i < mySize) {
          this._factors(i) = newFac
        } else {
          this._factors += newFac
        }
      }
    } else {
      this._factors.clear
    }
  }

  override 
  def displayName: String = {
    val name = lookupServiceTemplate(classOf[Indicator], "Indicators") match {
      case Some(tpInstance) => tpInstance.shortName
      case None => serviceClassName
    }
        
    Indicator.displayName(name, factors)
  }

  /**
   * @NOTICE
   * Here we get a new indicator instance by searching DefaultFileSystem(on NetBeans).
   * This is because that this instance may from other modules (i.e. SolarisIndicator),
   * it may not be seen from this module. Actually we should not set dependency on
   * those added-on modules.
   * @param baseSer for indicator
   */
  override 
  protected def createServiceInstance(args: Any*): Option[Indicator] = args match {
    case Seq(baseSerx: BaseTSer) => lookupServiceTemplate(classOf[Indicator], "Indicators") match {
        case Some(indx) =>
          // is this indicator from another symbol ?
          val baseSer = (
            for (s <- uniSymbol if s != baseSerx.serProvider.uniSymbol;
                 p <- baseSerx.serProvider.serProviderOf(s);
                 b <- p.serOf(baseSerx.freq)
            ) yield b 
          ) getOrElse baseSerx

          val instance = if (factors.length == 0) {
            // this means this indicatorDescritor's factors may not be set yet, so set a default one now
            val instancex = Indicator(indx.getClass.asInstanceOf[Class[Indicator]], baseSer)
            factors = instancex.factors
            instancex
          } else {
            // should set facs here, because it's from one that is stored in xml
            Indicator(indx.getClass.asInstanceOf[Class[Indicator]], baseSer, factors: _*)
          }
          
          Option(instance)
        case None => None
      }
    case _ => None
  }
    
  def setFacsToDefault {
    val defaultFacs = PersistenceManager().defaultContent.lookupDescriptor(
      classOf[IndicatorDescriptor], serviceClassName, freq
    ) match {
      case None => lookupServiceTemplate(classOf[Indicator], "Indicators") match {
          case None => None
          case Some(x) => Some(x.factors)
        }
      case Some(defaultDescriptor) => Some(defaultDescriptor.factors)
    }

    defaultFacs foreach {x => factors = x}
  }

  override 
  def createDefaultActions: Array[Action] = {
    IndicatorDescriptorActionFactory().createActions(this)
  }

}

