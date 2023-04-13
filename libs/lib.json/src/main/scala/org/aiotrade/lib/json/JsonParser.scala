/*
 * Copyright (c) 2006-2011, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.json

import org.aiotrade.lib.io.CharArray
import org.aiotrade.lib.io.NullCharArray
import org.aiotrade.lib.io.ByteStack
import org.aiotrade.lib.io.RestReader
import scala.annotation.tailrec

/**
 * @author Caoyuan Deng
 *
 * @param data that to be read
 * @param start current position in the buffer
 * @param end end position in the buffer (one past last valid index)
 * @param in optional reader to obtain data from
 */


class JsonParser(val rest: RestReader) {
  import Json.Event._

  private var event = 0  // last event read

  // idea - if someone passes us a CharArrayReader, we could
  // directly use that buffer as it's def.

  // We need to keep some state in order to (at a minimum) know if
  // we should skip ',' or ':'.
  private val stack = new ByteStack
  private var state: Byte = 0

  // parser states stored in the stack
  private val STATE_OBJ_START:   Byte = 1  // '{' just read
  private val STATE_ARR_START:   Byte = 2  // '[' just read
  private val STATE_ARR_ELEMENT: Byte = 3  // array element just read
  private val STATE_MEM_NAME:    Byte = 4  // object member name  (map key) just read
  private val STATE_MEM_VAL:     Byte = 5  // object member value (map val) just read

  // vals and their states
  private var boolVal = false     // boolean value read
  private var longVal = 0L        // long value read

  private var valState = 0            // info about value that was just read (or is in the middle of being read)
  private var numState = 0            // current state while reading a number
  private val HAS_FRACTION = 0x01     // numState flag, '.' already read
  private val HAS_EXPONENT = 0x02     // numState flag, '[eE][+-]?[0-9]' already read

  // temporary output buffer
  private val out = new CharArray(64)
  /**
   * Returns the next event encountered in the JSON stream, one of
   * <ul>
   * <li>{@link #STRING}</li>
   * <li>{@link #LONG}</li>
   * <li>{@link #NUMBER}</li>
   * <li>{@link #BIGNUMBER}</li>
   * <li>{@link #BOOLEAN}</li>
   * <li>{@link #NULL}</li>
   * <li>{@link #OBJECT_START}</li>
   * <li>{@link #OBJECT_END}</li>
   * <li>{@link #OBJECT_END}</li>
   * <li>{@link #ARRAY_START}</li>
   * <li>{@link #ARRAY_END}</li>
   * <li>{@link #EOF}</li>
   * </ul>
   */
  def nextEvent: Int = {
    valState match {
      case STRING    => readStringChars(NullCharArray, rest.pos)
      case BIGNUMBER => continueNumber(NullCharArray)
      case _ =>
    }

    valState = 0
    // TODO: factor out getSkipWS to here and check speed
    state match {
      case 0 =>
        event = next(rest.nextSkipWs)
        event
      case STATE_OBJ_START =>
        rest.nextSkipWs match {
          case '}' =>
            state = stack.pop
            event = OBJECT_END
            event
          case '"' =>
            state = STATE_MEM_NAME
            valState = STRING
            event = STRING
            event
          case _ => throw err("Expected string")
        }
      case STATE_MEM_NAME =>
        rest.nextSkipWs match {
          case ':' =>
            state = STATE_MEM_VAL  // set state first because it might be pushed...
            event = next(rest.next)
            event
          case _ => throw err("Expected {key,value} separator ':', but get '" + rest.last + "'")
        }
      case STATE_MEM_VAL =>
        rest.nextSkipWs match {
          case '}' =>
            state = stack.pop
            event = OBJECT_END
            event
          case ',' =>
            rest.nextSkipWs match {
              case '"' =>
                state = STATE_MEM_NAME
                valState = STRING
                event = STRING
                event
              case _ => throw err("Expected string")
            }
          case _ => throw err("Expected ',' or '}'")
        }
      case STATE_ARR_START =>
        rest.nextSkipWs match {
          case ']' =>
            state = stack.pop
            event = ARRAY_END
            event
          case c =>
            state = STATE_ARR_ELEMENT  // set state first, might be pushed...
            event = next(c)
            event
        }
      case STATE_ARR_ELEMENT =>
        rest.nextSkipWs match {
          case ']' =>
            state = stack.pop
            event = ARRAY_END
            event
          case ',' =>
            // state = STATE_ARR_ELEMENT
            event = next(rest.next)
            event
          case _ => throw err("Expected ',' or ']'")
        }
      case _ => 0
    }
  }

