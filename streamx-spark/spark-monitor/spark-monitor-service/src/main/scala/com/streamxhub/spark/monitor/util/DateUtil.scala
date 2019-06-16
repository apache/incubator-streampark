package com.streamxhub.spark.monitor.util

import java.io.Serializable
import java.text.{ParseException, SimpleDateFormat}
import java.util.{Calendar, Date}

import scala.collection.mutable
import scala.util.Try

object DateUtil {


  var format_day = "yyyy-MM-dd"

  def now(str: String = "yyyyMMdd"): String = {
    format(fmt = str)
  }

  /**
    *
    * @param date
    * @param fmt
    * @return
    */
  def format(date: Date = new Date(), fmt: String = "yyyMMdd"): String = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    df.format(date)
  }

  /**
    * 获取毫秒时间
    *
    * @param date
    * @param fmt
    * @return
    */
  def getTime(date: String, fmt: String = "yyyy-MM-dd HH:mm:ss"): Long = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    df.parse(date).getTime
  }

  def getYesterday: String = {
    val df: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd 00:00:00")
    val cal: Calendar = Calendar.getInstance()
    cal.add(Calendar.DATE, -1)
    df.format(cal.getTime)
  }


  /**
    *
    * @param start
    * @param end
    * @param fmt
    * @param open
    * @return
    */
  def rangeStr(start: String, end: String, fmt: String = "yyyyMMdd", open: Boolean = false): Set[Date] = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    range(df.parse(start), df.parse(end), open)
  }

  /**
    * 获取时间区间
    *
    * @param start
    * @param end
    * @param open 右闭区间？
    * @return
    */
  def range(start: Serializable, end: Serializable, open: Boolean = false, fmt: String = "yyyyMMdd"): Set[Date] = {
    val startDate = Try(start.asInstanceOf[Date]).getOrElse(parse(start.toString, fmt))
    val endDate = Try(end.asInstanceOf[Date]).getOrElse(parse(end.toString, fmt))
    val set: mutable.Set[Date] = mutable.Set.empty[Date]
    //lDate.add(dBegin);
    val calBegin: Calendar = Calendar.getInstance
    // 使用给定的 Date 设置此 Calendar 的时间
    calBegin.setTime(startDate)
    val calEnd: Calendar = Calendar.getInstance
    calEnd.setTime(endDate)

    // 测试此日期是否在指定日期之后
    while (endDate.after(calBegin.getTime)) {
      set.add(calBegin.getTime)
      calBegin.add(Calendar.YEAR, 1)
    }
    if (open) set.add(endDate)
    set.toSet
  }

  def parse(date: Serializable, fmt: String = "yyyy-MM-dd HH:mm:ss"): Date = {
    if (date == null) return null
    val format = new SimpleDateFormat(fmt)
    try
      return format.parse(date.toString)
    catch {
      case e: ParseException =>
        e.printStackTrace()
    }
    null
  }

  //获取今天之前的n天
  def getCurrDayPrevDay(format: String, i: Int): String = {
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, -Math.abs(i))
    val statTime = new SimpleDateFormat(format).format(cal.getTime)
    statTime
  }

  def secondOfDay(date: Date = new Date()): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    minuteOfDay(date) * 60 + calendar.get(Calendar.SECOND)
  }

  def minuteOfDay(date: Date = new Date()): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
  }


}
