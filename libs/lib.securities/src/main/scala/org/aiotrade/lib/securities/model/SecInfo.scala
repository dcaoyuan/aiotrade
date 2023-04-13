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
package org.aiotrade.lib.securities.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._


final class SecInfo extends BelongsToSec {
  
  /** 
   * @note in case of BelongsToSec.self is SecInfo, the sec.uniSymbol will be fetched from its secInfo
   * i.e. cycle assignment will occur, we should avoid this by override sec_=(Sec) 
   */
  override def sec_=(sec: Sec) {
    _sec = sec
  }

  var validFrom: Long = _
  var validTo: Long = _
  var name: String = ""
  var totalShare: Long = _
  var freeFloat: Long = _
  var tradingUnit: Int = 100
  var upperLimit: Double = -1
  var lowerLimit: Double = -1

//  private var _id: Long = _
//  def id = _id
//  def id_= (id: Long){_id = id}

  override def toString = {
    "SecInfo(uniSymbol=" + uniSymbol + ")"
  }

  final def copyFrom(another: SecInfo) {
    this.validFrom = another.validFrom
    this.validTo = another.validTo
    this.uniSymbol = another.uniSymbol
    this.name = another.name
    this.totalShare = another.totalShare
    this.freeFloat = another.freeFloat
  }

  def copyFromWithoutName(another: SecInfo) {
    this.validFrom = another.validFrom
    this.validTo = another.validTo
    this.totalShare = another.totalShare
    this.freeFloat = another.freeFloat
  }

  override def equals(a: Any): Boolean = a match {
    case that: SecInfo => (this.sec eq that.sec) && this.validFrom == that.validFrom
    case _ => false
  }

  final def valueNonEquals(that : SecInfo): Boolean = !valueEquals(that)
  final def valueEquals(that: SecInfo): Boolean = {
    valueEquals(this.validFrom, that.validFrom) &&
    valueEquals(this.validTo, that.validTo) &&
    valueEquals(this.totalShare, that.totalShare) &&
    valueEquals(this.freeFloat, that.freeFloat)
  }

  final def diffValues(that: SecInfo): List[(String, Long, Long)] = {
    var result: List[(String, Long, Long)] = Nil

    if (valueNonEquals(this.validFrom, that.validFrom))
      result = ("validFrom", this.validFrom, that.validFrom) :: result

    if (valueNonEquals(this.validTo, that.validTo))
      result = ("validTo", this.validTo, that.validTo) :: result

    if (valueNonEquals(this.totalShare, that.totalShare))
      result = ("totalShare", this.totalShare, that.totalShare) :: result

    if (valueNonEquals(this.freeFloat, that.freeFloat))
      result = ("freeFloat", this.freeFloat, that.freeFloat) :: result

    result
  }

  private def valueNonEquals(a: Long, b: Long) = !valueEquals(a, b)
  private def valueEquals(a: Long, b: Long) = math.abs(a - b) == 0
}

object SecInfos extends Table[SecInfo] {
  /**
   * Belongs to one Sec
   */
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val validFrom = "validFrom" BIGINT()
  val validTo = "validTo" BIGINT()
  val uniSymbol = "uniSymbol" VARCHAR(10) DEFAULT("''")
  val name = "name" VARCHAR(40) DEFAULT("''")
  val totalShare = "totalShare" BIGINT()
  val freeFloat = "freeFloat" BIGINT()
  val tradingUnit = "tradingUnit" INTEGER()
  val upperLimit = "upperLimit" DOUBLE()
  val lowerLimit = "lowerLimit" DOUBLE()
}