  def lastEvent = event

  /**
   * Return the next event when parser is in a neutral state (no
   * map separators or array element separators to read
   */
  @tailrec
  private def next(c: Char): Int = c match {
    // @todo in case of '\n' try and keep track of linecounts?
    case ' ' | '\t' | '\r' | '\n' => next(rest.next)
    case '"' =>
      valState = STRING
      STRING
    case '{' =>
      stack.push(state)
      state = STATE_OBJ_START
      OBJECT_START
    case '[' =>
      stack.push(state)
      state = STATE_ARR_START
      ARRAY_START
    case '0' =>
      out.reset
      // special case '0'?  if next char isn't '.' val=0
      // test next char
      rest.next match {
        case '.' =>
          rest.backup(1)
          readNumber('0', false)
          valState
        case c1 if !isDigit(c1) =>
          out.unsafePut('0')
          rest.backup(1)
          longVal = 0
          valState = LONG
          LONG
        case _ => throw err("Leading zeros not allowed")
      }
    case _ if isDigit(c) && c != '0' =>
      out.reset
      longVal = readNumber(c, false)
      valState
    case '-' =>
      out.reset
      out.unsafePut('-')
      val c1 = rest.next
      if (!isDigit(c1)) throw err("expected digit after '-'")
      longVal = readNumber(c1, true)
      valState
    case 't' =>
      valState = BOOLEAN
      // TODO: test performance of this non-branching inline version.
      // if ((('r'-getChar)|('u'-getChar)|('e'-getChar)) != 0) err("")
      rest.expect(Json.TRUE_CHARS)
      boolVal = true
      BOOLEAN
    case 'f' =>
      valState = BOOLEAN
      rest.expect(Json.FALSE_CHARS)
      boolVal = false
      BOOLEAN
    case 'n' =>
      valState = NULL
      rest.expect(Json.NULL_CHARS)
      NULL
    case rest.EOF =>
      if (stack.getLevel > 0) throw new RuntimeException("Premature EOF")
      EOF
    case _ => throw err(null)
  }


  /**
   * Returns the long read... only significant if valState==LONG after this call.
   *
   * Max value of Long in Java is 2^63 - 1 =  9,223,372,036,854,775,807, with 19 digits
   * Mix value of Long in Java is -2^63    = -9,223,372,036,854,775,808
   *
   * @param fstChar should be the first numeric digit read.
   * @return real Long value of number if is big number, or 0 if it has fraction or exp
   */
  private def readNumber(fstChar: Char, isNeg: Boolean): Long = {
    // unsafe OK since we know output is big enough
    out.unsafePut(fstChar)

    // We build up the number in the negative plane since it's larger (by one) than
    // the positive plane.
    val v: Long = '0' - fstChar
    loopInReadNumber(rest.next, v, 1, isNeg)
  }
  
  @tailrec
  private def loopInReadNumber(c: Char, v: Long, i: Int, isNeg: Boolean): Long = c match {
    case _ if i > 21 =>
      numState = 0
      valState = BIGNUMBER
      0
    case _ if isDigit(c) =>
      out.unsafePut(c)
      val v1 = v * 10 - (c - '0')
      loopInReadNumber(rest.next, v1, i + 1, isNeg)
    case '.' =>
      out.unsafePut('.')
      valState = readFrac(out, 22 - i)
      0
    case 'e' | 'E' =>
      out.unsafePut(c)
      numState = 0
      valState = readExp(out, 22 - i)
      0
    case _ =>
      // return the number, relying on nextEvent to return an error
      // for invalid chars following the number.
      if (c != rest.EOF) rest.backup(1)

      // the max number of digits we are reading only allows for
      // a long to wrap once, so we can just check if the sign is
      // what is expected to detect an overflow.
      valState = if (v <= 0) LONG else BIGNUMBER
      // -0 is allowed by the spec
      if (isNeg) v else -v
  }

  // read digits right of decimal point
  private def readFrac(chars: CharArray, max: Int): Int = {
    numState = HAS_FRACTION  // deliberate set instead of '|'

    loopInReadFrac(chars, max - 1, rest.next)
  }
  
