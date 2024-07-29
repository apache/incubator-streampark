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
package org.apache.streampark.spark.core

import org.apache.streampark.common.conf.ConfigKeys._

import org.apache.spark.SparkConf
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.annotation.meta.getter

/** <b><code>SparkBatch</code></b> <p/> Spark streaming handle entrance <p/> */
trait SparkStreaming extends Spark {

  @(transient @getter)
  protected lazy val context: StreamingContext = {

    /** Construct StreamingContext */
    def _context(): StreamingContext = {
      val duration = sparkConf.get(KEY_SPARK_BATCH_DURATION).toInt
      new StreamingContext(sparkSession.sparkContext, Seconds(duration))
    }

    checkpoint match {
      case "" => _context()
      case checkpointPath =>
        val tmpContext =
          StreamingContext.getOrCreate(checkpointPath, _context, createOnError = createOnError)
        tmpContext.checkpoint(checkpointPath)
        tmpContext
    }
  }

  final override def start(): Unit = {
    context.start()
    context.awaitTermination()
  }

  /**
   * The purpose of the config phase is to allow the developer to set more parameters (other than
   * the agreed configuration file) by means of hooks. Such as, conf.set("spark.serializer",
   * "org.apache.spark.serializer.KryoSerializer") conf.registerKryoClasses(Array(classOf[User],
   * classOf[Order],...))
   */
  override def config(sparkConf: SparkConf): Unit = {}

  /**
   * The ready phase is an entry point for the developer to do other actions after the parameters
   * have been set, and is done after initialization and before the program starts.
   */
  override def ready(): Unit = {}

  /**
   * The destroy phase, is the last phase before jvm exits after the program has finished running,
   * and is generally used to wrap up the work.
   */
  override def destroy(): Unit = {}

}
