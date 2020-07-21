/**
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
package com.streamxhub.flink.test

import java.util

import com.streamxhub.common.util.ConfigUtils
import com.streamxhub.flink.core.request.HBaseRequest
import com.streamxhub.flink.core.source.HBaseSource
import com.streamxhub.flink.core.wrapper.HBaseQuery
import com.streamxhub.flink.core.{FlinkStreaming, StreamingContext}
import org.apache.hadoop.hbase.CellUtil
import org.apache.hadoop.hbase.util.Bytes
import org.apache.flink.api.scala._
import org.apache.hadoop.hbase.client.Get

object HBaseSourceApp extends FlinkStreaming {

  override def handler(context: StreamingContext): Unit = {


    implicit val conf = ConfigUtils.getHBaseConfig(context.parameter.toMap)

    val id = new HBaseSource(context).getDataStream[String](() => {
      Thread.sleep(10000)
      new HBaseQuery("person", new Get("123322242".getBytes()))
    }, r => new String(r.getRow))

    HBaseRequest(id).requestOrdered(x => {
      new HBaseQuery("person", new Get(x.getBytes()))
    }, r => {
      val map = new util.HashMap[String, String]()
      val cellScanner = r.cellScanner()
      while (cellScanner.advance()) {
        val cell = cellScanner.current()
        val q = Bytes.toString(CellUtil.cloneQualifier(cell))
        val (name, v) = q.split("_") match {
          case Array(_type, name) =>
            _type match {
              case "i" => name -> Bytes.toInt(CellUtil.cloneValue(cell))
              case "s" => name -> Bytes.toString(CellUtil.cloneValue(cell))
              case "d" => name -> Bytes.toDouble(CellUtil.cloneValue(cell))
              case "f" => name -> Bytes.toFloat(CellUtil.cloneValue(cell))
            }
          case _ =>
        }
        map.put(name.toString, v.toString)
      }
      map.toString
    }).print("Async")


  }

}
