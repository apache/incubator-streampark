/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.flink.core.test

import com.streamxhub.streamx.flink.core.{FlinkSqlValidationResult, FlinkSqlValidator}
import org.scalatest.funsuite.AnyFunSuite

// scalastyle:off println
class FlinkSqlValidationFunSuite extends AnyFunSuite {

  def verify(sql: String)(func: FlinkSqlValidationResult => Unit): Unit = func(FlinkSqlValidator.verifySql(sql.stripMargin))

  test("create catalog") {
    verify("create catalog my_catalog") { r =>
      assert(r.success == false)
    }
  }

  test("create database") {
    verify("create database my_database") { r =>
      assert(r.success == false)
    }
  }

  test("create table") {
    verify(
      """
        |CREATE TABLE user_log (
        |    user_id VARCHAR,
        |    item_id VARCHAR,
        |    category_id VARCHAR,
        |    behavior VARCHAR,
        |    ts TIMESTAMP(3)
        | ) WITH (
        |'connector.type' = 'kafka', -- 使用 kafka connector
        |'connector.version' = 'universal',  -- kafka 版本，universal 支持 0.11 以上的版本
        |'connector.topic' = 'user_behavior',  -- kafka topic
        |'connector.properties.bootstrap.servers'='kafka-1:9092,kafka-2:9092,kafka-3:9092',
        |'connector.startup-mode' = 'earliest-offset', -- 从起始 offset 开始读取
        |'format.type' = 'json'  -- 数据源格式为 json
        | );
        | """) { r =>
      println(r.exception)
    }
  }

}
