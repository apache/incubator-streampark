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

package com.streamxhub.streamx.spark.core

import org.apache.spark.sql.SparkSession
import org.apache.spark.{SparkConf, SparkContext}

import scala.annotation.meta.getter

/**
 * <b><code>SparkBatch</code></b>
 * <p/>
 * Spark 批处理 入口封装
 * <p/>
 * <b>Creation Time:</b> 2022/8/8 20:44.
 *
 * @author gn
 * @since streamx ${PROJECT_VERSION}
 */
trait SparkBatch extends Spark {


  @(transient@getter)
  var context: SparkContext

  /**
   * SparkContext 运行之前执行
   *
   * @param ssc
   */
  def beforeStarted(): Unit = {}


  /**
   * StreamingContext 运行之后执行
   */
  def afterStarted(): Unit = {}

  /**
   * StreamingContext 停止后 程序停止前 执行
   */
  def beforeStop(): Unit = {}

  /**
   * 处理函数
   *
   * @param sc
   */
  def handle(): Unit


  def main(args: Array[String]): Unit = {

    this._args = args

    initArgs(args)

    this.context = sparkSession.sparkContext
    handle()
    afterStarted()
    this.context.stop()
    beforeStop()
  }


}
