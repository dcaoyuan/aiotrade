package org.aiotrade.lib.trading

import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.Exchange

trait ExpenseScheme {
  /**
   * @param price
   * @param quantity
   * @param sec, may be null
   */
  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec): Double

  /**
   * @param price
   * @param quantity
   * @param sec, may be null
   */
  def getClosingExpenses(price: Double, quantity: Double, sec: Sec): Double
}

object ExpenseScheme {
  val LimitedProportional1Scheme = LimitedProportionalScheme(0.05, 5, 100)
  val LimitedProportional2Scheme = LimitedProportionalScheme(0.05, 5, Double.PositiveInfinity) // no maximum limit
}

object NoExpensesScheme extends ExpenseScheme {
  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec) = 0.0
  def getClosingExpenses(price: Double, quantity: Double, sec: Sec) = 0.0

  override 
  def hashCode = 11 * toString.hashCode

  override
  def toString = "None expenses scheme"
}

final case class SimpleFixedScheme(expenses: Double = 9.95) extends ExpenseScheme {
  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec) = expenses
  def getClosingExpenses(price: Double, quantity: Double, sec: Sec) = expenses
}


final case class LimitedProportionalScheme(percentage: Double, minimum: Double, maximum: Double) extends ExpenseScheme {

  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    var expenses = quantity * price / 100.0 * percentage
    if (expenses < minimum) {
      expenses = minimum
    }
    if (expenses > maximum) {
      expenses = maximum
    }
    expenses
  }

  def getClosingExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    var expenses = quantity * price / 100.0 * percentage
    if (expenses < minimum) {
      expenses = minimum
    }
    if (expenses > maximum) {
      expenses = maximum
    }
    expenses
  }
}

/**
 * Sample params:
 * @param level1 = 0.01
 * @param level1quantity = 500
 * @param level2 = 0.005
 * @param minimum = 1.0
 */
final case class TwoLevelsPerShareScheme(level1: Double, level1quantity: Double, level2: Double, minimum: Double) extends ExpenseScheme {

  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    var expenses = level1 * (if (quantity > level1quantity) level1quantity else quantity)
    if (quantity > level1quantity) {
      expenses += level2 * (quantity - level1quantity)
    }
    if (expenses < minimum) {
      expenses = minimum
    }
    expenses
  }

  def getClosingExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    var expenses = level1 * (if (quantity > level1quantity) level1quantity else quantity)
    if (quantity > level1quantity) {
      expenses += level2 * (quantity - level1quantity)
    }
    if (expenses < minimum) {
      expenses = minimum
    }
    expenses
  }
}

/**
 * ﻿DTS生产环境里股指期货保证金比例为15%，交易费用如下：
 * ETF：0.45%%，
 * 股指期货：0.28%%。
 * 股票：上海：印花：0.1%，经手：0.696%%，证管：0.2%%，过户（按面值计算）：3%%
 *      深圳：印花：0.1%，经手：0.951%%，证管：0.2%% 
 * @param commissionRate, 佣金(券商).      Applied on both sides of sell and buy, usally 0.5% - 0.03%, 0.08%
 * @param stamptaxRate,   印花税(交易所).   Applied on sell side, 0.1% 
 * @param regulatoryRate, 证管费(交易所).   Applied on both sides, 0.2%%
 * @param brokerageRate,  经手费(交易所).   Applied on both sides, 0.696%%
 * @param transferFee,    过户费(登记公司). Applied on both sides, Shanghai: RMB0.3 per 1000 quantity, Shenzhen 0.0
 * @param minCommissionFee, Usally RMB5.0 
 */
final case class ChinaStocksExpenseScheme(commissionRate: Double, 
                                          stamptaxRate: Double = 0.001, regulatoryRate: Double = 0.00002, brokerageRate: Double = 0.0000696, 
                                          SHTransferFee: Double = 0.3, SZTransferRate: Double = 0.0000225,
                                          minCommissionFee: Double = 5.0
) extends ExpenseScheme {
  
  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    val amount = price * quantity
    
    val transferFee = Exchange.exchangeOf(sec.uniSymbol) match {
      case Exchange.SS => SHTransferFee * (quantity / 1000 + 1)
      case Exchange.SZ => SZTransferRate * amount
    }
    
    regulatoryRate * amount +
    math.max(commissionRate * amount, minCommissionFee) + 
    transferFee
  }
  
  def getClosingExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    val amount = price * quantity
    
    val transferFee = Exchange.exchangeOf(sec.uniSymbol) match {
      case Exchange.SS => SHTransferFee * (quantity / 1000 + 1)
      case Exchange.SZ => SZTransferRate * amount
    }

    stamptaxRate * amount +
    regulatoryRate * amount +
    math.max(commissionRate * amount, minCommissionFee) + 
    transferFee
  }
}

final case class ChinaFuturesExpenseScheme(commissionRate: Double = 0.000028, stamptaxRate: Double = 0.00005) extends ExpenseScheme {
  
  def getOpeningExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    val amount = quantity * price
    (commissionRate + stamptaxRate) * amount
  }

  def getClosingExpenses(price: Double, quantity: Double, sec: Sec): Double = {
    val amount = quantity * price
    (commissionRate + stamptaxRate) * amount
  }
}