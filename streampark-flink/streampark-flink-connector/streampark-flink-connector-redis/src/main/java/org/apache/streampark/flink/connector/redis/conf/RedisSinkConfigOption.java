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

package org.apache.streampark.flink.connector.redis.conf;

import org.apache.streampark.common.conf.ConfigOption;
import org.apache.streampark.common.util.ConfigUtils;

import java.io.Serializable;
import java.util.Properties;

/** Redis sink configuration options. */
public class RedisSinkConfigOption implements Serializable {

    public static final String REDIS_SINK_PREFIX = "redis.sink";
    public static final String DEFAULT_CONNECT_TYPE = "jedisPool";
    private static final String SIGN_COMMA = ",";

    public final ConfigOption<String> host;
    public final ConfigOption<String> connectType;
    public final ConfigOption<Integer> port;

    private final String prefix;
    private final Properties prop;

    public RedisSinkConfigOption(String prefixStr, Properties properties) {
        this.prefix = prefixStr != null ? prefixStr : REDIS_SINK_PREFIX;
        this.prop = properties != null ? properties : new Properties();

        this.host =
                ConfigOption.<String>builder("host")
                        .required(true)
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    Object v = prop.remove(k);
                                    return v != null ? v.toString() : null;
                                })
                        .build();

        this.connectType =
                ConfigOption.<String>builder("connectType")
                        .defaultValue(DEFAULT_CONNECT_TYPE)
                        .classType(String.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    Object v = prop.remove(k);
                                    if (v == null || v.toString().isEmpty()) {
                                        return DEFAULT_CONNECT_TYPE;
                                    }
                                    return v.toString();
                                })
                        .build();

        this.port =
                ConfigOption.<Integer>builder("port")
                        .defaultValue(6379)
                        .classType(Integer.class)
                        .prefix(prefix)
                        .properties(prop)
                        .handle(
                                k -> {
                                    Object v = prop.remove(k);
                                    if (v == null || v.toString().isEmpty()) {
                                        return 6379;
                                    }
                                    return Integer.parseInt(v.toString());
                                })
                        .build();
    }

    public static RedisSinkConfigOption of(Properties properties) {
        return new RedisSinkConfigOption(REDIS_SINK_PREFIX, properties);
    }

    public Properties getInternalConfig() {
        return ConfigUtils.getConf(prop, prefix, "");
    }

    public String getSignComma() {
        return SIGN_COMMA;
    }
}
