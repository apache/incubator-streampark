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
import com.streamxhub.streamx.flink.core.scala.sink.{HBaseOutputFormat, HBaseSink}
import org.apache.flink.api.scala._
import com.streamxhub.streamx.common.util.ConfigUtils
import org.apache.hadoop.hbase.client.{Mutation, Put}
import org.apache.hadoop.hbase.util.Bytes

import java.util.{Collections, Random}

object HBaseSinkApp extends FlinkStreaming {

  override def handle(): Unit = {
    val source = context.addSource(new TestSource)
    val random = new Random()

    //定义转换规则...
    implicit def entry2Put(entity: TestEntity): java.lang.Iterable[Mutation] = {
      val put = new Put(Bytes.toBytes(System.nanoTime() + random.nextInt(1000000)), entity.timestamp)
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("cid"), Bytes.toBytes(entity.cityId))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("oid"), Bytes.toBytes(entity.orderId))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("os"), Bytes.toBytes(entity.orderStatus))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("oq"), Bytes.toBytes(entity.quantity))
      put.addColumn(Bytes.toBytes("cf"), Bytes.toBytes("sid"), Bytes.toBytes(entity.siteId))
      Collections.singleton(put)
    }
    //source ===> trans ===> sink

    //1）插入方式1
    HBaseSink().sink[TestEntity](source, "order")

    //2) 插入方式2
    //1.指定HBase 配置文件
    implicit val prop = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    //2.插入...
    source.writeUsingOutputFormat(new HBaseOutputFormat[TestEntity]("order", entry2Put))


  }

}
