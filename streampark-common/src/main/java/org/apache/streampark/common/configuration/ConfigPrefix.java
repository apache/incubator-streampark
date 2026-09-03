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
import java.util.Objects;

/**
 * A validated configuration namespace prefix.
 *
 * <p>Using a value object instead of scattered string constants makes prefix removal explicit and
 * prevents accidental substring operations on unrelated keys.
 */
public final class ConfigPrefix implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String value;

    private ConfigPrefix(String value) {
        this.value = value;
    }

    /**
     * Creates a non-blank prefix value.
     *
     * @param value literal namespace prefix
     * @return validated prefix
     */
    public static ConfigPrefix of(String value) {
        String normalized = Objects.requireNonNull(value, "prefix must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("prefix must not be blank");
        }
        return new ConfigPrefix(normalized);
    }

    /**
     * Returns the literal prefix.
     *
     * @return prefix text
     */
    public String value() {
        return value;
    }

    /**
     * Returns whether the supplied key belongs to this namespace.
     *
     * @param key key to inspect; {@code null} never matches
     * @return whether the key starts with this prefix
     */
    public boolean matches(String key) {
        return key != null && key.startsWith(value);
    }

    /**
     * Removes this prefix from a configuration key.
     *
     * @param key key belonging to this namespace
     * @return suffix after the prefix
     * @throws ConfigException when the key does not start with this prefix
     */
    public String stripFrom(String key) {
        if (!matches(key)) {
            throw new ConfigException("Configuration key '" + key + "' does not start with '" + value + "'");
        }
        return key.substring(value.length());
    }

    @Override
    public boolean equals(Object object) {
        return object instanceof ConfigPrefix && value.equals(((ConfigPrefix) object).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
