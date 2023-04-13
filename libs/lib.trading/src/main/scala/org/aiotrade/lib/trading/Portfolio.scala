package org.aiotrade.lib.trading

trait Portfolio {
  def id: Long
  def description: String
  def account: Account
  def positions: Array[Position]
  def profit: Double
}
