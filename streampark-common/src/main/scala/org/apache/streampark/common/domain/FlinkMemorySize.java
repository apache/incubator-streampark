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

package org.apache.streampark.common.domain;

import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.BYTES;
import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.GIGA_BYTES;
import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.KILO_BYTES;
import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.MEGA_BYTES;
import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.TERA_BYTES;
import static org.apache.streampark.common.domain.FlinkMemorySize.MemoryUnit.hasUnit;

import org.apache.streampark.common.util.AssertUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

/**
 * MemorySize is a representation of a number of bytes, viewable in different units.
 *
 * <h2>Parsing</h2>
 *
 * <p>The size can be parsed from a text expression. If the expression is a pure number, the value
 * will be interpreted as bytes.
 */
public class FlinkMemorySize implements java.io.Serializable, Comparable<FlinkMemorySize> {

    private static final long serialVersionUID = 1L;

    public static final FlinkMemorySize ZERO = new FlinkMemorySize(0L);

    public static final FlinkMemorySize MAX_VALUE = new FlinkMemorySize(Long.MAX_VALUE);

    private static final List<MemoryUnit> ORDERED_UNITS = Arrays.asList(BYTES, KILO_BYTES, MEGA_BYTES, GIGA_BYTES, TERA_BYTES);

    // ------------------------------------------------------------------------

    /**
     * The memory size, in bytes.
     */
    private final long bytes;

    /**
     * The memorized value returned by toString().
     */
    private transient String stringified;

    /**
     * The memorized value returned by toHumanReadableString().
     */
    private transient String humanReadableStr;

    /**
     * Constructs a new MemorySize.
     *
     * @param bytes The size, in bytes. Must be zero or larger.
     */
    public FlinkMemorySize(long bytes) {
        AssertUtils.checkArgument(bytes >= 0, "bytes must be >= 0");
        this.bytes = bytes;
    }

    public static FlinkMemorySize ofMebiBytes(long mebiBytes) {
        return new FlinkMemorySize(mebiBytes << 20);
    }

    // ------------------------------------------------------------------------

    /**
     *
     * @return Gets the memory size in bytes.
     */
    public long getBytes() {
        return bytes;
    }

    /**
     *
     * @return Gets the memory size in Kibibytes (= 1024 bytes).
     */
    public long getKibiBytes() {
        return bytes >> 10;
    }

    /**
     *
     * @return Gets the memory size in Mebibytes (= 1024 Kibibytes).
     */
    public int getMebiBytes() {
        return (int) (bytes >> 20);
    }

    /**
     *
     * @return Gets the memory size in Gibibytes (= 1024 Mebibytes).
     */
    public long getGibiBytes() {
        return bytes >> 30;
    }

    /**
     * @return Gets the memory size in Tebibytes (= 1024 Gibibytes).
     */
    public long getTebiBytes() {
        return bytes >> 40;
    }

    // ------------------------------------------------------------------------

    @Override
    public int hashCode() {
        return (int) (bytes ^ (bytes >>> 32));
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this
            || (obj != null
            && obj.getClass() == this.getClass()
            && ((FlinkMemorySize) obj).bytes == this.bytes);
    }

    @Override
    public String toString() {
        if (stringified == null) {
            stringified = formatToString();
        }

        return stringified;
    }

    private String formatToString() {
        FlinkMemorySize.MemoryUnit highestIntegerUnit =
            IntStream.range(0, ORDERED_UNITS.size())
                .sequential()
                .filter(idx -> bytes % ORDERED_UNITS.get(idx).getMultiplier() != 0)
                .boxed()
                .findFirst()
                .map(
                    idx -> {
                        if (idx == 0) {
                            return ORDERED_UNITS.get(0);
                        } else {
                            return ORDERED_UNITS.get(idx - 1);
                        }
                    })
                .orElse(BYTES);

        return String.format(
            "%d %s",
            bytes / highestIntegerUnit.getMultiplier(), highestIntegerUnit.getUnits()[1]);
    }

    public String toHumanReadableString() {
        if (humanReadableStr == null) {
            humanReadableStr = formatToHumanReadableString();
        }

        return humanReadableStr;
    }

