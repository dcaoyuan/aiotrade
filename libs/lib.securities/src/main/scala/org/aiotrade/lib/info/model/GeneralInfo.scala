package org.aiotrade.lib.info.model

import ru.circumflex.orm.Table
import ru.circumflex.orm._
import org.aiotrade.lib.securities.model.Flag
import org.aiotrade.lib.math.timeseries.TVal
import scala.collection.mutable
import scala.collection.JavaConversions._
import org.aiotrade.lib.securities.dataserver.RichInfo

object GeneralInfos extends Table[GeneralInfo]{

  val publishTime = "publishTime" BIGINT
  val title = "title" VARCHAR(80) DEFAULT("''")
  val url = "url" VARCHAR(100)  DEFAULT("''")
  val infoClass = "infoClass" TINYINT
  val combinValue = "combineValue" BIGINT //for excluding repeated news

  def infoCategories = inverse(InfoContentCategories.generalInfo)
  def infoSecs = inverse(InfoSecs.generalInfo)
  def infoContents = inverse(Contents.generalInfo)
  def infoAbstracts = inverse(ContentAbstracts.generalInfo)
  
  private val urlToInfo = mutable.Map[String, GeneralInfo]()
  private var isLoad = false

  def infoOf(url : String) : Option[GeneralInfo]= {
    synchronized {
      if(!isLoad){
        val allInfo = (SELECT (GeneralInfos.*) FROM GeneralInfos list)
        allInfo foreach {x => urlToInfo.put(x.url, x)}
      }
      urlToInfo.get(url)
    }
    
  }

  val timeIdx = getClass.getSimpleName + "_time_idx" INDEX(publishTime.name)
}

object GeneralInfo {
  val NEWS = 1
  val FILING = 2
  val ANALYSIS_REPORT = 3
  val SYS_NOTIFICATION = 4
  val RICH_INFO = 5

  def save(info : RichInfo) {
    GeneralInfos.save(info.generalInfo)
    val content = new Content()
    content.generalInfo = info.generalInfo
    if(info.content.length < 1000) content.content = info.content else content.content = info.content.substring(0,1000)
    Contents.save(content)
    val infosecs = info.secs map { sec =>
      val infosec = new InfoSec()
      infosec.generalInfo = info.generalInfo
      infosec.sec =sec
      infosec
    }
    InfoSecs.insertBatch(infosecs.toArray)

    val infocates = info.categories map { cate =>
      val infocate = new InfoContentCategory()
      infocate.generalInfo = info.generalInfo
      infocate.category = cate
      infocate
      
    }
    InfoContentCategories.insertBatch(infocates.toArray)

  }

}

import GeneralInfo._
class GeneralInfo extends TVal with Flag {
  
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

  var publishTime: Long = -1
  var title: String = ""
  var infoClass : Int = NEWS
  var url : String = ""
  var combinValue : Long = -1

  def summary = infoAbstracts.headOption match {
    case Some(x) => x.content
    case None => ""
  }

  def content = infoContents.headOption match {
    case Some(x) => x.content
    case None => ""
  }
  def categories = infoCategories map (cate => cate.category)
  def secs = infoSecs map (sec => sec.sec)

  def infoCategories : Seq[InfoContentCategory] = GeneralInfos.infoCategories(this)
  def infoSecs : Seq[InfoSec] = GeneralInfos.infoSecs(this)
  def infoContents : Seq[Content] = GeneralInfos.infoContents(this)
  def infoAbstracts : Seq[ContentAbstract] = GeneralInfos.infoAbstracts(this)

//  def weight: Float = 0F
//  def link: String = url
//
//  def exportToMap: Map[String, String] = {
//    val map = Map[String, String]()
//    map += ("TITLE" -> title)
//
//    if(summary != null) map += ("CONTENT" -> summary)
//    //if(categories(0) != null) map += ("CATEGORY" -> categories(0).name)
//    map += ("PUBLISH_TIME" -> publishTime.toString)
//    if(link != null) map += ("LINK" -> link)
//
//    map
//  }
//
//  def exportToJavaMap: java.util.Map[String, String] = {
//    exportToMap
//  }
}

