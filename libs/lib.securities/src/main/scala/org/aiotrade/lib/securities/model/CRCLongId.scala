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

/**
 * Try to generate an unique long id from a given crckey string, we choose java.util.zip.CRC32
 * since it returns same value of function CRC32(String) in mysql.
 * 
 * @note 
 * 1. Check conflict by:
 *    select secs_id, uniSymbol, crc32(unisymbol) as crc, count(*) from sec_infos group by crc having count(*) > 1;
 *    select a.secs_id, a.uniSymbol, crc32(a.uniSymbol) from sec_infos as a inner join (
 *      select secs_id, uniSymbol, crc32(unisymbol) as crc, count(*) from sec_infos group by crc having count(*) > 1
 *    ) as b on a.uniSymbol = b.uniSymbol order by a.uniSymbol;
 * 2. Select data of uniSymbol:
 *    select * from quotes1d where secs_id = crc32(uniSymbol)
 * 3. How about when uniSymbol changed of a sec?
 *    we need to use its original uniSymbol, or create a new sec
 *    
 * @note When corresponding code/unisymbol etc changes, the crckey may keep unchanged to keep the same long id,
 * if you change the crckey, you should also change long id.
 *    
 * Used by model class   
 *    
 * @author Caoyuan Deng
 */ 
import scala.reflect.ClassTag

trait CRCLongId {
  /** 
   * 
   * @param key string that was used to generate crc32 long id when the id was 
   *   created and insert into database. For:
   *     Sec: the original uniSymbol
   *     Exchange: code
   *     Sector: category + "." + code // concat(category, '.', code) in mysql 
   *     @note the crckey should be upper case  
   */
  var crckey: String = ""

  private var _id: Long = Long.MinValue
  def id: Long = {
    if (_id != Long.MinValue) {
      _id
    } else {
      val c = new java.util.zip.CRC32
      c.update(crckey.toUpperCase.getBytes("UTF-8"))
      c.getValue
    }
  }
  def id_=(id: Long) {
    this._id = id
  }
  
  override def hashCode = {
    val iv = id.intValue
    val lv = id.longValue
    if (iv == id.longValue) iv else (lv ^ (lv >>> 32)).toInt
  }
}

object CRCLongId {
  val MaxId = 0xFFFFFFFF // CRC32 is 32bit integer
}

/**
 * 
 * Table with the corresponding id and crckey column.
 */
abstract class CRCLongPKTable[R <: CRCLongId: ClassTag] extends ru.circumflex.orm.Table[R] { 
  override val id = "id" BIGINT()
  val crckey = "crckey" VARCHAR(30)
}
