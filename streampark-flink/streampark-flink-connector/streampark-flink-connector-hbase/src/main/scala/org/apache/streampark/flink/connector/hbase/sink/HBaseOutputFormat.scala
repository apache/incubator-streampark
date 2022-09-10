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

package org.apache.streampark.flink.connector.hbase.sink

import org.apache.streampark.common.util.Logger
import org.apache.streampark.flink.connector.function.TransformFunction
import org.apache.streampark.flink.connector.hbase.internal.HBaseSinkFunction
import org.apache.flink.api.common.io.RichOutputFormat
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.hadoop.hbase.client.Mutation

import java.lang.{Iterable => JIter}
import java.util.Properties

class HBaseOutputFormat[T: TypeInformation](tabName: String, prop: Properties) extends RichOutputFormat[T] with Logger {

  var sinkFunction: HBaseSinkFunction[T] = _

  // for Scala
  def this(tabName: String,
           properties: Properties,
           scalaTransformFunc: T => JIter[Mutation]) = {
    this(tabName, properties)
    this.sinkFunction = new HBaseSinkFunction[T](tabName, properties, scalaTransformFunc)
  }

  // for JAVA
  def this(tabName: String,
           properties: Properties,
           javaTransformFunc: TransformFunction[T, JIter[Mutation]]) = {

    this(tabName, properties)
    this.sinkFunction = new HBaseSinkFunction[T](tabName, properties, javaTransformFunc)
  }


  var configuration: Configuration = _

  override def configure(configuration: Configuration): Unit = this.configuration = configuration

  override def open(taskNumber: Int, numTasks: Int): Unit = sinkFunction.open(this.configuration)

  override def writeRecord(record: T): Unit = sinkFunction.invoke(record, null)

  override def close(): Unit = sinkFunction.close()
}
