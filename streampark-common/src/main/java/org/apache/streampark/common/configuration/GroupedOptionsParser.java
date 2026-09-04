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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parses repeated {@code --group key=value} arguments used by quick-start connectors.
 *
 * <p>Each group retains insertion order and may contain multiple distinct keys. The returned outer
 * and inner maps are immutable so connector initialization observes a stable argument snapshot.
 */
public final class GroupedOptionsParser {

    private GroupedOptionsParser() {
    }

    /**
     * Parses group and entry pairs from a tokenized argument array.
     *
     * @param arguments alternating {@code --group} and {@code key=value} tokens
     * @return immutable group-to-entry mapping
     * @throws ConfigException when a pair is incomplete, malformed, or duplicates a key in a group
     */
    public static Map<String, Map<String, String>> parse(String[] arguments) {
        Objects.requireNonNull(arguments, "arguments must not be null");
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();
        for (int index = 0; index < arguments.length; index += 2) {
            String group = arguments[index];
            if (!group.startsWith(CommandLineParser.LONG_OPTION_PREFIX)
                || group.length() == CommandLineParser.LONG_OPTION_PREFIX.length()) {
                throw new ConfigException("Expected a --group argument but found '" + group + "'");
            }
            if (index + 1 >= arguments.length) {
                throw new ConfigException("Missing key=value for group " + group);
            }
            String expression = arguments[index + 1];
            int separator = expression.indexOf('=');
            if (separator <= 0) {
                throw new ConfigException("Grouped option must use key=value syntax: " + expression);
            }
            String groupName = group.substring(CommandLineParser.LONG_OPTION_PREFIX.length());
            String key = expression.substring(0, separator).trim();
            String value = expression.substring(separator + 1);
            Map<String, String> entries =
                groups.computeIfAbsent(groupName, ignored -> new LinkedHashMap<>());
            if (entries.putIfAbsent(key, value) != null) {
                throw new ConfigException("Duplicate grouped option: " + groupName + "." + key);
            }
        }

        // Freeze both map levels; wrapping only the outer map would still allow connectors to
        // mutate entries belonging to another initialization stage.
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        groups.forEach(
            (group, values) -> result.put(group, Collections.unmodifiableMap(new LinkedHashMap<>(values))));
        return Collections.unmodifiableMap(result);
    }
}
