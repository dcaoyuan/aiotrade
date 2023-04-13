/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import scala.collection.mutable
import org.aiotrade.lib.math.timeseries.TVal
import java.util.Collections
import java.util.logging.Level
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import java.util.Calendar
import org.aiotrade.lib.math.timeseries.TFreq

final class PriceDistribution extends BelongsToSec with TVal with Flag with Serializable {

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {this._time = time}

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {this._flag = flag}

  private val data = new Array[Double](4)

  def price = data(0)
  def volumeUp = data(1)
  def volumeDown = data(2)
  def volumeEven = data(3)

  def price_= (value: Double){ data(0) = value}
  def volumeUp_= (value: Double){ data(1) = value}
  def volumeDown_= (value: Double){ data(2) = value}
  def volumeEven_= (value: Double){ data(3) = value}

  def copyFrom(another: PriceDistribution) {
    this.sec = another.sec
    this.time = another.time
    this.flag = another.flag
    System.arraycopy(another.data, 0, data, 0, data.length)
  }

  override def toString() = {
    val sp = new StringBuffer
    sp.append("unisymbol:").append(uniSymbol)
    sp.append(",time:").append(time)
    sp.append(",price:").append(price)
    sp.append(",volumeUp:").append(volumeUp)
    sp.append(",volumeDown:").append(volumeDown)
    sp.append(",volumeEven:").append(volumeEven)
    sp.toString
  }
}

@serializable
final class PriceCollection extends BelongsToSec with TVal with Flag  {
  @transient
  val cal = Calendar.getInstance
  private var map = mutable.Map[String, PriceDistribution]()

  var isTransient = true

  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {this._time = time}

  private var _flag: Int = 1 // dafault is closed
  def flag = _flag
  def flag_=(flag: Int) {this._flag = flag}

  private val data = new Array[Double](2)

  def avgPrice = data(0)
  def totalVolume = data(1)

  private def avgPrice_= (value: Double){ data(0) = value}
  private def totalVolume_= (value: Double){ data(1) = value}

  def get(price: String) = map.get(price)
    
  def put(price: String, pd: PriceDistribution) = {
    if (map.isEmpty){
      this.time = pd.time
      this.sec = pd.sec
      this.flag = pd.flag
    }

    if (TFreq.DAILY.round(this.time, cal) == TFreq.DAILY.round(pd.time, cal)){
      val vol0 = map.get(price) match{
        case Some(pr) => pr.volumeDown + pr.volumeEven + pr.volumeUp
        case None => 0
      }
      
      val vol = pd.volumeUp + pd.volumeDown + pd.volumeEven
      avgPrice = (avgPrice * totalVolume + pd.price * (vol - vol0)) / (totalVolume + vol - vol0)
      totalVolume += vol - vol0
      pd.flag = flag
      map.put(price, pd)
      if (this.closed_?) pd.closed_! else pd.unclosed_!
    }
  }

  def keys = map.keys

  def values = map.values

  def clear = {
    map.clear
    var i= -1
    while ({i += 1; i< data.length}) data(i) = 0.0
  }

  def isEmpty = map.isEmpty

  override def closed_! = {
    super.closed_!
    this.map.values foreach (_.closed_!)
  }

  override def unclosed_! = {
    super.unclosed_!
    this.map.values foreach (_.unclosed_!)
  }

  override def justOpen_! = {
    super.justOpen_!
    this.map.values foreach (_.justOpen_!)
  }

  override def unjustOpen_! = {
    super.unjustOpen_!
    this.map.values foreach (_.unjustOpen_!)
  }

  override def fromMe_! = {
    super.fromMe_!
    this.map.values foreach (_.fromMe_!)
  }

  override def unfromMe_! = {
    super.unfromMe_!
    this.map.values foreach (_.unfromMe_!)
  }
  
  override def toString() ={
    val sp = new StringBuffer
    sp.append("\nunisymbol:").append(uniSymbol)
    sp.append("\ntime:").append(time)
    sp.append("\navgPrice:").append(avgPrice)
    sp.append("\ntotalVolume:").append(totalVolume)
    for((key, value) <- this.map) sp.append("\n").append(value.toString)
    sp.toString
  }
}


