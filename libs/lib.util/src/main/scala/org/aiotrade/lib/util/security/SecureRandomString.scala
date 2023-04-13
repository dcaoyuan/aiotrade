package org.aiotrade.lib.util.security

import java.math.BigInteger
import java.security.SecureRandom

object SercureRandomString {
  private val random = new SecureRandom
  private val radix = 32 // max 36: 26 alphas + 10 numbers
  private val bitsPerChar = math.log(radix) / math.log(2)
}

/**
 * @Note: the length is not guaranteed
 */
import SercureRandomString._
class SercureRandomString(length: Int) {
  if (length < 1) throw new IllegalArgumentException("length < 1: " + length)

  private val nBits = math.ceil(bitsPerChar * length).toInt

  private def next: String = {
    // 0 to 2 ^ nBits - 1
    new BigInteger(nBits, random).toString(radix)
  }
}