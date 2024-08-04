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

package org.apache.streampark.catalog.connections;

import org.apache.flink.util.Preconditions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public class JdbcConnectionOptions implements Serializable {

    public static final String USER_KEY = "user";
    public static final String PASSWORD_KEY = "password";

    private static final long serialVersionUID = 1L;

    protected final String url;
    @Nullable
    protected final String driverName;
    protected final int connectionCheckTimeoutSeconds;
    @Nonnull
    protected final Properties properties;

    protected JdbcConnectionOptions(
                                    String url,
                                    @Nullable String driverName,
                                    int connectionCheckTimeoutSeconds,
                                    @Nonnull Properties properties) {
        Preconditions.checkArgument(
            connectionCheckTimeoutSeconds > 0,
            "Connection check timeout seconds shouldn't be smaller than 1");
        this.url = Preconditions.checkNotNull(url, "jdbc url is empty");
        this.driverName = driverName;
        this.connectionCheckTimeoutSeconds = connectionCheckTimeoutSeconds;
        this.properties =
            Preconditions.checkNotNull(properties, "Connection properties must be non-null");
    }

    public String getDbURL() {
        return url;
    }

    @Nullable
    public String getDriverName() {
        return driverName;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(properties.getProperty(USER_KEY));
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(properties.getProperty(PASSWORD_KEY));
    }

    public int getConnectionCheckTimeoutSeconds() {
        return connectionCheckTimeoutSeconds;
    }

    @Nonnull
    public Properties getProperties() {
        return properties;
    }

    @Nonnull
    public static Properties getBriefAuthProperties(String user, String password) {
        final Properties result = new Properties();
        if (Objects.nonNull(user)) {
            result.put(USER_KEY, user);
        }
        if (Objects.nonNull(password)) {
            result.put(PASSWORD_KEY, password);
        }
        return result;
    }

    /** Builder for {@link JdbcConnectionOptions}. */
    public static class JdbcConnectionOptionsBuilder {

        private String url;
        private String driverName;
        private int connectionCheckTimeoutSeconds = 60;
        private final Properties properties = new Properties();

        public JdbcConnectionOptionsBuilder withUrl(String url) {
            this.url = url;
            return this;
        }

        public JdbcConnectionOptionsBuilder withDriverName(String driverName) {
            this.driverName = driverName;
            return this;
        }

        public JdbcConnectionOptionsBuilder withProperty(String propKey, String propVal) {
            Preconditions.checkNotNull(propKey, "Connection property key mustn't be null");
            Preconditions.checkNotNull(propVal, "Connection property value mustn't be null");
            this.properties.put(propKey, propVal);
            return this;
        }

        public JdbcConnectionOptionsBuilder withUsername(String username) {
            if (Objects.nonNull(username)) {
                this.properties.put(USER_KEY, username);
            }
            return this;
        }

        public JdbcConnectionOptionsBuilder withPassword(String password) {
            if (Objects.nonNull(password)) {
                this.properties.put(PASSWORD_KEY, password);
            }
            return this;
        }

        /**
         * Set the maximum timeout between retries, default is 60 seconds.
         *
         * @param connectionCheckTimeoutSeconds the timeout seconds, shouldn't smaller than 1
         *     second.
         */
        public JdbcConnectionOptionsBuilder withConnectionCheckTimeoutSeconds(
                                                                              int connectionCheckTimeoutSeconds) {
            this.connectionCheckTimeoutSeconds = connectionCheckTimeoutSeconds;
            return this;
        }

        public JdbcConnectionOptions build() {
            return new JdbcConnectionOptions(
                url, driverName, connectionCheckTimeoutSeconds, properties);
        }
    }
}
