/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
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
package org.aiotrade.modules.ui;

import java.io.IOException;
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement
import java.sql.SQLException;
import java.sql.Statement
import java.util.Properties;
import org.aiotrade.lib.charting.chart.QuoteChart;
import org.aiotrade.lib.charting.laf.LookFeel
import org.aiotrade.lib.view.securities.persistence.ContentPersistenceHandler
import org.aiotrade.lib.view.securities.persistence.ContentParseHandler
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.math.timeseries.datasource.DataContract
import org.aiotrade.lib.math.timeseries.descriptor.Content
import org.aiotrade.lib.securities.PersistenceManager
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Ticker
import org.aiotrade.lib.securities.util.UserOptionsManager
import org.aiotrade.lib.util.swing.action.RefreshAction;
import org.aiotrade.modules.ui.nodes.SymbolNodes
import org.openide.ErrorManager;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.xml.XMLUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import scala.collection.mutable.ArrayBuffer


/**
 * This class implements persistence via NetBeans Node system
 * Actually it will be an application context
 *
 * For h2 db connect:
 * user: dbuser
 * password: dbuserpwd
 * jdbc:h2:~/myprjs/aiotrade.sf/opensource/suite/application/target/userdir/db/aiotrade
 *
 * @author Caoyuan Deng
 */
object NetBeansPersistenceManager {
  private val SYMBOL_INDEX_TABLE_NAME = "AIOTRADESYMBOLINDEX"
}

import NetBeansPersistenceManager._
class NetBeansPersistenceManager extends PersistenceManager {
  private val classLoader = Thread.currentThread.getContextClassLoader

  /** @TODO
   * WindowManager.getDefault()
   */
  private val WIN_MAN = new NetBeansWindowManager
  /**
   * we perfer content instances long lives in application context, so, don't
   * use weak reference map here.
   */
  val defaultContent: Content = restoreContent("Default")
  private var propsLoaded = false
  private var props: Properties = _
  private var dbDriver: String = _
  private var dbUser: String = _
  private var dbPassword: String = _
  private var dbProps: Properties = _
  private var dbUrl: String = _
  private var connPoolMgr: MiniConnectionPoolManager = _

  private var dbType: Int = _
  private val DB_DERBY = 0
  private val DB_H2 = 1

  private val inSavingProps = new Object

  restoreProperties
  //checkAndCreateDatabaseIfNecessary
  
  /**
   * 'symbols' folder in default file system, usually the 'config' dir in userdir.
   * Physical folder "symbols" is defined in layer.xml
   */
  private def symbolsFolder = FileUtil.getConfigFile("symbols")
  
  private def defualtContentFile = FileUtil.getConfigFile("UserOptions/DefaultContent.xml")

  def saveContent(content: Content) {
    if (content.uniSymbol.equalsIgnoreCase("Default")) {
      val default = defualtContentFile
      if (default != null) {
        writeContent(default, content)
      }
    } else {
      SymbolNodes.findSymbolNode(content.uniSymbol) foreach {node =>
        /** refresh node's icon in explorer window */
        val children = node.getChildren
        for (child <- children.getNodes) {
          child.getLookup.lookup(classOf[RefreshAction]).execute
        }
        
        val dob = node.getLookup.lookup(classOf[DataObject])
        val writeTo = if (dob != null) {
          dob.getPrimaryFile
        } else {
          // @todo, create a new one?
          defualtContentFile // default one
        } 
        
        if (writeTo != null) {
          writeContent(writeTo, content)
        }
      }
    }
  }
  
  private def writeContent(writeTo: FileObject, content: Content) {
    var out: PrintStream = null
    var lock: FileLock = null
    try {
      lock = writeTo.lock

      out = new PrintStream(writeTo.getOutputStream(lock))
      out.print(ContentPersistenceHandler.dumpContent(content))
    } catch {
      case ex: IOException => ErrorManager.getDefault.notify(ex)
    } finally {
      /** should remember to do out.close() here */
      if (out  != null) out.close
      if (lock != null) lock.releaseLock
    }
  }

  /** Deserialize a Symbol from xml file */
  def restoreContent(uniSymbol: String): Content = {
    val contentOpt = if (uniSymbol.equalsIgnoreCase("Default")) {
      val defaultContentFile = FileUtil.getConfigFile("UserOptions/DefaultContent.xml")
      if (defaultContentFile != null) {
        readContent(defaultContentFile)
      } else None
    } else {
      Option(symbolsFolder.getFileObject(uniSymbol, "sec")) match {
        case Some(fo) => readContent(fo)
        case None =>       
          val content = defaultContent.clone
          content.uniSymbol = uniSymbol
          content.lookupDescriptors(classOf[DataContract[_]]) foreach {_.srcSymbol = uniSymbol}
          Some(content)
      }
    }

    contentOpt getOrElse (null)
  }
  
