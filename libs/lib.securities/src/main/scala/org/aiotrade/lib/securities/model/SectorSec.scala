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

import org.aiotrade.lib.util.ValidTime
import ru.circumflex.orm.Table

/**
 * @author Caoyuan Deng
 */
final class SectorSec {
  @transient var sector: Sector = _
  @transient var sec: Sec = _

  var sectorKey: String = _
  var unisymbol: String = _
  var weight: Float = _
  var validFrom: Long = _
  var validTo: Long = _

  def toSecValidTime: ValidTime[Sec] = ValidTime(sec, validFrom, validTo)
  def toSectorValidTime: ValidTime[Sector] = ValidTime(sector, validFrom, validTo)
  
  override def toString = {
    "SectorSec(sector.key=" + sectorKey + ", " + (if (sec == null) "with null sec ?" else " with good sec") + ")"
  }
}

object SectorSecs extends Table[SectorSec] {
  val sector  = "sectors_id" BIGINT() REFERENCES(Sectors)
  val sec = "secs_id" BIGINT() REFERENCES(Secs)

  val weight = "weight" FLOAT()
  val validFrom = "validFrom" BIGINT()
  val validTo = "validTo" BIGINT()

  val sectorIdx = getClass.getSimpleName + "_sector_idx" INDEX(sector.name)
  val secIdx = getClass.getSimpleName + "_sec_idx" INDEX(sec.name)
  val validFromIdx = getClass.getSimpleName + "_validFrom_idx" INDEX(validFrom.name)
  val validToIdx = getClass.getSimpleName + "_validToFrom_idx" INDEX(validTo.name)
}

