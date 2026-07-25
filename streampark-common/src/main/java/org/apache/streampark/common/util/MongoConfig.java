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

package org.apache.streampark.common.util;

import org.apache.streampark.common.conf.ConfigKeys;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/** MongoDB client configuration helper. */
public final class MongoConfig {

    private static final Pattern LEADING_DOT_PATTERN = Pattern.compile("^\\.");

    public static final String CLIENT_URI = "client-uri";
    public static final String ADDRESS = "address";
    public static final String REPLICA_SET = "replica-set";
    public static final String DATABASE = "database";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String MIN_CONNECTIONS_PER_HOST = "min-connections-per-host";
    public static final String MAX_CONNECTIONS_PER_HOST = "max-connections-per-host";
    public static final String THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER =
        "threads-allowed-to-block-for-connection-multiplier";
    public static final String SERVER_SELECTION_TIMEOUT = "server-selection-timeout";
    public static final String MAX_WAIT_TIME = "max-wait-time";
    public static final String MAX_CONNECTION_IDEL_TIME = "max-connection-idel-time";
    public static final String MAX_CONNECTION_LIFE_TIME = "max-connection-life-time";
    public static final String CONNECT_TIMEOUT = "connect-timeout";
    public static final String SOCKET_TIMEOUT = "socket-timeout";
    public static final String SOCKET_KEEP_ALIVE = "socket-keep-alive";
    public static final String SSL_ENABLED = "ssl-enabled";
    public static final String SSL_INVALID_HOST_NAME_ALLOWED = "ssl-invalid-host-name-allowed";
    public static final String ALWAYS_USE_M_BEANS = "always-use-m-beans";
    public static final String HEARTBEAT_SOCKET_TIMEOUT = "heartbeat-socket-timeout";
    public static final String HEARTBEAT_CONNECT_TIMEOUT = "heartbeat-connect-timeout";
    public static final String MIN_HEARTBEAT_FREQUENCY = "min-heartbeat-frequency";
    public static final String HEARTBEAT_FREQUENCY = "heartbeat-frequency";
    public static final String LOCAL_THRESHOLD = "local-threshold";
    public static final String AUTHENTICATION_DATABASE = "authentication-database";

    private MongoConfig() {
    }

    public static String getProperty(Properties properties, String k) {
        return getProperty(properties, k, "");
    }

    public static String getProperty(Properties properties, String k, String alias) {
        return getProperties(properties, alias).getProperty(k);
    }

    public static Properties getProperties(Properties properties) {
        return getProperties(properties, "");
    }

