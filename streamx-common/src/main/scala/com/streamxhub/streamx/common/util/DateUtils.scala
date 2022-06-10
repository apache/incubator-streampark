/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.common.util

import java.text.{ParseException, SimpleDateFormat}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util._
import java.util.concurrent.TimeUnit
import scala.util._


object DateUtils {

  val fullFormat = "yyyy-MM-dd HH:mm:ss"

  val format_yyyyMMdd = "yyyyMMdd"

  val fullCompact = "yyyyMMddHHmmss"

  def parse(date: String, format: String = fullFormat, timeZone: TimeZone = TimeZone.getDefault): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(format)
    df.setTimeZone(timeZone)
    df.parse(date)
  }

  def stringToDate(date: String): Date = {
    parse(date)
  }

  def milliSecond2Date(time: Long): Date = new Date(time)

  def second2Date(time: Long): Date = milliSecond2Date(time * 1000)

  def now(dateFormat: String = format_yyyyMMdd, timeZone: TimeZone = TimeZone.getDefault): String = {
    val df: SimpleDateFormat = new SimpleDateFormat(dateFormat)
    df.setTimeZone(timeZone)
    df.format(new Date())
  }

  def minuteOfDay(date: Date = new Date(), timeZone: TimeZone = TimeZone.getDefault): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTimeZone(timeZone)
    calendar.setTime(date)
    calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
  }

  def minuteOf(date: Date = new Date()): Int = (date.getTime / 1000 / 60).toInt

  def secondOf(date: Date = new Date()): Int = (date.getTime / 1000).toInt

  def secondOfDay(date: Date = new Date(), timeZone: TimeZone = TimeZone.getDefault): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTimeZone(timeZone)
    calendar.setTime(date)
    minuteOfDay(date) * 60 + calendar.get(Calendar.SECOND)
  }

  def format(date: Date = new Date(), fmt: String = fullFormat, timeZone: TimeZone = TimeZone.getDefault): String = {
    if (date == null) null else {
      val simpleDateFormat = new SimpleDateFormat(fmt)
      simpleDateFormat.setTimeZone(timeZone)
      simpleDateFormat.format(date)
    }
  }

  def getTime(time: String, fmt: String = fullFormat, timeZone: TimeZone = TimeZone.getDefault): Long = {
    val simpleDateFormat = new SimpleDateFormat(fmt)
    simpleDateFormat.setTimeZone(timeZone)
    Try(simpleDateFormat.parse(time).getTime)
      .filter(_ > 0).getOrElse(System.currentTimeMillis())
  }


  //日期加减...
  def addAndSubtract(i: Int, date: Date = new Date, timeZone: TimeZone = TimeZone.getDefault): Date = {
    val cal = Calendar.getInstance
    cal.setTimeZone(timeZone)
    cal.setTime(date)
    cal.add(Calendar.DATE, i)
    cal.getTime
  }

  def localToUTC(localTime: String, format: String = fullFormat): Date = {
    val value = new SimpleDateFormat(format).parse(localTime)
    localToUTC(value)
  }

  def localToUTC(localTime: Date): Date = {
    val localTimeInMillis = localTime.getTime
    /** long时间转换成Calendar */
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(localTimeInMillis)
    /** 取得时间偏移量 */
    val zoneOffset = calendar.get(java.util.Calendar.ZONE_OFFSET)
    /** 取得夏令时差 */
    val dstOffset = calendar.get(java.util.Calendar.DST_OFFSET)

    /** 从本地时间里扣除这些差量，即可以取得UTC时间 */
    calendar.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset))

    /** 取得的时间就是UTC标准时间 */
    new Date(calendar.getTimeInMillis)
  }

  def utcToLocal(utcDate: Date): Date = {
    val sdf = new SimpleDateFormat()
    sdf.setTimeZone(TimeZone.getDefault)
    val localTime = sdf.format(utcDate.getTime)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.parse(localTime)
  }

  /**
   * <p>Description:UTC时间转化为本地时间 </p>
   */
  def utcToLocal(utcTime: String, format: String = fullFormat): Date = {
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    val utcDate: Date = sdf.parse(utcTime)
    utcToLocal(utcDate)
  }

  /**
   * Convert duration in seconds to rich time duration format. e.g. 2 days 3 hours 4 minutes 5 seconds
   *
   * @param duration in second
   * @return
   */
  def toRichTimeDuration(duration: Long): String = {
    val days = TimeUnit.SECONDS.toDays(duration)
    val duration1 = duration - TimeUnit.DAYS.toSeconds(days)
    val hours = TimeUnit.SECONDS.toHours(duration1)
    val duration2 = duration1 - TimeUnit.HOURS.toSeconds(hours)
    val minutes = TimeUnit.SECONDS.toMinutes(duration2)
    val duration3 = duration2 - TimeUnit.MINUTES.toSeconds(minutes)
    val seconds = TimeUnit.SECONDS.toSeconds(duration3)
    val builder = new StringBuilder
    if (days != 0) builder.append(days + " days ")
    if (days != 0 || hours != 0) builder.append(hours + " hours ")
    if (days != 0 || hours != 0 || minutes != 0) builder.append(minutes + " minutes ")
    builder.append(seconds + " seconds")
    builder.toString
  }

  def getTimeUnit(time: String, default: (Int, TimeUnit) = (5, TimeUnit.SECONDS)): (Int, TimeUnit) = {
    val timeUnit = time match {
      case "" => null
      case x: String =>
        val num = x.replaceAll("\\s+|[a-z|A-Z]+$", "").toInt
        val unit = x.replaceAll("^[0-9]+|\\s+", "") match {
          case "" => null
          case "s" => TimeUnit.SECONDS
          case "m" | "min" => TimeUnit.MINUTES
          case "h" => TimeUnit.HOURS
          case "d" | "day" => TimeUnit.DAYS
          case _ => throw new IllegalArgumentException()
        }
        (num, unit)
    }
    timeUnit match {
      case null => default

      /**
       * 未带单位,值必须为毫秒,这里转成对应的秒...
       */
      case other if other._2 == null => (other._1 / 1000, TimeUnit.SECONDS)
      case other => other
    }
  }

  def formatFullTime(localDateTime: LocalDateTime): String = formatFullTime(localDateTime, fullFormat)

  def formatFullTime(localDateTime: LocalDateTime, pattern: String): String = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    localDateTime.format(dateTimeFormatter)
  }

  private def getDateFormat(date: Date, dateFormatType: String): String = {
    val format = new SimpleDateFormat(dateFormatType)
    format.format(date)
  }

  @throws[ParseException] def formatCSTTime(date: String, format: String): String = {
    val sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
    val d = sdf.parse(date)
    DateUtils.getDateFormat(d, format)
  }

}
