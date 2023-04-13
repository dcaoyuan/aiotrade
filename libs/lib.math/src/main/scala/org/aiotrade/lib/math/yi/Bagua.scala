package org.aiotrade.lib.math.yi

/**
 * 
 * http://zh.wikipedia.org/wiki/%E5%85%AB%E5%8D%A6
 * 
 * @author Caoyuan Deng
 */
abstract class Bagua(
  val i: Int,
  val name: String, 
  val trigram: String, 
  val nature: String, 
  val character: String, 
  val family: String, 
  val direction: String, 
  val wuxing: String, 
  val number: String, 
  val unicode: String
) {
  
  override
  def equals(o: Any) = o match {
    case other: Bagua => this.i == other.i
    case _ => false
  }
  
  override 
  def hashCode = i
}

object Bagua {
  val baguas = Array(Qian, Dui, Li, Zhen, Xun, Kan, Gen, Kun).sortBy(_.i)
  val numberToBagua = baguas map {x => x.number -> x} toMap
  
  def withNumber(number: String) = numberToBagua(number)
}

case object Qian extends Bagua(7, "乾", "☰", "天", "健", "父", "西北", "金", "111", "u2630")
case object Dui  extends Bagua(6, "兑", "☱", "泽", "悦", "少女", "西", "金", "110", "u2631")
case object Li   extends Bagua(5, "离", "☲", "火", "丽", "中女", "南", "火", "101", "u2632")
case object Zhen extends Bagua(4, "震", "☳", "雷", "动", "长男", "东", "木", "100", "u2633")
case object Xun  extends Bagua(3, "巽", "☴", "风", "入", "长女", "东南", "木", "011", "u2634")
case object Kan  extends Bagua(2, "坎", "☵", "水", "陷", "中男", "北", "水", "010", "u2635")
case object Gen  extends Bagua(1, "艮", "☶", "山", "止", "少男", "东北", "土", "001", "u2636")
case object Kun  extends Bagua(0, "坤", "☷", "地", "顺", "母", "西南", "土", "000", "u2637")
