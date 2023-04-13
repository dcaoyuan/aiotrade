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

import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.io.UnsupportedEncodingException
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.TransformerFactoryConfigurationError
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 *
 * @author Caoyuan Deng
 */
class BeansDocument {
    
  var builder: DocumentBuilder = null
  try {
    builder = DocumentBuilderFactory.newInstance.newDocumentBuilder
  } catch {
    case ex: ParserConfigurationException =>
      ex.printStackTrace
  }

  private val doc: Document = if (builder == null) null else builder.newDocument

  private val beans: Element = if (doc != null) {
    val beans1 = doc.createElement("beans")
    doc.appendChild(beans1)
    beans1
  } else {
    null
  }
    
  def createBean(o:AnyRef): Element = {
    val bean = doc.createElement("bean")
    bean.setAttribute("id", "" + o.hashCode)
    bean.setAttribute("class", o.getClass.getName)
    bean
  }
    
  def appendBean(bean: Element): Unit = {
    beans.appendChild(bean)
  }
    
  def valueConstructorArgOfBean(bean: Element, index: Int, value: Any): Element = {
    val arg = doc.createElement("constructor-arg")
    arg.setAttribute("index", "" + index)
    arg.setAttribute("value", "" + value)
    bean.appendChild(arg)
    arg
  }
    
  def innerPropertyOfBean(bean: Element, name: String, innerBean: Element): Element = {
    val prop = doc.createElement("property")
    prop.setAttribute("name", name)
    prop.appendChild(innerBean)
    bean.appendChild(prop)
    prop
  }
    
  def valuePropertyOfBean(bean: Element, name:String, value:Any): Element = {
    val prop = doc.createElement("property")
    prop.setAttribute("name", name)
    prop.setAttribute("value", "" + value)
    bean.appendChild(prop)
    prop
  }
    
  def referPropertyOfBean(bean: Element, name: String, o:AnyRef): Element = {
    val prop = doc.createElement("property")
    prop.setAttribute("name", name)
    prop.setAttribute("ref", "" + o.hashCode)
    bean.appendChild(prop)
    prop
  }
    
  def listPropertyOfBean(bean: Element, name: String ): Element = {
    val prop = doc.createElement("property")
    prop.setAttribute("name", name)
    val list = getDoc.createElement("list")
    prop.appendChild(list)
    bean.appendChild(prop)
    list
  }
    
  def innerElementOfList(list: Element, innerbean: Element): Element = {
    list.appendChild(innerbean)
    innerbean
  }
    
  def valueElementOfList(list: Element, value: Any): Element = {
    val elem = doc.createElement("value")
    elem.setNodeValue("" + value)
    list.appendChild(elem)
    elem
  }
    
  def getDoc: Document = {
    doc
  }
    
  def saveDoc: Unit = {
    val file = new File("test.xml")
    try {
      saveToFile(new FileOutputStream(file))
    } catch {
      case ex: FileNotFoundException =>
        ex.printStackTrace
    }
  }
    
  def saveToFile(out: FileOutputStream): Unit = {
    val factory = TransformerFactory.newInstance
    factory.setAttribute("indent-number", 4)
    try {
      val t = factory.newTransformer
      t.setOutputProperty(OutputKeys.METHOD, "xml")
      t.setOutputProperty(OutputKeys.INDENT, "yes")
      /**
       * must wrap the outputstream with a writer (or bufferedwriter to
       * workaround a "buggy" behavior of the xml handling code to indent.
       */
      t.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")))
    } catch {
      case ex: TransformerConfigurationException =>
        ex.printStackTrace
      case ex: TransformerFactoryConfigurationError =>
        ex.printStackTrace
      case ex: TransformerException =>
        ex.printStackTrace
      case ex: UnsupportedEncodingException =>
        ex.printStackTrace
    }
  }

}
