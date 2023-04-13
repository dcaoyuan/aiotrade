package org.aiotrade.lib.io

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException

class DecodeException(msg: String) extends Exception(msg)

object Encoding {


  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def  encodeBoolean(value: Boolean): Array[Byte] = {
    val baos = new ByteArrayOutputStream(1)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeBoolean(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeByte(value: Byte): Array[Byte] = {
    val baos = new ByteArrayOutputStream(1)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeByte(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeChar(value: Char): Array[Byte] = {
    val baos = new ByteArrayOutputStream(2)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeChar(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeDouble(value: Double): Array[Byte] = {
    val baos = new ByteArrayOutputStream(8)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeDouble(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeFloat(value: Float): Array[Byte] = {
    val baos = new ByteArrayOutputStream(4)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeFloat(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeInt(value: Int): Array[Byte] = {
    val baos = new ByteArrayOutputStream(4)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeInt(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeLong(value: Long): Array[Byte] = {
    val baos = new ByteArrayOutputStream(8)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeLong(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeShort(value: Short): Array[Byte] = {
    val baos = new ByteArrayOutputStream(2)
    val dos = new DataOutputStream(baos)

    try {
      dos.writeShort(value)
    } catch {case ioe: IOException =>}

    baos.toByteArray
  }

  /**
   * Encodes the specified value, returning the result as a byte array.
   *
   * @param value
   *            the value to encode
   * @return a byte array containing the encoded value
   */
  def encodeString(value: String): Array[Byte] = {
    val baos = new ByteArrayOutputStream
    val dos = new DataOutputStream(baos)

    try {
      dos.writeUTF(value)
    } catch {case ioe: IOException =>
    }

    baos.toByteArray
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeBoolean(buffer: Array[Byte]): Boolean = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readBoolean
    } catch {case ioe: IOException => throw new DecodeException("bad boolean")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeByte(buffer: Array[Byte]): Byte = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readByte
    } catch {case ioe: IOException => throw new DecodeException("bad byte")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeChar(buffer: Array[Byte]): Char = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readChar
    } catch {case ioe: IOException => throw new DecodeException("bad char")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeDouble(buffer: Array[Byte]): Double = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readDouble
    } catch {case ioe: IOException => throw new DecodeException("bad double")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeFloat(buffer: Array[Byte]): Float = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readFloat
    } catch {case ioe: IOException => throw new DecodeException("bad float")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeInt(buffer: Array[Byte]): Int = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readInt
    } catch {case ioe: IOException => throw new DecodeException("bad int")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeLong(buffer: Array[Byte]): Long = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readLong
    } catch {case ioe: IOException => throw new DecodeException("bad long")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeShort(buffer: Array[Byte]): Short = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readShort
    } catch {case ioe: IOException => throw new DecodeException("bad short")}
  }

  /**
   * Decodes and returns the value stored in the specified buffer.
   *
   * @param buffer
   *            the buffer containing the encoded value
   * @return the decoded value
   * @exception DecodeException
   *                if the value could not be decoded
   */
  @throws(classOf[DecodeException])
  def decodeString(buffer: Array[Byte]): String = {
    val bais = new ByteArrayInputStream(buffer)
    val dis = new DataInputStream(bais)

    try {
      return dis.readUTF
    } catch {case ioe: IOException => throw new DecodeException("bad string")}
  }

  /**
   * Reverses the specified byte array in-place.
   *
   * @param buffer
   *            the byte array to reverse
   */
  @throws(classOf[DecodeException])
  def reverse(buffer: Array[Byte]) {
    var i = 0
    var j = buffer.length
    while (i < j) {
      val tmp = buffer(i)
      buffer(i) = buffer(j)
      buffer(j) = tmp
      i += 1
      j -= 1
    }
  }

}
