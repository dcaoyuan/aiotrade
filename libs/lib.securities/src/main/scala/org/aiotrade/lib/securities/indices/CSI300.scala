package org.aiotrade.lib.securities.indices

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.ZipInputStream
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.math.timeseries.TFreq
import org.aiotrade.lib.securities
import org.aiotrade.lib.securities.QuoteSer
import org.aiotrade.lib.securities.SecPicking
import org.aiotrade.lib.securities.model.Exchange
import org.aiotrade.lib.securities.model.Quote
import org.aiotrade.lib.securities.model.Sec
import org.aiotrade.lib.securities.model.SecInfo
import org.aiotrade.lib.securities.model.Sector
import org.aiotrade.lib.util.ValidTime
import scala.collection.mutable

object CSI300 {
  private val log = Logger.getLogger(getClass.getName)
  private val config = org.aiotrade.lib.util.config.Config()
  
  private val ONE_DAY = 24 * 60 * 60 * 1000

  val indexCurrentMonthSymbol    = "IF0001.FF"
  val indexNextMonthSymbol       = "IF0002.FF"
  val indexNextSeasonSymbol      = "IF0003.FF"
  val indexAfterNextSeasonSymbol = "IF0004.FF"

  private val IFDataHome = "org/aiotrade/lib/securities/indices/"
  private val IFQuoteFileName  = "IF0000Quote"
  private val IFMemberFileName = "IF0000Member"
  private val IFWeightFileName = "IF0000Weight"
  
  /**
   * @param file path without ext, will be try filePath + ".txt", then filePath + ".zip"
   * @return Option inputstream
   */
  private def getInputStream(filePath: String): Option[InputStream] = {
    getInputStream(filePath + ".txt", false) match {
      case None => getInputStream(filePath + ".zip", true)
      case some => some
    }
  }
  
