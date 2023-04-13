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
package org.aiotrade.lib.securities

import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.math.timeseries.DefaultBaseTSer
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.securities.model.MoneyFlow
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Reactions

/**
 *
 * @author Caoyuan Deng
 */
class MoneyFlowSer(_sec: Sec, _freq: TFreq) extends DefaultBaseTSer(_sec, _freq) with WithFreeFloat {

  private var _shortName: String = ""

  val lastModify = TVar[Long]("LM", Plot.None)
  val amountInCount = TVar[Double]("aIC", Plot.None)
  val amountOutCount = TVar[Double]("aOC", Plot.None)
  val relativeAmount = TVar[Double]("RA", Plot.None)
  val netBuyPercent = TVar[Double]("NBP", Plot.None)

  val volumeIn = TVar[Double]("Vi", Plot.None)
  val amountIn = TVar[Double]("Ai", Plot.None)
  val volumeOut = TVar[Double]("Vo", Plot.None)
  val amountOut = TVar[Double]("Ao", Plot.None)
  val volumeEven = TVar[Double]("Ve", Plot.None)
  val amountEven = TVar[Double]("Ae", Plot.None)
  
  val superVolumeIn = TVar[Double]("suVi", Plot.None)
  val superAmountIn = TVar[Double]("suAi", Plot.None)
  val superVolumeOut = TVar[Double]("suVo", Plot.None)
  val superAmountOut = TVar[Double]("suAo", Plot.None)
  val superVolumeEven = TVar[Double]("suVe", Plot.None)
  val superAmountEven = TVar[Double]("suAe", Plot.None)

  val largeVolumeIn = TVar[Double]("laVi", Plot.None)
  val largeAmountIn = TVar[Double]("laAi", Plot.None)
  val largeVolumeOut = TVar[Double]("laVo", Plot.None)
  val largeAmountOut = TVar[Double]("laAo", Plot.None)
  val largeVolumeEven = TVar[Double]("laVe", Plot.None)
  val largeAmountEven = TVar[Double]("laAe", Plot.None)

  val mediumVolumeIn = TVar[Double]("meVi", Plot.None)
  val mediumAmountIn = TVar[Double]("meAi", Plot.None)
  val mediumVolumeOut = TVar[Double]("meVo", Plot.None)
  val mediumAmountOut = TVar[Double]("meAo", Plot.None)
  val mediumVolumeEven = TVar[Double]("meVe", Plot.None)
  val mediumAmountEven = TVar[Double]("meAe", Plot.None)

  val smallVolumeIn = TVar[Double]("smVi", Plot.None)
  val smallAmountIn = TVar[Double]("smAi", Plot.None)
  val smallVolumeOut = TVar[Double]("smVo", Plot.None)
  val smallAmountOut = TVar[Double]("smAo", Plot.None)
  val smallVolumeEven = TVar[Double]("smVe", Plot.None)
  val smallAmountEven = TVar[Double]("smAe", Plot.None)
  
  val volumeNet = TVar[Double]("V", Plot.None)
  val amountNet = TVar[Double]("A", Plot.None)
  val superVolumeNet  = TVar[Double]("suV", Plot.None)
  val superAmountNet  = TVar[Double]("suA", Plot.None)
  val largeVolumeNet  = TVar[Double]("laV", Plot.None)
  val largeAmountNet  = TVar[Double]("laA", Plot.None)
  val mediumVolumeNet = TVar[Double]("meV", Plot.None)
  val mediumAmountNet = TVar[Double]("meA", Plot.None)
  val smallVolumeNet  = TVar[Double]("smV", Plot.None)
  val smallAmountNet  = TVar[Double]("smA", Plot.None)
  
