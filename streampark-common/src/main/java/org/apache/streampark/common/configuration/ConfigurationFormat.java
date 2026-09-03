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

import java.nio.file.Path;
import java.util.Locale;

/**
 * Configuration document formats supported by {@link ConfigurationParser}.
 *
 * <p>The format controls syntax parsing only. Every parser produces the same flattened,
 * origin-aware {@link Configuration} representation.
 */
public enum ConfigurationFormat {

    /** YAML configuration, including nested mappings and scalar lists. */
    YAML,

    /** HOCON configuration resolved through Typesafe Config. */
    HOCON,

    /** Java properties configuration with exact, flat keys. */
    PROPERTIES;

    /**
     * Infers a supported configuration format from a path extension.
     *
     * @param path path whose filename identifies the format
     * @return inferred document format
     * @throws ConfigException when the filename has no supported extension
     */
    public static ConfigurationFormat fromPath(Path path) {
        String filename = path.getFileName().toString().toLowerCase(Locale.ROOT);
        if (filename.endsWith(".yaml") || filename.endsWith(".yml")) {
            return YAML;
        }
        if (filename.endsWith(".conf") || filename.endsWith(".hocon")) {
            return HOCON;
        }
        if (filename.endsWith(".properties")) {
            return PROPERTIES;
        }
        throw new ConfigException("Unsupported configuration file format: " + path);
    }
}
