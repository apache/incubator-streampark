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
package org.apache.streampark;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** some simple tests */
class RegExpTest {

    /**
     * Case insensitive, matches everything as one line, . matches newlines, note: \s matches any
     * whitespace character, including newlines
     */
    public static final int DEFAULT_PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.DOTALL;

    /**
     * CREATE CATALOG catalog_name WITH (key1=val1, key2=val2, ...)<br>
     * Example：create catalog hive_catalog with('name' = 'my_hive', 'conf' = '/home/hive/conf')
     */
    private static final Pattern CREATE_HIVE_CATALOG = Pattern.compile("CREATE\\s+CATALOG\\s+.+",
        DEFAULT_PATTERN_FLAGS);

    @Test
    void testCreateHiveCatalog() {
        String str = "create catalog hive with (\n"
            + "    'type' = 'hive',\n"
            + "    'hadoop-conf-dir' = 'D:\\IDEAWorkspace\\work\\baishan\\log\\data-max\\src\\main\\resources',\n"
            + "    'hive-conf-dir' = 'D:\\IDEAWorkspace\\work\\baishan\\log\\data-max\\src\\main\\resources'\n"
            + ")";
        Matcher matcher = CREATE_HIVE_CATALOG.matcher(str);
        System.out.println(matcher.matches());
    }

    /**
     * CREATE [TEMPORARY|TEMPORARY SYSTEM] FUNCTION [IF NOT EXISTS]
     * [catalog_name.][db_name.]function_name AS identifier [LANGUAGE JAVA|SCALA|PYTHON]<br>
     * Example：create function test_fun as com.flink.testFun
     */
    private static final Pattern CREATE_FUNCTION = Pattern.compile(
        "(CREATE\\s+(TEMPORARY\\s+|TEMPORARY\\s+SYSTEM\\s+|)FUNCTION\\s+(IF NOT EXISTS\\s+|)([A-Za-z]+[A-Za-z\\d.\\-_]+)\\s+AS\\s+'([A-Za-z].+)'\\s+LANGUAGE\\s+(JAVA|SCALA|PYTHON)\\s*)",
        DEFAULT_PATTERN_FLAGS);

    @Test
    void testCreateFunction() {
        String str =
            "create   function if not exists hive.get_json_value as 'com.flink.function.JsonValueFunction' language java";
        Matcher matcher = CREATE_FUNCTION.matcher(str);
        System.out.println(matcher.matches());
    }

    /** USE [catalog_name.]database_name */
    private static final Pattern USE_DATABASE = Pattern.compile("USE\\s+(?!(CATALOG|MODULES)).*",
        DEFAULT_PATTERN_FLAGS);

    @Test
    void testUseDatabase() {
        String str = "use modul.a ";
        Matcher matcher = USE_DATABASE.matcher(str);
        System.out.println(matcher.matches());
    }

    /** SHOW [USER] FUNCTIONS */
    private static final Pattern SHOW_FUNCTIONS = Pattern.compile("SHOW\\s+(USER\\s+|)FUNCTIONS",
        DEFAULT_PATTERN_FLAGS);

    @Test
    void testShowFunction() {
        String str = "show user functions";
        Matcher matcher = SHOW_FUNCTIONS.matcher(str);
        System.out.println(matcher.matches());
    }
}
