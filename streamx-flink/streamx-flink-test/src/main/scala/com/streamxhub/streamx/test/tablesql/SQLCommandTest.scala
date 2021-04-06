/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.test.tablesql

import com.streamxhub.streamx.flink.common.util.SQLCommandUtil

object SQLCommandTest extends App {

  val sql =
    """
      |
      |CREATE TABLE user_log (
      |    user_id VARCHAR,
      |    item_id VARCHAR,
      |    category_id VARCHAR,
      |    behavior VARCHAR,
      |    ts TIMESTAMP(3)
      |) WITH (
      |connector.type = kafka, -- 使用 kafka connector
      |connector.version = universal,  -- kafka 版本，universal 支持 0.11 以上的版本
      |connector.topic = user_behavior,  -- kafka topic
      |connector.properties.bootstrap.servers=test-hadoop-7:9092,test-hadoop-8:9092,test-hadoop-9:9092,
      |connector.startup-mode = earliest-offset, -- 从起始 offset 开始读取
      |update-mode = append,
      |format.type = 'json,  -- 数据源格式为 json
      |format.derive-schema = true -- 从 DDL schema 确定 json 解析规则
      |);
      |
      |CREATE TABLE pvuv_sink (
      |    dt VARCHAR,
      |    pv BIGINT,
      |    uv BIGINT
      |) WITH (
      |'connector.type' = 'jdbc', -- 使用 jdbc connector
      |'connector.url' = 'jdbc:mysql://localhost:3306/test', -- jdbc url
      |'connector.table' = 'pvuv_sink', -- 表名
      |'connector.username' = 'root', -- 用户名
      |'connector.password' = '123456', -- 密码
      |'connector.write.flush.max-rows' = '1' -- 默认5000条，为了演示改为1条
      |);
      |
      |INSERT INTO pvuv_sink
      |SELECT
      |  DATE_FORMAT(ts, 'yyyy-MM-dd HH:00') dt,
      |  COUNT(*) AS pv,
      |  COUNT(DISTINCT user_id) AS uv
      |FROM user_log
      |GROUP BY DATE_FORMAT(ts, 'yyyy-MM-dd HH:00');
      |
      |""".stripMargin

  val sqlError = SQLCommandUtil.verifySQL(sql)
  println(sqlError)

}