  private def getInputStream(fullFilePath: String, isZip: Boolean): Option[InputStream] = {
    try {
      log.info("Getting inputstream for: " + fullFilePath)
      // try resource first
      val is = getClass.getClassLoader.getResourceAsStream(fullFilePath) match {
        case null => new FileInputStream(fullFilePath)
        case x => x
      }
      if (isZip) {
        val zis = new ZipInputStream(is)
        // @Note should call zis.getNextEntry to go ahead to wrapped file's position
        if (zis.getNextEntry != null) Some(zis) else None
      } else {
        Option(is)
      }
    } catch {
      case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex); None
    }
  }
  
  def loadIFMembers(secPicking: SecPicking) {
    val inOpt = config.getString("if0000.member") match {
      case Some(memberFile) => 
        log.info("Loading IF0000 members from " + memberFile)
        getInputStream(memberFile)
      case None => 
        log.info("Loading IF0000 members from " + IFDataHome + IFMemberFileName)
        getInputStream(IFDataHome + IFMemberFileName)
    }
    
    inOpt map (is => new BufferedReader(new InputStreamReader(is, "utf-8"))) match {
      case Some(reader) =>
        val df = new SimpleDateFormat("M/d/yyyy")
        val cal = Calendar.getInstance(Exchange.SS.timeZone)
        val dayFreq = TFreq.DAILY
        
        var line: String = null
        var lineNum = 0
        try {
          while ({line = reader.readLine; line != null}) {
            lineNum += 1
            line = line.trim
            if (line.length != 0 && !line.startsWith("#")) {
              line.split("\\s+") match {
                case Array(inOut, dateStr, symbol, name) =>
                  val uniSymbol = symbol + (if (symbol.startsWith("6")) ".SS" else ".SZ")
                  Exchange.secOf(uniSymbol) match {
                    case Some(sec) =>
                      val date = df.parse(dateStr)
                      cal.clear
                      cal.setTime(date)
                      val time = dayFreq.round(cal.getTimeInMillis, cal)
                  
                      val existedValidTimes = secPicking.secToValidTimes.getOrElse(sec, Nil)
                      if (inOut.toLowerCase == "in") {
                        existedValidTimes.find(_.isValid(time)) match {
                          case Some(validTime) =>
                          case None => secPicking += ValidTime(sec, time, 0)
                        }
                      } else { // out
                        existedValidTimes.find(_.isValid(time)) match {
                          case Some(validTime) => validTime.validTo = time - 1
                          case None => secPicking += ValidTime(sec, 0, time - 1)
                        }
                      }
                    case None =>
                      log.info("There is no sec of symbol: " + uniSymbol + ", " + name)
                  }
                case xs =>
                  log.info("Wrong or empty line at line(" + lineNum + "): " + line + ", was split to: " + xs.mkString("(", "," ,")"))
              }
            }
          }
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        } finally {
          try {
            reader.close
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
        
      case None =>
    }
  }
  
  def loadIFWeights(secPicking: SecPicking) {
    val inOpt = config.getString("if0000.weight") match {
      case Some(weightFile) => 
        log.info("Loading IF0000 weights from " + weightFile)
        getInputStream(weightFile)
      case None => 
        log.info("Loading IF0000 weights from " + IFDataHome + IFWeightFileName)
        getInputStream(IFDataHome + IFWeightFileName)
    }
    
    inOpt map (is => new BufferedReader(new InputStreamReader(is, "utf-8"))) match {
      case Some(reader) =>
        val df = new SimpleDateFormat("yyyy-M-d")
        val cal = Calendar.getInstance(Exchange.SS.timeZone)
        val dayFreq = TFreq.DAILY
        
        val secToTimes = new mutable.HashMap[Sec, mutable.HashMap[Long, Double]]()
        
        // skip first line
        reader.readLine

        var line: String = null
        var lineNum = 0
        try {
          while ({line = reader.readLine; line != null}) {
            lineNum += 1
            line = line.trim
            if (line.length != 0 && !line.startsWith("#")) {
              line.split("\\s?,\\s?").map(_.replaceAll("\"", "")) match {
                case Array(symbol, weightStr, endDateStr) =>
                  val uniSymbol = if (symbol.length == 6 && symbol.startsWith("6")) {
                    symbol + ".SS"
                  } else {
                    val preceding = Array.fill(6 - symbol.length)('0')
                    new String(preceding) + symbol + ".SZ"
                  }
                  Exchange.secOf(uniSymbol) match {
                    case Some(sec) =>
                      val date = df.parse(endDateStr)
                      cal.clear
                      cal.setTime(date)
                      val time = dayFreq.round(cal.getTimeInMillis, cal) // round to 0:00 at the day

                      val weight = weightStr.toDouble / 100.0
                      secToTimes(sec) = secToTimes.getOrElse(sec, new mutable.HashMap[Long, Double]()) += time -> weight
                    case None =>
                      log.info("There is no sec of symbol: " + uniSymbol)
                  }
                case xs =>
                  log.info("Wrong or empty line at line(" + lineNum + "): " + line + ", was split to: " + xs.mkString("(", "," ,")"))
              }
            }
          }
          
          for ((sec, _timeToWeight) <- secToTimes; timeToWeight = _timeToWeight.toArray.sortBy(-1 * _._1)) {
            //var prevValidTo = -1L
            for ((time, weight) <- timeToWeight) {
              //val validFrom = time
              //val validTo = if (prevValidTo == -1) 0 /* validFrom + ONE_DAY - 1 */ else prevValidTo
              secPicking += (sec, ValidTime(weight, time, time))
              //prevValidTo = validFrom - 1
            }
            log.info("Loaded weights for %s: %s".format(sec.uniSymbol, timeToWeight.length))
          }
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        } finally {
          try {
            reader.close
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
        
      case None =>
    }
  }
  
  def loadAShares = {
    val category = Sector.Category.Board
    val code = Sector.Board.AShare
    securities.getSecsOfSector(category, code) // about 2200 secs, loaded in 160s, memory: 1.6G
  }
  
  def buildSecPicking() = {
    val secPicking = new SecPicking()
    loadIFMembers(secPicking)
    loadIFWeights(secPicking)
    secPicking
  }
  
  def buildSecPicking(knownLastSecs: Array[Sec]) = {
    val secPicking = new SecPicking()
    loadIFMembers(secPicking)
    loadIFWeights(secPicking)
    for (sec <- knownLastSecs) {
      secPicking.secToValidTimes.getOrElse(sec, Nil).sortBy(-1 * _.validTo).headOption match {
        case Some(lastValidTime) => 
          if (lastValidTime.validTo != 0) {
            log.info("Warning: the lastValidTime of %s is at %s".format(sec.uniSymbol, new Date(lastValidTime.validTo)))
          }
        case None =>
          // it should have been existed before content of txt
          secPicking += ValidTime(sec, 0, 0)
      }
    }
    
    secPicking
  }
  
  def buildIFs(csi300Ser: QuoteSer): (Sec, Sec, Sec, Sec) = {
    val indexCurrentMonth = createIF(indexCurrentMonthSymbol)
    val indexNextMonth = createIF(indexNextMonthSymbol)
    val indexNextSeason = createIF(indexNextSeasonSymbol)
    val indexAfterNextSeason = createIF(indexAfterNextSeasonSymbol)
    loadIF0000Ser(indexCurrentMonth, csi300Ser)
    
    (indexCurrentMonth, indexNextMonth, indexNextSeason, indexAfterNextSeason)
  }
  
  def createIF(uniSymbol: String) = {
    val symbol = uniSymbol.toUpperCase
    val exchange = Exchange.SS
    val sec = new Sec
    sec.crckey = symbol
    sec.exchange = exchange
    sec.id = sec.id // To calculate the crc32(crckey)
    
    val secInfo = new SecInfo
    secInfo.sec = sec
    secInfo.uniSymbol = symbol
    secInfo.name = symbol
    sec.secInfo = secInfo
    
    sec
  }
  
  private def loadIF0000Ser(sec: Sec, csi300Ser: QuoteSer) {
    val ser = new QuoteSer(sec, csi300Ser.freq)
    sec.setSer(ser)

    getInputStream(IFDataHome + IFQuoteFileName) match {
      case Some(is) =>
        try {
          val quotes = new ArrayList[Quote]()
          val timestamps = csi300Ser.timestamps
          val realQuotes = loadIFQuotes(is)
          val realFromTime = realQuotes(0).time
          val realToTime = realQuotes(realQuotes.length - 1).time
          var i = 0
          var isRealQuotesInserted = false
          while (i < timestamps.length) {
            val time = timestamps(i)
            if (time < realFromTime || time > realToTime) {
              csi300Ser.valueOf(time) match {
                case Some(quote) => quotes += quote
                case _ =>
              }
            } else {
              if (!isRealQuotesInserted) {
                isRealQuotesInserted = true
                quotes ++= realQuotes
              }
            }
          
            i += 1
          }

          log.info(quotes.length + " quotes are added to " + sec.uniSymbol)
          ser ++= quotes.toArray
          
        } catch {
          case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
        } finally {
          try {
            is.close
          } catch {
            case ex: Throwable => log.log(Level.WARNING, ex.getMessage, ex)
          }
        }
      case _ =>
        log.warning("Can't load IF quotes from file!")
    }
  }
  
  private def loadIFQuotes(is: InputStream) = {
    val df = new SimpleDateFormat("yyyy/M/d")
    val reader = new BufferedReader(new InputStreamReader(is))
		
    val cal = Calendar.getInstance(Exchange.SS.timeZone)
    val quotes = new ArrayList[Quote]()
    var line: String = null
    try {
      // skip first line
      reader.readLine
      var clusterId = -1
      while ({line = reader.readLine; line != null}) {
        line = line.trim
        if (line.length > 0) {
          line.split("\\s+") match {
            case Array(symbol, dateStr, open, high, low, close, volume, amount, prevClose) if (symbol == "SFIF0001") =>
              val date = df.parse(dateStr)
              cal.clear
              cal.setTime(date)
              val time = cal.getTimeInMillis
              
              val quote = new Quote
              quotes += quote
              quote.time = time
              quote.open = open.toDouble
              quote.high = high.toDouble
              quote.low = low.toDouble
              quote.close = close.toDouble
              quote.volume = volume.toDouble
              quote.amount = amount.toDouble
              quote.prevClose = prevClose.toDouble
            case _ =>
          }
        }
      }
    } catch {
      case ex: Exception => log.log(Level.WARNING, ex.getMessage, ex)
    }
	
    quotes
  }
  
  @throws(classOf[IOException])
  private def readCSI300Members(filePath: String): String = {
    val is = new FileInputStream(new File(filePath))
    try {
      val fc = is.getChannel
      val bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size)
      // instead of using default, pass in a decoder.
      Charset.forName("UTF-8").decode(bb).toString
    } finally {
      is.close
    }
  }
  
  // --- simple test
  def main(args: Array[String]) {
    val secPicking = buildSecPicking()
    var prevSecs = Set[Sec]()
    try {
      secPicking.allSecs foreach secPicking.printWeights
      println(secPicking)
      
      val cal = Calendar.getInstance
      cal.clear
      // begin
      cal.set(2005, 3, 28)
      printSecs(cal)
      cal.set(2005, 7, 1)
      printSecs(cal)
      cal.set(2007, 10, 30)
      printSecs(cal)
      
      // --- begins shift
      
      cal.set(2007, 11, 30)
      printSecs(cal)
      
      cal.set(2008, 0, 1)
      printSecs(cal)
      cal.set(2008, 5, 29)
      printSecs(cal)
      cal.set(2008, 6, 1)
      printSecs(cal)
      cal.set(2008, 11, 29)
      printSecs(cal)

      cal.set(2009, 0, 1)
      printSecs(cal)
      cal.set(2009, 5, 29)
      printSecs(cal)
      cal.set(2009, 6, 1)
      printSecs(cal)
      cal.set(2009, 11, 29)
      printSecs(cal)

      cal.set(2010, 0, 1)
      printSecs(cal)
      cal.set(2010, 5, 29)
      printSecs(cal)
      cal.set(2010, 6, 1)
      printSecs(cal)
      cal.set(2010, 11, 29)
      printSecs(cal)

      cal.set(2011, 0, 1)
      printSecs(cal)
      cal.set(2011, 5, 29)
      printSecs(cal)
      cal.set(2011, 6, 1)
      printSecs(cal)
      cal.set(2011, 11, 29)
      printSecs(cal)

      cal.set(2012, 0, 1)
      printSecs(cal)
      cal.set(2012, 5, 29)
      printSecs(cal)
      cal.set(2012, 6, 1)
      printSecs(cal)
      cal.set(2012, 11, 29)
      printSecs(cal)
      
      cal.set(2013, 0, 1)
      printSecs(cal)
      
      cal.set(2013, 2, 29)
      printSecs(cal)
      val secs = secPicking.at(cal.getTimeInMillis).sortBy(_.uniSymbol)
      val secToWeight = secPicking.weightsAt(cal.getTimeInMillis)
      println(secs.map(_.uniSymbol).mkString("=== members ===\n", "\n", "\n"))
      println(secToWeight.map(x => x._1.uniSymbol -> x._2).mkString("=== weights ===\n", "\n", "\n"))

      System.exit(0)
    } catch {
      case ex: Throwable => println(ex.getMessage); System.exit(-1)
    }
    
    def printSecs(cal: Calendar) {
      val secs = secPicking.at(cal.getTimeInMillis).toSet
      val secToWeight = secPicking.weightsAt(cal.getTimeInMillis)
      val (secsWithWeight, secsLackWeight) = secs partition secToWeight.contains
      val sumWeight = secToWeight.filter(x => secsWithWeight.contains(x._1)).values.sum
      println(cal.getTime + ": members=" + secs.size + ", weights=" + secToWeight.size + ", secsWithWeight=" + secsWithWeight.size + ", secsLackWeight=" + secsLackWeight.size + ", sumWeight=" + sumWeight)
      println("Out: \n" + secsStr((prevSecs -- secs)))
      println("In: \n"  + secsStr((secs -- prevSecs)))
      println("Secs lack weight: \n" + secsLackWeight.map(_.uniSymbol).mkString(" "))
      println("===============================\n")
      prevSecs = secs
    }
    
    def secsStr(secs: Set[Sec]) = {
      val sb = new StringBuilder()
      var sn = 0
      val sorted = secs.toList.sortBy(_.uniSymbol).map(x => x.uniSymbol + " " + x.name)
      for (sec <- sorted) {
        sn += 1
        sb.append(sn).append("\t").append(sec).append("\n")
      }
      sb.toString
    }
  }
}
