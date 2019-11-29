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

import com.streamxhub.spark.core.util.SQLContextUtil
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SaveMode
import org.apache.spark.streaming.Time

import scala.collection.JavaConversions._
import scala.collection.Map
import scala.reflect.ClassTag
import scala.reflect.runtime.universe.TypeTag

/**
  *
  * Mysql Sink
  *
  * 序列化有问题,暂不支持 checkpoint
  *
  */
class MySQLSink[T <: scala.Product : ClassTag : TypeTag](@transient override val sc: SparkContext,
                                                         initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  private lazy val prop: Properties = {
    val p = new Properties()
    p.putAll(param ++ initParams)
    p
  }

  override val prefix: String = "spark.sink.mysql."

  private lazy val url = prop.getProperty("url")
  private lazy val table = prop.getProperty("table")

  private val saveMode =
    prop.getProperty("saveMode", "append").toLowerCase() match {
      case "overwrite" => SaveMode.Overwrite
      case "errorifexists" => SaveMode.ErrorIfExists
      case "ignore" => SaveMode.Ignore
      case _ => SaveMode.Append
    }


  /**
    * 输出 到 Mysql
    *
    * @param rdd
    * @param time
    */
  override def sink(rdd: RDD[T], time: Time): Unit = {
    val sqlContext = SQLContextUtil.getSqlContext(rdd.sparkContext)
    import sqlContext.implicits._
    //
    val begin = System.currentTimeMillis()

    val df = rdd.toDF()
    //     写入 Mysql
    df.write.mode(saveMode).jdbc(url, table, prop)
    val count = df.count()
    val end = System.currentTimeMillis()

    logger.info(s"time:[$time] write [$count] events use time ${(end - begin) / 1000} S ")
  }
}

