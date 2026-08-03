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

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Flink configuration parsing utilities. */
public final class FlinkConfigurationUtils {

    private static final Logger LOG =
        StreamParkLoggerFactory.loggerFactory()
            .getLogger(FlinkConfigurationUtils.class.getName());

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("(.*?)=(.*?)");

    private static final Pattern MULTI_PROPERTY_PATTERN =
        Pattern.compile("-D(.*?)\\s*=\\s*([\"'])(.*)\\2");

    private FlinkConfigurationUtils() {
    }

    public static Map<String, String> loadFlinkConf(File file) {
        AssertUtils.required(
            file != null && file.exists() && file.isFile(),
            "[StreamPark] loadFlinkConfYaml: file must not be null");
        try {
            return loadFlinkConf(
                org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> loadFlinkConf(String yaml) {
        AssertUtils.required(
            yaml != null && !yaml.isEmpty(), "[StreamPark] loadFlinkConfYaml: yaml must not be null");
        return new HashMap<>(PropertiesUtils.fromYamlText(yaml));
    }

    public static Map<String, String> loadLegacyFlinkConf(File file) {
        AssertUtils.required(
            file != null && file.exists() && file.isFile(),
            "[StreamPark] loadFlinkConfYaml: file must not be null");
        try {
            return loadLegacyFlinkConf(
                org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> loadLegacyFlinkConf(String yaml) {
        AssertUtils.required(
            yaml != null && !yaml.isEmpty(), "[StreamPark] loadFlinkConfYaml: yaml must not be null");
        Map<String, String> flinkConf = new LinkedHashMap<>();
        java.util.Scanner scanner = new java.util.Scanner(yaml);
        AtomicInteger lineNo = new AtomicInteger(0);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            lineNo.incrementAndGet();
            int commentStart = line.indexOf('#');
            String conf = (commentStart >= 0 ? line.substring(0, commentStart) : line).trim();
            if (!conf.isEmpty()) {
                String[] kv = conf.split(": ", 2);
                if (kv.length == 2) {
                    String key = kv[0].trim();
                    String value = kv[1].trim();
                    if (!key.isEmpty() && !value.isEmpty()) {
                        flinkConf.put(key, value);
                    } else {
                        LOG.warn(
                            "[StreamPark] Error after splitting key and value in configuration {}: {}",
                            lineNo.get(),
                            line);
                    }
                } else {
                    LOG.warn(
                        "[StreamPark] Error while trying to split key and value in configuration. {} : {}",
                        lineNo.get(),
                        line);
                }
            }
        }
        scanner.close();
        return flinkConf;
    }

    @Nonnull
    public static Map<String, String> extractDynamicProperties(String properties) {
        if (StringUtils.isEmpty(properties)) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        String simple = MULTI_PROPERTY_PATTERN.matcher(properties).replaceAll("");
        String[] parts = simple.split("\\s?-D");
        if (Utils.isNotEmpty(parts)) {
            for (String x : parts) {
                if (!x.isEmpty()) {
                    Matcher p = PROPERTY_PATTERN.matcher(x.trim());
                    if (p.matches()) {
                        map.put(p.group(1).trim(), p.group(2).trim());
                    }
                }
            }
        }
        Matcher matcher = MULTI_PROPERTY_PATTERN.matcher(properties);
        while (matcher.find()) {
            map.put(matcher.group(1).trim(), matcher.group(3));
        }
        return map;
    }

    @Nonnull
    public static List<String> extractArguments(String args) {
        if (StringUtils.isNotEmpty(args)) {
            return extractArguments(args.split("\\s+"));
        }
        return new ArrayList<>();
    }

    public static List<String> extractArguments(String[] array) {
        List<String> programArgs = new ArrayList<>();
        Iterator<String> iter = Arrays.asList(array).iterator();
        while (iter.hasNext()) {
            String v = iter.next();
            String p = v.substring(0, 1);
            if ("'".equals(p) || "\"".equals(p)) {
                String value = v;
                if (!v.endsWith(p)) {
                    while (!value.endsWith(p) && iter.hasNext()) {
                        value += " " + iter.next();
                    }
                }
                programArgs.add(value.substring(1, value.length() - 1));
            } else {
                int eqIndex = v.indexOf('=');
                if (eqIndex > 0 && v.length() > 2) {
                    char open = v.charAt(0);
                    char close = v.charAt(v.length() - 1);
                    if ((open == '\'' && close == '\'') || (open == '"' && close == '"')) {
                        programArgs.add(v.substring(1, v.length() - 1));
                    } else if (eqIndex > 1 && v.charAt(eqIndex - 1) == '\'' && v.endsWith("'")) {
                        programArgs.add(v.substring(0, eqIndex - 1) + "=" + v.substring(eqIndex + 1, v.length() - 1));
                    } else if (eqIndex > 1 && v.charAt(eqIndex - 1) == '"' && v.endsWith("\"")) {
                        programArgs.add(v.substring(0, eqIndex - 1) + "=" + v.substring(eqIndex + 1, v.length() - 1));
                    } else {
                        programArgs.add(v);
                    }
                } else {
                    programArgs.add(v);
                }
            }
        }
        return programArgs;
    }

    public static Map<String, Map<String, String>> extractMultipleArguments(String[] array) {
        Iterator<String> iter = Arrays.asList(array).iterator();
        Map<String, Map<String, String>> map = new LinkedHashMap<>();
        while (iter.hasNext()) {
            String v = iter.next();
            if (v.length() >= 2 && v.startsWith("--")) {
                String kv = iter.next();
                int eqIndex = kv.indexOf('=');
                if (eqIndex > 0) {
                    String[] values = new String[]{kv.substring(0, eqIndex), kv.substring(eqIndex + 1)};
                    String k1 = values[0].trim();
                    String v1 = stripOuterQuotes(values[1].trim());
                    String k = v.substring(2);
                    map.computeIfAbsent(k, key -> new LinkedHashMap<>()).put(k1, v1);
                }
            }
        }
        return map;
    }

    @Nonnull
    public static Map<String, String> extractDynamicPropertiesAsJava(String properties) {
        return new HashMap<>(extractDynamicProperties(properties));
    }

    @Nonnull
    public static Map<String, Map<String, String>> extractMultipleArgumentsAsJava(String[] args) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Map<String, String>> entry : extractMultipleArguments(args).entrySet()) {
            result.put(entry.getKey(), new HashMap<>(entry.getValue()));
        }
        return result;
    }

    private static String stripOuterQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
