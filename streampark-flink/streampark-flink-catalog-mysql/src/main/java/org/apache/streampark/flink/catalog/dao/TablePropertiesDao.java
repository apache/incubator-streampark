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

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TablePropertiesDao extends PropertiesDao {

  public TablePropertiesDao(DataSource dataSource) {
    super(dataSource);
  }

  public Map<String, String> getProperties(Integer id) throws SQLException {
    return jdbcQuery(
        "SELECT `key`,`value` FROM catalog_table_property WHERE `table_id`=?",
        rs -> {
          Map<String, String> props = new HashMap<>();
          while (rs.next()) {
            String key = rs.getString("key");
            String value = rs.getString("value");
            props.put(key, value);
          }
          return props;
        },
        id);
  }

  public void save(Integer id, Map<String, String> opts) throws SQLException {
    String sql = "INSERT INTO catalog_table_property(`table_id`,`key`,`value`) VALUES (?,?,?)";
    Connection connection = dataSource.getConnection();
    PreparedStatement ps = connection.prepareStatement(sql);
    super.batchStatement(ps, id, opts, false);
    ps.addBatch();
    ps.executeBatch();
    close(ps, connection);
  }

  public void upsert(Integer id, Map<String, String> opts) throws SQLException {
    String sql =
        "INSERT INTO catalog_table_property(`table_id`,`key`,`value`) "
            + "VALUES (?,?,?) ON DUPLICATE KEY UPDATE `value` =?, `update_time` = now()";

    Connection connection = dataSource.getConnection();
    PreparedStatement ps = connection.prepareStatement(sql);
    super.batchStatement(ps, id, opts, true);
    ps.addBatch();
    ps.executeBatch();
    close(ps, connection);
  }
}