object PriceDistributions  extends Table[PriceDistribution] {
  private val log = Logger.getLogger(this.getClass.getName)
  private val SIZE = 200

  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val time = "time" BIGINT()
  val price = "price" DOUBLE()
  val volumeUp = "volumeUp" DOUBLE()
  val volumeDown = "volumeDown" DOUBLE()
  val volumeEven = "volumeEven" DOUBLE()
  val flag = "flag" INTEGER()

  private val dailyCache = mutable.Map[(Sec, Long), PriceCollection]()

  @deprecated
  def dailyDistribuOf(sec: Sec, date: Long): PriceCollection ={
    dailyCache.get(sec -> date) match {
      case Some(map) => map
      case None =>
        val map = new PriceCollection

        dailyCache.put(sec -> date, map)

        try{
          (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
          ) foreach {x => map.put(x.price.toString, x)}
        }
        catch{
          case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
        }

        map.isTransient = map.isEmpty
        map.time = date
        map.sec = sec
        if (map.isTransient){
          map.unclosed_!
          map.justOpen_!
          map.fromMe_!
        }
        map
    }
  }

  @deprecated
  def dailyDistribuOf_ignoreCache(sec: Sec, date: Long): PriceCollection ={
    val map = new PriceCollection
    try{
      (SELECT (this.*) FROM this WHERE ((this.time EQ date) AND (this.sec.field EQ Secs.idOf(sec))) list
      ) foreach {x => map.put(x.price.toString, x)}
    }
    catch{
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }

    map.time = date
    map.sec = sec
    map.isTransient = map.isEmpty
    if (map.isTransient){
      map.unclosed_!
      map.justOpen_!
      map.fromMe_!
    }
    map
  }

