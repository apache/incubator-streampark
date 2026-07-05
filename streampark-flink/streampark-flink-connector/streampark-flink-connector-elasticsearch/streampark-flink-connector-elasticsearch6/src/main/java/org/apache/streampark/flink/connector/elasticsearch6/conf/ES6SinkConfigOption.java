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

package org.apache.streampark.flink.connector.elasticsearch6.conf;

import org.apache.streampark.common.conf.ConfigOption;
import org.apache.streampark.common.util.ConfigUtils;
import org.apache.http.HttpHost;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;

public class ES6SinkConfigOption implements Serializable {
    public static final String ES_SINK_PREFIX = "es.sink";
    private static final String SIGN_COMMA = ",";
    private static final String SIGN_COLON = ":";

    public final ConfigOption<Boolean> disableFlushOnCheckpoint;
    public final ConfigOption<HttpHost[]> host;
    public final ConfigOption<String> userName;
    public final ConfigOption<String> password;
    public final ConfigOption<Integer> connectRequestTimeout;
    public final ConfigOption<Integer> connectTimeout;
    public final ConfigOption<Integer> maxRetry;
    public final ConfigOption<String> contentType;
    public final ConfigOption<String> pathPrefix;
    public final ConfigOption<Boolean> staleConnectionCheckEnabled;
    public final ConfigOption<Boolean> redirectsEnabled;
    public final ConfigOption<Integer> maxRedirects;
    public final ConfigOption<Boolean> relativeRedirectsAllowed;
    public final ConfigOption<Boolean> authenticationEnabled;
    public final ConfigOption<Integer> socketTimeout;
    public final ConfigOption<Boolean> contentCompressionEnabled;
    public final ConfigOption<Boolean> normalizeUri;

    private final String prefix;
    private final Properties prop;

    public ES6SinkConfigOption(String prefixStr, Properties properties) {
        this.prefix = prefixStr != null ? prefixStr : ES_SINK_PREFIX;
        this.prop = properties != null ? properties : new Properties();

        this.disableFlushOnCheckpoint = ConfigOption.<Boolean>builder("es.disableFlushOnCheckpoint").defaultValue(false).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.host = ConfigOption.<HttpHost[]>builder("host").required(true).classType(HttpHost[].class).prefix(prefix).properties(prop)
            .handle(k -> java.util.Arrays.stream(prop.getProperty(k).split(SIGN_COMMA)).map(x -> {
                String[] parts = x.split(SIGN_COLON);
                return new HttpHost(parts[0], Integer.parseInt(parts[1]));
            }).toArray(HttpHost[]::new)).build();
        this.userName = ConfigOption.<String>builder("es.auth.user").defaultValue(null).classType(String.class).prefix(prefix).properties(prop).build();
        this.password = ConfigOption.<String>builder("es.auth.password").defaultValue(null).classType(String.class).prefix(prefix).properties(prop).build();
        this.connectRequestTimeout = ConfigOption.<Integer>builder("es.connect.request.timeout").defaultValue(-1).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.connectTimeout = ConfigOption.<Integer>builder("es.connect.timeout").defaultValue(-1).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.maxRetry = ConfigOption.<Integer>builder("es.rest.max.retry.timeout").defaultValue(10000).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.contentType = ConfigOption.<String>builder("es.rest.content.type").defaultValue("application/json").classType(String.class).prefix(prefix).properties(prop).build();
        this.pathPrefix = ConfigOption.<String>builder("es.rest.path.prefix").defaultValue(null).classType(String.class).prefix(prefix).properties(prop).build();
        this.staleConnectionCheckEnabled = ConfigOption.<Boolean>builder("es.connect.check.enable").defaultValue(false).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.redirectsEnabled = ConfigOption.<Boolean>builder("es.redirects.enable").defaultValue(false).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.maxRedirects = ConfigOption.<Integer>builder("es.max.redirects").defaultValue(50).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.relativeRedirectsAllowed = ConfigOption.<Boolean>builder("es.relative.redirects.allowed").defaultValue(true).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.authenticationEnabled = ConfigOption.<Boolean>builder("es.authentication.enable").defaultValue(true).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.socketTimeout = ConfigOption.<Integer>builder("es.socket.timeout").defaultValue(-1).classType(Integer.class).prefix(prefix).properties(prop).build();
        this.contentCompressionEnabled = ConfigOption.<Boolean>builder("es.content.compression.enable").defaultValue(true).classType(Boolean.class).prefix(prefix).properties(prop).build();
        this.normalizeUri = ConfigOption.<Boolean>builder("es.normalize.uri").defaultValue(true).classType(Boolean.class).prefix(prefix).properties(prop).build();

    }

    public static ES6SinkConfigOption of(Properties properties) {
        return new ES6SinkConfigOption(ES_SINK_PREFIX, properties);
    }

    public Map<String, String> getInternalConfig() {
        return ConfigUtils.getConfMap(prop, prefix);
    }
}
