package org.aiotrade.lib.util.pinyin

import org.aiotrade.lib.util.pinyin.format.HanyuPinyinOutputFormat
import org.aiotrade.lib.util.pinyin.format.exception.BadHanyuPinyinOutputFormatCombination

/**
 * A class provides several utility functions to convert Chinese characters
 * (both Simplified and Tranditional) into various Chinese Romanization
 * representations
 * 
 */
object PinyinHelper {
  /**
   * Get all unformmatted Hanyu Pinyin presentations of a single Chinese
   * character (both Simplified and Tranditional)
   *
   * <p>
   * For example, <br/> If the input is '间', the return will be an array with
   * two Hanyu Pinyin strings: <br/> "jian1" <br/> "jian4" <br/> <br/> If the
   * input is '李', the return will be an array with single Hanyu Pinyin
   * string: <br/> "li3"
   *
   * <p>
   * <b>Special Note</b>: If the return is "none0", that means the input
   * Chinese character exists in Unicode CJK talbe, however, it has no
   * pronounciation in Chinese
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted Hanyu Pinyin
   *         presentations with tone numbers; null for non-Chinese character
   *
   */
  def toHanyuPinyins(ch: Char): Array[String] =
    getUnformattedHanyuPinyins(ch)

  /**
   * Get all Hanyu Pinyin presentations of a single Chinese character (both
   * Simplified and Tranditional)
   *
   * <p>
   * For example, <br/> If the input is '间', the return will be an array with
   * two Hanyu Pinyin strings: <br/> "jian1" <br/> "jian4" <br/> <br/> If the
   * input is '李', the return will be an array with single Hanyu Pinyin
   * string: <br/> "li3"
   *
   * <p>
   * <b>Special Note</b>: If the return is "none0", that means the input
   * Chinese character is in Unicode CJK talbe, however, it has no
   * pronounciation in Chinese
   *
   * @param ch
   *            the given Chinese character
   * @param outputFormat
   *            describes the desired format of returned Hanyu Pinyin String
   *
   * @return a String array contains all Hanyu Pinyin presentations with tone
   *         numbers; return null for non-Chinese character
   *
   * @throws BadHanyuPinyinOutputFormatCombination
   *             if certain combination of output formats happens
   *
   * @see HanyuPinyinOutputFormat
   * @see BadHanyuPinyinOutputFormatCombination
   *
   */
  @throws(classOf[BadHanyuPinyinOutputFormatCombination])
  def toHanyuPinyins(ch: Char, outputFormat: HanyuPinyinOutputFormat): Set[String] =
    getFormattedHanyuPinyins(ch, outputFormat)

  /**
   * Return the formatted Hanyu Pinyin representations of the given Chinese
   * character (both in Simplified and Tranditional) in array format.
   *
   * @param ch
   *            the given Chinese character
   * @param outputFormat
   *            Describes the desired format of returned Hanyu Pinyin string
   * @return The formatted Hanyu Pinyin representations of the given codepoint
   *         in array format; null if no record is found in the hashtable.
   */
  @throws(classOf[BadHanyuPinyinOutputFormatCombination])
  private def getFormattedHanyuPinyins(ch: Char, outputFormat: HanyuPinyinOutputFormat): Set[String] = {
    val pinyins = getUnformattedHanyuPinyins(ch)
    var pinyinSet = Set[String]()

    var i = 0
    while (i < pinyins.length) {
      pinyinSet += PinyinFormatter.formatHanyuPinyin(pinyins(i), outputFormat)
      i += 1
    }

    pinyinSet
  }

  /**
   * Delegate function
   *
   * @param ch
   *            the given Chinese character
   * @return unformatted Hanyu Pinyin strings; null if the record is not found
   */
  private def getUnformattedHanyuPinyins(ch: Char): Array[String] =
    ChineseToPinyinConverter.getHanyuPinyins(ch)

