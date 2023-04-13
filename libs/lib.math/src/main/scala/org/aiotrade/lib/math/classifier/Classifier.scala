package org.aiotrade.lib.math.classifier

/** 
 * Every classifier must be:
 * <UL> 
 *   <LI> able to load a <CODE>TrainingSet</CODE>, and </LI>
 *   <LI> able to classify an <CODE>Instance</CODE></LI>
 * </UL>
 * 
 * This interface reflects these two elementary methods.
 * 
 * @author Caoyuan Deng
 */
trait Classifier {
  def name: String
  def trainOn(instance: Instance)
  def classify(instance: Instance): Concept
}

final case class Attribute[+T](name: String, value: T)

trait Concept {
  def name: String
  def parent: Concept
  def instances: Array[Instance]
}

trait Instance {

  def attributes: Array[_ <: Attribute[_]]
  def attributeOf(attrName: String): Attribute[_]
  def concept: Concept
}

