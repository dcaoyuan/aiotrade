package org.aiotrade.lib.util.pinyin.format

/**
 * This classes define how the Hanyu Pinyin should be outputted.
 * 
 */
final case class HanyuPinyinOutputFormat(vCharType: HanyuPinyinVCharType = HanyuPinyinVCharType.WITH_U_AND_COLON,
                                         caseType: HanyuPinyinCaseType = HanyuPinyinCaseType.LOWERCASE,
                                         toneType: HanyuPinyinToneType = HanyuPinyinToneType.WITH_TONE_NUMBER)
