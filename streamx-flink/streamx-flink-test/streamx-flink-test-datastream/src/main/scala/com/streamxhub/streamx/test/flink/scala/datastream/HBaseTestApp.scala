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

import com.streamxhub.streamx.common.util.ConfigUtils
import com.streamxhub.streamx.flink.core.java.wrapper.HBaseQuery
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.request.HBaseRequest
import org.apache.flink.api.scala._
import org.apache.hadoop.hbase.client.Get

object HBaseTestApp extends FlinkStreaming {

  override def handle(): Unit = {
    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)
    //one topic
    val source = context.fromCollection(Seq("123456", "1111", "222"))

    source.print("source:>>>")

    HBaseRequest(source).requestOrdered[(String, Boolean)](x => {
      new HBaseQuery("person", new Get(x.getBytes()))
    }, timeout = 5000, resultFunc = (a, r) => {
      a -> !r.isEmpty
    }).print(" check.... ")


  }

}
