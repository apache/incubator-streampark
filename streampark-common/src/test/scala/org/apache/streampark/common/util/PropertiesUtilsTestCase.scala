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
package org.apache.streampark.common.util

import org.apache.commons.lang3.StringUtils
import org.junit.jupiter.api.{Assertions, Test}

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

class PropertiesUtilsTestCase {

  @Test def testExtractProgramArgs(): Unit = {
    val argsStr = "--host localhost:8123\n" +
      "--sql \"insert into table_a select * from table_b\"\n" +
      "--c d\r\n" +
      "--including-tables \"BASE_CARD_ETPS|BASE_CHECKED_STAT\"\n"
    val programArgs = new ArrayBuffer[String]()
    if (StringUtils.isNotEmpty(argsStr)) {
      val multiChar = "\""
      val array = argsStr.split("\\s+")
      if (!array.exists(_.startsWith(multiChar))) {
        array.foreach(
          x => {
            if (x.trim.nonEmpty) {
              programArgs += x
            }
          })
      } else {
        val argsArray = new ArrayBuffer[String]()
        val tempBuffer = new ArrayBuffer[String]()

        @tailrec
        def processElement(index: Int, multi: Boolean): Unit = {

          if (index == array.length) {
            if (tempBuffer.nonEmpty) {
              argsArray += tempBuffer.mkString(" ")
            }
            return
          }

          val next = index + 1
          val elem = array(index).trim

          if (elem.isEmpty) {
            processElement(next, multi = false)
          } else {
            if (multi) {
              if (elem.endsWith(multiChar)) {
                tempBuffer += elem.dropRight(1)
                argsArray += tempBuffer.mkString(" ")
                tempBuffer.clear()
                processElement(next, multi = false)
              } else {
                tempBuffer += elem
                processElement(next, multi)
              }
            } else {
              if (elem.startsWith(multiChar)) {
                if (elem.endsWith(multiChar)) {
                  tempBuffer += elem.drop(1).dropRight(1)
                } else {
                  tempBuffer += elem.drop(1)
                }
                processElement(next, multi = true)
              } else {
                if (elem.endsWith(multiChar)) {
                  argsArray += elem.dropRight(1)
                } else {
                  argsArray += elem
                }
                processElement(next, multi = false)
              }
            }
          }
        }

        processElement(0, multi = false)
        argsArray.foreach(x => programArgs += x)
      }
    }

    Assertions.assertEquals("localhost:8123", programArgs(1))
    Assertions.assertEquals("insert into table_a select * from table_b", programArgs(3))
    Assertions.assertEquals("d", programArgs(5))
    Assertions.assertEquals("BASE_CARD_ETPS|BASE_CHECKED_STAT", programArgs(7))
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

    val map = PropertiesUtils.extractDynamicProperties(dynamicProperties)
    Assertions.assertEquals(map("env.java.opts1"), "-Dfile.encoding=UTF-8")
    Assertions.assertEquals(map("env.java.opts2"), "-Dfile.enc\\\"oding=UTF-8")
    Assertions.assertEquals(map("env.java.opts3"), " -Dfile.encoding=UTF-8")
    Assertions.assertEquals(map("yarn.application.id"), "123")
    Assertions.assertEquals(map("yarn.application.name"), "streampark job")
    Assertions.assertEquals(map("yarn.application.queue"), "flink")
    Assertions.assertEquals(map("diy.param.name"), "apache streampark")
  }

}
