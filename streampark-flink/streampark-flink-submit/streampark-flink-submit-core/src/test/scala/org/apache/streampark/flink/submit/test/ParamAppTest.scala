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
package org.apache.streampark.flink.submit.test

import org.apache.flink.api.java.utils.ParameterTool

import java.util.regex.Pattern
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

object ParamAppTest extends App {
  val arg = Array(
    "--flink.deployment.option.parallelism",
    "10"
  )
  val argsx = Array(
    "--flink.home",
    "hdfs://nameservice1/streampark/flink/flink-1.11.1",
    "--app.name",
    "testApp123",
    "--flink.deployment.option.parallelism",
    "5"
  )
  val param = ParameterTool.fromArgs(arg).mergeWith(ParameterTool.fromArgs(argsx))
  // scalastyle:off println
  println(param)
  // scalastyle:on println

  val argsStr = "--kafkaBootstrap 127.0.0.1:9092\n" +
    "--kafkaTopic topic_test\n" +
    "--kafkaGroupId topic_group\n" +
    "--ckUrl jdbc:clickhouse://localhost:8123/default\n" +
    "--ckUsername developer\n--ckPassword Mttproxy666\n" +
    "--ckBatchSize 800\n" +
    "--ckInsertSql 'insert into default.test values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)'\n" +
    "--checkpointType 'rocksdb'\n" +
    "--checkpointInterval 5 \n" +
    "--checkpointTimeOut 3"

  //old
  val oldProgramArgs = new ArrayBuffer[String]()
  Try(argsStr.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) oldProgramArgs += x)
  println(oldProgramArgs)
  //new
  val newProgramArgs = new ArrayBuffer[String]()
  val pattern = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'")
  val regexMatcher = pattern.matcher(argsStr)
  while (regexMatcher.find()) {
    newProgramArgs += regexMatcher.group().replaceAll("\"", "")
  }
  println(newProgramArgs)
}
