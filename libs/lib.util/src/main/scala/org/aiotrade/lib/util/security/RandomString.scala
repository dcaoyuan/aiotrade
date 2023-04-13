package org.aiotrade.lib.util.security

import java.util.Random

object RandomString {
  // it's said that java.util.Random is thread-safe
  private val random = new Random

  private val symbols = {
    val x = new Array[Char](36)
    var i = 0
    while (i < 10) {
      x(i) = ('0' + i).toChar
      i += 1
    }
    while (i < 36) {
      x(i) = ('a' + i - 10).toChar
      i += 1
    }
    x
  }
}

import RandomString._
class RandomString(length: Int) {
  if (length < 1) throw new IllegalArgumentException("length < 1: " + length)

  def next: String = {
    val buf = new Array[Char](length)
    var i = 0
    while (i < buf.length) {
      buf(i) = symbols(random.nextInt(symbols.length))
      i += 1
    }
    new String(buf)
  }
}
