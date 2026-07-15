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

package org.apache.streampark.flink.connector.elasticsearch5.conf;

import org.apache.streampark.common.conf.ConfigOption;
import org.apache.streampark.common.util.ConfigUtils;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Properties;

public class ESSinkConfigOption implements Serializable {
    public static final String ES_SINK_PREFIX = "es.sink";
    private static final String SIGN_COMMA = ",";
    private static final String SIGN_COLON = ":";

    public final ConfigOption<Boolean> disableFlushOnCheckpoint;
    public final ConfigOption<InetSocketAddress[]> host;

    private final String prefix;
    private final Properties prop;

    public ESSinkConfigOption(String prefixStr, Properties properties) {
        this.prefix = prefixStr != null ? prefixStr : ES_SINK_PREFIX;
        this.prop = properties != null ? properties : new Properties();

        this.disableFlushOnCheckpoint = ConfigOption.<Boolean>builder("es.disableFlushOnCheckpoint")
            .defaultValue(false).classType(Boolean.class).prefix(prefix).properties(prop).build();

        this.host = ConfigOption.<InetSocketAddress[]>builder("host").required(true)
            .classType(InetSocketAddress[].class).prefix(prefix).properties(prop)
            .handle(k -> {
                String value = prop.getProperty(k);
                if (value == null) return new InetSocketAddress[0];
                return java.util.Arrays.stream(value.split(SIGN_COMMA)).map(x -> {
                    String[] parts = x.split(SIGN_COLON);
                    return new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
                }).toArray(InetSocketAddress[]::new);
            }).build();
    }

    public static ESSinkConfigOption of(Properties properties) {
        return new ESSinkConfigOption(ES_SINK_PREFIX, properties);
    }

    public Map<String, String> getInternalConfig() {
        return ConfigUtils.getConfMap(prop, prefix);
    }
}
