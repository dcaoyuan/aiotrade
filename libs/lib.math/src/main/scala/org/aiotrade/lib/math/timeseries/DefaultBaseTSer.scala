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

/**
 *
 * @author Caoyuan Deng
 */
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.datasource.SerProvider

class DefaultBaseTSer(_serProvider: SerProvider, _freq: TFreq) extends DefaultTSer(_freq) with BaseTSer {
  def this() = this(null, TFreq.DAILY)

  private val log = Logger.getLogger(getClass.getName)
  
  private var _isOnCalendarMode = false
    
  attach(TStamps(INIT_CAPACITY))

  def serProvider = _serProvider

  /*-
   * !NOTICE
   * This should be the only place to create an Item from outside, because it's
   * a bit complex to finish an item creating procedure, the procedure contains
   * at least 3 steps:
   * 1. create a clear holder, which with clear = true, and idx to be set
   *    later by holders;
   * 2. add the time to timestamps properly.
   * @see #internal_addClearItemAndNullVarValuesToList_And_Filltimestamps__InTimeOrder(long, SerItem)
   * 3. add null value to vars at the proper idx.
   * @see #internal_addTime_addClearItem_addNullVarValues()
   *
   * So we do not try to provide other public methods such as addItem() that can
   * add item from outside, you should use this method to create a new (a clear)
   * item and return it, or just clear it, if it has be there.
   * And that why define some motheds signature begin with internal_, becuase
   * you'd better never think to open these methods to protected or public.
   * @return Returns the index of time.
   */
  def createOrReset(time: Long){
    try {
      writeLock.lock
      /**
       * @NOTE:
       * Should only get index from timestamps which has the proper
       * position <-> time <-> item mapping
       */
      val idx = timestamps.indexOfOccurredTime(time)
      if (idx >= 0 && idx < holders.size) {
        // existed, reset it
        vars foreach (_.reset(idx))
        holders(idx) = false
      } else {
        // append at the end: create a new one, add placeholder
        val holder = createItem(time)
        internal_addItem_fillTimestamps_InTimeOrder(time, holder)
      }
      
    } finally {
      writeLock.unlock
    }
  }

  def createWhenNonExist(time: Long) {
    try {
      writeLock.lock
      /**
       * @NOTE:
       * Should only get index from timestamps which has the proper
       * position <-> time <-> item mapping
       */
      val idx = timestamps.indexOfOccurredTime(time)
      if (!(idx >= 0 && idx < holders.size)) {
        // append at the end: create a new one, add placeholder
        val holder = createItem(time)
        internal_addItem_fillTimestamps_InTimeOrder(time, holder)
      }

    } finally {
      writeLock.unlock
    }
  }

