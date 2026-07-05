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

import java.net.URLClassLoader;
import java.util.ArrayList;
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

    public static void main(String[] args) {
        System.out.print(read(args));
    }

    public static String read(String[] args) {
        switch (args[0]) {
            case "--vmopt":
                ClassLoader classLoader = ClassLoader.getSystemClassLoader();
                if (classLoader instanceof URLClassLoader) {
                    return "";
                }
                return "--add-opens java.base/jdk.internal.loader=ALL-UNNAMED "
                    + "--add-opens jdk.zipfs/jdk.nio.zipfs=ALL-UNNAMED";
            default:
                String action = args[0];
                String conf = args[1];
                Map<String, String> map;
                try {
                    String extension = conf.substring(conf.lastIndexOf('.') + 1).toLowerCase();
                    switch (extension) {
                        case "yml":
                        case "yaml":
                            map = PropertiesUtils.fromYamlFile(conf);
                            break;
                        case "conf":
                            map = PropertiesUtils.fromHoconFile(conf);
                            break;
                        case "properties":
                            map = PropertiesUtils.fromPropertiesFile(conf);
                            break;
                        default:
                            throw new IllegalArgumentException(
                                "[StreamPark] Usage:flink.conf file error,must be (yml|conf|properties)");
                    }
                } catch (Exception e) {
                    map = Collections.emptyMap();
                }
                String[] programArgs = new String[args.length - 2];
                System.arraycopy(args, 2, programArgs, 0, programArgs.length);
                switch (action) {
                    case "--option":
                        String[] option = getOption(map, programArgs);
                        StringBuilder buffer = new StringBuilder();
                        try {
                            org.apache.commons.cli.CommandLine line =
                                PARSER.parse(FLINK_OPTIONS, option, false);
                            for (org.apache.commons.cli.Option x : line.getOptions()) {
                                buffer.append(" -").append(x.getOpt());
                                if (x.hasArg()) {
                                    buffer.append(" ").append(x.getValue());
                                }
                            }
                        } catch (Exception exception) {
                            exception.printStackTrace();
                        }
                        String mainClass = map.get(OPTION_MAIN);
                        if (mainClass != null) {
                            buffer.append(" -c ").append(mainClass);
                        }
                        return buffer.toString().trim();
                    case "--property":
                        StringBuilder propertyBuffer = new StringBuilder();
                        for (Map.Entry<String, String> entry : map.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (!OPTION_MAIN.equals(key)
                                && key.startsWith(PROPERTY_PREFIX)
                                && value != null
                                && !value.isEmpty()) {
                                String propertyKey = key.substring(PROPERTY_PREFIX.length()).trim();
                                String propertyValue = value.trim();
                                if (ConfigKeys.KEY_FLINK_APP_NAME().equals(propertyKey)) {
                                    propertyBuffer
                                        .append(" -D")
                                        .append(propertyKey)
                                        .append("=")
                                        .append(propertyValue.replace(" ", "_"));
                                } else {
                                    propertyBuffer
                                        .append(" -D")
                                        .append(propertyKey)
                                        .append("=")
                                        .append(propertyValue);
                                }
                            }
                        }
                        return propertyBuffer.toString().trim();
                    case "--name":
                        String appName =
                            map.getOrDefault(
                                PROPERTY_PREFIX.concat(ConfigKeys.KEY_FLINK_APP_NAME()), "");
                        appName = appName.trim();
                        return appName.isEmpty() ? "" : appName;
                    case "--detached":
                        String[] detachedOption = getOption(map, programArgs);
                        try {
                            org.apache.commons.cli.CommandLine line =
                                PARSER.parse(FlinkRunOption.allOptions(), detachedOption, false);
                            boolean detached =
                                line.hasOption(FlinkRunOption.DETACHED_OPTION.getOpt())
                                    || line.hasOption(
                                        FlinkRunOption.DETACHED_OPTION.getLongOpt());
                            return detached ? "Detached" : "Attach";
                        } catch (Exception e) {
                            e.printStackTrace();
                            return "Attach";
                        }
                    default:
                        return null;
                }
        }
    }

    public static String[] getOption(Map<String, String> map, String[] args) {
        Map<String, Object> optionMap = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key.startsWith(OPTION_PREFIX) && value != null && !value.isEmpty()) {
                String optionKey = key.substring(OPTION_PREFIX.length());
                if (FLINK_OPTIONS.hasOption(optionKey)) {
                    Object parsedValue;
                    if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                        parsedValue = Boolean.parseBoolean(value);
                    } else {
                        parsedValue = value;
                    }
                    if (parsedValue instanceof Boolean) {
                        if ((Boolean) parsedValue) {
                            optionMap.put("-" + optionKey.trim(), true);
                        }
                    } else {
                        optionMap.put("-" + optionKey.trim(), parsedValue);
                    }
                }
            }
        }
        if (args.length > 0) {
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
                e.printStackTrace();
            }
        }
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
