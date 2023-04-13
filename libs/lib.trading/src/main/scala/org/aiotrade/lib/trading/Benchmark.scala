package org.aiotrade.lib.trading

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import org.aiotrade.lib.collection.ArrayList
import org.aiotrade.lib.util
import org.aiotrade.lib.util.actors.Reactor

/**
 * 
 * @author Caoyuan Deng
 */
class Benchmark(tradingService: TradingService) extends Reactor {
  final case class Payoff(time: Long, nav: Double, accRate: Double, periodRate: Double, riskFreeRate: Double, referNav: Double) {
    val periodRateForSharpe = periodRate - riskFreeRate
    
    override 
    def toString = {
      "%1$tY.%1$tm.%1$td \t %2$ 8.3f \t %3$ 8.2f%% \t %4$ 8.2f%% \t %5$ 8.2f%% \t %6$ 8.2f%% \t %7$ 8.3f".format(
        new Date(time), nav, accRate * 100, periodRate * 100, riskFreeRate * 100, periodRateForSharpe * 100, referNav
      )
    }
  }
  
  final case class MarginCall(time: Long, availableFunds: Double, equity: Double, positionEquity: Double, positionMagin: Double) {
    override 
    def toString = {
      "%1$tY.%1$tm.%1$td \t %2$ 8.2f \t %3$ 8.2f \t %4$ 8.2f \t %5$ 8.2f".format(
        new Date(time), availableFunds, equity, positionEquity, positionMagin
      )
    }
  }
  
  var initialEquity = tradingService.accounts.foldLeft(0.0){(s, x) => s + x.initialEquity}
  var payoffRatio = 0.0
  var annualizedPayoffRatio = 0.0
  private var lastEquity = 0.0
  private var maxEquity = Double.MinValue
  private var maxDrawdownEquity = Double.MaxValue
  var maxDrawdownRatio = Double.MinValue
  
  var tradeCount: Int = _
  var tradeFromTime: Long = Long.MinValue
  var tradeToTime: Long = Long.MinValue
  var tradePeriod: Int = _
  
  val times = new ArrayList[Long]
  val equities = new ArrayList[Double]()
  val referIndices = new ArrayList[Double]()
  private var initialReferIndex = Double.NaN
  private val marginCalls = ArrayList[MarginCall]
  private var secTransactions = Array[SecTransaction]()
  private var expTransactions = Array[ExpensesTransaction]()
  
  var dailyPayoffs: Array[Payoff] = Array()
  var weeklyPayoffs: Array[Payoff] = Array()
  var monthlyPayoffs: Array[Payoff] = Array()
  var rrr: Double = _
  var sharpeRatioOnWeek: Double = _
  var sharpeRatioOnMonth: Double = _
  
  var timezone = TimeZone.getDefault
  var dailyRiskFreeRate = 0.0 
  var weeklyRiskFreeRate = 0.0 // 0.003
  var monthlyRiskFreeRate = 0.0 // 0.003
  
  var reportHourOfDay = 23
  var reportDayOfWeek = Calendar.SATURDAY
  var reportDayOfMonth = 26
  
  def at(time: Long, equity: Double, referIndex: Double) {
    times += time
    equities += equity
    referIndices += referIndex
    
    if (tradeFromTime == Long.MinValue) tradeFromTime = time
    tradeToTime = time
    
    if (initialEquity.isNaN) initialEquity = equity
    if (initialReferIndex.isNaN) initialReferIndex = referIndex

    lastEquity = equity
    payoffRatio = equity / initialEquity - 1
    
    tradingService.accounts foreach {
      case x: FutureAccount if x.availableFunds < 0 =>
        marginCalls += MarginCall(time, x.availableFunds, x.equity, x.positionEquity, x.positionMargin)
      case _ =>
    }
    calcMaxDrawdown(equity)
  }
  
