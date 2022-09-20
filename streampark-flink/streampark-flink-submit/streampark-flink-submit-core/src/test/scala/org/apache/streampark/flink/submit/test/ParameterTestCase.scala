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
import org.junit.jupiter.api.{Assertions, Test}

import java.util.regex.Pattern
import scala.collection.mutable.ArrayBuffer
import scala.util.Try

class ParameterTestCase {

  @Test def readArgs(): Unit = {
    val arg = Array(
      "--flink.deployment.option.parallelism",
      "10"
    )
    val args = Array(
      "--flink.home",
      "hdfs://nameservice1/streampark/flink/flink-1.11.1",
      "--app.name",
      "testApp123",
      "--flink.deployment.option.parallelism",
      "5"
    )
    val param = ParameterTool.fromArgs(arg).mergeWith(ParameterTool.fromArgs(args))

    Assertions.assertEquals("hdfs://nameservice1/streampark/flink/flink-1.11.1", param.get("flink.home"))
    Assertions.assertEquals("testApp123", param.get("app.name"))
    Assertions.assertEquals("5", param.get("flink.deployment.option.parallelism"))
  }

  @Test def testExtractProgramArgs(): Unit = {
    val argsStr = "--url localhost:8123 \n" +
      "--insertSql 'insert into default.test values (?,?,?,?,?)'"

    //old
    val oldProgramArgs = new ArrayBuffer[String]()
    Try(argsStr.split("\\s+")).getOrElse(Array()).foreach(x => if (x.nonEmpty) oldProgramArgs += x)
    Assertions.assertEquals("localhost:8123", oldProgramArgs(1))
    Assertions.assertNotEquals("'insert into default.test values (?,?,?,?,?)'", oldProgramArgs(3))
    //new
    val newProgramArgs = new ArrayBuffer[String]()
    val pattern = Pattern.compile("[^\\s\"']+|\"[^\"]*\"|'[^']*'")
    val regexMatcher = pattern.matcher(argsStr)
    while (regexMatcher.find()) {
      newProgramArgs += regexMatcher.group().replaceAll("\"", "")
    }
    Assertions.assertEquals("localhost:8123", newProgramArgs(1))
    Assertions.assertEquals("'insert into default.test values (?,?,?,?,?)'", newProgramArgs(3))
  }
}
