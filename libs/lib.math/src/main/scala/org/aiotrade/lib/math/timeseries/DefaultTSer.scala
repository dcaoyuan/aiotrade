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

import java.awt.Color
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.indicator.Plot
import org.aiotrade.lib.util
import scala.collection.mutable
import scala.reflect.ClassTag


/**
 * This is the default data container, which is a time sorted data contianer.
 *
 * This container has one copy of 'vars' (without null value) for both
 * compact and natural, and with two time positions:
 *   'timestamps', and
 *   'calendarTimes'
 * the 'timestamps' is actaully with the same idx correspinding to 'vars'
 *
 *
 * This class implemets all interface of ser and partly BaseTSer.
 * So you can use it as full series, but don't use those methods of BaseTSer
 * except you sub class this.
 *
 * @author Caoyuan Deng
 */
class DefaultTSer(private var _freq: TFreq) extends TSer {
  private val log = Logger.getLogger(getClass.getName)

  protected val INIT_CAPACITY = 100

  /**
   * The length of ONE_MIN is 15000/240 = 62.5 days, about 3 months
   * The length of DAILY is 15000/ 250 = 60 years.
   */
  protected val MAX_DATA_SIZE = 15000

  /**
   * a place holder plus flags
   */
  final protected type Holder = Boolean
  final protected val holders = new ArrayList[Holder]//(INIT_CAPACITY)// this will cause timestamps' lock deadlock?
  /**
   * Each var element of array is a Var that contains a sequence of values for one field of SerItem.
   * @Note: Don't use scala's HashSet or HashMap to store Var, these classes seems won't get all of them stored
   */
  val vars = new ArrayList[TVar[_]]

  /**
   * we implement occurred timestamps and items in density mode instead of spare
   * mode, to avoid itemOf(time) return null even in case of timestamps has been
   * filled. DefaultItem is a lightweight virtual class, don't worry about the
   * memory occupied.
   *
   * Should only get index from timestamps which has the proper mapping of :
   * position <-> time <-> item
   */
  private var _timestamps: TStamps = _

  private var tsLogCheckedCursor = 0
  private var tsLogCheckedSize = 0

  /**  Long description */
  protected var lname = ""
  /** Short description */
  protected var sname = ""

  def this() = this(TFreq.DAILY)

  def freq = _freq
  def set(freq: TFreq) {
    _freq = freq
  }

  def timestamps: TStamps = _timestamps
  def attach(timestamps: TStamps) {
    _timestamps = timestamps
  }

  /**
   * used only by InnerVar's constructor and AbstractIndicator's functions
   */
  protected def addVar(v: TVar[_]) {
    vars += v
  }

  /**
   * @todo, holder.size or timestamps.size ?
   */
  def size: Int = {
    try {
      readLock.lock
      
      holders.size
    } finally {
      readLock.unlock
    }
  }

  def exists(time: Long): Boolean = {
    try {
      readLock.lock
      //timestamps.readLock.lock

      /**
       * @NOTE:
       * Should only get index from timestamps which has the proper
       * position <-> time <-> item mapping
       */
      val idx = timestamps.indexOfOccurredTime(time)
      idx >= 0 && idx < holders.size
    } finally {
      readLock.unlock
      //timestamps.readLock.unlock
    }
  }

  protected def assignValue(tval: TVal) {
    // todo
  }

  /**
   * return a holder with value is true
   */
  protected def createItem(time: Long): Holder = true

  def longName: String = lname
  def shortName: String = sname
  def shortName_=(sname: String) {
    this.sname = sname
  }

  def displayName = shortName + " - (" + longName + ")"

