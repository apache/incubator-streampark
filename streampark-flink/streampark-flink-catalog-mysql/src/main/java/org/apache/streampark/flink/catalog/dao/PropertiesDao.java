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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

public class PropertiesDao extends BaseDao {

  public PropertiesDao(DataSource dataSource) {
    super(dataSource);
  }

  public void batchStatement(
      PreparedStatement ps, Integer id, Map<String, String> properties, Boolean upsert) {
    properties.forEach(
        (k, v) -> {
          try {
            ps.setInt(1, id);
            ps.setString(2, k);
            ps.setString(3, v);
            if (upsert) {
              ps.setString(4, v);
            }
          } catch (SQLException e) {
            throw new RuntimeException(e);
          }
        });
  }
}
