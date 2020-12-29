/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.streamxhub.flink.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple parser for determining the type of command and its parameters.
 *
 * @author benjobs
 */

public final class SQLCommandUtils {

    private SQLCommandUtils() {
        // private
    }

    public static List<SQLCommandCall> parseSQL(List<String> lines) {
        List<SQLCommandCall> calls = new ArrayList<>();
        StringBuilder stmt = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("--")) {
                // skip empty line and comment line
                continue;
            }
            stmt.append("\n").append(line);
            if (line.trim().endsWith(";")) {
                Optional<SQLCommandCall> optionalCall = parseSQL(stmt.toString());
                if (optionalCall.isPresent()) {
                    calls.add(optionalCall.get());
                } else {
                    throw new RuntimeException("Unsupported command '" + stmt.toString() + "'");
                }
                // clear string builder
                stmt.setLength(0);
            }
        }
        return calls;
    }

    public static Optional<SQLCommandCall> parseSQL(String stmt) {
        // normalize
        stmt = stmt.trim();
        // remove ';' at the end
        if (stmt.endsWith(";")) {
            stmt = stmt.substring(0, stmt.length() - 1).trim();
        }

        // parse
        for (SQLCommand cmd : SQLCommand.values()) {
            final Matcher matcher = cmd.pattern.matcher(stmt);
            if (matcher.matches()) {
                final String[] groups = new String[matcher.groupCount()];
                for (int i = 0; i < groups.length; i++) {
                    groups[i] = matcher.group(i + 1);
                }
                return cmd.operandConverter.apply(groups).map((operands) -> new SQLCommandCall(cmd, operands));
            }
        }
        return Optional.empty();
    }

    // --------------------------------------------------------------------------------------------

    private static final Function<String[], Optional<String[]>> NO_OPERANDS = (operands) -> Optional.of(new String[0]);

    private static final Function<String[], Optional<String[]>> SINGLE_OPERAND = (operands) -> Optional.of(new String[]{operands[0]});

    private static final int DEFAULT_PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

    /**
     * Supported SQL commands.
     */
    public enum SQLCommand {
        SET("SET(\\s+(\\S+)\\s*=(.*))?",
                (operands) -> {
                    if (operands.length < 3) {
                        return Optional.empty();
                    } else if (operands[0] == null) {
                        return Optional.of(new String[0]);
                    }
                    return Optional.of(new String[]{operands[1], operands[2]});
                }),
        INSERT_INTO("(INSERT\\s+INTO.*)", SINGLE_OPERAND),

        CREATE_TABLE("(CREATE\\s+TABLE.*)", SINGLE_OPERAND),

        ;

        public final Pattern pattern;
        public final Function<String[], Optional<String[]>> operandConverter;

        SQLCommand(String matchingRegex, Function<String[], Optional<String[]>> operandConverter) {
            this.pattern = Pattern.compile(matchingRegex, DEFAULT_PATTERN_FLAGS);
            this.operandConverter = operandConverter;
        }

        @Override
        public String toString() {
            return super.toString().replace('_', ' ');
        }

        public boolean hasOperands() {
            return operandConverter != NO_OPERANDS;
        }
    }

    /**
     * Call of SQL command with operands and command type.
     */
    public static class SQLCommandCall {
        public final SQLCommand command;
        public final String[] operands;

        public SQLCommandCall(SQLCommand command, String[] operands) {
            this.command = command;
            this.operands = operands;
        }

        public SQLCommandCall(SQLCommand command) {
            this(command, new String[0]);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SQLCommandCall that = (SQLCommandCall) o;
            return command == that.command && Arrays.equals(operands, that.operands);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(command);
            result = 31 * result + Arrays.hashCode(operands);
            return result;
        }

        @Override
        public String toString() {
            return command + "(" + Arrays.toString(operands) + ")";
        }
    }
}