  /**
   * @Note:
   * This function is not thread safe, since tsLogCheckedCursor and tsLogCheckedSize
   * should be atomic accessed/modified during function's running scope so.
   * Should avoid to enter here by multiple actors concurrent
   */
  def validate {
    try {
      writeLock.lock
      //timestamps.readLock.lock

      val tlog = timestamps.log
      val tlogCursor = tlog.logCursor
      var checkingCursor = tsLogCheckedCursor
      while (tlogCursor > -1 && checkingCursor <= tlogCursor) {
        val cursorMoved = if (checkingCursor != tsLogCheckedCursor) {
          // is checking a new log, should reset tsLogCheckedSize
          tsLogCheckedSize = 0
          true
        } else false

        val tlogFlag = tlog(checkingCursor)
        val tlogCurrSize = tlog.checkSize(tlogFlag)
        if (!cursorMoved && tlogCurrSize == tsLogCheckedSize) {
          // same log with same size, actually nothing changed
        } else {
          tlog.checkKind(tlogFlag) match {
            case TStampsLog.INSERT =>
              val begIdx = tlog.insertIndexOfLog(checkingCursor)

              val begIdx1 = if (!cursorMoved) {
                // if insert log is a merged one, means the inserts were continually happening one behind one
                begIdx + tsLogCheckedSize
              } else begIdx
                                    
              val insertSize = if (!cursorMoved) {
                tlogCurrSize - tsLogCheckedSize
              } else tlogCurrSize

              val newHolders = new Array[Holder](insertSize)
              var i = 0
              while (i < insertSize) {
                val time = timestamps(begIdx1 + i)
                vars foreach (_.putNull(time))
                newHolders(i) = createItem(time)
                i += 1
              }
              holders.insertAll(begIdx1, newHolders)
              log.fine(shortName + "(" + freq + ") Log check: cursor=" + checkingCursor + ", insertSize=" + insertSize + ", begIdx=" + begIdx1 + " => newSize=" + holders.size)
                            
            case TStampsLog.APPEND =>
              val begIdx = holders.size

              val appendSize = if (!cursorMoved) {
                tlogCurrSize - tsLogCheckedSize
              } else tlogCurrSize

              val newHolders = new Array[Holder](appendSize)
              var i = 0
              while (i < appendSize) {
                val time = timestamps(begIdx + i)
                vars foreach (_.putNull(time))
                newHolders(i) = createItem(time)
                i += 1
              }
              holders ++= newHolders
              log.fine(shortName + "(" + freq + ") Log check: cursor=" + checkingCursor + ", appendSize=" + appendSize + ", begIdx=" + begIdx + " => newSize=" + holders.size)

            case x => assert(false, "Unknown log type: " + x)
          }
        }
                
        tsLogCheckedCursor = checkingCursor
        tsLogCheckedSize = tlogCurrSize
        checkingCursor = tlog.nextCursor(checkingCursor)
      }

      assert(timestamps.size == holders.size,
             "Timestamps size=" + timestamps.size + " vs items size=" + holders.size +
             ", checkedCursor=" + tsLogCheckedCursor +
             ", log=" + tlog)
    } catch {
      case ex: Throwable => log.log(Level.WARNING, "exception", ex)
    } finally {
      writeLock.unlock
      //timestamps.readLock.unlock
    }

  }

  def clear(fromTime: Long) {
    try {
      writeLock.lock
      //timestamps.readLock.lock
            
      val fromIdx = timestamps.indexOrNextIndexOfOccurredTime(fromTime)
      if (fromIdx < 0) {
        return
      }

      vars foreach {_.clear(fromIdx)}

//      for (i <- timestamps.size - 1 to fromIdx) {
//        timestamps.remove(i)
//      }

      val count = holders.size - fromIdx
      holders.remove(fromIdx, count)
    } finally {
      writeLock.unlock
      //timestamps.readLock.unlock
    }

    publish(TSerEvent.Cleared(this, shortName, fromTime, Long.MaxValue))
  }

  def indexOfOccurredTime(time: Long): Int = {
    try {
      readLock.lock
      //timestamps.readLock.lock

      timestamps.indexOfOccurredTime(time)
    } finally {
      readLock.unlock
      //timestamps.readLock.unlock
    }
  }