  @tailrec
  private def loopInReadFrac(chars: CharArray, max: Int, c: Char): Int = c match {
    case _ if max < 0 => BIGNUMBER
    case c if isDigit(c) =>
      chars.put(c)
      loopInReadFrac(chars, max - 1, rest.next)
    case 'e' | 'E' =>
      chars.put(c)
      readExp(chars, max - 1)
    case  _ =>
      if (c != rest.EOF) rest.backup(1)
      NUMBER
  }

  // call after 'e' or 'E' has been seen to read the rest of the exponent
  private def readExp(chars: CharArray, max: Int): Int = {
    numState |= HAS_EXPONENT

    var c = rest.next
    var max1 = max - 1

    if (c == '+' || c == '-') {
      chars.put(c)
      c = rest.next
      max1 -= 1
    }

    // make sure at least one digit is read.
    if (!isDigit(c)) {
      throw err("missing exponent number")
    }
    chars.put(c)

    readExpDigits(chars, max1 - 1, rest.next)
  }

  // continuation of readExpStart
  @tailrec
  private def readExpDigits(chars: CharArray, max: Int, c: Char): Int = {
    if (max < 0) {
      BIGNUMBER
    } else if (isDigit(c)) {
      chars.put(c)
      readExpDigits(chars, max - 1, rest.next)
    } else {
      if (c != rest.EOF) rest.backup(1)
      NUMBER
    }
  }

  private def continueNumber(chars: CharArray) {
    if (chars != out) chars.put(out)

    if ((numState & HAS_EXPONENT) != 0) {
      readExpDigits(chars, Integer.MAX_VALUE, rest.next)
    } else if (numState != 0) {
      readFrac(chars, Integer.MAX_VALUE)
    } else {
      loopInContinueNumber(chars, rest.next)
    }
  }
  
  @tailrec
  private def loopInContinueNumber(chars: CharArray, c: Char): Unit = c match {
    case _ if isDigit(c) =>
      chars.put(c)
      loopInContinueNumber(chars, rest.next)
    case '.' =>
      chars.put(c)
      readFrac(chars, Integer.MAX_VALUE)
    case 'e' | 'E' =>
      chars.put(c)
      readExp(chars, Integer.MAX_VALUE)
    case _ => if (c != rest.EOF) rest.backup(1)
  }


  // backslash has already been read when this is called
  private def readEscapedChar: Char = {
    rest.next match {
      case 'b'  => '\b'
      case 't'  => '\t'
      case 'n'  => '\n'
      case 'f'  => '\f'
      case 'r'  => '\r'
      case '/'  => '/'
      case '\\' => '\\'
      case '"'  => '"'
      case 'u'  => (  (hexVal(rest.next) << 12)
                    | (hexVal(rest.next) <<  8)
                    | (hexVal(rest.next) <<  4)
                    | (hexVal(rest.next))).toChar
      case _ => throw err("Invalid character escape in string")
    }
  }

  private def readStringChars: CharArray = {
    val start = rest.pos
    loopInReadStringChars(start)
  }

  // @Note should be aware that when call rest.charAt(i), i should < rest.end
  @tailrec
  private def loopInReadStringChars(i: Int): CharArray = {
    if (i >= rest.end || rest.charAt(i) == '\\') {
      // could not decide yet, need going on use a bigger buffer
      out.reset
      readStringChars(out, i)
      out
    } else if (rest.charAt(i) == '"') {
      // found end of string, task finish
      val tmp = new CharArray(rest.data, rest.pos, i) // directly use input buffer, batch copy data
      rest.seek(i + 1) // move pos till consumer this '"'
      tmp
    } else {
      loopInReadStringChars(i + 1)
    }
  }


  /**
   * from is the pointer to the middle of a buffer to start scanning for a
   * non-string character ('"' or "/").  pos =< mid < end
   * this should be faster for strings with fewer escapes, but probably slower for many escapes.
   */
  private def readStringChars(output: CharArray, from: Int) {
    val from1 = if (from >= rest.end) {
      output.put(rest.data, rest.pos, from - rest.pos)
      rest.fillMore
      rest.pos
    } else from

    rest.charAt(from1) match {
      case '"' =>
        flush(output, from1)
        rest.skip(1) // skip this '"'
      case '\\' =>
        flush(output, from1)
        rest.skip(1) // skip this '\\'
        output.put(readEscapedChar)
        readStringChars(output, rest.pos)
      case _ =>
        readStringChars(output, from1 + 1)
    }
  }

