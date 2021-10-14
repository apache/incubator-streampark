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
package com.streamxhub.streamx.test.flink.scala.datastream

import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.sink.JdbcSink
import com.streamxhub.streamx.flink.core.scala.source.KafkaSource
import org.apache.flink.api.scala._
import org.apache.flink.connector.jdbc.{JdbcConnectionOptions, JdbcStatementBuilder, JdbcSink => JSink}

import java.sql.PreparedStatement

object JdbcSinkApp extends FlinkStreaming {

  override def handle(): Unit = {

    /**
     * 从kafka里读数据.这里的数据是数字或者字母,每次读取1条
     */
    val source = KafkaSource().getDataStream[String]()
      .uid("kfkSource1")
      .name("kfkSource1")
      .map(x => {
        x.value
      })


    /**
     * 假设这里有一个orders表.有一个字段,id的类型是int
     * 在数据插入的时候制造异常:
     * 1)正确情况: 当从kafka中读取的内容全部是数字时会插入成功,kafka的消费的offset也会更新.
     * 如: 当前kafka size为20,手动输入10个数字,则size为30,然后会将这10个数字写入到Mysql,kafka的offset也会更新
     * 2)异常情况: 当从kafka中读取的内容非数字会导致插入失败,kafka的消费的offset会回滚
     * 如: 当前的kafka size为30,offset是30, 手动输入1个字母,此时size为31,写入mysql会报错,kafka的offset依旧是30,不会发生更新.
     */
    JdbcSink(parallelism = 5).sink[String](source)(x => {
      s"insert into orders(id,timestamp) values('$x',${System.currentTimeMillis()})"
    }).uid("mysqlSink").name("mysqlSink")

    /**
     * what fuck....
     * 官方提供的jdbc
     */
    if (1 == 2) {
      source.addSink(JSink.sink[String](
        "insert into orders (id,timestamp) values (?,?)",
        new JdbcStatementBuilder[String]() {
          override def accept(t: PreparedStatement, u: String): Unit = {
            t.setString(1, u)
            t.setLong(2, System.currentTimeMillis())
          }
        },
        new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
          .withUrl("jdbc:mysql://localhost:3306/test?useSSL=false&allowPublicKeyRetrieval=true")
          .withDriverName("com.mysql.jdbc.Driver")
          .withUsername("root")
          .withPassword("123322242")
          .build()))

    }
  }

}
