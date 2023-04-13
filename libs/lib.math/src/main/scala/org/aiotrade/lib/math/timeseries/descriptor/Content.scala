/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.math.timeseries.descriptor

import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.Action
import org.aiotrade.lib.math.PersistenceManager
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.util.swing.action.WithActions
import org.aiotrade.lib.util.swing.action.WithActionsHelper
import org.aiotrade.lib.util.swing.action.SaveAction
import scala.collection.mutable.ArrayBuffer

/**
 *
 * @author Caoyuan Deng
 */
class Content(var uniSymbol: String) extends WithActions with Cloneable {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var withActionsHelper = new WithActionsHelper(this)

  /** use List to store descriptor, so they can be ordered by index */
  private var descriptorBuf = ArrayBuffer[Descriptor[_]]()
    
  def descriptors: List[Descriptor[_]] = descriptorBuf.toList
    
  def addDescriptor(descriptor: Descriptor[_]) {
    if (!descriptorBuf.contains(descriptor)) {
      descriptorBuf += descriptor
      descriptor.containerContent = this
    }
  }
    
  def removeDescriptor(descriptor: Descriptor[_]) {
    descriptorBuf.remove(descriptorBuf.indexOf(descriptor))
  }
    
  def removeDescriptor(idx: Int) {
    descriptorBuf.remove(idx)
  }
    
  def indexOf(descriptor: Descriptor[_]): Int = {
    descriptorBuf.indexOf(descriptor)
  }
    
  def lastIndexOf[T <: Descriptor[_]](clz: Class[T]): Int = {
    var lastOne = null.asInstanceOf[T]
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor)) {
      lastOne = descriptor.asInstanceOf[T]
    }
        
    if (lastOne != null) descriptorBuf.indexOf(lastOne) else -1
  }
    
  def clearDescriptors[T <: Descriptor[_]](clz: Class[T]) {
    /**
     * try to avoid java.util.ConcurrentModificationException by add those to
     * toBeRemoved, then call descriptorList.removeAll(toBeRemoved)
     */
    var toBeRemoved = List[Int]()
    var i = 0
    for (descriptor <- descriptorBuf) {
      if (clz.isInstance(descriptor)) {
        toBeRemoved ::= i
      }
      i += 1
    }

    for (i <- toBeRemoved) {
      descriptorBuf.remove(i)
    }
  }
    
  /**
   *
   * @param clazz the Class being looking up
   * @return found collection of Descriptor instances.
   *         If found none, return an empty collection other than null
   */
  def lookupDescriptors[T <: Descriptor[_]](clz: Class[T]): Seq[T] = {
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor)) yield descriptor.asInstanceOf[T]
  }
    
  /**
   * Lookup the descriptorList of clazz (Indicator/Drawing/Source etc) with the same time frequency
   */
  def lookupDescriptors[T <: Descriptor[_]](clz: Class[T], freq: TFreq): Seq[T] = {
    for (descriptor <- descriptorBuf if clz.isInstance(descriptor) && descriptor.freq == freq)
      yield descriptor.asInstanceOf[T]
  }
    
  def lookupDescriptor[T <: Descriptor[_]](clz: Class[T], serviceClassName: String, freq: TFreq): Option[T] = {
    lookupDescriptors(clz) find (_.idEquals(serviceClassName, freq))
  }
    
  def lookupActiveDescriptor[T <: Descriptor[_]](clz: Class[T]): Option[T] = {
    lookupDescriptors(clz) find (_.active)
  }
    
  def createDescriptor[T <: Descriptor[_]](clz: Class[T], serviceClassName: String, freq: TFreq): Option[T] = {
    try {
      val descriptor = clz.newInstance
      descriptor.set(serviceClassName, freq)
      addDescriptor(descriptor)
            
      Some(descriptor.asInstanceOf[T])
    } catch {
      case ex: IllegalAccessException => log.log(Level.WARNING, ex.getMessage, ex); None
      case ex: InstantiationException => log.log(Level.WARNING, ex.getMessage, ex); None
    }
  }
  
  override 
  def clone: Content = {
    try {
      val newone = super.clone.asInstanceOf[Content]
      newone.withActionsHelper = new WithActionsHelper(newone)
      newone.descriptorBuf = descriptorBuf map {x => 
        val y = x.clone
        y.containerContent = newone
        y
      }
      newone
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex); null 
    }
  }

  def addAction(action: Action): Action = {
    withActionsHelper.addAction(action)
  }
    
  def lookupAction[T <: Action](tpe: Class[T]): Option[T] = {
    withActionsHelper.lookupAction(tpe)
  }
    
  def createDefaultActions: Array[Action] = {
    Array(new ContentSaveAction)
  }
    
  private class ContentSaveAction extends SaveAction {
    def execute {
      PersistenceManager().saveContent(Content.this)
    }
  }
    
}

