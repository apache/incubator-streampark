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
package com.streamxhub.streamx.flink.core

import com.streamxhub.streamx.common.util.Logger
import com.streamxhub.streamx.flink.core.SqlCommand._
import org.apache.flink.streaming.api.environment.LocalStreamEnvironment
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.table.api.{EnvironmentSettings, TableEnvironment}
import org.apache.flink.table.api.bridge.scala.StreamTableEnvironment
import org.apache.flink.table.api.internal.TableEnvironmentImpl

object FlinkSqlLineage extends Logger {

  def lineageSql(sql: String): String = {
    val settings = EnvironmentSettings.newInstance.inStreamingMode.build
    val context = TableEnvironment.create(settings)
    SqlCommandParser.parseSQL(sql).foreach(x => {
      val args = if (x.operands.isEmpty) null else x.operands.head
      val command = x.command.name
      x.command match {
        case USE =>
          context.useDatabase(args)
          logInfo(s"$command: $args")
        case USE_CATALOG =>
          context.useCatalog(args)
          logInfo(s"$command: $args")
        case SELECT =>
          throw new Exception(s"[StreamX] Unsupported SELECT in current version.")
        case CREATE_FUNCTION | DROP_FUNCTION | ALTER_FUNCTION |
             CREATE_CATALOG | DROP_CATALOG |
             CREATE_TABLE | DROP_TABLE | ALTER_TABLE |
             CREATE_VIEW | DROP_VIEW |
             CREATE_DATABASE | DROP_DATABASE | ALTER_DATABASE =>
          context.executeSql(x.originSql)

        case _ =>
      }
    })
    context.asInstanceOf[TableEnvironmentImpl].explainLineage(sql)


  }

}
