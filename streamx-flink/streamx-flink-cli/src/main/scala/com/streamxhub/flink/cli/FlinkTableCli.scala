/**
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
package com.streamxhub.flink.cli

import com.streamxhub.flink.core.scala.{FlinkStreamTable, FlinkTable, StreamTableContext, TableContext}

import scala.util.{Failure, Success, Try}

object FlinkTableCli extends FlinkStreamTable {

  override def handle(context: StreamTableContext): Unit = {
    val sql = context.getSQL()
    val segment = sql.split("\\n").toList
    SQLCommandUtil.parseSQL(segment).foreach(call => {
      call.command match {
        case SQLCommand.SET =>
          context.getConfig.getConfiguration.setString(call.operands.head, call.operands(1))
        case SQLCommand.CREATE_TABLE | SQLCommand.CREATE_VIEW | SQLCommand.INSERT_INTO =>
          val ddlDml = call.operands(0)
          Try(context.executeSql(ddlDml)) match {
            case Success(_) =>
            case Failure(e) => throw new RuntimeException("[StreamX] SQL parse failed:\n" + ddlDml + "\n", e)
          }
        case _ => throw new RuntimeException("[StreamX] Unsupported command: " + call.command)
      }
    })
    logInfo(s"[StreamX] tableSQL: $sql")
  }

}
