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

import com.streamxhub.streamx.flink.core.scala.FlinkStreamTable
import com.streamxhub.streamx.flink.core.scala.table.descriptors.{Kafka, KafkaVer}
import org.apache.flink.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.descriptors.Csv

object KafkaTableApp extends FlinkStreamTable {

  override def handle(): Unit = {

    //connect kafka data
    context
      .connect(Kafka("hello", KafkaVer.UNIVERSAL))
      .withFormat(new Csv)
      .withSchema(
        "id" -> DataTypes.STRING(),
        "name" -> DataTypes.STRING()
      )
      .createTemporaryTable("kafka2Table")

    val table: Table = context.from("kafka2Table")

    //print sink
    table.>>[Entity].print("print==>")

    /**
     * 'key 等同于 $"key"
     */
    // select  where
    table.
      select($"id", $"name")
      .where($"id" === "flink")
      .>>[(String, String)].print("simple where==>")

    table.
      select('id, 'name)
      .where('id === "flink")
      .>>[(String, String)].print("simple where2==>")

    /**
     * 查询id=flink,
     * name like apache%
     */
    table.select("id", "name")
      .where("id" === "flink")
      .where("name" like "apache%")
      .>>[(String, String)].print("like where==>")

    /**
     * filter等同于where的操作
     */
    table.select("id", "name")
      .filter("id" === "flink")
      .>>[(String, String)].print("Select -> filter ==>")

    /**
     * groupBy
     */
    table
      .groupBy($"id")
      .select($"id", $"id".count as "count")
      .<<[(String, Long)].print("GroupBy ==>")

  }

}

case class Entity(id: String, name: String)
