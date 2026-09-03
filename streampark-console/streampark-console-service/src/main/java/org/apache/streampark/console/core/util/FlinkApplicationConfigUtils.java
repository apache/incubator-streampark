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
import org.apache.streampark.common.configuration.ConfigPrefix;
import org.apache.streampark.common.configuration.ConfigurationFormat;
import org.apache.streampark.common.configuration.ConfigurationParser;
import org.apache.streampark.common.configuration.FlinkOptions;
import org.apache.streampark.common.configuration.option.ApplicationOptions;
import org.apache.streampark.common.util.DeflaterUtils;
import org.apache.streampark.console.core.entity.FlinkApplication;
import org.apache.streampark.console.core.entity.FlinkApplicationConfig;
import org.apache.streampark.console.core.enums.ConfigFileTypeEnum;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Converts persisted application configuration into runtime and transport representations. */
public final class FlinkApplicationConfigUtils {

    private static final List<ConfigPrefix> USER_PREFIXES =
        Arrays.asList(
            FlinkOptions.OPTION_PREFIX,
            FlinkOptions.PROPERTY_PREFIX,
            FlinkOptions.TABLE_PREFIX,
            ApplicationOptions.APPLICATION_PREFIX,
            ApplicationOptions.SQL_PREFIX);

    private FlinkApplicationConfigUtils() {
    }

    /** Copies decoded configuration to a Flink application request. */
    public static void applyTo(
                               FlinkApplicationConfig config,
                               FlinkApplication application) {
        application.setConfig(encodedContent(config));
        application.setConfigId(config.getId());
        application.setFormat(config.getFormat());
    }

    /** Reads stored user configuration without StreamPark namespace prefixes. */
    public static Map<String, String> read(FlinkApplicationConfig config) {
        ConfigurationFormat format = formatOf(config.getFormat());
        if (format == null) {
            return new LinkedHashMap<>();
        }

        Map<String, String> parsed =
            ConfigurationParser.parse(
                DeflaterUtils.unzipString(config.getContent()),
                format,
                "Flink application configuration " + config.getId())
                .toMap();
        Map<String, String> result = new LinkedHashMap<>();
        parsed.forEach(
            (key, value) -> {
                String normalizedKey = stripPrefix(key);
                if (result.putIfAbsent(normalizedKey, value) != null) {
                    throw new ConfigException(
                        "Duplicate application configuration key: " + normalizedKey);
                }
            });
        return result;
    }

    private static String encodedContent(FlinkApplicationConfig config) {
        String content = DeflaterUtils.unzipString(config.getContent());
        return Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));
    }

    private static ConfigurationFormat formatOf(Integer fileType) {
        ConfigFileTypeEnum format = ConfigFileTypeEnum.of(fileType);
        if (format == null) {
            return null;
        }
        switch (format) {
            case YAML:
                return ConfigurationFormat.YAML;
            case PROPERTIES:
                return ConfigurationFormat.PROPERTIES;
            case HOCON:
                return ConfigurationFormat.HOCON;
            default:
                return null;
        }
    }

    private static String stripPrefix(String key) {
        for (ConfigPrefix prefix : USER_PREFIXES) {
            if (prefix.matches(key)) {
                return prefix.stripFrom(key);
            }
        }
        return key;
    }
}
