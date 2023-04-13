package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import java.util.Date
import java.util.UUID

trait Transaction {
  def id: Long
  def time: Long
  def amount: Double
  def subTransactions: Array[Transaction]
  
  /**
   * Gets the order that originated this transaction, if any.
   *
   * @return the order reference, or <code>null</code> if the transaction wasn't
   * originated from an order.
   */
  def order: Order
}

final case class ExpensesTransaction(time: Long, amount: Double, order: Order) extends Transaction {
  def this(amount: Double) = this(System.currentTimeMillis, amount, null)

  val id = UUID.randomUUID.getMostSignificantBits
  val subTransactions: Array[Transaction] = Array[Transaction]()
  
  override 
  def toString = {
    "ExpenseTransaction(time=%1$tY.%1$tm.%1$td, amount=%2$ 10.2f)".format(
      new Date(time), amount
    )
  }
}

/**
 * @Note quantity and amount should consider signum according to the side
 */
final case class SecTransaction(time: Long, price: Double, quantity: Double, amount: Double, order: Order) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits
  val subTransactions: Array[Transaction] = Array[Transaction]()

  override
  def toString = {
    "Transaction(time=%1$tY.%1$tm.%1$td, sec=%2$s, side=%3$s, price=%4$ 10.2f, quantity=%5$ 10.2f, amount=%6$ 10.2f)".format(
      new Date(time), order.sec.uniSymbol, order.side, price, quantity, amount
    )
  }
}

final case class TradeTransaction(time: Long, secTransactions: Array[SecTransaction], expensesTransaction: ExpensesTransaction, order: Order) extends Transaction {
  val id = UUID.randomUUID.getMostSignificantBits

  val subTransactions = {
    val xs = new ArrayList[Transaction]() ++= secTransactions
    xs += expensesTransaction
    
    xs.toArray
  }

  val secAmount = {
    var sum = 0.0
    var i = 0
    while (i < secTransactions.length) {
      sum += secTransactions(i).amount
      i += 1
    }
    sum
  }
  
  val expensesAmount = expensesTransaction.amount
  
  val amount = secAmount + expensesAmount
  
  override
  def toString = {
    "Transaction(time=%1$tY.%1$tm.%1$td, sec=%2$s, side=%3$s, secAmount=%4$ 10.2f, expenses=%5$ 10.2f, amount=%6$ 10.2f)".format(
      new Date(time), order.sec.uniSymbol, order.side, secAmount, expensesAmount, amount
    )
  }
}
