/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.util.io

import java.io._

class DataOutputStream(out: OutputStream) extends FilterOutputStream(out) with DataOutput {
  /**
   * The number of bytes written to the data output stream so far.
   * If this counter overflows, it will be wrapped to Integer.MAX_VALUE.
   */
  var written = 0

  /**
   * byteArr is initialized on demand by writeUTF
   */
  private var byteArr: Array[Byte] = null

  private val any = new AnyRef

  /**
   * Increases the written counter by the specified value
   * until it reaches Integer.MAX_VALUE.
   */
  private def incCount(value: Int) {
    val temp = written + value
    if (temp < 0) written = Integer.MAX_VALUE
    else written = temp
  }

  /**
   * Writes the specified byte (the low eight bits of the argument
   * <code>b</code>) to the underlying output stream. If no exception
   * is thrown, the counter <code>written</code> is incremented by
   * <code>1</code>.
   * <p>
   * Implements the <code>write</code> method of <code>OutputStream</code>.
   *
   * @param      b   the <code>byte</code> to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  override def write(b: Int): Unit = any synchronized {
    out.write(b)
    incCount(1)
  }

  /**
   * Writes <code>len</code> bytes from the specified byte array
   * starting at offset <code>off</code> to the underlying output stream.
   * If no exception is thrown, the counter <code>written</code> is
   * incremented by <code>len</code>.
   *
   * @param      b     the data.
   * @param      off   the start offset in the data.
   * @param      len   the number of bytes to write.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  override def write(b: Array[Byte], off: Int, len: Int):Unit = any synchronized{
    out.write(b, off, len)
    incCount(len)
  }

  /**
   * Flushes this data output stream. This forces any buffered output
   * bytes to be written out to the stream.
   * <p>
   * The <code>flush</code> method of <code>DataOutputStream</code>
   * calls the <code>flush</code> method of its underlying output stream.
   *
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   * @see        java.io.OutputStream#flush
   */
  @throws(classOf[IOException])
  override def flush = out.flush

  /**
   * Writes a <code>boolean</code> to the underlying output stream as
   * a 1-byte value. The value <code>true</code> is written out as the
   * value <code>(byte)1</code> the value <code>false</code> is
   * written out as the value <code>(byte)0</code>. If no exception is
   * thrown, the counter <code>written</code> is incremented by
   * <code>1</code>.
   *
   * @param      v   a <code>boolean</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeBoolean(v: Boolean) {
    out.write(if (v)1 else 0)
    incCount(1)
  }

  /**
   * Writes out a <code>byte</code> to the underlying output stream as
   * a 1-byte value. If no exception is thrown, the counter
   * <code>written</code> is incremented by <code>1</code>.
   *
   * @param      v   a <code>byte</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeByte(v: Int) {
    out.write(v)
    incCount(1)
  }

  /**
   * Writes a <code>short</code> to the underlying output stream as two
   * bytes, high byte first. If no exception is thrown, the counter
   * <code>written</code> is incremented by <code>2</code>.
   *
   * @param      v   a <code>short</code> to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeShort(v: Int) {
    out.write((v >>> 8) & 0xFF)
    out.write((v >>> 0) & 0xFF)
    incCount(2)
  }

  /**
   * Writes a <code>char</code> to the underlying output stream as a
   * 2-byte value, high byte first. If no exception is thrown, the
   * counter <code>written</code> is incremented by <code>2</code>.
   *
   * @param      v   a <code>char</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeChar(v: Int) {
    out.write((v >>> 8) & 0xFF)
    out.write((v >>> 0) & 0xFF)
    incCount(2)
  }

  /**
   * Writes an <code>int</code> to the underlying output stream as four
   * bytes, high byte first. If no exception is thrown, the counter
   * <code>written</code> is incremented by <code>4</code>.
   *
   * @param      v   an <code>int</code> to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeInt(v: Int) {
    out.write((v >>>  0) & 0xFF)
    out.write((v >>>  8) & 0xFF)
    out.write((v >>> 16) & 0xFF)
    out.write((v >>> 24) & 0xFF)
    incCount(4)
  }

  private val writeBuffer = new Array[Byte](8)

  /**
   * Writes a <code>long</code> to the underlying output stream as eight
   * bytes, high byte first. In no exception is thrown, the counter
   * <code>written</code> is incremented by <code>8</code>.
   *
   * @param      v   a <code>long</code> to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeLong(v: Long) {
    for(i <- 0 to 7){
      writeBuffer(i) = (v >>> (56 - i * 8)).asInstanceOf[Byte]
    }
    out.write(writeBuffer, 0, 8)
    incCount(8)
  }

  /**
   * Converts the float argument to an <code>int</code> using the
   * <code>floatToIntBits</code> method in class <code>Float</code>,
   * and then writes that <code>int</code> value to the underlying
   * output stream as a 4-byte quantity, high byte first. If no
   * exception is thrown, the counter <code>written</code> is
   * incremented by <code>4</code>.
   *
   * @param      v   a <code>float</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   * @see        java.lang.Float#floatToIntBits(float)
   */
  @throws(classOf[IOException])
  def writeFloat(v: Float) {
    writeInt(java.lang.Float.floatToIntBits(v))
  }

  /**
   * Converts the double argument to a <code>long</code> using the
   * <code>doubleToLongBits</code> method in class <code>Double</code>,
   * and then writes that <code>long</code> value to the underlying
   * output stream as an 8-byte quantity, high byte first. If no
   * exception is thrown, the counter <code>written</code> is
   * incremented by <code>8</code>.
   *
   * @param      v   a <code>double</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   * @see        java.lang.Double#doubleToLongBits(double)
   */
  @throws(classOf[IOException])
  def writeDouble(v :Double) {
    writeLong(java.lang.Double.doubleToLongBits(v))
  }

