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

package org.apache.streampark.flink.connector.clickhouse.conf;

import org.apache.streampark.common.conf.ConfigOption;
import org.apache.streampark.common.constants.Constants;
import org.apache.streampark.common.util.ConfigUtils;
import org.asynchttpclient.config.AsyncHttpClientConfigDefaults;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class ClickHouseSinkConfigOption implements Serializable {
    public static final String CLICKHOUSE_SINK_PREFIX = "clickhouse.sink";
    private static final String SIGN_COMMA = ",";

    public final ConfigOption<List<String>> hosts;
    public final ConfigOption<String> user;
    public final ConfigOption<String> password;
    public final ConfigOption<String> database;
    public final ConfigOption<Integer> requestTimeout;
    public final ConfigOption<Integer> connectTimeout;
    public final ConfigOption<Integer> maxRequestRetry;
    public final ConfigOption<Integer> maxConnections;
    public final ConfigOption<String> failoverTable;
    public final ConfigOption<String> jdbcUrl;
    public final ConfigOption<String> driverClassName;
    public final ConfigOption<Integer> batchSize;
    public final ConfigOption<Long> flushInterval;

    private final String prefix;
    private final Properties prop;

    public ClickHouseSinkConfigOption(String prefixStr, Properties properties) {
        this.prefix = prefixStr != null ? prefixStr : CLICKHOUSE_SINK_PREFIX;
        this.prop = properties != null ? properties : new Properties();

        this.hosts = ConfigOption.<List<String>>builder("hosts").defaultValue(new ArrayList<>()).classType(List.class)
            .prefix(prefix).properties(prop).handle(k -> {
                String v = prop.getProperty(k);
                if (v == null) return new ArrayList<>();
                return Arrays.stream(v.split(SIGN_COMMA)).filter(s -> !s.isEmpty())
                    .map(ClickHouseSinkConfigOption::normalizeHostUrl)
                    .collect(Collectors.toList());
            }).build();

        this.user = ConfigOption.<String>builder("user").required(true).classType(String.class).prefix(prefix).properties(prop).build();
        this.password = ConfigOption.<String>builder("password").defaultValue("").classType(String.class).prefix(prefix).properties(prop).build();
        this.database = ConfigOption.<String>builder("database").defaultValue(Constants.DEFAULT).classType(String.class).prefix(prefix).properties(prop).build();
        this.requestTimeout = ConfigOption.<Integer>builder("requestTimeout").defaultValue(AsyncHttpClientConfigDefaults.defaultRequestTimeout()).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.connectTimeout = ConfigOption.<Integer>builder("connectTimeout").defaultValue(AsyncHttpClientConfigDefaults.defaultConnectTimeout()).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.maxRequestRetry = ConfigOption.<Integer>builder("maxRequestRetry").defaultValue(AsyncHttpClientConfigDefaults.defaultMaxRequestRetry()).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.maxConnections = ConfigOption.<Integer>builder("maxConnections").defaultValue(AsyncHttpClientConfigDefaults.defaultMaxConnections()).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.failoverTable = ConfigOption.<String>builder("failover.table").classType(String.class).prefix(prefix).properties(prop).build();
        this.jdbcUrl = ConfigOption.<String>builder("jdbcUrl").classType(String.class).prefix(prefix).properties(prop).build();
        this.driverClassName = ConfigOption.<String>builder("driverClassName").classType(String.class).prefix(prefix).properties(prop).build();
        this.batchSize = ConfigOption.<Integer>builder("batchSize").defaultValue(1).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.flushInterval = ConfigOption.<Long>builder("flushInterval").defaultValue(1000L).classType(Long.class).prefix(prefix).properties(prop)
            .handle(k -> { Object r = prop.remove(k); return r != null ? Long.parseLong(r.toString()) : 1000L; }).build();
    }

    public static ClickHouseSinkConfigOption of(Properties properties) { return new ClickHouseSinkConfigOption(CLICKHOUSE_SINK_PREFIX, properties); }
    public Properties getInternalConfig() { return ConfigUtils.getConf(prop, prefix, ""); }

    private static String normalizeHostUrl(String host) {
        String trimmed = host.replaceAll("\\s++", "");
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }
        return Constants.HTTP_SCHEMA + trimmed;
    }
}
