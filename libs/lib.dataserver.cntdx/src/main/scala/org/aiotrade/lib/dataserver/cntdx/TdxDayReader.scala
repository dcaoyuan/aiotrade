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

package org.aiotrade.lib.dataserver.cntdx

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.Calendar
import java.util.TimeZone
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities.model.Quote

/**
 * Dump table to txt using ',' to separat fields
 * mkdir tmp
 * chmod 777 tmp
 * cd tmp
 * mysqldump --opt --default-character-set=utf8 -ufaster -pfaster -T ./ --database faster --tables quotes1d
 *
 * chmod 666 quotes1d.txt
 * mysql> load data local infile 'quotes1d.txt' into table quotes1d (@dummy, vwap, amount, volume, flag, secs_id, time, low, open, high, close);
 * or
 * nohup mysql -e "load data local infile 'quotes1d.txt' into table quotes1d (@dummy, vwap, amount, volume, flag, secs_id, time, low, open, high, close)" faster &
 * 
 * @author Caoyuan Deng
 */
@throws(classOf[IOException])
class TdxDayReader private (input: Either[FileChannel, InputStream]) {
  import TdxDayReader._
  
  private val timeZone = TimeZone.getTimeZone("Asia/Shanghai")
  private val cal = Calendar.getInstance
  cal.setTimeZone(timeZone)

  var isClosed = false

  private var bBuf: ByteBuffer = _
  private var _recordCount: Int = _

  load

  /** 
   * Can be reloaded 
   */
  def load {
    input match {
      case Left(x) => load(x)
      case Right(x) => load(x)
    }
  }

  private def load(fileChannel: FileChannel) {
    if (isClosed) throw new IOException("This reader has closed")

    //bBuf = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size)
    val length = fileChannel.size.toInt
    _recordCount = length / 20
    bBuf = ByteBuffer.allocate(length)
    fileChannel.position(0)
    fileChannel.read(bBuf)
    bBuf.rewind    
  }

  private def load(is: InputStream) {
    if (isClosed) throw new IOException("This reader has closed")
    
    val out = new ByteArrayOutputStream
    val buf = new Array[Byte](4096)
    val bis = new BufferedInputStream(is)
    var len = -1
    while ({len = bis.read(buf); len != -1}) {
      out.write(buf, 0, len)
    }
    val bytes = out.toByteArray
    bBuf = ByteBuffer.wrap(bytes)    
    val length = bytes.length
    _recordCount = length / 20
  }

  /**
   * Returns the number of records in the DBF.
   */
  def recordCount = _recordCount

  def hasNext: Boolean = bBuf.position < bBuf.capacity

  /**
   * Reads the returns the next row in the DBF stream.
   * @returns The next row as an Object array. Types of the elements
   * these arrays follow the convention mentioned in the class description.
   */
  @throws(classOf[IOException])
  def nextRecord: Quote = {
    if (isClosed) {
      throw new IOException("Source is not open")
    }

    val quote = new Quote
    try {
      val date    = readLittleEndianInt(bBuf)
      val open    = readLittleEndianInt(bBuf) / 100.0
      val high    = readLittleEndianInt(bBuf) / 100.0
      val low     = readLittleEndianInt(bBuf) / 100.0
      val close   = readLittleEndianInt(bBuf) / 100.0
      val amount  = java.lang.Float.intBitsToFloat(readLittleEndianInt(bBuf))
      val volume  = readLittleEndianInt(bBuf)
      val reserve = readLittleEndianInt(bBuf)
      //println("date: " + date + ", open: " + open + ", high: " + high + ", low: " + low + ", close: " + close + ", amount: " + amount + ", volume: " + volume + ", resever: " + reserve)
      
      val dateStr = date.toString
      cal.set(Calendar.YEAR, dateStr.substring(0, 4).toInt)
      cal.set(Calendar.MONTH, dateStr.substring(4, 6).toInt - 1)
      cal.set(Calendar.DAY_OF_MONTH, dateStr.substring(6, 8).toInt)
      val time = TFreq.DAILY.round(cal.getTimeInMillis, cal)
      
      quote.time = time
      quote.open = open
      quote.high = high
      quote.low = low
      quote.close = close
      quote.amount = amount
      quote.volume = volume
      
      quote.fromMe_!
      quote.closed_!
    } catch {
      case ex: EOFException => return null
      case ex: IOException => throw new IOException(ex.getMessage)
    }

    quote
  }

  @throws(classOf[IOException])
  def readLittleEndianInt(in: ByteBuffer): Int = {
    var bigEndian = 0
    var shiftBy = 0
    while (shiftBy < 32) {
      bigEndian |= (in.get & 0xff) << shiftBy
      shiftBy += 8
    }

    bigEndian
  }

  def close {
    isClosed = true
    try {
      input match {
        case Left(x) => x.close
        case Right(x) => x.close
      }
    } catch {
      case ex: IOException => throw ex
    }
  }
}

object TdxDayReader {
  private val nBytesRecord = 32 // size of bytes per record

  val nBytesFields = Array(
    4,  // date, int
    4,  // open, int
    4,  // high, int
    4,  // low, int
    4,  // close, int
    4,  // amount float
    4,  // volumn, int
    4   // reserve, or prev_close, int
  )
  

  @throws(classOf[IOException])
  def apply(is: InputStream): TdxDayReader = is match {
    case x: FileInputStream =>
      val fileChannel = x.getChannel
      apply(fileChannel)
    case _ => new TdxDayReader(Right(is))
  }

  @throws(classOf[IOException])
  def apply(file: File): TdxDayReader = {
    val fileChannel = (new FileInputStream(file)).getChannel
    apply(fileChannel)
  }

  @throws(classOf[IOException])
  def apply(fileName: String): TdxDayReader = {
    val fileChannel = (new RandomAccessFile(fileName, "r")).getChannel
    apply(fileChannel)
  }
  
  def apply(fileChannel: FileChannel): TdxDayReader = {
    try {
      new TdxDayReader(Left(fileChannel))
    } catch {
      case ex: Throwable => tryCloseFileChannel(fileChannel); throw ex
    }
  }

  private def tryCloseFileChannel(fileChannel: FileChannel) {
    if (fileChannel != null) {
      try {
        fileChannel.close
      } catch {
        case ex: Throwable =>
      }
    }
  }

  def readQuotes(file: File): Array[Quote] = {
    val quotes = ArrayList[Quote]
    val reader = TdxDayReader(file)
    while (reader.hasNext) {
      val quote = reader.nextRecord
      quotes += quote
      if (debug) println(quote)
    }
    reader.close
    quotes.toArray
  }
    
  // --- smiple test
  private val debug = false
  def main(args: Array[String]) {
    try {
      test
      System.exit(0)
    } catch {
      case _: Throwable => System.exit(1)
    }
  }
  
  def test {
    for (fileName <- List("sh000001.day", "sh600000.day")) {
      val ex = fileName.substring(0, 2)
      val symbol = fileName.substring(2, 8) + (if (ex.equalsIgnoreCase("SH")) ".SS" else ".SZ")
    
      val file = new File(fileName)
      if (file.exists) {
        val quotes = readQuotes(file)
        println(symbol + " -- quotes:\n " + quotes.mkString("\n"))
      }
    }
  }
}