  def report: String = {
    tradePeriod = daysBetween(tradeFromTime, tradeToTime)
    annualizedPayoffRatio = math.pow(1 + payoffRatio, 365.24 / tradePeriod) - 1
    rrr = annualizedPayoffRatio / maxDrawdownRatio
    
    val navs = toNavs(initialEquity, equities)
    val referNavs = toNavs(initialReferIndex, referIndices)
    dailyPayoffs = calcPeriodicReturns(times.toArray, navs, referNavs)(getDailyReportTime(reportHourOfDay))(dailyRiskFreeRate)
    weeklyPayoffs = calcPeriodicReturns(times.toArray, navs, referNavs)(getWeeklyReportTime(reportDayOfWeek))(weeklyRiskFreeRate)
    monthlyPayoffs = calcPeriodicReturns(times.toArray, navs, referNavs)(getMonthlyReportTime(reportDayOfMonth))(monthlyRiskFreeRate)
    sharpeRatioOnWeek = math.sqrt(52) * calcSharpeRatio(weeklyPayoffs)
    sharpeRatioOnMonth = math.sqrt(12) * calcSharpeRatio(monthlyPayoffs)
    
    val transactions = collectTransactions
    secTransactions = transactions._1
    expTransactions = transactions._2
    
    toString
  }
  
  private def calcMaxDrawdown(equity: Double) {
    if (equity > maxEquity) {
      maxEquity = math.max(equity, maxEquity)
      maxDrawdownEquity = equity
    } else if (equity < maxEquity) {
      maxDrawdownEquity = math.min(equity, maxDrawdownEquity)
      maxDrawdownRatio = math.max((maxEquity - maxDrawdownEquity) / maxEquity, maxDrawdownRatio)
    }
  }
  
  private def calcSharpeRatio(xs: Array[Payoff]) = {
    if (xs.length > 0) {
      var sum = 0.0
      var i = 0
      while (i < xs.length) {
        val x = xs(i)
        sum += x.periodRateForSharpe
        i += 1
      }
      val average = sum / xs.length
      var devSum = 0.0
      i = 0
      while (i < xs.length) {
        val x = xs(i).periodRateForSharpe - average
        devSum += x * x
        i += 1
      }
      val stdDev = math.sqrt(devSum / xs.length)
      average / stdDev
    } else {
      0.0
    }
  }
  
  private def toNavs(initialValue: Double, values: ArrayList[Double]) = {
    val navs = Array.ofDim[Double](values.length)
    var i = 0
    while (i < values.length) {
      navs(i) = values(i) / initialValue
      i += 1
    }
    navs
  }
  
  /**
   * navs Net Asset Value
   */
  private def calcPeriodicReturns(times: Array[Long], navs: Array[Double], referNavs: Array[Double])(getPeriodicReportTimeFun: Long => Long)(riskFreeRate: Double) = {
    if (times.length > 0) {
      val reportTimes = new ArrayList[Long]()
      val reportNavs = new ArrayList[Double]()
      val reportReferNavs = new ArrayList[Double]()
    
      var prevNav = 1.0
      var prevReferNav = 1.0
      var reportTime = getPeriodicReportTimeFun(times(0)) // first reportTime
      var i = 0
      while (i < times.length) {
        val time = times(i)
        val nav = navs(i)
        val referNav = referNavs(i)

        if (time > reportTime) { // just passed reportTime,it's time to report previous periodic data
          reportTimes += reportTime
          reportNavs += prevNav
          reportReferNavs += prevReferNav
          // and set next reportTime
          reportTime = getPeriodicReportTimeFun(time)
        } else if (i == times.length - 1) { // last period, and not reported, report it whatever
          reportTimes += reportTime
          reportNavs += nav
          reportReferNavs += referNav
        } else {
        }
      
        // -- adjust control vars for next loop
        prevNav = nav
        prevReferNav = referNav
        i += 1
      }
    
      // -- calculate payoffs
      val payoffs = Array.ofDim[Payoff](reportTimes.length)
      prevNav = 1.0
      i = 0
      while (i < reportTimes.length) {
        val time = reportTimes(i)
        val nav = reportNavs(i)
        val referNav = reportReferNavs(i)
        val accRate = nav - 1
        val periodRate = nav / prevNav - 1
        payoffs(i) = Payoff(time, nav, accRate, periodRate, riskFreeRate, referNav)
        
        prevNav = nav
        i += 1
      }
      
      payoffs
    } else {
      Array[Payoff]()
    }
  } 
  
