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
package com.streamxhub.streamx.common.util

import java.io._

object FileUtils extends org.apache.commons.io.FileUtils {

  def exists(path: String): Unit = {
    require(path != null && path.nonEmpty && new File(path).exists(), s"file $path is not exist!")
  }

  def getPathFromEnv(env: String): String = {
    val path = System.getenv(env)
    require(Utils.notEmpty(path), s"$env is not set on system env")
    val file = new File(path)
    require(file.exists(), s"$env is not exist!")
    file.getAbsolutePath
  }

  def resolvePath(parent: String, child: String): String = {
    val file = new File(parent, child)
    require(file.exists, s"${file.getAbsolutePath} is not exist!")
    file.getAbsolutePath
  }

}
