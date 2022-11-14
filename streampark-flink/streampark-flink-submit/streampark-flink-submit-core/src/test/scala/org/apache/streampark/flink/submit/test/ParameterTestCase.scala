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

import org.apache.commons.lang3.StringUtils
import org.apache.flink.api.java.utils.ParameterTool
import org.apache.streampark.flink.submit.FlinkSubmitter
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
    val argsStr = "--host localhost:8123\n\n\n" +
      "--sql \"\"\"insert into table_a select * from table_b\"\"\"\n" +
      "--c d\r\n" +
      "--x yyy"
    val programArgs = new ArrayBuffer[String]()
    if (StringUtils.isNotEmpty(argsStr)) {
      val multiLineChar = "\"\"\""
      val array = argsStr.split("\\s+")
      if (array.filter(_.startsWith(multiLineChar)).isEmpty) {
        array.foreach(programArgs +=)
      } else {
        val argsArray = new ArrayBuffer[String]()
        val tempBuffer = new ArrayBuffer[String]()

        def processElement(index: Int, multiLine: Boolean): Unit = {

          if (index == array.length) {
            if (tempBuffer.nonEmpty) {
              argsArray += tempBuffer.mkString(" ")
            }
            return
          }

          val next = index + 1
          val elem = array(index)

          if (elem.trim.nonEmpty) {
            if (!multiLine) {
              if (elem.startsWith(multiLineChar)) {
                tempBuffer += elem.drop(3)
                processElement(next, true)
              } else {
                argsArray += elem
                processElement(next, false)
              }
            } else {
              if (elem.endsWith(multiLineChar)) {
                tempBuffer += elem.dropRight(3)
                argsArray += tempBuffer.mkString(" ")
                tempBuffer.clear()
                processElement(next, false)
              } else {
                tempBuffer += elem
                processElement(next, multiLine)
              }
            }
          } else {
            tempBuffer += elem
            processElement(next, false)
          }
        }

        processElement(0, false)
        argsArray.foreach(x => programArgs += x.trim)
      }
    }

    Assertions.assertEquals("localhost:8123", programArgs(1))
    Assertions.assertEquals("insert into table_a select * from table_b", programArgs(3))
    Assertions.assertEquals("d", programArgs(5))
    Assertions.assertEquals("yyy", programArgs(7))
  }

  @Test def testDynamicProperties(): Unit = {
    val dynamicProperties =
      """
        |-Denv.java.opts1="-Dfile.encoding=UTF-8"
        |-Denv.java.opts2 = "-Dfile.enc\"oding=UTF-8"
        |-Denv.java.opts3 = " -Dfile.encoding=UTF-8"
        |-Dyarn.application.id=123
        |-Dyarn.application.name="streampark job"
        |-Dyarn.application.queue=flink
        |-Ddiy.param.name=apache streampark
        |
        |""".stripMargin

    val map = FlinkSubmitter.extractDynamicProperties(dynamicProperties)
    Assertions.assertEquals(map("env.java.opts1"), "-Dfile.encoding=UTF-8")
    Assertions.assertEquals(map("env.java.opts2"), "-Dfile.enc\\\"oding=UTF-8")
    Assertions.assertEquals(map("env.java.opts3"), " -Dfile.encoding=UTF-8")
    Assertions.assertEquals(map("yarn.application.id"), "123")
    Assertions.assertEquals(map("yarn.application.name"), "streampark job")
    Assertions.assertEquals(map("yarn.application.queue"), "flink")
    Assertions.assertEquals(map("diy.param.name"), "apache streampark")
  }

}
