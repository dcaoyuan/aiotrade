/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.io

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object FileCopier {
  
  @throws(classOf[IOException])
  def copyOne(src: File, dest: File, keepTimestamp: Boolean = true) {
    copy(src, Array(dest), true)
  }

  @throws(classOf[IOException])
  def copy(src: File, dests: Array[File], keepTimestamp: Boolean = true) {
    val timestamp = src.lastModified
    val in = new FileInputStream(src) getChannel
    
    for (dest <- dests) {
      val out = new FileOutputStream(dest) getChannel

      in.transferTo(0, src.length, out)
      out.close
      
      if (keepTimestamp) dest.setLastModified(timestamp)
    }
    
    in.close
  }
}