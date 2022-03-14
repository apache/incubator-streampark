/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.streamx.test.flink.scala.tablesql

import java.util.Scanner
import scala.language.postfixOps


object SQLRegExpTest extends App {

  val sql =
    """
      |CREATE TABLE user_log (
      |    user_id VARCHAR,
      |    item_id VARCHAR,
      |    category_id VARCHAR,
      |    behavior VARCHAR,
      |    ts TIMESTAMP(3)
      |) WITH (
      |'connector.type' = 'kafka', --kafka 123
      |connector.version = universal, --fdsafdsafdsafds
      |connector.topic = user_behavior, --fdsafdsafdsafds
      |connector.properties.bootstrap.servers=test-hadoop-7:9092,test-hadoop-8:9092,test-hadoop-9:9092,
      |connector.startup-mode = earliest-offset,
      |update-mode = append,
      |format.type = json,
      |format.derive-schema = 'true'
      |);
      |""".stripMargin

  val reg = "WITH\\s*\\(\\s*\\n+((.*)\\s*=(.*)(,|)\\s*\\n+)+\\);".r
  val matcher = reg.pattern.matcher(sql)
  if (matcher.find()) {
    val segment = matcher.group()
    val scanner = new Scanner(segment)
    while (scanner.hasNextLine) {
      val line = scanner.nextLine().replaceAll("--(.*)$", "") trim
      val propReg = "\\s*(.*)\\s*=(.*)(,|)\\s*"
      if (line.matches(propReg)) {
        var newLine = line
          .replaceAll("^'|^", "'")
          .replaceAll("('|)\\s*=\\s*('|)", "' = '")
          .replaceAll("('|),\\s*$", "',")
        if (!line.endsWith(",")) {
          newLine = newLine.replaceFirst("('|)\\s*$", "'")
        }
        println(newLine)
      }
    }
  }


}
