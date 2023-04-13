package org.aiotrade.lib.util.pinyin.format

/**
 * Define the output format of character 'ü'
 * 
 */
object HanyuPinyinVCharType {
  /**
   * The option indicates that the output of 'ü' is "u:"
   */
  case object WITH_U_AND_COLON extends HanyuPinyinVCharType("WITH_U_AND_COLON")

  /**
   * The option indicates that the output of 'ü' is "v"
   */
  case object WITH_V extends HanyuPinyinVCharType("WITH_V")

  /**
   * The option indicates that the output of 'ü' is "ü" in Unicode form
   */
  case object WITH_U_UNICODE extends HanyuPinyinVCharType("WITH_U_UNICODE")
}

class HanyuPinyinVCharType private (val name: String)
