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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Entry point for staged, type-safe {@link ConfigOption} declarations.
 *
 * <p>The staged API requires an option type before defaults, validation, fallback keys, and
 * documentation can be declared. Calling {@code build()} produces immutable metadata and performs
 * validation of the declared default.
 */
public final class ConfigOptions {

    private ConfigOptions() {
    }

    /**
     * Starts an option declaration for a canonical key.
     *
     * @param key non-blank canonical configuration key
     * @return builder that requires a value type before option metadata can be completed
     */
    public static OptionBuilder key(String key) {
        return new OptionBuilder(key);
    }

    /**
     * First stage of an option declaration, used to select its value type.
     *
     * <p>Separating this stage prevents defaults and validators from being declared before Java
     * knows the option's value type.
     */
    public static final class OptionBuilder {

        private final String key;

        private OptionBuilder(String key) {
            this.key = key;
        }

        /**
         * Declares a string option.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<String> stringType() {
            return typed(ConfigValueTypes.STRING);
        }

        /**
         * Declares a 32-bit integer option.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<Integer> intType() {
            return typed(ConfigValueTypes.INTEGER);
        }

        /**
         * Declares a 64-bit integer option.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<Long> longType() {
            return typed(ConfigValueTypes.LONG);
        }

        /**
         * Declares a strict boolean option.
         *
         * <p>Only boolean values and case-insensitive {@code true}/{@code false} strings are
         * accepted.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<Boolean> booleanType() {
            return typed(ConfigValueTypes.BOOLEAN);
        }

        /**
         * Declares a duration option whose bare numbers represent milliseconds.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<Duration> durationType() {
            return durationType(ChronoUnit.MILLIS);
        }

        /**
         * Declares a duration option with an explicit unit for bare numeric values.
         *
         * @param unitForBareNumbers unit assigned to values without a suffix
         * @return typed metadata builder
         */
        public TypedOptionBuilder<Duration> durationType(ChronoUnit unitForBareNumbers) {
            return typed(ConfigValueTypes.duration(unitForBareNumbers));
        }

        /**
         * Declares a binary data-size option.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<DataSize> dataSizeType() {
            return typed(ConfigValueTypes.DATA_SIZE);
        }

        /**
         * Declares a case-insensitive enum option.
         *
         * @param enumClass enum class defining accepted constants
         * @param <E> enum value type
         * @return typed metadata builder
         */
        public <E extends Enum<E>> TypedOptionBuilder<E> enumType(Class<E> enumClass) {
            return typed(ConfigValueTypes.enumType(enumClass));
        }

        /**
         * Declares a comma-separated or structured string-list option.
         *
         * @return typed metadata builder
         */
        public TypedOptionBuilder<List<String>> stringListType() {
            return typed(ConfigValueTypes.STRING_LIST);
        }

        private <T> TypedOptionBuilder<T> typed(ConfigValueType<T> valueType) {
            return new TypedOptionBuilder<>(key, valueType);
        }
    }

    /**
     * Second stage of an option declaration, used to complete immutable metadata.
     *
     * <p>Builder instances are mutable and intended for a single declaration. The resulting
     * {@link ConfigOption} defensively copies fallback keys and validates its default value.
     *
     * @param <T> option value type
     */
    public static final class TypedOptionBuilder<T> {

        private final String key;
        private final ConfigValueType<T> valueType;
        private T defaultValue;
        private boolean hasDefaultValue;
        private String description = "";
        private List<String> fallbackKeys = Collections.emptyList();
        private boolean sensitive;
        private Predicate<T> validator = ignored -> true;
        private String validationMessage = "value does not satisfy the option constraint";

        private TypedOptionBuilder(String key, ConfigValueType<T> valueType) {
            this.key = key;
            this.valueType = valueType;
        }

        /**
         * Declares a default value.
         *
         * <p>The value is validated when {@link #build()} constructs the option.
         *
         * @param value non-null default value
         * @return this builder
         */
        public TypedOptionBuilder<T> defaultValue(T value) {
            this.defaultValue = Objects.requireNonNull(value, "default value must not be null");
            this.hasDefaultValue = true;
            return this;
        }

        /**
         * Declares that callers must explicitly provide the option.
         *
         * @return this builder
         */
        public TypedOptionBuilder<T> noDefaultValue() {
            this.defaultValue = null;
            this.hasDefaultValue = false;
            return this;
        }

        /**
         * Sets the public option description.
         *
         * @param description human-readable description
         * @return this builder
         */
        public TypedOptionBuilder<T> withDescription(String description) {
            this.description = Objects.requireNonNull(description, "description must not be null");
            return this;
        }

        /**
         * Adds legacy fallback keys in resolution order.
         *
         * <p>The canonical key always wins. Fallbacks provide a migration path and do not create
         * aliases in an effective configuration snapshot.
         *
         * @param fallbackKeys legacy keys considered after the canonical key
         * @return this builder
         */
        public TypedOptionBuilder<T> withFallbackKeys(String... fallbackKeys) {
            this.fallbackKeys = Arrays.asList(fallbackKeys.clone());
            return this;
        }

        /**
         * Marks conversion failures as sensitive so their raw value is redacted.
         *
         * @return this builder
         */
        public TypedOptionBuilder<T> sensitive() {
            this.sensitive = true;
            return this;
        }

        /**
         * Adds a semantic value constraint evaluated after type conversion.
         *
         * @param validator predicate that accepts valid converted values
         * @param validationMessage diagnostic used when the predicate rejects a value
         * @return this builder
         */
        public TypedOptionBuilder<T> check(Predicate<T> validator, String validationMessage) {
            this.validator = Objects.requireNonNull(validator, "validator must not be null");
            this.validationMessage =
                Objects.requireNonNull(validationMessage, "validationMessage must not be null");
            return this;
        }

        /**
         * Builds immutable option metadata and validates its declared default.
         *
         * @return completed option metadata
         * @throws ConfigException when the declared default violates the option constraint
         * @throws IllegalArgumentException when a fallback key is blank
         */
        public ConfigOption<T> build() {
            List<String> keys = new ArrayList<>(fallbackKeys.size());
            for (String fallbackKey : fallbackKeys) {
                String keyValue = Objects.requireNonNull(fallbackKey, "fallback key must not be null").trim();
                if (keyValue.isEmpty()) {
                    throw new IllegalArgumentException("fallback key must not be blank");
                }
                keys.add(keyValue);
            }
            return new ConfigOption<>(
                key,
                valueType,
                defaultValue,
                hasDefaultValue,
                description,
                keys,
                sensitive,
                validator,
                validationMessage);
        }
    }
}
