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

package org.apache.streampark.spark.connector.source

import org.apache.streampark.common.util.Logger

import org.apache.spark.SparkConf
import org.apache.spark.streaming.StreamingContext
import org.apache.spark.streaming.dstream.DStream

import scala.annotation.meta.getter
import scala.reflect.ClassTag
import scala.util.Try

/** Base source trait */
trait Source extends Logger with Serializable {

  @(transient @getter)
  val ssc: StreamingContext

  @(transient @getter)
  lazy val sparkConf: SparkConf = ssc.sparkContext.getConf

  val prefix: String

  lazy val param: Map[String, String] = sparkConf.getAll.flatMap {
    case (k, v) if k.startsWith(prefix) && Try(v.nonEmpty).getOrElse(false) =>
      Some(k.substring(prefix.length) -> v)
    case _ => None
  }.toMap

  type SourceType

  /** get DStream */
  def getDStream[R: ClassTag](f: SourceType => R): DStream[R]
}
