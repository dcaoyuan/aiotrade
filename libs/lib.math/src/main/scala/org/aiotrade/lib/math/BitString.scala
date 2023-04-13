package org.aiotrade.lib.math

import java.math.BigInteger
import java.util.Random

final class BitString private (private var data: Array[Int]) extends Cloneable with Serializable {

  val length = data.length

  /**
   * Returns the bit at the specified index.
   * @param index The index of the bit to look-up (0 is the least-significant bit).
   * @return A boolean indicating whether the bit is set or not.
   * @throws IndexOutOfBoundsException If the specified index is not a bit
   * position in this bit string.
   */
  def getBit(index: Int): Boolean = {
    import BitString._
    assertValidIndex(index)
    val word = index / WORD_LENGTH
    val offset = index % WORD_LENGTH
    (data(word) & (1 << offset)) != 0
  }

  /**
   * Sets the bit at the specified index.
   * @param index The index of the bit to set (0 is the least-significant bit).
   * @param set A boolean indicating whether the bit should be set or not.
   * @throws IndexOutOfBoundsException If the specified index is not a bit
   * position in this bit string.
   */
  def setBit(index: Int, set: Boolean) {
    import BitString._
    assertValidIndex(index)
    val word = index / WORD_LENGTH
    val offset = index % WORD_LENGTH
    if (set) {
      data(word) |= (1 << offset)
    } else { // Unset the bit.
      data(word) &= ~(1 << offset)
    }
  }

  /**
   * Inverts the value of the bit at the specified index.
   * @param index The bit to flip (0 is the least-significant bit).
   * @throws IndexOutOfBoundsException If the specified index is not a bit
   * position in this bit string.     
   */
  def flipBit(index: Int) {
    import BitString._
    assertValidIndex(index)
    val word = index / WORD_LENGTH
    val offset = index % WORD_LENGTH
    data(word) ^= (1 << offset)
  }


  /**
   * Helper method to check whether a bit index is valid or not.
   * @param index The index to check.
   * @throws IndexOutOfBoundsException If the index is not valid.
   */
  private def assertValidIndex(index: Int) {
    if (index >= length || index < 0) {
      throw new IndexOutOfBoundsException("Invalid index: " + index + " (length: " + length + ")");
    }
  }

  /**
   * @return The number of bits that are 1s rather than 0s.
   */
  def countSetBits: Int = {
    var count = 0
    var i = 0
    while (i < length) {
      var x = data(i)
      while (x != 0) {
        x &= (x - 1) // Unsets the least significant on bit.
        count += 1 // Count how many times we have to unset a bit before x equals zero.
      }
      i += 1
    }
    count
  }


  /**
   * @return The number of bits that are 0s rather than 1s.
   */
  def countUnsetBits: Int = {
    length - countSetBits
  }


  /**
   * Interprets this bit string as being a binary numeric value and returns
   * the integer that it represents.
   * @return A {@link BigInteger} that contains the numeric value represented
   * by this bit string.
   */
  def toNumber: BigInteger = {
    new BigInteger(toString, 2)
  }


  /**
   * An efficient method for exchanging data between two bit strings.  Both bit strings must
   * be long enough that they contain the full length of the specified substring. 
   * @param other The bitstring with which this bitstring should swap bits.
   * @param start The start position for the substrings to be exchanged.  All bit
   * indices are big-endian, which means position 0 is the rightmost bit.
   * @param length The number of contiguous bits to swap.
   */
  def swapSubstring(other: BitString, start: Int, length: Int) {
    import BitString._
    
    assertValidIndex(start)
    other.assertValidIndex(start)
        
    var word = start / WORD_LENGTH

    val partialWordSize = (WORD_LENGTH - start) % WORD_LENGTH
    if (partialWordSize > 0) {
      swapBits(other, word, 0xFFFFFFFF << (WORD_LENGTH - partialWordSize))
      word += 1
    }

    var remainingBits = length - partialWordSize
    val stop = remainingBits / WORD_LENGTH
    var i = word
    while (i < stop) {
      val temp = data(i)
      data(i) = other.data(i)
      other.data(i) = temp
      i += 1
    }

    remainingBits %= WORD_LENGTH
    if (remainingBits > 0) {
      swapBits(other, word, 0xFFFFFFFF >>> (WORD_LENGTH - remainingBits))
    }
  }


