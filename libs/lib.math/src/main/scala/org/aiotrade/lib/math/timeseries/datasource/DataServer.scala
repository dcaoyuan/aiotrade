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
package datasource

import java.awt.Image
import java.awt.Toolkit
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.util.actors.Publisher
import org.aiotrade.lib.util.actors.Reactor
import scala.collection.mutable
import scala.reflect.ClassTag

/**
 * This class will load the quote datas from data source to its data storage: quotes.
 * 
 * @param [V] data storege type
 * @author Caoyuan Deng
 */
abstract class DataServer[V: ClassTag] extends Ordered[DataServer[V]] with Publisher {
  import DataServer._
  private val log = Logger.getLogger(this.getClass.getName)

  type C <: DataContract[_]

  private case class RequestData(contracts: Iterable[C])
  // @Note due to bug in PartialFunction, the inner final case class will cause isDefinedAt won't be compiled
  case class DataLoaded(values: Array[V], contract: C)
  case class DataProcessed(contract: C)

  protected val EmptyValues = Array[V]()
  protected val ANCIENT_TIME: Long = Long.MinValue

  protected val subscribingMutex = new Object
  // --- Following maps should be created once here, since server may be singleton:
  //private val contractToStorage = new HashMap[C, ArrayList[V]] // use ArrayList instead of ArrayBuffer here, for toArray performance
  private val _refreshContracts = mutable.Set[C]()
  /** a quick seaching map */
  private val _refreshSymbolToContract = mutable.Map[String, C]()
  // --- Above maps should be created once here, since server may be singleton

  private var isRefreshable = false

  /**
   * @note Beware of a case in producer-consumer module:
   * During process DataLoaded, we may have accepted lots HeartBeat.
   * The producer is the one who implements requestData(...) and publishs DataLoaded,
   * after data collected. For example, who reads the data file and produces values;
   * The consumer is the one who accept DataLoaded and implements processData(...).
   * When producer collects data very quickly, (much) faster than consumer, the
   * values that are carried by DataLoaded will be blocked and the datum are stored
   * in actor's mailbox, i.e. the memory. In extreme cases, the memory will be exhausted
   * finally. 
   * 
   * You have to balance in this case, if the data that to be collected are very
   * important, that cannot be lost, you should increase the memory or store data
   * in persistence cache first. Otherwise, you have to try to sync the producer and 
   * the consumer.
   */
  private var flowCount: Int = _ // flow control that tries to balance request and process
  // --- a proxy actor for HeartBeat event etc, which will detect the speed of
  // refreshing requests, if consumer can not catch up the producer, will drop
  // some requests.
  reactions += {
    case HeartBeat(interval) =>
      if (isRefreshable && flowCount < 5) {
        // refresh from loadedTime for subscribedContracts
        try {
          log.fine("Got HeartBeat message, going to request data, flowCount=" + flowCount)
          requestActor ! RequestData(subscribedContracts)
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        }
      } else {
        flowCount -= 1 // give chance to requestData
      }
      flowCount = math.max(0, flowCount) // avoid too big gap
  }
  listenTo(DataServer)

  
  /**
   * We'll separate requestActor and processActor, so the request and process routines can be balanced a bit.
   * Otherwise, if the RequestData messages were sent batched, there will be no change to fetch DataLoaded message
   * before RequestData
   */
  private val requestActor = new Reactor {
    reactions += {
      case RequestData(contracts) =>
        try {
          flowCount += 1
          log.fine("Got RequestData message, going to request data, flowCount=" + flowCount)
          requestData(contracts)
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        }
    }
  }

  private val processActor = new Reactor {
    reactions += {
      // @Note 'contract' may be null, for instance: batch tickers loaded with multiple symbols.
      case DataLoaded(values, contract) =>
        val t0 = System.currentTimeMillis
        try {
          flowCount -= 1
          log.info("Got DataLoaded message, going to process data, flowCount=" + flowCount)
          val loadedTime = processData(values, contract)
          if (contract ne null) {
            log.info("Processed data for " + contract.srcSymbol)
            contract.loadedTime = math.max(loadedTime, contract.loadedTime)
          }
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        }
      
        publish(DataProcessed(contract))
        log.info("Processed data in " + (System.currentTimeMillis - t0) + "ms")
    }
  }

  // --- public interfaces

  def loadData(contracts: Iterable[C]) {
    log.info("Fired RequestData message for " + contracts.map(_.srcSymbol))
    // transit to async load reactor to put requests in queue (actor's mailbox)
    requestActor ! RequestData(contracts)
  }

