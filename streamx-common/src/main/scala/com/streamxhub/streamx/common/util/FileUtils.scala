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
package com.streamxhub.streamx.common.util

import java.io._
import java.net.URL
import java.util
import scala.collection.JavaConversions._

object FileUtils extends org.apache.commons.io.FileUtils {

  def createTempDir(): File = {
    val TEMP_DIR_ATTEMPTS = 10000
    val baseDir = new File(System.getProperty("java.io.tmpdir"))
    val baseName = System.currentTimeMillis + "-"
    for (counter <- 0 until TEMP_DIR_ATTEMPTS) {
      val tempDir = new File(baseDir, baseName + counter)
      if (tempDir.mkdir) {
        return tempDir
      }
    }
    throw new IllegalStateException(s"[StreamX] Failed to create directory within $TEMP_DIR_ATTEMPTS  attempts (tried $baseName 0 to $baseName ${TEMP_DIR_ATTEMPTS - 1})")
  }

  def exists(path: String): Unit = {
    require(path != null && path.nonEmpty && new File(path).exists(), s"[StreamX] FileUtils.exists: file $path is not exist!")
  }

  def getPathFromEnv(env: String): String = {
    val path = System.getenv(env)
    require(Utils.notEmpty(path), s"[StreamX] FileUtils.getPathFromEnv: $env is not set on system env")
    val file = new File(path)
    require(file.exists(), s"[StreamX] FileUtils.getPathFromEnv: $env is not exist!")
    file.getAbsolutePath
  }

  def resolvePath(parent: String, child: String): String = {
    val file = new File(parent, child)
    require(file.exists, s"[StreamX] FileUtils.resolvePath: ${file.getAbsolutePath} is not exist!")
    file.getAbsolutePath
  }

  def getSuffix(filename: String): String = {
    require(filename != null)
    filename.drop(filename.lastIndexOf("."))
  }

  def listFileAsURL(dirPath: String): util.List[URL] = {
    new File(dirPath) match {
      case x if x.exists() && x.isDirectory =>
        val files = x.listFiles()
        if (files != null && files.nonEmpty) {
          files.map(f => f.toURI.toURL).toList
        } else {
          util.Collections.emptyList()
        }
      case _ => util.Collections.emptyList()
    }
  }


}
