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

import com.streamxhub.streamx.common.conf.ConfigConst.KEY_FLINK_SQL
import com.streamxhub.streamx.common.util.DeflaterUtils
import com.streamxhub.streamx.flink.core.{FlinkSqlExecutor, FlinkTableInitializer, StreamTableContext}
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer

// scalastyle:off println
class FlinkSqlExecuteFunSuite extends AnyFunSuite {

  def execute(sql: String)(implicit func: String => Unit): Unit = {
    val args = ArrayBuffer(KEY_FLINK_SQL("--"), DeflaterUtils.zipString(sql.stripMargin))
    val context = new StreamTableContext(FlinkTableInitializer.initStreamTable(args.toArray, null, null))
    FlinkSqlExecutor.executeSql(KEY_FLINK_SQL(), context.parameter, context)
  }

  test("insert") {
    execute(
      """
        |CREATE TABLE source_table (
        |id INT,
        |score INT,
        |address STRING
        |) WITH (
        |'connector' = 'datagen',
        |'rows-per-second'='5',
        |'fields.id.kind'='sequence',
        |'fields.id.start'='1',
        |'fields.id.end'='100',
        |'fields.score.min'='1',
        |'fields.score.max'='100',
        |'fields.address.length'='10'
        |);
        |
        |CREATE TABLE sink_table (
        |id INT,
        |score INT,
        |address STRING
        |) WITH (
        |'connector' = 'print'
        |);
        |
        |insert into sink_table
        |select * from source_table;
        |""") { r =>
      println(r)
    }
  }

}