  def existsFromHead(time: Long): Boolean = {
    try {
      readLock.lock
      val idx = indexOfOccurredTimeFromHead(time)
      if (idx >= 0 && idx < holders.size) {
        true
      } else {
        false
      }
    } finally {
      readLock.unlock
      //timestamps.readLock.unlock
    }
  }

  def indexOfOccurredTimeFromHead(time: Long): Int = {
    try {
      readLock.lock
      var i = -1
      while({i += 1; i < timestamps.size}){
        if (timestamps(i) == time) return i
        else if (timestamps(i) > time) return -1
      }
      
      -1
    } finally {
      readLock.unlock
    }
  }

  def existsFromTail(time: Long): Boolean = {
    try {
      readLock.lock
      val idx = indexOfOccurredTimeFromTail(time)
      if (idx >= 0 && idx < holders.size) {
        true
      } else {
        false
      }
    } finally {
      readLock.unlock
    }
  }

  def indexOfOccurredTimeFromTail(time: Long): Int = {
    try {
      readLock.lock
      var i = timestamps.size
      while({i -= 1; i >= 0}){
        if (timestamps(i) == time) return i
        else if (timestamps(i) < time) return -1
      }
      
      -1
    } finally {
      readLock.unlock
    }
  }

  def firstOccurredTime: Long = {
    try {
      readLock.lock
      //timestamps.readLock.lock

      timestamps.firstOccurredTime
    } finally {
      readLock.unlock
      //timestamps.readLock.unlock
    }    
  }
  
  def lastOccurredTime: Long = {
    try {
      readLock.lock
      //timestamps.readLock.lock

      timestamps.lastOccurredTime
    } finally {
      readLock.unlock
      //timestamps.readLock.unlock
    }
  }

  override 
  def toString = {
    val sb = new StringBuilder(20)
    
    sb.append(shortName).append("(").append(freq).append("): size=").append(size).append(", ")
    if (timestamps != null && timestamps.length > 0) {
      val len = timestamps.length
      
      val fst = timestamps(0)
      val lst = timestamps(len - 1)
      val cal = util.calendarOf()
      cal.setTimeInMillis(fst)
      sb.append(cal.getTime)
      sb.append(" - ")
      cal.setTimeInMillis(lst)
      sb.append(cal.getTime)
      sb.append(", values=(\n")
      for (v <- vars) {
        sb.append(v.name).append(": ... ")
        var i = math.max(0, len - 6) // print last 6 values
        while (i < len) {
          sb.append(v(i)).append(", ")
          i += 1
        }
        sb.append("\n")
      }
    }
    sb.append(")")
    
    sb.toString
  }

  /** 
   * Ser may be used as the HashMap key, for efficient reason, we define equals and hashCode method as it: 
   */
  override 
  def equals(a: Any) = a match {
    case x: TSer => this.getClass == x.getClass && this.hashCode == x.hashCode
    case _ => false
  }

  private val _hashCode = System.identityHashCode(this)
  override 
  def hashCode: Int = _hashCode

  object TVar {
    def apply[V: ClassTag](): TVar[V] = new InnerTVar[V]("", Plot.None)
    def apply[V: ClassTag](name: String): TVar[V] = new InnerTVar[V](name, Plot.None)
    def apply[V: ClassTag](name: String, plot: Plot): TVar[V] = new InnerTVar[V](name, plot)
  }
  
  final protected class InnerTVar[V: ClassTag](_name: String, _plot: Plot) extends AbstractInnerTVar[V](_name, _plot) {

    private var _values = new ArrayList[V](INIT_CAPACITY)
    def values = _values
    
    def put(time: Long, value: V): Boolean = {
      val idx = timestamps.indexOfOccurredTime(time)
      if (idx >= 0) {
        if (idx == values.size) {
          values += value
        } else {
          values.insertOne(idx, value)
        }
        true
      } else {
        assert(false, "Fill timestamps first before put an element! " + ": " + "idx=" + idx + ", time=" + time)
        false
      }
    }
    
