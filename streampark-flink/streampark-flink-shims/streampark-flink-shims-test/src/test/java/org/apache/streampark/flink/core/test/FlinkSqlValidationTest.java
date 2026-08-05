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

package org.apache.streampark.flink.core.test;

import org.apache.streampark.flink.core.FlinkSqlValidationResult;
import org.apache.streampark.flink.core.FlinkSqlValidator;

import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlinkSqlValidationTest {

    private void verify(String sql, Consumer<FlinkSqlValidationResult> func) {
        String insert =
            "CREATE TABLE my_source ( `f` BIGINT ) WITH ( 'connector' = 'datagen' );\n"
                + "create TABLE my_sink(`f` BIGINT ) with ('connector' = 'print');\n"
                + "insert into my_sink select * from my_source;\n";

        func.accept(FlinkSqlValidator.verifySql(sql + "\n" + insert));
    }

    @Test
    void select() {
        verify(
            "-- select ---\n"
                + "select id, name, age, fetch_millisecond() as millisecond\n"
                + "from source_kafka1;\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void setReset() {
        verify(
            "-- set -------\n"
                + "set 'table.local-time-zone' = 'GMT+08:00';\n"
                + "\n"
                + "-- reset -----\n"
                + "reset 'table.local-time-zone';\n"
                + "reset;\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void create() {
        verify(
            "-- create --\n"
                + "CREATE TABLE Orders (\n"
                + "    `user` BIGINT,\n"
                + "    product STRING,\n"
                + "    order_time TIMESTAMP(3)\n"
                + ") WITH (\n"
                + "    'connector' = 'kafka',\n"
                + "    'scan.startup.mode' = 'earliest-offset'\n"
                + ");\n"
                + "\n"
                + "create catalog hive with (\n"
                + "    'type' = 'hive',\n"
                + "    'hadoop-conf-dir' = '$path',\n"
                + "    'hive-conf-dir' = '$path'\n"
                + ");\n"
                + "create database test;\n"
                + "\n"
                + "create view if not exists a.b.test_view123 as\n"
                + "select id, name, age, fetch_millisecond() as millisecond\n"
                + "from source_kafka1;\n"
                + "\n"
                + "create function fetch_millisecond as "
                + "'org.apache.streampark.flink.core.test.FetchMillisecond' language java;\n"
                + "create temporary function fetch_millisecond as "
                + "'org.apache.streampark.flink.core.test.FetchMillisecond' language java;\n"
                + "create TEMPORARY SYSTEM function fetch_millisecond as "
                + "'org.apache.streampark.flink.core.test.FetchMillisecond' language java;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void drop() {
        verify(
            "drop catalog hive;\n"
                + "drop catalog IF EXISTS hive;\n"
                + "\n"
                + "drop table test;\n"
                + "drop TEMPORARY table test;\n"
                + "drop table IF EXISTS test;\n"
                + "drop TEMPORARY table IF EXISTS test;\n"
                + "drop TEMPORARY table IF EXISTS test.test;\n"
                + "drop TEMPORARY table IF EXISTS hive.test.test;\n"
                + "\n"
                + "drop database test;\n"
                + "drop database if exists test;\n"
                + "drop database if exists hive.test;\n"
                + "drop database if exists hive.test RESTRICT;\n"
                + "drop database if exists hive.test CASCADE;\n"
                + "\n"
                + "drop view test_view;\n"
                + "drop TEMPORARY view test_view;\n"
                + "drop view IF EXISTS test_view;\n"
                + "drop TEMPORARY view IF EXISTS test_view;\n"
                + "drop view test.test_view;\n"
                + "\n"
                + "drop function test_function;\n"
                + "drop TEMPORARY function test_function;\n"
                + "drop TEMPORARY SYSTEM function test_function;\n"
                + "drop function IF EXISTS test_function;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void alter() {
        verify(
            "alter table test rename to test1;\n"
                + "alter table test set ('k' = 'v');\n"
                + "\n"
                + "alter view test_view rename to test_view2;\n"
                + "alter view test_view as\n"
                + "select id, name, age, fetch_millisecond() as millisecond\n"
                + "from source_kafka1;\n"
                + "\n"
                + "alter temporary function fetch_millisecond as "
                + "'org.apache.streampark.flink.core.test.FetchMillisecond' language java;\n"
                + "alter TEMPORARY SYSTEM function fetch_millisecond as "
                + "'org.apache.streampark.flink.core.test.FetchMillisecond' language java;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void desc() {
        verify(
            "-- desc ---\n"
                + "DESCRIBE test;\n"
                + "desc test;\n"
                + "desc test.test;\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void explain() {
        verify(
            "-- explain ---\n"
                + "EXPLAIN PLAN FOR\n"
                + "select id, name, age, fetch_millisecond() as millisecond\n"
                + "from source_kafka1;\n"
                + "\n"
                + "EXPLAIN ESTIMATED_COST\n"
                + "select id, name, age, fetch_millisecond() as millisecond\n"
                + "from source_kafka1;\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void use() {
        verify(
            "-- use ---\n"
                + "use catalog hive;\n"
                + "\n"
                + "use modules hive;\n"
                + "use modules hive, core;\n"
                + "\n"
                + "use test;\n"
                + "use hive.test;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void show() {
        verify(
            "-- show ---\n"
                + "show catalogs;\n"
                + "\n"
                + "show current catalog;\n"
                + "\n"
                + "show databases;\n"
                + "\n"
                + "show current database;\n"
                + "\n"
                + "show tables;\n"
                + "show tables from db1;\n"
                + "show tables from db1 like '%n';\n"
                + "show tables from db1 not like '%n';\n"
                + "\n"
                + "show create table test;\n"
                + "\n"
                + "show columns from orders;\n"
                + "show columns from database1.orders;\n"
                + "show columns from catalog1.database1.orders;\n"
                + "show columns from orders like '%r';\n"
                + "show columns from orders not like '%_r';\n"
                + "\n"
                + "show views;\n"
                + "\n"
                + "show create view test_view;\n"
                + "\n"
                + "show functions;\n"
                + "show user functions;\n"
                + "\n"
                + "show modules;\n"
                + "show full modules;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }

    @Test
    void load() {
        verify(
            "-- load ---\n"
                + "load module hive;\n"
                + "load module hive with ('hive-version' = '2.3.6');\n"
                + "\n"
                + "-- unload ---\n"
                + "unload module hive;\n"
                + "\n",
            r -> assertTrue(r.success()));
    }
}
