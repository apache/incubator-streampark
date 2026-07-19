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

import org.apache.streampark.common.util.TypeCastUtils;

import java.util.Properties;
import java.util.function.Function;

/**
 * @param <T> type of the configuration value
 */
public final class ConfigOption<T> {

    private final String key;
    private final T defaultValue;
    private final boolean required;
    private final Class<T> classType;
    private final String description;
    private final Function<String, T> handle;
    private final String prefix;
    private final Properties prop;
    private final String fullKey;

    private ConfigOption(Builder<T> b) {
        this.key = b.key;
        this.defaultValue = b.defaultValue;
        this.required = b.required;
        this.classType = b.classType;
        this.description = b.description != null ? b.description : "";
        this.handle = b.handle;
        this.prefix = b.prefix != null ? b.prefix : "";
        this.prop = b.prop;
        this.fullKey = (!this.prefix.isEmpty()) ? this.prefix + "." + this.key : this.key;
    }

    public T get() {
        if (handle != null) {
            if (required) {
                try {
                    return handle.apply(fullKey);
                } catch (Exception e) {
                    throw error(e.getMessage());
                }
            } else {
                try {
                    return handle.apply(fullKey);
                } catch (Exception e) {
                    return defaultValue;
                }
            }
        } else {
            if (required) {
                Object v = prop.get(fullKey);
                if (v == null)
                    throw error("Is require");
                return TypeCastUtils.cast(v.toString(), classType);
            } else {
                String v = prop.getProperty(fullKey);
                if (v == null)
                    return defaultValue;
                return TypeCastUtils.cast(v, classType);
            }
        }
    }

    public IllegalArgumentException error(String message) {
        return new IllegalArgumentException(
            "[StreamPark] config error: key:" + fullKey + ", detail: " + message);
    }

    public static <T> Builder<T> builder(Class<T> classType) {
        return new Builder<>(classType);
    }

    public static final class Builder<T> {

        private final Class<T> classType;
        private String key;
        private T defaultValue;
        private boolean required = false;
        private String description;
        private Function<String, T> handle;
        private String prefix;
        private Properties prop;

        private Builder(Class<T> classType) {
            this.classType = classType;
        }

        public Builder<T> key(String key) {
            this.key = key;
            return this;
        }

        public Builder<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public Builder<T> required(boolean required) {
            this.required = required;
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

        public Builder<T> prop(Properties prop) {
            this.prop = prop;
            return this;
        }

        public ConfigOption<T> build() {
            return new ConfigOption<>(this);
        }
    }
}
