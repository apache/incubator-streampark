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

package org.apache.streampark.plugin.profiling.util;

import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
    private static final int KB_SIZE = 1024;
    private static final int MB_SIZE = 1024 * 1024;
    private static final int GB_SIZE = 1024 * 1024 * 1024;

    private static final int ARGUMENT_VALUE_COUNT_LIMIT = 10000;

    public static List<String> splitByLength(String str, int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("length could not be 0 or less than 0: " + length);
        }

        if (str == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < str.length(); ) {
            int endIndex = Math.min(i + length, str.length());
            String fragment = str.substring(i, endIndex);
            i = endIndex;

            result.add(fragment);
        }

        return result;
    }

    public static List<String> extractByRegex(String str, String regex) {
        List<String> result = new ArrayList<>();

        if (str == null || regex == null) {
            return result;
        }

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            result.add(matcher.group());
        }

        return result;
    }

    public static Long getBytesValueOrNull(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }

        str = str.toLowerCase();
        int scale = 1;

        try {
            if (str.endsWith("kb")) {
                str = str.substring(0, str.length() - 2).trim();
                scale = KB_SIZE;
            }
            if (str.endsWith("k")) {
                str = str.substring(0, str.length() - 1).trim();
                scale = KB_SIZE;
            } else if (str.endsWith("mb")) {
                str = str.substring(0, str.length() - 2).trim();
                scale = MB_SIZE;
            } else if (str.endsWith("m")) {
                str = str.substring(0, str.length() - 1).trim();
                scale = MB_SIZE;
            } else if (str.endsWith("gb")) {
                str = str.substring(0, str.length() - 2).trim();
                scale = GB_SIZE;
            } else if (str.endsWith("g")) {
                str = str.substring(0, str.length() - 1).trim();
                scale = GB_SIZE;
            } else if (str.endsWith("bytes")) {
                str = str.substring(0, str.length() - "bytes".length()).trim();
                scale = 1;
            }

            str = str.replace(",", "");

            if (!NumberUtils.isNumber(str)) {
                return null;
            }

            double doubleValue = Double.parseDouble(str);
            return (long) (doubleValue * scale);
        } catch (Throwable ex) {
            return null;
        }
    }

    public static String getArgumentValue(String str, String argument) {
        if (str == null || str.isEmpty() || argument == null || argument.isEmpty()) {
            return null;
        }

        String[] values = getArgumentValues(str, argument, 1);

        if (values.length == 0) {
            return null;
        }

        return values[0];
    }

    public static String[] getArgumentValues(String str, String argument) {
        return getArgumentValues(str, argument, ARGUMENT_VALUE_COUNT_LIMIT);
    }

    private static String[] getArgumentValues(String str, String argument, int maxCount) {
        if (str == null || str.isEmpty() || argument == null || argument.isEmpty()) {
            return new String[0];
        }

        if (maxCount > ARGUMENT_VALUE_COUNT_LIMIT) {
            throw new RuntimeException("Does not support values more than " + ARGUMENT_VALUE_COUNT_LIMIT);
        }

        List<String> list = new ArrayList<>();

        int startIndex = 0;

        for (int index = str.indexOf(argument, startIndex);
             index >= 0;
             index = str.indexOf(argument, startIndex)) {
            int argumentValueStartIndex = index + argument.length();
            StringValueAndIndex stringValueAndIndex =
                getArgumentValueString(str, argumentValueStartIndex);
            list.add(stringValueAndIndex.str);

            if (stringValueAndIndex.endIndex < 0 || stringValueAndIndex.endIndex >= str.length() - 1) {
                break;
            }

            if (list.size() >= maxCount) {
                break;
            }

            startIndex = stringValueAndIndex.endIndex + 1;

            if (startIndex <= argumentValueStartIndex) {
                break;
            }
        }

        return list.toArray(new String[list.size()]);
    }

    private static StringValueAndIndex getArgumentValueString(String str, int startIndex) {
        if (startIndex >= str.length()) {
            return new StringValueAndIndex("", 0);
        }

        boolean valueStarted = false;
        char endingCharacter = 0;
        StringBuilder sb = new StringBuilder();
        int endingIndex = -1;

        // TODO handle character escape

        for (int i = startIndex; i < str.length(); i++) {
            char ch = str.charAt(i);

            if (!valueStarted) {
                if (Character.isWhitespace(ch)) {
                    continue;
                } else if (ch == '\'' || ch == '"') {
                    valueStarted = true;
                    endingCharacter = ch;
                } else {
                    valueStarted = true;
                    sb.append(ch);
                }
            } else {
                if (Character.isWhitespace(ch)) {
                    if (endingCharacter == 0) {
                        endingIndex = i - 1;
                        break;
                    } else {
                        sb.append(ch);
                    }
                } else if (ch == '\'' || ch == '"') {
                    if (ch == endingCharacter) {
                        endingIndex = i;
                        break;
                    } else {
                        sb.append(ch);
                    }
                } else {
                    sb.append(ch);
                }
            }
        }

        if (!valueStarted) {
            return new StringValueAndIndex("", 0);
        } else {
            if (endingIndex == -1) {
                endingIndex = str.length() - 1;
            }
            return new StringValueAndIndex(sb.toString(), endingIndex);
        }
    }

    static class StringValueAndIndex {
        private final String str;
        private final int endIndex;

        public StringValueAndIndex(String str, int endIndex) {
            this.str = str;
            this.endIndex = endIndex;
        }
    }
}
