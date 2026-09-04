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

package org.apache.streampark.common.configuration;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Immutable binary data size.
 *
 * <p>Unit suffixes are case-insensitive. {@code k}, {@code m}, {@code g}, and {@code t}, including
 * their {@code b} and {@code ib} forms, use powers of 1024. Negative sizes and arithmetic overflow
 * are rejected.
 */
public final class DataSize implements Comparable<DataSize>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern SIZE_PATTERN = Pattern.compile("^([+-]?\\d+)\\s*([a-zA-Z]*)$");

    private final long bytes;

    private DataSize(long bytes) {
        this.bytes = bytes;
    }

    /**
     * Creates a data size from bytes.
     *
     * @param bytes non-negative byte count
     * @return immutable data size
     * @throws IllegalArgumentException when {@code bytes} is negative
     */
    public static DataSize ofBytes(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("data size must not be negative");
        }
        return new DataSize(bytes);
    }

    /**
     * Creates a data size from mebibytes.
     *
     * @param mebiBytes non-negative mebibyte count
     * @return immutable data size
     * @throws ArithmeticException when conversion overflows a {@code long}
     */
    public static DataSize ofMebiBytes(long mebiBytes) {
        return ofBytes(Math.multiplyExact(mebiBytes, 1024L * 1024L));
    }

    /**
     * Parses a non-negative binary data size.
     *
     * <p>Bare numbers represent bytes. The suffixes {@code k}, {@code m}, {@code g}, and {@code t},
     * with optional {@code b} or {@code ib}, use binary multiples.
     *
     * @param value textual data size
     * @return parsed data size
     * @throws IllegalArgumentException when the value is negative, malformed, or uses an unknown
     *     unit
     * @throws ArithmeticException when conversion overflows a {@code long}
     */
    public static DataSize parse(String value) {
        String text = Objects.requireNonNull(value, "data size must not be null").trim();
        Matcher matcher = SIZE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("invalid data size: " + value);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2).toLowerCase(Locale.ROOT);
        long multiplier;
        switch (unit) {
            case "":
            case "b":
                multiplier = 1L;
                break;
            case "k":
            case "kb":
            case "kib":
                multiplier = 1024L;
                break;
            case "m":
            case "mb":
            case "mib":
                multiplier = 1024L * 1024L;
                break;
            case "g":
            case "gb":
            case "gib":
                multiplier = 1024L * 1024L * 1024L;
                break;
            case "t":
            case "tb":
            case "tib":
                multiplier = 1024L * 1024L * 1024L * 1024L;
                break;
            default:
                throw new IllegalArgumentException("unsupported data size unit: " + unit);
        }
        return ofBytes(Math.multiplyExact(amount, multiplier));
    }

    /**
     * Returns the exact number of bytes.
     *
     * @return byte count
     */
    public long bytes() {
        return bytes;
    }

    @Override
    public int compareTo(DataSize other) {
        return Long.compare(bytes, other.bytes);
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof DataSize && bytes == ((DataSize) object).bytes;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bytes);
    }

    @Override
    public String toString() {
        return bytes + "b";
    }
}
