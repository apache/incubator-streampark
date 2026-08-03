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
import org.apache.streampark.common.enums.ApiType;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.common.util.FileUtils;
import org.apache.streampark.common.util.HdfsUtils;
import org.apache.streampark.common.util.PropertiesUtils;
import org.apache.streampark.flink.core.conf.FlinkConfiguration;

import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.api.TableConfig;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/** Initializes Flink streaming execution environment from CLI args and config files. */
public class FlinkStreamingInitializer extends org.apache.streampark.common.util.LoggerSupport {

    protected final String[] args;
    protected final ApiType apiType;

    scala.Function2<org.apache.flink.streaming.api.scala.StreamExecutionEnvironment, ParameterTool, scala.runtime.BoxedUnit> streamEnvConfFunc;
    scala.Function2<TableConfig, ParameterTool, scala.runtime.BoxedUnit> tableConfFunc;
    StreamEnvConfigFunction javaStreamEnvConfFunc;
    TableEnvConfigFunction javaTableEnvConfFunc;

    private FlinkConfiguration configuration;
    private org.apache.flink.streaming.api.scala.StreamExecutionEnvironment streamEnv;

    protected FlinkStreamingInitializer(String[] args, ApiType apiType) {
        this.args = args;
        this.apiType = apiType;
    }

    ParameterTool parameter() {
        return getConfiguration().parameter();
    }

    FlinkConfiguration getConfiguration() {
        if (configuration == null) {
            configuration = initParameter();
        }
        return configuration;
    }

    org.apache.flink.streaming.api.scala.StreamExecutionEnvironment getStreamEnv() {
        if (streamEnv == null) {
            org.apache.flink.streaming.api.scala.StreamExecutionEnvironment env =
                new org.apache.flink.streaming.api.scala.StreamExecutionEnvironment(
                    org.apache.flink.streaming.api.environment.StreamExecutionEnvironment.getExecutionEnvironment(
                        getConfiguration().envConfig()));
            switch (apiType) {
                case JAVA:
                    if (javaStreamEnvConfFunc != null) {
                        javaStreamEnvConfFunc.configuration(env.getJavaEnv(), getConfiguration().parameter());
                    }
                    break;
                case SCALA:
                    if (streamEnvConfFunc != null) {
                        streamEnvConfFunc.apply(env, getConfiguration().parameter());
                    }
                    break;
                default:
                    break;
            }
            env.getConfig().setGlobalJobParameters(getConfiguration().parameter());
            streamEnv = env;
        }
        return streamEnv;
    }

    FlinkConfiguration initParameter() {
        ParameterTool argsMap = ParameterTool.fromArgs(args);
        String configPath = argsMap.get(ConfigKeys.KEY_APP_CONF(), null);
        if (configPath == null || configPath.isEmpty()) {
            throw new ExceptionInInitializerError(
                "[StreamPark] Usage:can't find config,please set \"--conf $path \" in main arguments");
        }
        Map<String, String> configMap = parseConfig(configPath);
        Map<String, String> properConf =
            extractConfigByPrefix(configMap, ConfigKeys.KEY_FLINK_PROPERTY_PREFIX());
        Map<String, String> appConf = extractConfigByPrefix(configMap, ConfigKeys.KEY_APP_PREFIX());

        ParameterTool parameter =
            ParameterTool.fromSystemProperties()
                .mergeWith(ParameterTool.fromMap(properConf))
                .mergeWith(ParameterTool.fromMap(appConf))
                .mergeWith(argsMap);

        Configuration envConfig = Configuration.fromMap(properConf);
        return new FlinkConfiguration(parameter, envConfig, null);
    }

    Map<String, String> parseConfig(String config) {
        String format = config.contains(".")
            ? config.substring(config.lastIndexOf('.') + 1).toLowerCase()
            : "";
        Map<String, String> map = readConfigContent(config, format);
        Map<String, String> filtered = new HashMap<>();
        map.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                filtered.put(key, value);
            }
        });
        return filtered;
    }

    private Map<String, String> readConfigContent(String config, String format) {
        if (config.startsWith("yaml://")) {
            return readConfigText(format, DeflaterUtils.unzipString(config.substring(7)));
        }
        if (config.startsWith("conf://")) {
            return readConfigText(format, DeflaterUtils.unzipString(config.substring(7)));
        }
        if (config.startsWith("prop://")) {
            return readConfigText(format, DeflaterUtils.unzipString(config.substring(7)));
        }
        if (config.startsWith("hdfs://")) {
            try {
                return readConfigText(format, HdfsUtils.read(config));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Failed to read HDFS config: " + config, e);
            }
        }
        File configFile = new File(config);
        if (!configFile.exists()) {
            throw new IllegalArgumentException(
                "[StreamPark] Usage: application config file: " + configFile + " is not found!!!");
        }
        try {
            return readConfigText(format, FileUtils.readFile(configFile));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Failed to read config file: " + configFile, e);
        }
    }

    private Map<String, String> readConfigText(String format, String text) {
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
        Map<String, String> result = new HashMap<>();
        configMap.forEach(
            (key, value) -> {
                if (key.startsWith(prefix)) {
                    result.put(key.substring(prefix.length()), value);
                }
            });
        return result;
    }
}
