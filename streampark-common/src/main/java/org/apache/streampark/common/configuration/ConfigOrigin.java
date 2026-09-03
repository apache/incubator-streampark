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
 * Immutable metadata describing where an effective configuration value originated.
 *
 * <p>The source category participates in precedence decisions, while the human-readable name is
 * retained for diagnostics. Origins describe values rather than entire snapshots because a
 * snapshot can combine entries from multiple files and runtime layers.
 */
public final class ConfigOrigin implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ConfigSource source;
    private final String name;

    private ConfigOrigin(ConfigSource source, String name) {
        this.source = source;
        this.name = name;
    }

    /**
     * Creates validated source metadata with a diagnostic name.
     *
     * @param source source category used for precedence
     * @param name non-blank source name, such as a file path or {@code command line}
     * @return immutable source metadata
     */
    public static ConfigOrigin of(ConfigSource source, String name) {
        Objects.requireNonNull(source, "source must not be null");
        String normalized = Objects.requireNonNull(name, "origin name must not be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("origin name must not be blank");
        }
        return new ConfigOrigin(source, normalized);
    }

    /**
     * Returns the source category used for precedence.
     *
     * @return source category
     */
    public ConfigSource source() {
        return source;
    }

    /**
     * Returns the human-readable source name used in diagnostics.
     *
     * @return non-blank source name
     */
    public String name() {
        return name;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ConfigOrigin)) {
            return false;
        }
        ConfigOrigin that = (ConfigOrigin) object;
        return source == that.source && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, name);
    }

    @Override
    public String toString() {
        return source + ":" + name;
    }
}
