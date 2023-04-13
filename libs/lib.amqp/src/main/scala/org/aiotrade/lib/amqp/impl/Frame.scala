
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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.MalformedFrameException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.Date;


object Frame {

  @throws(classOf[IOException])
  def fromBodyFragment(channelNumber: Int, body: Array[Byte], offset: Int, length: Int): Frame = {
    val frame = new Frame(AMQP.FRAME_BODY, channelNumber)
    val bodyOut = frame.getOutputStream
    bodyOut.write(body, offset, length)
    frame
  }

  /**
   * Protected API - Factory method to instantiate a Frame by reading an
   * AMQP-wire-protocol frame from the given input stream.
   *
   * @return a new Frame if we read a frame successfully, otherwise null
   */
  @throws(classOf[IOException])
  def readFrom(is: DataInputStream): Frame = {
    var tpe = 0
    var channel = 0

    try {
      tpe = is.readUnsignedByte
    } catch {
      case ex: SocketTimeoutException =>
        // System.err.println("Timed out waiting for a frame.");
        return null // failed
    }

    if (tpe == 'A') {
      /*
       * Probably an AMQP.... header indicating a version
       * mismatch.
       */
      /*
       * Otherwise meaningless, so try to read the version,
       * and throw an exception, whether we read the version
       * okay or not.
       */
      protocolVersionMismatch(is)
    }

    channel = is.readUnsignedShort
    val payloadSize = is.readInt
    val payload = new Array[Byte](payloadSize)
    is.readFully(payload)

    val frameEndMarker = is.readUnsignedByte
    if (frameEndMarker != AMQP.FRAME_END) {
      throw new MalformedFrameException("Bad frame end marker: " + frameEndMarker)
    }

    new Frame(tpe, channel, payload)
  }

  /**
   * Private API - A protocol version mismatch is detected by checking the
   * three next bytes if a frame type of (int)'A' is read from an input
   * stream. If the next three bytes are 'M', 'Q' and 'P', then it's
   * likely the broker is trying to tell us we are speaking the wrong AMQP
   * protocol version.
   *
   * @throws MalformedFrameException
   *                 if an AMQP protocol version mismatch is detected
   * @throws MalformedFrameException
   *                 if a corrupt AMQP protocol identifier is read
   */
  @throws(classOf[IOException])
  def protocolVersionMismatch(is: DataInputStream) {
    var x: MalformedFrameException = null

    // We expect the letters M, Q, P in that order: generate an informative error if they're not found
    val expectedBytes = Array[Byte]('M', 'Q', 'P' )
    for (expectedByte <- expectedBytes) {
      val nextByte = is.readUnsignedByte
      if (nextByte != expectedByte) {
        throw new MalformedFrameException("Invalid AMQP protocol header from server: expected character " +
                                          expectedByte + ", got " + nextByte);
      }
    }

    try {
      val transportHigh = is.readUnsignedByte
      val transportLow  = is.readUnsignedByte
      val serverMajor   = is.readUnsignedByte
      val serverMinor   = is.readUnsignedByte
      x = new MalformedFrameException("AMQP protocol version mismatch; we are version " + AMQP.PROTOCOL.MAJOR + "-" + AMQP.PROTOCOL.MINOR
                                      + ", server is " + serverMajor + "-" + serverMinor + " with transport " + transportHigh + "." + transportLow);
    } catch {
      case ex: IOException => x = new MalformedFrameException("Invalid AMQP protocol header from server")
    }
    throw x
  }

  /**
   * Utility for constructing a java.util.Map instance from an
   * even-length array containing alternating String keys (on the
   * even elements, starting at zero) and values (on the odd
   * elements, starting at one).
   */
  def buildTable(keysValues: Array[_]): Map[String, _] = {
    var result = Map[String, Any]()
    var index = 0
    while (index < keysValues.length) {
      val key = keysValues(index).asInstanceOf[String]
      val value = keysValues(index + 1)
      result += (key -> value)
      index += 2
    }
    result
  }

  /** Computes the AMQP wire-protocol length of protocol-encoded table entries.
   */
  @throws(classOf[UnsupportedEncodingException])
  def tableSize(table: Map[String, Any]): Long = {
    var acc = 0L
    for ((key, value) <- table) {
      acc += shortStrSize(key)
      acc += fieldValueSize(value)
    }
    acc
  }