  /**
   * @param the hour of day of settlement, default 23 o'clock. @Note not >= 24
   * @param the current date calendar
   */
  private def getDailyReportTime(_reportHourOfDay: Int = 23)(now: Long) = {
    val cal = util.calendarOf(timezone)
    cal.setTimeInMillis(now)

    val reportHourOfDay = math.min(_reportHourOfDay, 23)
    if (cal.get(Calendar.HOUR_OF_DAY) > reportHourOfDay) {
      cal.add(Calendar.DAY_OF_YEAR, 1) // will report in next day
    }
    
    cal.set(Calendar.HOUR_OF_DAY, reportHourOfDay)
    cal.getTimeInMillis
  }

  /**
   * @param the day of week of settlement, default SATURDAY
   * @param the current date calendar
   */
  private def getWeeklyReportTime(_reportDayOfWeek: Int = Calendar.SATURDAY)(now: Long) = {
    val cal = util.calendarOf(timezone)
    cal.setTimeInMillis(now)

    val reportDayOfWeek = math.min(_reportDayOfWeek, Calendar.SUNDAY)
    if (cal.get(Calendar.DAY_OF_WEEK) > reportDayOfWeek) {
      cal.add(Calendar.WEEK_OF_YEAR, 1) // will report in next week
    }
    
    cal.set(Calendar.DAY_OF_WEEK, reportDayOfWeek)
    cal.getTimeInMillis
  }

