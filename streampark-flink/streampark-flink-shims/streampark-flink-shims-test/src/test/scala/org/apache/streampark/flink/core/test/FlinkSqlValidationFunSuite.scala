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

import org.apache.streampark.flink.core.{FlinkSqlValidationResult, FlinkSqlValidator}

import org.scalatest.funsuite.AnyFunSuite

// scalastyle:off println
class FlinkSqlValidationFunSuite extends AnyFunSuite {

  def verify(sql: String)(func: FlinkSqlValidationResult => Unit): Unit = {

    val insert =
      """
        |CREATE TABLE my_source ( `f` BIGINT ) WITH ( 'connector' = 'datagen' );
        |create TABLE my_sink(`f` BIGINT ) with ('connector' = 'print');
        |insert into my_sink select * from my_source;
        |""".stripMargin

    func(FlinkSqlValidator.verifySql(s"""
                                        |$sql
                                        |$insert
                                        |""".stripMargin))
  }

  test("select") {
    verify("""
             |-- select ---
             |select id, name, age, fetch_millisecond() as millisecond
             |from source_kafka1;
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("set_reset") {
    verify("""
             |-- set -------
             |set 'table.local-time-zone' = 'GMT+08:00';
             |
             |-- reset -----
             |reset 'table.local-time-zone';
             |reset;
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("create") {
    verify(
      """
        |-- create --
        |CREATE TABLE Orders (
        |    `user` BIGINT,
        |    product STRING,
        |    order_time TIMESTAMP(3)
        |) WITH (
        |    'connector' = 'kafka',
        |    'scan.startup.mode' = 'earliest-offset'
        |);
        |
        |create catalog hive with (
        |    'type' = 'hive',
        |    'hadoop-conf-dir' = '$path',
        |    'hive-conf-dir' = '$path'
        |);
        |create database test;
        |
        |create view if not exists a.b.test_view123 as
        |select id, name, age, fetch_millisecond() as millisecond
        |from source_kafka1;
        |
        |create function fetch_millisecond as 'org.apache.streampark.flink.core.test.FetchMillisecond' language java;
        |create temporary function fetch_millisecond as 'org.apache.streampark.flink.core.test.FetchMillisecond' language java;
        |create TEMPORARY SYSTEM function fetch_millisecond as 'org.apache.streampark.flink.core.test.FetchMillisecond' language java;
        |
        |""".stripMargin)(r => assert(r.success == true))
  }

  test("drop") {
    verify("""
             |drop catalog hive;
             |drop catalog IF EXISTS hive;
             |
             |drop table test;
             |drop TEMPORARY table test;
             |drop table IF EXISTS test;
             |drop TEMPORARY table IF EXISTS test;
             |drop TEMPORARY table IF EXISTS test.test;
             |drop TEMPORARY table IF EXISTS hive.test.test;
             |
             |drop database test;
             |drop database if exists test;
             |drop database if exists hive.test;
             |drop database if exists hive.test RESTRICT;
             |drop database if exists hive.test CASCADE;
             |
             |drop view test_view;
             |drop TEMPORARY view test_view;
             |drop view IF EXISTS test_view;
             |drop TEMPORARY view IF EXISTS test_view;
             |drop view test.test_view;
             |
             |drop function test_function;
             |drop TEMPORARY function test_function;
             |drop TEMPORARY SYSTEM function test_function;
             |drop function IF EXISTS test_function;
             |
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("alter") {
    verify(
      """
        |alter table test rename to test1;
        |alter table test set ('k' = 'v');
        |
        |alter view test_view rename to test_view2;
        |alter view test_view as
        |select id, name, age, fetch_millisecond() as millisecond
        |from source_kafka1;
        |
        |alter temporary function fetch_millisecond as 'org.apache.streampark.flink.core.test.FetchMillisecond' language java;
        |alter TEMPORARY SYSTEM function fetch_millisecond as 'org.apache.streampark.flink.core.test.FetchMillisecond' language java;
        |
        |""".stripMargin)(r => assert(r.success == true))
  }

  test("desc") {
    verify("""
             |-- desc ---
             |DESCRIBE test;
             |desc test;
             |desc test.test;
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("explain") {
    verify("""
             |-- explain ---
             |EXPLAIN PLAN FOR
             |select id, name, age, fetch_millisecond() as millisecond
             |from source_kafka1;
             |
             |EXPLAIN ESTIMATED_COST
             |select id, name, age, fetch_millisecond() as millisecond
             |from source_kafka1;
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("use") {
    verify("""
             |-- use ---
             |use catalog hive;
             |
             |use modules hive;
             |use modules hive, core;
             |
             |use test;
             |use hive.test;
             |
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("show") {
    verify("""
             |-- show ---
             |show catalogs;
             |
             |show current catalog;
             |
             |show databases;
             |
             |show current database;
             |
             |show tables;
             |show tables from db1;
             |show tables from db1 like '%n';
             |show tables from db1 not like '%n';
             |
             |show create table test;
             |
             |show columns from orders;
             |show columns from database1.orders;
             |show columns from catalog1.database1.orders;
             |show columns from orders like '%r';
             |show columns from orders not like '%_r';
             |
             |show views;
             |
             |show create view test_view;
             |
             |show functions;
             |show user functions;
             |
             |show modules;
             |show full modules;
             |
             |""".stripMargin)(r => assert(r.success == true))
  }

  test("load") {
    verify("""
             |-- load ---
             |load module hive;
             |load module hive with ('hive-version' = '2.3.6');
             |
             |-- unload ---
             |unload module hive;
             |
             |""".stripMargin)(r => assert(r.success == true))
  }

}
