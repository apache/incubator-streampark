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

package org.apache.streampark.flink.core.conf;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.PropertiesUtils;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Parses Flink application CLI parameters from configuration files. */
public final class ParameterCli {

    private static final String PROPERTY_PREFIX = ConfigKeys.KEY_FLINK_PROPERTY_PREFIX();
    private static final String OPTION_PREFIX = ConfigKeys.KEY_FLINK_OPTION_PREFIX();
    private static final String OPTION_MAIN = PROPERTY_PREFIX + "$internal.application.main";

    private static final Options FLINK_OPTIONS = FlinkRunOption.allOptions();
    private static final DefaultParser PARSER = new DefaultParser();

    private ParameterCli() {
    }

    public static void emit(String output, PrintStream out) {
        out.print(output);
    }

    public static String read(String[] args) {
        if ("--vmopt".equals(args[0])) {
            return readVmOpt();
        }
        return readConfigAction(args[0], args[1], Arrays.copyOfRange(args, 2, args.length));
    }

    private static String readVmOpt() {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        if (classLoader instanceof URLClassLoader) {
            return "";
        }
        return "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED "
            + "--add-opens jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED";
    }

    private static String readConfigAction(String action, String conf, String[] programArgs) {
        Map<String, String> map = loadConfigMap(conf);
        switch (action) {
            case "--option":
                return buildOptionString(map, programArgs);
            case "--property":
                return buildPropertyString(map);
            case "--name":
                return readAppName(map);
            case "--detached":
                return readDetachedMode(map, programArgs);
            default:
                return null;
        }
    }

    private static Map<String, String> loadConfigMap(String conf) {
        try {
            String extension = conf.substring(conf.lastIndexOf('.') + 1).toLowerCase();
            switch (extension) {
                case "yml":
                case "yaml":
                    return PropertiesUtils.fromYamlFile(conf);
                case "conf":
                    return PropertiesUtils.fromHoconFile(conf);
                case "properties":
                    return PropertiesUtils.fromPropertiesFile(conf);
                default:
                    throw new IllegalArgumentException(
                        "[StreamPark] Usage:flink.conf file error,must be (yml|conf|properties)");
            }
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static String buildOptionString(Map<String, String> map, String[] programArgs) {
        String[] option = getOption(map, programArgs);
        StringBuilder buffer = new StringBuilder();
        try {
            org.apache.commons.cli.CommandLine line = PARSER.parse(FLINK_OPTIONS, option, false);
            for (org.apache.commons.cli.Option x : line.getOptions()) {
                buffer.append(" -").append(x.getOpt());
                if (x.hasArg()) {
                    buffer.append(" ").append(x.getValue());
                }
            }
        } catch (Exception exception) {
            // Ignore invalid CLI options and continue with parsed values.
        }
        String mainClass = map.get(OPTION_MAIN);
        if (mainClass != null) {
            buffer.append(" -c ").append(mainClass);
        }
        return buffer.toString().trim();
    }

    private static String buildPropertyString(Map<String, String> map) {
        StringBuilder propertyBuffer = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            appendPropertyEntry(propertyBuffer, entry.getKey(), entry.getValue());
        }
        return propertyBuffer.toString().trim();
    }

    private static void appendPropertyEntry(StringBuilder propertyBuffer, String key, String value) {
        if (OPTION_MAIN.equals(key)
            || !key.startsWith(PROPERTY_PREFIX)
            || value == null
            || value.isEmpty()) {
            return;
        }
        String propertyKey = key.substring(PROPERTY_PREFIX.length()).trim();
        String propertyValue = value.trim();
        propertyBuffer.append(" -D").append(propertyKey).append("=");
        if (ConfigKeys.KEY_FLINK_APP_NAME().equals(propertyKey)) {
            propertyBuffer.append(propertyValue.replace(" ", "_"));
        } else {
            propertyBuffer.append(propertyValue);
        }
    }

    private static String readAppName(Map<String, String> map) {
        String appName =
            map.getOrDefault(PROPERTY_PREFIX.concat(ConfigKeys.KEY_FLINK_APP_NAME()), "");
        appName = appName.trim();
        return appName.isEmpty() ? "" : appName;
    }

    private static String readDetachedMode(Map<String, String> map, String[] programArgs) {
        String[] detachedOption = getOption(map, programArgs);
        try {
            org.apache.commons.cli.CommandLine line =
                PARSER.parse(FlinkRunOption.allOptions(), detachedOption, false);
            boolean detached =
                line.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt())
                    || line.hasOption(FlinkRunOption.DETACHED_OPTION.getLongOpt());
            return detached ? "Detached" : "Attach";
        } catch (Exception e) {
            return "Attach";
        }
    }

    public static String[] getOption(Map<String, String> map, String[] args) {
        Map<String, Object> optionMap = collectConfiguredOptions(map);
        mergeProgramArgs(optionMap, args);
        return toOptionArray(optionMap);
    }

    private static Map<String, Object> collectConfiguredOptions(Map<String, String> map) {
        Map<String, Object> optionMap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            putConfiguredOption(optionMap, entry.getKey(), entry.getValue());
        }
        return optionMap;
    }

    private static void putConfiguredOption(Map<String, Object> optionMap, String key, String value) {
        if (!key.startsWith(OPTION_PREFIX) || value == null || value.isEmpty()) {
            return;
        }
        String optionKey = key.substring(OPTION_PREFIX.length());
        if (!FLINK_OPTIONS.hasOption(optionKey)) {
            return;
        }
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            if (Boolean.parseBoolean(value)) {
                optionMap.put("-" + optionKey.trim(), true);
            }
            return;
        }
        optionMap.put("-" + optionKey.trim(), value);
    }

    private static void mergeProgramArgs(Map<String, Object> optionMap, String[] args) {
        if (args.length == 0) {
            return;
        }
        try {
            org.apache.commons.cli.CommandLine line = PARSER.parse(FLINK_OPTIONS, args, false);
            for (org.apache.commons.cli.Option x : line.getOptions()) {
                if (x.hasArg()) {
                    optionMap.put("-" + x.getLongOpt().trim(), x.getValue());
                } else {
                    optionMap.put("-" + x.getLongOpt().trim(), true);
                }
            }
        } catch (Exception e) {
            // Ignore invalid CLI options merged from program args.
        }
    }

    private static String[] toOptionArray(Map<String, Object> optionMap) {
        List<String> array = new ArrayList<>();
        for (Map.Entry<String, Object> entry : optionMap.entrySet()) {
            array.add(entry.getKey());
            if (entry.getValue() instanceof String) {
                array.add(entry.getValue().toString());
            }
        }
        return array.toArray(new String[0]);
    }
}
