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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream
import java.io.OutputStream
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;

/**
 * Helper class to generates AMQP wire-protocol encoded values.
 * Accumulates our output
 */
class ValueWriter(out: DataOutputStream) {
  private val DEFAULT_BUFFER_SIZE = 1024 * 4

  /** Public API - encodes a short string. */
  @throws(classOf[IOException])
  final def writeShortstr(str: String) {
    val bytes = str.getBytes("utf-8");
    val length = bytes.length;
    if (length > 255) {
      throw new IllegalArgumentException(
        "Short string too long; utf-8 encoded length = " + length + ", max = 255.")
    }
    out.writeByte(bytes.length)
    out.write(bytes)
  }

  /** Public API - encodes a long string from a LongString. */
  @throws(classOf[IOException])
  final def writeLongstr(str: LongString) {
    writeLong(str.length.toInt)
    copy(str.getStream, out)
  }

  /** Public API - encodes a long string from a String. */
  @throws(classOf[IOException])
  final def writeLongstr(str: String) {
    val bytes = str.getBytes("utf-8")
    writeLong(bytes.length)
    out.write(bytes)
  }

  /** Public API - encodes a short integer. */
  @throws(classOf[IOException])
  final def writeShort(s: Int) {
    out.writeShort(s)
  }

  /** Public API - encodes an integer. */
  @throws(classOf[IOException])
  final def writeLong(l: Int) {
    // java's arithmetic on this type is signed, however it's
    // reasonable to use ints to represent the unsigned long
    // type - for values < Integer.MAX_VALUE everything works
    // as expected
    out.writeInt(l)
  }

  /** Public API - encodes a long integer. */
  @throws(classOf[IOException])
  final def writeLonglong(ll: Long) {
    out.writeLong(ll)
  }

  /** Public API - encodes a table. */
  @throws(classOf[IOException])
  final def writeTable(table: Map[String, Any]) {
    if (table == null) {
      // Convenience.
      out.writeInt(0)
    } else {
      out.writeInt(Frame.tableSize(table).toInt)
      for((key, value) <- table) {
        writeShortstr(key)
        writeFieldValue(value)
      }
    }
  }

  @throws(classOf[IOException])
  final def writeFieldValue(value: Any) {
    value match {
      case v: Byte =>
        writeOctet('b')
        out.writeByte(v)
      case v: Short =>
        writeOctet('s')
        out.writeShort(v)
      case v: Int =>
        writeOctet('I')
        writeLong(v)
      case v: Long =>
        writeOctet('l')
        out.writeLong(v)
      case v: Float =>
        writeOctet('f')
        out.writeFloat(v)
      case v: Double =>
        writeOctet('d')
        out.writeDouble(v)
      case v: Boolean =>
        writeOctet('t')
        out.writeBoolean(v)
      case v: String =>
        writeOctet('S')
        writeLongstr(v)
      case v: LongString =>
        writeOctet('S')
        writeLongstr(v)
      case v: BigDecimal =>
        writeOctet('D')
        writeOctet(v.scale)
        val unscaled = v.unscaledValue
        if(unscaled.bitLength > 32) /*Integer.SIZE in Java 1.5*/
          throw new IllegalArgumentException("BigDecimal too large to be encoded")
        writeLong(v.unscaledValue().intValue)
      case v: Date =>
        writeOctet('T')
        writeTimestamp(v)
      case v: Map[String, _] =>
        writeOctet('F')
        // Ignore the warnings here.  We hate erasure
        // (not even a little respect)
        writeTable(v)
      case v: Array[Byte] =>
        writeOctet('x')
        writeLong(v.length)
        out.write(v)
      case null =>
        writeOctet('V')
      case v: List[_] =>
        writeOctet('A')
        writeArray(v)
      case v: AnyRef => throw new IllegalArgumentException ("Invalid value type: " + v.getClass.getName)
    }
  }

  @throws(classOf[IOException])
  def writeArray(value: List[_]) {
    if (value==null) {
      out.write(0)
    }
    else {
      out.writeInt(Frame.arraySize(value).toInt)
      for (item <- value) {
        writeFieldValue(item)
      }
    }
  }

  /** Public API - encodes an octet from an int. */
  @throws(classOf[IOException])
  final def writeOctet(octet: Int) {
    out.writeByte(octet)
  }

  /** Public API - encodes an octet from a byte. */
  @throws(classOf[IOException])
  final def writeOctet(octet: Byte)
  {
    out.writeByte(octet)
  }

  /** Public API - encodes a timestamp. */
  @throws(classOf[IOException])
  final def writeTimestamp(timestamp: Date) {
    // AMQP uses POSIX time_t which is in seconds since the epoch began
    writeLonglong(timestamp.getTime / 1000)
  }

  /**
   * Public API - call this to ensure all accumulated
   * values are correctly written to the output stream.
   */
  @throws(classOf[IOException])
  def flush {
    out.flush
  }

  /**
   * Copy bytes from an <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   * <p>
   * Large streams (over 2GB) will return a bytes copied value of
   * <code>-1</code> after the copy has completed since the correct
   * number of bytes cannot be returned as an int. For large streams
   * use the <code>copyLarge(InputStream, OutputStream)</code> method.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  @throws(classOf[IOException])
  def copy(input: InputStream, output: OutputStream): Int = {
    val count = copyLarge(input, output)
    if (count > Int.MaxValue) {
      return -1
    }
    count.toInt
  }

  /**
   * Copy bytes from a large (over 2GB) <code>InputStream</code> to an
   * <code>OutputStream</code>.
   * <p>
   * This method buffers the input internally, so there is no need to use a
   * <code>BufferedInputStream</code>.
   *
   * @param input  the <code>InputStream</code> to read from
   * @param output  the <code>OutputStream</code> to write to
   * @return the number of bytes copied
   * @throws NullPointerException if the input or output is null
   * @throws IOException if an I/O error occurs
   */
  @throws(classOf[IOException])
  def copyLarge(input: InputStream, output: OutputStream): Long = {
    val buffer = new Array[Byte](DEFAULT_BUFFER_SIZE)
    var count = 0
    var n = 0
    while ({n = input.read(buffer); n != -1}) {
      output.write(buffer, 0, n)
      count += n
    }
    count
  }
}
