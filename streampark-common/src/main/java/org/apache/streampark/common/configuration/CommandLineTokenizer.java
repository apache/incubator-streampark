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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Tokenizes a command-line string without invoking a shell or performing variable expansion.
 *
 * <p>The result is suitable for parsers that consume a JVM-style {@code String[]} argument array.
 * Avoiding a shell is a security boundary: metacharacters, substitutions, and redirects remain
 * ordinary token content and are never executed.
 */
public final class CommandLineTokenizer {

    private CommandLineTokenizer() {
    }

    /**
     * Splits a command line while honoring single quotes, double quotes, and backslash escapes.
     *
     * <p>The tokenizer intentionally implements only argument grouping. Shell substitutions,
     * redirects, globbing, and environment expansion are never evaluated.
     *
     * @param commandLine command string; {@code null} and blank input produce an empty list
     * @return immutable tokens with grouping quotes and escape characters removed
     * @throws ConfigException when a quote or escape sequence is incomplete
     */
    public static List<String> tokenize(String commandLine) {
        if (commandLine == null || commandLine.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        char quote = 0;
        boolean escaping = false;
        boolean tokenStarted = false;
        // A single pass is sufficient because the tokenizer tracks only quote and escape state.
        // tokenStarted preserves deliberately empty quoted arguments such as "".
        for (int index = 0; index < commandLine.length(); index++) {
            char character = commandLine.charAt(index);
            if (escaping) {
                current.append(character);
                escaping = false;
                tokenStarted = true;
                continue;
            }
            if (character == '\\' && quote != '\'') {
                escaping = true;
                tokenStarted = true;
                continue;
            }
            if (quote != 0) {
                if (character == quote) {
                    quote = 0;
                } else {
                    current.append(character);
                }
                tokenStarted = true;
                continue;
            }
            if (character == '\'' || character == '"') {
                quote = character;
                tokenStarted = true;
            } else if (Character.isWhitespace(character)) {
                if (tokenStarted) {
                    tokens.add(current.toString());
                    current.setLength(0);
                    tokenStarted = false;
                }
            } else {
                current.append(character);
                tokenStarted = true;
            }
        }
        if (escaping) {
            throw new ConfigException("Command line ends with an incomplete escape sequence");
        }
        if (quote != 0) {
            throw new ConfigException("Command line contains an unterminated quoted value");
        }
        if (tokenStarted) {
            tokens.add(current.toString());
        }
        return Collections.unmodifiableList(tokens);
    }
}
