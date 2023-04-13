/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.aiotrade.lib.amqp

object DataType {
  val NULL        = 0x40.toByte // fixed/0 The null value.
  val TRUE        = 0x41.toByte // fixed/0 The boolean value true.
  val FALSE       = 0x42.toByte // fixed/0 The boolean value false.
  val UBYTE       = 0x50.toByte // fixed/1 8-bit unsigned integer.
  val USHORT      = 0x60.toByte // fixed/2 16-bit unsigned integer in network byte order.
  val UINT        = 0x70.toByte // fixed/4 32-bit unsigned integer in network byte order.
  val ULONG       = 0x80.toByte // fixed/8 64-bit unsigned integer in network byte order.
  val BYTE        = 0x51.toByte // fixed/1 8-bit two's-complement integer.
  val SHORT       = 0x61.toByte // fixed/2 16-bit two's-complement integer in network byte order.
  val INT         = 0x71.toByte // fixed/4 32-bit two's-complement integer in network byte order.
  val LONG        = 0x81.toByte // fixed/8 64-bit two's-complement integer in network byte order.
  val FLOAT       = 0x72.toByte // fixed/4 IEEE 754-2008 binary32.
  val DOUBLE      = 0x82.toByte // fixed/8 IEEE 754-2008 binary64.
  val CHAR        = 0x73.toByte // fixed/4 A UTF-32 encoded unicode character.
  val TIMESTAMP   = 0x83.toByte // fixed/8 Encodes a point in time using a 64 bit signed integer representing milliseconds since Midnight Jan 1, 1970 UTC. For the purpose of this representation, milliseconds are taken to be (1/(24*60*60*1000))th of a day.
  val UUID        = 0x98.toByte // fixed/16 UUID as defined in section 4.1.2 of RFC-4122.
  val VBIN8       = 0xa0.toByte // variable/1 Up to 2^8 - 1 octets of binary data.
  val VBIN32      = 0xb0.toByte // variable/4 Up to 2^32 - 1 octets of binary data.
  val STR8_UTF8   = 0xa1.toByte // variable/1 Up to 2^8 - 1 octets worth of UTF-8 unicode.
  val STR8_UTF16  = 0xa2.toByte // variable/1 Up to 2^8 - 1 octets worth of UTF-16 unicode.
  val STR32_UTF8  = 0xb1.toByte // variable/4 Up to 2^32 - 1 octets worth of UTF-8 unicode.
  val STR32_UTF16 = 0xb2.toByte // variable/4 Up to 2^32 - 1 octets worth of UTF-16 unicode.
  val SYM8        = 0xa3.toByte // variable/1 Up to 2^8 - 1 seven bit ASCII characters representing a symbolic value.
  val SYM32       = 0xb3.toByte // variable/2 Up to 2^32 - 1 seven bit ASCII characters representing a symbolic value.
  val LIST8       = 0xc0.toByte // compound/1 Up to 2^8 - 1 list elements with total size less than 2^8 octets.
  val LIST32      = 0xd0.toByte // compound/4 Up to 2^32 - 1 list elements with total size less than 2^32 octets.
  val ARRAY8      = 0xe0.toByte // array/1 Up to 2^8 - 1 array elements with total size less than 2^8 octets.
  val ARRAY32     = 0xf0.toByte // array/4 Up to 2^32 - 1 array elements with total size less than 2^32 octets.
  val MAP8        = 0xc1.toByte // compound/1 Up to 2^8 - 1 octets of encoded map data. A map is encoded as a compound value where the constituent elements form alternating key value pairs.
  val MAP32       = 0xd1.toByte // compound/4 Up to 2^32 - 1 octets of encoded map data. See map8 above for a definition of encoded map data.
}

class DataType {
  import DataType._

  val a = Array[Byte]()
  a match {
    case Array(TRUE, _*) =>
  }
}
