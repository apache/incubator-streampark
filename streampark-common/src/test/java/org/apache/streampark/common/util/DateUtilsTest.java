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

package org.apache.streampark.common.util;

import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateUtilsTest {

    private static final String DATE_TEST_CASE = "2000-01-01 00:00:01";
    private static final long TIME_STAMP_TEST_CASE = 946656001000L;
    private static final TimeZone GMT8 = TimeZone.getTimeZone("GMT+8:00");

    @Test
    void stringToDateShouldParseStringCorrectly() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        Date date = sdf.parse(DATE_TEST_CASE);
        assertEquals(date.getTime(), DateUtils.stringToDate(DATE_TEST_CASE).getTime());
    }

    @Test
    void milliSecond2DateShouldConvertCorrectly() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        sdf.setTimeZone(GMT8);
        Date date = sdf.parse(DATE_TEST_CASE);
        assertEquals(date.getTime(), DateUtils.milliSecond2Date(TIME_STAMP_TEST_CASE).getTime());
    }

    @Test
    void second2DateShouldConvertCorrectly() throws Exception {
        Date resultDate = DateUtils.second2Date(TIME_STAMP_TEST_CASE / 1000);
        Date date = DateUtils.parse(DATE_TEST_CASE, DateUtils.FULL_FORMAT, GMT8);
        assertEquals(date.getTime(), resultDate.getTime());
    }

    @Test
    void nowShouldReturnCurrentDateInYyyyMmDdFormat() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FORMAT_YYYYMMDD);
        sdf.setTimeZone(TimeZone.getDefault());
        String today = sdf.format(new Date());
        assertEquals(today, DateUtils.now());
    }

    @Test
    void minuteOfDayShouldReturnCorrectMinute() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        assertEquals(0, DateUtils.minuteOfDay(sdf.parse(DATE_TEST_CASE)));
        assertEquals(1, DateUtils.minuteOfDay(sdf.parse("2000-01-01 00:01:01")));
        assertEquals(61, DateUtils.minuteOfDay(sdf.parse("2000-01-01 01:01:01")));
    }

    @Test
    void secondOfDayShouldReturnCorrectSecond() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        assertEquals(1, DateUtils.secondOfDay(sdf.parse(DATE_TEST_CASE)));
        assertEquals(61, DateUtils.secondOfDay(sdf.parse("2000-01-01 00:01:01")));
        assertEquals(3661, DateUtils.secondOfDay(sdf.parse("2000-01-01 01:01:01")));
    }

    @Test
    void minuteOfShouldReturnCorrectMinute() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        sdf.setTimeZone(GMT8);
        Date date = sdf.parse(DATE_TEST_CASE);
        long timestampMinute = TIME_STAMP_TEST_CASE / 1000 / 60;
        assertEquals(timestampMinute, DateUtils.minuteOf(date));
    }

    @Test
    void secondOfShouldReturnCorrectSecond() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        sdf.setTimeZone(GMT8);
        Date date = sdf.parse(DATE_TEST_CASE);
        long timestampSecond = TIME_STAMP_TEST_CASE / 1000;
        assertEquals(timestampSecond, DateUtils.secondOf(date));
    }

    @Test
    void addAndSubtractShouldAdjustDateByDays() throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(DateUtils.FULL_FORMAT);
        sdf.setTimeZone(GMT8);
        Date date = sdf.parse(DATE_TEST_CASE);
        Date resultDate = DateUtils.addAndSubtract(-1, date, GMT8);
        assertEquals("1999-12-31 00:00:01", DateUtils.format(resultDate, DateUtils.FULL_FORMAT, GMT8));

        date = sdf.parse("2020-02-29 00:00:01");
        resultDate = DateUtils.addAndSubtract(1, date, GMT8);
        assertEquals("2020-03-01 00:00:01", DateUtils.format(resultDate, DateUtils.FULL_FORMAT, GMT8));
        resultDate = DateUtils.addAndSubtract(-1, date, GMT8);
        assertEquals("2020-02-28 00:00:01", DateUtils.format(resultDate, DateUtils.FULL_FORMAT, GMT8));

        date = sdf.parse("2020-02-01 00:00:01");
        resultDate = DateUtils.addAndSubtract(28, date, GMT8);
        assertEquals("2020-02-29 00:00:01", DateUtils.format(resultDate, DateUtils.FULL_FORMAT, GMT8));
    }

    @Test
    void getTimeShouldReturnMilliseconds() {
        assertEquals(
            TIME_STAMP_TEST_CASE,
            DateUtils.getTime(DATE_TEST_CASE, DateUtils.FULL_FORMAT, GMT8));
    }

    @Test
    void toDurationShouldFormatHumanReadableDuration() {
        long oneSecond = TimeUnit.SECONDS.toMillis(1);
        long oneMinute = TimeUnit.MINUTES.toMillis(1);
        long oneHour = TimeUnit.HOURS.toMillis(1);
        long oneDay = TimeUnit.DAYS.toMillis(1);
        long all = oneSecond + oneMinute + oneHour + oneDay;

        assertEquals("1 days 1 hours 1 minutes 1 seconds ", DateUtils.toDuration(all));
        assertEquals("0 hours 0 minutes 1 seconds ", DateUtils.toDuration(oneSecond));
        assertEquals("0 hours 1 minutes ", DateUtils.toDuration(oneMinute));
        assertEquals("1 hours ", DateUtils.toDuration(oneHour));
        assertEquals("1 days ", DateUtils.toDuration(oneDay));
    }

    @Test
    void getTimeUnitShouldParseTimeUnit() {
        DateUtils.TimeUnitPair pair = DateUtils.getTimeUnit("");
        assertEquals(5, pair.num);
        assertEquals(TimeUnit.SECONDS, pair.unit);

        pair = DateUtils.getTimeUnit("5s");
        assertEquals(5, pair.num);
        assertEquals(TimeUnit.SECONDS, pair.unit);

        pair = DateUtils.getTimeUnit("4m");
        assertEquals(4, pair.num);
        assertEquals(TimeUnit.MINUTES, pair.unit);

        pair = DateUtils.getTimeUnit("3h");
        assertEquals(3, pair.num);
        assertEquals(TimeUnit.HOURS, pair.unit);

        pair = DateUtils.getTimeUnit("2d");
        assertEquals(2, pair.num);
        assertEquals(TimeUnit.DAYS, pair.unit);

        assertThrows(IllegalArgumentException.class, () -> DateUtils.getTimeUnit("5s4m3h2d"));
        DateUtils.TimeUnitPair invalid = DateUtils.getTimeUnit("invalid");
        assertEquals(5, invalid.num);
        assertEquals(TimeUnit.SECONDS, invalid.unit);
    }

    @Test
    void formatCSTTimeShouldFormatCorrectly() throws Exception {
        String usDate = "Fri Feb 11 17:30:00 CST 2024";
        assertEquals("2024-02-11 17:30:00", DateUtils.formatCSTTime(usDate, DateUtils.FULL_FORMAT));
    }
}
