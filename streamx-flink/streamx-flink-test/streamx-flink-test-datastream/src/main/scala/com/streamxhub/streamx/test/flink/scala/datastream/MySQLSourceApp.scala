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
import com.streamxhub.streamx.flink.core.scala.source.JdbcSource
import org.apache.flink.api.scala.createTypeInformation

object MySQLSourceApp extends FlinkStreaming {

  override def handle(): Unit = {

    JdbcSource().getDataStream[Order](lastOne => {
      val laseOffset = if (lastOne == null) "2020-10-10 23:00:00" else lastOne.timestamp
      s"select * from t_order where timestamp > '$laseOffset' order by timestamp asc "
    },
      _.map(x => new Order(x("market_id").toString, x("timestamp").toString)), null
    ).print()

  }

}

class Order(val marketId: String, val timestamp: String) extends Serializable
