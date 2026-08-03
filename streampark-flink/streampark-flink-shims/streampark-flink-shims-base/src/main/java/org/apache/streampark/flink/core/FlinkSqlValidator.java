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

package org.apache.streampark.flink.core;

import org.apache.streampark.common.enums.FlinkSqlValidationFailedType;
import org.apache.streampark.common.util.ExceptionUtils;

import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.flink.sql.parser.validate.FlinkSqlConformance;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.planner.delegation.FlinkSqlParserFactories;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Validates Flink SQL syntax using Calcite parser. */
public final class FlinkSqlValidator {

    private static final String FLINK112_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.calcite.CalciteParser";

    private static final String FLINK113_PLUS_CALCITE_PARSER_CLASS =
        "org.apache.flink.table.planner.parse.CalciteParser";

    private static final Pattern SYNTAX_ERROR_PATTERN =
        Pattern.compile(".*at\\sline\\s(\\d+),\\scolumn\\s(\\d+).*");

    private static final Map<String, SqlParser.Config> SQL_PARSER_CONFIG_MAP = createSqlParserConfigMap();

    private static final Log LOG = new Log();

    private FlinkSqlValidator() {
    }

    public static FlinkSqlValidationResult verifySql(String sql) {
        AtomicReference<FlinkSqlValidationResult> earlyReturn = new AtomicReference<>();
        List<SqlCommandCall> sqlCommands =
            SqlCommandParser.parseSQL(sql, earlyReturn::set);
        FlinkSqlValidationResult earlyResult = earlyReturn.get();
        if (earlyResult != null) {
            return earlyResult;
        }

        String sqlDialect = SqlDialect.DEFAULT.name().toLowerCase();
        boolean hasInsert = false;
        for (SqlCommandCall call : sqlCommands) {
            String args = call.operands()[0];
            SqlCommand command = call.command();
            switch (command) {
                case SET:
                case RESET:
                    if (command == SqlCommand.SET
                        && args.equals(TableConfigOptions.TABLE_SQL_DIALECT.key())) {
                        sqlDialect = call.operands()[call.operands().length - 1];
                    }
                    break;
                case BEGIN_STATEMENT_SET:
                case END_STATEMENT_SET:
                    LOG.warn("SQL Client Syntax: " + call.command().getCommandName());
                    break;
                default:
                    if (command == SqlCommand.INSERT) {
                        hasInsert = true;
                    }
                    try {
                        Class<?> calciteClass;
                        try {
                            calciteClass = Class.forName(FLINK112_CALCITE_PARSER_CLASS);
                        } catch (ClassNotFoundException e) {
                            calciteClass = Class.forName(FLINK113_PLUS_CALCITE_PARSER_CLASS);
                        }
                        String dialectUpper = sqlDialect.toUpperCase();
                        if ("HIVE".equals(dialectUpper)) {
                            // skip calcite validation for HIVE dialect
                        } else if ("DEFAULT".equals(dialectUpper)) {
                            Constructor<?> constructor =
                                calciteClass.getConstructor(SqlParser.Config.class);
                            Object parser =
                                constructor.newInstance(
                                    SQL_PARSER_CONFIG_MAP.get(sqlDialect.toUpperCase()));
                            Method method = parser.getClass().getDeclaredMethod("parse", String.class);
                            method.setAccessible(true);
                            method.invoke(parser, call.originSql());
                        } else {
                            throw new UnsupportedOperationException(
                                "unsupported dialect: " + sqlDialect);
                        }
                    } catch (ReflectiveOperationException e) {
                        return syntaxErrorResult(call, e);
                    } catch (RuntimeException e) {
                        return syntaxErrorResult(call, e);
                    }
                    break;
            }
        }

        if (hasInsert) {
            return new FlinkSqlValidationResult();
        }
        return new FlinkSqlValidationResult(
            false,
            FlinkSqlValidationFailedType.SYNTAX_ERROR,
            sqlCommands.get(0).lineStart(),
            sqlCommands.get(sqlCommands.size() - 1).lineEnd(),
            0,
            0,
            null,
            "No 'INSERT' statement to trigger the execution of the Flink job.");
    }

    private static FlinkSqlValidationResult syntaxErrorResult(SqlCommandCall call, Throwable e) {
        String exception = ExceptionUtils.stringifyException(e);
        String causedBy = exception.substring(exception.indexOf("Caused by:"));
        String cleanUpError = exception.replaceAll("[\r\n]", "");
        Matcher matcher = SYNTAX_ERROR_PATTERN.matcher(cleanUpError);
        if (matcher.matches()) {
            int line = Integer.parseInt(matcher.group(1));
            int column = Integer.parseInt(matcher.group(2));
            int errorLine = call.lineStart() + line - 1;
            return new FlinkSqlValidationResult(
                false,
                FlinkSqlValidationFailedType.SYNTAX_ERROR,
                call.lineStart(),
                call.lineEnd(),
                errorLine,
                column,
                call.originSql(),
                causedBy.replaceAll("at\\sline\\s" + line, "at line " + errorLine));
        }
        return new FlinkSqlValidationResult(
            false,
            FlinkSqlValidationFailedType.SYNTAX_ERROR,
            call.lineStart(),
            call.lineEnd(),
            0,
            0,
            call.originSql(),
            causedBy);
    }

    private static Map<String, SqlParser.Config> createSqlParserConfigMap() {
        Map<String, SqlParser.Config> map = new HashMap<>();
        map.put(SqlDialect.DEFAULT.name(), getConfig(SqlDialect.DEFAULT));
        map.put(SqlDialect.HIVE.name(), getConfig(SqlDialect.HIVE));
        return map;
    }

    private static SqlParser.Config getConfig(SqlDialect sqlDialect) {
        org.apache.calcite.sql.validate.SqlConformance conformance;
        if (sqlDialect == SqlDialect.HIVE) {
            try {
                conformance = FlinkSqlConformance.DEFAULT;
            } catch (NoSuchFieldError e) {
                conformance = FlinkSqlConformance.DEFAULT;
            }
        } else if (sqlDialect == SqlDialect.DEFAULT) {
            conformance = FlinkSqlConformance.DEFAULT;
        } else {
            throw new UnsupportedOperationException("Unsupported sqlDialect: " + sqlDialect);
        }
        return SqlParser.config()
            .withParserFactory(FlinkSqlParserFactories.create(conformance))
            .withConformance(conformance)
            .withLex(Lex.JAVA)
            .withIdentifierMaxLength(256);
    }

    private static final class Log extends org.apache.streampark.common.util.LoggerSupport {

        void warn(String msg) {
            logWarn(msg);
        }
    }
}
