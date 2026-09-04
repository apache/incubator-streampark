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

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Central policy for configuration values that must never be rendered in clear text.
 *
 * <p>The policy supplements the explicit {@link ConfigOptions.TypedOptionBuilder#sensitive()}
 * marker for untyped or engine-native configuration. Matching is intentionally conservative:
 * false positives mask diagnostics, while false negatives can expose credentials in logs.
 */
public final class SensitiveKeys {

    /** Stable replacement used by diagnostics and redacted map views. */
    public static final String MASK = "******";

    /**
     * Markers cover Flink's native redaction policy and StreamPark-specific credential names.
     * Substring matching intentionally favors masking over exposing an unknown secret convention.
     */
    private static final Set<String> MARKERS = Set.of(
        "password",
        "passwd",
        "secret",
        "fs.azure.account.key",
        "apikey",
        "api-key",
        "api.key",
        "auth-params",
        "service-key",
        "token",
        "basic-auth",
        "jaas.config",
        "http-headers",
        "access-key",
        "access.key",
        "accesskey",
        "credential",
        "keytab",
        "private-key",
        "private.key");

    private SensitiveKeys() {
    }

    /**
     * Returns whether a configuration key conventionally identifies secret material.
     *
     * @param key configuration key to inspect
     * @return {@code true} when the lower-cased key contains a supported sensitive marker
     */
    public static boolean isSensitive(String key) {
        String normalized = Objects.requireNonNull(key, "key must not be null")
            .toLowerCase(Locale.ROOT);
        return MARKERS.stream().anyMatch(normalized::contains);
    }
}
