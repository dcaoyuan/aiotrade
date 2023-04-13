package org.aiotrade.lib.trading

import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID
import java.util.logging.Logger
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Sec
import scala.collection.mutable

class PaperBroker(val name: String) extends Broker {
  private val log = Logger.getLogger(getClass.getName)
  private val orderIdFormatter = new SimpleDateFormat("yyMMddHHmmssSSS")
  
  val id: Long = UUID.randomUUID.getMostSignificantBits
  
  val allowedSides = List(
    OrderSide.Buy, 
    OrderSide.Sell
  )

  val allowedTypes = List(
    OrderType.Limit, 
    OrderType.Market
  )
  
  val allowedValidity = List(
    OrderValidity.Day
  )

  @throws(classOf[BrokerException])
  def connect {
    // for paper broker, listenTo ticker server
  }

  @throws(classOf[BrokerException])
  def disconnect {
    // for paper broker, deafTo ticker server
  }
  
  @throws(classOf[BrokerException])
  override 
  def cancel(order: Order) {
    executingOrders synchronized {
      val secOrders = executingOrders.getOrElse(order.sec, new mutable.HashSet[Order]())
      secOrders -= order
      if (secOrders.isEmpty) {
        executingOrders -= order.sec
      } else {
        executingOrders(order.sec) = secOrders
      }
    }

    order.status = OrderStatus.Canceled
      
    log.info("Order cancelled: %s".format(order))
        
    publish(OrderDeltasEvent(this, Array(OrderDelta.Updated(order))))
  }

  @throws(classOf[BrokerException])
  override
  def submit(order: Order) {
    executingOrders synchronized {
      val secOrders = executingOrders.getOrElse(order.sec, new mutable.HashSet[Order]())
      secOrders += order
      executingOrders(order.sec) = secOrders
    }

    order.id = orderIdFormatter.format(new Date()).toLong
    order.status = OrderStatus.PendingNew

    log.info("Order submitted: %s".format(order))

    publish(OrderDeltasEvent(this, Array(OrderDelta.Updated(order))))
    
    // for paper work, we assume all orders can be executed sucessfully.
    // for real trading, this should be trigged by returning order executed event 
    processTicker(order.sec, order.time, order.price, order.quantity)
  }  

  def isAllowOrderModify = false
  @throws(classOf[BrokerException])
  def modify(order: Order) {
    throw BrokerException("Modify not allowed", null)
  }
  

  def canTrade(sec: Sec) = {
    true
  }
  
  def getSecurityBySymbol(symbol: String): Sec = {
    Exchange.secOf(symbol).get
  }

  def getSymbolBySecurity(sec: Sec) = {
    sec.uniSymbol
  }

  def toOrder(oc: OrderCompose): Option[Order] = {
    import oc._
    
    if (referIdx >= timestamps.length) {
      println("No order: %s. referIdx %s >= timestamps.length, it's future time, paper work will stop here.".format(
          oc, referIdx, timestamps.length)
      )
      None
    } else {
      val time = timestamps(referIdx)
      ser.valueOf(time) match {
        case Some(quote) =>
          if (side.isOpening) {
          
            if (funds.isSet) {
              funds(math.min(account.tradingRule.maxFundsPerOrder, funds))
            }

            if (account.availableFunds > 0) {
              if (price.notSet) {
                price(account.tradingRule.buyPriceRule(quote))
              }
              if (quantity.notSet) {
                quantity(account.tradingRule.buyQuantityRule(quote, price, funds))
              }
            } else {
              quantity(0.0)
            }
          
          } else { // closing
            
            if (price.notSet) {
              price(account.tradingRule.sellPriceRule(quote))
            }
            if (quantity.notSet) {
              quantity(
                positionOf(sec) match {
                  case Some(position) => 
                    // @Note quantity of position may be negative because of sellShort etc.
                    account.tradingRule.sellQuantityRule(quote, price, math.abs(position.quantity))
                  case None => 0
                }
              )
            }
            
          }
          
          quantity(math.abs(quantity))
          
          if (quantity > 0) {
            val order = Order(account, sec, price, quantity, side, tpe)
            println("Prepared %s".format(order))
            Some(order)
          } else {
            println("No order, since quantity <= 0, something should be wrong! : %s. quantity=%5.2f Quote: volume=%5.2f, average=%5.2f, expenses=%5.2f".format(
                oc, quantity, quote.volume, quote.average, quote.average * account.tradingRule.multiplier * account.tradingRule.marginRate)
            )
            None
          }
          
        case None => 
          println("No order: %s. Quote of this time did not exist.".format(oc))
          if (side.isOpening) {
            // @todo, pend opening order or not ?
          } else {
            // try next freq period
            next (1)
          }
          None
      }
    }
  }
  
  /**
   * For paper work, use this method to receive ticker data to drive orders being executed gradually
   */
  def processTicker(sec: Sec, time: Long, price: Double, quantity: Double) {
    var deltas = List[OrderDelta]()

    val secOrders = executingOrders synchronized {executingOrders.getOrElse(sec, new mutable.HashSet[Order]())}
    
    var toRemove = List[Order]()
    for (order <- secOrders) {
      order.status match {
        case OrderStatus.PendingNew | OrderStatus.Partial =>
          order.tpe match {
            case OrderType.Market =>
              deltas ::= OrderDelta.Updated(order)
              fill(order, time, price, quantity)
                
            case OrderType.Limit =>
              order.side match {
                case (OrderSide.Buy | OrderSide.SellShort) if price <= order.price =>
                  deltas ::= OrderDelta.Updated(order)
                  fill(order, time, price, quantity)
                    
                case (OrderSide.Sell | OrderSide.BuyCover) if price >= order.price => 
                  deltas ::= OrderDelta.Updated(order)
                  fill(order, time, price, quantity)

                case _ =>
              }
                
            case _ =>
          }
          
        case _ =>
      }
        
      if (order.status == OrderStatus.Filled) {
        toRemove ::= order
      }
    }
    
    if (toRemove.nonEmpty) {
      executingOrders synchronized {
        secOrders --= toRemove
        if (secOrders.isEmpty) {
          executingOrders -= sec
        } else {
          executingOrders(sec) = secOrders
        }
      }
    }

    if (deltas.nonEmpty) {
      publish(OrderDeltasEvent(this, deltas))
    }
  }
  
  /**
   * Fill order by price and quantity, and process binding account.
   */
  def fill(order: Order, time: Long, price: Double, quantity: Double) {
    order.account.processTransaction(order, PaperExecution(order, time, price, quantity))
  }

  def accounts: Array[Account] = {
    Array[Account]() // @todo get from db
  }
}
