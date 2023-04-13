package org.aiotrade.lib.util.pinyin.format

/**
 * Define the output case of Hanyu Pinyin string
 * 
 */
object HanyuPinyinCaseType {
  /**
   * The option indicates that hanyu pinyin is outputted as uppercase letters
   */
  case object UPPERCASE extends HanyuPinyinCaseType("UPPERCASE")

  /**
   * The option indicates that hanyu pinyin is outputted as lowercase letters
   */
  case object LOWERCASE extends HanyuPinyinCaseType("LOWERCASE")
}

class HanyuPinyinCaseType private (val name: String)
