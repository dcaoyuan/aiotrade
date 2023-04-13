
//   The contents of this file are subject to the Mozilla Public License
//   Version 1.1 (the "License"); you may not use this file except in
//   compliance with the License. You may obtain a copy of the License at
//   http://www.mozilla.org/MPL/
//
//   Software distributed under the License is distributed on an "AS IS"
//   basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//   License for the specific language governing rights and limitations
//   under the License.
//
//   The Original Code is RabbitMQ.
//
//   The Initial Developers of the Original Code are LShift Ltd,
//   Cohesive Financial Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created before 22-Nov-2008 00:00:00 GMT by LShift Ltd,
//   Cohesive Financial Technologies LLC, or Rabbit Technologies Ltd
//   are Copyright (C) 2007-2008 LShift Ltd, Cohesive Financial
//   Technologies LLC, and Rabbit Technologies Ltd.
//
//   Portions created by LShift Ltd are Copyright (C) 2007-2009 LShift
//   Ltd. Portions created by Cohesive Financial Technologies LLC are
//   Copyright (C) 2007-2009 Cohesive Financial Technologies
//   LLC. Portions created by Rabbit Technologies Ltd are Copyright
//   (C) 2007-2009 Rabbit Technologies Ltd.
//
//   All Rights Reserved.
//
//   Contributor(s): ______________________________________.
//

package org.aiotrade.lib.amqp.impl

import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.util.Arrays


/**
 * An object providing access to a LongString.
 * This might be implemented to read directly from connection
 * socket, depending on the size of the content to be read -
 * long strings may contain up to 4Gb of content.
 */
object LongString {
  val MAX_LENGTH = 0xffffffffL

  /**
   * Private API - Implementation of {@link LongString}. When
   * interpreting bytes as a string, uses UTF-8 encoding.
   */
  class ByteArrayLongString(bytes: Array[Byte]) extends LongString {

    override def equals(o: Any) = {
      if (o.isInstanceOf[LongString]) {
        val other = o.asInstanceOf[LongString]
        Arrays.equals(bytes, other.getBytes)
      } else false
    }

    override def hashCode = Arrays.hashCode(bytes)

    /** {@inheritDoc} */
    def getBytes = bytes

    /** {@inheritDoc} */
    @throws(classOf[IOException])
    def getStream: DataInputStream = new DataInputStream(new ByteArrayInputStream(bytes))
    
    /** {@inheritDoc} */
    def length: Long = bytes.length

    override def toString = {
      try {
        new String(bytes, "utf-8")
      } catch {case ex: UnsupportedEncodingException => throw new Error("utf-8 encoding support required")}
    }
  }

  /**
   * Converts a String to a LongString using UTF-8 encoding.
   * @param string the string to wrap
   * @return a LongString wrapping it
   */
  def asLongString(string: String): LongString = {
    try {
      new ByteArrayLongString(string.getBytes("utf-8"))
    } catch {case _: UnsupportedEncodingException => throw new Error("utf-8 encoding support required")}
  }

  /**
   * Converts a binary block to a LongString.
   * @param bytes the data to wrap
   * @return a LongString wrapping it
   */
  def asLongString(bytes: Array[Byte]): LongString = new ByteArrayLongString(bytes)
}

trait LongString {
  /**
   * Get the length of the content of the long string in bytes
   * @return the length in bytes >= 0 <= MAX_LENGTH
   */
  def length: Long

  /**
   * Get the content stream.
   * Repeated calls to this function return the same stream,
   * which may not support rewind.
   * @return An input stream the reads the content
   * @throws IOException if an error is encountered
   */
  @throws(classOf[IOException])
  def getStream: DataInputStream

  /**
   * Get the content as a byte array.
   * Repeated calls to this function return the same array.
   * This function will fail if getContentLength() > Integer.MAX_VALUE
   * throwing an IllegalStateException.
   * @return the content as an array
   */
  def getBytes: Array[Byte]
}
