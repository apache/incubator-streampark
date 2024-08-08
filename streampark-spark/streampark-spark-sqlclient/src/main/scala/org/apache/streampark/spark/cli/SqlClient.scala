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

package org.apache.streampark.spark.cli

import org.apache.streampark.common.conf.ConfigKeys.KEY_SPARK_SQL
import org.apache.streampark.common.util.DeflaterUtils
import org.apache.streampark.spark.core.{SparkBatch, SparkStreaming}
import org.apache.streampark.spark.core.util.ParameterTool

import org.apache.commons.lang3.StringUtils
import org.apache.spark.sql.DataFrame

import scala.collection.mutable.ArrayBuffer
import scala.util.{Failure, Success, Try}

object SqlClient extends App {

  val arguments = ArrayBuffer(args: _*)

  private[this] val parameterTool = ParameterTool.fromArgs(args)

  private[this] val sparkSql = {
    val sql = parameterTool.get(KEY_SPARK_SQL())
    require(StringUtils.isNotBlank(sql), "Usage: spark sql cannot be null")
    Try(DeflaterUtils.unzipString(sql)) match {
      case Success(value) => value
      case Failure(_) =>
        throw new IllegalArgumentException("Usage: spark sql is invalid or null, please check")
    }
  }

  private[this] val defaultMode = "BATCH"

  private[this] val mode = defaultMode

  mode match {
    case "STREAMING" | "AUTOMATIC" => StreamSqlApp.main(arguments.toArray)
    case "BATCH" => BatchSqlApp.main(arguments.toArray)
    case _ =>
      throw new IllegalArgumentException(
        "Usage: runtime execution-mode invalid, optional [STREAMING|BATCH|AUTOMATIC]")
  }

  private[this] object BatchSqlApp extends SparkBatch {
    override def handle(sql: String): DataFrame = super.handle(sql)
  }

  private[this] object StreamSqlApp extends SparkStreaming {
    override def handle(sql: String): DataFrame = super.handle(sql)
  }

}
