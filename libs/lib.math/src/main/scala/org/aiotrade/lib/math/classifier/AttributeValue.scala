package org.aiotrade.lib.math.classifier

/**
 * 
 * @author Caoyuan Deng
 */
@cloneable
final case class AttributeValue[+T](value: T) {
  private var _count = 0

  def count = _count
  
  def increaseOne = {
    _count += 1
    this
  }
  
  def decreaseOne = {
    _count -= 1
    this
  }
  
  def reset = {
    _count = 0
    this
  }
  
  /**
   * This should not be an abstract method so that scalac knows it's a override of
   * @cloneable instead of java.lang.Object#clone
   */
  override 
  def clone: AttributeValue[T] = {super.clone.asInstanceOf[AttributeValue[T]]}
}
