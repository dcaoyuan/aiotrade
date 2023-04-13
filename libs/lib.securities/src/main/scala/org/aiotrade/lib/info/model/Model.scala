package org.aiotrade.lib.info.model

import ru.circumflex.orm._
import org.aiotrade.lib.util.config
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.util.config.Config

object Model {

  def main(args: Array[String]) {
    Config(args(0))
    testCategory
    //createSamples
    System.exit(0)
  }
  def createSamples = {
    schema
    testsave
    testSelect
    COMMIT
  }


  def testCategory {
    println (ContentCategories.cateOf("richinfo.filing"))
  }
  def testSelect {
    val infos = (SELECT (GeneralInfos.*) FROM GeneralInfos list)
    for(info <- infos) {
      for(category <- info.categories) {
        println("category:"+category.code + ":" + category.name)
      }
      for(abstract_ <- info.infoAbstracts) {
        println("abstract:" + abstract_.content)
      }
      for(content <- info.infoContents) {
        println("content:" + content.content)
      }
      for(sec <- info.secs) {
        println("sec:" + sec.secInfo.uniSymbol)
      }
    }
  }



  def schema {
    val tables = List(ContentCategories,GeneralInfos,ContentAbstracts,
                      Contents,Newses,Filings,AnalysisReports,InfoSecs,InfoContentCategories)

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => println(msg.body))

  }

  def testsave {
    val parentfiling = new ContentCategory()
    parentfiling.name = "公告"
    parentfiling.code = "filing"

    ContentCategories.save(parentfiling)


    ContentCategories.idOf(parentfiling) match {
      case Some(id) =>
        val period = new ContentCategory()
        period.parent = id
        period.name = "定期报告"
        period.code = "filing.period"

        ContentCategories.save(period)
      case None => Unit
    }

    val reportroot = new ContentCategory()
    reportroot.name = "研究报告"
    reportroot.code = "report"
    ContentCategories.save(reportroot)

    ContentCategories.idOf(reportroot) match {
      case Some(id) =>
        val industryAna = new ContentCategory()
        industryAna.name = "行业研究"
        industryAna.parent = id
        industryAna.code = "report.industry"
        ContentCategories.save(industryAna)
      case None => Unit
    }




    val info = new GeneralInfo()
    info.title = "研究报告测试"
    info.publishTime = 1;
    info.infoClass = GeneralInfo.ANALYSIS_REPORT
    info.combinValue = 1L

    GeneralInfos.save(info)

    val abstract_ = new ContentAbstract()
    abstract_.content = "this is abstract"
    abstract_.generalInfo = info
    ContentAbstracts.save(abstract_)
    val content_ = new Content()
    content_.content = "This is contenct"
    content_.generalInfo = info
    Contents.save(content_)

    Exchange.secOf("600000.SS") match {
      case Some(x) => val infosec1 = new InfoSec()
        infosec1.sec = x
        infosec1.generalInfo = info
        InfoSecs.save(infosec1)
      case None => Unit
    }

    Exchange.secOf("000001.SZ") match {
      case Some(x) => val infosec2 = new InfoSec()
        infosec2.sec = x
        infosec2.generalInfo = info
        InfoSecs.save(infosec2)
      case None => Unit
    }

    ContentCategories.cateOf("report.industry") match
    {
      case Some(x) =>     val infocate1 = new InfoContentCategory()
        infocate1.generalInfo = info
        infocate1.category = x
        InfoContentCategories.save(infocate1)
      case None => Unit
    }


    val filingdemo = new Filing()
    filingdemo.format = Filing.PDF
    filingdemo.generalInfo = info
    filingdemo.publisher = "万科"
    filingdemo.size = 30000
    Filings.save(filingdemo)

  }


}