  /** Computes the AMQP wire-protocol length of a protocol-encoded field-value. */
  @throws(classOf[UnsupportedEncodingException])
  def fieldValueSize(value: Any): Long = {
    var acc = 1L // for the type tag
    value match {
      case _: Byte =>
        acc += 1
      case _: Short =>
        acc += 2
      case _: Int =>
        acc += 4
      case _: Long =>
        acc += 8
      case _: Float =>
        acc += 4
      case _: Double =>
        acc += 8
      case _: Boolean =>
        acc += 1
      case v: String =>
        acc += longStrSize(v)
      case v: LongString =>
        acc += 4 + v.length
      case _: BigDecimal =>
        acc += 5
      case _: Timestamp =>
        acc += 8
      case _: Date =>
        acc += 8
      case v: Map[String, _] =>
        acc += 4 + tableSize(v)
      case v: Array[Byte] =>
        acc += 4 + v.length
      case v: List[_] =>
        acc += 4 + arraySize(v)
      case null =>
      case _ => throw new IllegalArgumentException("invalid value in table")
    }
    acc
  }

  /** Computes the AMQP wire-protocol length of an encoded field-array */
  @throws(classOf[UnsupportedEncodingException])
  def arraySize(values: List[Any]): Long = {
    var acc = 0L
    for (value <- values) {
      acc += fieldValueSize(value)
    }
    acc
  }

  /** Computes the AMQP wire-protocol length of a protocol-encoded long string. */
  @throws(classOf[UnsupportedEncodingException])
  def longStrSize(str: String): Long = {
    str.getBytes("utf-8").length + 4
  }

  /** Computes the AMQP wire-protocol length of a protocol-encoded short string. */
  @throws(classOf[UnsupportedEncodingException])
  def shortStrSize(str: String): Int = {
    str.getBytes("utf-8").length + 1
  }

}

/**
 * Represents an AMQP wire-protocol frame, with frame type, channel number, and payload bytes.
 *
 * @param Frame type code
 * @param Frame channel number, 0-65535
 * @param Frame payload bytes (for inbound frames)
 */
class Frame(tpe: Int, channel: Int, payload: Array[Byte]) {
  /** Frame payload (for outbound frames) */
  var accumulator: ByteArrayOutputStream = _

  /**
   * Constructs an uninitialized frame.
   */
  def this() = this(0, 0, null)

  /**
   * Constructs a frame for output with a type and a channel number and a
   * fresh accumulator waiting for payload.
   */
  def this(tpe: Int, channel: Int) = this(tpe, channel, null)

  /**
   * Public API - writes this Frame to the given DataOutputStream
   */
  @throws(classOf[IOException])
  def writeTo(os: DataOutputStream) {
    os.writeByte(tpe)
    os.writeShort(channel)
    if (accumulator != null) {
      os.writeInt(accumulator.size)
      accumulator.writeTo(os);
    } else {
      os.writeInt(payload.length)
      os.write(payload)
    }
    os.write(AMQP.FRAME_END)
  }

  /**
   * Public API - retrieves the frame payload
   */
  def getPayload: Array[Byte] = {
    if (payload == null) {
      // This is a Frame we've constructed ourselves. For some reason (e.g.
      // testing), we're acting as if we received it even though it
      // didn't come in off the wire.
      accumulator.toByteArray
    } else {
      payload
    }

  }

  /**
   * Public API - retrieves a new DataInputStream streaming over the payload
   */
  def getInputStream: DataInputStream = {
    new DataInputStream(new ByteArrayInputStream(getPayload))
  }

  /**
   * Public API - retrieves a fresh DataOutputStream streaming into the accumulator
   */
  def getOutputStream: DataOutputStream = {
    new DataOutputStream(accumulator);
  }

  override def toString = {
    val sb = new StringBuffer
    sb.append("Frame(" + tpe + ", " + channel + ", ")
    if (accumulator == null) {
      sb.append(payload.length + " bytes of payload")
    } else {
      sb.append(accumulator.size + " bytes of accumulator")
    }
    sb.append(")")
    sb.toString
  }

}

