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

package org.apache.streampark.catalog;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 *  Catalog Store Options for Jdbc.
 */
public class JdbcCatalogStoreFactoryOptions {

    public static final String IDENTIFIER = "jdbc";

    public static final ConfigOption<String> URL =
        ConfigOptions.key("url")
            .stringType()
            .noDefaultValue()
            .withDescription(
                "The JDBC database url.");
    public static final ConfigOption<String> TABLE_NAME =
        ConfigOptions.key("table-name")
            .stringType()
            .noDefaultValue()
            .withDescription(
                "The name of JDBC table to connect.");
    public static final ConfigOption<String> DRIVER =
        ConfigOptions.key("driver")
            .stringType()
            .noDefaultValue()
            .withDescription(
                "The class name of the JDBC driver to use to connect to this URL, if not set, it will automatically be derived from the URL.");
    public static final ConfigOption<String> USERNAME =
        ConfigOptions.key("username")
            .stringType()
            .noDefaultValue()
            .withDescription(
                "The JDBC user name. 'username' and 'password' must both be specified if any of them is specified.");

    public static final ConfigOption<String> PASSWORD =
        ConfigOptions.key("password")
            .stringType()
            .noDefaultValue()
            .withDescription(
                "The JDBC password.");

    public static final ConfigOption<Integer> MAX_RETRY_TIMEOUT =
        ConfigOptions.key("max-retry-timeout")
            .intType()
            .defaultValue(60)
            .withDescription(
                "Maximum timeout between retries. The timeout should be in second granularity and shouldn't be smaller than 1 second.");

    private JdbcCatalogStoreFactoryOptions() {
    }
}
