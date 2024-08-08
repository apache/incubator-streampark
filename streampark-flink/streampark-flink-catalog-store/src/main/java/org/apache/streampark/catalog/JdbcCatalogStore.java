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

import org.apache.streampark.catalog.connections.JdbcConnectionProvider;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.table.catalog.AbstractCatalogStore;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.catalog.exceptions.CatalogException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 *  Catalog Store for Jdbc.
 */
public class JdbcCatalogStore extends AbstractCatalogStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcCatalogStore.class);
    private final JdbcConnectionProvider jdbcConnectionProvider;
    private transient Connection connection;
    private transient Statement statement;
    private transient ResultSet resultSet;

    private final String catalogTableName;

    public JdbcCatalogStore(JdbcConnectionProvider jdbcConnectionProvider, String catalogTableName) {
        this.jdbcConnectionProvider = jdbcConnectionProvider;
        this.catalogTableName = catalogTableName;
    }

    @Override
    public void open() {
        try {
            this.connection = jdbcConnectionProvider.getOrEstablishConnection();
            this.statement = this.connection.createStatement();
            super.open();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        closeResultSetAndStatement();
        try {
            jdbcConnectionProvider.close();
        } catch (Exception e) {
            throw new CatalogException(e);
        }
        super.close();
    }

    @Override
    public void storeCatalog(String catalogName, CatalogDescriptor catalogDescriptor) throws CatalogException {
        checkOpenState();
        try {
            if (!contains(catalogName)) {
                statement.executeUpdate(String.format(
                    "insert into %s (catalog_name,configuration,create_time,update_time) values ('%s','%s',now(),now())",
                    this.catalogTableName, catalogName,
                    JacksonUtils.write(catalogDescriptor.getConfiguration().toMap())));
            } else {
                LOG.error("catalog {} is exist.", catalogName);
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new CatalogException(String.format("Store catalog %s failed!", catalogName), e);
        }
    }

    @Override
    public void removeCatalog(String catalogName, boolean ignoreIfNotExists) throws CatalogException {
        checkOpenState();
        try {
            int effectRow = statement.executeUpdate(
                String.format("delete from %s where catalog_name='%s'", this.catalogTableName, catalogName));

            if (effectRow == 0 && !ignoreIfNotExists) {
                throw new CatalogException(String.format("Remove catalog %s failed!", catalogName));
            }
        } catch (SQLException e) {
            LOG.error("Remove catalog {} failed!", catalogName, e);
            throw new CatalogException(String.format("Remove catalog %s failed!", catalogName));
        }
    }

    @Override
    public Optional<CatalogDescriptor> getCatalog(String catalogName) throws CatalogException {
        checkOpenState();
        try {
            resultSet = statement
                .executeQuery(
                    String.format("select * from %s where catalog_name='%s'", this.catalogTableName, catalogName));
            while (resultSet.next()) {
                return Optional.of(CatalogDescriptor.of(catalogName,
                    Configuration.fromMap(JacksonUtils.read(resultSet.getString("configuration"), Map.class))));
            }
        } catch (SQLException | JsonProcessingException e) {
            throw new CatalogException(String.format("Get catalog %s failed!", catalogName), e);
        }
        return Optional.empty();
    }

    @Override
    public Set<String> listCatalogs() throws CatalogException {
        checkOpenState();
        Set<String> catalogs = new HashSet<>();
        try {
            resultSet = statement.executeQuery(String.format("select * from %s;", this.catalogTableName));
            while (resultSet.next()) {
                catalogs.add(resultSet.getString("catalog_name"));
            }
            return catalogs;
        } catch (SQLException e) {
            throw new CatalogException("List catalogs failed!", e);
        }
    }

    @Override
    public boolean contains(String catalogName) throws CatalogException {
        checkOpenState();
        try {
            resultSet = statement.executeQuery(
                String.format("select * from %s where catalog_name='%s';", this.catalogTableName, catalogName));
            while (resultSet.next()) {
                resultSet.getString("catalog_name");
                return true;
            }
        } catch (SQLException e) {
            throw new CatalogException(String.format("Catalog %s is contains failed!", catalogName), e);
        }
        return false;
    }

    private void closeResultSetAndStatement() {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
            resultSet = null;
            statement = null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
