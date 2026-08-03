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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.PrintStream;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ParameterCli {

    private static final String PROPERTY_PREFIX = ConfigKeys.KEY_FLINK_PROPERTY_PREFIX();
    private static final String OPTION_PREFIX = ConfigKeys.KEY_FLINK_OPTION_PREFIX();
    private static final String OPTION_MAIN = PROPERTY_PREFIX + "$internal.application.main";

    private static final Options FLINK_OPTIONS = FlinkRunOption.allOptions();
    private static final DefaultParser PARSER = new DefaultParser();

    private ParameterCli() {
    }

    public static void main(String[] args) {
        emit(read(args), System.out);
    }

    static void emit(String output, PrintStream out) {
        out.print(output);
    }

    public static String read(String[] args) {
        if ("--vmopt".equals(args[0])) {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            if (loader instanceof URLClassLoader) {
                return "";
            }
            return "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED "
                + "--add-opens jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED";
        }
        String action = args[0];
        String conf = args[1];
        Map<String, String> map = loadConfig(conf);
        String[] programArgs = new String[args.length - 2];
        System.arraycopy(args, 2, programArgs, 0, programArgs.length);
        switch (action) {
            case "--option":
                return buildOption(map, programArgs);
            case "--property":
                return buildProperty(map);
            case "--name":
                return map.getOrDefault(
                    PROPERTY_PREFIX + ConfigKeys.KEY_FLINK_APP_NAME(), "").trim();
            case "--detached":
                return buildDetachedMode(map, programArgs);
            default:
                return null;
        }
    }

    private static Map<String, String> loadConfig(String conf) {
        try {
            String extension = conf.substring(conf.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
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
            throw new IllegalArgumentException(
                "[StreamPark] Failed to load flink config file: " + conf, e);
        }
    }

    private static String buildOption(Map<String, String> map, String[] programArgs) {
        String[] option = getOption(map, programArgs);
        StringBuilder buffer = new StringBuilder();
        try {
            CommandLine line = PARSER.parse(FLINK_OPTIONS, option, false);
            for (org.apache.commons.cli.Option x : line.getOptions()) {
                buffer.append(" -").append(x.getOpt());
                if (x.hasArg()) {
                    buffer.append(" ").append(x.getValue());
                }
            }
        } catch (ParseException exception) {
            // Ignore unrecognized CLI tokens; valid options are still collected below.
        }
        String mainClass = map.get(OPTION_MAIN);
        if (mainClass != null) {
            buffer.append(" -c ").append(mainClass);
        }
        return buffer.toString().trim();
    }

    private static String buildProperty(Map<String, String> map) {
        StringBuilder buffer = new StringBuilder();
        map.entrySet().stream()
            .filter(
                x -> !OPTION_MAIN.equals(x.getKey())
                    && x.getKey().startsWith(PROPERTY_PREFIX)
                    && x.getValue() != null
                    && !x.getValue().isEmpty())
            .forEach(
                x -> {
                    String key = x.getKey().substring(PROPERTY_PREFIX.length()).trim();
                    String value = x.getValue().trim();
                    if (ConfigKeys.KEY_FLINK_APP_NAME().equals(key)) {
                        buffer.append(" -D").append(key).append('=').append(value.replace(' ', '_'));
                    } else {
                        buffer.append(" -D").append(key).append('=').append(value);
                    }
                });
        return buffer.toString().trim();
    }

    private static String buildDetachedMode(Map<String, String> map, String[] programArgs) {
        try {
            String[] option = getOption(map, programArgs);
            CommandLine line = PARSER.parse(FlinkRunOption.allOptions(), option, false);
            boolean detached =
                line.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt())
                    || line.hasOption(FlinkRunOption.DETACHED_OPTION.getLongOpt());
            return detached ? "Detached" : "Attach";
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse Flink detached mode options", e);
        }
    }

    public static String[] getOption(Map<String, String> map, String[] args) {
        Map<String, Object> optionMap = new LinkedHashMap<>();
        mergeConfigOptions(map, optionMap);
        mergeProgramOptions(args, optionMap);
        return flattenOptions(optionMap);
    }

    private static void mergeConfigOptions(Map<String, String> map, Map<String, Object> optionMap) {
        map.entrySet().stream()
            .filter(x -> x.getKey().startsWith(OPTION_PREFIX))
            .filter(x -> x.getValue() != null && !x.getValue().isEmpty())
            .filter(
                x -> {
                    String key = x.getKey().substring(OPTION_PREFIX.length());
                    return FLINK_OPTIONS.hasOption(key);
                })
            .forEach(
                x -> {
                    String optKey = "-" + x.getKey().substring(OPTION_PREFIX.length()).trim();
                    Object value = parseOptionValue(x.getValue());
                    if (value instanceof Boolean && Boolean.TRUE.equals(value)) {
                        optionMap.put(optKey, true);
                    } else if (!(value instanceof Boolean)) {
                        optionMap.put(optKey, value);
                    }
                });
    }

    private static void mergeProgramOptions(String[] args, Map<String, Object> optionMap) {
        if (args.length == 0) {
            return;
        }
        try {
            CommandLine line = PARSER.parse(FLINK_OPTIONS, args, false);
            for (org.apache.commons.cli.Option x : line.getOptions()) {
                if (x.hasArg()) {
                    optionMap.put("-" + x.getLongOpt().trim(), x.getValue());
                } else {
                    optionMap.put("-" + x.getLongOpt().trim(), true);
                }
            }
        } catch (ParseException e) {
            // Ignore unrecognized CLI tokens merged from program arguments.
        }
    }

    private static String[] flattenOptions(Map<String, Object> optionMap) {
        List<String> array = new ArrayList<>();
        optionMap.forEach(
            (key, value) -> {
                array.add(key);
                if (value instanceof String) {
                    array.add(value.toString());
                }
            });
        return array.toArray(new String[0]);
    }

    private static Object parseOptionValue(String raw) {
        if ("true".equalsIgnoreCase(raw) || "false".equalsIgnoreCase(raw)) {
            return Boolean.parseBoolean(raw);
        }
        return raw;
    }
}
