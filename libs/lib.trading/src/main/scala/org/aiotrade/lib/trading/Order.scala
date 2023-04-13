package org.aiotrade.lib.trading

import java.util.Date
import java.util.logging.Logger
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher

/**
 * quantity should always be >= 0
 */
final case class Order(account: TradableAccount, sec: Sec, price: Double, var quantity: Double, side: OrderSide, tpe: OrderType = OrderType.Market, var funds: Double = Double.NaN) extends Publisher {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _id: Long = _
  
  private var _time: Long = 0
  private var _expireTime: Long = Long.MinValue
  private var _stopPrice: Double = Double.NaN
  private var _validity: OrderValidity = _
  private var _reference: String = _
  
  // --- executing related
  
  private var _filledQuantity: Double = _
  private var _averagePrice: Double = _

  private var _status: OrderStatus = OrderStatus.New
  private var _message: String = _
  
  def id = _id
  def id_=(id: Long) {
    this._id = id
  }

  def time = _time
  def time_=(time: Long) {
    this._time = time
  }
  
  def stopPrice = _stopPrice
  def stopPrice_=(stopPrice: Double) {
    this._stopPrice = stopPrice
  }
  
  def validity = _validity
  def validity_=(validity: OrderValidity) {
    this._validity = validity
  }

  def expireTime = _expireTime
  def expireTime_=(time: Long) {
    this._expireTime = time
  }

  def reference = _reference
  def reference_=(reference: String) {
    this._reference = reference
  }

  // --- executing related
  
  def status = _status
  def status_=(status: OrderStatus) {
    if (_status != status) {
      val oldValue = _status
      _status = status
      publish(OrderEvent.StatusChanged(this, oldValue, status))
      if (status == OrderStatus.Filled) {
        publish(OrderEvent.Completed(this))
      }
    }
  }

  def remainQuantity = quantity - _filledQuantity
  def filledQuantity = _filledQuantity
  def filledQuantity_=(filledQuantity: Double) {
    val oldValue = _filledQuantity
    if (filledQuantity != Long.MinValue && filledQuantity != _filledQuantity) {
      _filledQuantity = filledQuantity
      publish(OrderEvent.FilledQuantityChanged(this, oldValue, filledQuantity))
    }
  }

  def averagePrice = _averagePrice
  def averagePrice_=(averagePrice: Double) {
    val oldValue = _averagePrice
    if (averagePrice != Double.NaN && averagePrice != _averagePrice) {
      _averagePrice = averagePrice
      publish(OrderEvent.AveragePriceChanged(this, oldValue, averagePrice))
    }
  }
  
  def message = _message
  def message_=(message: String) {
    _message = message
  }
  
  /**
   * Fill order by price and quantity
   */
  def fill(price: Double, quantity: Double) {
    if (quantity > 0) {
      var oldTotalAmount = _filledQuantity * _averagePrice
      _filledQuantity += quantity
      _averagePrice = (oldTotalAmount + price * quantity) / _filledQuantity

      status = if (remainQuantity <= 0) {
        OrderStatus.Filled
      } else {
        // if quantity is NaN, (remainQuantity <= 0) is always false @Todo how to deal with order in this case.
        OrderStatus.Partial
      }

      log.info("Filling order with price=%1$ 5.2f, quantity=%2$ 5.2f. After filled %3$s".format(price, quantity, this))
    } else {
      log.warning("Filling quantity <= 0: fillingPrice=%s, fillingQuantity=%s, remainQuantity=%s".format(price, quantity, remainQuantity))
    }
  }
  
  override
  def toString = {
    "Order(time=%1$tY.%1$tm.%1$td, sec=%2$s, tpe=%3$s, side=%4$s, quantity(filled)=%5$s(%6$s), price=%7$ 5.2f, funds=%8$ 5.2f, status=%9$s, stopPrice=%10$ 5.2f, validity=%11$s, expiration=%12$s, refrence=%13$s)".format(
      new Date(time), sec.uniSymbol, tpe, side, quantity, _filledQuantity, price, funds, status, stopPrice, validity, expireTime, reference
    )
  }
}

/**
 * @param name
 * @param +/- quantity
 * @param is to open or close a position
 */
sealed abstract class OrderSide(val name: String, val signum: Int, val isOpening: Boolean)
object OrderSide {
  final case object Buy       extends OrderSide("Buy",        1, true)
  final case object BuyCover  extends OrderSide("BuyCover",   1, false)
  final case object Sell      extends OrderSide("Sell",      -1, false)  
  final case object SellShort extends OrderSide("SellShort", -1, true)
}

sealed abstract class OrderType(val name: String)
object OrderType {
  final case object Market extends OrderType("Market")
  final case object Limit extends OrderType("Limit")
  final case object Stop extends OrderType("Stop")
  final case object StopLimit extends OrderType("StopLimit")
}

sealed abstract class OrderValidity(val name: String)
object OrderValidity {
  final case object Day extends OrderValidity("Day")
  final case object ImmediateOrCancel extends OrderValidity("ImmediateOrCancel")
  final case object AtOpening extends OrderValidity("AtOpening")
  final case object AtClosing extends OrderValidity("AtClosing")
  final case object GoodTillCancel extends OrderValidity("GoodTillCancel")
  final case object GoodTillDate extends OrderValidity("GoodTillDate")
}

sealed abstract class OrderStatus(val name: String)
object OrderStatus {
  final case object New extends OrderStatus("New")
  final case object PendingNew extends OrderStatus("PendingNew")
  final case object Partial extends OrderStatus("Partial")
  final case object Filled extends OrderStatus("Filled")
  final case object Canceled extends OrderStatus("Canceled")
  final case object Rejected extends OrderStatus("Rejected")
  final case object PendingCancel extends OrderStatus("PendingCancel")
  final case object Expired extends OrderStatus("Expired")
}

sealed trait OrderEvent {
  def order: Order
}
object OrderEvent {
  final case class Completed(order: Order) extends OrderEvent
  final case class IdChanged(order: Order, oldValue: String, value: String) extends OrderEvent
  final case class StatusChanged(order: Order, oldValue: OrderStatus, value: OrderStatus) extends OrderEvent
  final case class FilledQuantityChanged(order: Order, oldValue: Double, value: Double) extends OrderEvent
  final case class AveragePriceChanged(order: Order, oldValue: Double, value: Double) extends OrderEvent 
}
