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
package org.aiotrade.lib.indicator

import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.ComputeFrom
import org.aiotrade.lib.math.indicator.Factor
import org.aiotrade.lib.math.indicator.Id
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.TSerEvent
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util
import org.aiotrade.lib.util.ValidTime
import org.aiotrade.lib.util.actors.Publisher
import scala.reflect._

/**
 * @author Caoyuan Deng
 */
abstract class PanelIndicator[T <: Indicator : ClassTag]($freq: TFreq) extends FreeIndicator(null, $freq) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _sectorKey: String = ""
  def name: String
  final lazy val key = name + "_" + sectorKey.trim + "_" + freq.shortName
  def sectorKey = _sectorKey
  def sectorKey_=(sectorKey: String) {
    this._sectorKey = sectorKey
  }

  val indicators = new ArrayList[(T, ValidTime[Sec])]
  
  private var lastFromTime = Long.MaxValue
  reactions += {
    case PanelIndicator.PanelHeartBeat => 
      computeFrom(lastFromTime)
      lastFromTime = computedTime
    case ComputeFrom(time) => 
      lastFromTime = time
    case TSerEvent.Loaded(_, _, fromTime, toTime, _, callback) => 
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Refresh(_, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Updated(_, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Computed(src, _, fromTime, toTime, _, callback) =>
      lastFromTime = math.min(fromTime, lastFromTime)
      //computeFrom(fromTime)
    case TSerEvent.Cleared(src, _, fromTime, toTime, _, callback) =>
      clear(fromTime)
  }
  listenTo(PanelIndicator)

  def addSecs(secValidTimes: collection.Seq[ValidTime[Sec]]) {
    secValidTimes foreach addSec
    log.info("Add secs: " + secValidTimes.length)
    publish(ComputeFrom(0))
  }

  def addSec(secValidTime: ValidTime[Sec]): Option[T] = {
    secValidTime.ref.serOf(freq) match {
      case Some(baseSer) =>
        val ind = org.aiotrade.lib.math.indicator.Indicator(classTag[T].runtimeClass.asInstanceOf[Class[T]], baseSer, factors: _*)
        listenTo(ind)
        indicators += ((ind, secValidTime))
        Some(ind)
      case _ => None
    }
  }

  def removeSec(secValidTime: ValidTime[Sec]): Option[T] = {
    secValidTime.ref.serOf(freq) match {
      case Some(baseSer) =>
        val ind = org.aiotrade.lib.math.indicator.Indicator(classTag[T].asInstanceOf[Class[T]], baseSer, factors: _*)
        deafTo(ind)
        indicators -= ((ind, secValidTime))
        Some(ind)
      case _ => None
    }
  }
  
  def descriptor = "(" + this.getClass.getSimpleName + "," + sectorKey + "," + freq.shortName + ")"

  override def computeFrom(fromTime0: Long) {
    
    val (firstTime, lastTime) = firstLastTimeOf(indicators)
    
    val fromTime = if (fromTime0 == 0 || fromTime0 == 1) { // fromTime maybe 1, when called by computeFrom(afterThisTime)
      firstTime
    } else fromTime0
    
    if (fromTime == Long.MaxValue || lastTime == Long.MinValue) return
    
    val t0 = System.currentTimeMillis
    compute(fromTime, lastTime)
    log.info(descriptor + ", size=" + size + ", computed " + util.formatTime(fromTime) + " - " + util.formatTime(lastTime) + " in " + (System.currentTimeMillis - t0) + "ms")
//    val vmap = export(fromTime, lastTime)
//    if (vmap.nonEmpty && vmap.values.head.asInstanceOf[Array[_]].size != 0) publish(key -> vmap)
  }
  
  /**
   * Implement this method for actual computing.
   * @param from time, included
   * @param to time, included
   */
  protected def compute(fromTime: Long, toTime: Long)
  
  protected def firstLastTimeOf(inds: ArrayList[(T, ValidTime[Sec])]): (Long, Long) = {
    var firstTime = Long.MaxValue
    var lastTime = Long.MinValue

    var i = -1
    while ({i += 1; i < inds.length}) {
      val ind = inds(i)._1
      if (ind != null && ind.timestamps.length > 0) {
        val fTime = ind.firstOccurredTime
        firstTime = math.min(firstTime, fTime)
        
        val lTime = ind.lastOccurredTime
        lastTime = math.max(lastTime, lTime)
      }
    }

    (firstTime, lastTime)
  }
}

object PanelIndicator extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)

  private val idToIndicator = new ConcurrentHashMap[Id[_ <: PanelIndicator[_ <: Indicator]], PanelIndicator[_ <: Indicator]](8, 0.9f, 1)

  private val runtime = Runtime.getRuntime
  private case object PanelHeartBeat
  private val interval = 30000L // 30 seconds
  private var count = 0
  var indicatorCount = 0

  def startTimer() = {
    val timer = new Timer("PanelIndictorTimer")
    timer.scheduleAtFixedRate(new TimerTask {
        def run {
          log.info("Publish panel heart beat: " + listeners.size)
          publish(PanelHeartBeat)
//          count += 1
//          if (count > 10){
//            count = 0
//            log.info("Before collect garbage, Max memory:" + (runtime.maxMemory/1024.0f/1024) + "M, total memory:" + (runtime.totalMemory/1024.0f/1024 + "M, free memory:" + (runtime.freeMemory/1024.0f/1024) + "M" ))
//            System.gc
//            log.info("After collect garbage, Max memory:" + (runtime.maxMemory/1024.0f/1024) + "M, total memory:" + (runtime.totalMemory/1024.0f/1024 + "M, free memory:" + (runtime.freeMemory/1024.0f/1024) + "M" ))
//          }
        }
      }, 1000, interval)
  }

  def idOf[T](klass: Class[T], sectorKey: String, freq: TFreq, factors: Factor*) = {
    val factorArr = factors.toArray
    val factorLen = factorArr.length
    val args = new Array[Any](factorLen + 1)
    args(0) = freq
    System.arraycopy(factorArr, 0, args, 1, factorLen)

    Id(klass, sectorKey, args: _*)
  }
  
  def apply[T <: PanelIndicator[_ <: Indicator]](klass: Class[T], sectorKey: String, freq: TFreq, factors: Factor*): (T, Boolean) = {
    val id = idOf(klass, sectorKey, freq, factors: _*)
    
    idToIndicator.get(id) match {
      case null =>
        /** if got none from idToIndicator, try to create new one */
        try {
          val indicator = klass.getConstructor(classOf[TFreq]).newInstance(freq)
          indicator.sectorKey = sectorKey
          indicator.factors = factors.toArray
          idToIndicator.putIfAbsent(id, indicator)
          
          indicatorCount += 1
          log.info("Started panel indicator: " + indicator.descriptor + ", indicators count: " + indicatorCount)
          (indicator, true)
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex); (null.asInstanceOf[T], false)
        }
      case x => (x.asInstanceOf[T], false)
    }
  }
}
