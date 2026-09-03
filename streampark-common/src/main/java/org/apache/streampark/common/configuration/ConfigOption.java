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
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Immutable metadata for a typed configuration value.
 *
 * <p>An option never reads global state. Values are resolved by a {@link ReadableConfig}, which
 * keeps option declaration separate from configuration loading and precedence.
 *
 * @param <T> value type
 */
public final class ConfigOption<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String key;
    private final ConfigValueType<T> valueType;
    private final T defaultValue;
    private final boolean hasDefaultValue;
    private final String description;
    private final List<String> fallbackKeys;
    private final boolean sensitive;
    private final Predicate<T> validator;
    private final String validationMessage;

    public ConfigOption(String key,
                        ConfigValueType<T> valueType,
                        T defaultValue,
                        boolean hasDefaultValue,
                        String description,
                        List<String> fallbackKeys,
                        boolean sensitive,
                        Predicate<T> validator,
                        String validationMessage) {
        this.key = requireKey(key);
        this.valueType = Objects.requireNonNull(valueType, "valueType must not be null");
        this.hasDefaultValue = hasDefaultValue;
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.fallbackKeys = List.copyOf(Objects.requireNonNull(fallbackKeys, "fallbackKeys must not be null"));
        this.sensitive = sensitive;
        this.validator = Objects.requireNonNull(validator, "validator must not be null");
        this.validationMessage =
            Objects.requireNonNull(validationMessage, "validationMessage must not be null");
        if (hasDefaultValue) {
            this.defaultValue = Configuration.immutableValue(defaultValue);
            validate(this.defaultValue);
        } else {
            this.defaultValue = null;
        }
    }

    /**
     * Returns the canonical configuration key.
     *
     * @return canonical key
     */
    public String key() {
        return key;
    }

    /**
     * Returns whether this option declares a default value.
     *
     * @return whether a default exists
     */
    public boolean hasDefaultValue() {
        return hasDefaultValue;
    }

    /**
     * Returns the declared default value.
     *
     * @return declared default
     * @throws ConfigException when the option has no default
     */
    public T defaultValue() {
        if (!hasDefaultValue) {
            throw new ConfigException("Configuration option '" + key + "' has no default value");
        }
        return defaultValue;
    }

    /**
     * Returns the human-readable option description.
     *
     * @return option description
     */
    public String description() {
        return description;
    }

    /**
     * Returns legacy keys considered after the canonical key.
     *
     * @return immutable fallback-key list in resolution order
     */
    public List<String> fallbackKeys() {
        return fallbackKeys;
    }

    /**
     * Returns whether conversion errors for this option must redact the supplied value.
     *
     * @return whether this option contains sensitive material
     */
    public boolean sensitive() {
        return sensitive;
    }

    Class<T> valueClass() {
        return valueType.valueClass();
    }

    T convert(Object value) {
        T converted = valueType.convert(value);
        validate(converted);
        return converted;
    }

    String format(T value) {
        validate(value);
        return valueType.format(value);
    }

    void validate(T value) {
        if (value == null) {
            throw new ConfigException("Configuration option '" + key + "' must not be null");
        }
        if (!validator.test(value)) {
            throw new ConfigException(
                "Invalid value for configuration option '" + key + "': " + validationMessage);
        }
    }

    private static String requireKey(String key) {
        String normalized = Objects.requireNonNull(key, "key must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        return normalized;
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ConfigOption && key.equals(((ConfigOption<?>) object).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return "ConfigOption{" + "key='" + key + '\'' + ", type=" + valueClass().getSimpleName() + '}';
    }
}
