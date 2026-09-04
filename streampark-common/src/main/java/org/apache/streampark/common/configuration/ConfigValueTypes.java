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

import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in strict converters used by {@link ConfigOptions}.
 *
 * <p>Converters accept values produced by YAML, HOCON, properties, and command-line parsers while
 * rejecting ambiguous coercions. In particular, booleans accept only {@code true}/{@code false},
 * integral types reject fractional numbers, and durations reject negative values. Keeping these
 * rules in one package-private catalog makes option conversion consistent across modules.
 */
final class ConfigValueTypes {

    private static final Pattern DURATION_PATTERN = Pattern.compile("^([+-]?\\d+)\\s*([a-zA-Z]*)$");

    static final ConfigValueType<String> STRING =
        simple(String.class, value -> value instanceof String ? (String) value : value.toString(), value -> value);

    static final ConfigValueType<Integer> INTEGER =
        simple(Integer.class, ConfigValueTypes::toInteger, Object::toString);

    static final ConfigValueType<Long> LONG =
        simple(Long.class, ConfigValueTypes::toLong, Object::toString);

    static final ConfigValueType<Boolean> BOOLEAN =
        simple(Boolean.class, ConfigValueTypes::toBoolean, Object::toString);

    static final ConfigValueType<DataSize> DATA_SIZE =
        simple(
            DataSize.class,
            value -> value instanceof DataSize ? (DataSize) value : DataSize.parse(value.toString()),
            DataSize::toString);

    @SuppressWarnings("unchecked")
    static final ConfigValueType<List<String>> STRING_LIST =
        simple(
            (Class<List<String>>) (Class<?>) List.class,
            ConfigValueTypes::toStringList,
            values -> String.join(",", values));

    private ConfigValueTypes() {
    }

    static ConfigValueType<Duration> duration(ChronoUnit unitForBareNumbers) {
        Objects.requireNonNull(unitForBareNumbers, "duration unit must not be null");
        return simple(
            Duration.class,
            value -> toDuration(value, unitForBareNumbers),
            Duration::toString);
    }

    static <E extends Enum<E>> ConfigValueType<E> enumType(Class<E> enumClass) {
        Objects.requireNonNull(enumClass, "enum class must not be null");
        return simple(
            enumClass,
            value -> {
                if (enumClass.isInstance(value)) {
                    return enumClass.cast(value);
                }
                String text = value.toString().trim();
                for (E constant : enumClass.getEnumConstants()) {
                    if (constant.name().equalsIgnoreCase(text)) {
                        return constant;
                    }
                }
                throw new IllegalArgumentException(
                    "expected one of " + java.util.Arrays.toString(enumClass.getEnumConstants()));
            },
            Enum::name);
    }

    static String formatUnknown(Object value) {
        if (value instanceof Collection) {
            List<String> values = new ArrayList<>();
            for (Object element : (Collection<?>) value) {
                values.add(String.valueOf(element));
            }
            return String.join(",", values);
        }
        return String.valueOf(value);
    }

    private static Integer toInteger(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return Math.toIntExact(toLong(value));
        }
        return Integer.valueOf(value.toString().trim());
    }

    private static Long toLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            Number number = (Number) value;
            long longValue = number.longValue();
            // Number#longValue silently truncates floating-point input. Comparing the round-trip
            // value rejects that lossy coercion before it reaches an integral option.
            if (Double.compare(number.doubleValue(), longValue) != 0) {
                throw new IllegalArgumentException("expected an integral number");
            }
            return longValue;
        }
        return Long.valueOf(value.toString().trim());
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = value.toString().trim();
        if ("true".equalsIgnoreCase(text)) {
            return true;
        }
        if ("false".equalsIgnoreCase(text)) {
            return false;
        }
        throw new IllegalArgumentException("expected 'true' or 'false'");
    }

    private static Duration toDuration(Object value, ChronoUnit unitForBareNumbers) {
        if (value instanceof Duration) {
            return requireNonNegative((Duration) value);
        }
        if (value instanceof Number) {
            return durationOf(toLong(value), unitForBareNumbers);
        }
        String text = value.toString().trim();
        try {
            // ISO-8601 remains the canonical representation; compact suffixes support existing
            // StreamPark configuration files and command-line values.
            return requireNonNegative(Duration.parse(text));
        } catch (DateTimeParseException ignored) {
            Matcher matcher = DURATION_PATTERN.matcher(text);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("invalid duration: " + value);
            }
            long amount = Long.parseLong(matcher.group(1));
            String suffix = matcher.group(2).toLowerCase(Locale.ROOT);
            ChronoUnit unit;
            switch (suffix) {
                case "":
                    unit = unitForBareNumbers;
                    break;
                case "ns":
                    unit = ChronoUnit.NANOS;
                    break;
                case "us":
                    unit = ChronoUnit.MICROS;
                    break;
                case "ms":
                    unit = ChronoUnit.MILLIS;
                    break;
                case "s":
                    unit = ChronoUnit.SECONDS;
                    break;
                case "m":
                case "min":
                    unit = ChronoUnit.MINUTES;
                    break;
                case "h":
                    unit = ChronoUnit.HOURS;
                    break;
                case "d":
                    unit = ChronoUnit.DAYS;
                    break;
                default:
                    throw new IllegalArgumentException("unsupported duration unit: " + suffix);
            }
            return durationOf(amount, unit);
        }
    }

    private static Duration durationOf(long value, ChronoUnit unit) {
        return requireNonNegative(Duration.of(value, unit));
    }

    private static Duration requireNonNegative(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative");
        }
        return duration;
    }

    private static List<String> toStringList(Object value) {
        if (value instanceof Collection) {
            List<String> result = new ArrayList<>();
            for (Object element : (Collection<?>) value) {
                result.add(String.valueOf(element));
            }
            return Collections.unmodifiableList(result);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = text.split(",", -1);
        List<String> result = new ArrayList<>(parts.length);
        for (String part : parts) {
            result.add(part.trim());
        }
        return Collections.unmodifiableList(result);
    }

    private static <T> ConfigValueType<T> simple(
                                                 Class<T> valueClass,
                                                 Function<Object, T> converter,
                                                 Function<T, String> formatter) {
        return new ConfigValueType<T>() {

            @Override
            public Class<T> valueClass() {
                return valueClass;
            }

            @Override
            public T convert(Object value) {
                return converter.apply(Objects.requireNonNull(value, "configuration value must not be null"));
            }

            @Override
            public String format(T value) {
                return formatter.apply(value);
            }
        };
    }
}
