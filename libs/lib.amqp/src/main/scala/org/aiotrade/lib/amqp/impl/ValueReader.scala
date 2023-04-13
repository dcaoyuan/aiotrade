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

import com.rabbitmq.client.MalformedFrameException
import com.rabbitmq.client.impl.TruncatedInputStream
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date
import scala.collection.mutable.ListBuffer


object ValueReader {
  private val INT_MASK = 0xffffffffL
  /**
   * Protected API - Cast an int to a long without extending the
   * sign bit of the int out into the high half of the long.
   */
  protected def unsignedExtend(value: Int): Long = {
    val extended = value;
    extended & INT_MASK
  }

  /** Public API - convenience method - reads a short string from a DataInput
   Stream. */
  @throws(classOf[IOException])
  final def readShortstr(in: DataInputStream): String = {
    val b = new Array[Byte](in.readUnsignedByte);
    in.readFully(b)
    new String(b, "utf-8")
  }

  /** Public API - convenience method - reads a 32-bit-length-prefix
   * byte vector from a DataInputStream.
   */
  @throws(classOf[IOException])
  final def readBytes(in: DataInputStream): Array[Byte] = {
    val contentLength = unsignedExtend(in.readInt)
    if(contentLength < Integer.MAX_VALUE) {
      val buffer = new Array[Byte](contentLength.toInt)
      in.readFully(buffer)
      return buffer
    } else {
      throw new UnsupportedOperationException("Very long byte vectors and strings not currently supported")
    }
  }

  /** Public API - convenience method - reads a long string argument
   * from a DataInputStream.
   */
  @throws(classOf[IOException])
  final def readLongstr(in: DataInputStream): LongString = {
    LongString.asLongString(readBytes(in))
  }

  /**
   * Public API - reads a table argument from a given stream. Also
   * called by {@link ContentHeaderPropertyReader}.
   */
  @throws(classOf[IOException])
  final def readTable(in: DataInputStream): Map[String, _] = {
    var table = Map[String, Any]()
    val tableLength = unsignedExtend(in.readInt)

    val tableIn = new DataInputStream (new TruncatedInputStream(in, tableLength))
    while (tableIn.available > 0) {
      val name = readShortstr(tableIn)
      val value = readFieldValue(tableIn)
      if (!table.contains(name))
        table += (name -> value)
    }
    
    table
  }

  @throws(classOf[IOException])
  final def readFieldValue(in: DataInputStream): Any = {
    in.readUnsignedByte match {
      case 'S' =>
        readLongstr(in)
      case 'I' =>
        in.readInt
      case 'D' =>
        val scale = in.readUnsignedByte
        val unscaled = new Array[Byte](4)
        in.readFully(unscaled)
        new BigDecimal(new BigInteger(unscaled), scale)
      case 'T' =>
        readTimestamp(in)
      case 'F' =>
        readTable(in)
      case 'A' =>
        readArray(in)
      case 'b' =>
        in.readByte
      case 'd' =>
        in.readDouble
      case 'f' =>
        in.readFloat
      case 'l' =>
        in.readLong
      case 's' =>
        in.readShort
      case 't' =>
        in.readBoolean
      case 'x' =>
        readBytes(in)
      case 'V' =>
        null
      case _ => throw new MalformedFrameException("Unrecognised type in table")
    }
  }

  /** Read a field-array */
  @throws(classOf[IOException])
  def readArray(in: DataInputStream): List[_] = {
    val length = unsignedExtend(in.readInt)
    val arrayIn = new DataInputStream(new TruncatedInputStream(in, length))
    val array = new ListBuffer[Any]
    while(arrayIn.available > 0) {
      val value = readFieldValue(arrayIn)
      array += value
    }
    array.toList
  }

  /** Public API - convenience method - reads a timestamp argument from the DataInputStream. */
  @throws(classOf[IOException])
  final def readTimestamp(in: DataInputStream): Date = {
    new Date(in.readLong * 1000)
  }

}

/**
 * Helper class to reade AMQP wire-protocol encoded values.
 *
 * @param The stream we are reading from
 */
class ValueReader(in: DataInputStream) {
    
  /** Public API - reads a short string. */
  @throws(classOf[IOException])
  final def readShortstr: String = {
    ValueReader.readShortstr(in)
  }

  /** Public API - convenience method - reads a 32-bit-length-prefix
   * byte vector
   */
  @throws(classOf[IOException])
  def readBytes: Array[Byte] = {
    ValueReader.readBytes(in)
  }

  /** Public API - reads a long string. */
  @throws(classOf[IOException])
  final def readLongstr: LongString = {
    ValueReader.readLongstr(in);
  }

  /** Public API - reads a short integer. */
  @throws(classOf[IOException])
  final def readShort: Int = {
    in.readUnsignedShort();
  }

  /** Public API - reads an integer. */
  @throws(classOf[IOException])
  final def readLong: Int = {
    in.readInt
  }

  /** Public API - reads a long integer. */
  @throws(classOf[IOException])
  final def readLonglong: Long = {
    in.readLong();
  }


  /** Public API - reads a table. */
  @throws(classOf[IOException])
  final def readTable: Map[String, _] = {
    ValueReader.readTable(this.in);
  }

  /** Public API - reads an octet. */
  @throws(classOf[IOException])
  final def readOctet: Int = {
    in.readUnsignedByte();
  }

  /** Public API - reads an timestamp. */
  @throws(classOf[IOException])
  final def readTimestamp: Date = {
    ValueReader.readTimestamp(in)
  }

}
