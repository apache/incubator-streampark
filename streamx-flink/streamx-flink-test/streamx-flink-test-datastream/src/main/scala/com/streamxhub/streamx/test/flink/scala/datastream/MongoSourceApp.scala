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

import com.mongodb.BasicDBObject
import com.streamxhub.streamx.common.util.{DateUtils, JsonUtils}
import com.streamxhub.streamx.flink.core.scala.FlinkStreaming
import com.streamxhub.streamx.flink.core.scala.source.MongoSource
import org.apache.flink.api.scala.createTypeInformation

import scala.collection.JavaConversions.asScalaIterator

object MongoSourceApp extends FlinkStreaming {

  override def handle(): Unit = {
    implicit val prop = context.parameter.getProperties
    val source = MongoSource()
    source.getDataStream[String](
      "shop",
      (a, d) => {
        Thread.sleep(1000)
        /**
         * 从上一条记录提前offset数据,作为下一条数据查询的条件,如果offset为Null,则表明是第一次查询,需要指定默认offset
         */
        val offset = if (a == null) "2019-09-27 00:00:00" else {
          JsonUtils.read[Map[String, _]](a).get("updateTime").toString
        }
        val cond = new BasicDBObject().append("updateTime", new BasicDBObject("$gte", DateUtils.parse(offset)))
        d.find(cond)
      },
      _.toList.map(_.toJson()), null
    ).print()
  }

}
