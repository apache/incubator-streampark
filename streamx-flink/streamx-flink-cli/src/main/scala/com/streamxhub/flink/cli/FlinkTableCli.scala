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

import com.streamxhub.flink.core.scala.{FlinkTable, TableContext}

import scala.util.{Failure, Success, Try}
import scala.collection.JavaConversions._

object FlinkTableCli extends FlinkTable {

  override def handle(context: TableContext): Unit = {
    SQLCommandUtil.parseSQL(context.getStatement()).foreach(call => {
      call.command match {
        case SQLType.SET =>
          val key = call.operands(0)
          val value = call.operands(1)
          context.getConfig.getConfiguration.setString(key, value)
        case SQLType.CREATE_TABLE | SQLType.CREATE_VIEW | SQLType.INSERT_INTO => {
          val ddlDml = call.operands(0)
          Try(context.sqlUpdate(ddlDml)) match {
            case Success(_) =>
            case Failure(e) => throw new RuntimeException("[StreamX] SQL parse failed:\n" + ddlDml + "\n", e)
          }
        }
        case _ => throw new RuntimeException("[StreamX] Unsupported command: " + call.command)
      }
    })
  }

}
