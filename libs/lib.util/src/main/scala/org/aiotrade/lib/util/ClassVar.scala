/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
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

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import scala.reflect.ClassTag

/**
 *
 * @param <C> type of class
 * @param <V> type of var
 * 
 * @author Caoyuan Deng
 */
final class ClassVar[C, V: ClassTag](val name: String, val getter: Method, val setter: Method) {
  if (getter != null) getter.setAccessible(true)
  if (setter != null) setter.setAccessible(true)

  def get(from: C): V = {
    try {
      getter.invoke(from).asInstanceOf[V]
    } catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }

  /**
   * @Note set(to: C, value: Any) instead of set(to: C, value: V) here to prevent 
   * the AnyVal type case to be broadcasted to usages.
   */
  def set(to: C, value: V) {
    try {
      if (value != null) {
        setter.invoke(to, value.asInstanceOf[AnyRef])
      } else {
        val propValue = reflect.classTag[V].newArray(1).apply(0)
        setter.invoke(to, propValue.asInstanceOf[AnyRef])
      }
    } catch {
      case e: Exception => throw new RuntimeException(e)
    }
  }
  
  def copy(from: C, to: C) {
    set(to, get(from))
  }
}

object ClassVar {
  def apply[C, V: ClassTag](name: String, getter: Method, setter: Method) = new ClassVar[C, V](name, getter, setter)
  
  def unapply(x: ClassVar[_, _]) = Some((x.name, x.getter, x.setter))
  
  def getPublicVars[C](clz: Class[C]): List[ClassVar[C, _]] = {
    var fields: List[ClassVar[C, _]] = Nil
    val methods = clz.getMethods
    for (method <- methods; if Modifier.isPublic(method.getModifiers)) {
      val name = method.getName
      val params = method.getParameterTypes
      if (name.endsWith("_$eq") && params.length == 1) {
        val getterName = name.substring(0, name.length - 4)
        val paramType = params(0)
        methods find (x => x.getName == getterName && x.getReturnType == paramType) match {
          case Some(getter) => fields ::= ClassVar(getterName, getter, method)
          case _ =>
        }
      }
    }
    fields
  }
}
