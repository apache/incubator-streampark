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
import org.apache.flink.api.common.functions.ReduceFunction
import org.apache.flink.api.scala._

import scala.util.Try

object StreamingTestApp extends FlinkStreaming {

  override def handle(): Unit = {
    val host = context.parameter.get("host")
    val port = Try(context.parameter.get("port").toInt).getOrElse(7070)

    val source = context.socketTextStream(host, port)

    source.flatMap(_.split("\\s+"))
      .map(x => {
        (x, 1, 2)
      })
      .keyBy(_._1)
      .reduce(new ReduceFunction[(String, Int, Int)] {
        override def reduce(v1: (String, Int, Int), v2: (String, Int, Int)): (String, Int, Int) = {
          (v1._1, v1._2 + v2._2, v1._3 + v2._3)
        }
      })


  }
}
