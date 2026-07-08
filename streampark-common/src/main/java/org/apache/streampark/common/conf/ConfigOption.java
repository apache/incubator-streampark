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

package org.apache.streampark.common.conf;

import org.apache.streampark.common.util.StringCastUtils;

import java.util.Properties;
import java.util.function.Function;

/**
 * Configuration option with typed value retrieval.
 *
 * @param <T> value type
 */
public class ConfigOption<T> {

    private final String key;
    private final T defaultValue;
    private final boolean required;
    private final Class<?> classType;
    private final String description;
    private final Function<String, T> handle;
    private final String prefix;
    private final Properties prop;

    public ConfigOption(
                        String key,
                        T defaultValue,
                        boolean required,
                        Class<?> classType,
                        String description,
                        Function<String, T> handle,
                        String prefix,
                        Properties prop) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.required = required;
        this.classType = classType;
        this.description = description;
        this.handle = handle;
        this.prefix = prefix != null ? prefix : "";
        this.prop = prop != null ? prop : new Properties();
    }

    public static <T> Builder<T> builder(String key) {
        return new Builder<>(key);
    }

    private String fullKey() {
        if (prefix != null && !prefix.isEmpty()) {
            return prefix + "." + key;
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    public T get() {
        String fk = fullKey();
        if (handle == null) {
            if (required) {
                Object v = prop.get(fk);
                if (v == null) {
                    throw error("Is require");
                }
                return StringCastUtils.cast(v.toString(), classType);
            } else {
                String v = prop.getProperty(fk);
                if (v == null) {
                    return defaultValue;
                }
                return StringCastUtils.cast(v, classType);
            }
        } else {
            try {
                return handle.apply(fk);
            } catch (Exception e) {
                if (required) {
                    throw error(e.getMessage());
                }
                return defaultValue;
            }
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException(
            "[StreamPark] config error: key:" + fullKey() + ", detail: " + message);
    }

    public static class Builder<T> {

        private final String key;
        private T defaultValue;
        private boolean required;
        private Class<?> classType;
        private String description = "";
        private Function<String, T> handle;
        private String prefix = "";
        private Properties prop = new Properties();

        Builder(String key) {
            this.key = key;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> required(boolean required) {
            this.required = required;
            return this;
        }

        public Builder<T> classType(Class<?> classType) {
            this.classType = classType;
            return this;
        }

        public Builder<T> description(String description) {
            this.description = description;
            return this;
        }

        public Builder<T> handle(Function<String, T> handle) {
            this.handle = handle;
            return this;
        }

        public Builder<T> prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder<T> properties(Properties prop) {
            this.prop = prop;
            return this;
        }

        public ConfigOption<T> build() {
            return new ConfigOption<>(key, defaultValue, required, classType, description, handle, prefix, prop);
        }
    }
}