    private String formatToHumanReadableString() {
        FlinkMemorySize.MemoryUnit highestUnit =
            IntStream.range(0, ORDERED_UNITS.size())
                .sequential()
                .filter(idx -> bytes > ORDERED_UNITS.get(idx).getMultiplier())
                .boxed()
                .max(Comparator.naturalOrder())
                .map(ORDERED_UNITS::get)
                .orElse(BYTES);

        if (highestUnit == BYTES) {
            return String.format("%d %s", bytes, BYTES.getUnits()[1]);
        } else {
            double approximate = 1.0 * bytes / highestUnit.getMultiplier();
            return String.format(
                Locale.ROOT,
                "%.3f%s (%d bytes)",
                approximate,
                highestUnit.getUnits()[1],
                bytes);
        }
    }

    @Override
    public int compareTo(FlinkMemorySize that) {
        return Long.compare(this.bytes, that.bytes);
    }

    // ------------------------------------------------------------------------
    //  Calculations
    // ------------------------------------------------------------------------

    public FlinkMemorySize add(FlinkMemorySize that) {
        return new FlinkMemorySize(Math.addExact(this.bytes, that.bytes));
    }

    public FlinkMemorySize subtract(FlinkMemorySize that) {
        return new FlinkMemorySize(Math.subtractExact(this.bytes, that.bytes));
    }

    public FlinkMemorySize multiply(double multiplier) {
        AssertUtils.checkArgument(multiplier >= 0, "multiplier must be >= 0");

        BigDecimal product =
            BigDecimal.valueOf(this.bytes).multiply(BigDecimal.valueOf(multiplier));
        if (product.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new ArithmeticException("long overflow");
        }
        return new FlinkMemorySize(product.longValue());
    }

    public FlinkMemorySize divide(long by) {
        AssertUtils.checkArgument(by >= 0, "divisor must be >= 0");
        return new FlinkMemorySize(bytes / by);
    }

    // ------------------------------------------------------------------------
    //  Parsing
    // ------------------------------------------------------------------------

    /**
     * Parses the given string as as MemorySize.
     *
     * @param text The string to parse
     * @return The parsed MemorySize
     * @throws IllegalArgumentException Thrown, if the expression cannot be parsed.
     */
    public static FlinkMemorySize parse(String text) throws IllegalArgumentException {
        return new FlinkMemorySize(parseBytes(text));
    }

    /**
     * Parses the given string with a default unit.
     *
     * @param text        The string to parse.
     * @param defaultUnit specify the default unit.
     * @return The parsed MemorySize.
     * @throws IllegalArgumentException Thrown, if the expression cannot be parsed.
     */
    public static FlinkMemorySize parse(String text, FlinkMemorySize.MemoryUnit defaultUnit)
        throws IllegalArgumentException {
        if (!hasUnit(text)) {
            return parse(text + defaultUnit.getUnits()[0]);
        }

        return parse(text);
    }

    /**
     * Parses the given string as bytes. The supported expressions are listed under {@link
     * FlinkMemorySize}.
     *
     * @param text The string to parse
     * @return The parsed size, in bytes.
     * @throws IllegalArgumentException Thrown, if the expression cannot be parsed.
     */
    public static long parseBytes(String text) throws IllegalArgumentException {
        AssertUtils.checkNotNull(text, "text");

        final String trimmed = text.trim();
        AssertUtils.checkArgument(!trimmed.isEmpty(), "argument is an empty- or whitespace-only string");

        final int len = trimmed.length();
        int pos = 0;

        char current;
        while (pos < len && (current = trimmed.charAt(pos)) >= '0' && current <= '9') {
            pos++;
        }

        final String number = trimmed.substring(0, pos);
        final String unit = trimmed.substring(pos).trim().toLowerCase(Locale.US);

        if (number.isEmpty()) {
            throw new NumberFormatException("text does not start with a number");
        }

        final long value;
        try {
            // this throws a NumberFormatException on overflow
            value = Long.parseLong(number);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                "The value '"
                    + number
                    + "' cannot be re represented as 64bit number (numeric overflow).");
        }

