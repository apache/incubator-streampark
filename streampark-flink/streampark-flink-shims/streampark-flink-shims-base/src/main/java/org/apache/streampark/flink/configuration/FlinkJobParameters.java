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

package org.apache.streampark.flink.configuration;

import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.configuration.ConfigOption;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.ReadableConfig;

import org.apache.flink.api.common.ExecutionConfig;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Thin Flink adapter over StreamPark's immutable application configuration.
 *
 * <p>Flink requires global job parameters to extend {@link
 * ExecutionConfig.GlobalJobParameters}. All parsing, precedence, conversion, and validation remain
 * in the common configuration subsystem; this class only bridges that snapshot into Flink.
 */
public final class FlinkJobParameters extends ExecutionConfig.GlobalJobParameters
    implements
        ReadableConfig {

    private static final long serialVersionUID = 1L;

    private final Configuration configuration;

    private FlinkJobParameters(Configuration configuration) {
        this.configuration =
            Objects.requireNonNull(configuration, "configuration must not be null");
    }

    /**
     * Creates a Flink parameter adapter for an immutable application snapshot.
     *
     * @param configuration application configuration snapshot
     * @return Flink global-job-parameter adapter
     */
    public static FlinkJobParameters of(Configuration configuration) {
        return new FlinkJobParameters(configuration);
    }

    /**
     * Returns the underlying immutable application configuration.
     *
     * @return application configuration snapshot
     */
    public Configuration configuration() {
        return configuration;
    }

    @Override
    public <T> T get(ConfigOption<T> option) {
        return configuration.get(option);
    }

    @Override
    public <T> Optional<T> getOptional(ConfigOption<T> option) {
        return configuration.getOptional(option);
    }

    @Override
    public boolean contains(ConfigOption<?> option) {
        return configuration.contains(option);
    }

    /**
     * Returns a required parameter by its exact key.
     *
     * <p>This compatibility accessor does not apply option defaults or fallback keys. New code
     * should prefer {@link #get(ConfigOption)} when typed metadata is available.
     *
     * @param key exact application parameter key
     * @return string representation of the configured value
     * @throws ConfigException when the key is absent
     */
    public String get(String key) {
        return configuration.getOptionalString(key)
            .orElseThrow(() -> new ConfigException("Required application parameter '" + key + "' is missing"));
    }

    /**
     * Returns a parameter by its exact key or a caller-supplied default value.
     *
     * @param key exact application parameter key
     * @param defaultValue value returned when the key is absent
     * @return configured or default value
     */
    public String get(String key, String defaultValue) {
        return configuration.getOptionalString(key).orElse(defaultValue);
    }

    /**
     * Returns whether an exact parameter key is present.
     *
     * @param key exact application parameter key
     * @return whether the key is present
     */
    public boolean has(String key) {
        return configuration.containsKey(key);
    }

    @Override
    public Map<String, String> toMap() {
        return configuration.toMap();
    }

    @Override
    public String toString() {
        return configuration.toString();
    }
}
