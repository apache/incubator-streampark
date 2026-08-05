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

import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Flink SQL command types. */
public enum SqlCommand {

    // ---- SELECT Statements -----------------------------------------------------------------
    SELECT("select", "(SELECT\\s+.+)"),

    // ---- CREATE Statements -----------------------------------------------------------------
    CREATE_TABLE("create table", "(CREATE\\s+(TEMPORARY\\s+|)TABLE\\s+.+)"),
    CREATE_CATALOG("create catalog", "(CREATE\\s+CATALOG\\s+.+)"),
    CREATE_DATABASE("create database", "(CREATE\\s+DATABASE\\s+.+)"),
    CREATE_VIEW(
        "create view",
        "(CREATE\\s+(TEMPORARY\\s+|)VIEW\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+SELECT\\s+.+)"),
    CREATE_FUNCTION(
        "create function",
        "(CREATE\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+.*)"),

    // ---- DROP Statements -------------------------------------------------------------------
    DROP_CATALOG("drop catalog", "(DROP\\s+CATALOG\\s+.+)"),
    DROP_TABLE("drop table", "(DROP\\s+(TEMPORARY\\s+|)TABLE\\s+.+)"),
    DROP_DATABASE("drop database", "(DROP\\s+DATABASE\\s+.+)"),
    DROP_VIEW("drop view", "(DROP\\s+(TEMPORARY\\s+|)VIEW\\s+.+)"),
    DROP_FUNCTION(
        "drop function", "(DROP\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.+)"),

    // ---- ALTER Statements ------------------------------------------------------------------
    ALTER_TABLE("alter table", "(ALTER\\s+TABLE\\s+.+)"),
    ALTER_VIEW("alter view", "(ALTER\\s+VIEW\\s+.+)"),
    ALTER_DATABASE("alter database", "(ALTER\\s+DATABASE\\s+.+)"),
    ALTER_FUNCTION(
        "alter function",
        "(ALTER\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.+)"),

    // ---- INSERT Statement ------------------------------------------------------------------
    INSERT("insert", "(INSERT\\s+(INTO|OVERWRITE)\\s+.+)"),

    // ---- DESCRIBE Statement ----------------------------------------------------------------
    DESC("desc", "(DESC\\s+.+)"),
    DESCRIBE("describe", "(DESCRIBE\\s+.+)"),

    // ---- EXPLAIN Statement -----------------------------------------------------------------
    EXPLAIN("explain", "(EXPLAIN\\s+.+)"),

    // ---- USE Statements --------------------------------------------------------------------
    USE_CATALOG("use catalog", "(USE\\s+CATALOG\\s+.+)"),
    USE_MODULES("use modules", "(USE\\s+MODULES\\s+.+)"),
    USE_DATABASE("use database", "(USE\\s+(?!(CATALOG|MODULES)).+)"),

    // ---- SHOW Statements -------------------------------------------------------------------
    SHOW_CATALOGS("show catalogs", "(SHOW\\s+CATALOGS\\s*)"),
    SHOW_CURRENT_CATALOG("show current catalog", "(SHOW\\s+CURRENT\\s+CATALOG\\s*)"),
    SHOW_DATABASES("show databases", "(SHOW\\s+DATABASES\\s*)"),
    SHOW_CURRENT_DATABASE("show current database", "(SHOW\\s+CURRENT\\s+DATABASE\\s*)"),
    SHOW_TABLES("show tables", "(SHOW\\s+TABLES.*)"),
    SHOW_CREATE_TABLE("show create table", "(SHOW\\s+CREATE\\s+TABLE\\s+.+)"),
    SHOW_COLUMNS("show columns", "(SHOW\\s+COLUMNS\\s+.+)"),
    SHOW_VIEWS("show views", "(SHOW\\s+VIEWS\\s*)"),
    SHOW_CREATE_VIEW("show create view", "(SHOW\\s+CREATE\\s+VIEW\\s+.+)"),
    SHOW_FUNCTIONS("show functions", "(SHOW\\s+(USER\\s+|)FUNCTIONS\\s*)"),
    SHOW_MODULES("show modules", "(SHOW\\s+(FULL\\s+|)MODULES\\s*)"),

    // ---- LOAD Statements -------------------------------------------------------------------
    LOAD_MODULE("load module", "(LOAD\\s+MODULE\\s+.+)"),

    // ---- UNLOAD Statements -----------------------------------------------------------------
    UNLOAD_MODULE("unload module", "(UNLOAD\\s+MODULE\\s+.+)"),

    // ---- SET Statements --------------------------------------------------------------------
    SET(
        "set",
        "SET(\\s+(\\S+)\\s*=(.*))?",
        groups -> {
            if (groups.length < 3) {
                return Optional.empty();
            }
            if (groups[0] == null) {
                return Optional.of(new String[]{cleanUp(groups[0])});
            }
            return Optional.of(new String[]{cleanUp(groups[1]), cleanUp(groups[2])});
        }),

    // ---- RESET Statements ------------------------------------------------------------------
    RESET("reset", "RESET\\s+'(.*)'"),
    RESET_ALL("reset all", "RESET", groups -> Optional.of(new String[]{"ALL"})),

    // ---- INSERT SET Statements -------------------------------------------------------------
    /** @deprecated SQL Client syntax; not supported on this platform. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    BEGIN_STATEMENT_SET(
        "begin statement set", "BEGIN\\s+STATEMENT\\s+SET", SqlCommandConverters.NO_OPERANDS),
    /** @deprecated SQL Client syntax; not supported on this platform. */
    @Deprecated(since = "2.1.0", forRemoval = false)
    END_STATEMENT_SET("end statement set", "END", SqlCommandConverters.NO_OPERANDS),

    // Since: 2.1.2 for flink 1.18
    DELETE("delete", "(DELETE\\s+FROM\\s+.+)"),
    UPDATE("update", "(UPDATE\\s+.+)");

    private static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

    private final String name;
    private final String regex;
    private final SqlCommandConverter converter;
    private Matcher matcher;

    SqlCommand(String name, String regex) {
        this(name, regex, SqlCommandConverters.DEFAULT);
    }

    SqlCommand(String name, String regex, SqlCommandConverter converter) {
        this.name = name;
        this.regex = regex;
        this.converter = converter;
    }

    /** Command label (e.g. {@code "select"}, {@code "create table"}). */
    public String getName() {
        return name;
    }

    public String getRegex() {
        return regex;
    }

    public SqlCommandConverter getConverter() {
        return converter;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    public boolean matches(String input) {
        if (StringUtils.isBlank(regex)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex, PATTERN_FLAGS);
        matcher = pattern.matcher(input);
        return matcher.matches();
    }

    /** Resolve the first matching command for the given statement. */
    public static SqlCommand get(String stmt) {
        for (SqlCommand command : values()) {
            if (command.matches(stmt)) {
                return command;
            }
        }
        return null;
    }

    static String cleanUp(String sql) {
        return sql.trim().replaceAll("^(['\"])|(['\"])$", "");
    }
}
