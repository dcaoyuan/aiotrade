package org.aiotrade.lib.dbfdriver

import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.util.Arrays

object Utils {
  val ALIGN_LEFT  = 10
  val ALIGN_RIGHT = 12

  /**
   * Read byte as unsigned to int
   * val b: Byte = -20
   * b && 0xFF // out 236
   */
  @throws(classOf[IOException])
  def readUnsignedInt(in: ByteBuffer): Int = in.get & 0xFF
   
  @throws(classOf[IOException])
  def readLittleEndianInt(in: ByteBuffer): Int = {
    var bigEndian = 0
    var shiftBy = 0
    while (shiftBy < 32) {
      bigEndian |= (in.get & 0xff) << shiftBy
      shiftBy += 8
    }

    bigEndian
  }

  @throws(classOf[IOException])
  def readLittleEndianShort(in: ByteBuffer): Short = {
    val low  = in.get & 0xff
    val high = in.get & 0xFF

    (high << 8 | low).toShort
  }

  def trimLeftSpaces(bytes: Array[Byte]): Array[Byte] = {
    var i = -1
    while ({i += 1; i < bytes.length && bytes(i) != ' '}) {
      // skip
    }
    
    val outBytes = new Array[Byte](bytes.length - i)
    System.arraycopy(bytes, i, outBytes, 0, bytes.length - i)
    outBytes
  }
  
  def littleEndian(value: Short): Short = {
    val num1 = value
    var mask = 0xff

    var num2 = num1 & mask
    num2 <<= 8
    mask <<= 8

    num2 |= (num1 & mask) >> 8
    num2.toShort
  }

  def littleEndian(value: Int): Int = {
    val num1 = value
    var mask = 0xff
    var num2 = 0x00

    num2 |= num1 & mask

    var i = -1
    while ({i += 1; i < 4}) {
      num2 <<= 8
      mask <<= 8
      num2 |= (num1 & mask) >> (8 * i)
    }

    num2
  }

  @throws(classOf[UnsupportedEncodingException])
  def textPadding(text: String, charsetName: String, length: Int, alignment: Int = ALIGN_LEFT, paddingByte: Byte = ' '.toByte): Array[Byte] = {
    val bytes = new Array[Byte](length)
    val srcLength = text.getBytes(charsetName).length

    if (srcLength >= length) {
      return Arrays.copyOfRange(text.getBytes(charsetName), 0, length)
    }

    Arrays.fill(bytes, paddingByte)

    alignment match {
      case ALIGN_LEFT =>
        System.arraycopy(text.getBytes(charsetName), 0, bytes, 0, srcLength)
      case ALIGN_RIGHT =>
        val t_offset = length - text.length
        System.arraycopy(text.getBytes(charsetName), 0, bytes, t_offset, srcLength)
    }

    bytes
  }

  @throws(classOf[UnsupportedEncodingException])
  def doubleFormating(doubleNum: Double, characterSetName: String, fieldLength: Int, sizeDecimalPart: Int): Array[Byte] = {
    val sizeWholePart = fieldLength - (if (sizeDecimalPart > 0) sizeDecimalPart + 1 else 0)

    val sb = new StringBuilder(fieldLength)
    var i = -1
    while ({i += 1; i < sizeWholePart}) {
      sb.append("#")
    }

    if (sizeDecimalPart > 0) {
      sb.append(".")
      var i = -1
      while ({i += 1; i < sizeDecimalPart}) {
        sb.append("0")
      }
    }

    val df = new DecimalFormat(sb.toString)
    textPadding(df.format(doubleNum), characterSetName, fieldLength, ALIGN_RIGHT)
  }
}
