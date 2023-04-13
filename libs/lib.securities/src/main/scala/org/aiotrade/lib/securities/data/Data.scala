package org.aiotrade.lib.securities.data

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.logging.Logger
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.info.model.AnalysisReports
import org.aiotrade.lib.info.model.ContentCategories
import org.aiotrade.lib.info.model.GeneralInfos
import org.aiotrade.lib.info.model.InfoContentCategories
import org.aiotrade.lib.info.model.InfoSecs
import org.aiotrade.lib.info.model.ContentAbstracts
import org.aiotrade.lib.info.model.Contents
import org.aiotrade.lib.info.model.Filings
import org.aiotrade.lib.info.model.Newses
import org.aiotrade.lib.sector.model.PortfolioBreakouts
import org.aiotrade.lib.sector.model.Sectors
import org.aiotrade.lib.sector.model.BullVSBears
import org.aiotrade.lib.sector.model.Portfolios
import org.aiotrade.lib.securities.model.Companies
import org.aiotrade.lib.securities.model.Company
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Exchanges
import org.aiotrade.lib.securities.model.ExchangeCloseDates
import org.aiotrade.lib.securities.model.Executions
import org.aiotrade.lib.securities.model.MoneyFlows1d
import org.aiotrade.lib.securities.model.MoneyFlows1m
import org.aiotrade.lib.securities.model.Quotes1d
import org.aiotrade.lib.securities.model.Quotes1m
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.SecIssues
import org.aiotrade.lib.securities.model.Secs
import org.aiotrade.lib.securities.model.SecDividends
import org.aiotrade.lib.securities.model.SecInfo
import org.aiotrade.lib.securities.model.SecInfos
import org.aiotrade.lib.securities.model.SecStatuses
import org.aiotrade.lib.securities.model.Tickers
import org.aiotrade.lib.securities.model.TickersLast
import ru.circumflex.orm._
import scala.actors.Scheduler
import scala.collection.mutable

/**
 * Set /usr/local/etc/my.cnf:

[mysqld]
default-storage-engine = innodb
character-set-server=utf8
collation-server=utf8_general_ci
init-connect='SET NAMES utf8'
[mysql]
default-character-set=utf8

 * and check:
 *   mysql> SHOW VARIABLES LIKE 'character%';
 *   mysql> SHOW VARIABLES LIKE 'collation_%';
 *
 * and under mysql client console:
 *   mysql> SET NAMES utf8;
 *
 * and under mysql command line tools:
 *   --default-character-set=utf8
 *
 * and under mysql jdbc url:
 *   jdbc:mysql://localhost:3306/aiotrade?useUnicode=true
 *   
 *   
 * Dump all db
 * nohup time mysqldump --opt --default-character-set=utf8 -ufaster -pfaster faster | gzip > faster.dump.gz
 * 
 * Import dump db
 * mysql > create database faster
 * $ gzip -d faster.dump.gz
 * $ nohup time mysql -uuser -ppasswd faster < faster.dump &
 * 
 * Check msyql err log:
 * /var/db/mysql/xxxhostxx.err
 *
 * Restart mysql:
 * $ /usr/local/etc/rc.d/mysql-server restart
 *
 * Show progress
 * $ mysql -e 'show processlist'
 * 
 * 
 * ******************************
 * 
 * Dump table to txt using ',' to separat fields
 * mkdir tmp
 * chmod 777 tmp
 * cd tmp
 * mysqldump --opt --default-character-set=utf8 -ufaster -pfaster -T ./ faster secs sec_infos companies --fields-terminated-by=,
 *
 * mysql> load data infile '/Users/dcaoyuan/tmp/secs.txt' into table secs character set utf8 fields terminated by ',';
 * 
 * Dump tables except tickers/executions:
nohup time -o timer.txt mysqldump --opt --default-character-set=utf8 -uroot faster \
analysis_reports         \
companies                \
content_abstracts        \
content_categories       \
contents                 \
exchange_close_dates     \
exchanges                \
filings                  \
general_infos            \
info_content_categories  \
info_secs                \
money_flows1d            \
money_flows1m            \
newses                   \
quotes1d                 \
quotes1m                 \
sec_dividends            \
sec_infos                \
sec_issues               \
sec_statuses             \
secs                     \
sector_secs              \
sectors                  \
tickers_last             \
| gzip > faster20110622.dump.gz &

 * Dump tables structure of tickers/executions:
 
nohup mysqldump --no-data --opt --default-character-set=utf8 -uroot faster executions tickers > faster_excutions_tickers.sql &

 * Gzip:
 *  $ nohup time -o timer.txt cat filename | gzip > filename.gz &
 * Gunzip:
 *  $ nohup time -o timer.txt gzip -dc filename.gz > filename &
 * 

 *
 *
 * Restart mysqld on freebsd
 *   $ sudo /usr/local/etc/rc.d/mysql-server restart
 * Restart mysqld on macports
 *   $ sudo /opt/local/etc/LaunchDaemons/org.macports.mysql5/mysql5.wrapper restart  
 */

