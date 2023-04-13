/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aiotrade.lib.util

import java.util.ResourceBundle
import scala.collection.mutable
/**
 *
 * @author dcaoyuan
 */
class Bundle {
  private val RESOURCE_NAME = "Bundle"
  private val cache = mutable.Map[Class[_], ResourceBundle]()

  def getString(clazz: Class[_], name: String): String = {
    getResourceBundle(clazz).getString(name)
  }

  private def getResourceBundle(clazz: Class[_]): ResourceBundle = {
    cache.get(clazz) getOrElse {
      var name = clazz.getName
      val dotIdx = name.lastIndexOf('.')
      if (dotIdx != -1) {
        name = name.substring(0, dotIdx) + "." + RESOURCE_NAME
      } else {
        name = RESOURCE_NAME
      }
      val rb = ResourceBundle.getBundle(name)
      cache.put(clazz, rb)
      rb
    }
  }
}
