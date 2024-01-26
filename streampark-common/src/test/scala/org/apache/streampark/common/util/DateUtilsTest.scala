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

import org.junit.jupiter.api.Assertions._
import org.junit.jupiter.api.Test

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class DateUtilsTest {
  val dateTestCase = "2000-01-01 00:00:01"
  val timeStampTestCase: Long = 946656001000L;

  @Test def stringToDateTest(): Unit = {
    val sdf: SimpleDateFormat = new SimpleDateFormat(DateUtils.fullFormat)
    val date = sdf.parse(dateTestCase);

    assertEquals(date, DateUtils.stringToDate(dateTestCase))
  }

  @Test def milliSecond2DateTest(): Unit = {
    val sdf: SimpleDateFormat = new SimpleDateFormat(DateUtils.fullFormat)
    val date = sdf.parse(dateTestCase)

    assertEquals(date, DateUtils.milliSecond2Date(timeStampTestCase))
  }

  @Test def getTimeTest(): Unit = {
    assertEquals(timeStampTestCase, DateUtils.getTime(dateTestCase))
  }

  @Test def toDurationTest(): Unit = {
    val oneSecondInMillis: Long = TimeUnit.SECONDS.toMillis(1)
    val oneMinutesInMillis: Long = TimeUnit.MINUTES.toMillis(1)
    val oneHourInMillis: Long = TimeUnit.HOURS.toMillis(1)
    val oneDayInMillis: Long = TimeUnit.DAYS.toMillis(1)
    val allConditionInOne =
      oneSecondInMillis + oneMinutesInMillis + oneHourInMillis + oneDayInMillis

    assertEquals("1 days 1 hours 1 minutes 1 seconds ", DateUtils.toDuration(allConditionInOne))
    assertEquals("0 hours 0 minutes 1 seconds ", DateUtils.toDuration(oneSecondInMillis))
    assertEquals("0 hours 1 minutes ", DateUtils.toDuration(oneMinutesInMillis))
    assertEquals("1 hours ", DateUtils.toDuration(oneHourInMillis))
    assertEquals("1 days ", DateUtils.toDuration(oneDayInMillis))
  }

  @Test def getTimeUnitTest(): Unit = {
    assertEquals((5, TimeUnit.SECONDS), DateUtils.getTimeUnit(""))
    assertEquals((5, TimeUnit.SECONDS), DateUtils.getTimeUnit("5s"))
    assertEquals((4, TimeUnit.MINUTES), DateUtils.getTimeUnit("4m"))
    assertEquals((3, TimeUnit.HOURS), DateUtils.getTimeUnit("3h"))
    assertEquals((2, TimeUnit.DAYS), DateUtils.getTimeUnit("2d"))

    assertThrows(classOf[IllegalArgumentException], () => DateUtils.getTimeUnit("5s4m3h2d"))
    assertThrows(classOf[IllegalArgumentException], () => DateUtils.getTimeUnit("invalid"))
  }
}