  /**
   * Writes to output from rest's current pos till exactly before the <p>before</p>,
   * and seek to before
   */
  private def flush(output: CharArray, before: Int) = {
    val len = before - rest.pos
    if (len > 0) {
      output.put(rest.data, rest.pos, len)
    }
    rest.seek(before)
  }

  private def isDigit(c: Char) = c >= '0' && c <= '9'

  /**
   * @param hex char: 0-9, A-F, a-f
   */
  private def hexVal(c: Char): Int = {
    if (c >= '0' && c <= '9') {
      c - '0' // 0 to 9
    } else if (c >= 'A' && c <= 'F') {
      c - 'A' // 10 to 15
    } else if (c >= 'a' && c <= 'f') {
      c - 'a' // 10 to 15
    } else {
      throw err("invalid hex digit")
    }
  }

  def goto(what: Int) {
    valState match {
      case `what` =>
        valState = 0
      case 0 =>
        nextEvent      // TODO
        if (valState != what) {
          throw err("type mismatch")
        }
        valState = 0
      case _ => throw err("type mismatch")
    }
  }

  /** Returns the characters of a JSON string value, decoding any escaped characters.
   * <p/>The underlying buffer of the returned <code>CharArray</code> should *not* be
   * modified as it may be shared with the input buffer.
   * <p/>The returned <code>CharArray</code> will only be valid up until
   * the next JsonParser method is called.  Any required data should be
   * read before that point.
   */
  def getStringChars = {
    goto(STRING)
    readStringChars
  }

  /** Reads a JSON string into the output, decoding any escaped characters. */
  def getStringChars(output: CharArray) = {
    goto(STRING)
    readStringChars(output, rest.pos)
  }

  /**
   * Returns the characters of a JSON numeric value.
   * <p/>The underlying buffer of the returned <code>CharArray</code> should *not* be
   * modified as it may be shared with the input buffer.
   * <p/>The returned <code>CharArray</code> will only be valid up until
   * the next JsonParser method is called. Any required data should be
   * read before that point.
   */
  def getNumberChars: CharArray = {
    val ev = if (valState == 0) nextEvent else 0

    valState match {
      case LONG | NUMBER =>
        valState = 0
        out
      case BIGNUMBER =>
        continueNumber(out)
        valState = 0
        out
      case _ => throw err("Unexpected " + ev)
    }
  }

  /** Reads a numeric value into the output. */
  def getNumberChars(output: CharArray) {
    val ev = if (valState == 0) nextEvent else 0

    valState match {
      case LONG | NUMBER =>
        output.put(out)
      case BIGNUMBER =>
        continueNumber(output)
      case _ => throw err("Unexpected " + ev)
    }

    valState = 0
  }

  /** Returns the JSON string value, decoding any escaped characters. */
  def getString = getStringChars.toString

  /** Reads a number from the input stream and parses it as a long, only if
   * the value will in fact fit into a signed 64 bit integer. */
  def getLong = {
    goto(LONG)
    longVal
  }

  /** Reads a number from the input stream and parses it as a double */
  def getDouble = {
    getNumberChars.toString.toDouble
  }

  /** Reads a boolean value */
  def getBoolean = {
    goto(BOOLEAN)
    boolVal
  }

  /** Reads a null value */
  def getNull = goto(NULL)

  private def err(msg: String) = {
    // We can't tell if EOF was hit by comparing pos =< end
    // because the illegal char could have been the last in the buffer
    // or in the stream. To deal with this, the "eof" var was introduced

    val str = "char='" + (if (rest.pos < rest.end) rest.last.asInstanceOf[Char] + "'" else "(EOF)")
    val pos = "position=" + rest.count
    val tot = str + ", " + pos
    val msg1 = if (msg == null) {
      if (rest.pos >= rest.end) "Unexpected EOF" else "JSON Parse Error"
    } else msg

    new RuntimeException(msg1 + ": " + tot)
  }

  override def toString = {
    "start=" + rest.pos + ",end=" + rest.end + ",state=" + stack.peak + ",valState=" + valState
  }

}
