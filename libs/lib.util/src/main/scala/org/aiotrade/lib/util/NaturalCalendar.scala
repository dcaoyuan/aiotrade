/*
 * Copyright (c) 2006-2007, AIOTrade Computing Co. and Contributors
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 *  o Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer. 
 *    
 *  o Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution. 
 *    
 *  o Neither the name of AIOTrade Computing Co. nor the names of 
 *    its contributors may be used to endorse or promote products derived 
 *    from this software without specific prior written permission. 
 *    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" 
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.aiotrade.lib.util

import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

/**
 * 
 * @author Caoyuan Deng 
 */
object NaturalCalendar {
  private val calendar: Calendar = new GregorianCalendar
  /**
   * The largest decimal literal of type int is 2,147,483,648 (2^31),
   * the total seconds of 1 year is about 366 * 24 * 60 * 60 = 31,622,400
   * the total seconds of 100 years is about 3,162,240,000
   * the total seconds of 68 years is about 2,150,323,200
   * So, if we choose the timeBaseDate as Jan 1, 1970,
   * plus 68 years to Jan 1, 2038, minus 50 years to Jan 1, 1902
   */
  private val timeBaseDate: NaturalCalendar = new NaturalCalendar(1970, 1, 1)

  private val ONE_SECOND: Int  = 1
  private val ONE_MINUTE: Int  = 60 * ONE_SECOND
  private val ONE_HOUR:   Int  = 60 * ONE_MINUTE
  private val ONE_DAY:    Long = 24 * ONE_HOUR
  private val ONE_WEEK:   Long =  7 * ONE_DAY

  def getTimeBaseDate: NaturalCalendar = timeBaseDate

  def calcJulianDate(y: Int, m: Int, d: Double): Double ={
    var year = y
    var month = m
    val double_day = d
    val day = double_day.toInt

    if (m == 1 | m == 2) {
      year -= 1
      month += 12
    }
    val A = (year / 100).toInt
    val B = 2 - A + (A / 4).toInt
    val C = (365.25 * year).toInt
    val D = (30.6001 * (month + 1)).toInt

    B + C + D + d + 1720994.5f
  }
  /** Method to calculate Gann important dates
   */
  def getGannImportantDate(date: Date): Int = {
    calendar.setTime(date)
    val month = calendar.get(Calendar.MONTH) + 1
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    var m = -1
    month match {
      case 1 =>
        if (day >= 7 && day <= 10 || day >= 19 && day <= 24) {
          m = 1
        }

      case 2 =>
        if (day >= 3 && day <= 10 || day >= 20 && day <= 25) {
          m = 2
        }

      case 3 =>
        if (day >= 20 && day <= 27) {
          m = 3
        }

      case 4 =>
        if (day >= 7 && day <= 12 || day >= 20 && day <= 25) {
          m = 4
        }

      case 5 =>
        if (day >= 3 && day <= 10 || day >= 21 && day <= 28) {
          m = 5
        }

      case 6 =>
        if (day >= 10 && day <= 15 || day >= 21 && day <= 27) {
          m = 6
        }

      case 7 =>
        if (day >= 7 && day <= 10 || day >= 21 && day <= 27) {
          m = 7
        }

      case 8 =>
        if (day >= 5 && day <= 8 || day >= 14 && day <= 20) {
          m = 8
        }

      case 9 =>
        if (day >= 3 && day <= 10 || day >= 21 && day <= 28) {
          m = 9
        }

      case 10 =>
        if (day >= 7 && day <= 14 || day >= 21 && day <= 30) {
          m = 10
        }

      case 11 =>
        if (day >= 5 && day <= 10 || day >= 20 && day <= 30) {
          m = 11
        }

      case 12 =>
        if (day >= 3 && day <= 10 || day >= 15 && day <= 24) {
          m = 12
        }

    }
    m
  }

