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
package org.apache.streampark.flink.catalog.dao;

import org.apache.streampark.common.tuple.Tuple2;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TableDao extends BaseDao {

  public TableDao(DataSource dataSource) {
    super(dataSource);
  }

  public List<String> getTableName(String tableType, Integer databaseId) throws SQLException {
    return jdbcQuery(
        "SELECT `table_name` FROM catalog_table WHERE `table_type`=? AND `database_id` = ?",
        rs -> {
          List<String> tables = new ArrayList<>();
          while (rs.next()) {
            String table = rs.getString(1);
            tables.add(table);
          }
          return tables;
        },
        tableType,
        databaseId);
  }

  public Tuple2<String, String> getById(Integer id) throws SQLException {
    return jdbcQuery(
        "SELECT `comment`,`table_type` FROM catalog_table WHERE `id`=?",
        rs -> {
          if (rs.next()) {
            return Tuple2.of(rs.getString("comment"), rs.getString("table_type"));
          }
          return null;
        },
        id);
  }

  public Integer getTableId(String objectName, Integer dbId) throws SQLException {
    return jdbcQuery(
        "SELECT `id` FROM catalog_table WHERE `table_name`=? AND `database_id`=?",
        rs -> {
          if (rs.next()) {
            return rs.getInt(1);
          }
          return null;
        },
        objectName,
        dbId);
  }

  public void drop(Integer id) throws SQLException {
    jdbcUpdate("DELETE FROM catalog_table_property WHERE `table_id`=?", id);
    jdbcUpdate("DELETE FROM catalog_column WHERE `table_id`=?", id);
    jdbcUpdate("DELETE FROM catalog_table WHERE `table_id`=?", id);
  }

  public void rename(String tableName, Integer id) throws SQLException {
    jdbcUpdate("UPDATE catalog_table SET `table_name`=? WHERE `id`=?", tableName, id);
  }

  public Integer save(String objectName, String tableType, Integer dbId, String comment)
      throws SQLException {
    return jdbcInsert(
        "INSERT INTO catalog_table(`table_name`,`table_type`,`database_id`,`comment`) VALUES (?,?,?,?)",
        rs -> {
          if (rs.next()) {
            return rs.getInt(1);
          }
          return null;
        },
        objectName,
        tableType,
        dbId,
        comment);
  }
}
