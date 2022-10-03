/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.streampark.flink.connector.influx.bean

import org.apache.streampark.common.enums.ApiType
import org.apache.streampark.common.enums.ApiType.ApiType
import org.apache.streampark.flink.connector.influx.function.{InfluxFieldFunction, InfluxTagFunction}

/**
 *
 * @param database        database
 * @param measurement     measurement
 * @param retentionPolicy retentionPolicy
 * @param tagFun          tag function
 * @param fieldFun        field function
 * @tparam T
 */
case class InfluxEntity[T](apiType: ApiType = ApiType.scala,
                           database: String,
                           measurement: String,
                           retentionPolicy: String
                          ) {
  var scalaTagFun: T => Map[String, String] = _
  var scalaFieldFun: T => Map[String, Object] = _

  var javaTagFun: InfluxTagFunction[T] = _
  var javaFieldFun: InfluxFieldFunction[T] = _

  // for java
  def this(database: String,
           measurement: String,
           retentionPolicy: String,
           javaTagFun: InfluxTagFunction[T],
           javaFieldFun: InfluxFieldFunction[T]) {
    this(ApiType.java, database, measurement, retentionPolicy)
    this.javaTagFun = javaTagFun
    this.javaFieldFun = javaFieldFun
  }

  // for scala
  def this(database: String,
           measurement: String,
           retentionPolicy: String,
           scalaTagFun: T => Map[String, String],
           scalaFieldFun: T => Map[String, Object]) {
    this(ApiType.scala, database, measurement, retentionPolicy)
    this.scalaTagFun = scalaTagFun
    this.scalaFieldFun = scalaFieldFun
  }

}
