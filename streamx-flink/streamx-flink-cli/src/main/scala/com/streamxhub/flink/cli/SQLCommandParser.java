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

import org.apache.flink.table.api.internal.TableEnvironmentInternal;
import org.apache.flink.table.delegation.Parser;
import org.apache.flink.table.operations.*;
import org.apache.flink.table.operations.ddl.*;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author benjobs
 */
public final class SQLCommandParser {

    private SQLCommandParser() {
    }

    public static List<SQLCommandCall2> parse(TableEnvironmentInternal tableEnv, List<String> lines) {
        List<SQLCommandCall2> calls = new ArrayList<>();
        StringBuilder stmt = new StringBuilder();
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("--")) {
                // skip empty line and comment line
                continue;
            }
            stmt.append("\n").append(line);
            if (line.trim().endsWith(";")) {
                Optional<SQLCommandCall2> optionalCall = parse(tableEnv, stmt.toString());
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


    /**
     * Parse it via flink SqlParser first, then fallback to regular expression matching.
     *
     * @param tableEnv
     * @param stmt
     * @return
     */
    public static Optional<SQLCommandCall2> parse(TableEnvironmentInternal tableEnv, String stmt) {
        Parser sqlParser = tableEnv.getParser();
        SQLCommandCall2 SQLCommandCall2;
        try {
            // parse statement via regex matching first
            Optional<SQLCommandCall2> callOpt = parseByRegexMatching(stmt);
            if (callOpt.isPresent()) {
                SQLCommandCall2 = callOpt.get();
            } else {
                SQLCommandCall2 = parseBySqlParser(sqlParser, stmt);
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.of(SQLCommandCall2);
    }

    private static SQLCommandCall2 parseBySqlParser(Parser sqlParser, String stmt) throws Exception {
        List<Operation> operations;
        try {
            operations = sqlParser.parse(stmt);
        } catch (Throwable e) {
            throw new Exception("Invalidate SQL statement.", e);
        }
        if (operations.size() != 1) {
            throw new Exception("Only single statement is supported now.");
        }

        final SQLCommand2 cmd;
        String[] operands = new String[]{stmt};
        Operation operation = operations.get(0);
        if (operation instanceof CatalogSinkModifyOperation) {
            boolean overwrite = ((CatalogSinkModifyOperation) operation).isOverwrite();
            cmd = overwrite ? SQLCommand2.INSERT_OVERWRITE : SQLCommand2.INSERT_INTO;
        } else if (operation instanceof CreateTableOperation) {
            cmd = SQLCommand2.CREATE_TABLE;
        } else if (operation instanceof DropTableOperation) {
            cmd = SQLCommand2.DROP_TABLE;
        } else if (operation instanceof AlterTableOperation) {
            cmd = SQLCommand2.ALTER_TABLE;
        } else if (operation instanceof CreateViewOperation) {
            cmd = SQLCommand2.CREATE_VIEW;
        } else if (operation instanceof DropViewOperation) {
            cmd = SQLCommand2.DROP_VIEW;
        } else if (operation instanceof CreateDatabaseOperation) {
            cmd = SQLCommand2.CREATE_DATABASE;
        } else if (operation instanceof DropDatabaseOperation) {
            cmd = SQLCommand2.DROP_DATABASE;
        } else if (operation instanceof AlterDatabaseOperation) {
            cmd = SQLCommand2.ALTER_DATABASE;
        } else if (operation instanceof CreateCatalogOperation) {
            cmd = SQLCommand2.CREATE_CATALOG;
        } else if (operation instanceof DropCatalogOperation) {
            cmd = SQLCommand2.DROP_CATALOG;
        } else if (operation instanceof UseCatalogOperation) {
            cmd = SQLCommand2.USE_CATALOG;
            operands = new String[]{((UseCatalogOperation) operation).getCatalogName()};
        } else if (operation instanceof UseDatabaseOperation) {
            cmd = SQLCommand2.USE;
            operands = new String[]{((UseDatabaseOperation) operation).getDatabaseName()};
        } else if (operation instanceof ShowCatalogsOperation) {
            cmd = SQLCommand2.SHOW_CATALOGS;
            operands = new String[0];
        } else if (operation instanceof ShowDatabasesOperation) {
            cmd = SQLCommand2.SHOW_DATABASES;
            operands = new String[0];
        } else if (operation instanceof ShowTablesOperation) {
            cmd = SQLCommand2.SHOW_TABLES;
            operands = new String[0];
        } else if (operation instanceof ShowFunctionsOperation) {
            cmd = SQLCommand2.SHOW_FUNCTIONS;
            operands = new String[0];
        } else if (operation instanceof CreateCatalogFunctionOperation ||
                operation instanceof CreateTempSystemFunctionOperation) {
            cmd = SQLCommand2.CREATE_FUNCTION;
        } else if (operation instanceof DropCatalogFunctionOperation ||
                operation instanceof DropTempSystemFunctionOperation) {
            cmd = SQLCommand2.DROP_FUNCTION;
        } else if (operation instanceof AlterCatalogFunctionOperation) {
            cmd = SQLCommand2.ALTER_FUNCTION;
        } else if (operation instanceof ExplainOperation) {
            cmd = SQLCommand2.EXPLAIN;
        } else if (operation instanceof DescribeTableOperation) {
            cmd = SQLCommand2.DESCRIBE;
            operands = new String[]{((DescribeTableOperation) operation).getSqlIdentifier().asSerializableString()};
        } else if (operation instanceof QueryOperation) {
            cmd = SQLCommand2.SELECT;
        } else {
            throw new Exception("Unknown operation: " + operation.asSummaryString());
        }
        return new SQLCommandCall2(cmd, operands, stmt);
    }

    private static Optional<SQLCommandCall2> parseByRegexMatching(String stmt) {
        // parse statement via regex matching
        for (SQLCommand2 cmd : SQLCommand2.values()) {
            if (cmd.pattern != null) {
                final Matcher matcher = cmd.pattern.matcher(stmt);
                if (matcher.matches()) {
                    final String[] groups = new String[matcher.groupCount()];
                    for (int i = 0; i < groups.length; i++) {
                        groups[i] = matcher.group(i + 1);
                    }
                    return cmd.operandConverter.apply(groups)
                            .map((operands) -> {
                                String[] newOperands = operands;
                                if (cmd == SQLCommand2.EXPLAIN) {
                                    // convert `explain xx` to `explain plan for xx`
                                    // which can execute through executeSql method
                                    newOperands = new String[]{"EXPLAIN PLAN FOR " + operands[0] + " " + operands[1]};
                                }
                                return new SQLCommandCall2(cmd, newOperands, stmt);
                            });
                }
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
    public enum SQLCommand2 {
        QUIT(
                "(QUIT|EXIT)",
                NO_OPERANDS),
        CLEAR(
                "CLEAR",
                NO_OPERANDS),

        HELP(
                "HELP",
                NO_OPERANDS),

        SHOW_CATALOGS(
                "SHOW\\s+CATALOGS",
                NO_OPERANDS),

        SHOW_DATABASES(
                "SHOW\\s+DATABASES",
                NO_OPERANDS),

        SHOW_TABLES(
                "SHOW\\s+TABLES",
                NO_OPERANDS),

        SHOW_FUNCTIONS(
                "SHOW\\s+FUNCTIONS",
                NO_OPERANDS),

        SHOW_MODULES(
                "SHOW\\s+MODULES",
                NO_OPERANDS),

        USE_CATALOG(
                "USE\\s+CATALOG\\s+(.*)",
                SINGLE_OPERAND),

        USE(
                "USE\\s+(?!CATALOG)(.*)",
                SINGLE_OPERAND),

        CREATE_CATALOG(null, SINGLE_OPERAND),

        DROP_CATALOG(null, SINGLE_OPERAND),

        DESC(
                "DESC\\s+(.*)",
                SINGLE_OPERAND),

        DESCRIBE(
                "DESCRIBE\\s+(.*)",
                SINGLE_OPERAND),

        EXPLAIN(
                "EXPLAIN\\s+(.*)",
                SINGLE_OPERAND),

        CREATE_DATABASE(
                "(CREATE\\s+DATABASE\\s+.*)",
                SINGLE_OPERAND),

        DROP_DATABASE(
                "(DROP\\s+DATABASE\\s+.*)",
                SINGLE_OPERAND),

        ALTER_DATABASE(
                "(ALTER\\s+DATABASE\\s+.*)",
                SINGLE_OPERAND),

        CREATE_TABLE("(CREATE\\s+TABLE\\s+.*)", SINGLE_OPERAND),

        DROP_TABLE("(DROP\\s+TABLE\\s+.*)", SINGLE_OPERAND),

        ALTER_TABLE(
                "(ALTER\\s+TABLE\\s+.*)",
                SINGLE_OPERAND),

        DROP_VIEW(
                "DROP\\s+VIEW\\s+(.*)",
                SINGLE_OPERAND),

        CREATE_VIEW(
                "CREATE\\s+VIEW\\s+(\\S+)\\s+AS\\s+(.*)",
                (operands) -> {
                    if (operands.length < 2) {
                        return Optional.empty();
                    }
                    return Optional.of(new String[]{operands[0], operands[1]});
                }),

        CREATE_FUNCTION(null, SINGLE_OPERAND),

        DROP_FUNCTION(null, SINGLE_OPERAND),

        ALTER_FUNCTION(null, SINGLE_OPERAND),

        SELECT(
                "(SELECT.*)",
                SINGLE_OPERAND),

        INSERT_INTO(
                "(INSERT\\s+INTO.*)",
                SINGLE_OPERAND),

        INSERT_OVERWRITE(
                "(INSERT\\s+OVERWRITE.*)",
                SINGLE_OPERAND),

        SET(
                "SET(\\s+(\\S+)\\s*=(.*))?", // whitespace is only ignored on the left side of '='
                (operands) -> {
                    if (operands.length < 3) {
                        return Optional.empty();
                    } else if (operands[0] == null) {
                        return Optional.of(new String[0]);
                    }
                    return Optional.of(new String[]{operands[1], operands[2]});
                }),

        RESET(
                "RESET",
                NO_OPERANDS),

        SOURCE(
                "SOURCE\\s+(.*)",
                SINGLE_OPERAND);

        public final Pattern pattern;
        public final Function<String[], Optional<String[]>> operandConverter;

        SQLCommand2(String matchingRegex, Function<String[], Optional<String[]>> operandConverter) {
            if (matchingRegex == null) {
                this.pattern = null;
            } else {
                this.pattern = Pattern.compile(matchingRegex, DEFAULT_PATTERN_FLAGS);
            }
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
    public static class SQLCommandCall2 {
        public final SQLCommand2 command;
        public final String[] operands;
        public final String sql;

        public SQLCommandCall2(SQLCommand2 command, String[] operands, String sql) {
            this.command = command;
            this.operands = operands;
            this.sql = sql;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            SQLCommandCall2 that = (SQLCommandCall2) o;
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
