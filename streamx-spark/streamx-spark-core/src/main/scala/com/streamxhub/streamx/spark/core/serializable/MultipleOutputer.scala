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

package com.streamxhub.streamx.spark.core.serializable

import org.apache.avro.mapreduce.AvroMultipleOutputs
import org.apache.hadoop.mapreduce.lib.output.MultipleOutputs

/**
  *
  */
/**
  * 多目录输出
  *
  * @tparam K
  * @tparam V
  */
trait MultipleOutputer[K, V] {
  def write(key: K, value: V, path: String): Unit

  def close(): Unit
}

object MultipleOutputer {

  /**
    * Avro 多文件输出
    *
    * @param mo
    * @tparam K
    * @tparam V
    */
  implicit class AvroMultipleOutputer[K, V](mo: AvroMultipleOutputs) extends MultipleOutputer[K, V] {
    def write(key: K, value: V, path: String): Unit = mo.write(key, value, path)

    def close(): Unit = mo.close()
  }

  /**
    * 无格式多路径输出
    *
    * @param mo
    * @tparam K
    * @tparam V
    */
  implicit class PlainMultipleOutputer[K, V](mo: MultipleOutputs[K, V]) extends MultipleOutputer[K, V] {
    def write(key: K, value: V, path: String): Unit = mo.write(key, value, path)

    def close(): Unit = mo.close()
  }

}
