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

package org.apache.streampark.flink.packer

import org.apache.streampark.common.conf.Workspace
import org.apache.streampark.common.constants.Constants
import org.apache.streampark.common.util.Logger

import org.apache.commons.io.FileUtils

import java.io.File

import scala.util.Try

/** Garbage resource collector during packing. */
object PackerResourceGC extends Logger {

  private val appWorkspacePath: String = Workspace.local.APP_WORKSPACE

  /**
   * Start a building legacy resources collection process.
   *
   * @param expiredHours
   *   Expected expiration time of building resources.
   */
  def startGc(expiredHours: Integer): Unit = {
    val appWorkspace = new File(appWorkspacePath)
    if (!appWorkspace.exists()) return
    val evictedBarrier = System.currentTimeMillis - expiredHours * 3600 * 1000

    // find flink building path that should be evicted, which are based on pattern
    // matching of filename.
    val evictedFiles = appWorkspace.listFiles
      .filter(_.isDirectory)
      .filter(_.getName.contains("@"))
      .flatMap(findLastModifiedOfSubFile)
      .filter(_._2 < evictedBarrier)
      .map(_._1)

    if (evictedFiles.isEmpty) return
    logInfo(s"Delete expired building resources, ${evictedFiles.mkString(", ")}")
    evictedFiles.foreach(path => Try(FileUtils.deleteDirectory(path)))
  }

  private def findLastModifiedOfSubFile(file: File): Array[(File, Long)] = {
    val isApplicationMode =
      file.listFiles.map(_.getName).exists(_.contains(Constants.JAR_SUFFIX))
    if (isApplicationMode) {
      Array(file -> file.listFiles.map(_.lastModified).max)
    } else {
      file.listFiles
        .filter(_.isDirectory)
        .map(subFile => subFile -> subFile.listFiles.map(_.lastModified).max)
    }
  }

}
