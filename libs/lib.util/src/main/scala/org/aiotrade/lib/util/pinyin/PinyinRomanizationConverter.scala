package org.aiotrade.lib.util.pinyin

/**
 * Contains the logic translating among different Chinese Romanization systems
 * 
 */
import java.io.FileNotFoundException
import java.io.IOException
import scala.xml.Elem
import scala.xml.Node
import scala.xml.XML

object PinyinRomanizationConverter {

  /**
   * A DOM model contains variable pinyin presentations
   */
  private val pinyinMappingDoc: Elem = try {
    val mappingFileName = "org/aiotrade/lib/util/pinyin/pinyin_mapping.xml"

    XML.load(ResourceHelper.getResourceInputStream(mappingFileName))

  } catch {
    case ex: FileNotFoundException => throw ex
    case ex: IOException => throw ex
    case ex: Throwable => throw ex
  }

  /**
   * convert the given unformatted Pinyin string from source Romanization
   * system to target Romanization system
   *
   * @param sourcePinyinStr
   *            the given unformatted Pinyin string
   * @param sourcePinyinSystem
   *            the Romanization system which is currently used by the given
   *            unformatted Pinyin string
   * @param targetPinyinSystem
   *            the Romanization system that should be converted to
   * @return unformatted Pinyin string in target Romanization system; null if
   *         error happens
   */
  def convertRomanizationSystem(sourcePinyinStr: String,
                                sourcePinyinSystem: PinyinRomanizationType,
                                targetPinyinSystem: PinyinRomanizationType): String = {
    val pinyin = TextHelper.extractPinyinString(sourcePinyinStr)
    val toneNumber = TextHelper.extractToneNumber(sourcePinyinStr)

    var targetPinyinStr: String = null
    try {

      pinyinMappingDoc \ "item" foreach {x =>
        if (search(x, sourcePinyinSystem.tagName, pinyin)) {
          targetPinyinStr = x \ targetPinyinSystem.tagName text
        }
      }

    } catch {
      case ex: Throwable => ex.printStackTrace
    }

    targetPinyinStr
  }

  private def search(p: Node, tagName: String, Name: String): Boolean = (p \ tagName text) match {
    case Name => true
    case _ => false
  }

}