  /**
   * Get all unformmatted Tongyong Pinyin presentations of a single Chinese
   * character (both Simplified and Tranditional)
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted Tongyong Pinyin
   *         presentations with tone numbers; null for non-Chinese character
   *
   * @see #toHanyuPinyinStringArray(char)
   *
   */
  def toTongyongPinyins(ch: Char): Set[String] =
    convertToTargetPinyins(ch, PinyinRomanizationType.TONGYONG_PINYIN)

  /**
   * Get all unformmatted Wade-Giles presentations of a single Chinese
   * character (both Simplified and Tranditional)
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted Wade-Giles presentations
   *         with tone numbers; null for non-Chinese character
   *
   * @see #toHanyuPinyinStringArray(char)
   *
   */
  def toWadeGilesPinyins(ch: Char): Set[String] =
    convertToTargetPinyins(ch, PinyinRomanizationType.WADEGILES_PINYIN)

  /**
   * Get all unformmatted MPS2 (Mandarin Phonetic Symbols 2) presentations of
   * a single Chinese character (both Simplified and Tranditional)
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted MPS2 (Mandarin Phonetic
   *         Symbols 2) presentations with tone numbers; null for non-Chinese
   *         character
   *
   * @see #toHanyuPinyinStringArray(char)
   *
   */
  def toMPS2Pinyins(ch: Char): Set[String] =
    convertToTargetPinyins(ch, PinyinRomanizationType.MPS2_PINYIN)

  /**
   * Get all unformmatted Yale Pinyin presentations of a single Chinese
   * character (both Simplified and Tranditional)
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted Yale Pinyin
   *         presentations with tone numbers; null for non-Chinese character
   *
   * @see #toHanyuPinyinStringArray(char)
   *
   */
  def toYalePinyins(ch: Char): Set[String] =
    convertToTargetPinyins(ch, PinyinRomanizationType.YALE_PINYIN)

  /**
   * @param ch
   *            the given Chinese character
   * @param targetPinyinSystem
   *            indicates target Chinese Romanization system should be
   *            converted to
   * @return string representations of target Chinese Romanization system
   *         corresponding to the given Chinese character in array format;
   *         null if error happens
   *
   * @see PinyinRomanizationType
   */
  private def convertToTargetPinyins(ch: Char, targetPinyinSystem: PinyinRomanizationType ): Set[String] = {
    val hanyuPinyins = getUnformattedHanyuPinyins(ch)
    var pinyinSet = Set[String]()

    var i = 0
    while (i < hanyuPinyins.length) {
      pinyinSet += PinyinRomanizationConverter.convertRomanizationSystem(hanyuPinyins(i), PinyinRomanizationType.HANYU_PINYIN, targetPinyinSystem)
      i += 1
    }

    pinyinSet
  }

  /**
   * Get all unformmatted Gwoyeu Romatzyh presentations of a single Chinese
   * character (both Simplified and Tranditional)
   *
   * @param ch
   *            the given Chinese character
   *
   * @return a String array contains all unformmatted Gwoyeu Romatzyh
   *         presentations with tone numbers; null for non-Chinese character
   *
   * @see #toHanyuPinyinStringArray(char)
   *
   */
  def toGwoyeuRomatzyhs(ch: Char): Set[String] =
    convertToGwoyeuRomatzyhs(ch)

  /**
   * @param ch
   *            the given Chinese character
   *
   * @return Gwoyeu Romatzyh string representations corresponding to the given
   *         Chinese character in array format; null if error happens
   *
   * @see PinyinRomanizationType
   */
  private def convertToGwoyeuRomatzyhs(ch: Char): Set[String] = {
    val hanyuPinyins = getUnformattedHanyuPinyins(ch)
    var pinyinSet = Set[String]()

    var i = 0
    while (i < hanyuPinyins.length)      {
      pinyinSet += GwoyeuRomatzyhConverter.convertHanyuPinyinToGwoyeuRomatzyh(hanyuPinyins(i))
      i += 1
    }

    pinyinSet
  }

}
