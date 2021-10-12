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
package com.streamxhub.streamx.flink.core

import org.apache.flink.api.java.utils.ParameterTool
import org.apache.flink.table.api.TableEnvironment
import org.apache.flink.table.descriptors.{ConnectTableDescriptor, ConnectorDescriptor}
import org.apache.flink.table.module.ModuleEntry


class TableContext(override val parameter: ParameterTool,
                   private val tableEnv: TableEnvironment) extends FlinkTableTrait(parameter, tableEnv) {

  /**
   * for scala
   *
   * @param args
   */
  def this(args: (ParameterTool, TableEnvironment)) = this(args._1, args._2)

  /**
   * for java
   *
   * @param args
   */
  def this(args: TableEnvConfig) = this(FlinkTableInitializer.initJavaTable(args))


  override def useModules(strings: String*): Unit = tableEnv.useModules(strings: _*)


  override def listFullModules(): Array[ModuleEntry] = tableEnv.listFullModules()

  @Deprecated override def connect(connectorDescriptor: ConnectorDescriptor): ConnectTableDescriptor = tableEnv.connect(connectorDescriptor)

}
