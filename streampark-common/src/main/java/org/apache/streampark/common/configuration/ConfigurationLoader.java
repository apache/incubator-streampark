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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Composes named configuration layers according to the fixed StreamPark precedence model.
 *
 * <p>Layers may be registered in any order because {@link Configuration.Builder} applies source
 * precedence as each value is added. Registered maps are copied immediately so callers cannot
 * mutate a pending load.
 */
public final class ConfigurationLoader {

    private final Configuration.Builder builder = Configuration.builder();

    /**
     * Registers a named, defensively copied source layer.
     *
     * @param source source category used for precedence
     * @param name non-blank source name retained in value origins
     * @param values raw configuration values
     * @return this loader
     */
    public ConfigurationLoader add(ConfigSource source, String name, Map<String, ?> values) {
        builder.add(name, source, values);
        return this;
    }

    /**
     * Captures the current JVM system properties as one system-property layer.
     *
     * <p>Properties are copied when this method is called. Subsequent changes to {@link
     * System#getProperties()} do not alter the registered layer.
     *
     * @return this loader
     */
    public ConfigurationLoader addSystemProperties() {
        Properties properties = System.getProperties();
        Map<String, Object> values = new LinkedHashMap<>();
        properties.forEach((key, value) -> values.put(String.valueOf(key), value));
        return add(ConfigSource.SYSTEM_PROPERTIES, "JVM system properties", values);
    }

    /**
     * Resolves every registered layer into one immutable configuration snapshot.
     *
     * @return immutable configuration snapshot
     */
    public Configuration load() {
        return builder.build();
    }
}
