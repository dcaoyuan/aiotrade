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
package org.aiotrade.lib.util

/**
 *
 * @author  Caoyuan Deng
 * @version 1.0, November 24, 2006, 5:06 PM
 * @since   1.0.4
 */
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

trait ChangeSubject {

  @transient
  private val observerToOwner = mutable.Map[ChangeObserver, AnyRef]()

  def addObserver(owner: AnyRef, observer: ChangeObserver) {
    synchronized {observerToOwner(observer) = owner}
  }

  def removeObserver(observer: ChangeObserver) {
    if (observer == null) {
      return
    }

    if (observerToOwner.keySet.contains(observer)) {
      synchronized {observerToOwner -= observer}
    }
  }

  def removeObserversOf(owner: AnyRef) {
    val toRemove = new ListBuffer[ChangeObserver]
    for ((observer, ownerx) <- observerToOwner if ownerx == owner) {
      toRemove += observer
    }

    synchronized {observerToOwner --= toRemove}
  }

  def removeObservers {
    synchronized {observerToOwner.clear}
  }

  /**
   * A ChangeObservable implement can support may type of ChangeObserver, so
   * we only apply O here, the implement class can choose to notify this type
   * of observers
   *
   * @Note since Class[T] is not co-variant, we have to explicitly use [T <: ChangeObserver]
   */
  def notifyChanged[T <: ChangeObserver](observerType: Class[T]) {
    for (observer <- observerToOwner.keysIterator if observerType.isInstance(observer)) {
      if (observer.updater isDefinedAt this) observer.updater(this)
    }
  }

  def notifyChanged {
    for (observer <- observerToOwner.keysIterator) {
      if (observer.updater isDefinedAt this) observer.updater(this)
    }
  }

  /**
   * for use of wrap class
   */
//  def notifyObservers(subject: Observable): Unit = synchronized {
//    if (changed) {
//      /** must clone the observers in case deleteObserver is called */
//      val clone = new Array[ObserverRef](observerRefs.size)
//      observerRefs.copyToArray(clone, 0)
//      clearChanged
//      clone foreach {_.get.update(subject)}
//    }
//  }

  def observers: Iterable[ChangeObserver] = {
    observerToOwner.keySet
  }

  def observersOf[T <: ChangeObserver](observerType: Class[T]): Iterable[T] = {
    val result = new ListBuffer[T]
    for (observer <- observerToOwner.keysIterator if observerType.isInstance(observer)) {
      result += observer.asInstanceOf[T]
    }
    result
  }

  /**
   * Returns the total number of obervers.
   */
  def observerCount: Int = {
    observerToOwner.size
  }

  private def observerCountOf[T <: ChangeObserver](observerType: Class[T]): Int = {
    var count = 0
    for (observer <- observerToOwner.keysIterator if observerType.isInstance(observer)) {
      count += 1
    }
    count
  }

  override def toString = {
    val sb = new StringBuilder("ChangeObserverList: ")
    sb.append(observerToOwner.size).append(" observers: ")
    for (observer <- observerToOwner.keysIterator) {
      sb.append(" type ").append(observer.getClass.getName)
      sb.append(" observer ").append(observer)
    }

    sb.toString
  }
}

