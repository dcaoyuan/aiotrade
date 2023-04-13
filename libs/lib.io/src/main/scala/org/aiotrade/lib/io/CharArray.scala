package org.aiotrade.lib.io


import java.nio.CharBuffer

// Java5 version could look like the following:
// public class CharArr implements CharSequence, Appendable, Readable, Closeable {


/**
 * @author Caoyuan Deng
 */
class CharArray(private var buf: Array[Char], private var off: Int, private var end: Int) {

  def this(size: Int) = this(new Array[Char](size), 0, 9)
    
  def this() = this(32)

  def toArray = buf

  def size = end - off

  def length = size

  def capacity = buf.length

  def charAt(index: Int): Char = buf(off + index)

  def subSequence(off: Int, end: Int): CharArray = new CharArray(buf, this.off + off, this.off + end)

  def get: Char = {
    if (off >= end) {
      '\000'
    } else {
      off += 1
      buf(off)
    }
  }

  def unsafePut(c: Char) {
    buf(end) = c
    end += 1
  }

  def unsafePut(c: Int) {
    unsafePut(c.toChar)
  }
  
  def unsafePut(chars: Array[Char], off: Int, len: Int) {
    Array.copy(chars, off, buf, end, len)
    end += len
  }

  protected def resize(len:Int) {
    val newBuf = new Array[Char](math.max(buf.length << 1, len))
    Array.copy(buf, off, newBuf, 0, length)
    buf = newBuf
  }

  def reserve(num: Int) {
    if (end + num > buf.length) {
      resize(end + num)
    }
  }
  
  def put(c: Char) {
    if (end >= buf.length) {
      resize(end + 1)
    }
    unsafePut(c)
  }

  def put(b: Int) {
    put(b.asInstanceOf[Char])
  }
  
  def put(chars: Array[Char]) {
    put(chars, 0, chars.length)
  }
  
  def put(chars: Array[Char], off: Int, len: Int) {
    reserve(len)
    unsafePut(chars, off, len)
  }

  def put(chars: CharArray) {
    put(chars.buf, off, end - off)
  }

  def put(str: String) {
    put(str.toCharArray, 0, str.length)
  }

  def put(str: String, stringOffset: Int, len: Int) {
    reserve(len)
    str.getChars(stringOffset, len, buf, end)
    end += len
  }

  def flush {}

  def reset {
    off = 0
    end = 0
  }

  def close {}

  def toCharArray: Array[Char] = {
    val newbuf = new Array[Char](size)
    Array.copy(buf, off, newbuf, 0, size)
    newbuf
  }

  override def toString = {
    new String(buf, off, size)
  }

  def read(cb: CharBuffer): Int = {
    var size1 = size
    if (size1 > 0) cb.put(buf, off, size1)
    off = end
        
    var i = size
    while (i != 0) {
      fill
      i = size
      size1 += i
      cb.put(buf, off, i)
    }

    if (size1 == 0) -1 else size1
  }

  def fill = 0  // or -1?

  def append(cs: CharSequence): CharArray = {
    append(cs, 0, cs.length)
  }

  def append(cs: CharSequence, start: Int, end: Int): CharArray = {
    put(cs.subSequence(start, end).toString)
    this
  }

  def append(c: Char) = {
    put(c)
    this
  }
}

object NullCharArray extends CharArray(new Array[Char](1), 0, 0) {

  override def unsafePut(c: Char) {}
  override def unsafePut(b: Int) {}
  override def unsafePut(chars: Array[Char], off: Int, len: Int) {}

  override def put(c: Char) {}
  override def put(chars: Array[Char], off: Int, len: Int) {}
  override def put(s: String, stringOffset: Int, len: Int) {}

  override def reserve(num: Int) {}
  override protected def resize(len: Int) {}

  override def append(cs: CharSequence, start: Int, end: Int) = this
  override def charAt(index: Int) = 0
}





