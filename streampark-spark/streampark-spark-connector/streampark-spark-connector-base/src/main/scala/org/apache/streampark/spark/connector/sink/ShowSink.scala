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

package org.apache.streampark.spark.connector.sink

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.streaming.Time

/**
 * print sink
 *
 * @param sc
 * @param initParams
 * @tparam T
 */
class ShowSink[T](
    @transient override val sc: SparkContext,
    initParams: Map[String, String] = Map.empty[String, String])
  extends Sink[T] {

  override val prefix: String = "spark.sink.show."

  private lazy val prop = filterProp(param, initParams, prefix)

  private val num = prop.getProperty("num", "10").toInt

  override def sink(rdd: RDD[T], time: Time = Time(System.currentTimeMillis())): Unit = {
    val firstNum = rdd.take(num + 1)
    // scalastyle:off println
    println("-------------------------------------------")
    println("Time: " + time)
    println("-------------------------------------------")
    firstNum.take(num).foreach(println)
    if (firstNum.length > num) println("...")
    println()
    // scalastyle:on println
  }
}
