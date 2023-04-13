package org.aiotrade.lib.info.model

import java.util.logging.Logger
import scala.collection.mutable.ListBuffer

trait InfoContent{
  
  def publishTime: Long
  
  /**
   * link is the unique identifier of each piece of InfoContent
   */
  def link: String

  def export: Any
}

trait InfoHelper {
  
  def check(v: String): String = if(v == null) "" else v
}


final case class NewsContent(title: String, summary: String, publishTime: Long,
                             sourceName: String, link: String, publisher: String,
                             combineCount: Int, uniSymbol: String, subject: String) extends InfoContent with InfoHelper{
  def export = {
    List("TITLE" -> check(title),
         "SUMMARY" -> check(summary),
         "PUBLISH_TIME" -> publishTime,
         "SOURCE_NAME" -> check(sourceName),
         "LINK" -> check(link),
         "PUBLISHER" -> check(publisher),
         "COMBINE_COUNT" -> combineCount,
         "SECURITY_CODE" -> check(uniSymbol),
         "SUBJECT" -> check(subject))
  }
}

object NewsContent {
  protected val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(v: List[(String, Any)]): NewsContent = v match {
    case List(("TITLE", title: String),
              ("SUMMARY", summary: String),
              ("PUBLISH_TIME", publishTime: Long),
              ("SOURCE_NAME", sourceName: String),
              ("LINK", link: String),
              ("PUBLISHER", publisher: String),
              ("COMBINE_COUNT", combineCount: Int),
              ("SECURITY_CODE", uniSymbol: String),
              ("SUBJECT", subject: String)) => NewsContent(title, summary, publishTime, sourceName,
                                                           link, publisher, combineCount, uniSymbol, subject)

    case x => log.severe("Fail to import " + this.getClass.getName + " from " + v)
      NewsContent("", "", 0L, "", "", "", 0, "", "")
  }

  def importFrom(v: ListBuffer[List[(String, Any)]]): ListBuffer[NewsContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
    
}

final case class FilingContent(title: String,  publishTime: Long, link: String, 
                               publisher: String, uniSymbol: String,
                               fileType: String, fileSize: Int) extends InfoContent with InfoHelper{
  def export = {
    List("TITLE" -> check(title),
         "PUBLISH_TIME" -> publishTime,
         "LINK" -> check(link),
         "SOURCE_NAME" -> check(publisher),
         "SECURITY_NAME" -> check(uniSymbol),
         "FILE_TYPE" -> check(fileType),
         "FILE_SIZE" -> fileSize)
  }
}

object FilingContent{
  protected val log = Logger.getLogger(this.getClass.getName)

  def importFrom(v: List[(String, Any)]): FilingContent = v match {
    case List(("TITLE", title: String),
              ("PUBLISH_TIME",  publishTime: Long),
              ("LINK", link: String),
              ("SOURCE_NAME", publisher: String),
              ("SECURITY_NAME", uniSymbol: String),
              ("FILE_TYPE", fileType: String),
              ("FILE_SIZE", fileSize: Int)) => FilingContent(title, publishTime, link, publisher,
                                                             uniSymbol, fileType, fileSize)
    case x => log.severe("Fail to import " + this.getClass.getName + " from " + v)
      FilingContent("", 0L, "", "", "", "", 0)
  }

  def importFrom(v: ListBuffer[List[(String, Any)]]): ListBuffer[FilingContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
}

final case class AnalysisReportContent(title: String, summary: String, publishTime: Long,
                                       link: String, publisher: String,var uniSymbol: String,
                                       sourceName: String, subject: String) extends InfoContent with InfoHelper{
  def export = {
    List("TITLE" -> check(title),
         "SUMMARY" -> check(summary),
         "PUBLISH_TIME" -> publishTime,
         "LINK" -> check(link),
         "PUBLISHER" -> check(publisher),
         "SECURITY_CODE" -> check(uniSymbol),
         "SOURCE_NAME" -> check(sourceName),
         "SUBJECT" -> check(subject))
  }
}

object AnalysisReportContent{
  protected val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(v: List[(String, Any)]): AnalysisReportContent = v match {
    case List(("TITLE", title: String),
              ("SUMMARY", summary: String),
              ("PUBLISH_TIME", publishTime: Long),
              ("LINK", link: String),
              ("PUBLISHER", publisher: String),
              ("SECURITY_CODE", uniSymbol: String),
              ("SOURCE_NAME", sourceName: String),
              ("SUBJECT", subject: String)) => AnalysisReportContent(title, summary, publishTime, link,
                                                                     publisher, uniSymbol, sourceName, subject)

    case x => log.severe("Fail to import " + this.getClass.getName + " from " + v)
      AnalysisReportContent("", "", 0L, "", "", "", "", "")
  }

  def importFrom(v: ListBuffer[List[(String, Any)]]): ListBuffer[AnalysisReportContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
}

final case class NotificationContent(title: String, content: String, 
                                     publishTime: Long, link: String) extends InfoContent with InfoHelper{
  def export = {
    List("TITLE" -> check(title),
         "CONTENT" -> check(content),
         "PUBLISH_TIME" -> publishTime,
         "LINK" -> check(link))
  }
}

object NotificationContent {
  protected val log = Logger.getLogger(this.getClass.getName)
  
  def importFrom(v: List[(String, Any)]): NotificationContent = v match {
    case List(("TITLE", title: String),
              ("CONTENT", content: String),
              ("CATEGORY", category: String),
              ("PUBLISH_TIME", publishTime: Long),
              ("LINK", link: String)) => NotificationContent(title, content, publishTime, link)
    case x => log.severe("Fail to import " + this.getClass.getName + " from " + v)
      NotificationContent("", "", 0L, "")
  }

  def importFrom(v: ListBuffer[List[(String, Any)]]): ListBuffer[NotificationContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
}

final case class SecPoolContent(startFrom: Long,  tpe: String, uniSymbol: String, symbolName: String) extends InfoContent with InfoHelper {
  def link = System.currentTimeMillis.toString
  def publishTime = startFrom
  def export = (startFrom, check(tpe), check(uniSymbol), check(symbolName))
}

object SecPoolContent {
  protected val log = Logger.getLogger(this.getClass.getName)

  def importFrom(v: (Long, String, String, String)): SecPoolContent = v match {
    case (startFrom: Long, tpe: String, uniSymbol: String, symbolName: String) => SecPoolContent(startFrom, tpe, uniSymbol, symbolName)
    case x =>  log.severe("Fail to import " + this.getClass.getName + " from " + v)
      SecPoolContent(0L, "", "", "")
  }

  def importFrom(v: ListBuffer[(Long, String, String, String)]): ListBuffer[SecPoolContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
}

final case class BullVSBearContent(optimismRatio: Float, analysis: String, predictTime: Long) extends InfoContent with InfoHelper {
  def link = predictTime.toString
  def publishTime = predictTime
  def export = (optimismRatio, check(analysis), predictTime)
}

object BullVSBearContent{
  protected val log = Logger.getLogger(this.getClass.getName)

  def importFrom(v: (Float, String, Long)) = v match {
    case (optimismRatio: Float, analysis: String, predictTime: Long) => BullVSBearContent(optimismRatio, analysis, predictTime)
    case x => log.severe("Fail to import " + this.getClass.getName + " from " + v)
      BullVSBearContent(0.5F, "", 0L)
  }

  def importFrom(v: ListBuffer[(Float, String, Long)]): ListBuffer[BullVSBearContent] = {
    for(vv <- v) yield {
      importFrom(vv)
    }
  }
}