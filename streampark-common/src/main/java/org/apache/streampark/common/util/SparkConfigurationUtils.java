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

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;

import javax.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public final class SparkConfigurationUtils {

    private static final Pattern SPARK_PROPERTY_COMPLEX_PATTERN =
        Pattern.compile("^[\"']?([^=]+)=(.*)[\"']?$");

    private SparkConfigurationUtils() {
    }

    @Nonnull
    public static Map<String, String> extractPropertiesAsJava(String properties) {
        return new HashMap<>(extractProperties(properties));
    }

    @Nonnull
    public static Map<String, String> extractProperties(String properties) {
        if (StringUtils.isEmpty(properties)) {
            return new HashMap<>();
        }
        Map<String, String> map = new HashMap<>();
        for (String x : splitSparkConfSegments(properties)) {
            if (Utils.isNotEmpty(x) && !x.isEmpty()) {
                java.util.regex.Matcher p = SPARK_PROPERTY_COMPLEX_PATTERN.matcher(x);
                if (p.matches()) {
                    map.put(p.group(1).trim(), p.group(2).trim());
                }
            }
        }
        return map;
    }

    @Nonnull
    public static List<String> extractArgumentsAsJava(String arguments) {
        if (StringUtils.isEmpty(arguments)) {
            return Lists.newArrayList();
        }
        List<String> result = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean inQuote = false;
        char quoteChar = 0;
        for (int i = 0; i < arguments.length(); i++) {
            char c = arguments.charAt(i);
            if (inQuote) {
                token.append(c);
                if (c == quoteChar) {
                    inQuote = false;
                }
            } else if (c == '"' || c == '\'') {
                inQuote = true;
                quoteChar = c;
                token.append(c);
            } else if (Character.isWhitespace(c)) {
                if (token.length() > 0) {
                    result.add(token.toString());
                    token.setLength(0);
                }
            } else {
                token.append(c);
            }
        }
        if (token.length() > 0) {
            result.add(token.toString());
        }
        return result;
    }

    private static List<String> splitSparkConfSegments(String properties) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int i = 0;
        while (i < properties.length()) {
            if (startsWithAt(properties, i, "--conf")) {
                flushSegment(segments, current);
                i += 6;
            } else if (startsWithAt(properties, i, "-c")
                && (i + 2 >= properties.length() || Character.isWhitespace(properties.charAt(i + 2)))) {
                flushSegment(segments, current);
                i += 2;
            } else {
                current.append(properties.charAt(i++));
            }
        }
        flushSegment(segments, current);
        return segments;
    }

    private static boolean startsWithAt(String value, int index, String token) {
        if (index + token.length() > value.length()) {
            return false;
        }
        return value.regionMatches(index, token, 0, token.length());
    }

    private static void flushSegment(List<String> segments, StringBuilder current) {
        if (current.length() > 0) {
            segments.add(current.toString().trim());
            current.setLength(0);
        }
    }
}