        final long multiplier = parseUnit(unit).map(FlinkMemorySize.MemoryUnit::getMultiplier).orElse(1L);
        final long result = value * multiplier;

        // check for overflow
        if (result / multiplier != value) {
            throw new IllegalArgumentException(
                "The value '"
                    + text
                    + "' cannot be re represented as 64bit number of bytes (numeric overflow).");
        }

        return result;
    }

    private static Optional<MemoryUnit> parseUnit(String unit) {
        if (matchesAny(unit, BYTES)) {
            return Optional.of(BYTES);
        } else if (matchesAny(unit, KILO_BYTES)) {
            return Optional.of(KILO_BYTES);
        } else if (matchesAny(unit, MEGA_BYTES)) {
            return Optional.of(MEGA_BYTES);
        } else if (matchesAny(unit, GIGA_BYTES)) {
            return Optional.of(GIGA_BYTES);
        } else if (matchesAny(unit, TERA_BYTES)) {
            return Optional.of(TERA_BYTES);
        } else if (!unit.isEmpty()) {
            throw new IllegalArgumentException(
                "Memory size unit '"
                    + unit
                    + "' does not match any of the recognized units: "
                    + FlinkMemorySize.MemoryUnit.getAllUnits());
        }

        return Optional.empty();
    }

    private static boolean matchesAny(String str, FlinkMemorySize.MemoryUnit unit) {
        for (String s : unit.getUnits()) {
            if (s.equals(str)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Enum which defines memory unit, mostly used to parse value from configuration file.
     *
     * <p>To make larger values more compact, the common size suffixes are supported:
     *
     * <ul>
     *   <li>1b or 1bytes (bytes)
     *   <li>1k or 1kb or 1kibibytes (interpreted as kibibytes = 1024 bytes)
     *   <li>1m or 1mb or 1mebibytes (interpreted as mebibytes = 1024 kibibytes)
     *   <li>1g or 1gb or 1gibibytes (interpreted as gibibytes = 1024 mebibytes)
     *   <li>1t or 1tb or 1tebibytes (interpreted as tebibytes = 1024 gibibytes)
     * </ul>
     */
    public enum MemoryUnit {
        BYTES(new String[]{"b", "bytes"}, 1L),
        KILO_BYTES(new String[]{"k", "kb", "kibibytes"}, 1024L),
        MEGA_BYTES(new String[]{"m", "mb", "mebibytes"}, 1024L * 1024L),
        GIGA_BYTES(new String[]{"g", "gb", "gibibytes"}, 1024L * 1024L * 1024L),
        TERA_BYTES(new String[]{"t", "tb", "tebibytes"}, 1024L * 1024L * 1024L * 1024L);

        private final String[] units;

        private final long multiplier;

        MemoryUnit(String[] units, long multiplier) {
            this.units = units;
            this.multiplier = multiplier;
        }

        public String[] getUnits() {
            return units;
        }

        public long getMultiplier() {
            return multiplier;
        }

        public static String getAllUnits() {
            return concatenateUnits(
                BYTES.getUnits(),
                KILO_BYTES.getUnits(),
                MEGA_BYTES.getUnits(),
                GIGA_BYTES.getUnits(),
                TERA_BYTES.getUnits());
        }

        public static boolean hasUnit(String text) {
            AssertUtils.checkNotNull(text, "text");

            final String trimmed = text.trim();
            AssertUtils.checkArgument(!trimmed.isEmpty(), "argument is an empty- or whitespace-only string");

            final int len = trimmed.length();
            int pos = 0;

            char current;
            while (pos < len && (current = trimmed.charAt(pos)) >= '0' && current <= '9') {
                pos++;
            }

            final String unit = trimmed.substring(pos).trim().toLowerCase(Locale.US);

            return unit.length() > 0;
        }

        private static String concatenateUnits(final String[]... allUnits) {
            final StringBuilder builder = new StringBuilder(128);

            for (String[] units : allUnits) {
                builder.append('(');

                for (String unit : units) {
                    builder.append(unit);
                    builder.append(" | ");
                }

                builder.setLength(builder.length() - 3);
                builder.append(") / ");
            }

            builder.setLength(builder.length() - 3);
            return builder.toString();
        }
    }
}