  private def readContent(fo: FileObject): Option[Content] = {
    var is: InputStream = null
    try {
      is = fo.getInputStream
      val xmlReader = XMLUtil.createXMLReader
      val handler = new ContentParseHandler
      xmlReader.setContentHandler(handler)
      xmlReader.parse(new InputSource(is))

      Option(handler.getContent)
    } catch {
      case ex: IOException  => ErrorManager.getDefault.notify(ex); None
      case ex: SAXException => ErrorManager.getDefault.notify(ex); None
    } finally {
      if (is != null) is.close
    }
  }

  def saveProperties {
    inSavingProps synchronized {
      val propsFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties");
      if (propsFile != null) {
        var out: OutputStream = null
        var lock: FileLock = null
        try {
          lock = propsFile.lock

          val props = new Properties

          val laf = LookFeel

          val lafStr = laf.getClass.getName
          val colorReversedStr = LookFeel().isPositiveNegativeColorReversed.toString
          val thinVolumeStr = LookFeel().isThinVolumeBar.toString;
          val quoteChartTypeStr = LookFeel().getQuoteChartType.toString
          val antiAliasStr = LookFeel().isAntiAlias.toString
          val autoHideScrollStr = LookFeel().isAutoHideScroll.toString

          var proxyTypeStr = ""
          var proxyHostStr = ""
          var proxyPortStr = ""
          var proxy = UserOptionsManager.getProxy
          if (proxy == null) {
            proxyTypeStr = "SYSTEM"
          } else {
            val proxyType = proxy.`type`
            proxyType match {
              case Proxy.Type.DIRECT =>
                proxyTypeStr = proxyType.toString
                proxyHostStr = ""
                proxyPortStr = ""
              case Proxy.Type.HTTP =>
                proxyTypeStr = proxyType.toString
                val addr = proxy.address.asInstanceOf[InetSocketAddress]
                proxyHostStr = addr.getHostName
                proxyPortStr = addr.getPort.toString
              case _ =>
                proxyTypeStr = "SYSTEM"
            }
          }

          var strDbDriver = ""
          var strDbUrl = ""
          var strDbUser = ""
          var strDbPassword = ""
          if (props != null) {
            strDbDriver = props.getProperty("org.aiotrade.platform.jdbc.driver")
            strDbUrl = props.getProperty("org.aiotrade.platform.jdbc.url")
            strDbUser = props.getProperty("org.aiotrade.platform.jdbc.user")
            strDbPassword = props.getProperty("org.aiotrade.platform.jdbc.password")
          }

          props.setProperty("org.aiotrade.platform.option.lookfeel", lafStr)
          props.setProperty("org.aiotrade.platform.option.colorreversed", colorReversedStr)
          props.setProperty("org.aiotrade.platform.option.thinvolume", thinVolumeStr)
          props.setProperty("org.aiotrade.platform.option.quotecharttype", quoteChartTypeStr)
          props.setProperty("org.aiotrade.platform.option.antialias", antiAliasStr)
          props.setProperty("org.aiotrade.platform.option.autohidescroll", autoHideScrollStr)

          props.setProperty("org.aiotrade.platform.option.proxytype", proxyTypeStr)
          props.setProperty("org.aiotrade.platform.option.proxyhost", proxyHostStr)
          props.setProperty("org.aiotrade.platform.option.proxyport", proxyPortStr)

          props.setProperty("org.aiotrade.platform.jdbc.driver", strDbDriver)
          props.setProperty("org.aiotrade.platform.jdbc.url", strDbUrl)
          props.setProperty("org.aiotrade.platform.jdbc.user", strDbUser)
          props.setProperty("org.aiotrade.platform.jdbc.password", strDbPassword)

          /** save to file */
          out = propsFile.getOutputStream(lock)
          props.store(out, null)
        } catch {case ex: IOException => ErrorManager.getDefault().notify(ex)} finally {
          if (out  != null) out.close
          if (lock != null) lock.releaseLock
        }

      }
    }

  }