  /**
   * Writes out the string to the underlying output stream as a
   * sequence of bytes. Each character in the string is written out, in
   * sequence, by discarding its high eight bits. If no exception is
   * thrown, the counter <code>written</code> is incremented by the
   * length of <code>s</code>.
   *
   * @param      s   a string of bytes to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeBytes(s: String) {
    for (c <- s) out.write(c.asInstanceOf[Byte])
    incCount(s.length)
  }

  /**
   * Writes a string to the underlying output stream as a sequence of
   * characters. Each character is written to the data output stream as
   * if by the <code>writeChar</code> method. If no exception is
   * thrown, the counter <code>written</code> is incremented by twice
   * the length of <code>s</code>.
   *
   * @param      s   a <code>String</code> value to be written.
   * @exception  IOException  if an I/O error occurs.
   * @see        java.io.DataOutputStream#writeChar(int)
   * @see        java.io.FilterOutputStream#out
   */
  @throws(classOf[IOException])
  def writeChars(s: String) {
    for (v <- s) {
      out.write((v >>> 8) & 0xFF)
      out.write((v >>> 0) & 0xFF)
    }
    incCount(s.length * 2)
  }

  /**
   * Writes a string to the underlying output stream using
   * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
   * encoding in a machine-independent manner.
   * <p>
   * First, two bytes are written to the output stream as if by the
   * <code>writeShort</code> method giving the number of bytes to
   * follow. This value is the number of bytes actually written out,
   * not the length of the string. Following the length, each character
   * of the string is output, in sequence, using the modified UTF-8 encoding
   * for the character. If no exception is thrown, the counter
   * <code>written</code> is incremented by the total number of
   * bytes written to the output stream. This will be at least two
   * plus the length of <code>str</code>, and at most two plus
   * thrice the length of <code>str</code>.
   *
   * @param      str   a string to be written.
   * @exception  IOException  if an I/O error occurs.
   */
  @throws(classOf[IOException])
  def writeUTF(str: String) {
    DataOutputStream.writeUTF(str, this)
  }

  /**
   * Returns the current value of the counter <code>written</code>,
   * the number of bytes written to this data output stream so far.
   * If the counter overflows, it will be wrapped to Integer.MAX_VALUE.
   *
   * @return  the value of the <code>written</code> field.
   * @see     java.io.DataOutputStream#written
   */
  def size = written
}

object DataOutputStream{

  /**
   * Writes a string to the specified DataOutput using
   * <a href="DataInput.html#modified-utf-8">modified UTF-8</a>
   * encoding in a machine-independent manner.
   * <p>
   * First, two bytes are written to out as if by the <code>writeShort</code>
   * method giving the number of bytes to follow. This value is the number of
   * bytes actually written out, not the length of the string. Following the
   * length, each character of the string is output, in sequence, using the
   * modified UTF-8 encoding for the character. If no exception is thrown, the
   * counter <code>written</code> is incremented by the total number of
   * bytes written to the output stream. This will be at least two
   * plus the length of <code>str</code>, and at most two plus
   * thrice the length of <code>str</code>.
   *
   * @param      str   a string to be written.
   * @param      out   destination to write to
   * @return     The number of bytes written out.
   * @exception  IOException  if an I/O error occurs.
   */
  @throws(classOf[IOException])
  def writeUTF(str: String, out: DataOutput) = {
    var utflen = 0
    var count = 0

    /* use charAt instead of copying String to char array */
    for (c <- str) {
      if ((c >= 0x0001) && (c <= 0x007F)) {
        utflen += 1
      } else if (c > 0x07FF) {
        utflen += 3
      } else {
        utflen += 2
      }
    }

    if (utflen > 65535)
      throw new UTFDataFormatException(
        "encoded string too long: " + utflen + " bytes")

    var byteArr: Array[Byte] = null
    if (out.isInstanceOf[DataOutputStream]) {
      val dos = out.asInstanceOf[DataOutputStream]
      if(dos.byteArr == null || (dos.byteArr.length < (utflen+2)))
        dos.byteArr = new Array[Byte]((utflen*2) + 2)
      byteArr = dos.byteArr
    } else {
      byteArr = new Array[Byte](utflen+2)
    }

    byteArr(count) = ((utflen >>> 8) & 0xFF).asInstanceOf[Byte]
    count += 1
    byteArr(count) = ((utflen >>> 0) & 0xFF).asInstanceOf[Byte]
    count += 1

    var break = false
    for (c <- str) {
      if (!((c >= 0x0001) && (c <= 0x007F))) break=true

      if (!break){
        byteArr(count) = c.asInstanceOf[Byte]
        count += 1}
      else{
        if ((c >= 0x0001) && (c <= 0x007F)) {
          byteArr(count) = c.asInstanceOf[Byte]
          count += 1

        } else if (c > 0x07FF) {
          byteArr(count) = (0xE0 | ((c >> 12) & 0x0F)).asInstanceOf[Byte]
          count += 1
          byteArr(count) = (0x80 | ((c >>  6) & 0x3F)).asInstanceOf[Byte]
          count += 1
          byteArr(count) = (0x80 | ((c >>  0) & 0x3F)).asInstanceOf[Byte]
          count += 1
        } else {
          byteArr(count) = (0xC0 | ((c >>  6) & 0x1F)).asInstanceOf[Byte]
          count += 1
          byteArr(count) = (0x80 | ((c >>  0) & 0x3F)).asInstanceOf[Byte]
          count += 1
        }
      }
    }

    out.write(byteArr, 0, utflen+2)
    utflen + 2
  }

}
