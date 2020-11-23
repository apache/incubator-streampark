/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.common.util

import java.util._
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.TimeZone
import scala.util._


object DateUtils {

  val fullFormat = "yyyy-MM-dd HH:mm:ss"

  val dayFormat1 = "yyyyMMdd"

  val dayFormat2 = "yyyy-MM-dd"

  def parse(date: String, format: String = fullFormat, timeZone: TimeZone = TimeZone.getDefault): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(format)
    df.setTimeZone(timeZone)
    df.parse(date)
  }

  def milliSecond2Date(time: Long) = new Date(time)

  def second2Date(time: Long) = milliSecond2Date(time * 1000)

  def now(dateFormat: String = dayFormat1, timeZone: TimeZone = TimeZone.getDefault) = {
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

  def minuteOf(date: Date = new Date()): Int = {
    (date.getTime / 1000 / 60).toInt
  }

  def secondOf(date: Date = new Date()): Int = {
    (date.getTime / 1000).toInt
  }

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
  def +-(i: Int, date: Date = new Date, timeZone: TimeZone = TimeZone.getDefault): Date = {
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

  def main(args: Array[String]): Unit = {
    println(DateUtils.+-(-1))
  }

}