  /**
   * @param the day of month of settlement, default 26
   * @param the current date calendar
   */
  private def getMonthlyReportTime(_reportDayOfMonth: Int = 26)(now: Long) = {
    val cal = util.calendarOf(timezone)
    cal.setTimeInMillis(now)

    val reportDayOfMonth = math.min(_reportDayOfMonth, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    if (cal.get(Calendar.DAY_OF_MONTH) > reportDayOfMonth) {
      cal.add(Calendar.MONTH, 1) // will report in next month
    }
    
    cal.set(Calendar.DAY_OF_MONTH, reportDayOfMonth)
    cal.getTimeInMillis
  }
  
  
  private def collectTransactions = {
    val secTransactions = new ArrayList[SecTransaction]()
    val expTransactions = new ArrayList[ExpensesTransaction]()
    for {
      account <- tradingService.accounts
      TradeTransaction(time, secTransaction, expensesTransaction, order) <- account.transactions
    } {
      secTransactions ++= secTransaction
      expTransactions += expensesTransaction
    }
    (secTransactions.toArray, expTransactions.toArray)
  }


  override 
  def toString = {
    val statDaily   = calcStatistics(dailyPayoffs)
    val statWeekly  = calcStatistics(weeklyPayoffs)
    val statMonthly = calcStatistics(monthlyPayoffs)
    ;
    """
================ Benchmark Report -- %1$s ================
Trade period           : %2$tY.%2$tm.%2$td --- %3$tY.%3$tm.%3$td (%4$s calendar days, %5$s trading periods)
Initial equity         : %6$.0f
Final equity           : %7$.0f  
Total Return           : %8$.2f%%
Annualized Return      : %9$.2f%% 
Max Drawdown           : %10$.2f%%
RRR                    : %11$5.2f
Sharpe Ratio on Weeks  : %12$5.2f  (%13$s weeks)
Sharpe Ratio on Months : %14$5.2f  (%15$s months)

================ Daily Return ================
date \u0009 nav \u0009 accum \u0009 period \u0009 riskfree \u0009 sharpe \u0009 refer
%16$s
Average:%17$ 5.2f%%  Max:%18$ 5.2f%%  Min:%19$ 5.2f%%  Stdev: %20$5.2f%%  Win: %21$5.2f%%  Loss: %22$5.2f%%  Tie: %23$5.2f%%

================ Weekly Return ================
date \u0009 nav \u0009 accum \u0009 period \u0009 riskfree \u0009 sharpe \u0009 refer
%24$s
Average:%25$ 5.2f%%  Max:%26$ 5.2f%%  Min:%27$ 5.2f%%  Stdev: %28$5.2f%%  Win: %29$5.2f%%  Loss: %30$5.2f%%  Tie: %31$5.2f%%

================ Monthly Return ================
date \u0009 nav \u0009 accum \u0009 period \u0009 riskfree \u0009 sharpe \u0009 refer
%32$s
Average:%33$ 5.2f%%  Max:%34$ 5.2f%%  Min:%35$ 5.2f%%  Stdev: %36$5.2f%%  Win: %37$5.2f%%  Loss: %38$5.2f%%  Tie: %39$5.2f%%
    
================ Margin Call ================
date           avaliableFunds           equity  positionEquity positionMargin
%40$s

================ Executions ================
date \u0009 sec \u0009 quantity \u0009 price \u0009 amount
%41$s
    """.format(
      tradingService.param,
      tradeFromTime, tradeToTime, tradePeriod, times.length,
      initialEquity,
      lastEquity,
      payoffRatio * 100,
      annualizedPayoffRatio * 100,
      maxDrawdownRatio * 100,
      rrr,
      sharpeRatioOnWeek, weeklyPayoffs.length,
      sharpeRatioOnMonth, monthlyPayoffs.length,
      
      dailyPayoffs.mkString("\n"),
      statDaily._1, statDaily._2, statDaily._3, statDaily._4, statDaily._5, statDaily._6, statDaily._7,
      
      weeklyPayoffs.mkString("\n"),
      statWeekly._1, statWeekly._2, statWeekly._3, statWeekly._4, statWeekly._5, statWeekly._6, statWeekly._7,
      
      monthlyPayoffs.mkString("\n"),
      statMonthly._1, statMonthly._2, statMonthly._3, statMonthly._4, statMonthly._5, statMonthly._6, statMonthly._7,
      
      marginCalls.mkString("\n"),
      secTransactions map (x => "%1$tY.%1$tm.%1$td \t %2$s \t %3$ d \t %4$8.2f \t %5$8.2f".format(new Date(x.time), x.order.sec.uniSymbol, x.quantity.toInt, x.price, math.abs(x.amount))) mkString ("\n")
    )
  }
  
  private def calcStatistics(payoffs: Array[Payoff]) = {
    val len = payoffs.length.toDouble
    var sum = 0.0
    var max = Double.MinValue
    var min = Double.MaxValue
    var win = 0
    var loss = 0
    var tie = 0
    var i = 0
    while (i < len) {
      val periodRate = payoffs(i).periodRate
      sum += periodRate
      max = math.max(max, periodRate)
      min = math.min(min, periodRate)
      if (periodRate > 0) win += 1 
      else if (periodRate < 0) loss += 1 
      else tie += 1
      i += 1
    }
    
    val average = if (len > 0) sum / len else 0.0
    var devSum = 0.0
    i = 0
    while (i < len) {
      val x = payoffs(i).periodRate - average
      devSum += x * x
      i += 1
    }
    val stdDev = math.sqrt(devSum / len)

    if (len > 0) {
      (average * 100, max * 100, min * 100, stdDev * 100, win / len * 100, loss / len * 100, tie / len * 100)
    } else {
      (0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
  }
  
  /**
   * @Note time2 > time1
   */
  private def daysBetween(time1: Long, time2: Long): Int = {
    val cal1 = Calendar.getInstance
    val cal2 = Calendar.getInstance
    cal1.setTimeInMillis(time1)
    cal2.setTimeInMillis(time2)
    
    val years = cal2.get(Calendar.YEAR) - cal1.get(Calendar.YEAR)
    var days = cal2.get(Calendar.DAY_OF_YEAR) - cal1.get(Calendar.DAY_OF_YEAR)
    var i = 0
    while (i < years) {
      cal1.set(Calendar.YEAR, cal1.get(Calendar.YEAR) + 1)
      days += cal1.getActualMaximum(Calendar.DAY_OF_YEAR)
      i += 1
    }
    days
  }
}

