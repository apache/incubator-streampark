/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.plugin.profiling;

import com.streamxhub.streamx.plugin.profiling.util.AgentLogger;
import com.streamxhub.streamx.plugin.profiling.util.ExponentialBackoffRetryPolicy;
import com.streamxhub.streamx.plugin.profiling.util.Utils;

import org.yaml.snakeyaml.Yaml;
import scalaj.http.Http;
import scalaj.http.HttpResponse;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author benjobs
 */
public class YamlConfigProvider implements ConfigProvider {
    private static final AgentLogger LOGGER =
        AgentLogger.getLogger(YamlConfigProvider.class.getName());

    private static final String OVERRIDE_KEY = "override";

    private String filePath;

    public YamlConfigProvider() {
    }

    public YamlConfigProvider(String filePath) {
        setFilePath(filePath);
    }

    public void setFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("filePath cannot be null or empty");
        }

        this.filePath = filePath;
    }

    @Override
    public Map<String, Map<String, List<String>>> getConfig() {
        return getConfig(filePath);
    }

    private static Map<String, Map<String, List<String>>> getConfig(String configFilePathOrUrl) {
        if (configFilePathOrUrl == null || configFilePathOrUrl.isEmpty()) {
            LOGGER.warn("Empty YAML config file path");
            return new HashMap<>();
        }

        byte[] bytes;

        try {
            bytes =
                new ExponentialBackoffRetryPolicy<byte[]>(3, 100)
                    .attempt(
                        () -> {
                            String filePathLowerCase = configFilePathOrUrl.toLowerCase();
                            if (filePathLowerCase.startsWith("http://")
                                || filePathLowerCase.startsWith("https://")) {
                                return getHttp(configFilePathOrUrl);
                            } else {
                                return Files.readAllBytes(Paths.get(configFilePathOrUrl));
                            }
                        });

            LOGGER.info("Read YAML config from: " + configFilePathOrUrl);
        } catch (Throwable e) {
            LOGGER.warn("Failed to read file: " + configFilePathOrUrl, e);
            return new HashMap<>();
        }

        if (bytes == null || bytes.length == 0) {
            return new HashMap<>();
        }

        Map<String, Map<String, List<String>>> result = new HashMap<>();

        try {
            try (ByteArrayInputStream stream = new ByteArrayInputStream(bytes)) {
                Yaml yaml = new Yaml();
                Object yamlObj = yaml.load(stream);

                if (!(yamlObj instanceof Map)) {
                    LOGGER.warn("Invalid YAML config content: " + yamlObj);
                    return result;
                }

                Map yamlMap = (Map) yamlObj;

                Map overrideMap = null;

                for (Object key : yamlMap.keySet()) {
                    String configKey = key.toString();
                    Object valueObj = yamlMap.get(key);
                    if (valueObj == null) {
                        continue;
                    }

                    if (configKey.equals(OVERRIDE_KEY)) {
                        if (valueObj instanceof Map) {
                            overrideMap = (Map) valueObj;
                        } else {
                            LOGGER.warn("Invalid override property: " + valueObj);
                        }
                    } else {
                        addConfig(result, "", configKey, valueObj);
                    }
                }

                if (overrideMap != null) {
                    for (Object key : overrideMap.keySet()) {
                        String overrideKey = key.toString();
                        Object valueObj = overrideMap.get(key);
                        if (valueObj == null) {
                            continue;
                        }

                        if (!(valueObj instanceof Map)) {
                            LOGGER.warn("Invalid override section: " + key + ": " + valueObj);
                            continue;
                        }

                        Map<Object, Object> overrideValues = (Map<Object, Object>) valueObj;
                        for (Map.Entry<Object, Object> entry : overrideValues.entrySet()) {
                            if (entry.getValue() == null) {
                                continue;
                            }
                            String configKey = entry.getKey().toString();
                            addConfig(result, overrideKey, configKey, entry.getValue());
                        }
                    }
                }

                return result;
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to read config file: " + configFilePathOrUrl, e);
            return new HashMap<>();
        }
    }

    private static void addConfig(
        Map<String, Map<String, List<String>>> config, String override, String key, Object value) {
        Map<String, List<String>> configMap = config.computeIfAbsent(override, k -> new HashMap<>());

        if (value instanceof List) {
            List<String> configValueList = configMap.computeIfAbsent(key, k -> new ArrayList<>());
            for (Object entry : (List) value) {
                configValueList.add(entry.toString());
            }
        } else if (value instanceof Object[]) {
            List<String> configValueList = configMap.computeIfAbsent(key, k -> new ArrayList<>());
            for (Object entry : (Object[]) value) {
                configValueList.add(entry.toString());
            }
        } else if (value instanceof Map) {
            for (Object mapKey : ((Map) value).keySet()) {
                String propKey = mapKey.toString();
                Object propValue = ((Map) value).get(propKey);
                if (propValue != null) {
                    addConfig(config, override, key + "." + propKey, propValue);
                }
            }
        } else {
            List<String> configValueList = configMap.computeIfAbsent(key, k -> new ArrayList<>());
            configValueList.add(value.toString());
        }
    }

    private static byte[] getHttp(String url) {
        try {
            LOGGER.debug(String.format("Getting url: %s", url));
            HttpResponse response = Http.apply(url).timeout(1000, 5000).asString();
            if (response.isError()) {
                throw new RuntimeException(
                    "Failed response from url: " + url + ", response code: " + response.code());
            }
            return Utils.toByteArray((InputStream) response.body());
        } catch (Throwable ex) {
            throw new RuntimeException("Failed getting url: " + url, ex);
        }
    }
}
