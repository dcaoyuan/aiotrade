package org.aiotrade.lib.securities.model

import junit.framework.TestCase
import scala.collection.mutable
import junit.framework.Assert._
import java.text.SimpleDateFormat
import java.util.TimeZone

class TestExchangeUnit extends TestCase {

  private lazy val N   = Exchange.N
  private lazy val SS  = Exchange.SS
  private lazy val SZ  = Exchange.SZ
  private lazy val L   = Exchange.L
  private lazy val HK  = Exchange.HK
  private lazy val OQ  = Exchange.OQ
  
  private val timeToExchangeStatus = mutable.Map[Long, ExchangeStatus]()

  private val dateFormat = new SimpleDateFormat("yyyyMMddHHmmss")
  protected val timeZone = TimeZone.getTimeZone("Asia/Shanghai")


  override def setUp() {
    import ExchangeStatus._
    timeToExchangeStatus.put(parseDateStr("20100915091000"), PreOpen(parseDateStr("20100915091000"),9*60+10))
    timeToExchangeStatus.put(parseDateStr("20100915091500"), OpeningCallAcution(parseDateStr("20100915091500"),9*60+15))
    timeToExchangeStatus.put(parseDateStr("20100915092000"), OpeningCallAcution(parseDateStr("20100915092000"),9*60+20))
    timeToExchangeStatus.put(parseDateStr("20100915092500"), OpeningCallAcution(parseDateStr("20100915092500"),9*60+25))
    timeToExchangeStatus.put(parseDateStr("20100915092700"), Break(parseDateStr("20100915092700"),9*60+27))
    timeToExchangeStatus.put(parseDateStr("20100915093000"), Open(parseDateStr("20100915093000"),9*60+30))
    timeToExchangeStatus.put(parseDateStr("20100915093500"), Opening(parseDateStr("20100915093500"),9*60+35))
    timeToExchangeStatus.put(parseDateStr("20100915113000"), Opening(parseDateStr("20100915113000"),11*60+30))
    timeToExchangeStatus.put(parseDateStr("20100915114500"), Unknown(parseDateStr("20100915114500"),11*60+45))
    timeToExchangeStatus.put(parseDateStr("20100915130000"), Opening(parseDateStr("20100915130000"),13*60+00))
    timeToExchangeStatus.put(parseDateStr("20100915134500"), Opening(parseDateStr("20100915134500"),13*60+45))
    timeToExchangeStatus.put(parseDateStr("20100915150000"), Close(parseDateStr("20100915150000"),15*60+00))
    timeToExchangeStatus.put(parseDateStr("20100915150100"), Close(parseDateStr("20100915150100"),15*60+01))
    timeToExchangeStatus.put(parseDateStr("20100915150200"), Close(parseDateStr("20100915150200"),15*60+02))
    timeToExchangeStatus.put(parseDateStr("20100915150300"), Closed(parseDateStr("20100915150300"),15*60+03))
    dateFormat.setTimeZone(timeZone)
  }

  def testExchangeStatus() {
    timeToExchangeStatus foreach {
      x => assertEquals(SS.statusOf(x._1),x._2)
    }
  }




  def testStringAppend(){

    assertEquals("Hello world!", "Hello world!")

  }

  private def parseDateStr(yyyyMMddHHmmss : String) : Long = {
    val time = try {
      dateFormat.parse(yyyyMMddHHmmss).getTime
    } catch {case ex: Exception => 0L}

    time

  }


}
