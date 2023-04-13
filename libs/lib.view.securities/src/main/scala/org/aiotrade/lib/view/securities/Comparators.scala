/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.view.securities

import java.util.Comparator

object Comparators {
  
  val comparator = new Comparator[Object] {
    def compare(o1: Object, o2: Object): Int = {
      (o1, o2) match {
        case ("-", "-") => 0
        case ("-",  _ ) => -1
        case (_  , "-") => 1
        case (s1: String, s2: String) =>
          val s11 = if (s1.endsWith("%")) s1.substring(0, s1.length - 1) else s1
          val s12 = if (s2.endsWith("%")) s2.substring(0, s2.length - 1) else s2
          try {
            val d1 = s11.toDouble
            val d2 = s12.toDouble
            if (d1 > d2) 1 else if (d1 < d2) -1 else 0
          } catch {
            case _: Throwable => s1 compareTo s2
          }
        case _ => 0
      }
    }
  }

  val symbolComparator = new Comparator[Object] {
    def compare(o1: Object, o2: Object): Int = {
      (o1, o2) match {
        case ("-", "-") => 0
        case ("-",  _ ) => -1
        case (_  , "-") => 1
        case (s1: String, s2: String) =>
          val s1s = s1.split('.')
          val s2s = s2.split('.')
          val s1a = if (s1s.length > 1) s1s(1) + s1s(0) else s1
          val s2a = if (s2s.length > 1) s2s(1) + s2s(0) else s2
          s2a.compareTo(s1a)
        case _ => 0
      }
    }
  }


}
