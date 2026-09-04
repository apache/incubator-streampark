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

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Atomic process-wide reference to the current immutable server configuration snapshot.
 *
 * <p>The global reference exists only at the process boundary. Domain code should capture
 * {@link #current()} once at the beginning of a multi-step operation and pass that snapshot to
 * derived objects. Replacing or overlaying configuration never mutates an existing snapshot.
 */
public final class GlobalConfiguration {

    private static final AtomicReference<Configuration> CURRENT =
        new AtomicReference<>(new ConfigurationLoader().addSystemProperties().load());

    private GlobalConfiguration() {
    }

    /**
     * Returns the current immutable process snapshot.
     *
     * @return current snapshot
     */
    public static Configuration current() {
        return CURRENT.get();
    }

    /**
     * Atomically update the process snapshot.
     *
     * @param configuration new immutable snapshot
     */
    public static void update(Configuration configuration) {
        CURRENT.set(Objects.requireNonNull(configuration, "configuration must not be null"));
    }

    /**
     * Atomically overlays a named source layer while honoring source precedence.
     *
     * @param source source category used for precedence
     * @param name non-blank origin name retained in diagnostics
     * @param values raw values to overlay
     */
    public static void overlay(ConfigSource source, String name, Map<String, ?> values) {
        Objects.requireNonNull(source, "source must not be null");
        CURRENT.updateAndGet(
            current -> Configuration.builder(current).add(name, source, values).build());
    }

    /**
     * Atomically sets a validated runtime option.
     *
     * @param option target option metadata
     * @param value non-null converted value
     * @param originName non-blank runtime origin retained in diagnostics
     * @param <T> option value type
     */
    public static <T> void set(ConfigOption<T> option, T value, String originName) {
        CURRENT.updateAndGet(
            current -> Configuration.builder(current).set(option, value, originName).build());
    }
}
