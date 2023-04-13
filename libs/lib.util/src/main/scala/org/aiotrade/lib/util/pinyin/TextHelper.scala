package org.aiotrade.lib.util.pinyin

/**
 * Contains the utility functions supporting text processing
 * 
 * 
 */
object TextHelper {

  /**
   * @param hanyuPinyinWithToneNumber
   * @return Hanyu Pinyin string without tone number
   */
  def extractToneNumber(hanyuPinyinWithToneNumber: String): String =
    hanyuPinyinWithToneNumber.substring(hanyuPinyinWithToneNumber.length - 1)

  /**
   * @param hanyuPinyinWithToneNumber
   * @return only tone number
   */
  def extractPinyinString(hanyuPinyinWithToneNumber: String): String =
    hanyuPinyinWithToneNumber.substring(0, hanyuPinyinWithToneNumber.length - 1)

}
