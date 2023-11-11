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

import org.junit.jupiter.api.{Assertions, Test}

import scala.collection.mutable.ArrayBuffer
import scala.language.postfixOps

class PropertiesUtilsTestCase {

  @Test def testExtractProgramArgs(): Unit = {
    val args = "mysql-sync-table \n" +
      "--warehouse hdfs:///paimon \n" +
      "--database test_db \n" +
      "--table test_table \n" +
      "--mysql-conf hostname=localhost \n" +
      "--mysql-conf username=root \n" +
      "--mysql-conf password=123456 \n" +
      "--mysql-conf database-name='employees' \n" +
      "--mysql-conf table-name='employees' \n" +
      "--catalog-conf metastore=hive \n" +
      "--catalog-conf uri=thrift://localhost:9083 \n" +
      "--table-conf bucket=1 \n" +
      "--table-conf changelog-producer=input \n" +
      "--table-conf sink.parallelism=1"
    val programArgs = new ArrayBuffer[String]()
    programArgs ++= PropertiesUtils.extractArguments(args)
    println(programArgs)
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
