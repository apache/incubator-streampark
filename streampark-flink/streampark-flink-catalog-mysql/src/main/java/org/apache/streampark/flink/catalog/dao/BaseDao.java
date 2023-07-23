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

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;

import javax.sql.DataSource;

import java.sql.SQLException;

public class BaseDao {

  public DataSource dataSource;

  public BaseDao(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public <T> T jdbcQuery(String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return new QueryRunner(dataSource).query(sql, rsh, params);
  }

  public void jdbcUpdate(String sql, Object... params) throws SQLException {
    new QueryRunner(dataSource).update(sql, params);
  }

  public <T> T jdbcInsert(String sql, ResultSetHandler<T> rsh, Object... params)
      throws SQLException {
    return new QueryRunner(dataSource).insert(sql, rsh, params);
  }

  public void jdbcBatch(String sql, Object[][] params) throws SQLException {
    new QueryRunner(dataSource).batch(sql, params);
  }

  public void close(AutoCloseable... closes) throws SQLException {
    for (AutoCloseable closeable : closes) {
      if (closeable != null) {
        try {
          closeable.close();
        } catch (Exception e) {
          throw new SQLException(e);
        }
      }
    }
  }
}