@deprecated("Use SyncUtil, this class is for reference only")
object Data {
  private val log = Logger.getLogger(this.getClass.getName)

  private lazy val classLoader = Thread.currentThread.getContextClassLoader
  private var prefixPath = "src/main/resources/"
  lazy val dataFileDir = prefixPath + "data"

  // holding strong reference of exchange
  var exchanges = Array[Exchange]()

  // holding temporary id for secs, companies, industries etc
  val idToExchange = mutable.Map[String, Exchange]()
  val idToSec = mutable.Map[String, Sec]()
  val idToSecInfo = mutable.Map[String, SecInfo]()
  val idToCompany = mutable.Map[String, Company]()

  val secRecords = new ArrayList[Sec]
  val secInfoRecords = new ArrayList[SecInfo]
  val companyRecords = new ArrayList[Company]()

  private lazy val N   = Exchange("N",  "NY", "", "America/New_York", Array(9, 30, 16, 00))  // New York
  private lazy val SS  = Exchange("SS", "SS", "", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shanghai
  private lazy val SZ  = Exchange("SZ", "SZ", "", "Asia/Shanghai", Array(9, 30, 11, 30, 13, 0, 15, 0)) // Shenzhen
  private lazy val L   = Exchange("L",  "L", "", "UTC", Array(8, 00, 15, 30)) // London
  private lazy val HK  = Exchange("HK", "HK", "", "Asia/Shanghai", Array(10, 0, 12, 30, 14,30,16,0)) // HongKong
  private lazy val OQ  = Exchange("OQ", "OQ", "", "America/New_York", Array(9, 30, 16, 00)) // NASDAQ

  def main(args: Array[String]) {
    if (args.length == 0) {
      println("You must give the config file name!")
      return
    } else {
      println("Will load config from " + args(0))
    }

    org.aiotrade.lib.util.config.Config(args(0))
    log.info("Current user workind dir: " + System.getProperty("user.dir"))
    log.info("Table of exchanges exists: " + Exchanges.exists)

    if (Exchanges.exists) {
      log.info("!!! Table 'exchanges' existed, cannot create data unless you drop table 'exchanges' first !!!")
      return
    }

    createData

    ((SELECT (Secs.*, SecInfos.*) FROM (Secs JOIN SecInfos)
      ) list) foreach {x =>
      log.info("secs: id=" + x._1 + ", uniSymbol=" + x._2.uniSymbol + ", name=" + x._2.name)
    }

    //companyRecords map (_.shortName) foreach println

    Scheduler.shutdown
    System.exit(0)
  }


  def createData {
    schema

    createExchanges
    
    readFromSecInfos(readerOf("sec_infos.txt"))
    readFromCompanies(readerOf("companies.txt"))
    
    Secs.updateBatch_!(secRecords.toArray, Secs.secInfo, Secs.company)
    COMMIT
  }

  /**
   * @Note All tables should be ddl.dropCreate together, since schema will be
   * droped before create tables each time.
   */
  def schema {
    val tables = List(
      // -- basic tables
      Companies, Secs, SecDividends, SecInfos, SecIssues, SecStatuses,
      Exchanges, ExchangeCloseDates,
      Quotes1d, Quotes1m, MoneyFlows1d, MoneyFlows1m,
      Tickers, TickersLast, Executions,

      // -- info tables
      ContentCategories, GeneralInfos, ContentAbstracts,
      Contents, Newses, Filings, AnalysisReports, InfoSecs, InfoContentCategories,

      // -- sector tables
      BullVSBears, Sectors, Portfolios, PortfolioBreakouts
    )

    val ddl = new DDLUnit(tables: _*)
    ddl.dropCreate.messages.foreach(msg => log.info(msg.body))
  }

  def createExchanges = {
    exchanges = Array(SS, SZ, N, L, HK, OQ)
    Exchanges.insertBatch_!(exchanges)
  
    exchanges foreach {x => assert(Exchanges.idOf(x).isDefined, x + " with none id")}
    exchanges foreach {x => log.info("Exchange: " + x + ", id=" + Exchanges.idOf(x).get)}
  }

  def readerOf(fileName: String) = {
    val is = classLoader.getResourceAsStream("data/" + fileName)
    new BufferedReader(new InputStreamReader(is, "UTF-8"))
  }

  def readFromSecInfos(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, secs_id, validFrom, validTo, uniSymbol, name, totalShare, freeFloat, tradingUnit, upperLimit, lowerLimit) =>
          val sec = new Sec
          val exchange = exchangeOf(uniSymbol)
          sec.exchange = exchange
          secRecords += sec
          idToSec.put(secs_id, sec)

          val secInfo = new SecInfo
          secInfo.validFrom = validFrom.toLong
          secInfo.validTo = validTo.toLong
          secInfo.uniSymbol = uniSymbol
          secInfo.name = name
          secInfo.totalShare = totalShare.toLong
          secInfo.freeFloat = freeFloat.toLong
          secInfo.tradingUnit = tradingUnit.toInt
          secInfoRecords += secInfo
          idToSecInfo.put(id, secInfo)
          secInfo.sec = sec

          sec.secInfo = secInfo
          
        case xs => log.warning("sec_infos data file error at line: " + xs.mkString(","))
      }
    }
    Secs.insertBatch_!(secRecords.toArray)
    SecInfos.insertBatch_!(secInfoRecords.toArray)
  }

  def readFromCompanies(reader: BufferedReader) {
    var line: String = null
    while ({line = reader.readLine; line != null}) {
      line.split(',') match {
        case Array(id, secs_id, validFrom, validTo, shortName, fullName, listDate) =>
          val company = new Company
          company.validFrom = validFrom.toLong
          company.validTo = validTo.toLong
          company.shortName = shortName
          company.fullName = fullName
          company.listDate = listDate.toLong
          idToSec.get(secs_id) match {
            case Some(sec) => 
              company.sec = sec
              sec.company = company
            case None =>
          }
          companyRecords += company
          idToCompany.put(id, company)

        case xs => log.warning("companies data file error at line: " + xs.mkString(","))
      }
    }
    Companies.insertBatch_!(companyRecords.toArray)
  }

  def exchangeOfIndex(uniSymbol: String) : Option[Exchange] = {
    uniSymbol match {
      case "^DJI" => Some(N)
      case "^HSI" => Some(HK)
      case _=> None     
    }
  }

  def exchangeOf(uniSymbol: String): Exchange = {
    uniSymbol.toUpperCase.split('.') match {
      case Array(symbol) => 
        exchangeOfIndex(symbol) match {
          case Some(exchg) => exchg
          case None => N  
        }
      case Array(symbol, "L" ) => L
      case Array(symbol, "SS") => SS
      case Array(symbol, "SZ") => SZ
      case Array(symbol, "HK") => HK
      case _ => SZ
    }
  }

  def createSimpleSecs = {
    val secInfosFile = new File(dataFileDir, "sec_infos.txt")

    for (symbol <- List("GOOG", "YHOO", "ORCL")) {
      Exchanges.createSimpleSec(symbol, symbol, false)
    }

    for (symbol <- List("BP.L", "VOD.L", "BT-A.L", "BARC.L", "BAY.L", "TSCO.L", "HSBA.L")) {
      Exchanges.createSimpleSec(symbol, symbol, false)
    }
  }

}
