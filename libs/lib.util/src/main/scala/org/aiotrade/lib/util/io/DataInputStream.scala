package org.aiotrade.lib.util.io

import java.io._
import java.util.Calendar
import java.util.TimeZone

/**
 *
 * @Note THS' data order is opposite to java's DataInputStream
 */
class DataInputStream(in: InputStream) extends FilterInputStream(in) with DataInput {

  @throws(classOf[IOException])
  override def read(b: Array[Byte]): Int = {
    in.read(b, 0, b.length)
  }

  @throws(classOf[IOException])
  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    in.read(b, off, len)
  }

  @throws(classOf[IOException])
  def readFully(b: Array[Byte]) {
    readFully(b, 0, b.length)
  }

  @throws(classOf[IOException])
  def readFully(b: Array[Byte], off: Int, len: Int) {
    if (len < 0)
      throw new IndexOutOfBoundsException()
    var n = 0
    while (n < len) {
      val count = in.read(b, off + n, len - n)
      if (count < 0)
        throw new EOFException()
      n += count
    }
  }

  @throws(classOf[IOException])
  def skipBytes(n: Int): Int = {
    var total = 0
    var cur = 0

    while ((total<n) && {(cur = in.skip(n-total).toInt); cur > 0}) {
      total += cur
    }

    total
  }

  @throws(classOf[IOException])
  def readBoolean(): Boolean = {
    val ch = in.read()
    if (ch < 0)
      throw new EOFException()
    ch != 0
  }

  @throws(classOf[IOException])
  def readByte(): Byte = {
    val ch = in.read()
    if (ch < 0)
      throw new EOFException()
    ch.toByte
  }

  @throws(classOf[IOException])
  def readBytesString(len: Int, charSet: String): String = {
    val bytes = new Array[Byte](len)
    var readSize = 0
    do{
      readSize += read(bytes, readSize, len - readSize)
    }while(readSize < len)

    new String(bytes, charSet)
  }

  @throws(classOf[IOException])
  def readUnsignedByte(): Int = {
    val ch = in.read()
    if (ch < 0)
      throw new EOFException()
    ch
  }

  @throws(classOf[IOException])
  def readShort(): Short = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0)
      throw new EOFException()
    ((ch2 << 8) + (ch1 << 0)).toShort
  }

  @throws(classOf[IOException])
  def readUnsignedShort(): Int = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0)
      throw new EOFException()
    (ch2 << 8) + (ch1 << 0)
  }

  @throws(classOf[IOException])
  def readChar(): Char = {
    val ch1 = in.read()
    val ch2 = in.read()
    if ((ch1 | ch2) < 0)
      throw new EOFException()
    ((ch2 << 8) + (ch1 << 0)).toChar
  }

  @throws(classOf[IOException])
  def readInt(): Int = {
    val ch1 = in.read()
    val ch2 = in.read()
    val ch3 = in.read()
    val ch4 = in.read()
    if ((ch1 | ch2 | ch3 | ch4) < 0)
      throw new EOFException()
    (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0)
  }

  private val readBuffer = new Array[Byte](8)
  @throws(classOf[IOException])
  def readLong(): Long = {
    readFully(readBuffer, 0, 8)
    (readBuffer(7).toLong << 56) +
    ((readBuffer(6) & 255).toLong << 48) +
    ((readBuffer(5) & 255).toLong << 40) +
    ((readBuffer(4) & 255).toLong << 32) +
    ((readBuffer(3) & 255).toLong << 24) +
    ((readBuffer(2) & 255) << 16) +
    ((readBuffer(1) & 255) <<  8) +
    ((readBuffer(0) & 255) <<  0)
  }

  @throws(classOf[IOException])
  def readFloat(): Float = {
    java.lang.Float.intBitsToFloat(readInt())
  }

  @throws(classOf[IOException])
  def readDouble(): Double = {
    java.lang.Double.longBitsToDouble(readLong())
  }

  @deprecated
  @throws(classOf[IOException])
  def readLine(): String = {
    throw new RuntimeException("Unsupported operation.")
  }

  @throws(classOf[IOException])
  def readUTF(length: Int): String = {
    val buf = new Array[Byte](length)
    readFully(buf)
    val len = buf.indexOf(0)
    val bytes = new Array[Byte](len)
    System.arraycopy(buf, 0, bytes, 0, bytes.length)
    new String(bytes, "GB2312")
  }

  /**
   * Used to read description, the length is fixed of 178 bytes
   */
  @throws(classOf[IOException])
  def readUTF(): String = {
    readUTF(178)
  }
  
  private val cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
  def readDate(): Long = {
    val i = readInt
    cal.clear
    if (i >= 0) {
      cal.set(i / 10000, (i % 10000) / 100 - 1, i % 100)
    }
    cal.getTimeInMillis
  }
}

