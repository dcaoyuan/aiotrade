package org.aiotrade.lib.trading

import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.securities.model.Sec

class Position private (private var _account: TradableAccount, 
                        private var _time: Long, 
                        private var _sec: Sec, 
                        private var _price: Double, 
                        private var _quantity: Double
) {
  def this() = this(null, Long.MinValue, null, Double.NaN, Double.NaN) /* for serializable */  

  private var _subPositions: ArrayList[Position] = null
  private var _currentPrice = _price
  private var _maxGainLossRatio = 0.0
  private var _minGainLossRatio = 0.0
  
  def subPositions: Array[Position] = if (_subPositions == null) Array() else _subPositions.toArray

  def account = _account
  
  def sec = _sec
  def sec_=(sec: Sec) {
    _sec = sec
    if (_subPositions != null) {
      var i = 0
      while (i < _subPositions.length) {
        val pos = _subPositions(i)
        pos._sec = sec
        i += 1
      }
    }
  }
  
  def time = _time
  def time_=(time: Long) = {
    _time = time
  }
  
  def quantity = _quantity
  def quantity_=(quantity: Double) {
    _quantity = quantity
  }
  
  def price = _price
  def price_=(price: Double) {
    _price = price
  }
  
  def currentPrice = _currentPrice
  
  def update(currentPrice: Double) {
    if (!currentPrice.isNaN) {
      _currentPrice = currentPrice
      _maxGainLossRatio = math.max(_maxGainLossRatio, gainLossRatio)
      _minGainLossRatio = math.min(_minGainLossRatio, gainLossRatio)
    }
  }
  
  def add(time: Long, price: Double, quantity: Double) {
    _subPositions = if (_subPositions == null) new ArrayList[Position]() else _subPositions
    _subPositions += Position(account, time, sec, quantity, price)
    
    if (math.signum(quantity) == math.signum(_quantity) || math.signum(_quantity) == 0.0) {
      val total = _quantity * _price + quantity * price
      _quantity += quantity
      _price = total / _quantity
    } else {
      _quantity += quantity
      if (math.signum(quantity) == math.signum(_quantity)) {
        _price = price
      }
    }
  }
  
  def isLong:  Boolean = _quantity > 0
  def isShort: Boolean = _quantity < 0
  
  def gainLoss = (_currentPrice - _price) * quantity * _account.tradingRule.multiplier
  def equity = _currentPrice * math.abs(quantity) * _account.tradingRule.multiplier

  /**
   * @todo, consider expenses?
   */
  def gainLossRatio = (if (isLong) 1 else if (isShort) -1 else 0) * (_currentPrice - _price) / _price
  def maxGainLossRatio = _maxGainLossRatio
  def minGainLossRatio = _minGainLossRatio
  
  override 
  def toString = {
    "Position(%s, quantity=%.0f, price=%.6f, currentPrice=%.4f, equity=%.2f, gainLoss=%.2f, gainLossRatio=%.2f%%)".format(
      sec.uniSymbol, quantity, price, currentPrice, equity, gainLoss, gainLossRatio * 100
    )
  }
}

object Position {
  def apply(account: TradableAccount, time: Long, sec: Sec, price: Double, quantity: Double) = 
    new Position(account, time, sec, price, quantity)
}

sealed trait PositionEvent {
  def position: Position
}
final case class PositionOpened (position: Position) extends PositionEvent
final case class PositionClosed (position: Position) extends PositionEvent
final case class PositionChanged(position: Position) extends PositionEvent
