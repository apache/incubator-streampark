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
package com.streamxhub.streamx.test.stream

import com.streamxhub.streamx.flink.common.conf.ParameterCli

object ParameterCliApp extends App {

  /**
   * 测试从yaml中解析参数.....
   */
  val array = "--option /Users/benjobs/Github/StreamX/streamx-flink/streamx-flink-test/assembly/conf/application.yml -s hdfs://nameservice1/flink/savepoints/savepoint-142cb6-cc4258164dd4 -p 2345"
  ParameterCli.main(array.split("\\s+"))

}
