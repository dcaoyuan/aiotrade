package org.aiotrade.lib.util.pinyin

import org.aiotrade.lib.util.pinyin.format.HanyuPinyinCaseType
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinOutputFormat
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinToneType
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinVCharType
import org.aiotrade.lib.util.pinyin.format.exception.BadHanyuPinyinOutputFormatCombination

/**
 * Contains logic to format given Pinyin string
 * 
 */
object PinyinFormatter {
  /**
   * @param pinyinStr
   *            unformatted Hanyu Pinyin string
   * @param outputFormat
   *            given format of Hanyu Pinyin
   * @return formatted Hanyu Pinyin string
   * @throws BadHanyuPinyinOutputFormatCombination
   */
  @throws(classOf[BadHanyuPinyinOutputFormatCombination])
  def formatHanyuPinyin(pinyinStr: String, outputFormat: HanyuPinyinOutputFormat): String = {
    if ((HanyuPinyinToneType.WITH_TONE_MARK == outputFormat.toneType) &&
        ((HanyuPinyinVCharType.WITH_V == outputFormat.vCharType) || (HanyuPinyinVCharType.WITH_U_AND_COLON == outputFormat.vCharType)))
    {
      throw new BadHanyuPinyinOutputFormatCombination("tone marks cannot be added to v or u:")
    }

    var pinyin = pinyinStr

    outputFormat.toneType match {
      case HanyuPinyinToneType.WITHOUT_TONE =>
        pinyin = pinyin.replaceAll("[1-5]", "")
      case HanyuPinyinToneType.WITH_TONE_MARK =>
        pinyin = pinyin.replaceAll("u:", "v")
        pinyin = toneNumberToToneMark(pinyin)
      case _ =>
    }
    
    outputFormat.vCharType match {
      case HanyuPinyinVCharType.WITH_V =>
        pinyin = pinyin.replaceAll("u:", "v")
      case HanyuPinyinVCharType.WITH_U_UNICODE =>
        pinyin = pinyin.replaceAll("u:", "ü")
      case _ =>
    }

    outputFormat.caseType match {
      case HanyuPinyinCaseType.UPPERCASE =>
        pinyin = pinyin.toUpperCase
      case _ =>
    }
    
    pinyin
  }

  /**
   * Convert tone numbers to tone marks using Unicode <br/><br/>
   *
   * <b>Algorithm for determining location of tone mark</b><br/>
   *
   * A simple algorithm for determining the vowel on which the tone mark
   * appears is as follows:<br/>
   *
   * <ol>
   * <li>First, look for an "a" or an "e". If either vowel appears, it takes
   * the tone mark. There are no possible pinyin syllables that contain both
   * an "a" and an "e".
   *
   * <li>If there is no "a" or "e", look for an "ou". If "ou" appears, then
   * the "o" takes the tone mark.
   *
   * <li>If none of the above cases hold, then the last vowel in the syllable
   * takes the tone mark.
   *
   * </ol>
   *
   * @param pinyinStr
   *            the ascii represention with tone numbers
   * @return the unicode represention with tone marks
   */
  private def toneNumberToToneMark(pinyin: String): String = {
    val lowerCasePinyin = pinyin.toLowerCase

    if (lowerCasePinyin.matches("[a-z]*[1-5]?")) {
      val defautlCharValue = '$'
      val defautlIndexValue = -1

      var unmarkedVowel = defautlCharValue
      var idxOfUnmarkedVowel = defautlIndexValue

      val charA = 'a'
      val charE = 'e'
      val ouStr = "ou"
      val allUnmarkedVowelStr = "aeiouv"
      val allMarkedVowelStr = "āáăàaēéĕèeīíĭìiōóŏòoūúŭùuǖǘǚǜü"

      if (lowerCasePinyin.matches("[a-z]*[1-5]")) {
        val tuneNumber = Character.getNumericValue(lowerCasePinyin.charAt(lowerCasePinyin.length - 1))

        val idxOfA = lowerCasePinyin.indexOf(charA)
        val idxOfE = lowerCasePinyin.indexOf(charE)
        val idxOfOU = lowerCasePinyin.indexOf(ouStr)

        if (-1 != idxOfA) {
          idxOfUnmarkedVowel = idxOfA
          unmarkedVowel = charA
        } else if (-1 != idxOfE) {
          idxOfUnmarkedVowel = idxOfE
          unmarkedVowel = charE
        } else if (-1 != idxOfOU) {
          idxOfUnmarkedVowel = idxOfOU
          unmarkedVowel = ouStr.charAt(0)
        } else {
          var i = lowerCasePinyin.length - 1
          var break = false
          while (i >= 0 && !break) {
            if (String.valueOf(lowerCasePinyin.charAt(i)).matches("[" + allUnmarkedVowelStr + "]")) {
              idxOfUnmarkedVowel = i
              unmarkedVowel = lowerCasePinyin.charAt(i)
              break = true
            }
            i -= 1
          }
        }

        if ((defautlCharValue != unmarkedVowel) && (defautlIndexValue != idxOfUnmarkedVowel)) {
          val rowIdx = allUnmarkedVowelStr.indexOf(unmarkedVowel)
          val colIdx = tuneNumber - 1

          val vowelLocation = rowIdx * 5 + colIdx

          val markedVowel = allMarkedVowelStr.charAt(vowelLocation)

          val sb = new StringBuffer

          sb.append(lowerCasePinyin.substring(0, idxOfUnmarkedVowel).replaceAll("v", "ü"))
          sb.append(markedVowel)
          sb.append(lowerCasePinyin.substring(idxOfUnmarkedVowel + 1, lowerCasePinyin.length - 1).replaceAll("v", "ü"))

          sb.toString

        } else { // error happens in the procedure of locating vowel
          lowerCasePinyin
        }
      } else { // input string has no any tune number
        // only replace v with ü (umlat) character
        lowerCasePinyin.replaceAll("v", "ü")
      }
    } else { // bad format
      lowerCasePinyin
    }
  }
}