  @deprecated
  def dailyDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    seqToMap(
      try{
        (SELECT (this.*) FROM (this) WHERE (
            this.sec.field EQ Secs.idOf(sec)
          ) ORDER_BY (this.time) list
        )
      }
      catch{
        case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      })
  }

  def closedDistribuOf(sec: Sec): mutable.Map[Long, PriceCollection] = {
    val map = mutable.Map[Long, PriceCollection]()
    (try{
        SELECT (this.*) FROM (this) WHERE (
          this.sec.field EQ Secs.idOf(sec)
        ) ORDER_BY (this.time) list
      }
     catch{
        case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      }
    ) foreach {x =>
      if (x.closed_?){
        map.get(x.time) match{
          case Some(m) => m.isTransient = false; m.put(x.price.toString, x)
          case None =>
            val m = new PriceCollection
            m.isTransient = false
            map.put(x.time, m)
            m.time = x.time
            m.sec = sec
            m.put(x.price.toString, x)
        }
      }
    }

    log.info("Load price collection from DB:" + map.size)
    map
  }

  @deprecated
  def closedDistribuOf__filterByDB(sec: Sec): mutable.Map[Long, PriceCollection]= {
    seqToMap(
      try{
        SELECT (this.*) FROM (this) WHERE (
          (this.sec.field EQ Secs.idOf(sec)) AND (ORM.dialect.bitAnd(this.relationName + ".flag", Flag.MaskClosed) EQ Flag.MaskClosed)
        ) ORDER_BY (this.time) list
      }
      catch{
        case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
      }
    )
  }

  /**
   * Convert the data format
   * The Price Distribution's format is:
   *
   *  ----------------------------------------------------------------------------------
   *  |    Security |    Date     |  Price  |  Volume up  | Volume down | Volume even  |
   *  ----------------------------------------------------------------------------------
   *  |   600001.S  |  2011-01-01 |   9.05  |   100000    |     10000   |    10000     |
   *  |             |             |   9.04  |   100000    |     10000   |    10000     |
   *  |             |             |   9.03  |   100000    |     100000  |    10000     |
   *  |             --------------------------------------------------------------------
   *  |             |  2011-01-02 |   9.04  |   100000    |     10000   |    10000     |
   *  |             |             |   9.03  |   100000    |     100000  |    10000     |
   *  ----------------------------------------------------------------------------------
   *  |  600002.SS  |  2011-01-01 |   10.87 |   10000     |     10000   |    10000     |
   *  |             |             |   10.88 |   10000     |     10000   |    10000     |
   *  |                           ......                                |              |
   *  |                                                                 |              |
   *  ----------------------------------------------------------------------------------
   */
  private def seqToMap(list: Seq[PriceDistribution]): mutable.Map[Long, PriceCollection] = {
    val map = mutable.Map[Long, PriceCollection]()
    list foreach {x =>
      map.get(x.time) match{
        case Some(m) => m.isTransient = false; m.put(x.price.toString, x)
        case None =>
          val m = new PriceCollection
          m.isTransient = false
          map.put(x.time, m)
          m.time = x.time
          m.sec = x.sec
          m.put(x.price.toString, x)
      }
    }
    map
  }

  def saveBatch(sec: Sec, sortedPDs: Seq[PriceCollection]) {
    if (sortedPDs.isEmpty) return

    val head = sortedPDs.head
    val last = sortedPDs.last
    val frTime = math.min(head.time, last.time)
    val toTime = math.max(head.time, last.time)
    val exists = mutable.Map[(Long, Double), PriceDistribution]()
    (SELECT (this.*) FROM (this) WHERE (
        (this.sec.field EQ Secs.idOf(sec)) AND (this.time GE frTime) AND (this.time LE toTime)
      ) ORDER_BY (this.time) list
    ) foreach {x => exists.put(x.time -> x.price, x)}

    val updates = new ArrayList[PriceDistribution]()
    val inserts = new ArrayList[PriceDistribution]()

    sortedPDs.foreach{pc =>
      val (u, i) = pc.values.partition(x => exists.contains(x.time -> x.price))
      updates ++= sortAndMergingPriceDistribution(u, SIZE)
      inserts ++= sortAndMergingPriceDistribution(i, SIZE)
    }

    for (x <- updates) {
      val existOne = exists(x.time -> x.price)
      existOne.copyFrom(x)
    }

    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }

  def saveBatch(atSameTime: Long, pcs: Array[PriceCollection]) {
    if (pcs.isEmpty) return

    val exists = mutable.Map[(Sec, Double),PriceDistribution]()
    val res = try {
      SELECT (this.*) FROM (this) WHERE (
        (this.time EQ atSameTime) AND (this.sec.field GT 0) AND (this.sec.field LT CRCLongId.MaxId )
      ) list()
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex); Nil
    }
    res foreach {x => exists.put(x.sec -> x.price, x)}

    val updates = new ArrayList[PriceDistribution]()
    val inserts = new ArrayList[PriceDistribution]()

    pcs.foreach{pc =>
      val (u, i) = pc.values partition {x => exists.contains(x.sec -> x.price)}
      updates ++= sortAndMergingPriceDistribution(u, SIZE)
      inserts ++= sortAndMergingPriceDistribution(i, SIZE)
    }

    try {
      if (updates.length > 0) {
        this.updateBatch_!(updates.toArray)
      }
      if (inserts.length > 0) {
        this.insertBatch_!(inserts.toArray)
      }
    } catch {
      case ex: Throwable => log.log(Level.SEVERE, ex.getMessage, ex)
    }
  }
  
  /**
   * Merge the prices' size to size.
   * @param list The price collection needed to be merge.
   * @param size The limited size.
   * @return The merged price collection.
   */
  def sortAndMergingPriceDistribution(list: Iterable[PriceDistribution], size: Int = 60): Iterable[PriceDistribution] = {

    if (list == null || list.size <= size) return list

    val list1 = new java.util.ArrayList[PriceDistribution]()
    val sec = list.head.sec
    val time = list.head.time
    list foreach {x =>
      if (x.sec != sec || x.time != time) throw new Exception("The security and the time is not equal.")
      list1.add(x)
    }

    val retnList = ArrayList[PriceDistribution]()
    
    Collections.sort[PriceDistribution](list1, null)
    var i = 0
    var j = 0
    var pd1 = list1.get(0)
    val interval = (list1.get(list1.size - 1).price - list1.get(0).price) / (size - 1)
    while(i < size){
      val pd = new PriceDistribution
      pd.sec = sec
      pd.time = time
      pd.flag = pd1.flag
      pd.price = list1.get(0).price + i * interval
      while(j < list1.size && pd1.price <= pd.price + interval / 2){
        pd.volumeEven += pd1.volumeEven
        pd.volumeDown += pd1.volumeDown
        pd.volumeUp += pd1.volumeUp
        j += 1
        if (j < list1.size) pd1 = list1.get(j)
      }

      if (pd.volumeDown > 1e-6 || pd.volumeUp > 1e-6 || pd.volumeEven > 1e-6) retnList += pd
      i += 1
    }
    return retnList
  }

}