  def restoreProperties {
    val propsFile = FileUtil.getConfigFile("UserOptions/aiotrade.properties")
    if (propsFile != null) {
      try {
        props = new Properties
        val is = propsFile.getInputStream
        props.load(is)

        is.close
      } catch {case ex: IOException => ErrorManager.getDefault().notify(ex)}

      if (props != null) {
        val lafStr = props.getProperty("org.aiotrade.platform.option.lookfeel")
        val colorReversedStr = props.getProperty("org.aiotrade.platform.option.colorreversed")
        val thinVolumeStr = props.getProperty("org.aiotrade.platform.option.thinvolume")
        var quoteChartTypeStr = props.getProperty("org.aiotrade.platform.option.quotecharttype")
        val antiAliasStr = props.getProperty("org.aiotrade.platform.option.antialias")
        val autoHideScrollStr = props.getProperty("org.aiotrade.platform.option.autohidescroll")
        val proxyTypeStr = props.getProperty("org.aiotrade.platform.option.proxytype")
        var proxyHostStr = props.getProperty("org.aiotrade.platform.option.proxyhost")
        val proxyPortStr = props.getProperty("org.aiotrade.platform.option.proxyport", "80")

        if (lafStr != null) {
          try {
            val laf = Class.forName(lafStr.trim, true, classLoader).newInstance.asInstanceOf[LookFeel]
            LookFeel() = laf
          } catch {case ex: Exception => ErrorManager.getDefault.notify(ex)}

        }

        if (colorReversedStr != null) {
          LookFeel().setPositiveNegativeColorReversed(colorReversedStr.trim.toBoolean)
        }

        if (thinVolumeStr != null) {
          LookFeel().setThinVolumeBar(thinVolumeStr.trim.toBoolean)
        }

        if (quoteChartTypeStr != null) {
          quoteChartTypeStr = quoteChartTypeStr.trim
          if (quoteChartTypeStr.equalsIgnoreCase("bar")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Ohlc)
          } else if (quoteChartTypeStr.equalsIgnoreCase("candle")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Candle)
          } else if (quoteChartTypeStr.equalsIgnoreCase("line")) {
            LookFeel().setQuoteChartType(QuoteChart.Type.Line)
          }
        }

        if (antiAliasStr != null) {
          LookFeel().setAntiAlias(antiAliasStr.trim.toBoolean)
        }

        /** there may be too many hidden exceptions in the following code, try {} it */
        try {
          proxyHostStr = if (proxyHostStr == null) "" else proxyHostStr
          val port = proxyPortStr.trim.toInt
          val proxyAddr = new InetSocketAddress(proxyHostStr, port)

          val proxy: Proxy = if (proxyTypeStr != null) {
            if (proxyTypeStr.equalsIgnoreCase("SYSTEM")) {
              null
            } else {
              var tpe = Proxy.Type.valueOf(proxyTypeStr)
              tpe match {
                case Proxy.Type.DIRECT => Proxy.NO_PROXY
                case Proxy.Type.HTTP => new Proxy(Proxy.Type.HTTP, proxyAddr)
                case _ => null
              }
            }
          } else null

          UserOptionsManager.setProxy(proxy)
        } catch {case ex: Exception =>}

        UserOptionsManager.setOptionsLoaded(true)
      }
    }
  }

  def properties = {
    if (!propsLoaded) {
      restoreProperties
    }
    props
  }

  def lookupAllRegisteredServices[T](clz: Class[T], folderName: String): Seq[T] = {
    val lookup = Lookups.forPath(folderName)
    val tp = new Lookup.Template(clz)
    val instances = lookup.lookup(tp).allInstances.iterator
    var sinstances = List[T]()
    while (instances.hasNext) sinstances ::= instances.next
    sinstances
  }

  private def checkAndCreateDatabaseIfNecessary {
    dbDriver = props.getProperty("org.aiotrade.platform.jdbc.driver")

    try {
      Class.forName(dbDriver, true, classLoader)
    } catch {case ex: ClassNotFoundException => ex.printStackTrace}

    val strUserDir = System.getProperty("netbeans.user")
    dbUrl = props.getProperty("org.aiotrade.platform.jdbc.url") + strUserDir + "/db/" + "aiotrade"

    dbUser = props.getProperty("org.aiotrade.platform.jdbc.user")
    dbPassword = props.getProperty("org.aiotrade.platform.jdbc.password")

    dbProps = new Properties
    dbProps.put("user", dbUser)
    dbProps.put("password", dbPassword)

    /** test if database exists, if not, create it: */
    /** derby special properties */
    dbDriver match {
      case "org.apache.derby.jdbc.EmbeddedDriver" =>
        dbType = DB_DERBY
        dbProps.put("create", "true")
      case "org.h2.Driver" =>
        dbType = DB_H2
      case _ => ()
    }

    try {
      /** check and create symbol index table if necessary */
      if (!symbolIndexTableExists) {
        createSymbolIndexTable
      }

      if (!tickerTableExists) {
        createRealTimeTickerTable
      }

    } catch {case ex: SQLException => ex.printStackTrace}

    /** derby special properties */
    dbProps.remove("create")
  }

  private def getDbConnection: Connection = {
    val conn = try {
      dbType match {
        case DB_DERBY =>
          if (connPoolMgr == null) {
            //val dataSource = new org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource();
            //dataSource.setURL(dbUrl)
            //dataSource.setUser(dbUser)
            //dataSource.setPassword(dbPassword)
            //dataSource.setCreateDatabase("create")
            //connPoolManager = new MiniConnectionPoolManager(dataSource, 5)
          }
          DriverManager.getConnection(dbUrl, dbProps)
        case DB_H2 =>
          if (connPoolMgr == null) {
//            val dataSource = new org.h2.jdbcx.JdbcDataSource
//            dataSource.setURL(dbUrl)
//            dataSource.setUser(dbUser)
//            dataSource.setPassword(dbPassword)
//            connPoolMgr = new MiniConnectionPoolManager(dataSource, 5)
          }
          connPoolMgr.getConnection
        case _ => DriverManager.getConnection(dbUrl, dbProps)
      }
    } catch {case ex: SQLException => ex.printStackTrace; null}

    /**
     * Try to set Transaction Isolation to TRANSACTION_READ_UNCOMMITTED
     * level to get better perfomance.
     */
    try {
      conn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED)
    } catch {case ex: SQLException =>
        /**
         * As not all databases support TRANSACTION_READ_UNCOMMITTED level,
         * we should catch exception and ignore it here to avoid break the
         * followed actions.
         */
    }

    try {
      conn.setAutoCommit(false)
    } catch {case ex: SQLException => ex.printStackTrace}

    conn
  }

  /**
   * @param tb table name
   * @param connention
   */
  private def tableExists(tb: String): Boolean = {
    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = getDbConnection
      val existsTestSql = "SELECT * FROM " + tb + " WHERE 1 = 0"
      stmt = conn.prepareStatement(existsTestSql)
      val rs = stmt.executeQuery

      if (rs != null) rs.close
      return true
    } catch {
      case ex: SQLException => // may be caused by none exist, so don't report
    } finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }

    false
  }

  /**
   * @param conn a db connection
   * @return true if exists, false if none
   */
  private def symbolIndexTableExists: Boolean = {
    tableExists(SYMBOL_INDEX_TABLE_NAME)
  }

  private def createSymbolIndexTable {
    var conn: Connection = null
    var stmt: Statement = null
    try {
      conn = getDbConnection
      stmt = conn.createStatement

      val creatTableSql_derby = "CREATE TABLE " + SYMBOL_INDEX_TABLE_NAME + " (" +
      "qid        INTEGER  NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
      "qsymbol    CHAR(30) NOT NULL, " +
      "qtablename CHAR(60), " +
      "qfreq      CHAR(10))"

      val creatTableSql_h2_hsqldb = "CREATE CACHED TABLE " + SYMBOL_INDEX_TABLE_NAME + "(" +
      "qid        INTEGER  NOT NULL IDENTITY(1, 1) PRIMARY KEY, " +
      "qsymbol    CHAR(30) NOT NULL, " +
      "qtablename CHAR(60), " +
      "qfreq      CHAR(10))"

      var sql = if (dbDriver.contains("derby")) creatTableSql_derby else creatTableSql_h2_hsqldb
      stmt.executeUpdate(sql)

      // @Note: index name in db is glode name, so, use idx_tableName_xxx to identify them
      sql = "CREATE INDEX idx_qsymbol_" + SYMBOL_INDEX_TABLE_NAME + " ON " + SYMBOL_INDEX_TABLE_NAME + " (qsymbol)"
      stmt.executeUpdate(sql)

      sql = "CREATE INDEX idx_qfreq_" + SYMBOL_INDEX_TABLE_NAME + " ON " + SYMBOL_INDEX_TABLE_NAME + " (qfreq)"
      stmt.executeUpdate(sql)

      conn.commit
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }
  }

  /**
   * @param symbol
   * @param freq
   * @param connection
   */
  private def quoteTableExists(symbol: String, freq: TFreq): Boolean = {
    val tb = propTableName(symbol, freq)
    tableExists(tb)
  }

  /**
   * @return connection for following usage
   */
  private def createQuoteTableOf(symbol: String, freq: TFreq) {
    var conn: Connection = null
    var stmt: Statement = null
    try {
      conn = getDbConnection
      stmt = conn.createStatement

      /**
       * Only one identity column is allowed in each table. Identity
       * columns are autoincrement columns. They must be of INTEGER or
       * BIGINT type and are automatically primary key columns (as a
       * result, multi-column primary keys are not possible with an
       * IDENTITY column present)
       */
      val tb = propTableName(symbol, freq)
      val creatTableSql_derby = "CREATE TABLE " + tb + "(" +
      "qid        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
      "qtime      BIGINT  NOT NULL, " +
      "qopen      DOUBLE, " +
      "qhigh      DOUBLE, " +
      "qlow       DOUBLE, " +
      "qclose     DOUBLE, " +
      "qclose_adj DOUBLE, " +
      "qvolume    DOUBLE, " +
      "qamount    DOUBLE, " +
      "qvwap      DOUBLE, " +
      "qhasgaps   SMALLINT, " +
      "qsourceid  BIGINT)"

      val creatTableSql_h2_hsqldb = "CREATE CACHED TABLE " + tb + "(" +
      "qid        INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, " +
      "qtime      BIGINT  NOT NULL, " +
      "qopen      DOUBLE, " +
      "qhigh      DOUBLE, " +
      "qlow       DOUBLE, " +
      "qclose     DOUBLE, " +
      "qclose_adj DOUBLE, " +
      "qvolume    DOUBLE, " +
      "qamount    DOUBLE, " +
      "qvwap      DOUBLE, " +
      "qhasgaps   SMALLINT, " +
      "qsourceid  BIGINT)"

      var sql = if (dbDriver.contains("derby")) creatTableSql_derby else creatTableSql_h2_hsqldb
      stmt.executeUpdate(sql)

      /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
      sql = "CREATE INDEX idx_qtime_" + tb + " ON " + tb + " (qtime)"
      stmt.executeUpdate(sql)

      sql = "CREATE INDEX idx_qsourceid_" + tb + " ON " + tb + " (qsourceid)"
      stmt.executeUpdate(sql)

      /** insert a symbol index record into symbol index table */
      sql = "INSERT INTO " + SYMBOL_INDEX_TABLE_NAME + " (qsymbol, qtablename, qfreq) VALUES ('" + propSymbol(symbol) + "', '" + tb + "', '" + freq.toString + "')"
      stmt.executeUpdate(sql)

      conn.commit
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }
  }

  def saveQuotes(symbol: String, freq: TFreq, quotes: Array[Quote], sourceId: Long) {
    if (!quoteTableExists(symbol, freq)) {
      createQuoteTableOf(symbol, freq)
    }

    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = getDbConnection
      val tb = propTableName(symbol, freq)
      val sql =  "INSERT INTO " + tb  +
      " (qtime, qopen, qhigh, qlow, qclose, qvolume, qamount, qclose_adj, qvwap, qhasgaps, qsourceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

      stmt = conn.prepareStatement(sql)
      for (quote <- quotes) {
        stmt setLong  (1, quote.time)
        stmt setDouble (2, quote.open)
        stmt setDouble (3, quote.high)
        stmt setDouble (4, quote.low)
        stmt setDouble (5, quote.close)
        stmt setDouble (6, quote.volume)
        stmt setDouble (7, quote.amount)
        stmt setDouble (9, quote.vwap)
        stmt setByte  (10, if (quote.hasGaps) -1 else 1)
        stmt setLong  (11, sourceId)

        stmt.addBatch
      }
      stmt.executeBatch

      conn.commit
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }
  }

  def restoreQuotes(symbol: String, freq: TFreq): Array[Quote] = {
    val quotes = new ArrayBuffer[Quote]

    if (quoteTableExists(symbol, freq)) {
      var conn: Connection = null
      var stmt: PreparedStatement = null
      try {
        conn = getDbConnection
        val tb = propTableName(symbol, freq)
        val sql = "SELECT * FROM " + tb + " ORDER BY qtime ASC"

        stmt = conn.prepareStatement(sql)
        val rs = stmt.executeQuery
        while (rs.next) {
          val quote = new Quote

          quote.time      = rs.getLong("qtime")
          quote.open      = rs.getDouble("qopen")
          quote.high      = rs.getDouble("qhigh")
          quote.low       = rs.getDouble("qlow")
          quote.close     = rs.getDouble("qclose")
          quote.volume    = rs.getDouble("qvolume")
          quote.amount    = rs.getDouble("qamount")
          quote.vwap      = rs.getDouble("qvwap")
          quote.hasGaps   = (if (rs.getByte("qhasgaps") < 0) true else false)
          quote.sourceId  = rs.getLong("qsourceid")

          quotes += quote
        }
        rs.close

        conn.commit
        WIN_MAN.statusText = quotes.size + " quotes restored from database."
      } catch {case ex: SQLException => ex.printStackTrace} finally {
        if (conn != null) conn.close
        if (stmt != null) stmt.close
      }
    }

    quotes.toArray
  }

  def deleteQuotes(symbol: String, freq: TFreq, fromTime: Long, toTime: Long) {
    if (quoteTableExists(symbol, freq)) {
      var conn: Connection = null
      var stmt: PreparedStatement = null
      try {
        conn = getDbConnection
        val tb = propTableName(symbol, freq)
        val sql = "DELETE FROM " + tb + " WHERE qtime BETWEEN ? AND ? "

        stmt = conn.prepareStatement(sql)

        stmt.setLong(1, fromTime)
        stmt.setLong(2, toTime)

        stmt.execute

        conn.commit
        WIN_MAN.statusText = "Delete data of " + tb + " successfully."
      } catch {case ex: SQLException => ex.printStackTrace} finally {
        if (conn != null) conn.close
        if (stmt != null) stmt.close
      }
    }
  }

  def dropAllQuoteTables(symbol: String) {
    if (symbolIndexTableExists) {
      var conn: Connection = null
      var stmt: Statement = null
      try {
        conn = getDbConnection
        var tableNames = List[String]()
        var sql = "SELECT * FROM " + SYMBOL_INDEX_TABLE_NAME + " WHERE qsymbol = '" + propSymbol(symbol) + "'"

        stmt = conn.createStatement
        val rs = stmt.executeQuery(sql)
        while (rs.next) {
          val tableName = rs.getString("qtablename")
          tableNames ::= tableName
        }
        rs.close

        for (tableName <- tableNames) {
          sql = "DROP TABLE " + tableName
          stmt.executeUpdate(sql)
        }

        sql = "DELETE FROM " + SYMBOL_INDEX_TABLE_NAME + " WHERE qsymbol = '" + propSymbol(symbol) + "'"
        stmt.executeUpdate(sql)

        conn.commit
        WIN_MAN.statusText = "Clear data of " + symbol + " successfully."
      } catch {case ex: SQLException => ex.printStackTrace} finally {
        if (conn != null) conn.close
        if (stmt != null) stmt.close
      }
    }
  }


  // ----- realtime ticker tables

  private def tickerTableExists: Boolean = {
    val tb = "realtime_ticker"
    tableExists(tb)
  }

  /**
   * @return connection for following usage
   */
  private def createRealTimeTickerTable {
    var conn: Connection = null
    var stmt: Statement = null
    try {
      conn = getDbConnection
      stmt = conn.createStatement

      /**
       * Only one identity column is allowed in each table. Identity
       * columns are autoincrement columns. They must be of INTEGER or
       * BIGINT type and are automatically primary key columns (as a
       * result, multi-column primary keys are not possible with an
       * IDENTITY column present)
       */
      val tickerTb = "realtime_ticker"
      val createTableSql_derby = "CREATE TABLE " + tickerTb + "(" +
      "tid       INTEGER  NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
      "ttime     BIGINT   NOT NULL, " +
      "tsymbol   CHAR(30) NOT NULL, " +
      "prevclose DOUBLE, " +
      "lastprice DOUBLE, " +
      "dayopen   DOUBLE, " +
      "dayhigh   DOUBLE, " +
      "daylow    DOUBLE, " +
      "dayvolume DOUBLE, " +
      "dayamount DOUBLE, " +
      "daychange DOUBLE, " +
      "tsourceid BIGINT)"

      val createTableSql_h2_hsqldb = "CREATE CACHED TABLE " + tickerTb + "(" +
      "tid       INTEGER  NOT NULL IDENTITY(1, 1) PRIMARY KEY, " +
      "ttime     BIGINT   NOT NULL, " +
      "tsymbol   CHAR(30) NOT NULL, " +
      "prevclose DOUBLE, " +
      "lastprice DOUBLE, " +
      "dayopen   DOUBLE, " +
      "dayhigh   DOUBLE, " +
      "daylow    DOUBLE, " +
      "dayvolume DOUBLE, " +
      "dayamount DOUBLE, " +
      "daychange DOUBLE, " +
      "tsourceid BIGINT)"

      var sql = if (dbDriver.contains("derby")) createTableSql_derby else createTableSql_h2_hsqldb
      stmt.executeUpdate(sql)

      /** index name in db is glode name, so, use idx_tableName_xxx to identify them */
      sql = "CREATE INDEX idx_ttime_" + tickerTb + " ON " + tickerTb + " (ttime)"
      stmt.executeUpdate(sql)

      sql = "CREATE INDEX idx_tsymbol_" + tickerTb + " ON " + tickerTb + " (tsymbol)"
      stmt.executeUpdate(sql)

      sql = "CREATE INDEX idx_tsourceid_" + tickerTb + " ON " + tickerTb + " (tsourceid)"
      stmt.executeUpdate(sql)

      // --- depth table
      val depthTb = "realtime_depth"
      val stmtCreateDepthTableStr_derby = "CREATE TABLE " + depthTb + "(" +
      "did        INTEGER NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1) PRIMARY KEY, " +
      "tid        INTEGER NOT NULL, " +
      "dlevel     SMALLINT, " +
      "ddirection SMALLINT, " + // bid = -1, ask = 1
      "dprice     DOUBLE, " +
      "dsize      DOUBLE, " +
      "dopid      CHAR(30))"

      val stmtCreateDepthTableStr_h2_hsqldb = "CREATE CACHED TABLE " + depthTb + "(" +
      "did        INTEGER NOT NULL IDENTITY(1, 1) PRIMARY KEY, " +
      "tid        INTEGER NOT NULL, " +
      "dlevel     SMALLINT, " +
      "ddirection SMALLINT, " + // bid = -1, ask = 1
      "dprice     DOUBLE, " +
      "dsize      DOUBLE, " +
      "dopid      CHAR(30))"

      sql = if (dbDriver.contains("derby")) stmtCreateDepthTableStr_derby else stmtCreateDepthTableStr_h2_hsqldb
      stmt.executeUpdate(sql)

      sql = "CREATE INDEX idx_tid_" + depthTb + " ON " + depthTb + " (tid)"
      stmt.executeUpdate(sql)

      conn.commit
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }
  }

  def deleteRealTimeTickers {
    var conn: Connection = null
    var stmt1: PreparedStatement = null
    var stmt2: PreparedStatement = null
    try {
      conn = getDbConnection
      val tickerTb = "realtime_ticker"
      var sql = "DELETE FROM " + tickerTb

      stmt1 = conn.prepareStatement(sql)
      stmt1.execute

      val depthTb = "realtime_ticker"
      sql = "DELETE FROM " + depthTb

      stmt2 = conn.prepareStatement(sql)
      stmt2.execute

      conn.commit
      WIN_MAN.statusText = "Delete data of " + tickerTb + " successfully."
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn  != null) conn.close
      if (stmt1 != null) stmt1.close
      if (stmt2 != null) stmt2.close
    }
  }

  def saveRealTimeTickers(tickers: Array[Ticker], sourceId: Long) {
    var conn: Connection = null
    var stmt1: PreparedStatement = null
    var stmt2: PreparedStatement = null
    try {
      conn = getDbConnection
      val tickerTb = "realtime_ticker"
      val depthTb  = "realtime_depth"

      val sql1 =  "INSERT INTO " + tickerTb +
      " (ttime, tsymbol, prevclose, lastprice, dayopen, dayhigh, daylow, dayvolume, dayamount, daychange, tsourceid) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

      val sql2 =  "INSERT INTO " + depthTb +
      " (tid, dlevel, ddirection, dprice, dsize, dopid) VALUES (?, ?, ?, ?, ?, ?)"

      stmt1 = conn.prepareStatement(sql1, Statement.RETURN_GENERATED_KEYS)
      stmt2 = conn.prepareStatement(sql2)
      for (ticker <- tickers) {
        stmt1 setLong   (1, ticker.time)
        stmt1 setString (2, ticker.uniSymbol)
        stmt1 setDouble (3, ticker.prevClose)
        stmt1 setDouble (4, ticker.lastPrice)
        stmt1 setDouble (5, ticker.dayOpen)
        stmt1 setDouble (6, ticker.dayHigh)
        stmt1 setDouble (7, ticker.dayLow)
        stmt1 setDouble (8, ticker.dayVolume)
        stmt1 setDouble (9, ticker.dayAmount)
        stmt1 setDouble (10, ticker.dayChange)
        stmt1 setLong   (11, sourceId)

        stmt1.execute
        val keys = stmt1.getGeneratedKeys
        if (keys.next) {
          val tickerId = keys.getInt(1)

          // bid (direction = -1)
          var level = 0
          while (level < ticker.depth) {
            stmt2 setInt    (1, tickerId)
            stmt2 setInt    (2, level)
            stmt2 setInt    (3, -1)
            stmt2 setDouble (4, ticker.bidPrice(level))
            stmt2 setDouble (5, ticker.bidSize(level))
            stmt2 setString (6, "")

            stmt2.addBatch
            level += 1
          }

          // ask (direction = 1)
          level = 0
          while (level < ticker.depth) {
            stmt2 setInt    (1, tickerId)
            stmt2 setInt    (2, level)
            stmt2 setInt    (3, 1)
            stmt2 setDouble (4, ticker.askPrice(level))
            stmt2 setDouble (5, ticker.askSize(level))
            stmt2 setString (6, "")

            stmt2.addBatch
            level += 1
          }
        }
      }
      
      stmt2.executeBatch

      conn.commit
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn  != null) conn.close
      if (stmt1 != null) stmt1.close
      if (stmt2 != null) stmt2.close
    }
  }

  def restoreTickers(symbol: String): Array[Ticker] = {
    val tickers = new ArrayBuffer[Ticker]

    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = getDbConnection
      val tickerTb = "realtime_ticker"
      val depthTb  = "realtime_depth"
      val sql = "SELECT * FROM " + tickerTb + " AS a LEFT JOIN " + depthTb + " AS b ON a.id = b.tid WHERE a.tsymbol = '" + symbol + "' ORDER BY a.ttime ASC"

      stmt = conn.prepareStatement(sql)
      val rs = stmt.executeQuery
      var ticker: Ticker = null
      while (rs.next) {
        val symbol = rs.getString("tsymbol")
        if (ticker == null || ticker.uniSymbol != symbol) {
          // (ttime, tsymbol, prevclose, lastprice, dayopen, dayhigh, daylow, dayvolume, dayamount, daychange, tsourceid)
          ticker = new Ticker
          ticker.uniSymbol = symbol
          ticker.time      = rs getLong  ("ttime")
          ticker.prevClose = rs getDouble ("prevclose")
          ticker.lastPrice = rs getDouble ("lastprice")
          ticker.dayOpen   = rs getDouble ("dayopen")
          ticker.dayHigh   = rs getDouble ("dayhigh")
          ticker.dayLow    = rs getDouble ("daylow")
          ticker.dayVolume = rs getDouble ("dayvolume")
          ticker.dayAmount = rs getDouble ("dayamount")
          ticker.dayChange = rs getDouble ("daychange")
        } else {
          // (tid, dlevel, ddirection, dprice, dsize, dopid)
          val level     = rs getInt   ("dlevel")
          val direction = rs getInt   ("ddirection")
          val price     = rs getDouble ("dprice")
          val size      = rs getDouble ("dsize")
          val opid      = rs getDouble ("dopid")
          if (direction == -1) {
            ticker.setBidPrice(level, price)
            ticker.setBidSize (level, size)
          } else {
            ticker.setAskPrice(level, price)
            ticker.setAskSize (level, size)
          }
        }

        tickers += ticker
      }
      rs.close

      conn.commit

      WIN_MAN.statusText = tickers.size + " quotes restored from database."
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }

    tickers.toArray
  }

  /**
   * @return lastest tickers of all symbols
   */
  def restoreRealTimeTickersOverview: Array[Ticker] = {
    val tickers = new ArrayBuffer[Ticker]

    var conn: Connection = null
    var stmt: PreparedStatement = null
    try {
      conn = getDbConnection
      val tickerTb = "realtime_ticker"
      var sql = "SELECT * FROM " + tickerTb + " AS a WHERE a.ttime = (SELECT max(a1.ttime) FROM " + tickerTb + " AS a1 WHERE a.tsymbol = a1.tsymbol)"

      stmt = conn.prepareStatement(sql)
      val rs = stmt.executeQuery
      var ticker: Ticker = null
      while (rs.next) {
        val symbol = rs.getString("tsymbol")
        if (ticker == null || ticker.uniSymbol != symbol) {
          // (ttime, tsymbol, prevclose, lastprice, dayopen, dayhigh, daylow, dayvolume, dayamount, daychange, tsourceid)
          ticker = new Ticker
          ticker.uniSymbol = symbol
          ticker.time      = rs getLong   ("ttime")
          ticker.prevClose = rs getDouble ("prevclose")
          ticker.lastPrice = rs getDouble ("lastprice")
          ticker.dayOpen   = rs getDouble ("dayopen")
          ticker.dayHigh   = rs getDouble ("dayhigh")
          ticker.dayLow    = rs getDouble ("daylow")
          ticker.dayVolume = rs getDouble ("dayvolume")
          ticker.dayAmount = rs getDouble ("dayamount")
          ticker.dayChange = rs getDouble ("daychange")
        } else {
          // do not need depth
        }

        tickers += ticker
      }
      rs.close

      conn.commit
      WIN_MAN.statusText = "Load market overview successfully."
    } catch {case ex: SQLException => ex.printStackTrace} finally {
      if (conn != null) conn.close
      if (stmt != null) stmt.close
    }

    tickers.toArray
  }

  // select * from realtime_ticker as a where a.ttime = (SELECT max(a1.ttime) FROM realtime_ticker as a1 where a.tsymbol = a1.tsymbol) WHERE

  // ----- shutdown

  def shutdown {
    if (connPoolMgr != null) {
      connPoolMgr.dispose
    }
    
    if (dbProps == null) return
    
    /**
     * Derby special action:
     *
     * In embedded mode, an application should shut down Derby.
     * If the application fails to shut down Derby explicitly,
     * the Derby does not perform a checkpoint when the JVM shuts down,
     * which means that the next connection will be slower.
     * Explicitly shutting down Derby with the URL is preferred.
     * This style of shutdown will always throw an "exception".
     *
     * --------------------------------------------------------
     *
     * For h2 or hsqldb and many other databases:
     *
     * By default, a database is closed when the last connection is closed.
     * However, if it is never closed, the database is closed when the
     * virtual machine exists normally.
     */
    var SQLExGot = false
    try {
      dbProps.put("shutdown", "true")
      val conn = DriverManager.getConnection(dbUrl, dbProps)
    } catch {case ex: SQLException => SQLExGot = true}

    if (SQLExGot == true) {
      // shutdown sucessfully
    }

  }

  private def propTableName(symbol: String, freq: TFreq): String = {
    propSymbol(symbol) + "_" + freq.compactName
  }

  /**
   * table name can not contain '.', '^', '-' etc, and should start with letter instead of number
   */
  private def propSymbol(symbol: String): String = {
    val propSymbol = symbol.trim.replace('^', '_').replace('.', '_').replace('-', '_')
    "q" + propSymbol
  }
}
