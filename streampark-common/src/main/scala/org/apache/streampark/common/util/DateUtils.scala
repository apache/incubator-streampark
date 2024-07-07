/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.common.util

import java.text.{ParseException, SimpleDateFormat}
import java.time.{Duration, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import java.util._
import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.util._

object DateUtils {

  val fullFormat = "yyyy-MM-dd HH:mm:ss"

  val format_yyyyMMdd = "yyyyMMdd"

  def parse(
      date: String,
      format: String = fullFormat,
      timeZone: TimeZone = TimeZone.getDefault): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(format)
    df.setTimeZone(timeZone)
    df.parse(date)
  }

  def stringToDate(date: String): Date = {
    parse(date)
  }

  def milliSecond2Date(time: Long): Date = new Date(time)

  def second2Date(time: Long): Date = milliSecond2Date(time * 1000)

  def now(
      dateFormat: String = format_yyyyMMdd,
      timeZone: TimeZone = TimeZone.getDefault): String = {
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

  def format(
      date: Date = new Date(),
      fmt: String = fullFormat,
      timeZone: TimeZone = TimeZone.getDefault): String = {
    if (date == null) null
    else {
      val simpleDateFormat = new SimpleDateFormat(fmt)
      simpleDateFormat.setTimeZone(timeZone)
      simpleDateFormat.format(date)
    }
  }

  def getTime(
      time: String,
      fmt: String = fullFormat,
      timeZone: TimeZone = TimeZone.getDefault): Long = {
    val simpleDateFormat = new SimpleDateFormat(fmt)
    simpleDateFormat.setTimeZone(timeZone)
    Try(simpleDateFormat.parse(time).getTime)
      .filter(_ > 0)
      .getOrElse(System.currentTimeMillis())
  }

  /** Date plus and minus */
  def addAndSubtract(
      i: Int,
      date: Date = new Date,
      timeZone: TimeZone = TimeZone.getDefault): Date = {
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
    // convert long time to calendar
    val calendar = Calendar.getInstance
    calendar.setTimeInMillis(localTimeInMillis)
    // get the zone offset
    val zoneOffset = calendar.get(java.util.Calendar.ZONE_OFFSET)
    // get the daylight saving time offset
    val dstOffset = calendar.get(java.util.Calendar.DST_OFFSET)
    // subtract these offset from local time to get UTC time
    calendar.add(java.util.Calendar.MILLISECOND, -(zoneOffset + dstOffset))
    // get the UTC Standard Time
    new Date(calendar.getTimeInMillis)
  }

  def utcToLocal(utcDate: Date): Date = {
    val sdf = new SimpleDateFormat()
    sdf.setTimeZone(TimeZone.getDefault)
    val localTime = sdf.format(utcDate.getTime)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    sdf.parse(localTime)
  }

  /** <p>Description: convert UTC time to local time</p> */
  def utcToLocal(utcTime: String, format: String = fullFormat): Date = {
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"))
    val utcDate: Date = sdf.parse(utcTime)
    utcToLocal(utcDate)
  }

  /**
   * Convert duration in seconds to rich time duration format. e.g. 2 days 3 hours 4 minutes 5
   * seconds
   *
   * @param milliseconds
   * @return
   */
  def toDuration(milliseconds: Long): String = {
    val duration = Duration.ofMillis(milliseconds)
    val days = duration.toDays

    val duration1 = milliseconds - TimeUnit.DAYS.toMillis(days)
    lazy val hours = TimeUnit.MILLISECONDS.toHours(duration1)

    lazy val duration2 = duration1 - TimeUnit.HOURS.toMillis(hours)
    lazy val minutes = TimeUnit.MILLISECONDS.toMinutes(duration2)

    lazy val duration3 = duration2 - TimeUnit.MINUTES.toMillis(minutes)
    lazy val seconds = TimeUnit.MILLISECONDS.toSeconds(duration3)

    val builder = new mutable.StringBuilder
    if (days > 0) builder.append(days + " days ")
    if (hours > 0 || minutes > 0 || seconds > 0) {
      builder.append(hours + " hours ")
    }
    if (minutes > 0 || seconds > 0) builder.append(minutes + " minutes ")
    if (seconds > 0) builder.append(seconds + " seconds ")
    builder.toString
  }

  def toSecondDuration(time1: Date, time2: Date = new Date()): Long = {
    val startDateTime =
      LocalDateTime.ofInstant(time1.toInstant, ZoneId.systemDefault());
    val endDateTime =
      LocalDateTime.ofInstant(time2.toInstant, ZoneId.systemDefault());
    val duration = Duration.between(startDateTime, endDateTime)
    duration.toMillis / 1000
  }

  def getTimeUnit(
      time: String,
      default: (Int, TimeUnit) = (5, TimeUnit.SECONDS)): (Int, TimeUnit) = {
    val timeUnit = time match {
      case "" => null
      case x: String =>
        val num = x.replaceAll("\\s+|[a-z|A-Z]+$", "").toInt
        val unit = x.replaceAll("^\\d+|\\s+", "") match {
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
      // without unit, the value must be milliseconds, convert to seconds.
      case other if other._2 == null => (other._1 / 1000, TimeUnit.SECONDS)
      case other => other
    }
  }

  def formatFullTime(localDateTime: LocalDateTime): String =
    formatFullTime(localDateTime, fullFormat)

  def formatFullTime(localDateTime: LocalDateTime, pattern: String): String = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern(pattern)
    localDateTime.format(dateTimeFormatter)
  }

  private def getDateFormat(date: Date, dateFormatType: String): String = {
    val format = new SimpleDateFormat(dateFormatType)
    format.format(date)
  }

  @throws[ParseException]
  def formatCSTTime(date: String, format: String): String = {
    val sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US)
    val d = sdf.parse(date)
    DateUtils.getDateFormat(d, format)
  }

}
