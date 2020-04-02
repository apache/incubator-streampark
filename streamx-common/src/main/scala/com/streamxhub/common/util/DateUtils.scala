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

import java.text.SimpleDateFormat
import java.util.{Calendar, Date}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object DateUtils {

  val fullFormat = "yyyy-MM-dd HH:mm:ss"

  val dayFormat1 = "yyyyMMdd"

  val dayFormat2 = "yyyy-MM-dd"

  def parse(date: String, format: String = fullFormat): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(format)
    df.parse(date)
  }

  def milliSecond2Date(time: Long) = {
    new Date(time)
  }

  def second2Date(time: Long) = {
    milliSecond2Date(time * 1000)
  }

  def now(dateFormat: String = dayFormat1) = {
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

  def format(fmt: String = fullFormat, date: Date = new Date()): String = {
    if (date == null) null else new SimpleDateFormat(fmt).format(date)
  }

  def getTime(time: String, fmt: String = fullFormat): Long = {
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
  def rangeStr(start: String, end: String, fmt: String = dayFormat1, open: Boolean = false): List[Date] = {
    val df: SimpleDateFormat = new SimpleDateFormat(fmt)
    range(df.parse(start), df.parse(end), open)
  }

  //获取今天之前的n天
  def +-(i: Int, date: Date = new Date)(implicit format: String = dayFormat2): String = {
    val cal = Calendar.getInstance
    cal.setTime(date)
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
