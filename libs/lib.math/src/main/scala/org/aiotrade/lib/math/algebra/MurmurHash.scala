package org.aiotrade.lib.math.algebra

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * This is a very fast, non-cryptographic hash suitable for general hash-based
 * lookup.  See http://murmurhash.googlepages.com/ for more details.
 * <p/>
 * <p>The C version of MurmurHash 2.0 found at that site was ported
 * to Java by Andrzej Bialecki (ab at getopt org).</p>
 */
object MurmurHash {

  /**
   * Hashes bytes in an array.
   * @param data The bytes to hash.
   * @param seed The seed for the hash.
   * @return The 32 bit hash of the bytes in question.
   */
  def hash(data: Array[Byte], seed: Int): Int = {
    hash(ByteBuffer.wrap(data), seed)
  }

  /**
   * Hashes bytes in part of an array.
   * @param data    The data to hash.
   * @param offset  Where to start munging.
   * @param length  How many bytes to process.
   * @param seed    The seed to start with.
   * @return        The 32-bit hash of the data in question.
   */
  def hash(data: Array[Byte], offset: Int, length: Int, seed: Int): Int = {
    hash(ByteBuffer.wrap(data, offset, length), seed)
  }

  /**
   * Hashes the bytes in a buffer from the current position to the limit.
   * @param buf    The bytes to hash.
   * @param seed   The seed for the hash.
   * @return       The 32 bit murmur hash of the bytes in the buffer.
   */
  def hash(buf: ByteBuffer, seed: Int): Int = {
    // save Byte order for later restoration
    val byteOrder = buf.order
    buf.order(ByteOrder.LITTLE_ENDIAN)

    val m = 0x5bd1e995
    val r = 24

    var h = seed ^ buf.remaining

    while (buf.remaining >= 4) {
      var k = buf.getInt

      k *= m
      k ^= k >>> r
      k *= m

      h *= m
      h ^= k
    }

    if (buf.remaining > 0) {
      val finish = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN)
      // for big-endian version, use this first:
      // finish.position(4-buf.remaining())
      finish.put(buf).rewind
      h ^= finish.getInt
      h *= m
    }

    h ^= h >>> 13
    h *= m
    h ^= h >>> 15

    buf.order(byteOrder)
    h
  }


  def hash64A(data: Array[Byte], seed: Int): Long = {
    hash64A(ByteBuffer.wrap(data), seed)
  }

  def hash64A(data: Array[Byte], offset: Int, length: Int, seed: Int): Long = {
    hash64A(ByteBuffer.wrap(data, offset, length), seed)
  }

  def hash64A(buf: ByteBuffer, seed: Int): Long = {
    val byteOrder = buf.order
    buf.order(ByteOrder.LITTLE_ENDIAN)

    val m = 0xc6a4a7935bd1e995L;
    val r = 47

    var h = seed ^ (buf.remaining() * m)

    while (buf.remaining() >= 8) {
      var k = buf.getLong

      k *= m
      k ^= k >>> r
      k *= m

      h ^= k
      h *= m
    }

    if (buf.remaining > 0) {
      val finish = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
      // for big-endian version, do this first:
      // finish.position(8-buf.remaining())
      finish.put(buf).rewind
      h ^= finish.getLong
      h *= m
    }

    h ^= h >>> r
    h *= m
    h ^= h >>> r

    buf.order(byteOrder)
    h
  }

}
