package org.aiotrade.lib.util.pinyin

import java.io.FileNotFoundException
import java.io.IOException
import java.util.Properties
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Manage all external resources required in PinyinHelper class.
 * 
 */
object ChineseToPinyinConverter {
  private val log = Logger.getLogger(this.getClass.getName)

  private val NONE_STR = "(none0)"
  private val LBRACKET = '('
  private val RBRACKET = ')'
  private val COMMA = ","
  private val EMPTY_STRS = Array[String]()


  /**
   * A properties table contains <Unicode, HanyuPinyin> pairs
   */
  private val unicodeToHanyuPinyins: Map[String, Array[String]] = try {
    var map = Map[String, Array[String]]()

    val resourceName = "org/aiotrade/lib/util/pinyin/unicode_to_hanyu_pinyin.txt"
    val props = new Properties
    props.load(ResourceHelper.getResourceInputStream(resourceName))

    val entries = props.entrySet.iterator
    while (entries.hasNext) {
      val entry = entries.next
      
      val value = entry.getValue.asInstanceOf[String]
      if (isValidRecord(value)) {
        val stripedRecord = value.substring(1, value.length - 1)
        val pinyins = stripedRecord.split(COMMA)
        
        map += (entry.getKey.asInstanceOf[String] -> pinyins)
      }
    }

    map

  } catch {
    case ex: FileNotFoundException => log.log(Level.SEVERE, ex.getMessage, ex); Map[String, Array[String]]()
    case ex: IOException => log.log(Level.SEVERE, ex.getMessage, ex); Map[String, Array[String]]()
  }

  /**
   * @param record
   *            given record string of Hanyu Pinyin
   * @return return true if record is not null and record is not "none0" and
   *         record is not mal-formatted, else return false
   */
  private def isValidRecord(record: String) = {
    record != null && record != NONE_STR && record.length > 0 &&
    record.charAt(0) == LBRACKET &&
    record.charAt(record.length - 1) == RBRACKET
  }

  /**
   * Get the unformatted Hanyu Pinyin representations of the given Chinese
   * character in array format.
   *
   * @param ch
   *            given Chinese character in Unicode
   * @return The Hanyu Pinyin strings of the given Chinese character in array
   *         format; return null if there is no corresponding Pinyin string.
   */
  def getHanyuPinyins(codePointOfChar: Char): Array[String] = {
    val codepointHexStr = Integer.toHexString(codePointOfChar).toUpperCase
    unicodeToHanyuPinyins.getOrElse(codepointHexStr, EMPTY_STRS)
  }

}
