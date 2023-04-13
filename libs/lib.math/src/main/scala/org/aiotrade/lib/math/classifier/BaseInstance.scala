package org.aiotrade.lib.math.classifier

import java.io.BufferedReader
import java.io.IOException
import org.aiotrade.lib.collection.ArrayList

/**
 * 
 * @author Caoyuan Deng
 */
class BaseInstance(private var _concept: Concept, private var _attributes: Array[Attribute[String]]) extends Instance {
	
  def concept = _concept
  def concept_=(concept: Concept) {
    _concept = concept
  }
  
  def attributes = _attributes
  def attributes_=(attributes: Array[Attribute[String]]) {
    _attributes = attributes
  }
	
  /**
   * This method loads the training instances for the user clicks.
   * 
   * @param fileName the name of the file that contains the user clicks
   * @throws IOException 
   */
  @throws(classOf[IOException])
  def load(fileName: String): Array[BaseInstance] = Array() // @todo
		
  @throws(classOf[IOException])
  def load(bR: BufferedReader): Array[BaseInstance] = {
    val baseInstances = new ArrayList[BaseInstance]()
    var line: String = null
    while ({line = bR.readLine; line != null}) {
      val data = line.split(",")
      val n = data.length
      val attributes = new Array[Attribute[String]](n - 1)
      var i = 0
      while (i < n - 1) {
        attributes(i) = Attribute("a-" + i, data(i))
        i += 1
      }
                
      //The last value is assumed to be the class/concept
      baseInstances += BaseInstance(BaseConcept(data(n - 1)), attributes)
    }
        
    baseInstances.toArray
  }
    
  override 
  def toString = {
    val sb = new StringBuilder()
    if (attributes != null) {
      for (a <- attributes) {
				
        if ( a == null || a.name == null) {
          sb.append(" -  <NULL ATTRIBUTE> ")
        } else {
          if (a.value == null) {
            sb.append(" -  <NULL ATTRIBUTE VALUE> ")
          } else {
            sb.append(" -  " + a.name + " = " + a.value)
          }
        }
      }
    }
		
    sb.append(" -->  " + concept.name)
    sb.toString
  }

  def getAttribute(i: Int): Attribute[String] = attributes(i)
	
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  override
  def equals(a: Any): Boolean = a match {
    case other: BaseInstance =>
      // Check the concept
      if (concept == null) {
        if (other.concept != null) {
          return false
        }
      } else {
        if (!concept.equals(other.concept)) {
          return false
        }
      }

      // Finally check all the attributes
      var i = 0
      while (i < attributes.length) {
        if (attributes(i) == null) {
          if (other.attributes(i) != null) {
            return false
          }
        } else {
          if (attributes(i).name != other.attributes(i).name) {
            return false
          } else {
            if (attributes(i).value != other.attributes(i).value) {
              return false
            }
          }
        }
        i += 1
      } 
      true
    case _ => false
  }

  def attributeOf(attrName: String): Attribute[_] = {
    var matchedAttribute: Attribute[_] = null
        
    if( attributes != null ) {
      var i = 0
      var continue = true
      while (i < attributes.length && continue) {
        val a = attributes(i)
        if (attrName.equalsIgnoreCase(a.name)) {
          matchedAttribute = a
          continue = false
        }
        i += 1
      }
    }
        
    matchedAttribute
  }
}

object BaseInstance {
  def apply(concept: Concept, attributes: Array[Attribute[String]]) = new BaseInstance(concept, attributes)
  
  def apply(conceptName: String, attrNames: Array[String], attrValues: Array[String]) {
    val n = attrNames.length
    val attributes = new Array[Attribute[String]](n)
    var i = 0
    while (i < n) {
      attributes(i) = Attribute(attrNames(i), attrValues(i))
      i += 1
    }
    
    val concept = BaseConcept(conceptName)
    new BaseInstance(concept, attributes)
  }
}