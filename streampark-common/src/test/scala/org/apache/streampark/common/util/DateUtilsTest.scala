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

import org.scalatest.funsuite.AnyFunSuite

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DateUtilsTest extends AnyFunSuite {
  val dateTestCase = "2000-01-01 00:00:01"
  val timeStampTestCase: Long = 946656001000L

  test("stringToDate should parse string to date correctly") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    val date = sdf.parse(dateTestCase)
    assert(date.getTime == DateUtils.stringToDate(dateTestCase).getTime)
  }

  test("milliSecond2Date should convert milliseconds to date correctly") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    val date = sdf.parse(dateTestCase)
    assert(date.getTime == DateUtils.milliSecond2Date(timeStampTestCase).getTime)
  }

  test("second2Date should convert seconds to date correctly") {
    val resultDate = DateUtils.second2Date(timeStampTestCase / 1000)
    val date = DateUtils.parse(dateTestCase, DateUtils.fullFormat, TimeZone.getTimeZone("GMT+8:00"))
    assert(date.getTime == resultDate.getTime)
  }

  test("now should return current date in yyyy-MM-dd format") {
    val sdf = new SimpleDateFormat(DateUtils.format_yyyyMMdd)
    sdf.setTimeZone(TimeZone.getDefault)
    val today = sdf.format(new Date())
    assert(today == DateUtils.now())
  }

  test("minuteOfDay should return correct minute of day") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    var date = sdf.parse(dateTestCase)
    var minute = DateUtils.minuteOfDay(date)
    assert(minute == 0)

    date = sdf.parse("2000-01-01 00:01:01")
    minute = DateUtils.minuteOfDay(date)
    assert(minute == 1)

    date = sdf.parse("2000-01-01 01:01:01")
    minute = DateUtils.minuteOfDay(date)
    assert(minute == 61)
  }

  test("secondOfDay should return correct second of day") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    var date = sdf.parse(dateTestCase)
    var second = DateUtils.secondOfDay(date)
    assert(second == 1)

    date = sdf.parse("2000-01-01 00:01:01")
    second = DateUtils.secondOfDay(date)
    assert(second == 61)

    date = sdf.parse("2000-01-01 01:01:01")
    second = DateUtils.secondOfDay(date)
    assert(second == 3661)
  }

  test("minuteOf should return correct minute of the given date") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    // Avoid time zone issues
    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    val date = sdf.parse(dateTestCase)
    val minute = DateUtils.minuteOf(date)
    val timestampMinute = timeStampTestCase / 1000 / 60
    assert(timestampMinute == minute)
  }

  test("secondOf should return correct second of the given date") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    // Avoid time zone issues
    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    val date = sdf.parse(dateTestCase)
    val second = DateUtils.secondOf(date)
    val timestampSecond = timeStampTestCase / 1000
    assert(timestampSecond == second)
  }

  test("addAndSubtract should return the date after adding or subtracting specified days") {
    val sdf = new SimpleDateFormat(DateUtils.fullFormat)
    // Avoid time zone issues
    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8:00"))
    var date = sdf.parse(dateTestCase)
    var resultDate = DateUtils.addAndSubtract(-1, date)
    assert("1999-12-31 00:00:01" == DateUtils.format(resultDate))

    date = sdf.parse("2020-02-29 00:00:01")
    resultDate = DateUtils.addAndSubtract(1, date)
    assert("2020-03-01 00:00:01" == DateUtils.format(resultDate))
    resultDate = DateUtils.addAndSubtract(-1, date)
    assert("2020-02-28 00:00:01" == DateUtils.format(resultDate))

    date = sdf.parse("2020-02-01 00:00:01")
    resultDate = DateUtils.addAndSubtract(28, date)
    assert("2020-02-29 00:00:01" == DateUtils.format(resultDate))
  }

  test("getTime should return time in milliseconds for the given date string") {
    assert(timeStampTestCase == DateUtils.getTime(dateTestCase))
  }

  test("toDuration should convert milliseconds to human-readable duration string") {
    val oneSecondInMillis: Long = TimeUnit.SECONDS.toMillis(1)
    val oneMinutesInMillis: Long = TimeUnit.MINUTES.toMillis(1)
    val oneHourInMillis: Long = TimeUnit.HOURS.toMillis(1)
    val oneDayInMillis: Long = TimeUnit.DAYS.toMillis(1)
    val allConditionInOne =
      oneSecondInMillis + oneMinutesInMillis + oneHourInMillis + oneDayInMillis

    assert("1 days 1 hours 1 minutes 1 seconds " == DateUtils.toDuration(allConditionInOne))
    assert("0 hours 0 minutes 1 seconds " == DateUtils.toDuration(oneSecondInMillis))
    assert("0 hours 1 minutes " == DateUtils.toDuration(oneMinutesInMillis))
    assert("1 hours " == DateUtils.toDuration(oneHourInMillis))
    assert("1 days " == DateUtils.toDuration(oneDayInMillis))
  }

  test("getTimeUnit should parse time unit correctly") {
    assert((5, TimeUnit.SECONDS) == DateUtils.getTimeUnit(""))
    assert((5, TimeUnit.SECONDS) == DateUtils.getTimeUnit("5s"))
    assert((4, TimeUnit.MINUTES) == DateUtils.getTimeUnit("4m"))
    assert((3, TimeUnit.HOURS) == DateUtils.getTimeUnit("3h"))
    assert((2, TimeUnit.DAYS) == DateUtils.getTimeUnit("2d"))

    assertThrows[IllegalArgumentException](DateUtils.getTimeUnit("5s4m3h2d"))
    assertThrows[IllegalArgumentException](DateUtils.getTimeUnit("invalid"))
  }

  test("formatCSTTime should format CST time correctly") {
    val USDate = "Fri Feb 11 17:30:00 CST 2024"
    val date = DateUtils.formatCSTTime(USDate, DateUtils.fullFormat)
    assert("2024-02-11 17:30:00" == date)
  }
}
