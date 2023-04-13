package org.aiotrade.lib.dbfdriver
import java.util.Date
import java.text.SimpleDateFormat

object TestReader {
  val isPrintOnly = true

  val warmTimes = 5
  val times = if (isPrintOnly) 1 else 30
  val fileNames = List("show2003.dbf", "sjshq.dbf")
  
  def main(args: Array[String]) {
    try {
      for (fileName <- fileNames) {
        println("==== " + fileName + " ====")
        test1(fileName)
        if (!isPrintOnly) {
          println("==============")
          test2(fileName)
        }
      }
    
      System.exit(0)
    } catch {
      case ex: Throwable => ex.printStackTrace; System.exit(-1)
    }
  }

  def test1(fileName: String) {
    println("Use new DBFReader instance each time")
    
    var t0 = System.currentTimeMillis
    var i = -1
    while ({i += 1; i < times}) {
      if (i == warmTimes) t0 = System.currentTimeMillis // warm using the first warmTimes reading

      val reader = DBFReader(fileName, "GBK")

      readRecords(reader)
      reader.close
    }

    val countTimes = if (times > warmTimes) (times - warmTimes) else times
    println("Averagy time: " + (System.currentTimeMillis - t0) / countTimes + " ms")
  }

  def test2(fileName: String) {
    println("Use same DBFReader instance")
    var t0 = System.currentTimeMillis
    val reader = DBFReader(fileName, "GBK")
      
    var i = -1
    while ({i += 1; i < times}) {
      if (i == warmTimes) t0 = System.currentTimeMillis // warm using the first warmTimes reading

      reader.load
      readRecords(reader)
    }
    reader.close

    val countTimes = if (times > warmTimes) (times - warmTimes) else times
    println("Averagy time: " + (System.currentTimeMillis - t0) / countTimes + " ms")
  }

  def readRecords(reader: DBFReader) {
    if (isPrintOnly) {
      reader.header.fields foreach {x => print(x + " | ")}
      println
    }

    var i = -1
    val l = reader.recordCount
    while ({i += 1; i < l}) {
      val recordObjs = reader.nextRecord
      if (isPrintOnly) {
        recordObjs foreach {x => x match {
            case x: String => print( x + " | ")
            case x: Date => print(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(x) + " | ")
            case av => print(av + " | ")
          }}
        println
      } else {
        recordObjs foreach {x =>}
      }
    }
    
    println("Total Count: " + i)
  }

}