  /**
   * Add a null item and corresponding time in time order,
   * should process time position (add time to timestamps orderly).
   * Support inserting time/clearItem pair in random order
   *
   * @param time
   * @param clearItem
   */
  private def internal_addItem_fillTimestamps_InTimeOrder(time: Long, holder: Holder): Int = {
    // @Note: writeLock timestamps only when insert/append it
    val lastOccurredTime = timestamps.lastOccurredTime
    if (time < lastOccurredTime) {
      val existIdx = timestamps.indexOfOccurredTime(time)
      if (existIdx >= 0) {
        vars foreach (_.putNull(time))
        // as timestamps includes this time, we just always put in a none-null item
        holders.insertOne(existIdx, holder)
        
        existIdx
      } else {
        val idx = timestamps.indexOrNextIndexOfOccurredTime(time)
        assert(idx >= 0,  "Since itemTime < lastOccurredTime, the idx=" + idx + " should be >= 0")

        // (time at idx) > itemTime, insert this new item at the same idx, so the followed elems will be pushed behind
        try {
          timestamps.writeLock.lock

          // should add timestamps first
          timestamps.insertOne(idx, time)
          timestamps.log.logInsert(1, idx)

          vars foreach (_.putNull(time))
          holders.insertOne(idx, holder)
          
          idx

          // @todo Not remove it now
//          if (timestamps.size > MAX_DATA_SIZE){
//            val length = timestamps.size - MAX_DATA_SIZE
//            clearUntilIdx(length)
//          }

        } finally {
          timestamps.writeLock.unlock
        }
      }
    } else if (time > lastOccurredTime) {
      // time > lastOccurredTime, just append it behind the last:
      try {
        timestamps.writeLock.lock

        // should append timestamps first
        timestamps += time
        timestamps.log.logAppend(1)

        vars foreach (_.putNull(time))
        holders += holder
        
        this.size - 1

        // @todo Not remove it now.
//        if (timestamps.size > MAX_DATA_SIZE){
//          val length = timestamps.size - MAX_DATA_SIZE
//          clearUntilIdx(length)
//        }

      } finally {
        timestamps.writeLock.unlock
      }
    } else {
      // time == lastOccurredTime, keep same time and append vars and holders.
      val existIdx = timestamps.indexOfOccurredTime(time)
      if (existIdx >= 0) {
        vars foreach (_.putNull(time))
        holders += holder
        
        size - 1
      } else {
        assert(false,
               "As it's an adding action, we should not reach here! " +
               "Check your code, you are probably from createOrReset(long), " +
               "Does timestamps.indexOfOccurredTime(itemTime) = " + timestamps.indexOfOccurredTime(time) +
               " return -1 ?")
        -1
        // to avoid concurrent conflict, just do nothing here.
      }
    }
  }

  private def clearUntilIdx(idx: Int){
    timestamps.remove(0, idx)
    holders.remove(0, idx)
  }

  /**
   * Append TVals to ser.
   * To use this method, should define proper assignValue(value)
   */
  override 
  def ++=[V <: TVal](values: Array[V]): TSer = {
    if (values.length == 0) return this
    
    try {
      writeLock.lock

      var frTime = Long.MaxValue
      var toTime = Long.MinValue

      val lenth = values.length
      val shouldReverse = !isAscending(values)
      var i = if (shouldReverse) lenth - 1 else 0
      while (i >= 0 && i < lenth) {
        val value = values(i)
        if (value != null) {
          val time = value.time
          createOrReset(time)
          assignValue(value)

          frTime = math.min(frTime, time)
          toTime = math.max(toTime, time)
        } else {
          // @todo why will  happen? seems form loadFromPersistence
          log.warning("Value of i=" + i + " is null")
        }

        // shoudReverse: the recent quote's index is more in quotes, thus the order in timePositions[] is opposed to quotes
        // otherwise:    the recent quote's index is less in quotes, thus the order in timePositions[] is the same as quotes
        if (shouldReverse)
          i -= 1
        else
          i += 1
      }

      publish(TSerEvent.Updated(this, shortName, frTime, toTime))

    } finally {
      writeLock.unlock
    }

    log.fine("TimestampsLog: " + timestamps.log)
    this
  }

  def isOnCalendarMode = _isOnCalendarMode
  def toOnCalendarMode {
    _isOnCalendarMode = true
  }
  def toOnOccurredMode {
    _isOnCalendarMode = false
  }
        
  def indexOfTime(time: Long): Int = activeTimestamps.indexOfOccurredTime(time)
  def timeOfIndex(idx: Int): Long = activeTimestamps(idx)

  def rowOfTime(time: Long): Int = activeTimestamps.rowOfTime(time, freq)
  def timeOfRow(row: Int): Long = activeTimestamps.timeOfRow(row, freq)
  def lastOccurredRow: Int = activeTimestamps.lastRow(freq)
    
  override 
  def size: Int = activeTimestamps.sizeOf(freq)

  private def activeTimestamps: TStamps = {
    try {
      readLock.lock

      if (_isOnCalendarMode) timestamps.asOnCalendar else timestamps
    } finally {
      readLock.unlock
    }
  }
}
