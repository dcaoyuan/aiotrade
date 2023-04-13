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
package org.aiotrade.lib.indicator

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.BaseTSer
import org.aiotrade.lib.math.timeseries.Null
import org.aiotrade.lib.math.timeseries.TVar
import org.aiotrade.lib.math.indicator.Plot
import scala.collection.immutable
import scala.reflect.ClassTag

/**
 * 
 * @author Caoyuan Deng
 */
abstract class SpotIndicator(_baseSer: BaseTSer) extends Indicator(_baseSer) with org.aiotrade.lib.math.indicator.SpotIndicator {
    
  def this() = this(null)
    
  /**
   * @todo Also override existsFromHead and existsFromTail?
   */
  override 
  def exists(time: Long): Boolean = true

  override 
  def computeFrom(fromTime: Long) {
    // do nothing
  }
  
  protected def compute(fromIdx: Int, size: Int) {
    // do nothing
  }

  def computeSpot(time: Long) {
    /** get baseIdx before preComputeFrom(), which may clear this data */
    val baseIdx = baseSer.indexOfOccurredTime(time)
    computeSpot(time, baseIdx)
  }
  
  /**
   * @param time
   * @param baseIdx   baseIdx may be < 0, means there is no timestamps for this 
   *                  time yet, time could be future.
   */
  protected def computeSpot(time: Long, baseIdx: Int)
  
  object STVar {
    def apply[V: ClassTag](): TVar[V] = new SpotTVar[V]("", Plot.None)
    def apply[V: ClassTag](name: String): TVar[V] = new SpotTVar[V](name, Plot.None)
    def apply[V: ClassTag](name: String, plot: Plot): TVar[V] = new SpotTVar[V](name, plot)
  }
  
  final protected class SpotTVar[V: ClassTag](_name: String, _plot: Plot) extends AbstractInnerTVar[V](_name, _plot) {

    private var timeToValue = immutable.TreeMap[Long, V]() // must sort by time

    def values: ArrayList[V] = {
      throw new UnsupportedOperationException()
    }
    
    def put(time: Long, value: V): Boolean = {
      timeToValue += time-> value
      true
    }

    def put(time: Long, fromHeadOrTail: Boolean, value: V): Boolean = {
      throw new UnsupportedOperationException("Can only be accessed via time.")
    }

    def apply(time: Long): V = {
      if (!timeToValue.contains(time)) {
        computeSpot(time)
      }
      timeToValue.getOrElse(time, Null.value)
    }

    def apply(time: Long, fromHeadOrTail: Boolean): V = {
      throw new UnsupportedOperationException("Can only be accessed via time.")
    }

    def update(time: Long, value: V) {
      timeToValue += time -> value
    }

    override 
    def apply(idx: Int): V = {
      throw new UnsupportedOperationException("Can only be accessed via time.")
    }

    override 
    def update(idx: Int, value: V) {
      throw new UnsupportedOperationException("Can only be accessed via time.")
    }
    
    override 
    def reset(idx: Int) {
      throw new UnsupportedOperationException("Can only be accessed via time.")
    }

    override 
    def reset(time: Long) {
      timeToValue -= time
    }
    
    def timesIterator: Iterator[Long] = timeToValue.keysIterator
    def valuesIterator: Iterator[V] = timeToValue.valuesIterator
  } 
}

