package org.aiotrade.lib.util.pinyin

/**
 * The class describes variable Chinese Pinyin Romanization System
 * 
 */
object PinyinRomanizationType {
  /**
   * Hanyu Pinyin system
   */
  case object HANYU_PINYIN extends PinyinRomanizationType("Hanyu")

  /**
   * Wade-Giles Pinyin system
   */
  case object WADEGILES_PINYIN extends PinyinRomanizationType("Wade")

  /**
   * Mandarin Phonetic Symbols 2 (MPS2) Pinyin system
   */
  case object MPS2_PINYIN extends PinyinRomanizationType("MPSII")

  /**
   * Yale Pinyin system
   */
  case object YALE_PINYIN extends PinyinRomanizationType("Yale")

  /**
   * Tongyong Pinyin system
   */
  case object TONGYONG_PINYIN extends PinyinRomanizationType("Tongyong")

  /**
   * Gwoyeu Romatzyh system
   */
  case object GWOYEU_ROMATZYH extends PinyinRomanizationType("Gwoyeu")
}

class PinyinRomanizationType(val tagName: String)