  private var DATE_FORMAT = "YYYY-MM-DD"
  def setDateFormat(dateFormat: String): Unit = {
    DATE_FORMAT = dateFormat.toUpperCase.trim

    val year_s = DATE_FORMAT.indexOf('Y')
    val year_e = DATE_FORMAT.lastIndexOf('Y')
    val month_s = DATE_FORMAT.indexOf('M')
    val month_e = DATE_FORMAT.lastIndexOf('M')
    val day_s = DATE_FORMAT.indexOf('D')
    val day_e = DATE_FORMAT.lastIndexOf('D')
    val month_l = month_e - month_s + 1
    val year_l = year_e - year_s + 1
    //"Todo"
  }

  def main(args: Array[String]): Unit = {
    val date = new NaturalCalendar(1900, 1, 1)
    println("Julian Date: " + date.JULIAN_DATE)
  }


}

class NaturalCalendar(y: Int, m: Int, d: Double) {
  import NaturalCalendar._
  
  var YEAR: Int = _
  var MONTH: Int = _
  var DOUBLE_DAY: Double = _
  var DAY: Int = _
  var HOUR: Int = _
  var MINUTE: Int = _
  var SECOND: Int = _
  /** Julian Date, the days from epoch: Greenwich mean noon of January 1, 4713 B.C. */
  var JULIAN_DATE: Double = _

  set(y, m, d)

  def this(date: Calendar) = {
    this(date.get(Calendar.YEAR), date.get(Calendar.MONTH) + 1, date.get(Calendar.DAY_OF_MONTH))
  }

  /** Creat a new instance point to today
   */
  def this() = {
    this(Calendar.getInstance)
  }
        
  def this(julianDate: Double) = {
    this(0, 0, 0)
    set(julianDate)
  }
    
    
  def set(y: Int, m: Int, d: Double): NaturalCalendar ={
    YEAR = y
    MONTH = m
    DOUBLE_DAY = d
    DAY = DOUBLE_DAY.toInt

    var y1 = y
    var m1 = m
    if (m == 1 | m == 2) {
      y1 -= 1
      m1 += 12
    }
    val A = (y1 / 100).toInt
    val B = 2 - A + (A / 4).toInt
    val C = (365.25 * y1).toInt
    val D = (30.6001 * (m1 + 1)).toInt
        
    JULIAN_DATE = B + C + D + d + 1720994.5D
        
    this
  }
    
  def set(y: Int, m: Int, dayOfMonth: Int, hours: Int, minutes: Int, seconds: Int): NaturalCalendar =  {
    YEAR = y
    MONTH = m
        
    DOUBLE_DAY = dayOfMonth + (hours * ONE_HOUR + minutes * ONE_MINUTE + seconds).toDouble / ONE_DAY.toDouble
    DAY = DOUBLE_DAY.toInt
        
    HOUR = hours
    MINUTE = minutes
    SECOND = seconds
        
    var y1 = y
    var m1 = m
    if (m == 1 | m == 2) {
      y1 -= 1
      m1 += 12
    }
    val A = (y1 / 100).toInt
    val B = 2 - A + (A / 4).toInt
    val C = (365.25 * y).toInt
    val D = (30.6001 * (m + 1)).toInt
        
    JULIAN_DATE = B + C + D + dayOfMonth + 1720994.5D
        
    this
  }

  def set(julianDate: Double): NaturalCalendar = {
    JULIAN_DATE = julianDate
    var julianDate1 = julianDate
        
    julianDate1 += 0.5
    val I = (julianDate1).toInt
    val F = (I - julianDate1).toDouble
    val (aA, aB) = if (I > 2299160) {
      val A1 = ((I - 1867216.25) / 36524.25).toInt
      val B1 = I + 1 + A1 - (A1 / 4).toInt
      (A1, B1)
    } else {
      val B1 = I
      (0, B1)
    }
    val C = aB + 1524
    val D = ((C - 122.1) / 365.25).toInt
    val E = (365.25 * D).toInt
    val G = ((C - E) / 30.6001).toInt
        
    DOUBLE_DAY = C - E - F - (30.6001 * G).toInt
    MONTH = if (G < 13.5) G - 1 else G - 13
    YEAR = if (MONTH > 2.5) D - 4716 else D - 4715
    DAY = DOUBLE_DAY.toInt
        
    this
  }
    
