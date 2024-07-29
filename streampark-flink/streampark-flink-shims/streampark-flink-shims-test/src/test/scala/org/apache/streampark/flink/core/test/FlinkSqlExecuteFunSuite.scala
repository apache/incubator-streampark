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

package org.apache.streampark.flink.core.test

import org.apache.streampark.common.conf.ConfigKeys.{KEY_FLINK_SQL, PARAM_PREFIX}
import org.apache.streampark.common.util.DeflaterUtils
import org.apache.streampark.flink.core.{FlinkSqlExecutor, FlinkTableInitializer, StreamTableContext}

import org.scalatest.funsuite.AnyFunSuite

import scala.collection.mutable.ArrayBuffer

// scalastyle:off println
class FlinkSqlExecuteFunSuite extends AnyFunSuite {

  def execute(sql: String)(implicit func: String => Unit): Unit = {
    val args = ArrayBuffer(KEY_FLINK_SQL(PARAM_PREFIX), DeflaterUtils.zipString(sql.stripMargin))
    val context = new StreamTableContext(FlinkTableInitializer.initialize(args.toArray, null, null))
    FlinkSqlExecutor.executeSql(KEY_FLINK_SQL(), context.parameter, context)
  }

  test("execute") {
    execute("""
              |-- set -------
              |set 'table.local-time-zone' = 'GMT+08:00';
              |
              |-- reset -----
              |reset 'table.local-time-zone';
              |reset;
              |
              |CREATE temporary TABLE source_kafka1(
              |    `id` int COMMENT '',
              |    `name` string COMMENT '',
              |    `age` int COMMENT '',
              |    proc_time as PROCTIME()
              |) WITH (
              |    'connector' = 'kafka',
              |    'topic' = 'source1',
              |    'properties.bootstrap.servers' = 'node01:9092,node02:9092,node03:9092',
              |    'properties.group.id' = 'test',
              |    'scan.startup.mode' = 'latest-offset',
              |    'format' = 'csv',
              |    'csv.field-delimiter' = ' ',
              |    'csv.ignore-parse-errors' = 'true',
              |    'csv.allow-comments' = 'true'
              |);
              |
              |create table sink_kafka1(
              |    `id` int COMMENT '',
              |    `name` string COMMENT '',
              |    `age` int COMMENT ''
              |) with (
              |    'connector' = 'kafka',
              |    'topic' = 'sink1',
              |    'properties.bootstrap.servers' = 'node01:9092,node02:9092,node03:9092',
              |    'format' = 'csv'
              |);
              |
              |insert into sink_kafka1
              |select id, name, age
              |from source_kafka1;
              |
              |""".stripMargin)(r => println(r))
  }

}
