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

import org.apache.streampark.catalog.connections.JdbcConnectionOptions;
import org.apache.streampark.catalog.connections.JdbcConnectionProvider;
import org.apache.streampark.catalog.connections.SimpleJdbcConnectionProvider;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.catalog.CatalogStore;
import org.apache.flink.table.factories.CatalogStoreFactory;
import org.apache.flink.table.factories.FactoryUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.apache.flink.table.factories.FactoryUtil.createCatalogStoreFactoryHelper;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.DRIVER;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.IDENTIFIER;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.MAX_RETRY_TIMEOUT;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.PASSWORD;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.TABLE_NAME;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.URL;
import static org.apache.streampark.catalog.JdbcCatalogStoreFactoryOptions.USERNAME;

/** Catalog Store Factory for Jdbc. */
public class JdbcCatalogStoreFactory implements CatalogStoreFactory {

    private JdbcConnectionProvider jdbcConnectionProvider;
    private transient String catalogTableName;

    @Override
    public CatalogStore createCatalogStore() {
        return new JdbcCatalogStore(jdbcConnectionProvider, this.catalogTableName);
    }

    @Override
    public void open(Context context) {
        FactoryUtil.FactoryHelper<CatalogStoreFactory> factoryHelper =
            createCatalogStoreFactoryHelper(this, context);
        factoryHelper.validate();

        ReadableConfig options = factoryHelper.getOptions();
        JdbcConnectionOptions jdbcConnectionOptions =
            new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                .withUrl(options.get(URL))
                .withDriverName(options.get(DRIVER))
                .withUsername(options.get(USERNAME))
                .withPassword(options.get(PASSWORD))
                .withConnectionCheckTimeoutSeconds(options.get(MAX_RETRY_TIMEOUT))
                .build();

        this.catalogTableName = options.get(TABLE_NAME);
        this.jdbcConnectionProvider = new SimpleJdbcConnectionProvider(jdbcConnectionOptions);
    }

    @Override
    public void close() {
        this.jdbcConnectionProvider.closeConnection();
    }

    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<ConfigOption<?>> requiredOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(URL);
        options.add(DRIVER);
        options.add(USERNAME);
        options.add(PASSWORD);
        options.add(TABLE_NAME);
        return Collections.unmodifiableSet(options);
    }

    @Override
    public Set<ConfigOption<?>> optionalOptions() {
        Set<ConfigOption<?>> options = new HashSet<>();
        options.add(MAX_RETRY_TIMEOUT);
        return Collections.unmodifiableSet(options);
    }
}
