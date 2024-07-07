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

package org.apache.streampark.gateway;

import org.apache.streampark.common.util.AssertUtils;

import java.time.Duration;
import java.util.Map;

/** @param <T> */
public class ConfigOption<T> {

    private static final String EMPTY_DESCRIPTION = "";

    // ------------------------------------------------------------------------

    /** The current key for that config option. */
    private final String key;

    /** The default value for this option. */
    private final T defaultValue;

    /** The description for this option. */
    private final String description;

    /**
     * Type of the value that this ConfigOption describes.
     *
     * <ul>
     *   <li>typeClass == atomic class (e.g. {@code Integer.class}) -> {@code ConfigOption<Integer>}
     *   <li>typeClass == {@code Map.class} -> {@code ConfigOption<Map<String, String>>}
     *   <li>typeClass == atomic class and isList == true for {@code ConfigOption<List<Integer>>}
     * </ul>
     */
    private final Class<?> clazz;

    /**
     * Creates a new config option.
     *
     * @param key The current key for that config option
     * @param defaultValue The default value for this option
     * @param description Description for that option
     * @param clazz describes type of the ConfigOption, see description of the clazz field
     */
    private ConfigOption(String key, T defaultValue, String description, Class<?> clazz) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.description = description;
        this.clazz = clazz;
    }

    /**
     * Creates a new config option, using this option's key and default value, and adding the given
     * description. The given description is used when generation the configuration documentation.
     *
     * @param description The description for this option.
     * @return A new config option, with given description.
     */
    public ConfigOption<T> withDescription(final String description) {
        return new ConfigOption<>(key, defaultValue, description, clazz);
    }

    /**
     * Starts building a new {@link ConfigOption}.
     *
     * @param key The key for the config option.
     * @return The builder for the config option with the given key.
     */
    public static OptionBuilder key(String key) {
        AssertUtils.notNull(key);
        return new OptionBuilder(key);
    }

    public String getKey() {
        return key;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    /**
     * The option builder is used to create a {@link ConfigOption}. It is instantiated via {@link
     * ConfigOption#key(String)}.
     */
    public static final class OptionBuilder {

        /**
         * Workaround to reuse the {@link TypedConfigOptionBuilder} for a {@link Map Map&lt;String,
         * String&gt;}.
         */
        @SuppressWarnings("unchecked")
        private static final Class<Map<String, String>> PROPERTIES_MAP_CLASS =
                (Class<Map<String, String>>) (Class<?>) Map.class;

        /** The key for the config option. */
        private final String key;

        /**
         * Creates a new OptionBuilder.
         *
         * @param key The key for the config option
         */
        OptionBuilder(String key) {
            this.key = key;
        }

        /** Defines that the value of the option should be of {@link Boolean} type. */
        public TypedConfigOptionBuilder<Boolean> booleanType() {
            return new TypedConfigOptionBuilder<>(key, Boolean.class);
        }

        /** Defines that the value of the option should be of {@link Integer} type. */
        public TypedConfigOptionBuilder<Integer> intType() {
            return new TypedConfigOptionBuilder<>(key, Integer.class);
        }

        /** Defines that the value of the option should be of {@link Long} type. */
        public TypedConfigOptionBuilder<Long> longType() {
            return new TypedConfigOptionBuilder<>(key, Long.class);
        }

        /** Defines that the value of the option should be of {@link Float} type. */
        public TypedConfigOptionBuilder<Float> floatType() {
            return new TypedConfigOptionBuilder<>(key, Float.class);
        }

        /** Defines that the value of the option should be of {@link Double} type. */
        public TypedConfigOptionBuilder<Double> doubleType() {
            return new TypedConfigOptionBuilder<>(key, Double.class);
        }

        /** Defines that the value of the option should be of {@link String} type. */
        public TypedConfigOptionBuilder<String> stringType() {
            return new TypedConfigOptionBuilder<>(key, String.class);
        }

        /** Defines that the value of the option should be of {@link Duration} type. */
        public TypedConfigOptionBuilder<Duration> durationType() {
            return new TypedConfigOptionBuilder<>(key, Duration.class);
        }

        /**
         * Defines that the value of the option should be of {@link Enum} type.
         *
         * @param enumClass Concrete type of the expected enum.
         */
        public <T extends Enum<T>> TypedConfigOptionBuilder<T> enumType(Class<T> enumClass) {
            return new TypedConfigOptionBuilder<>(key, enumClass);
        }

        /**
         * Defines that the value of the option should be a set of properties, which can be represented
         * as {@code Map<String, String>}.
         */
        public TypedConfigOptionBuilder<Map<String, String>> mapType() {
            return new TypedConfigOptionBuilder<>(key, PROPERTIES_MAP_CLASS);
        }

        /**
         * Creates a ConfigOption with the given default value.
         *
         * <p>This method does not accept "null". For options with no default value, choose one of the
         * {@code noDefaultValue} methods.
         *
         * @param value The default value for the config option
         * @param <T> The type of the default value.
         * @return The config option with the default value.
         * @deprecated define the type explicitly first with one of the intType(), stringType(), etc.
         */
        @Deprecated
        public <T> ConfigOption<T> defaultValue(T value) {
            AssertUtils.notNull(value);
            return new ConfigOption<>(key, value, ConfigOption.EMPTY_DESCRIPTION, value.getClass());
        }

        /**
         * Creates a string-valued option with no default value. String-valued options are the only ones
         * that can have no default value.
         *
         * @return The created ConfigOption.
         * @deprecated define the type explicitly first with one of the intType(), stringType(), etc.
         */
        @Deprecated
        public ConfigOption<String> noDefaultValue() {
            return new ConfigOption<>(key, null, ConfigOption.EMPTY_DESCRIPTION, String.class);
        }
    }

    /**
     * Builder for {@link ConfigOption} with a defined atomic type.
     *
     * @param <T> atomic type of the option
     */
    public static class TypedConfigOptionBuilder<T> {

        private final String key;
        private final Class<T> clazz;

        TypedConfigOptionBuilder(String key, Class<T> clazz) {
            this.key = key;
            this.clazz = clazz;
        }

        /**
         * Creates a ConfigOption with the given default value.
         *
         * @param value The default value for the config option
         * @return The config option with the default value.
         */
        public ConfigOption<T> defaultValue(T value) {
            return new ConfigOption<>(key, value, ConfigOption.EMPTY_DESCRIPTION, clazz);
        }

        /**
         * Creates a ConfigOption without a default value.
         *
         * @return The config option without a default value.
         */
        public ConfigOption<T> noDefaultValue() {
            return new ConfigOption<>(key, null, ConfigOption.EMPTY_DESCRIPTION, clazz);
        }
    }
}
