package org.aiotrade.lib.util.pinyin.format

/**
 * Define the output format of Hanyu Pinyin tones
 * 
 * <p>
 * Chinese has four pitched tones and a "toneless" tone. They are called Píng(平,
 * flat), Shǎng(上, rise), Qù(去, high drop), Rù(入, drop) and Qing(轻, toneless).
 * Usually, we use 1, 2, 3, 4 and 5 to represent them.
 * 
 */
object HanyuPinyinToneType {
  /**
   * The option indicates that hanyu pinyin is outputted with tone numbers
   */
  case object WITH_TONE_NUMBER extends HanyuPinyinToneType("WITH_TONE_NUMBER")

  /**
   * The option indicates that hanyu pinyin is outputted without tone numbers
   * or tone marks
   */
  case object WITHOUT_TONE extends HanyuPinyinToneType("WITHOUT_TONE")

  /**
   * The option indicates that hanyu pinyin is outputted with tone marks
   */
  case object WITH_TONE_MARK extends HanyuPinyinToneType("WITH_TONE_MARK")
}

class HanyuPinyinToneType private (val name: String)