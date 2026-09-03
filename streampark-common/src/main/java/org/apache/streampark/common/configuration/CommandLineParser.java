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
import java.util.Objects;

/**
 * Strict parser for StreamPark's {@code --key value} and {@code --key=value} arguments.
 *
 * <p>This parser consumes an already-tokenized argument array, normally supplied by a JVM main
 * method. It does not perform shell parsing; command strings must first pass through {@link
 * CommandLineTokenizer}. Parsed values are assigned {@link ConfigSource#COMMAND_LINE} precedence.
 */
public final class CommandLineParser {

    /** Prefix identifying a StreamPark long option. */
    public static final String LONG_OPTION_PREFIX = "--";

    private CommandLineParser() {
    }

    /**
     * Parses command-line arguments into a configuration snapshot.
     *
     * <p>A key without an explicit value is represented by boolean {@code true}. Positional
     * arguments and duplicate keys are rejected because both otherwise hide deployment mistakes.
     *
     * @param arguments tokenized command-line arguments
     * @return immutable command-line configuration snapshot
     * @throws ConfigException when an argument is positional, malformed, duplicated, or null
     */
    public static Configuration parse(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index < arguments.length; index++) {
            String token = Objects.requireNonNull(arguments[index], "argument must not be null");
            if (!token.startsWith(LONG_OPTION_PREFIX) || token.length() == LONG_OPTION_PREFIX.length()) {
                throw new ConfigException("Expected a --key argument but found '" + token + "'");
            }

            String expression = token.substring(LONG_OPTION_PREFIX.length());
            int separator = expression.indexOf('=');
            String key;
            Object value;
            // An equals sign binds even an empty value to the current option. Without one, the
            // following non-option token is consumed; otherwise the option is treated as a flag.
            if (separator >= 0) {
                key = expression.substring(0, separator);
                value = expression.substring(separator + 1);
            } else {
                key = expression;
                if (index + 1 < arguments.length
                    && !arguments[index + 1].startsWith(LONG_OPTION_PREFIX)) {
                    value = arguments[++index];
                } else {
                    value = true;
                }
            }
            if (key.trim().isEmpty()) {
                throw new ConfigException("Command-line option key must not be blank");
            }
            if (values.putIfAbsent(key, value) != null) {
                throw new ConfigException("Duplicate command-line option: " + key);
            }
        }
        return Configuration.builder()
            .add("command line", ConfigSource.COMMAND_LINE, values)
            .build();
    }
}
