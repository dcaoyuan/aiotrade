/*
 * Copyright (c) 2006-2010, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.lib.math.indicator

/**
 * @author Caoyuan Deng
 * @Note ref should implement proper hashCode and equals method,
 *       it could be baseSer or name string etc
 */

final class Id[T](val klass: Class[T], val keyRef: AnyRef, val args: Any*) {

  @inline override 
  def equals(o: Any): Boolean = {
    o match {
      case Id(klass, keyRef, args@_*) if
        (this.klass.getName == klass.getName) &&
        ((this.keyRef eq keyRef) || (this.keyRef.isInstanceOf[String] && this.keyRef == keyRef)) &&
        (this.args.size == args.size)
        =>
        val itr1 = this.args.iterator
        val itr2 = args.iterator
        while (itr1.hasNext && itr2.hasNext) {
          if (itr1.next != itr2.next) {
            return false
          }
        }
        true
      case _ => false
    }
  }

  @inline override
  def hashCode: Int = {
    var h = 17
    h = 37 * h + klass.hashCode
    h = 37 * h + keyRef.hashCode
    val itr = args.iterator
    while (itr.hasNext) {
      val more: Int = itr.next match {
        case x: Short   => x
        case x: Char    => x
        case x: Byte    => x
        case x: Int     => x
        case x: Boolean => if (x) 0 else 1
        case x: Long    => (x ^ (x >>> 32)).toInt
        case x: Float   => java.lang.Float.floatToIntBits(x)
        case x: Double  => val x1 = java.lang.Double.doubleToLongBits(x); (x1 ^ (x1 >>> 32)).toInt
        case x: AnyRef  => x.hashCode
      }
      h = 37 * h + more
    }
    h
  }

  def keyString = "(" + klass.getSimpleName + "," + keyRef + "," + args.mkString(",") + ")"

  override 
  def toString = "Id(" + klass.getName + "," + keyRef + "," + args.mkString(",") + ")"
}

object Id {
  def apply[T](klass: Class[T], keyRef: AnyRef, args: Any*) = new Id(klass, keyRef, args: _*)
  def unapplySeq[T](e: Id[T]): Option[(Class[T], AnyRef, Seq[Any])] = Some((e.klass, e.keyRef, e.args))

  // simple test
  def main(args: Array[String]) {
    val keya = "abc"
    val keyb = "abc"

    val id1 = Id(classOf[String], keya, 1)
    val id2 = Id(classOf[String], keyb, 1)
    println(id1 == id2)

    val id3 = Id(classOf[String], keya)
    val id4 = Id(classOf[String], keyb)
    println(id3 == id4)

    val id5 = Id(classOf[Indicator], keya, org.aiotrade.lib.math.timeseries.TFreq.ONE_MIN)
    val id6 = Id(classOf[Indicator], keyb, org.aiotrade.lib.math.timeseries.TFreq.withName("1m").get)
    println(id5 == id6)
  }
}
