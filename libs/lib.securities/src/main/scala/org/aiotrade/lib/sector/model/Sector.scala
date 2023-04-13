package org.aiotrade.lib.sector.model

import ru.circumflex.orm._
import scala.collection.mutable
import org.aiotrade.lib.securities.model.Flag
import java.util.logging.Logger
import org.aiotrade.lib.math.timeseries.TVal
import scala.collection.JavaConversions._

object Sectors extends Table[Sector] {
  private val log = Logger.getLogger(this.getClass.getName)
  val name = "name" VARCHAR(30)
  val code = "code" VARCHAR(30)
  var portfolio = "portfolios_id".BIGINT REFERENCES(Portfolios)

  private var isLoad : Boolean = false
  private val codetoSector = mutable.Map[String, Sector]()

  def sectorOf(code : String) : Option[Sector] = {
    synchronized {
      if(!isLoad){
        load
        isLoad = true
      }
      codetoSector.get(code)
    }

  }
  private def load {
    val sectors = (SELECT (Sectors.*) FROM Sectors list)
    for (sector <- sectors){
      codetoSector.put(sector.code, sector)
    }
  }
  def portfolios = inverse(Portfolios.sector)
}

class Sector extends TVal with Flag {
  private val log = Logger.getLogger(this.getClass.getName)
  
  private var _time: Long = _
  def time = _time
  def time_=(time: Long) {
    this._time = time
  }

  private var _flag: Int = 1
  def flag = _flag // 1 // dafault is closed
  def flag_=(flag: Int) {
    this._flag = flag
  }

  var name : String = ""
  var code : String = ""
  var portfolio : Portfolio = _

  def portfolios  = Sectors.portfolio(this)

  def publishTime: Long = this.time
  def weight: Float = 0F
  def link: String = ""

//  def exportToMap: Map[String, String] = Map("" -> "")
//
//  def exportToJavaMap: java.util.Map[String, String] = exportToMap

//  def exportToList: List[Map[String, String]] = {
//    var list = List[Map[String, String]]()
//    if(portfolio != null && portfolio.breakouts != null){
//      for(portfolio <- portfolio.breakouts){
//        val map = Map[String, String]("SECURITY_CODE" -> portfolio.sec.uniSymbol)
//        map += ("SECURITY_NAME" -> portfolio.sec.secInfo.name)
//        map += ("ENTER_TIME" -> this.time.toString)
//        //list += map
//        list = list:+map
//      }
//    }
//    list
//  }
//
//  def exportToJavaList: java.util.List[java.util.Map[String, String]] = {
//
//    val list = new java.util.ArrayList[java.util.Map[String, String]]
//
//    if(portfolio != null && portfolio.breakouts != null){
//      for(portfolio <- portfolio.breakouts){
//        val map = Map[String, String]("SECURITY_CODE" -> portfolio.sec.uniSymbol)
//        map += ("SECURITY_NAME" -> portfolio.sec.secInfo.name)
//        map += ("ENTER_TIME" -> this.time.toString)
//        list.add(map)
//      }
//    }
//    list
//  }
}
