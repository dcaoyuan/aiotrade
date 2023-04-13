package org.aiotrade.lib.math.algebra

/**
 *  <p>
 *  This produces exactly the same hash values as the final C++
 *  version of MurmurHash3 and is thus suitable for producing the same hash values across
 *  platforms.
 *  <p>
 *  The 32 bit x86 version of this hash should be the fastest variant for relatively short keys like ids.
 *  <p>
 *  Note - The x86 and x64 versions do _not_ produce the same results, as the
 *  algorithms are optimized for their respective platforms.
 *  <p>
 *  The MurmurHash3 algorithm was created by Austin Appleby and put into the public domain.  See http://code.google.com/p/smhasher/
 */
object MurmurHash3 {

  /** Returns the MurmurHash3_x86_32 hash. */
  def murmurhash3_x86_32(data: Array[Byte], offset: Int, len: Int, seed: Int): Int = {

    val c1 = 0xcc9e2d51
    val c2 = 0x1b873593

    var h1 = seed
    val roundedEnd = offset + (len & 0xfffffffc)  // round down to 4 byte block

    var i = offset
    while (i < roundedEnd) {
      // little endian load order
      var k1 = (data(i) & 0xff) | ((data(i+1) & 0xff) << 8) | ((data(i+2) & 0xff) << 16) | (data(i+3) << 24)
      k1 *= c1
      k1 = (k1 << 15) | (k1 >>> 17)  // ROTL32(k1,15)
      k1 *= c2

      h1 ^= k1
      h1 = (h1 << 13) | (h1 >>> 19)  // ROTL32(h1,13)
      h1 = h1 * 5 + 0xe6546b64
      i += 4
    }

    // tail
    var k1 = 0

    (len & 0x03) match {
      case 3 =>
        k1 = (data(roundedEnd + 2) & 0xff) << 16
        // fallthrough
      case 2 =>
        k1 |= (data(roundedEnd + 1) & 0xff) << 8
        // fallthrough
      case 1 =>
        k1 |= data(roundedEnd) & 0xff
        k1 *= c1
        k1 = (k1 << 15) | (k1 >>> 17)  // ROTL32(k1,15)
        k1 *= c2
        h1 ^= k1
    }

    // finalization
    h1 ^= len

    // fmix(h1)
    h1 ^= h1 >>> 16
    h1 *= 0x85ebca6b
    h1 ^= h1 >>> 13
    h1 *= 0xc2b2ae35
    h1 ^= h1 >>> 16

    h1
  }

}