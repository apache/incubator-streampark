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

import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.types.DataType;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ColumnDao extends BaseDao {

  public ColumnDao(DataSource dataSource) {
    super(dataSource);
  }

  public List<Tuple3<String, String, String>> getColumn(Integer id) throws SQLException {
    return jdbcQuery(
        "SELECT `column_name`,`data_type`,`comment` FROM `catalog_column` WHERE `table_id`=?",
        rs -> {
          List<Tuple3<String, String, String>> columns = new ArrayList<>(0);
          while (rs.next()) {
            String colName = rs.getString("column_name");
            String dataType = rs.getString("data_type");
            String comment = rs.getString("comment");
            if (null != comment && comment.length() > 0) {
              columns.add(Tuple3.of(colName, dataType, comment));
            } else {
              columns.add(Tuple3.of(colName, dataType, null));
            }
          }
          return columns;
        },
        id);
  }

  public void save(Integer id, String dbName, String objectName, List<Schema.UnresolvedColumn> cols)
      throws SQLException {

    String sql = "INSERT INTO catalog_column(`table_id`,`column_name`,`data_type`) VALUES (?,?,?)";
    Connection connection = dataSource.getConnection();
    PreparedStatement ps = connection.prepareStatement(sql);

    for (Schema.UnresolvedColumn col : cols) {
      if (col instanceof Schema.UnresolvedPhysicalColumn) {
        Schema.UnresolvedPhysicalColumn pCol = (Schema.UnresolvedPhysicalColumn) col;
        if (!(pCol.getDataType() instanceof DataType)) {
          throw new UnsupportedOperationException(
              String.format(
                  "View data type %s.%s.%s : %s is not supported",
                  dbName, objectName, pCol.getName(), pCol.getDataType()));
        }
        DataType dataType = (DataType) pCol.getDataType();
        ps.setInt(1, id);
        ps.setString(2, pCol.getName());
        ps.setString(3, dataType.getLogicalType().asSerializableString());
        ps.addBatch();
      } else {
        throw new UnsupportedOperationException("View does not support virtual columns");
      }
    }
    ps.addBatch();
    ps.executeBatch();
    close(ps, connection);
  }
}
