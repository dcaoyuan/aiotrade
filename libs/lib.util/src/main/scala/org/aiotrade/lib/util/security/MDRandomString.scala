package org.aiotrade.lib.util.security

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom

object MDRandomString {
  private val digits = Array('0', '1', '2', '3', '4','5','6','7','8','9','a','b','c','d','e','f')
  private val random = SecureRandom.getInstance("SHA1PRNG")
}

import MDRandomString._
class MDRandomString(length: Int) {

  /**
   * @todo, choose algorithm accordinf to length
   * @see http://download-llnw.oracle.com/javase/1.4.2/docs/guide/security/CryptoSpec.html#AppA
   */
  private val algorithm: String = "SHA-1" // MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512

  def next = {
    try {
      val randomNum = random.nextInt.toString

      val md = MessageDigest.getInstance(algorithm)
      val bytes = md.digest(randomNum.getBytes)

      hexEncode(bytes)
    } catch {
      case ex: NoSuchAlgorithmException =>
    }
  }

  private def hexEncode(bytes: Array[Byte]): String = {
    val result = new StringBuilder(bytes.length)

    var i = 0
    while (i < bytes.length) {
      val b = bytes(i)
      result.append(digits((b & 0xf0) >> 4))
      result.append(digits( b & 0x0f))
      i += 1
    }
    
    result.toString
  }

}
