package com.streamxhub.common.util

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object DateUtils {

  private[this] val format = "yyyy-MM-dd HH:mm:ss"

  def parse(time: String, fmt: String = format): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    df.parse(time)
  }

  def milliSecond2Date(time: Long) = {
    new Date(time)
  }

  def second2Date(time: Long) = {
    milliSecond2Date(time * 1000)
  }

  def now(dateFormat: String = "yyyyMMdd") = {
    val df: SimpleDateFormat = new SimpleDateFormat(dateFormat)
    df.format(new Date())
  }

  def minuteOfDay(date: Date = new Date()): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
  }

  def minuteOf(date: Date = new Date()): Int = {
    (date.getTime / 1000 / 60).toInt
  }

  def secondOf(date: Date = new Date()): Int = {
    (date.getTime / 1000).toInt
  }

  def secondOfDay(date: Date = new Date()): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTime(date)
    minuteOfDay(date) * 60 + calendar.get(Calendar.SECOND)
  }

  def format(fmt: String = format, date: Date = new Date()): String = {
    if (date == null) null else new SimpleDateFormat(fmt).format(date)
  }

  def getTime(time: String, fmt: String = format): Long = {
    Try(new SimpleDateFormat(fmt).parse(time).getTime)
      .filter(_ > 0).getOrElse(System.currentTimeMillis())
  }

  /**
   *
   * @param start
   * @param end
   * @param fmt
   * @param open
   * @return
   */
  def rangeStr(start: String, end: String, fmt: String = "yyyyMMdd", open: Boolean = false): List[Date] = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    range(df.parse(start), df.parse(end), open)
  }

  //获取今天之前的n天
  def +-(i: Int)(implicit format: String = "yyyyMMdd"): String = {
    val cal = Calendar.getInstance
    cal.add(Calendar.DATE, i)
    val statTime = new SimpleDateFormat(format).format(cal.getTime)
    statTime
  }

  //获取今天之前的n天
  def option(format: String, i: Int, date: Date = new Date()): String = {
    val cal = Calendar.getInstance
    cal.setTime(date)
    cal.add(Calendar.DATE, i)
    new SimpleDateFormat(format).format(cal.getTime)
  }

  /**
   * 获取时间区间
   *
   * @param start
   * @param end
   * @param open 右闭区间？
   * @return
   */
  def range(start: Date, end: Date, open: Boolean = false): List[Date] = {
    var array = new ArrayBuffer[Date]()
    //lDate.add(dBegin);
    val calBegin: Calendar = Calendar.getInstance
    // 使用给定的 Date 设置此 Calendar 的时间
    calBegin.setTime(start)
    val calEnd: Calendar = Calendar.getInstance
    calEnd.setTime(end)

    // 测试此日期是否在指定日期之后
    while (end.after(calBegin.getTime)) {
      array += calBegin.getTime
      calBegin.add(Calendar.DAY_OF_MONTH, 1)
    }
    if (open) array += end
    array.toList.sorted
  }


  def main(args: Array[String]): Unit = {
    println(minuteOfDay())
  }

}
