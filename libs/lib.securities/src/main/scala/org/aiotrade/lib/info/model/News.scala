package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import org.aiotrade.lib.collection.ArrayList
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import java.util.logging.Logger

object Newses extends Table[News]{

  val generalInfo =  "generalInfos_id".BIGINT REFERENCES(GeneralInfos)

  val author = "author"  VARCHAR(30) DEFAULT("''")
  val orgPublisher = "orgPublisher" VARCHAR(30) DEFAULT("''")
  val hotness = "hotness" FLOAT()
}

class News extends TVal with Flag {
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

  var generalInfo : GeneralInfo = _
  var author : String = ""
  var orgPublisher : String = ""
  var hotness : Float = 0F


  private var _newses: ArrayList[News] = ArrayList[News]()

  def newses = _newses

  def += [News](value: org.aiotrade.lib.info.model.News){
    assert(value.generalInfo.publishTime == this.time,
           value + " is appended to a different TVal with time=" + this.time)

    value +=: _newses
  }

  def ++= [News](values: ArrayList[org.aiotrade.lib.info.model.News]){
    values.foreach(v => assert(v.generalInfo.publishTime == this.time,
                               v + " is appended to a different TVal with time=" + this.time))

    values ++=: _newses
  }

//  def publishTime: Long = this.time
//  //def weight: Float = 0F
//  def link: String = if(gene?ralInfo != null ) generalInfo.url else ""
  
//  def exportToMap: Map[String, String] = {
//    val map = Map[String, String]()
//    map += ("PUBLISH_TIME" -> publishTime.toString)
//    if(author != null) map += ("PUBLISHER" -> author)
//    if(link != null) map += ("LINK" -> link)
//    if(orgPublisher != null) map += ("SOURCE_NAME" -> orgPublisher)
//    map += ("COMBINE_COUNT" -> hotness.toString)
//    //map += ("weight" -> weight.toString)
//    try{
//      if(generalInfo.title != null ) map += ("TITLE" -> generalInfo.title)
//      if(generalInfo.infoAbstracts != null) map += ("SUMMARY" -> generalInfo.infoAbstracts(0).content)
//      if(generalInfo.secs.size > 0){
//        if(generalInfo.secs(0) != null) map += ("SECURITY_CODE" -> generalInfo.secs(0).secInfo.uniSymbol)
//      }
//      if(generalInfo.categories.size > 0){
//        if(generalInfo.categories(0) != null) map += ("SUBJECT" -> generalInfo.categories(0).name)
//      }
//    }
//    catch{
//      case _ => log.severe("News export to Map exception")
//    }
//    map
//  }

//  def exportToJavaMap: java.util.Map[String, String] = {
//    exportToMap
//  }
  
  override def toString: String = {
    this.generalInfo.title + "|" + this.generalInfo.publishTime + "|" + this.author
  }
}
