package org.aiotrade.lib.trading

import java.util.Currency
import java.util.Locale
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.util.actors.Publisher
import scala.collection.mutable

/**
 *
 * @Note Currency.getInstance(Locale.getDefault) may cause exception on some OSs,
 * So it's better to assign currency explictly.
 * 
 * @author Caoyuan Deng
 */
abstract class Account(val code: String, protected var _balance: Double, 
                       val currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Publisher {
  protected val _transactions = new ArrayList[TradeTransaction]()
  def transactions = _transactions.toArray
  
  val initialEquity = _balance
  def balance = _balance
  def balance_=(balance: Double) {
    _balance = balance
  }
  def credit(funds: Double) {_balance += funds}
  def debit (funds: Double) {_balance -= funds}

  def equity: Double
  def availableFunds: Double
}

abstract class TradableAccount($code: String, $balance: Double, val tradingRule: TradingRule, 
                               $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Account($code, $balance, $currency) {
  private val log = Logger.getLogger(this.getClass.getName)
  
  protected var _secToPosition = new mutable.HashMap[Sec, Position]()
  def positions = _secToPosition
  def positions_=(secToPosition: mutable.HashMap[Sec, Position]) {
    _secToPosition = secToPosition
  }

  def positionOf(sec: Sec): Option[Position] = _secToPosition.get(sec)
  def positionEquity   = _secToPosition.foldRight(0.0){(x, s) => s + x._2.equity}
  def positionGainLoss = _secToPosition.foldRight(0.0){(x, s) => s + x._2.gainLoss}

  def calcFundsToOpen(price: Double, quantity: Double, sec: Sec = null): Double
  
  /**
   * Considered both expenses and quantityPerLot
   */
  def calcQuantityToOpen(price: Double, funds: Double, sec: Sec) = {
    var quantity = (funds / price).toInt / tradingRule.quantityPerLot * tradingRule.quantityPerLot
    while (calcFundsToOpen(price, funds, sec) > funds) {
      quantity -= tradingRule.quantityPerLot
    }
    
    quantity
  }
  
  /**
   * Helper method
   *
   * @param equity
   * @param price
   * @return corresponding quantity
   */
  def toQuantity(equity: Double, price: Double): Double = {
    math.round(equity / (price * tradingRule.multiplier))
  }
  
  /**
   * Helper method
   * 
   * @param quantity
   * @param price
   * @return corresponding equity
   */
  def toEquity(quantity: Double, price: Double): Double = {
    math.abs(quantity) * price * tradingRule.multiplier
  }

  /**
   * @return  amount with signum
   */
  protected def calcSecTransactionAmount(order: Order, execution: Execution): Double
  
  def processTransaction(order: Order, execution: Execution) {
    order.fill(execution.price, execution.quantity)
    
    val transaction = toTransaction(order, execution)
    _transactions += transaction
    
    log.info(transaction.toString)
    
    _balance += transaction.amount

    for (SecTransaction(time, price, quantity, amount, order) <- transaction.secTransactions; sec = order.sec) {
      _secToPosition.get(sec) match {
        case None => 
          val position = Position(this, time, sec, price, quantity)
          _secToPosition(sec) = position
          publish(PositionOpened(position))
        
        case Some(position) =>
          position.add(time, price, quantity)
          if (position.quantity == 0) {
            _secToPosition -= sec
            publish(PositionClosed(position))
          } else {
            publish(PositionChanged(position))
          }
      }
    }
  }
  
  private def toTransaction(order: Order, execution: Execution): TradeTransaction = {
    execution match {
      case PaperExecution(order, time, price, quantity) =>
        val transactionAmount = calcSecTransactionAmount(order, execution)
        // @Note transactionAmount/fillingQuantity should consider signum for SecurityTransaction, which causes money in/out, position increase/decrease.
        val secTransaction = SecTransaction(time, price, order.side.signum * quantity, transactionAmount, order)
      
        val expenses = if (order.side.isOpening)  
          tradingRule.expenseScheme.getOpeningExpenses(price, quantity, order.sec)
        else
          tradingRule.expenseScheme.getClosingExpenses(price, quantity, order.sec)
        val expensesTransaction = ExpensesTransaction(time, -expenses, order)
    
        TradeTransaction(time, Array(secTransaction), expensesTransaction, order)
        
      case FullExecution(order, time, price, quantity, amount, expenses) =>
        val transactionAmount = -order.side.signum * amount
        // @Note transactionAmount/fillingQuantity should consider signum for SecurityTransaction, which causes money in/out, position add/remove.
        val secTransaction = SecTransaction(time, price, order.side.signum * quantity, transactionAmount, order)
        
        TradeTransaction(time, Array(secTransaction), ExpensesTransaction(time, -expenses, order), order)
    }
  }
  
}

class StockAccount($code: String, $balance: Double, $tradingRule: TradingRule, 
                   $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends TradableAccount($code, $balance, $tradingRule, $currency) {

  def equity = _balance + positionEquity
  def availableFunds = _balance
  
  def calcFundsToOpen(price: Double, quantity: Double, sec: Sec) = {
    quantity * price + tradingRule.expenseScheme.getOpeningExpenses(price, quantity, sec)
  }
  
  protected def calcSecTransactionAmount(order: Order, execution: Execution): Double = {
    -order.side.signum * (execution.price * tradingRule.multiplier * tradingRule.marginRate) * execution.quantity
  }

  override 
  def toString = "%1$s\t: availableFunds=%2$.0f, equity=%3$.0f, positionEquity=%4$.0f, positionGainLoss=%5$.0f, positions=%6$s".format(
    code, availableFunds, equity, positionEquity, positionGainLoss, positions.values.size
  )
}

class FutureAccount($code: String, $balance: Double, $tradingRule: TradingRule,
                    $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends TradableAccount($code, $balance, $tradingRule, $currency) {
  
  def riskLevel = positionMargin / equity * 100
  def positionMargin = positionEquity * tradingRule.marginRate

  def equity = _balance + positionGainLoss
  def availableFunds = equity - positionMargin
  
  def calcFundsToOpen(price: Double, quantity: Double, sec: Sec) = {
    quantity * price * tradingRule.multiplier * tradingRule.marginRate + 
    tradingRule.expenseScheme.getOpeningExpenses(price * tradingRule.multiplier, quantity, sec)
  }
  
  protected def calcSecTransactionAmount(order: Order, execution: Execution): Double = {
    if (order.side.isOpening) {
      // we won't minus the margin from balance, since the margin was actually not taken from balance, instead, availableFunds will minus margin.
      0.0 
    } else { // is to close some positions
      _secToPosition.get(order.sec) match {
        case Some(position) =>
          // calculate offset gain loss of closed position right now
          -order.side.signum * (execution.price - position.price) * execution.quantity * tradingRule.multiplier
        case _ => 0.0 // This should not happen!
      }
    }
  }

  override 
  def toString = "%1$s\t: availableFunds=%2$.0f, equity=%3$.0f, positionEquity=%4$.0f, positionGainLoss=%5$.0f, positions=%6$s, positionMargin=%7$.0f, risk=%8$.2f%%".format(
    code, availableFunds, equity, positionEquity, positionGainLoss, positions.values.map(_.quantity).mkString("(", ",", ")"), positionMargin, riskLevel
  )
}

class CashAccount($code: String, $balance: Double,
                  $currency: Currency = Currency.getInstance(Locale.getDefault)
) extends Account($code, $balance, $currency) {

  def equity = _balance
  def availableFunds: Double = _balance

  override 
  def toString = "%1$s\t: availableFunds=%2$.0f".format(
    code, availableFunds
  )

}