  /**
   * Implement this method to request data from data source.
   * It should publish DataLoaded event to enable processData and fire chained events, 
   * such as TSerEvent.Loaded etc.
   *
   * @note If contract.fromTime equals ANCIENT_TIME, you may need to process this condition.
   * @param contracts
   * @publish DataLoaded
   */
  protected def requestData(contracts: Iterable[C])

  /**
   * Publish loaded data to local reactor (including this DataServer instance), 
   * or to remote message system (by overridding it).
   * @Note this DataServer will react to DataLoaded with processData automatically if it
   * received this event
   * @See reactions += {...}
   */
  protected def publishData(msg: Any) {
    processActor ! msg
  }
    
  /**
   * @param values the TVal values
   * @param contract could be null
   * @return loadedTime
   */
  protected def processData(values: Array[V], contract: C): Long

  def startRefresh {isRefreshable = true}
  def stopRefresh  {isRefreshable = false}

  // ----- subscribe/unsubscribe is used for refresh only

  def subscribe(contract: C): Unit = subscribingMutex synchronized {
    _refreshContracts += contract
    _refreshSymbolToContract += contract.srcSymbol -> contract
  }

  def unsubscribe(contract: C): Unit = subscribingMutex synchronized {
    cancelRequest(contract)
    _refreshContracts -= contract
    _refreshSymbolToContract -= contract.srcSymbol
  }

  def subscribedContracts  = _refreshContracts
  def subscribedSrcSymbols = _refreshSymbolToContract

  def isContractSubsrcribed(contract: C): Boolean = {
    _refreshContracts contains contract
  }

  def isSymbolSubscribed(srcSymbol: String): Boolean = {
    _refreshSymbolToContract contains srcSymbol
  }

  /**
   * @TODO
   * temporary method? As in some data feed, the symbol is not unique,
   * it may be same in different exchanges with different secType.
   */
  def contractOf(srcSymbol: String): Option[C] = {
    _refreshSymbolToContract.get(srcSymbol)
  }

  def displayName: String
  def defaultDatePattern: String
  def sourceTimeZone: TimeZone
  /**
   * @return serial number, valid only when >= 0
   */
  def serialNumber: Int

  /**
   * Override it to return your icon
   * @return an image as the data server icon
   */
  def icon: Option[Image] = DEFAULT_ICON

  /**
   * Convert source sn to source id in format of :
   * sn (0-63)       id (64 bits)
   * 0               ..,0000,0000
   * 1               ..,0000,0001
   * 2               ..,0000,0010
   * 3               ..,0000,0100
   * 4               ..,0000,1000
   * ...
   * @return source id
   */
  def id: Long = {
    val sn = serialNumber
    assert(sn >= 0 && sn < 63, "source serial number should be between 0 to 63!")

    if (sn == 0) 0 else 1 << (sn - 1)
  }

  // -- end of public interfaces

  /** @Note DateFormat is not thread safe, so we always return a new instance */
  protected def dateFormatOf(timeZone: TimeZone): DateFormat = {
    val pattern = defaultDatePattern
    val dateFormat = new SimpleDateFormat(pattern)
    dateFormat.setTimeZone(timeZone)
    dateFormat
  }

  protected def isAscending(values: Array[_ <: TVal]): Boolean = {
    val size = values.length
    if (size <= 1) {
      true
    } else {
      var i = -1
      while ({i += 1; i < size - 1}) {
        if (values(i).time < values(i + 1).time) {
          return true
        } else if (values(i).time > values(i + 1).time) {
          return false
        }
      }
      false
    }
  }

  protected def cancelRequest(contract: C) {}

  override 
  def compare(another: DataServer[V]): Int = {
    if (this.displayName.equalsIgnoreCase(another.displayName)) {
      if (this.hashCode < another.hashCode) -1
      else if (this.hashCode == another.hashCode) 0
      else 1
    } else {
      this.displayName.compareTo(another.displayName)
    }
  }

  override 
  def toString: String = displayName
}

object DataServer extends Publisher {
  private lazy val DEFAULT_ICON: Option[Image] = {
    Option(classOf[DataServer[_]].getResource("defaultIcon.gif")) map {url => Toolkit.getDefaultToolkit.createImage(url)}
  }

  private val config = org.aiotrade.lib.util.config.Config()
  private val heartBeatInterval = config.getInt("dataserver.heartbeat", 318)
  final case class HeartBeat(interval: Long) 
  
  // in context of applet, a page refresh may cause timer into a unpredict status,
  // so it's always better to restart this timer? , if so, cancel it first.
  //    if (timer != null) {
  //      timer.cancel
  //    }
  private val timer = new Timer("DataServer Heart Beat Timer")
  timer.schedule(new TimerTask {
      def run = publish(HeartBeat(heartBeatInterval))
    }, 1000, heartBeatInterval)
}

