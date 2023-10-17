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

package org.apache.streampark.console.core.service.datasource.meta;

import org.apache.streampark.console.core.entity.Datasource;
import org.apache.streampark.console.core.service.datasource.config.DatasourceColumnInfo;
import org.apache.streampark.console.core.service.datasource.config.DatasourceMetaHelper;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class AbstractJdbcDatasourceMeta extends DatasourceMeta {

  private static final Logger logger = LoggerFactory.getLogger(AbstractJdbcDatasourceMeta.class);

  private static final Cache<String, Connection> jdbcDatasourceCache =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.DAYS).build();

  public Connection connection;

  AbstractJdbcDatasourceMeta(Datasource datasource) throws SQLException {
    if (jdbcDatasourceCache.getIfPresent(datasource.getDatasourceName()) == null) {
      getDataSource(datasource);
    } else {
      this.connection = jdbcDatasourceCache.getIfPresent(datasource.getDatasourceName());
      if (!this.connection.isValid(1000)) {
        jdbcDatasourceCache.invalidate(datasource.getDatasourceName());
        getDataSource(datasource);
      }
    }
  }

  public abstract String getJdbcUrl(Datasource datasource);

  public abstract String getJdbcDriverClass();

  public void getDataSource(Datasource datasource) throws SQLException {
    HikariDataSource hikariDataSource = new HikariDataSource();
    hikariDataSource.setUsername(datasource.getUsername());
    hikariDataSource.setPassword(datasource.getPassword());
    hikariDataSource.setJdbcUrl(getJdbcUrl(datasource));
    hikariDataSource.setDriverClassName(getJdbcDriverClass());
    this.connection = hikariDataSource.getConnection();
    jdbcDatasourceCache.put(datasource.getDatasourceName(), this.connection);
  }

  @Override
  public List<String> getTables(String databaseName) {
    List<String> list = new ArrayList<>();
    ResultSet resultSet = null;

    try {
      DatabaseMetaData metaData = this.connection.getMetaData();
      resultSet = metaData.getTables(databaseName, null, null, null);

      while (resultSet.next()) {
        String tableName = resultSet.getString(3);
        list.add(tableName);
      }
    } catch (SQLException e) {
      logger.error("Datasource getTables failed", e);
    } finally {
      close(resultSet);
    }
    return list;
  }

  @Override
  public List<String> getDatabases() {
    List<String> list = new ArrayList<>();
    ResultSet resultSet = null;
    try {
      DatabaseMetaData metaData = this.connection.getMetaData();
      resultSet = metaData.getCatalogs();

      while (resultSet.next()) {
        String databaseName = resultSet.getString(1);
        list.add(databaseName);
      }
    } catch (SQLException e) {
      logger.error("Datasource getDatabases failed", e);
    } finally {
      close(resultSet);
    }
    return list;
  }

  @Override
  public List<DatasourceColumnInfo> getColumns(String databaseName, String tableName) {
    List<DatasourceColumnInfo> list = new ArrayList<>();
    ResultSet resultSet = null;

    try {
      DatabaseMetaData metaData = connection.getMetaData();
      resultSet = metaData.getColumns(databaseName, null, tableName, "%");
      List<String> primaryKeys = getPrimaryKeys(databaseName, tableName);

      while (resultSet.next()) {
        String columnName = resultSet.getString("COLUMN_NAME");
        String columnType = resultSet.getString("TYPE_NAME");
        int columnSize = resultSet.getInt("COLUMN_SIZE");
        boolean isNullable = resultSet.getBoolean("NULLABLE");

        DatasourceColumnInfo columnInfo = new DatasourceColumnInfo();
        columnInfo.setName(columnName);
        columnInfo.setType(columnType);
        columnInfo.setSize(columnSize);
        columnInfo.setIsNull(isNullable);
        columnInfo.setIsPrimaryKey(primaryKeys.contains(columnName));
        list.add(columnInfo);
      }
    } catch (SQLException e) {
      logger.error("Datasource getColumns failed", e);
    } finally {
      close(resultSet);
    }
    return list;
  }

  public List<String> getPrimaryKeys(String databaseName, String table) {
    List<String> primaryList = new ArrayList<>();
    ResultSet resultSet = null;
    try {
      resultSet = connection.getMetaData().getPrimaryKeys(databaseName, null, table);
      while (resultSet.next()) {
        String primary = resultSet.getString("COLUMN_NAME");
        primaryList.add(primary);
      }
    } catch (SQLException e) {
      logger.error("Datasource getPrimaryKeys failed", e);
    } finally {
      close(resultSet);
    }
    return primaryList;
  }

  @Override
  public Boolean testConnection() {
    try {
      return this.connection.isValid(1000);
    } catch (SQLException e) {
      logger.error("Datasource testConnection failed", e);
      return false;
    }
  }

  @Override
  public String getFlinkDdl(Datasource datasource, String tableName) {
    List<DatasourceColumnInfo> columns = getColumns(datasource.getDatabase(), tableName);
    return DatasourceMetaHelper.getFlinkDdl(
        columns,
        getJdbcUrl(datasource),
        datasource.getUsername(),
        datasource.getPassword(),
        tableName);
  }

  public void close(ResultSet x) {
    if (x == null) {
      return;
    }
    try {
      x.close();
    } catch (Exception e) {
      logger.warn("Datasource close ResultSet failed", e);
    }
  }
}
