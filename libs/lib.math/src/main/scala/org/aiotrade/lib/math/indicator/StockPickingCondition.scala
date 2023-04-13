/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.math.indicator

object StockPickingCondition {
  case object MoneyFlowInPriceDown extends StockPickingCondition(122, "MIPD")
  case object MoneyFlowOutPriceUp extends StockPickingCondition(123, "MOPU")
  case object MainMoneyFlowInPriceDown extends StockPickingCondition(124, "MMIPD")
  case object MainMoneyFlowOutPriceUp extends StockPickingCondition(125, "MMOPU")
  case object MainMoneyFlowInRetailMoneyFlowOut extends StockPickingCondition(126, "MIRO")
  case object MainMoneyFlowOutRetailMoneyFlowIn extends StockPickingCondition(127, "MORI")
  case object Other extends StockPickingCondition(0, "Other")
  
  def withCode(code: Int) = {
    code match{
      case 122 => MoneyFlowInPriceDown
      case 123 => MoneyFlowOutPriceUp
      case 124 => MainMoneyFlowInPriceDown
      case 125 => MainMoneyFlowOutPriceUp
      case 126 => MainMoneyFlowInRetailMoneyFlowOut
      case 127 => MainMoneyFlowOutRetailMoneyFlowIn
      case _ => Other
    }
  }
}

class StockPickingCondition(conditionCode: Int, shortName: String){
  def code = conditionCode
  def name = shortName
}
