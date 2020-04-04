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
import java.util.{Calendar, Date, TimeZone}

import scala.collection.mutable.ArrayBuffer
import scala.util.Try


object DateUtils {

  val fullFormat = "yyyy-MM-dd HH:mm:ss"

  val dayFormat1 = "yyyyMMdd"

  val dayFormat2 = "yyyy-MM-dd"

  def parse(date: String, format: String = fullFormat,timeZone:TimeZone = TimeZone.getDefault): Date = {
    val df: SimpleDateFormat = new SimpleDateFormat(format)
    df.setTimeZone(timeZone)
    df.parse(date)
  }

  def milliSecond2Date(time: Long) = {
    new Date(time)
  }

  def second2Date(time: Long) = {
    milliSecond2Date(time * 1000)
  }

  def now(dateFormat: String = dayFormat1,timeZone:TimeZone = TimeZone.getDefault) = {
    val df: SimpleDateFormat = new SimpleDateFormat(dateFormat)
    df.setTimeZone(timeZone)
    df.format(new Date())
  }

  def minuteOfDay(date: Date = new Date(),timeZone:TimeZone = TimeZone.getDefault): Int = {
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

  def secondOfDay(date: Date = new Date(),timeZone:TimeZone = TimeZone.getDefault): Int = {
    val calendar = Calendar.getInstance()
    calendar.setTimeZone(timeZone)
    calendar.setTime(date)
    minuteOfDay(date) * 60 + calendar.get(Calendar.SECOND)
  }

  def format(date: Date = new Date(),fmt: String = fullFormat,timeZone:TimeZone = TimeZone.getDefault): String = {
    if (date == null) null else {
      val simpleDateFormat = new SimpleDateFormat(fmt)
        simpleDateFormat.setTimeZone(timeZone)
        simpleDateFormat.format(date)
    }
  }

  def getTime(time: String, fmt: String = fullFormat,timeZone:TimeZone = TimeZone.getDefault): Long = {
    val simpleDateFormat = new SimpleDateFormat(fmt)
    simpleDateFormat.setTimeZone(timeZone)
    Try(simpleDateFormat.parse(time).getTime)
      .filter(_ > 0).getOrElse(System.currentTimeMillis())
  }


  //获取今天之前的n天
  def +-(i: Int, date: Date = new Date,timeZone:TimeZone = TimeZone.getDefault)(implicit format: String = dayFormat2): String = {
    val cal = Calendar.getInstance
    cal.setTimeZone(timeZone)
    cal.setTime(date)
    cal.add(Calendar.DATE, i)
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(timeZone)
    val statTime = sdf.format(cal.getTime)
    statTime
  }

  //获取今天之前的n天
  def option(format: String, i: Int, date: Date = new Date(),timeZone:TimeZone = TimeZone.getDefault): String = {
    val cal = Calendar.getInstance
    cal.setTimeZone(timeZone)
    cal.setTime(date)
    cal.add(Calendar.DATE, i)
    val sdf = new SimpleDateFormat(format)
    sdf.setTimeZone(timeZone)
    sdf.format(cal.getTime)
  }

  def main(args: Array[String]): Unit = {
    println(minuteOfDay())
  }

}
