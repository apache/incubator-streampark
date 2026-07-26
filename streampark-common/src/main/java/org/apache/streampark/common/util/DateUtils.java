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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public final class DateUtils {

    public static final String FULL_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String FORMAT_YYYYMMDD = "yyyyMMdd";

    public static String fullFormat() {
        return FULL_FORMAT;
    }

    private DateUtils() {
    }

    public static Date parse(String date) throws ParseException {
        return parse(date, FULL_FORMAT, TimeZone.getDefault());
    }
    public static Date parse(String date, String format, TimeZone timeZone) throws ParseException {
        SimpleDateFormat df = new SimpleDateFormat(format);
        df.setTimeZone(timeZone);
        return df.parse(date);
    }
    public static Date stringToDate(String date) throws ParseException {
        return parse(date);
    }
    public static Date milliSecond2Date(long time) {
        return new Date(time);
    }
    public static Date second2Date(long time) {
        return milliSecond2Date(time * 1000);
    }
    public static String now() {
        return now(FORMAT_YYYYMMDD, TimeZone.getDefault());
    }
    public static String now(String dateFormat, TimeZone timeZone) {
        SimpleDateFormat df = new SimpleDateFormat(dateFormat);
        df.setTimeZone(timeZone);
        return df.format(new Date());
    }
    public static int minuteOfDay() {
        return minuteOfDay(new Date(), TimeZone.getDefault());
    }

    public static int minuteOfDay(Date date) {
        return minuteOfDay(date, TimeZone.getDefault());
    }

    public static int minuteOfDay(Date date, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
    }
    public static int minuteOf() {
        return minuteOf(new Date());
    }
    public static int minuteOf(Date date) {
        return (int) (date.getTime() / 1000 / 60);
    }
    public static int secondOf() {
        return secondOf(new Date());
    }
    public static int secondOf(Date date) {
        return (int) (date.getTime() / 1000);
    }
    public static int secondOfDay() {
        return secondOfDay(new Date(), TimeZone.getDefault());
    }

    public static int secondOfDay(Date date) {
        return secondOfDay(date, TimeZone.getDefault());
    }

    public static int secondOfDay(Date date, TimeZone timeZone) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(timeZone);
        calendar.setTime(date);
        return minuteOfDay(date, timeZone) * 60 + calendar.get(Calendar.SECOND);
    }
    public static String format() {
        return format(new Date(), FULL_FORMAT, TimeZone.getDefault());
    }
    public static String format(Date date, String fmt, TimeZone timeZone) {
        if (date == null)
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(timeZone);
        return sdf.format(date);
    }
    public static long getTime(String time) {
        return getTime(time, FULL_FORMAT, TimeZone.getDefault());
    }
    public static long getTime(String time, String fmt, TimeZone timeZone) {
        SimpleDateFormat sdf = new SimpleDateFormat(fmt);
        sdf.setTimeZone(timeZone);
        try {
            long t = sdf.parse(time).getTime();
            return t > 0 ? t : System.currentTimeMillis();
        } catch (ParseException e) {
            return System.currentTimeMillis();
        }
    }
    public static Date addAndSubtract(int i) {
        return addAndSubtract(i, new Date(), TimeZone.getDefault());
    }
    public static Date addAndSubtract(int i, Date date, TimeZone timeZone) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(timeZone);
        cal.setTime(date);
        cal.add(Calendar.DATE, i);
        return cal.getTime();
    }
    public static Date localToUTC(String localTime) throws ParseException {
        return localToUTC(localTime, FULL_FORMAT);
    }
    public static Date localToUTC(String localTime, String format) throws ParseException {
        return localToUTC(new SimpleDateFormat(format).parse(localTime));
    }
    public static Date localToUTC(Date localTime) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(localTime.getTime());
        int zoneOffset = calendar.get(Calendar.ZONE_OFFSET);
        int dstOffset = calendar.get(Calendar.DST_OFFSET);
        calendar.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));
        return new Date(calendar.getTimeInMillis());
    }
    public static Date utcToLocal(Date utcDate) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setTimeZone(TimeZone.getDefault());
        String localTime = sdf.format(utcDate.getTime());
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.parse(localTime);
    }
    public static Date utcToLocal(String utcTime) throws ParseException {
        return utcToLocal(utcTime, FULL_FORMAT);
    }
    public static Date utcToLocal(String utcTime, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcToLocal(sdf.parse(utcTime));
    }
    public static String toDuration(long milliseconds) {
        long days = Duration.ofMillis(milliseconds).toDays();
        long duration1 = milliseconds - TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(duration1);
        long duration2 = duration1 - TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration2);
        long duration3 = duration2 - TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration3);
        StringBuilder builder = new StringBuilder();
        if (days > 0)
            builder.append(days).append(" days ");
        if (hours > 0 || minutes > 0 || seconds > 0)
            builder.append(hours).append(" hours ");
        if (minutes > 0 || seconds > 0)
            builder.append(minutes).append(" minutes ");
        if (seconds > 0)
            builder.append(seconds).append(" seconds ");
        return builder.toString();
    }
    public static long toSecondDuration(Date time1) {
        return toSecondDuration(time1, new Date());
    }
    public static long toSecondDuration(Date time1, Date time2) {
        return Math.abs(time1.getTime() - time2.getTime()) / 1000;
    }
    public static TimeUnitPair getTimeUnit(String time) {
        return getTimeUnit(time, 5, TimeUnit.SECONDS);
    }
    public static TimeUnitPair getTimeUnit(String time, int defaultNum, TimeUnit defaultUnit) {
        if (time == null || time.isEmpty())
            return new TimeUnitPair(defaultNum, defaultUnit);
        String trimmed = time.trim();
        int digitEnd = 0;
        while (digitEnd < trimmed.length() && Character.isDigit(trimmed.charAt(digitEnd))) {
            digitEnd++;
        }
        if (digitEnd == 0) {
            return new TimeUnitPair(defaultNum, defaultUnit);
        }
        int num = Integer.parseInt(trimmed.substring(0, digitEnd));
        String unit = trimmed.substring(digitEnd).trim();
        if (unit.isEmpty())
            return new TimeUnitPair(num / 1000, TimeUnit.SECONDS);
        switch (unit) {
            case "s":
                return new TimeUnitPair(num, TimeUnit.SECONDS);
            case "m":
            case "min":
                return new TimeUnitPair(num, TimeUnit.MINUTES);
            case "h":
                return new TimeUnitPair(num, TimeUnit.HOURS);
            case "d":
            case "day":
                return new TimeUnitPair(num, TimeUnit.DAYS);
            default:
                throw new IllegalArgumentException();
        }
    }
    public static String formatFullTime(LocalDateTime localDateTime) {
        return formatFullTime(localDateTime, FULL_FORMAT);
    }
    public static String formatFullTime(LocalDateTime localDateTime, String pattern) {
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }
    public static String formatCSTTime(String date, String format) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US);
        Date parsed = sdf.parse(date);
        SimpleDateFormat output = new SimpleDateFormat(format);
        output.setTimeZone(sdf.getTimeZone());
        return output.format(parsed);
    }
    public static final class TimeUnitPair {

        public final int num;
        public final TimeUnit unit;
        public TimeUnitPair(int num, TimeUnit unit) {
            this.num = num;
            this.unit = unit;
        }
    }
}
