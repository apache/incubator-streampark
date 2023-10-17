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

package org.apache.streampark.console.core.service.datasource.config;

import org.apache.streampark.console.core.entity.Datasource;
import org.apache.streampark.console.core.service.datasource.meta.DatasourceMeta;
import org.apache.streampark.console.core.service.datasource.meta.MysqlDatasourceMeta;
import org.apache.streampark.console.core.service.datasource.meta.PostgresqlDatasourceMeta;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DatasourceMetaHelper {

  public static final String MYSQL = "mysql";

  public static final String POSTGRESQL = "postgresql";

  public static final String FLINK_FIELD_TEMPLATE = " `%s` %s";

  public static final String PRIMARY_KEY_TEMPLATE = " PRIMARY KEY (%s) NOT ENFORCED ";

  public static final String COMMA = ",";

  public static final String LINE_BREAK = "\n";

  public static final String FLINK_JDBC_TEMPLATE =
      "CREATE TABLE %s (\n"
          + "%s"
          + "\n) WITH (\n"
          + "   'connector' = 'jdbc',\n"
          + "   'url' = '%s',\n"
          + "   'username' = '%s',\n"
          + "   'password' = '%s',\n"
          + "   'table-name' = '%s'\n"
          + ")";

  public static DatasourceMeta getDatasourceMeta(Datasource datasource) {
    String datasourceType = datasource.getDatasourceType();
    if (StringUtils.isBlank(datasourceType)) {
      throw new RuntimeException("DatasourceType cannot is empty");
    }

    try {
      if (MYSQL.equalsIgnoreCase(datasourceType)) {
        return new MysqlDatasourceMeta(datasource);
      } else if (POSTGRESQL.equalsIgnoreCase(datasourceType)) {
        return new PostgresqlDatasourceMeta(datasource);
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    throw new RuntimeException(String.format("DatasourceType Unsupported type: %s", datasource));
  }

  public static List<String> getAllSupportType() {
    List<String> list = new ArrayList<>();
    list.add(MYSQL);
    list.add(POSTGRESQL);
    return list;
  }

  public static String getFlinkDdl(
      List<DatasourceColumnInfo> columns,
      String url,
      String username,
      String password,
      String tableName) {
    String fields = buildFlinkFields(columns);
    return String.format(
        FLINK_JDBC_TEMPLATE, tableName, fields, url, username, password, tableName);
  }

  public static String buildFlinkFields(List<DatasourceColumnInfo> columns) {
    if (CollectionUtils.isEmpty(columns)) {
      return "";
    }

    StringBuilder fieldStringBuilder = new StringBuilder();
    StringBuilder primaryKeyStringBuilder = new StringBuilder();

    for (DatasourceColumnInfo column : columns) {
      String columnType = jdbcColumnTypeConvertFlinkColumnType(column);
      fieldStringBuilder
          .append(String.format(FLINK_FIELD_TEMPLATE, column.getName(), columnType))
          .append(COMMA)
          .append(LINE_BREAK);
      if (column.getIsPrimaryKey()) {
        primaryKeyStringBuilder.append(column.getName()).append(COMMA);
      }
    }

    if (StringUtils.isNotBlank(primaryKeyStringBuilder)) {
      primaryKeyStringBuilder =
          primaryKeyStringBuilder.deleteCharAt(primaryKeyStringBuilder.length() - 1);
      fieldStringBuilder.append(String.format(PRIMARY_KEY_TEMPLATE, primaryKeyStringBuilder));
    } else {
      fieldStringBuilder.deleteCharAt(fieldStringBuilder.length() - 3);
    }

    return fieldStringBuilder.toString();
  }

  /**
   * Reference document:
   *
   * <p>https://nightlies.apache.org/flink/flink-docs-release-1.16/docs/connectors/table/jdbc/#data-type-mapping
   *
   * <p>https://ververica.github.io/flink-cdc-connectors/release-2.3/content/connectors/mysql-cdc%28ZH%29.html
   */
  private static String jdbcColumnTypeConvertFlinkColumnType(DatasourceColumnInfo column) {
    switch (column.getType().toUpperCase()) {
      case "CHAR":
      case "CHARACTER":
      case "VARCHAR":
      case "CHARACTER VARYING":
      case "TEXT":
      case "STRING":
        return "STRING";
      case "SMALLINT":
      case "TINYINT UNSIGNED":
      case "TINYINT UNSIGNED ZEROFILL":
      case "UNSIGNED":
        return "SMALLINT";
      case "BINARY":
      case "VARBINARY":
      case "BLOB":
      case "BYTEA":
      case "RAW":
        return "BYTES";
      case "TINYINT":
        return "TINYINT";
      case "INT":
      case "MEDIUMINT":
      case "SERIAL":
        return "INT";
      case "FLOAT":
        return "FLOAT";
      case "DOUBLE":
      case "BINARY_DOUBLE":
      case "FLOAT8":
        return "DOUBLE";
      case "LONG":
      case "BIGINT":
      case "INTEGER":
      case "INT UNSIGNED":
        return "BIGINT";
      case "DATE":
        return "DATE";
      case "BOOLEAN":
        return "BOOLEAN";
      case "TIME":
        return "TIME";
      case "DATETIME":
        return "TIMESTAMP";
      case "NUMERIC":
      case "DECIMAL":
        return "DECIMAL";
      case "ARRAY":
        return "ARRAY";
      default:
        return column.getType().toUpperCase();
    }
  }
}