  private def byTimeInMillins(time: Long): NaturalCalendar = {
    set(time + getTimeBaseDate.JULIAN_DATE)
    return this;
  }
    
  def add(i: Double): NaturalCalendar = {
    JULIAN_DATE += i
    set(JULIAN_DATE)
        
    this
  }
    
  /** Return period since timeBaseDate */
  private def getTime: Long = {
    (JULIAN_DATE - getTimeBaseDate.JULIAN_DATE).toLong
  }
    
  def after(date: NaturalCalendar): Boolean = {
    JULIAN_DATE > date.JULIAN_DATE
  }
    
  def before(date: NaturalCalendar): Boolean = {
    JULIAN_DATE < date.JULIAN_DATE
  }
    
  def equals(date: NaturalCalendar): Boolean = {
    JULIAN_DATE == date.JULIAN_DATE
  }
    
  def toDateString: String = {
    if (MONTH < 10 & DOUBLE_DAY < 10) {
      "0" + MONTH + "-" + "0" + DOUBLE_DAY.toInt + "-" + YEAR
    } else if (MONTH < 10 & DOUBLE_DAY >= 10) {
      "0" + MONTH + "-" + DOUBLE_DAY.toInt + "-" + YEAR
    } else if (MONTH >= 10 & DOUBLE_DAY < 10) {
      MONTH + "-" + "0" + DOUBLE_DAY.toInt + "-" + YEAR
    } else {
      MONTH + "-" + DOUBLE_DAY.toInt + "-" + YEAR
    }
  }
    
  def toMonthDayString: String = {
    if (MONTH < 10 & DOUBLE_DAY < 10) {
      "0" + MONTH + "-" + "0" + DOUBLE_DAY.toInt
    } else if (MONTH < 10 & DOUBLE_DAY >= 10) {
      "0" + MONTH + "-" + DOUBLE_DAY.toInt
    } else if (MONTH >= 10 & DOUBLE_DAY < 10) {
      MONTH + "-" + "0" + DOUBLE_DAY.toInt
    } else {
      MONTH + "-" + DOUBLE_DAY.toInt
    }
  }
    
  /** Calculate Chinese Month
   */
  def getChineseMonth: Int = {
    if (MONTH == 2 && DOUBLE_DAY == 3) {
      1
    } else if (MONTH == 3 && DOUBLE_DAY == 6) {
      2
    } else if (MONTH == 4 && DOUBLE_DAY == 5) {
      3
    } else if (MONTH == 5 && DOUBLE_DAY == 5) {
      4
    } else if (MONTH == 6 && DOUBLE_DAY == 6) {
      5
    } else if (MONTH == 7 && DOUBLE_DAY == 7) {
      6
    } else if (MONTH == 8 && DOUBLE_DAY == 7) {
      7
    } else if (MONTH == 9 && DOUBLE_DAY == 8) {
      8
    } else if (MONTH == 10 && DOUBLE_DAY == 8) {
      9
    } else if (MONTH == 11 && DOUBLE_DAY == 7) {
      10
    } else if (MONTH == 12 && DOUBLE_DAY == 7) {
      11
    } else if (MONTH == 1 && DOUBLE_DAY == 6) {
      12
    } else {
      -1
    }
  }
    
