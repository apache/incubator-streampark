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

/**
 * StreamPark's engine-neutral configuration model.
 *
 * <p>The package separates five responsibilities:
 *
 * <ol>
 *   <li>{@link org.apache.streampark.common.configuration.ConfigOption} declares immutable typed
 *       metadata.
 *   <li>{@link org.apache.streampark.common.configuration.ConfigurationParser} parses external
 *       documents without requiring an option registry.
 *   <li>{@link org.apache.streampark.common.util.YamlParser} provides strict structured YAML
 *       parsing with a bounded compatibility fallback.
 *   <li>{@link org.apache.streampark.common.configuration.ConfigurationLoader} composes named
 *       layers using the fixed source precedence.
 *   <li>{@link org.apache.streampark.common.configuration.Configuration} exposes an immutable,
 *       origin-aware snapshot to consumers.
 * </ol>
 *
 * <p>The lifecycle of a configuration value is declaration, parsing, layer composition, and typed
 * access. Parsers deliberately retain raw scalar or collection values; conversion and validation
 * happen only when a consumer reads a {@link
 * org.apache.streampark.common.configuration.ConfigOption}. This permits independently deployed
 * modules to define options without maintaining a central registry in {@code streampark-common}.
 *
 * <p>Precedence from lowest to highest is defaults, files, environment, JVM system properties,
 * command line, and runtime overrides. Origins remain attached to effective values so conversion
 * failures can identify the responsible source. Configuration parsing never mutates JVM system
 * properties, and immutable snapshots prevent a running operation from observing partial updates.
 * Engine-specific option catalogs belong to their engine module's {@code configuration} package.
 */
package org.apache.streampark.common.configuration;
