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
package org.aiotrade.lib.util.serialization

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import scala.collection.mutable

/**
 *
 * @author Caoyuan Deng
 */
object JavaDocument {
  def set(id: String, method: String, value: Any): String = {
    id + "." + method + "(" + value + ");\n"
  }

  def create(id: String, tpe: Class[_], args: Any*): String =  {
    var str = tpe.getName + " " + id + " = new\n " + tpe.getName + "(\n"

    for (i <- 0 until args.length) {
      if (i != args.length - 1) {
        str += args(i) + ", "
      } else {
        str += args(i) + ");\n"
      }
    }

    str
  }
}
class JavaDocument {
    
  def create(o: AnyRef): Unit = {
    val constructorArgMapField = mutable.Map[ConstructorArg, Field]()
    
    o.getClass.getFields.foreach{field =>
      val a = field.getAnnotation(classOf[ConstructorArg])
      if (a != null) {
        constructorArgMapField.put(a, field)
        a.index
        a.name
      }
    }
        
    val args = new Array[Class[_]](constructorArgMapField.size)
    val itr = constructorArgMapField.keySet.iterator
    while (itr.hasNext) {
      val a = itr.next
      args(a.index) = constructorArgMapField.get(a).getClass
    }

    val constructor: Constructor[_] = null
    try {
      o.getClass.getConstructor(args:_*)
    } catch {
      case ex: SecurityException =>
        ex.printStackTrace
      case ex: NoSuchMethodException=>
        ex.printStackTrace
    }
        
    val fields = new Array[Field](constructorArgMapField.size)
    val itr1 = constructorArgMapField.keySet.iterator
    while (itr1.hasNext) {
      val a = itr1.next
      fields(a.index) = constructorArgMapField.get(a).orNull
    }

    if (constructor != null) {
      var str = o.getClass.getName + " " + "dada" + " = new\n" + o.getClass.getName + "(\n"
      var index = 0
      for (field <- fields) {
        try {
          str = str + field.get(o)
          if (index < fields.length - 1) {
            str = str + ", \n"
          } else {
            str = str + ")\n"
          }
        } catch {
          case ex: IllegalArgumentException =>
            ex.printStackTrace
          case ex: IllegalAccessException =>
            ex.printStackTrace
        }
        index += 1
      }
    }
  }
    
}

