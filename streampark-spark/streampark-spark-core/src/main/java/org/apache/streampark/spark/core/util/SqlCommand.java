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

package org.apache.streampark.spark.core.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Spark SQL command types. */
public enum SqlCommand {

    ALTER_DATABASE("alter database", "(ALTER\\s+(DATABASE\\s+|SCHEMA\\s+|NAMESPACE\\s+)\\s+.+)"),
    ALTER_TABLE("alter table", "(ALTER\\s+TABLE\\s+.+)"),
    ALTER_VIEW("alter view", "(ALTER\\s+VIEW\\s+.+)"),
    CREATE_DATABASE("create database", "(CREATE\\s+(DATABASE\\s+|SCHEMA\\s+)\\s+.+)"),
    CREATE_FUNCTION("create function",
        "(CREATE\\s+(OR\\s+REPLACE\\s+|)(TEMPORARY\\s+|)FUNCTION\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+.*)"),
    CREATE_TABLE("create table", "(CREATE\\s+(EXTERNAL\\s+|)TABLE\\s+(IF\\s+NOT\\s+EXISTS\\s+|).+)"),
    CREATE_VIEW("create view",
        "(CREATE\\s+(OR\\s+REPLACE\\s+|)((GLOBAL\\s+|)TEMPORARY\\s+|)VIEW\\s+(IF\\s+NOT\\s+EXISTS\\s+|)(\\S+)\\s+AS\\s+SELECT\\s+.+)"),
    DROP_DATABASE("drop database", "(DROP\\s+(DATABASE\\s+|SCHEMA\\s+)(IF\\s+EXISTS\\s+|).+)"),
    DROP_FUNCTION("drop function", "(DROP\\s+(TEMPORARY\\s+|)FUNCTION\\s+(IF\\s+EXISTS\\s+|).+)"),
    DROP_TABLE("drop table", "(DROP\\s+TABLE\\s+(IF\\s+EXISTS\\s+|).+)"),
    DROP_VIEW("drop view", "(DROP\\s+VIEW\\s+(IF\\s+EXISTS\\s+|).+)"),
    REPAIR_TABLE("repair table", "((MSCK\\s+|)DROP\\s+TABLE\\s+.+)"),
    TRUNCATE_TABLE("truncate table", "(TRUNCATE\\s+TABLE\\s+.+)"),
    USE_DATABASE("use database", "(USE\\s+.+)"),
    INSERT("insert", "(INSERT\\s+(INTO|OVERWRITE)\\s+.+)"),
    INSERT_REPLACE("insert replace", "(INSERT\\s+INTO\\s+(TABLE|)\\s+(\\S+)\\s+REPLACE\\s+WHERE\\s+.+)"),
    INSERT_OVERWRITE_DIRECTORY("insert overwrite directory", "(INSERT\\s+OVERWRITE\\s+(LOCAL|)\\s+DIRECTORY\\s+.+)"),
    LOAD_DATE("load data", "(LOAD\\s+DATA\\s+(LOCAL|)\\s+INPATH\\s+.+)"),
    SELECT("select", "(SELECT\\s+.+)"),
    WITH_SELECT("with select", "(WITH\\s+.+)"),
    EXPLAIN("explain", "(EXPLAIN\\s+(EXTENDED|CODEGEN|COST|FORMATTED)\\s.+)"),
    ADD_FILE("add file", "(ADD\\s+(FILE|FILES)\\s+.+)"),
    ADD_JAR("add jar", "(ADD\\s+(JAR|JARS)\\s+.+)"),
    ANALYZE_TABLE("analyze table", "(ANALYZE\\s+(TABLES|TABLE)\\s+.+)"),
    CACHE_TABLE("cache table", "(CACHE\\s+(LAZY\\s+|)TABLE\\s+.+)"),
    UNCACHE_TABLE("uncache table", "(UNCACHE\\s+TABLE\\s+.+)"),
    CLEAR_CACHE("clear cache", "(CLEAR\\s+CACHE\\s*)"),
    DESCRIBE("describe", "((DESCRIBE|DESC)\\s+.+)"),
    LIST_FILE("list file", "(LIST\\s+(FILE|FILES)\\s+.+)"),
    LIST_JAR("list jar", "(LIST\\s+(JAR|JARS)\\s+.+)"),
    REFRESH("refresh", "(REFRESH\\s+.+)"),
    SET("set", "(SET(|\\\\s+.+))"),
    RESET("reset", "RESET\\\\s*(.*)?"),
    SHOW_COLUMNS("show columns", "(SHOW\\s+COLUMNS\\s+.+)"),
    SHOW_CREATE_TABLE("show create table", "(SHOW\\s+CREATE\\s+TABLE\\s+.+)"),
    SHOW_DATABASES("show databases", "(SHOW\\s+(DATABASES|SCHEMAS)\\s+.+)"),
    SHOW_FUNCTIONS("show functions", "(SHOW\\s+(USER|SYSTEM|ALL|)\\s+FUNCTIONS\\s+.+)"),
    SHOW_PARTITIONS("show partitions", "(SHOW\\s+PARTITIONS\\s+.+)"),
    SHOW_TABLE_EXTENDED("show table extended", "(SHOW\\s+TABLE\\s+EXTENDED\\s+.+)"),
    SHOW_TABLES("show tables", "(SHOW\\s+TABLES\\s+.+)"),
    SHOW_TBLPROPERTIES("show tblproperties", "(SHOW\\s+TBLPROPERTIES\\s+.+)"),
    SHOW_VIEWS("show views", "(SHOW\\s+VIEWS\\s.+)"),;

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

    public static SqlCommand get(String stmt) {
        for (SqlCommand command : values()) {
            if (command.matches(stmt)) {
                return command;
            }
        }
        return null;
    }
}
