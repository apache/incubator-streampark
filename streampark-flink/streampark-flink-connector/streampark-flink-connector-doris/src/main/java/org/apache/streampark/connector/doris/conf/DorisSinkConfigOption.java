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

package org.apache.streampark.connector.doris.conf;

import org.apache.streampark.common.conf.ConfigOption;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.ConfigUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

/** Doris sink configuration options. */
public class DorisSinkConfigOption implements Serializable {

    public static final String DORIS_SINK_PREFIX = "doris.sink";
    private static final String SIGN_COMMA = ",";

    public final ConfigOption<List<String>> loadUrl;
    public final ConfigOption<String> user;
    public final ConfigOption<String> loadFormat;
    public final ConfigOption<String> password;
    public final ConfigOption<String> database;
    public final ConfigOption<String> table;
    public final ConfigOption<Long> sinkOfferTimeout;
    public final ConfigOption<String> rowDelimiter;
    public final ConfigOption<Long> flushInterval;
    public final ConfigOption<Integer> connectTimeout;
    public final ConfigOption<Integer> maxRequestRetry;
    public final ConfigOption<Integer> maxConnections;
    public final ConfigOption<Integer> maxRow;
    public final ConfigOption<Integer> maxBytes;
    public final ConfigOption<Integer> maxRetries;
    public final ConfigOption<String> labelPrefix;
    public final ConfigOption<String> semantic;

    private final String prefix;
    private final Properties prop;

    public DorisSinkConfigOption(String prefixStr, Properties properties) {
        this.prefix = prefixStr != null ? prefixStr : DORIS_SINK_PREFIX;
        this.prop = properties != null ? properties : new Properties();

        this.loadUrl =
                ConfigOption.<List<String>>builder("load_url")
                        .required(true)
                        .defaultValue(new ArrayList<>())
                        .classType(List.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    String value = prop.getProperty(k);
                                    if (value == null || value.isEmpty()) {
                                        return new ArrayList<>();
                                    }
                                    return Arrays.stream(value.split(SIGN_COMMA))
                                            .filter(s -> !s.isEmpty())
                                            .map(DorisSinkConfigOption::normalizeLoadUrl)
                                            .collect(Collectors.toList());
                                })
                        .build();

        this.user =
                ConfigOption.<String>builder("user")
                        .required(true)
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.loadFormat =
                ConfigOption.<String>builder("loadFormat")
                        .defaultValue("csv")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.password =
                ConfigOption.<String>builder("password")
                        .required(true)
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.database =
                ConfigOption.<String>builder("database")
                        .defaultValue("")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.table =
                ConfigOption.<String>builder("table")
                        .defaultValue("")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.sinkOfferTimeout =
                ConfigOption.<Long>builder("sinkOfferTimeout")
                        .defaultValue(3000L)
                        .classType(Long.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    Object removed = prop.remove(k);
                                    return removed != null ? Long.parseLong(removed.toString()) : 3000L;
                                })
                        .build();

        this.rowDelimiter =
                ConfigOption.<String>builder("properties.row_delimiter")
                        .defaultValue("\n")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.flushInterval =
                ConfigOption.<Long>builder("flushInterval")
                        .defaultValue(300000L)
                        .classType(Long.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    Object removed = prop.remove(k);
                                    return removed != null ? Long.parseLong(removed.toString()) : 300000L;
                                })
                        .build();

        this.connectTimeout =
                ConfigOption.<Integer>builder("connectTimeout")
                        .defaultValue(5000)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxRequestRetry =
                ConfigOption.<Integer>builder("maxRequestRetry")
                        .defaultValue(1)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxConnections =
                ConfigOption.<Integer>builder("maxConnections")
                        .defaultValue(-1)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxRow =
                ConfigOption.<Integer>builder("maxRow")
                        .defaultValue(100000)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxBytes =
                ConfigOption.<Integer>builder("maxRow")
                        .defaultValue(94371840)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.maxRetries =
                ConfigOption.<Integer>builder("maxRetries")
                        .defaultValue(1)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.labelPrefix =
                ConfigOption.<String>builder("labelPrefix")
                        .defaultValue("doris")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();

        this.semantic =
                ConfigOption.<String>builder("semantic")
                        .defaultValue("AT_LEAST_ONCE")
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .build();
    }

    public static DorisSinkConfigOption of(String prefixStr, Properties properties) {
        return new DorisSinkConfigOption(prefixStr, properties);
    }

    public static DorisSinkConfigOption of(Properties properties) {
        return new DorisSinkConfigOption(DORIS_SINK_PREFIX, properties);
    }

    public Properties getInternalConfig() {
        return ConfigUtils.getConf(prop, prefix, "");
    }

    public Properties getInternalProperties() {
        return ConfigUtils.getConf(prop, prefix, ".properties");
    }

    private static String normalizeLoadUrl(String host) {
        String trimmed = host.replaceAll("\\s++", "");
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return Constants.HTTP_SCHEMA + trimmed;
    }
}
