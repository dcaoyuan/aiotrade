package org.aiotrade.lib.trading


import org.aiotrade.lib.math.timeseries.TStamps
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable

/**
 * Brokers that managed accounts and receive trade to fill orders
 *
 */
abstract class Broker extends Publisher {
  def id: Long
  def name: String

  @throws(classOf[BrokerException])
  def connect: Unit

  @throws(classOf[BrokerException])
  def disconnect: Unit

  /**
   * If order is submitted successfully, the order status should be changed to PendeingNew
   */
  @throws(classOf[BrokerException])
  def submit(order: Order)

  @throws(classOf[BrokerException])
  def cancel(order: Order): Unit

  @throws(classOf[BrokerException])
  def modify(order: Order): Unit
  def isAllowOrderModify: Boolean
  
  def allowedTypes: List[OrderType]
  def allowedSides: List[OrderSide]
  def allowedValidity: List[OrderValidity]
  def canTrade(sec: Sec): Boolean
  def getSecurityBySymbol(symbol: String): Sec
  def getSymbolBySecurity(sec: Sec)
  def accounts: Array[Account]
  
  /**
   * For those secs that are not registed to Exchange, for example a temporary futureIndex
   */
  val specialSecs = new mutable.HashMap[String, Sec]()
  val executingOrders = new mutable.HashMap[Sec, mutable.HashSet[Order]]()
  
  /**
   * Update account's funds, positions etc to newest status
   */
  def updateAccount(account: Account) {}
  
  def toOrder(orderCompose: OrderCompose): Option[Order]
  
  final class SetDouble(v: Double) {   
    def isSet() = !v.isNaN
    def notSet() = v.isNaN
  } 
  
  abstract class OrderCompose {
    def sec: Sec
    def side: OrderSide
    protected def referIdxAtDecision: Int
    
    /**
     * timestamps of refer
     */
    def timestamps: TStamps
    
    /**
     * Quote ser of this sec
     */
    def ser: QuoteSer

    private var _tpe: OrderType = OrderType.Market
    private var _account: TradableAccount = _
    private var _price = Double.NaN
    private var _funds = Double.NaN
    private var _quantity = Double.NaN
    private var _weight = Double.NaN
    private var _plusIdx = 0

    implicit def ToSetDouble(v: Double) = new SetDouble(v)
    
    def tpe = _tpe
    def tpe(tpe: OrderType): this.type = {
      _tpe = tpe
      this
    }
    
    def account = _account
    def using(account: TradableAccount): this.type = {
      _account = account
      this
    }
    
    def price = _price
    def price(price: Double): this.type = {
      _price = price
      this
    }

    def funds = _funds
    def funds(funds: Double): this.type = {
      _funds = funds
      this
    }
    
    def quantity = _quantity
    def quantity(quantity: Double): this.type = {
      _quantity = quantity
      this
    }
        
    /**
     * You can assign weight instead of funds or quantity, and let trading service to
     * calculate the actual funds according to total available funds and weights of
     * each target issue
     */
    def weight = _weight
    def weight(weight: Double): this.type = {
      _weight = weight
      this
    }

    /** on t + idx */
    def next(i: Int): this.type = {
      _plusIdx += i
      this
    }
    
    /**
     * @Note This referIdx may may point to future and.exceed timestamps' lastIdx.
     *   So be carefully to call timestamps(referIdx)
     */
    def referIdx = referIdxAtDecision + _plusIdx

    override 
    def toString = {
      "OrderCompose(account=%1$s, referIdx=%2$s, %3$s, %4$s, %5$10.2f, %6$d, %7$5.2f)".format(_account.code, referIdx, sec.uniSymbol, side, _funds, _quantity.toInt, _price)
    }

    def positionOf(sec: Sec): Option[Position] = {
      account.positions.get(sec)
    }
  }
}

final case class BrokerException(message: String, cause: Throwable) extends Exception(message, cause)

sealed trait OrderDelta {
  def order: Order
}
object OrderDelta {
  final case class Added(order: Order) extends OrderDelta
  final case class Removed(order: Order) extends OrderDelta
  final case class Updated(order: Order) extends OrderDelta  
}

final case class OrderDeltasEvent(broker: Broker, deltas: Seq[OrderDelta])

