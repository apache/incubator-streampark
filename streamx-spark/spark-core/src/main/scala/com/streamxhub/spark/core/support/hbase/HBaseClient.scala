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

package com.streamxhub.spark.core.support.hbase

import java.util.concurrent.ConcurrentHashMap

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.hbase.HBaseConfiguration
import org.apache.hadoop.hbase.client.{Connection, ConnectionFactory}
import org.apache.spark.SparkConf

import scala.collection.JavaConversions._

/**
  *
  *
  * Hbase 连接池
  */
object HBaseClient {


  @transient
  private lazy val pools: ConcurrentHashMap[String, Connection] = new ConcurrentHashMap[String, Connection]()


  def connect(params: Map[String, String]): Connection = {
    val conf = HBaseConfiguration.create
    for ((key, value) <- params) {
      conf.set(key, value)
    }
    connect(conf)
  }

  /*
  * spark.hbase.hbase.zookeeper.quorum=ip1,ip2,ip3
  * spark.hbase.hbase.master=ip:port
  * */
  def connect(sparkConf: SparkConf): Connection = {

    val conf = HBaseConfiguration.create

    for ((key, value) <- sparkConf.getAllWithPrefix("spark.hbase.")) {
      conf.set(key, value)
    }

    connect(conf)
  }

  def connect(conf: Configuration): Connection = {
    val zookeeper = conf.get("hbase.zookeeper.quorum")
    pools.getOrElseUpdate(zookeeper, ConnectionFactory.createConnection(conf))
  }

  def close(): Unit = pools.foreach { case (k, v) => v.close() }

}
