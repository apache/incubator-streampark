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

package org.apache.streampark.common.util;

import org.apache.streampark.shaded.org.slf4j.Logger;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** SQL dialect conversion utilities. */
public final class SqlConvertUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory().getLogger(SqlConvertUtils.class.getName());

    private static final Pattern FIELD_REGEXP =
        Pattern.compile(
            "\\s*(.*?)\\s+(([a-z]+)\\((.*?)\\)|[a-z]+)(\\s*|((.*?)(comment)\\s+(['|\"](.*?)['|\"])|(.*?))),$",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern PRIMARY_REGEXP =
        Pattern.compile("primary\\s+key\\s+\\((.*?)\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern COMMENT_REGEXP =
        Pattern.compile("(comment)\\s+(['\"])", Pattern.CASE_INSENSITIVE);

    private static final Pattern LENGTH_REGEXP = Pattern.compile("(.*?)\\s*\\([^\\\\)|^\\n]+,$");

    private SqlConvertUtils() {
    }

    private static String toFlinkDataType(String dataType, String length) {
        switch (dataType.toUpperCase()) {
            case "TEXT":
            case "LONGTEXT":
                return "VARCHAR";
            case "DATETIME":
                return "TIMESTAMP";
            case "INT":
                return "INTEGER";
            default:
                return dataType.toUpperCase();
        }
    }

    private static String toClickhouseDataType(String dataType, String length) {
        switch (dataType.toUpperCase()) {
            case "TEXT":
            case "LONGTEXT":
            case "BLOB":
            case "VARCHAR":
            case "VARBINARY":
                return "String";
            case "DATETIME":
            case "TIMESTAMP":
                return "DateTime";
            case "TINYINT":
                return "Int8";
            case "SMALLINT":
                return "Int16";
            case "FLOAT":
                return "Float32";
            case "DOUBLE":
                return "Float64";
            case "DATE":
                return "Date";
            case "BIGINT":
            case "INT":
                if (length == null) {
                    return "Int32";
                }
                int len = Integer.parseInt(length.trim());
                if (len < 3) {
                    return "Int8";
                } else if (len < 5) {
                    return "Int16";
                } else if (len < 9) {
                    return "Int32";
                }
                return "Int64";
            case "DECIMAL":
                String[] parts = length.split(",");
                if (parts.length == 2) {
                    int p = Integer.parseInt(parts[0].trim());
                    int s = Integer.parseInt(parts[1].trim());
                    int sum = p + s;
                    if (sum <= 32) {
                        return "Decimal32(" + s + ")";
                    } else if (sum <= 64) {
                        return "Decimal64(" + s + ")";
                    }
                    return "Decimal128(" + s + ")";
                }
                throw new IllegalArgumentException("Invalid decimal length: " + length);
            default:
                return dataType.toUpperCase();
        }
    }

    private static String formatSql(String sql) {
        String body =
            sql.substring(sql.indexOf('('), sql.lastIndexOf(')') + 1)
                .replaceAll("\r|\n|\r\n", "")
                .replaceFirst("\\(", "(\n")
                .replaceFirst("\\)$", "\n)")
                .replaceAll(",", ",\n");

        Scanner scanner = new Scanner(body);
        Map<Integer, String> map = new HashMap<>();
        while (scanner.hasNextLine()) {
            map.put(map.size(), scanner.nextLine().trim());
        }
        StringBuilder sqlBuffer = new StringBuilder(sql.substring(0, sql.indexOf('(')));
        int skipNo = -1;
        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            int idx = entry.getKey();
            if (idx > skipNo) {
                AbstractMap.SimpleEntry<Integer, String> length = lengthJoin(map, idx, entry.getValue());
                if (length.getKey() > idx) {
                    sqlBuffer.append(length.getValue()).append('\n');
                    skipNo = length.getKey();
                } else {
                    AbstractMap.SimpleEntry<Integer, String> comment = commentJoin(map, idx, entry.getValue());
                    if (comment.getKey() > idx) {
                        sqlBuffer.append(comment.getValue()).append('\n');
                        skipNo = comment.getKey();
                    } else {
                        sqlBuffer.append(entry.getValue()).append('\n');
                    }
                }
            }
        }
        scanner.close();
        return sqlBuffer.toString().trim().concat(sql.substring(sql.lastIndexOf(')') + 1));
    }

    private static AbstractMap.SimpleEntry<Integer, String> commentJoin(
                                                                        Map<Integer, String> map, int index,
                                                                        String segment) {
        Matcher matcher = COMMENT_REGEXP.matcher(segment);
        if (!matcher.find()) {
            return new AbstractMap.SimpleEntry<>(index, segment);
        }
        String quote = matcher.group(2);
        String regexp = "\\" + quote + "(,|)$";
        String cleaned =
            segment.replaceFirst("(?i)(comment)\\s+(['\"])", "").replace("\\" + quote, "");
        if (cleaned.matches(".*" + regexp)) {
            return new AbstractMap.SimpleEntry<>(index, segment);
        }
        String nextLine = map.get(index + 1);
        return commentJoin(map, index + 1, segment + nextLine);
    }

    private static AbstractMap.SimpleEntry<Integer, String> lengthJoin(
                                                                       Map<Integer, String> map, int index,
                                                                       String segment) {
        if (!LENGTH_REGEXP.matcher(segment).find()) {
            return commentJoin(map, index, segment);
        }
        String nextLine = map.get(index + 1);
        return lengthJoin(map, index + 1, segment.trim() + nextLine);
    }

    private interface TypeConverter {

        String convert(String dataType, String length);
    }

    private interface KeyConverter {

        String convert(String line);
    }

    private static String convertSql(
                                     String sql, TypeConverter typeFunc, KeyConverter keyFunc, String postfix) {
        String formattedSql = formatSql(sql);
        Scanner scanner = new Scanner(formattedSql);
        StringBuilder sqlBuffer = new StringBuilder();
        while (scanner.hasNextLine()) {
            String rawLine = scanner.nextLine().trim();
            String line = null;
            String upper = rawLine.toUpperCase().trim();
            if (upper.startsWith("CREATE ")) {
                line = rawLine;
            } else if (upper.startsWith("PRIMARY KEY ")) {
                if (keyFunc != null) {
                    line = keyFunc.convert(rawLine);
                }
            } else if (!upper.startsWith("UNIQUE KEY ") && !upper.startsWith("KEY ")) {
                Matcher matcher = FIELD_REGEXP.matcher(rawLine);
                if (matcher.find()) {
                    String fieldName = matcher.group(1);
                    String dataType;
                    if (matcher.group(3) != null) {
                        dataType = matcher.group(3);
                    } else {
                        dataType = matcher.group(2);
                    }
                    String length = matcher.group(4);
                    if (dataType != null) {
                        String fieldType = typeFunc.convert(dataType, length);
                        if (matcher.group(8) != null) {
                            line = fieldName + " " + fieldType + " COMMENT '" + matcher.group(10) + "'";
                        } else {
                            line = fieldName + " " + fieldType;
                        }
                    }
                }
            }
            if (line != null) {
                sqlBuffer.append(line);
                if (line.toUpperCase().trim().startsWith("CREATE ")) {
                    sqlBuffer.append('\n');
                } else {
                    sqlBuffer.append(",\n");
                }
            }
        }
        scanner.close();
        String result = sqlBuffer.toString().trim().replaceAll(",$", "\n)");
        if (postfix != null) {
            return result + " " + postfix;
        }
        return result;
    }

    public static String mysqlToFlinkSql(String sql, String postfix) {
        return convertSql(
            sql,
            SqlConvertUtils::toFlinkDataType,
            line -> {
                Matcher matcher = PRIMARY_REGEXP.matcher(line);
                if (matcher.find()) {
                    return matcher.group() + " NOT ENFORCED";
                }
                return null;
            },
            postfix);
    }

    public static String mysqlToClickhouse(String sql, String postfix) {
        return convertSql(sql, SqlConvertUtils::toClickhouseDataType, null, postfix);
    }
}
