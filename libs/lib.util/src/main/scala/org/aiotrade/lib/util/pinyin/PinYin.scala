package org.aiotrade.lib.util.pinyin

import java.io.UnsupportedEncodingException
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinCaseType
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinOutputFormat
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinToneType
import org.aiotrade.lib.util.pinyin.format.HanyuPinyinVCharType

object PinYin {
  private val outFormat = HanyuPinyinOutputFormat(
    HanyuPinyinVCharType.WITH_V,
    HanyuPinyinCaseType.UPPERCASE,
    HanyuPinyinToneType.WITHOUT_TONE
  )

  def getCnSpells(cn: Char): Set[String] = {
    try {
      val bytes = String.valueOf(cn).getBytes("GB2312")
      if (bytes.length < 2) {
        Set(new String(bytes))
      } else {
        val spells = PinyinHelper.toHanyuPinyins(cn, outFormat)
        if (spells.nonEmpty) {
          spells
        } else Set(String.valueOf(cn))
      }
    } catch {
      case ex: UnsupportedEncodingException => Set(String.valueOf(cn))
    }
  }

  def getFullSpells(cnStr: String): Set[String] = {
    if (cnStr == null || cnStr.trim == "") return Set(cnStr)

    var allSpells = Set("")

    val chars = cnStr.toCharArray
    var i = 0
    while (i < chars.length) {
      val spells = getCnSpells(toDBC(chars(i)))

      var newSpells = Set[String]()
      for (spell <- spells) {
        for (prevSpell <- allSpells) {
          newSpells += prevSpell + spell
        }
      }
      allSpells = newSpells

      i += 1
    }

    allSpells
  }

  def getFirstSpells(cnStr: String): Set[String] = {
    if (cnStr == null || cnStr.trim == "") return Set(cnStr)

    var allSpells = Set("")

    val chars = cnStr.toCharArray
    var i = 0
    while (i < chars.length) {
      toDBC(chars(i)) match {
        case ' ' =>
        case c =>
          val spells = getCnSpells(c)

          var newSpells = Set[String]()
          for (spell <- spells) {
            val first = if (spell.length > 0) {
              spell.charAt(0)
            } else c

            for (prevSpell <- allSpells) {
              newSpells += prevSpell + first
            }
          }
          allSpells = newSpells
      }

      i += 1
    }

    allSpells
  }


  /**
   *  Convert full-width character (SBC case) to half-width character (DBC case)
   */
  private def toDBC(c: Char): Char = c match {
    case'\u3000' => ' '
    case c if c > '\uFF00' && c < '\uFF5F' => (c - 65248).toChar
    case c => c
  }

  /**
   *  Convert full-width character (SBC case) to half-width character (DBC case)
   */
  private def toDBC(input: String): Array[Char] = {
    val chars = input.toCharArray
    var i = 0
    while (i < chars.length) {
      chars(i) = toDBC(chars(i))

      i += 1
    }
    
    chars
  }

  def main(args: Array[String]) {
    val test = getCnSpells('鄂')
    test foreach println

    val t1 = System.currentTimeMillis

    val ss = Array(
      "浦发银行",
      "浦 发 银 行",
      "西藏矿业",
      "This is 俄国",
      "上证指数",
      "中国石油",
      "ＳＴ＊东海",
      "国债（１）",
      "万科Ａ"
    )

    for (s <- ss) {
      val s1 = toDBC(s)
      println(new String(s1) + ":  " + getFirstSpells(s).mkString("(", ",", ")"))
      println(new String(s1) + ":  " + getFullSpells(s).mkString("(", ",", ")"))
    }

    println("Time used: " + (System.currentTimeMillis - t1) + "ms")
  }

}
