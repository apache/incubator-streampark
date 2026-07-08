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

package org.apache.streampark.flink.core;

import org.apache.streampark.common.conf.ConfigKeys;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.core.conf.FlinkConfiguration;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.ParameterTool;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Initializes Flink streaming execution environment from application arguments. */
public class FlinkStreamingInitializerV2 {

    final String[] args;

    StreamEnvConfigFunction javaStreamEnvConfFunc;

    private FlinkConfiguration configuration;

    private StreamExecutionEnvironment streamEnv;

    FlinkStreamingInitializerV2(String[] args) {
        this.args = args;
    }

    public static StreamingInitResult initialize(String[] args, StreamEnvConfigFunction config) {
        FlinkStreamingInitializerV2 flinkInitializer = new FlinkStreamingInitializerV2(args);
        flinkInitializer.javaStreamEnvConfFunc = config;
        return new StreamingInitResult(
            flinkInitializer.getConfiguration().parameter, flinkInitializer.getStreamEnv());
    }

    public static StreamingInitResult initialize(StreamEnvConfig args) {
        FlinkStreamingInitializerV2 flinkInitializer = new FlinkStreamingInitializerV2(args.args);
        flinkInitializer.javaStreamEnvConfFunc = args.conf;
        return new StreamingInitResult(
            flinkInitializer.getConfiguration().parameter, flinkInitializer.getStreamEnv());
    }

    ParameterTool getParameter() {
        return getConfiguration().parameter;
    }

    FlinkConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = initParameter();
        }
        return configuration;
    }

    StreamExecutionEnvironment getStreamEnv() {
        if (streamEnv == null) {
            streamEnv =
                StreamExecutionEnvironment.getExecutionEnvironment(
                    getConfiguration().envConfig);
            if (javaStreamEnvConfFunc != null) {
                javaStreamEnvConfFunc.configuration(streamEnv, getConfiguration().parameter);
            }
            streamEnv.getConfig().setGlobalJobParameters(getConfiguration().parameter);
        }
        return streamEnv;
    }

    FlinkConfiguration initParameter() {
        ParameterTool argsMap = ParameterTool.fromArgs(args);
        String configFile = argsMap.get(ConfigKeys.KEY_APP_CONF(), null);
        if (configFile == null || configFile.isEmpty()) {
            throw new ExceptionInInitializerError(
                "[StreamPark] Usage:can't find config,please set \"--conf $path \" in main arguments");
        }
        Map<String, String> configMap = parseConfig(configFile);
        Map<String, String> properConf =
            extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
        Map<String, String> appConf =
            extractConfigByPrefix(configMap, ConfigKeys.KEY_APP_PREFIX());

        ParameterTool parameter =
            ParameterTool.fromSystemProperties()
                .mergeWith(ParameterTool.fromMap(properConf))
                .mergeWith(ParameterTool.fromMap(appConf))
                .mergeWith(argsMap);

        Configuration envConfig = Configuration.fromMap(properConf);
        return new FlinkConfiguration(parameter, envConfig, null);
    }

    Map<String, String> parseConfig(String config) {
        Map<String, String> map;
        if (config.startsWith("yaml://")) {
            map = PropertiesUtils.fromYamlText(DeflaterUtils.unzipString(config.substring(7)));
        } else if (config.startsWith("conf://")) {
            map = PropertiesUtils.fromHoconText(DeflaterUtils.unzipString(config.substring(7)));
        } else if (config.startsWith("prop://")) {
            map =
                PropertiesUtils.fromPropertiesText(
                    DeflaterUtils.unzipString(config.substring(7)));
        } else if (config.startsWith("hdfs://")) {
            try {
                String text = HdfsUtils.read(config);
                map = readConfig(config, text);
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "[StreamPark] Failed to read application config from HDFS: " + config, e);
            }
        } else {
            File file = new File(config);
            if (!file.exists()) {
                throw new IllegalArgumentException(
                    "[StreamPark] Usage: application config file: "
                        + file
                        + " is not found!!!");
            }
            try {
                map = readConfig(config, FileUtils.readFile(file));
            } catch (IOException e) {
                throw new IllegalArgumentException(
                    "[StreamPark] Failed to read application config file: " + config, e);
            }
        }
        Map<String, String> filtered = new HashMap<>();
        map.forEach(
            (key, value) -> {
                if (value != null && !value.isEmpty()) {
                    filtered.put(key, value);
                }
            });
        return filtered;
    }

    private Map<String, String> readConfig(String config, String text) {
        String format = config.substring(config.lastIndexOf('.') + 1).toLowerCase();
        switch (format) {
            case "yml":
            case "yaml":
                return PropertiesUtils.fromYamlText(text);
            case "conf":
                return PropertiesUtils.fromHoconText(text);
            case "properties":
                return PropertiesUtils.fromPropertiesText(text);
            default:
                throw new IllegalArgumentException(
                    "[StreamPark] Usage: application config file error,must be [yaml|conf|properties]");
        }
    }

    Map<String, String> extractConfigByPrefix(Map<String, String> configMap, String prefix) {
        Map<String, String> map = new HashMap<>();
        configMap.forEach(
            (key, value) -> {
                if (key.startsWith(prefix)) {
                    map.put(key.substring(prefix.length()), value);
                }
            });
        return map;
    }

    /** Streaming initialization result. */
    public static final class StreamingInitResult {

        public final ParameterTool parameter;
        public final StreamExecutionEnvironment streamEnv;

        StreamingInitResult(ParameterTool parameter, StreamExecutionEnvironment streamEnv) {
            this.parameter = parameter;
            this.streamEnv = streamEnv;
        }
    }
}
