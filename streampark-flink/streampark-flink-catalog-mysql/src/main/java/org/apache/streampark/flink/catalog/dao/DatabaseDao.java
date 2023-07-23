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
import java.util.Map;

public class DatabaseDao extends BaseDao {

  public DatabaseDao(DataSource dataSource) {
    super(dataSource);
  }

  public List<String> listDatabases() throws SQLException {
    return jdbcQuery(
        "SELECT `database_name` FROM catalog_database",
        rs -> {
          List<String> databases = new ArrayList<>();
          while (rs.next()) {
            databases.add(rs.getString(1));
          }
          return databases;
        });
  }

  public Tuple2<Integer, String> getByName(String databaseName) throws SQLException {
    return jdbcQuery(
        "SELECT `id`,`comment` FROM catalog_database WHERE `database_name`=?",
        rs -> {
          if (rs.next()) {
            return Tuple2.of(rs.getInt("id"), rs.getString("comment"));
          }
          return null;
        },
        databaseName);
  }

  public void save(String databaseName, String comment, Map<String, String> properties)
      throws SQLException {
    Integer id =
        jdbcInsert(
            "INSERT INTO catalog_database(`database_name`,`comment`) VALUES (?, ?)",
            rs -> {
              if (rs.next()) {
                return rs.getInt(1);
              }
              return null;
            },
            databaseName,
            comment);
    if (properties != null && !properties.isEmpty()) {
      DatabasePropertiesDao propertiesDao = new DatabasePropertiesDao(dataSource);
      propertiesDao.save(id, properties);
    }
  }

  public void drop(Integer id) throws SQLException {
    jdbcUpdate("DELETE FROM catalog_database WHERE `database_id`=?", id);
    jdbcUpdate("DELETE FROM catalog_database_property WHERE `database_id`=?", id);
  }

  public void update(String comment, Integer id, Map<String, String> properties)
      throws SQLException {
    jdbcUpdate("UPDATE catalog_database SET `comment`=? WHERE `id`=?", comment, id);
    if (properties != null && !properties.isEmpty()) {
      DatabasePropertiesDao propertiesDao = new DatabasePropertiesDao(dataSource);
      propertiesDao.upsert(id, properties);
    }
  }
}