    public static Properties getProperties(Properties properties, String alias) {
        Properties prop = new Properties();
        for (String key : properties.stringPropertyNames()) {
            if (!key.startsWith(ConfigKeys.MONGO_PREFIX())) {
                continue;
            }
            String value = properties.getProperty(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            String stripped = key.replace(ConfigKeys.MONGO_PREFIX() + alias, "");
            String k = LEADING_DOT_PATTERN.matcher(stripped).replaceFirst("");
            prop.put(k, value.trim());
        }
        return prop;
    }

    public static MongoClient getClient(Properties properties) {
        return getClient(properties, "");
    }

    public static MongoClient getClient(Properties properties, String alias) {
        Properties mongoParam = getProperties(properties, alias);
        if (mongoParam.containsKey(CLIENT_URI)) {
            MongoClientURI clientURI = new MongoClientURI(mongoParam.getProperty(CLIENT_URI));
            return new MongoClient(clientURI);
        }
        MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
        if (mongoParam.containsKey(MAX_CONNECTIONS_PER_HOST)) {
            builder.connectionsPerHost(Integer.parseInt(mongoParam.getProperty(MAX_CONNECTIONS_PER_HOST)));
        }
        if (mongoParam.containsKey(MIN_CONNECTIONS_PER_HOST)) {
            builder.minConnectionsPerHost(Integer.parseInt(mongoParam.getProperty(MIN_CONNECTIONS_PER_HOST)));
        }
        if (mongoParam.containsKey(REPLICA_SET)) {
            builder.requiredReplicaSetName(mongoParam.getProperty(REPLICA_SET));
        }
        if (mongoParam.containsKey(THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER)) {
            builder.threadsAllowedToBlockForConnectionMultiplier(
                Integer.parseInt(mongoParam.getProperty(THREADS_ALLOWED_TO_BLOCK_FOR_CONNECTION_MULTIPLIER)));
        }
        if (mongoParam.containsKey(SERVER_SELECTION_TIMEOUT)) {
            builder.serverSelectionTimeout(Integer.parseInt(mongoParam.getProperty(SERVER_SELECTION_TIMEOUT)));
        }
        if (mongoParam.containsKey(MAX_WAIT_TIME)) {
            builder.maxWaitTime(Integer.parseInt(mongoParam.getProperty(MAX_WAIT_TIME)));
        }
        if (mongoParam.containsKey(MAX_CONNECTION_IDEL_TIME)) {
            builder.maxConnectionIdleTime(Integer.parseInt(mongoParam.getProperty(MAX_CONNECTION_IDEL_TIME)));
        }
        if (mongoParam.containsKey(MAX_CONNECTION_LIFE_TIME)) {
            builder.maxConnectionLifeTime(Integer.parseInt(mongoParam.getProperty(MAX_CONNECTION_LIFE_TIME)));
        }
        if (mongoParam.containsKey(CONNECT_TIMEOUT)) {
            builder.connectTimeout(Integer.parseInt(mongoParam.getProperty(CONNECT_TIMEOUT)));
        }
        if (mongoParam.containsKey(SOCKET_TIMEOUT)) {
            builder.socketTimeout(Integer.parseInt(mongoParam.getProperty(SOCKET_TIMEOUT)));
        }
        if (mongoParam.containsKey(SSL_ENABLED)) {
            builder.sslEnabled(Boolean.parseBoolean(mongoParam.getProperty(SSL_ENABLED)));
        }
        if (mongoParam.containsKey(SSL_INVALID_HOST_NAME_ALLOWED)) {
            builder.sslInvalidHostNameAllowed(
                Boolean.parseBoolean(mongoParam.getProperty(SSL_INVALID_HOST_NAME_ALLOWED)));
        }
        if (mongoParam.containsKey(ALWAYS_USE_M_BEANS)) {
            builder.alwaysUseMBeans(Boolean.parseBoolean(mongoParam.getProperty(ALWAYS_USE_M_BEANS)));
        }
        if (mongoParam.containsKey(HEARTBEAT_FREQUENCY)) {
            builder.heartbeatFrequency(Integer.parseInt(mongoParam.getProperty(HEARTBEAT_FREQUENCY)));
        }
        if (mongoParam.containsKey(MIN_HEARTBEAT_FREQUENCY)) {
            builder.minHeartbeatFrequency(Integer.parseInt(mongoParam.getProperty(MIN_HEARTBEAT_FREQUENCY)));
        }
        if (mongoParam.containsKey(HEARTBEAT_CONNECT_TIMEOUT)) {
            builder.heartbeatConnectTimeout(Integer.parseInt(mongoParam.getProperty(HEARTBEAT_CONNECT_TIMEOUT)));
        }
        if (mongoParam.containsKey(HEARTBEAT_SOCKET_TIMEOUT)) {
            builder.heartbeatSocketTimeout(Integer.parseInt(mongoParam.getProperty(HEARTBEAT_SOCKET_TIMEOUT)));
        }
        if (mongoParam.containsKey(LOCAL_THRESHOLD)) {
            builder.localThreshold(Integer.parseInt(mongoParam.getProperty(LOCAL_THRESHOLD)));
        }
        MongoClientOptions mongoClientOptions = builder.build();
        String[] addresses = mongoParam.getProperty(ADDRESS).split(",");
        List<ServerAddress> serverAddresses = new ArrayList<>();
        for (String x : addresses) {
            String[] hostAndPort = x.split(":");
            String host = hostAndPort[0];
            int port = Integer.parseInt(hostAndPort[1]);
            serverAddresses.add(new ServerAddress(host, port));
        }
        if (mongoParam.containsKey(USERNAME)) {
            String db;
            if (mongoParam.containsKey(AUTHENTICATION_DATABASE)) {
                db = mongoParam.getProperty(AUTHENTICATION_DATABASE);
            } else {
                db = mongoParam.getProperty(DATABASE);
            }
            MongoCredential mongoCredential =
                MongoCredential.createScramSha1Credential(
                    mongoParam.getProperty(USERNAME),
                    db,
                    mongoParam.getProperty(PASSWORD).toCharArray());
            return new MongoClient(serverAddresses, mongoCredential, mongoClientOptions);
        }
        return new MongoClient(serverAddresses, mongoClientOptions);
    }
}
