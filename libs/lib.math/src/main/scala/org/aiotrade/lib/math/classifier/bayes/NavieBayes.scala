package org.aiotrade.lib.math.classifier.bayes

import org.aiotrade.lib.math.classifier.Attribute
import org.aiotrade.lib.math.classifier.AttributeValue
import org.aiotrade.lib.math.classifier.Classifier
import org.aiotrade.lib.math.classifier.Concept
import org.aiotrade.lib.math.classifier.Instance
import scala.collection.mutable


/**
 * A basic implementation of the Naive Bayes algorithm.
 * 
 * The emphasis is on teaching the algorithm, not optimizing its performance.
 * 
 * The only constructor for this classifier takes a name and 
 * a training set as arguments.
 * 
 * @param name the name of the classifier
 * 
 * @author Caoyuan Deng
 */
@cloneable
class NaiveBayes(val name: String) extends Classifier {

  /**
   * These are the probabilities for each concept
   */
  private val conceptToPrior = new mutable.HashMap[Concept, Double]()
  private def numConcepts = conceptToPrior.size
	
  /**
   * This structure contains the fundamental calculation elements of 
   * the Naive Bayes method, i.e. the conditional probabilities.
   */
  private val p = new mutable.HashMap[Concept, mutable.HashMap[Attribute[_], AttributeValue[_]]]()
  
  private var _numTrainedInstances = 0
  def numTrainedInstances = _numTrainedInstances
	
  /**
   * These are the attribute indices that we should consider for training
   */
  private val attributeNames = new mutable.HashSet[String]()
	
  /** An auxiliary variable */
  private var _isVerbose = false
	

  def addAttribute(attrName: String): this.type = {
    attributeNames += attrName
    this
  }
  
  /**
   * Calculate concept priors and conditional probabilities according to the new instance
   * 
   * Strictly speaking these are not the prior probabilities but just the counts. 
   * However, we want to reuse these counts and the priors can be obtained by a simple division.
   */
  def trainOn(i: Instance) {
    _numTrainedInstances += 1
    
    // calculate concept priors
    conceptToPrior(i.concept) = conceptToPrior.getOrElse(i.concept, 0.0) + 1

    // calculate conditional probabilities
    var k = 0
    while (k < i.attributes.length) {
      val attribute = i.attributes(k)
      if (attribute != null && attributeNames.contains(attribute.name)) {
        val attributeToValue = p.get(i.concept) match {
          case None =>
            val x = new mutable.HashMap[Attribute[_], AttributeValue[_]]()
            p(i.concept) = x
            x
          case Some(x) => x
        }

        val attrValue = attributeToValue.get(attribute) match {
          case None =>
            val x = AttributeValue(attribute.value)
            attributeToValue(attribute) = x
            x
          case Some(x) => x
        }
        attrValue.increaseOne
      }
      
      k += 1
    }
  }
  
  def classify(i: Instance): Concept = {
    if (numConcepts == 0) {
      throw new RuntimeException("You have to train classifier first.")
    }
    if (_isVerbose) {
      println("\n*** Classifying instance: " + i.toString + "\n")
    }
    
    var bestConcept: Concept = null
    var bestP = 0.0
    for ((c, _) <- conceptToPrior) {
      val p = probability(c, i)
      if ( _isVerbose) {
        printf("P(%s|%s) = %.15f\n", c.name, i, p)
      }
      if (p >= bestP) {
        bestConcept = c
        bestP = p
      }
    }
    bestConcept
  }

  /**
   * This method calculates the <I>posterior probability</I> that we deal with 
   * concept <CODE>c</CODE> provided that we observed instance <CODE>i</CODE>.
   * This is the application of Bayes theorem.
   * 
   * @param c is a probable concept for instance <CODE>i</CODE>
   * @param i is the observed instance
   * @return posterior probability of <CODE>c</CODE> given instance <CODE>i</CODE>
   */
  def probability(c: Concept, i: Instance): Double = {
    var cP = 0.0
    if (conceptToPrior.contains(c)) {
      cP = (probability(i, c) * probability(c)) / probability(i)
    } else {
      // We have never seen this concept before
      // assign to it a "reasonable" value
      cP = 1.0 / (numConcepts + 1.0)
    }
    cP
  }

  /**
   * This method calculates the denumerator of Bayes theorem
   * 
   * @param <CODE>Instance</CODE> i
   * @return the probability of observing <CODE>Instance</CODE> i
   */
  def probability(i: Instance): Double = {
    var cp = 0.0
    for ((c, _) <- conceptToPrior) {
      cp += probability(i, c) * probability(c)
    }
    if (cp == 0) 1.0 / _numTrainedInstances else cp
  }
	
  def probability(c: Concept): Double = {
    conceptToPrior.get(c) match {
      case Some(instanceCount) => instanceCount / _numTrainedInstances
      case None => 0.0
    }
  }
	
  def probability(i: Instance, c: Concept): Double = {
    var cp = 1.0
    val attributeToValue = p(c)
    var k = 0
    while (k < i.attributes.length) {
      val attribute = i.attributes(k)
      if (attribute != null && attributeNames.contains(attribute.name)) {
        attributeToValue.get(attribute) match {
          case None => 
            // the specific attribute value is not present for the current concept.
            // Can you justify the following estimate?
            // Can you think of a better choice?
            cp *= 1.0 / (_numTrainedInstances + 1)
          case Some(x) =>
            cp *= x.count / conceptToPrior(c)
        }
      }
      
      k += 1
    }
    
    if (cp == 1) 1.0 / numConcepts else cp
  }
  
  def prettyPrint: String =  {
    val sb = new StringBuilder
    
    sb.append("\n" + name)
    sb.append("\n=== Conditional probabilities ===\n")
    for {
      (c, avs) <- p
      (a, v) <- avs
    } {
      sb.append(c).append(" - ").append(a).append(":\t").append(v.count.toDouble / _numTrainedInstances * 100).append("%\n")
    }
    sb.append("\n=== Concept Prior ===\n")
    conceptToPrior foreach {x => sb.append(x._1).append(" -> ").append(x._2 / _numTrainedInstances * 100).append("%\n")}
    sb.append("\nnumTrainedInstances: " + _numTrainedInstances)
    
    sb.toString
  }
  
  /**
   * This should not be an abstract method so that scalac knows it's a override of
   * @cloneable instead of java.lang.Object#clone
   */
  override 
  def clone: NaiveBayes = {
    val cloned = new NaiveBayes(this.name)
    
    this.conceptToPrior foreach {case (c, p) => cloned.conceptToPrior += (c -> p)}
    
    for {
      (c, avs) <- this.p
      (a, v) <- avs
    } {
      val attributeToValue = cloned.p.get(c) match {
        case None => 
          val x = new mutable.HashMap[Attribute[_], AttributeValue[_]]()
          cloned.p(c) = x
          x
        case Some(x) => x
      }
      // a is immutable, v is mutable since it has an inner variable _count, we should also clone it.
      attributeToValue += (a -> v.clone)
    }

    cloned._numTrainedInstances = this._numTrainedInstances
    
    this.attributeNames foreach {x => cloned.attributeNames += x}
    
    cloned._isVerbose = this._isVerbose
    
    cloned
  }
}

object NaiveBayes {
  def apply(name: String) = new NaiveBayes(name)
}
