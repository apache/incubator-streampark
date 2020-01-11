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

package com.streamxhub.spark.core.sink

import java.util.Properties

import org.apache.hadoop.hbase.client.{ConnectionFactory, HTable}
import org.apache.hadoop.hbase.{HBaseConfiguration, KeyValue}
import org.apache.hadoop.hbase.io.ImmutableBytesWritable
import org.apache.hadoop.hbase.mapreduce.{HFileOutputFormat2, LoadIncrementalHFiles}
import org.apache.hadoop.hbase.TableName
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.reflect.ClassTag

/**
  *
  * @deprecated 通过文件的方式批量加载数据
  */
@deprecated
class HFileSink[T: ClassTag](@transient override val sc: SparkContext,
                             val convert: RDD[T] => RDD[(ImmutableBytesWritable, KeyValue)],
                             val initParams: Map[String, String] = Map.empty[String, String]) extends Sink[T] {

  override val prefix: String = "spark.sink.hfile."

  private lazy val prop = filterProp(param,initParams,prefix)

  private val tableName = prop.getProperty("hbase.table")


  /** 输出
    *
    * @param rdd  初始化HFileSink时指定类型的RDD
    * @param time spark.streaming.Time
    */
  override def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    rdd.foreachPartition { i =>
      val hconfig = HBaseConfiguration.create()
      prop.foreach { case (k, v) => hconfig.set(k, v) }

      convert(rdd).saveAsNewAPIHadoopFile("/tmp/hfile_sink_tmp",
        classOf[ImmutableBytesWritable],
        classOf[KeyValue],
        classOf[HFileOutputFormat2],
        hconfig)
      val connect = ConnectionFactory.createConnection(hconfig)
      val table = connect.getTable(TableName.valueOf(tableName))
      val bulkLoader = new LoadIncrementalHFiles(hconfig)
      val admin = connect.getAdmin
      val location = connect.getRegionLocator(table.getName)
      bulkLoader.doBulkLoad(new Path("/tmp/hfile_sink_tmp"), admin, table, location)
    }
  }


}
