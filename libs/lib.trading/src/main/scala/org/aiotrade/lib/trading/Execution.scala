package org.aiotrade.lib.trading

/**
 * 
 * @author Caoyuan Deng
 */
trait Execution {
  val order: Order
  val time: Long
  val price: Double
  val quantity: Double
  val amount: Double
  val expenses: Double
}

/**
 * PaperExecution needs to calculate amount and expenses by others
 */
final case class PaperExecution(order: Order, time: Long, price: Double, quantity: Double) extends Execution {
  val amount = Double.NaN
  val expenses = Double.NaN
}

/**
 * 
 * FullExecution is usaully from a real trading service, which report the executions with amount and expenses
 */
final case class FullExecution(order: Order, time: Long, price: Double, quantity: Double, amount: Double, expenses: Double) extends Execution