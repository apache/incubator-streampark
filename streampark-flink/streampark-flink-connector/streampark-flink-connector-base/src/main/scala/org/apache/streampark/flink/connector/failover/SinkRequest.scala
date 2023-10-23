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

package org.apache.streampark.flink.connector.failover

import org.apache.streampark.common.util.Logger

import java.util
import java.util.regex.Pattern

import org.apache.streampark.common.Constant

import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

case class SinkRequest(records: util.List[String], var attemptCounter: Int = 0) extends Logger {
  def incrementCounter(): Unit = attemptCounter += 1

  def size: Int = records.size()

  private[this] lazy val TABLE_REGEXP =
    Pattern.compile("(insert\\s+into|update|delete)\\s+(.*?)(\\(|\\s+)", Pattern.CASE_INSENSITIVE)

  private[this] lazy val INSERT_REGEXP =
    Pattern.compile("^(.*?)\\s+(values|value)(.*)", Pattern.CASE_INSENSITIVE)

  lazy val sqlStatement: List[String] = {
    var result: List[String] = List.empty[String]
    val prefixMap: mutable.Map[String, ListBuffer[String]] =
      mutable.Map.empty[String, ListBuffer[String]]

    records.forEach(
      x => {
        // group statements by the part before 'value(s)' in insert statements.
        val valueMatcher = INSERT_REGEXP.matcher(x)
        if (valueMatcher.find()) {
          val prefix = valueMatcher.group(1)
          prefixMap.get(prefix) match {
            case Some(value) => value += valueMatcher.group(3)
            case None => prefixMap(prefix) = ListBuffer(valueMatcher.group(3))
          }
        } else {
          // other statements will be ignored.
          logWarn(s"ignore record: $x")
        }
      })
    if (prefixMap.nonEmpty) {
      // combine statements by the part before 'value(s)' in insert statements.
      result = prefixMap.map(m => s"""${m._1} VALUES ${m._2.mkString(",")}""").toList
    }

    logDebug(s"script to commit: ${result.mkString(Constant.SEMICOLON)}")

    result
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
