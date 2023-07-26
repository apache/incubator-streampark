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

package org.apache.streampark.console.core.entity;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import javax.validation.constraints.NotBlank;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_flink_catalog")
public class FlinkCatalog implements Serializable {

  @TableId(type = IdType.AUTO)
  private Long id;

  @NotBlank(message = "{required}")
  private String catalogName;

  @NotBlank(message = "{required}")
  private String properties;

  private String description;

  private Date createTime;

  private Date modifyTime;

  /**
   * Get create catalog sql
   *
   * @return sql
   */
  public String getCreateCatalogSql() {
    return "CREATE CATALOG `" + catalogName + "` WITH (" + properties + ");";
  }

  /**
   * Get use catalog sql
   *
   * @return sql
   */
  public String getUseCatalogSql() {
    return "USE CATALOG `" + catalogName + "`;";
  }

  /**
   * Get use database sql
   *
   * @return sql
   */
  public String getUseDatabaseSql(String databaseName) {
    return "USE " + catalogName + "." + databaseName + ";";
  }

  /**
   * Get show databases sql
   *
   * @return sql
   */
  public String getShowDatabasesSql() {
    return "SHOW DATABASES;";
  }

  /**
   * Get show tables sql in current database
   *
   * @return sql
   */
  public String getShowTablesSql(String databaseName) {
    if (StringUtils.isEmpty(databaseName)) {
      throw new IllegalArgumentException("databaseName can not be null");
    }
    return "SHOW TABLES IN `" + catalogName + "`.`" + databaseName + "`;";
  }

  /**
   * Get show views sql in current database
   *
   * @return sql
   */
  public String getShowViewsSql() {
    return "SHOW VIEWS;";
  }

  /**
   * Get show functions sql in current database
   *
   * @param databaseName database name
   * @return sql
   */
  public String getShowFunctionsSql(String databaseName) {
    if (StringUtils.isEmpty(databaseName)) {
      throw new IllegalArgumentException("databaseName can not be null");
    }
    return "SHOW FUNCTIONS IN `" + catalogName + "`.`" + databaseName + "`;";
  }

  /**
   * Get show create table sql
   *
   * @return sql
   */
  public String getShowCreateTableSql(String databaseName, String tableName) {
    if (StringUtils.isEmpty(databaseName)) {
      throw new IllegalArgumentException("databaseName can not be null");
    }
    if (StringUtils.isEmpty(tableName)) {
      throw new IllegalArgumentException("tableName can not be null");
    }
    return "SHOW CREATE TABLE `" + catalogName + "`.`" + databaseName + "`.`" + tableName + "`;";
  }

  public String getShowCreateViewSql(String databaseName, String objectName) {
    if (StringUtils.isEmpty(databaseName)) {
      throw new IllegalArgumentException("databaseName can not be null");
    }
    if (StringUtils.isEmpty(objectName)) {
      throw new IllegalArgumentException("objectName can not be null");
    }
    return "SHOW CREATE VIEW `" + catalogName + "`.`" + databaseName + "`.`" + objectName + "`;";
  }

  /**
   * Get show catalogs sql
   *
   * @return sql
   */
  public String getShowCatalogsSql() {
    return "SHOW CATALOGS;";
  }
}
