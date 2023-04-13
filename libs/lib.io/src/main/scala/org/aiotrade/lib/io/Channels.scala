/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.io

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel

object Channels {
  def asChannel(is: InputStream):  ReadableByteChannel = java.nio.channels.Channels.newChannel(is)

  def asChannel(os: OutputStream): WritableByteChannel = java.nio.channels.Channels.newChannel(os)

  def readAllToByteBuffer(is: InputStream): ByteBuffer = {
    val buf = new Array[Byte](8192)
    val os = new ByteArrayOutputStream
    var len = -1
    while ({len = is.read(buf); len != -1}) {
      os.write(buf, 0, len)
    }

    val bytes = os.toByteArray
    ByteBuffer.wrap(bytes)
  }
}