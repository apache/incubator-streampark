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

import org.apache.flink.api.java.tuple.Tuple2;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FunctionDao extends BaseDao {

  public FunctionDao(DataSource dataSource) {
    super(dataSource);
  }

  public void drop(Integer id) throws SQLException {
    jdbcUpdate("DELETE FROM catalog_function WHERE `id`=?", id);
  }

  public void update(Integer id, String className, String language) throws SQLException {
    jdbcUpdate(
        "UPDATE catalog_function SET `class_name`=?, `function_language`=? WHERE `id`=?",
        className,
        language,
        id);
  }

  public void save(String objectName, String className, Integer dbId, String language)
      throws SQLException {
    jdbcUpdate(
        "INSERT INTO catalog_function(`function_name`,`class_name`,`database_id`,`function_language`) VALUES (?,?,?,?)",
        objectName,
        className,
        dbId,
        language);
  }

  public Integer getId(Integer dbId, String name) throws SQLException {
    return jdbcQuery(
        "SELECT `id` FROM catalog_function WHERE `function_name`=? AND `database_id`=?",
        rs -> {
          if (rs.next()) {
            return rs.getInt(1);
          }
          return null;
        },
        name,
        dbId);
  }

  public Tuple2<String, String> getFunction(Integer id) throws SQLException {
    return jdbcQuery(
        "SELECT `class_name`,`function_language` FROM catalog_function WHERE `id`=?",
        rs -> {
          if (rs.next()) {
            String className = rs.getString("class_name");
            String language = rs.getString("function_language");
            return Tuple2.of(className, language);
          }
          return null;
        },
        id);
  }

  public List<String> listFunctions(Integer dbId) throws SQLException {
    return jdbcQuery(
        "SELECT `function_name` FROM catalog_function WHERE `database_id`=?",
        rs -> {
          List<String> functions = new ArrayList<>();
          while (rs.next()) {
            String n = rs.getString("function_name");
            functions.add(n);
          }
          return functions;
        },
        dbId);
  }
}
