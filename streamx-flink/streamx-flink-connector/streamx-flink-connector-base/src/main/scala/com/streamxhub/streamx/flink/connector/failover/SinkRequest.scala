/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.flink.connector.failover

import java.util
import java.util.regex.Pattern
import scala.collection.JavaConversions._

case class SinkRequest(records: util.List[String], var attemptCounter: Int = 0) {
  def incrementCounter(): Unit = attemptCounter += 1

  def size: Int = records.size()

  private[this] lazy val TABLE_REGEXP = Pattern.compile(
    "(insert\\s+into|update|delete)\\s+(.*?)(\\(|\\s+)",
    Pattern.CASE_INSENSITIVE
  )

  private[this] lazy val INSERT_REGEXP = Pattern.compile(
    "^(.*)\\s+(values|value)(.*)",
    Pattern.CASE_INSENSITIVE
  )

  lazy val sqlStatement: String = {
    val matcher = INSERT_REGEXP.matcher(records.head)
    if (!matcher.find()) null; else {
      val prefix = matcher.group(1)
      val values = records.map(x => {
        val valueMatcher = INSERT_REGEXP.matcher(x)
        if (valueMatcher.find()) {
          valueMatcher.group(3)
        } else {
          null
        }
      }).mkString(",")
      s"$prefix VALUES $values"
    }
  }

  lazy val table: String = {
    // 1) insert into default.table(c1,c2) values ...
    // 2) insert into default.table values ..."
    // 3) update default.table where ..."
    // 4) delete default.table where ..."
    val matcher = TABLE_REGEXP.matcher(records.head)
    if (matcher.find()) {
      matcher.group(2)
    } else null
  }

}

