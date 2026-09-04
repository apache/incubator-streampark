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

package org.apache.streampark.console.core.util;

import org.apache.streampark.common.configuration.ConfigException;
import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.FlinkRunOption;

import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

import java.io.PrintStream;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Builds the Flink shell fragments consumed by StreamPark's launcher scripts.
 *
 * <p>Configuration parsing failures and invalid actions are reported to the caller. Values are
 * shell-quoted when they contain characters outside a conservative safe set, so configuration
 * content cannot introduce additional shell tokens.
 *
 * <p>Stored options form the base launcher command. Explicit program arguments are parsed with the
 * same Commons CLI schema and replace stored values by long option name, ensuring that one option
 * is emitted at most once.
 */
public final class FlinkShellCommandBuilder {

    private static final Pattern SAFE_SHELL_TOKEN =
        Pattern.compile("[A-Za-z0-9_./:@%+=,-]+");
    private static final String PROPERTY_PREFIX = FlinkOptions.PROPERTY_PREFIX.value();
    private static final String OPTION_PREFIX = FlinkOptions.OPTION_PREFIX.value();
    private static final String OPTION_MAIN =
        PROPERTY_PREFIX + FlinkOptions.APPLICATION_MAIN_CLASS.key();

    private static final Options FLINK_OPTIONS = FlinkRunOption.allOptions();
    private static final DefaultParser PARSER = new DefaultParser();

    private FlinkShellCommandBuilder() {
    }

    /**
     * Writes a generated shell fragment without adding a line separator.
     *
     * @param output generated shell fragment
     * @param out destination stream
     */
    public static void emit(String output, PrintStream out) {
        out.print(output);
    }

    /**
     * Executes one launcher query such as {@code --property <config-file>}.
     *
     * @param args launcher action, configuration path, and optional program arguments
     * @return generated shell fragment or requested scalar value
     * @throws ConfigException when the action, configuration, or launcher arguments are invalid
     */
    public static String read(String[] args) {
        if (args == null || args.length == 0) {
            throw new ConfigException("A launcher action is required");
        }
        if ("--vmopt".equals(args[0])) {
            return readVmOpt();
        }
        if (args.length < 2) {
            throw new ConfigException("A configuration file is required for action " + args[0]);
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

    private static String readConfigAction(String action, String configFile, String[] programArgs) {
        Configuration configuration = ConfigurationParser.parse(Path.of(configFile));
        Map<String, String> values = configuration.toMap();
        switch (action) {
            case "--option":
                return buildOptionString(values, programArgs);
            case "--property":
                return buildPropertyString(values);
            case "--name":
                return readAppName(values);
            case "--detached":
                return readDetachedMode(values, programArgs);
            default:
                throw new ConfigException("Unsupported launcher action: " + action);
        }
    }

    private static String buildOptionString(Map<String, String> values, String[] programArgs) {
        StringBuilder buffer = new StringBuilder();
        try {
            org.apache.commons.cli.CommandLine line =
                PARSER.parse(FLINK_OPTIONS, getOption(values, programArgs), false);
            for (org.apache.commons.cli.Option option : line.getOptions()) {
                buffer.append(" -").append(option.getOpt());
                if (option.hasArg()) {
                    buffer.append(" ").append(shellToken(option.getValue()));
                }
            }
        } catch (org.apache.commons.cli.ParseException e) {
            throw new ConfigException("Invalid Flink launcher option", e);
        }
        String mainClass = values.get(OPTION_MAIN);
        if (mainClass != null && !mainClass.isEmpty()) {
            buffer.append(" -c ").append(shellToken(mainClass));
        }
        return buffer.toString().trim();
    }

    private static String buildPropertyString(Map<String, String> values) {
        StringBuilder buffer = new StringBuilder();
        values.forEach((key, value) -> appendPropertyEntry(buffer, key, value));
        return buffer.toString().trim();
    }

    private static void appendPropertyEntry(
                                            StringBuilder buffer,
                                            String key,
                                            String value) {
        if (OPTION_MAIN.equals(key)
            || !FlinkOptions.PROPERTY_PREFIX.matches(key)
            || value == null
            || value.isEmpty()) {
            return;
        }
        String propertyKey = FlinkOptions.PROPERTY_PREFIX.stripFrom(key).trim();
        String propertyValue = value.trim();
        if (FlinkOptions.PIPELINE_NAME.key().equals(propertyKey)) {
            propertyValue = propertyValue.replace(" ", "_");
        }
        buffer
            .append(" -D")
            .append(propertyKey)
            .append("=")
            .append(shellToken(propertyValue));
    }

    private static String readAppName(Map<String, String> values) {
        return values.getOrDefault(PROPERTY_PREFIX + FlinkOptions.PIPELINE_NAME.key(), "").trim();
    }

    private static String readDetachedMode(Map<String, String> values, String[] programArgs) {
        try {
            org.apache.commons.cli.CommandLine line =
                PARSER.parse(FLINK_OPTIONS, getOption(values, programArgs), false);
            boolean detached =
                line.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt())
                    || line.hasOption(FlinkRunOption.DETACHED_OPTION.getLongOpt());
            return detached ? "Detached" : "Attach";
        } catch (org.apache.commons.cli.ParseException e) {
            throw new ConfigException("Invalid Flink detached-mode option", e);
        }
    }

    static String[] getOption(Map<String, String> values, String[] args) {
        Map<String, Object> optionMap = collectConfiguredOptions(values);
        // Program arguments represent the caller's most specific intent and therefore replace
        // values collected from the persisted configuration.
        mergeProgramArgs(optionMap, args);
        return toOptionArray(optionMap);
    }

    private static Map<String, Object> collectConfiguredOptions(Map<String, String> values) {
        Map<String, Object> options = new LinkedHashMap<>();
        values.forEach((key, value) -> putConfiguredOption(options, key, value));
        return options;
    }

    private static void putConfiguredOption(
                                            Map<String, Object> optionMap,
                                            String key,
                                            String value) {
        if (!FlinkOptions.OPTION_PREFIX.matches(key) || value == null || value.isEmpty()) {
            return;
        }
        String optionKey = FlinkOptions.OPTION_PREFIX.stripFrom(key);
        if (!FLINK_OPTIONS.hasOption(optionKey)) {
            throw new ConfigException("Unknown Flink launcher option in configuration: " + optionKey);
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
            for (org.apache.commons.cli.Option option : line.getOptions()) {
                String key = "-" + option.getLongOpt().trim();
                optionMap.put(key, option.hasArg() ? option.getValue() : Boolean.TRUE);
            }
        } catch (org.apache.commons.cli.ParseException e) {
            throw new ConfigException("Invalid program launcher arguments", e);
        }
    }

    private static String[] toOptionArray(Map<String, Object> optionMap) {
        List<String> result = new ArrayList<>();
        optionMap.forEach(
            (key, value) -> {
                result.add(key);
                if (value instanceof String) {
                    result.add(value.toString());
                }
            });
        return result.toArray(new String[0]);
    }

    private static String shellToken(String value) {
        if (SAFE_SHELL_TOKEN.matcher(value).matches()) {
            return value;
        }
        // POSIX shells cannot escape a single quote inside single quotes. Close the quoted region,
        // emit an escaped quote, and reopen it without creating a second argument.
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