  override protected def assignValue(tval: TVal) {
    super.assignValue(tval)

    val time = tval.time
    tval match {
      case mf: MoneyFlow =>
        lastModify(time) = mf.lastModify
        amountInCount(time) = mf.amountInCount
        amountOutCount(time) = mf.amountOutCount
	
        volumeIn(time) = mf.volumeIn
        amountIn(time) = mf.amountIn
        volumeOut(time) = mf.volumeOut
        amountOut(time) = mf.amountOut
        volumeEven(time) = mf.volumeEven
        amountEven(time) = mf.amountEven
        
        superVolumeIn(time) = mf.superVolumeIn
        superAmountIn(time) = mf.superAmountIn
        superVolumeOut(time) = mf.superVolumeOut
        superAmountOut(time) = mf.superAmountOut
        superVolumeEven(time) = mf.superVolumeEven
        superAmountEven(time) = mf.superAmountEven

        largeVolumeIn(time) = mf.largeVolumeIn
        largeAmountIn(time) = mf.largeAmountIn
        largeVolumeOut(time) = mf.largeVolumeOut
        largeAmountOut(time) = mf.largeAmountOut
        largeVolumeEven(time) = mf.largeVolumeEven
        largeAmountEven(time) = mf.largeAmountEven

        mediumVolumeIn(time) = mf.mediumVolumeIn
        mediumAmountIn(time) = mf.mediumAmountIn
        mediumVolumeOut(time) = mf.mediumVolumeOut
        mediumAmountOut(time) = mf.mediumAmountOut
        mediumVolumeEven(time) = mf.mediumVolumeEven
        mediumAmountEven(time) = mf.mediumAmountEven

        smallVolumeIn(time) = mf.smallVolumeIn
        smallAmountIn(time) = mf.smallAmountIn
        smallVolumeOut(time) = mf.smallVolumeOut
        smallAmountOut(time) = mf.smallAmountOut
        smallVolumeEven(time) = mf.smallVolumeEven
        smallAmountEven(time) = mf.smallAmountEven
        
        volumeNet(time) = mf.volumeNet
        amountNet(time) = mf.amountNet
        superVolumeNet(time) = mf.superVolumeNet
        superAmountNet(time) = mf.superAmountNet
        largeVolumeNet(time) = mf.largeVolumeNet
        largeAmountNet(time) = mf.largeAmountNet
        mediumVolumeNet(time) = mf.mediumVolumeNet
        mediumAmountNet(time) = mf.mediumAmountNet
        smallVolumeNet(time) = mf.smallVolumeNet
        smallAmountNet(time) = mf.smallAmountNet

        relativeAmount(time) = mf.relativeAmount
        netBuyPercent(time) = mf.volumeNet / freeFloat(time)
      case _ =>
    }
  }

  def valueOf(time: Long): Option[MoneyFlow] = {
    if (exists(time)) {
      val mf = new MoneyFlow

      mf.lastModify = lastModify(time)
      mf.relativeAmount = relativeAmount(time)
      mf.netBuyPercent = netBuyPercent(time)
      mf.amountInCount = amountInCount(time)
      mf.amountOutCount = amountOutCount(time)

      mf.superVolumeIn = superVolumeIn(time)
      mf.superAmountIn = superAmountIn(time)
      mf.superVolumeOut = superVolumeOut(time)
      mf.superAmountOut = superAmountOut(time)
      mf.superVolumeEven = superVolumeEven(time)
      mf.superAmountEven = superAmountEven(time)

      mf.largeAmountIn = largeVolumeIn(time)
      mf.largeAmountIn = largeAmountIn(time)
      mf.largeVolumeOut = largeVolumeOut(time)
      mf.largeAmountOut = largeAmountOut(time)
      mf.largeVolumeEven = largeVolumeEven(time)
      mf.largeAmountEven = largeAmountEven(time)

      mf.mediumVolumeIn = mediumVolumeIn(time)
      mf.mediumAmountIn = mediumAmountIn(time)
      mf.mediumVolumeOut = mediumVolumeOut(time)
      mf.mediumAmountOut = mediumAmountOut(time)
      mf.mediumAmountEven = mediumVolumeEven(time)
      mf.mediumVolumeEven = mediumAmountEven(time)

      mf.smallVolumeIn = smallVolumeIn(time)
      mf.smallAmountIn = smallAmountIn(time)
      mf.smallVolumeOut = smallVolumeOut(time)
      mf.smallAmountOut = smallAmountOut(time)
      mf.smallVolumeEven = smallVolumeEven(time)
      mf.smallAmountEven = smallAmountEven(time)
      
      Some(mf)
    } else None
  }

  /**
   * Try to update today's quote item according to quote, if it does not
   * exist, create a new one.
   */
  def updateFrom(mf: MoneyFlow) {
    val time = mf.time
    createOrReset(time)

    assignValue(mf)
        
    /** be ware of fromTime here may not be same as ticker's event */
    publish(TSerEvent.Updated(this, "", time, time))
  }

  def doCalcRate{
    if (isLoaded) {
      calcRateByFreeFloat(netBuyPercent, volumeNet)
    } else {
      // to avoid forward reference when "reactions -= reaction", we have to define 'reaction' first
      var reaction: Reactions.Reaction = null
      reaction = {
        case TSerEvent.Loaded(ser: QuoteSer, _, _, _, _, _) if ser eq this =>
          reactions -= reaction
          calcRateByFreeFloat(netBuyPercent, volumeNet)
      }
      reactions += reaction

      // TSerEvent.Loaded may have been missed during above procedure, so confirm it
      if (isLoaded) {
        reactions -= reaction
        calcRateByFreeFloat(netBuyPercent, volumeNet)
      }
    }
  }

  override def shortName =  _shortName
  override def shortName_=(name: String) {
    this._shortName = name
  }
    
}

/**
 * Simple test.
 */
//object MoneyFlowSer{
//  def main(args: Array[String]){
//    val mfs = new MoneyFlowSer(null, TFreq.DAILY)
//    var c = mfs.getClass
//    while (c != null){
//      println("\n\n\n\n" + c + ", name=" + c.getName)
//      c.getDeclaredFields foreach {x => println(x + ", name=" + x.getName)}
//
//      c = c.getSuperclass
//    }
//
//    exit(0)
//  }
//}
