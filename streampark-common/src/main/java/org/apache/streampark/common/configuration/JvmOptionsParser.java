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
import java.util.List;
import java.util.Map;

/**
 * Parses JVM {@code -Dkey=value} options into a configuration snapshot.
 *
 * <p>Both {@code -Dkey=value} and the token pair {@code -D key=value} are accepted. The parser does
 * not modify {@link System#getProperties()}; it returns an immutable command-line layer that can be
 * composed explicitly with other sources.
 */
public final class JvmOptionsParser {

    private static final String PREFIX = "-D";

    private JvmOptionsParser() {
    }

    /**
     * Parses a JVM option string without invoking a shell.
     *
     * @param options JVM property options, optionally containing quoted values
     * @return immutable command-line configuration snapshot
     * @throws ConfigException when a token is not a {@code -D} property or duplicates a key
     */
    public static Configuration parse(String options) {
        List<String> tokens = CommandLineTokenizer.tokenize(options);
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int index = 0; index < tokens.size(); index++) {
            String token = tokens.get(index);
            String expression;
            if (PREFIX.equals(token)) {
                if (++index >= tokens.size()) {
                    throw new ConfigException("-D must be followed by key=value");
                }
                expression = tokens.get(index);
            } else if (token.startsWith(PREFIX)) {
                expression = token.substring(PREFIX.length());
            } else {
                throw new ConfigException("Expected a -Dkey=value option but found '" + token + "'");
            }

            int separator = expression.indexOf('=');
            if (separator <= 0) {
                throw new ConfigException("JVM property must use -Dkey=value syntax: " + token);
            }
            String key = expression.substring(0, separator).trim();
            String value = expression.substring(separator + 1);
            if (properties.putIfAbsent(key, value) != null) {
                throw new ConfigException("Duplicate JVM property: " + key);
            }
        }
        return Configuration.builder()
            .add("JVM options", ConfigSource.COMMAND_LINE, properties)
            .build();
    }
}
