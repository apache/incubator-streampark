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

import java.util.Optional;

/**
 * Read-only typed access to a configuration snapshot.
 *
 * <p>Consumers depend on this interface when they only require option resolution and should not
 * need to know how layers were parsed or composed. Implementations resolve canonical keys before
 * fallback keys and apply the type conversion and validation declared by each option.
 */
public interface ReadableConfig {

    /**
     * Returns the effective value of an option.
     *
     * @param option option metadata used for lookup, conversion, and validation
     * @param <T> option value type
     * @return configured value, or the declared default when the option is absent
     * @throws ConfigException when a required option is absent or its value is invalid
     */
    <T> T get(ConfigOption<T> option);

    /**
     * Returns the explicitly configured value of an option.
     *
     * <p>Defaults are intentionally not returned, which allows callers to distinguish an omitted
     * option from one whose effective value happens to equal its default.
     *
     * @param option option metadata used for lookup, conversion, and validation
     * @param <T> option value type
     * @return configured value, or an empty optional when neither canonical nor fallback key exists
     * @throws ConfigException when the configured value cannot be converted or validated
     */
    <T> Optional<T> getOptional(ConfigOption<T> option);

    /**
     * Returns whether an option is explicitly present under its canonical or a fallback key.
     *
     * @param option option metadata used for lookup
     * @return {@code true} when a configured value exists; {@code false} for defaults alone
     */
    boolean contains(ConfigOption<?> option);
}
