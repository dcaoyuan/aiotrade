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
package org.aiotrade.lib.math.timeseries

import java.util.Calendar
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import scala.collection.mutable
import org.aiotrade.lib.math.signal._
import scala.reflect.ClassTag

/**
 * @author Caoyuan Deng
 */
final class TimeQueue[V: ClassTag](freq: TFreq, limit: Int, onlyOneValue: Boolean = false) {

  private val log = Logger.getLogger(getClass.getName)
  private val lastIdx = limit - 1
  private var _time_xs = new Array[(Long, mutable.HashMap[String, Set[V]])](limit)

  val length = limit
  def apply(i: Int): (Long, collection.Map[String, Set[V]]) = _time_xs synchronized {_time_xs(i)}
  def last() = apply(lastIdx)

  def put(key: String, time: Long, value: V): Unit = _time_xs synchronized  {
    var willAdd = true
    var lastKeyToXs = _time_xs(lastIdx) match {
      case null => null
      case (timeMax, keyToXs) =>
        val nFreqs = freq.nFreqsBetween(timeMax, time)
        if (nFreqs == 0) {
          keyToXs
        } else if (nFreqs > 0) {
          val newTime_xs = new Array[(Long, mutable.HashMap[String, Set[V]])](limit)
          System.arraycopy(_time_xs, 1, newTime_xs, 0, lastIdx)
          _time_xs = newTime_xs
          _time_xs(lastIdx) = null
          null
        } else {
          willAdd = false
          null
        }
    }

    if (willAdd) {
      if (lastKeyToXs == null) {
        val newXs = new ArrayList[V]
        newXs += value
        val newMap = mutable.HashMap[String, Set[V]](key -> newXs.toSet)
        _time_xs(lastIdx) = (time, newMap)
      } else {
        var xs = lastKeyToXs.get(key).getOrElse(new ArrayList[V]().toSet)
        xs += value
        if (onlyOneValue) xs = Array(value).toSet
        lastKeyToXs(key) = xs
      }
    }
  }
}

object TimeQueue {
  // --- simple test
  def main(args: Array[String]) {
    val tq = new TimeQueue[Int](TFreq.DAILY, 2)

    val cal = Calendar.getInstance
    cal.setTimeInMillis(0)
    cal.set(1990, 0, 1)
    for (i <- 0 until 10) {
      cal.add(Calendar.DAY_OF_MONTH, 1)
      tq.put("a", cal.getTimeInMillis, i)
      tq.put("a", cal.getTimeInMillis, i + 10)
      tq.put("b", cal.getTimeInMillis, i)
      tq.put("b", cal.getTimeInMillis, i + 10)
    }

    println(tq._time_xs(0))
    println(tq._time_xs(1))
    val ok = {
      tq._time_xs(0) == (631929600000L, Map("a" -> Set(8), "b" -> Set(8))) &&
      tq._time_xs(1) == (632016000000L, Map("a" -> Set(9), "b" -> Set(9)))
    }
    println(ok)
    assert(ok, "Error.")
  }
}

