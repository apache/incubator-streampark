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

import scala.collection.mutable.ArrayBuffer

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
      "--insertSql1 'insert \'\'into default.test values (?,?,?,?,?)' \n" +
      "--insertSql2 'insert into default.test values (1,2,3,4,\"111\")'\n " +
      "--insertSql2 \"insert into default.test values (1,2,3,4,\'111\')\" \n" +
      "--insertSql2 'insert into default.test values (1,2,3,4,\"111\", \'22\', \'\')'"

    val array = argsStr.split("\\s")
    val argsArray = new ArrayBuffer[String]()
    val tempBuffer = new ArrayBuffer[String]()

    def processElement(index: Int, num: Int): Unit = {

      if (index == array.length) {
        if (tempBuffer.nonEmpty) {
          argsArray += tempBuffer.mkString(" ")
        }
        return
      }

      val next = index + 1
      val elem = array(index)

      if (elem.trim.nonEmpty) {
        if (num == 0) {
          if (elem.startsWith("'")) {
            tempBuffer += elem
            processElement(next, 1)
          } else if (elem.startsWith("\"")) {
            tempBuffer += elem
            processElement(next, 2)
          } else {
            argsArray += elem
            processElement(next, 0)
          }
        } else {
          tempBuffer += elem
          val end1 = elem.endsWith("'") && num == 1
          val end2 = elem.endsWith("\"") && num == 2
          if (end1 || end2) {
            argsArray += tempBuffer.mkString(" ")
            tempBuffer.clear()
            processElement(next, 0)
          } else {
            processElement(next, num)
          }
        }
      } else {
        tempBuffer += elem
        processElement(next, 0)
      }
    }

    processElement(0, 0)

    val programArgs = argsArray.map(_.trim.replaceAll("^[\"|']|[\"|']$", "")).toList

    Assertions.assertEquals("localhost:8123", programArgs(1))
    Assertions.assertEquals("insert \'\'into default.test values (?,?,?,?,?)", programArgs(3))
    Assertions.assertEquals("insert into default.test values (1,2,3,4,\"111\")", programArgs(5))
    Assertions.assertEquals("insert into default.test values (1,2,3,4,\'111\')", programArgs(7))
    Assertions.assertEquals("insert into default.test values (1,2,3,4,\"111\", \'22\', \'\')", programArgs(9))

  }
}
