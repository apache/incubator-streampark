/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.spark.core.sink

import com.streamxhub.streamx.common.conf.ConfigConst
import com.streamxhub.streamx.common.util.HBaseClient
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.hbase.client._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 *
 */
class RichHBSink(@transient override val sc: SparkContext,
                 val initParams: Map[String, String] = Map.empty[String, String]) extends Sink[Mutation] {


  override val prefix: String = "spark.sink.hbase."

  private lazy val prop = filterProp(param, initParams, prefix, "hbase.")

  private val tableName = prop.getProperty("hbase.table")

  private val commitBatch = prop.getOrElse(ConfigConst.KEY_HBASE_COMMIT_BATCH, "1000").toInt

  private def getConnect: Connection = HBaseClient(prop).connection

  private def getMutator: BufferedMutator = {
    val connection = getConnect
    val bufferedMutatorParams = new BufferedMutatorParams(TableName.valueOf(tableName))
    connection.getBufferedMutator(bufferedMutatorParams)
  }

  private def getTable: Table = getConnect.getTable(TableName.valueOf(tableName))

  /**
   * 输出
   *
   * @param rdd  spark.RDD
   * @param time spark.streaming.Time
   */
  override def sink(rdd: RDD[Mutation], time: Time): Unit = {
    rdd.foreachPartition { iter =>
      val mutator = getMutator
      val table = getTable
      val list = new mutable.ArrayBuffer[Mutation]()
      iter.foreach {
        case put: Put => mutator.mutate(put)
        case other => list += other
      }
      batch(table, list: _*)
      mutator.flush()
      mutator.close()
      table.close()
    }
  }

  /**
   * 批量插入
   *
   * @param actions
   */
  def batch(table: Table, actions: Mutation*): Unit = {
    if (actions.nonEmpty) {
      val start = System.currentTimeMillis()
      val (head, tail) = actions.splitAt(commitBatch)
      table.batch(head, new Array[AnyRef](head.length))
      println(s"batch ${head.size} use ${System.currentTimeMillis() - start} MS")
      batch(table, tail.toList: _*)
    }
  }
}
