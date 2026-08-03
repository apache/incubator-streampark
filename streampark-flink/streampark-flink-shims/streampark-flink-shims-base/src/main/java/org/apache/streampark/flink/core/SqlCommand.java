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
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Supported Flink SQL command types. */
public enum SqlCommand {

    SELECT("select", "(SELECT\\s+.+)"),
    CREATE_TABLE("create table", "(CREATE\\s+(TEMPORARY\\s+|)TABLE\\s+.+)"),
    CREATE_CATALOG("create catalog", "(CREATE\\s+CATALOG\\s+.+)"),
    CREATE_DATABASE("create database", "(CREATE\\s+DATABASE\\s+.+)"),
    CREATE_VIEW(
        "create view",
        "(CREATE\\s+(TEMPORARY\\s+|)VIEW\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+SELECT\\s+.+)"),
    CREATE_FUNCTION(
        "create function",
        "(CREATE\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+.*)"),
    DROP_CATALOG("drop catalog", "(DROP\\s+CATALOG\\s+.+)"),
    DROP_TABLE("drop table", "(DROP\\s+(TEMPORARY\\s+|)TABLE\\s+.+)"),
    DROP_DATABASE("drop database", "(DROP\\s+DATABASE\\s+.+)"),
    DROP_VIEW("drop view", "(DROP\\s+(TEMPORARY\\s+|)VIEW\\s+.+)"),
    DROP_FUNCTION(
        "drop function",
        "(DROP\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.+)"),
    ALTER_TABLE("alter table", "(ALTER\\s+TABLE\\s+.+)"),
    ALTER_VIEW("alter view", "(ALTER\\s+VIEW\\s+.+)"),
    ALTER_DATABASE("alter database", "(ALTER\\s+DATABASE\\s+.+)"),
    ALTER_FUNCTION(
        "alter function",
        "(ALTER\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+.+)"),
    INSERT("insert", "(INSERT\\s+(INTO|OVERWRITE)\\s+.+)"),
    DESC("desc", "(DESC\\s+.+)"),
    DESCRIBE("describe", "(DESCRIBE\\s+.+)"),
    EXPLAIN("explain", "(EXPLAIN\\s+.+)"),
    USE_CATALOG("use catalog", "(USE\\s+CATALOG\\s+.+)"),
    USE_MODULES("use modules", "(USE\\s+MODULES\\s+.+)"),
    USE_DATABASE("use database", "(USE\\s+(?!(CATALOG|MODULES)).+)"),
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
    LOAD_MODULE("load module", "(LOAD\\s+MODULE\\s+.+)"),
    UNLOAD_MODULE("unload module", "(UNLOAD\\s+MODULE\\s+.+)"),
    SET("set", "SET(\\s+(\\S+)\\s*=(.*))?", SqlCommandConverters::setOperands),
    RESET("reset", "RESET\\s+'(.*)'"),
    RESET_ALL("reset all", "RESET", SqlCommandConverters::resetAll),
    BEGIN_STATEMENT_SET(
        "begin statement set", "BEGIN\\s+STATEMENT\\s+SET", SqlCommandConverters::noOperands),
    END_STATEMENT_SET("end statement set", "END", SqlCommandConverters::noOperands),
    DELETE("delete", "(DELETE\\s+FROM\\s+.+)"),
    UPDATE("update", "(UPDATE\\s+.+)");

    private final String commandName;
    private final String regex;
    private final Function<String[], Optional<String[]>> converter;
    private Matcher matcher;

    SqlCommand(String commandName, String regex) {
        this(commandName, regex, SqlCommandConverters::firstGroup);
    }

    SqlCommand(
               String commandName,
               String regex,
               Function<String[], Optional<String[]>> converter) {
        this.commandName = commandName;
        this.regex = regex;
        this.converter = converter;
    }

    /** Scala field-style access (lowercase command label). */
    public String commandName() {
        return commandName;
    }

    public String getCommandName() {
        return commandName;
    }

    public Matcher matcher() {
        return matcher;
    }

    public Matcher getMatcher() {
        return matcher;
    }

    public Optional<String[]> convert(String[] groups) {
        return converter.apply(groups);
    }

    public boolean matches(String input) {
        if (StringUtils.isBlank(regex)) {
            return false;
        }
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        matcher = pattern.matcher(input);
        return matcher.matches();
    }

    public static SqlCommand get(String stmt) {
        for (SqlCommand command : values()) {
            if (command.matches(stmt)) {
                return command;
            }
        }
        return null;
    }
}
