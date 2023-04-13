package org.aiotrade.lib.util.pinyin

import java.io.FileNotFoundException
import java.io.IOException
import scala.xml.Elem
import scala.xml.Node
import scala.xml.Text
import scala.xml.XML


/**
 * A class contains logic that translates from Hanyu Pinyin to Gwoyeu Romatzyh
 * 
 * 
 */
object GwoyeuRomatzyhConverter {

  /**
   * A DOM model contains Hanyu Pinyin to Gwoyeu Romatzyh mapping
   */
  private val pinyinToGwoyeuMappingDoc: Elem = try {
    val mappingFileName = "org/aiotrade/lib/util/pinyin/pinyin_gwoyeu_mapping.xml"

    XML.load(ResourceHelper.getResourceInputStream(mappingFileName))
    
  } catch {
    case ex: FileNotFoundException => throw ex
    case ex: IOException => throw ex
    case ex: Throwable => throw ex
  }

  /**
   * The postfixs to distinguish different tone of Gwoyeu Romatzyh
   *
   * <i>Should be removed if new xPath parser supporting tag name with number.</i>
   */
  private val tones = Array( "_I", "_II", "_III", "_IV", "_V" )

  /**
   * @param hanyuPinyinStr
   *            Given unformatted Hanyu Pinyin with tone number
   * @return Corresponding Gwoyeu Romatzyh; null if no mapping is found.
   */
  def convertHanyuPinyinToGwoyeuRomatzyh(hanyuPinyinStr: String): String = {
    val pinyin = TextHelper.extractPinyinString(hanyuPinyinStr)
    val toneNumber = TextHelper.extractToneNumber(hanyuPinyinStr)

    // return value
    var gwoyeuStr: String = null
    try {
      pinyinToGwoyeuMappingDoc \ "item" foreach {x =>
        if (search(x, pinyin)) {
          gwoyeuStr = x \ (PinyinRomanizationType.GWOYEU_ROMATZYH.tagName + tones(Integer.parseInt(toneNumber) - 1)) text
        }
      }

    } catch {
      case ex: Throwable => ex.printStackTrace
    }

    gwoyeuStr
  }

  private def search(p: Node, Name: String): Boolean = p match {
    case <Hanyu>{Text(Name)}</Hanyu> => true
    case _ => false
  }

}