    /**
     * @todo ? update or put
     */
    def put(time: Long, fromHeadOrTail: Boolean, value: V): Boolean = {
      val idx = if (fromHeadOrTail) DefaultTSer.this.indexOfOccurredTimeFromHead(time) else DefaultTSer.this.indexOfOccurredTimeFromTail(time)
      if (idx >= 0) {
        if (idx == _values.size) {
          _values += value
        } else {
          _values.insertOne(idx, value)
        }
        true
      } else {
        assert(false, "Fill timestamps first before put an element! " + ": " + "idx=" + idx + ", time=" + time)
        false
      }
    }
    
    def apply(time: Long): V = {
      val idx = timestamps.indexOfOccurredTime(time)
      _values(idx)
    }

    def apply(time: Long, fromHeadOrTail: Boolean): V = {
      val idx = if (fromHeadOrTail) DefaultTSer.this.indexOfOccurredTimeFromHead(time) else DefaultTSer.this.indexOfOccurredTimeFromTail(time)
      _values(idx)
    }

    def update(time: Long, value: V) {
      val idx = timestamps.indexOfOccurredTime(time)
      values(idx) = value
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    override 
    def apply(idx: Int): V = {
      super.apply(idx)
    }

    // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
    override 
    def update(idx: Int, value: V) {
      super.update(idx, value)
    }
    
    def timesIterator: Iterator[Long] = timestamps.iterator
    def valuesIterator: Iterator[V] = _values.iterator
  }
  
  //@todo SparseTVar
  /* protected class SparseTVar[V: ClassTag](
   name: String, plot: Plot
   ) extends AbstractInnerTVar[V](name, plot) {

   // @todo: timestamps may be null when go here, use lazy val as a quick fix now, shoule review it
   lazy val values = new TStampedMapBasedList[V](timestamps)

   def put(time: Long, value: V): Boolean = {
   val idx = timestamps.indexOfOccurredTime(time)
   if (idx >= 0) {
   values.add(time, value)
   true
   } else {
   assert(false, "Add timestamps first before add an element! " + ": " + "idx=" + idx + ", time=" + time)
   false
   }
   }

   def update(time: Long, fromHeadOrTail: Boolean, value: V): Boolean = {
   val idx = if (fromHeadOrTail) DefaultTSer.this.indexOfOccurredTimeFromHead(time) else DefaultTSer.this.indexOfOccurredTimeFromTail(time)
   if (idx >= 0) {
   values.add(time, value)
   true
   } else {
   assert(false, "Add timestamps first before add an element! " + ": " + "idx=" + idx + ", time=" + time)
   false
   }
   }

   def apply(time: Long): V = values(time)

   def apply(time: Long, fromHeadOrTail: Boolean): V = values(time)

   def update(time: Long, value: V) {
   values(time) = value
   }

   // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
   override 
   def apply(idx: Int): V = {
   super.apply(idx)
   }

   // @Note, see https://lampsvn.epfl.ch/trac/scala/ticket/2599
   override 
   def update(idx: Int, value: V) {
   super.update(idx, value)
   }
   } */

  /**
   * Define inner Var class
   * -----------------------------------------------------------------------
   * Horizontal view of DefaultSer. Is' a reference of one of the field vars.
   *
   * Inner Var can only live with DefaultSer.
   *
   * We define it as inner class of DefaultSer, to avoid bad usage, especially
   * when its values is also managed by DefaultSer. We should make sure the
   * operation on values, including add, delete actions will be consistant by
   * cooperating with DefaultSer.
   */
  abstract class AbstractInnerTVar[V: ClassTag](var name: String, var plot: Plot) extends TVar[V] {

    addVar(this)

    def timestamps = DefaultTSer.this.timestamps

    var layer = -1 // -1 means not set
    // @todo: timestamps may be null when go here, use lazy val as a quick fix now, shoule review it
    private lazy val colors = new TStampedMapBasedList[Color](timestamps)
    def getColor(idx: Int) = colors(idx)
    def setColor(idx: Int, color: Color) {
      colors(idx) = color
    }
  }
}