  /**
   * @param other The BitString to exchange bits with.
   * @param word The word index of the word that will be swapped between the two bit strings.
   * @param swapMask A mask that specifies which bits in the word will be swapped.
   */
  private def swapBits(other: BitString, word: Int, swapMask: Int) {
    val preserveMask = ~swapMask
    val preservedThis = data(word) & preserveMask
    val preservedThat = other.data(word) & preserveMask
    val swapThis = data(word) & swapMask
    val swapThat = other.data(word) & swapMask
    data(word) = preservedThis | swapThat
    other.data(word) = preservedThat | swapThis
  }

  override
  def toString = {
    val sb = new StringBuilder()
    var i = length - 1
    while (i >= 0) {
      sb.append(if (getBit(i)) '1' else '0')
      i -= 1
    }
    sb.toString
  }

  override
  def clone: BitString = {
    try {
      val x = super.clone.asInstanceOf[BitString]
      x.data = data.clone
      x
    } catch { 
      case ex: CloneNotSupportedException =>
        // Not possible.
        throw new InternalError("Cloning failed.").initCause(ex)
    }
  }

  override
  def equals(o: Any) = o match {
    case that: BitString => length == that.length && java.util.Arrays.equals(data, that.data)
    case _ => false
  }

  override
  def hashCode: Int = {
    31 * length + java.util.Arrays.hashCode(data)
  }
}

object BitString {
  private val WORD_LENGTH = 32

  /**
   * Creates a bit string of the specified length with all bits
   * initially set to zero (off).
   * @param length The number of bits.
   */
  def apply(length: Int): BitString = {
    if (length < 0) {
      throw new IllegalArgumentException("Length must be non-negative.")
    }
    new BitString(new Array[Int]((length + WORD_LENGTH - 1) / WORD_LENGTH))
  }


  /**
   * Creates a bit string of the specified length with each bit set
   * randomly (the distribution of bits is uniform so long as the output
   * from the provided RNG is also uniform).  Using this constructor is
   * more efficient than creating a bit string and then randomly setting
   * each bit individually.
   * @param length The number of bits.
   * @param rng A source of randomness.
   */
  def apply(length: Int, rng: Random): BitString = {
    val instance = apply(length)
    val data = instance.data
    // We can set bits 32 at a time rather than calling rng.nextBoolean()
    // and setting each one individually.
    var i = 0
    while (i < data.length) {
      data(i) = rng.nextInt
      i += 1
    }
    // If the last word is not fully utilised, zero any out-of-bounds bits.
    // This is necessary because the countSetBits() methods will count
    // out-of-bounds bits.
    val bitsUsed = length % WORD_LENGTH
    if (bitsUsed < WORD_LENGTH) {
      val unusedBits = WORD_LENGTH - bitsUsed
      val mask = 0xFFFFFFFF >>> unusedBits
      data(data.length - 1) &= mask
    }
    instance
  }


  /**
   * Initialises the bit string from a character string of 1s and 0s
   * in big-endian order.
   * @param value A character string of ones and zeros.
   */    
  def apply(value: String): BitString = {
    val instance = apply(value.length)
    var i = 0
    while (i < value.length) {
      if (value.charAt(i) == '1') {
        instance.setBit(value.length - (i + 1), true)
      } else if (value.charAt(i) != '0') {
        throw new IllegalArgumentException("Illegal character at position " + i)
      }
      i += 1
    }
    instance
  }
}
