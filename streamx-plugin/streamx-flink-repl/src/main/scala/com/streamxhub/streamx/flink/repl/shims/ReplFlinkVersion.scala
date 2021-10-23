/*
 * Copyright (c) 2021 The StreamX Project
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
package com.streamxhub.streamx.flink.repl.shims

import java.util.regex.Pattern

/**
 * author: Al-assad
 *
 * @param version   Actual flink version number, like "1.13.2", "1.14.0"
 * @param flinkHome Autual flink home that must be a readable local path
 */
case class ReplFlinkVersion(version: String, flinkHome: String) {

  private val FLINK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$")

  // flink major version, like "1.13", "1.14"
  val majorVersion: String = {
    val matcher = FLINK_VER_PATTERN.matcher(version)
    matcher.matches()
    matcher.group(1)
  }

  // streamx flink shims version, like "streamx-flink-shims_flink-1.13"
  val shimsVersion: String = s"streamx-flink-shims_flink-$majorVersion"

  override def toString: String =
    s"ReplFlinkVersion@(version=$version, flinkHome=$flinkHome, majorVersion=$majorVersion, shimsVersion=$shimsVersion)"
}