  /** Methods to calculate four seasons period
   */
  def getSeasonPeriod: Int = {
    var m = -1
    val today = this
    val startDate = new NaturalCalendar(today.YEAR, 3, 21)
    val endDate = new NaturalCalendar(today.YEAR, 6, 21)
    if (today.JULIAN_DATE >= startDate.JULIAN_DATE && today.JULIAN_DATE < endDate.JULIAN_DATE) {
      m = 1
    }
    startDate.set(today.YEAR, 6, 21)
    endDate.set(today.YEAR, 9, 22)
    if (today.JULIAN_DATE >= startDate.JULIAN_DATE && today.JULIAN_DATE < endDate.JULIAN_DATE) {
      m = 2
    }
    startDate.set(today.YEAR, 9, 22)
    endDate.set(today.YEAR, 12, 21)
    if (today.JULIAN_DATE >= startDate.JULIAN_DATE && today.JULIAN_DATE < endDate.JULIAN_DATE) {
      m = 3
    }
    startDate.set(today.YEAR, 12, 21)
    endDate.set(today.YEAR, 12, 31)
    if (today.JULIAN_DATE >= startDate.JULIAN_DATE && today.JULIAN_DATE < endDate.JULIAN_DATE) {
      m = 4
    }
    startDate.set(today.YEAR - 1, 12, 31)
    endDate.set(today.YEAR, 3, 20)
    if (today.JULIAN_DATE >= startDate.JULIAN_DATE && today.JULIAN_DATE < endDate.JULIAN_DATE) {
      m = 4
    }
    m
  }
    
    
    
  def parseDate(dateString: String): String = {
    var dateStr = dateString.trim();
    var year: Int = 0
    var month: Int = 0
    var day: Int = 0
        
    if (DATE_FORMAT.equalsIgnoreCase("YYYY-MM-DD")) {
            
      year = Integer.parseInt(dateStr.substring(0, 3))
      month = Integer.parseInt(dateStr.substring(5, 6))
      day = Integer.parseInt(dateStr.substring(8, 9))
            
    } else if (DATE_FORMAT.equalsIgnoreCase("YYYYMMDD")) {
            
      year = Integer.parseInt(dateStr.substring(0, 3))
      month = Integer.parseInt(dateStr.substring(4, 5))
      day = Integer.parseInt(dateStr.substring(6, 7))
            
    } else if (DATE_FORMAT.equalsIgnoreCase("YYMMDD")) {
            
      year = Integer.parseInt(dateStr.substring(0, 1))
      year = if (year > 20) year + 1900 else year + 2000
      month = Integer.parseInt(dateStr.substring(2, 3))
      day = Integer.parseInt(dateStr.substring(4, 5))
            
    } else if (DATE_FORMAT.equalsIgnoreCase("YYYY-MMM-DD")) {
            
      year = Integer.parseInt(dateStr.substring(0, 1))
      year = if (year > 20) year + 1900 else year + 2000
      month = Integer.parseInt(dateStr.substring(2, 3))
      day = Integer.parseInt(dateStr.substring(4, 5))
            
    }
        
    dateString
  }
    
  /** Convert JAN, FEB etc to numeric MONTH
   * @param String MONTH
   */
  private def toNumericMonth(month: String): Byte = {
    var m: Byte = 0
    if (month.toUpperCase.startsWith("JAN")) {
      m = 1
    }
    if (month.toUpperCase.startsWith("FEB")) {
      m = 2
    }
    if (month.toUpperCase.startsWith("MAR")) {
      m = 3
    }
    if (month.toUpperCase.startsWith("APR")) {
      m = 4
    }
    if (month.toUpperCase.startsWith("MAY")) {
      m = 5
    }
    if (month.toUpperCase.startsWith("JUN")) {
      m = 6
    }
    if (month.toUpperCase.startsWith("JUL")) {
      m = 7
    }
    if (month.toUpperCase.startsWith("AUG")) {
      m = 8
    }
    if (month.toUpperCase.startsWith("SEP")) {
      m = 9
    }
    if (month.toUpperCase.startsWith("OCT")) {
      m = 10
    }
    if (month.toUpperCase.startsWith("NOV")) {
      m = 11
    }
    if (month.toUpperCase.startsWith("DEC")) {
      m = 12
    }
    m
  }
    
    
}
