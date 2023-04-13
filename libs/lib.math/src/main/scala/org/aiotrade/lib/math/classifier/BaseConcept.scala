package org.aiotrade.lib.math.classifier

import org.aiotrade.lib.collection.ArrayList

/**
 * 
 * @author Caoyuan Deng
 */
class BaseConcept(var name: String, var parent: BaseConcept) extends Concept {

  private val _instances = new ArrayList[Instance]()
	
  def addInstance(i: Instance): Unit = synchronized {
    _instances += i
  }
	
  def instances = _instances.toArray

  override
  def toString = name
	
  override 
  def hashCode: Int = {
    val prime = 31
    var result = 1
    result = prime * result + (if (name == null) 0 else name.hashCode)
    result = prime * result + (if (parent == null) 0 else parent.hashCode)
    result
  }

  override
  def equals(o: Any): Boolean = o match {
    case x: BaseConcept =>
      name == x.name && parent == x.parent
    case _ => false
  }    
}

object BaseConcept {
  def apply(name: String, parent: BaseConcept) = new BaseConcept(name, parent)
  def apply(name: String) = new BaseConcept(name, null)
}

