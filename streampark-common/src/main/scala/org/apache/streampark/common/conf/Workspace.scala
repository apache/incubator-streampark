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

package org.apache.streampark.common.conf

import org.apache.streampark.common.enums.StorageType
import org.apache.streampark.common.util.{HdfsUtils, SystemPropertyUtils}
import org.apache.streampark.common.util.Implicits._

import java.net.URI

object Workspace {

  def of(storageType: StorageType): Workspace = Workspace(storageType)

  lazy val local: Workspace = Workspace.of(StorageType.LFS)

  lazy val remote: Workspace = Workspace.of(StorageType.HDFS)

  private[this] lazy val localWorkspace = local.WORKSPACE

  /** local build path */
  lazy val APP_LOCAL_DIST = s"$localWorkspace/dist"

  /** dirPath of the maven local repository with built-in compilation process */
  lazy val MAVEN_LOCAL_PATH = s"$localWorkspace/mvnrepo"

  /** local sourceCode path.(for git...) */
  lazy val PROJECT_LOCAL_PATH = s"$localWorkspace/project"

  /** local log path. */
  lazy val LOG_LOCAL_PATH = s"$localWorkspace/logs"

  /** project build log path. */
  lazy val PROJECT_BUILD_LOG_PATH = s"$LOG_LOCAL_PATH/build_logs"

  /** project archives path */
  lazy val ARCHIVES_FILE_PATH = s"${remote.WORKSPACE}/historyserver/archive"

}

case class Workspace(storageType: StorageType) {

  private[this] def getConfigValue[T](option: InternalOption): T = {
    val s = SystemPropertyUtils.get(option.key)
    val v = InternalConfigHolder.get(option).asInstanceOf[T]
    val d = option.defaultValue.asInstanceOf[T]
    (s, v) match {
      case (null, null) => d
      case (null, b) => b
      case (a, null) => a.cast[T](option.classType)
      case (a, b) => if (b == d) a.cast[T](option.classType) else b
    }
  }

  lazy val WORKSPACE: String = {
    storageType match {
      case StorageType.LFS =>
        val path: String = getConfigValue[String](CommonConfig.STREAMPARK_WORKSPACE_LOCAL)
        require(path != null, "[StreamPark] streampark.workspace.local must not be null")
        path
      case StorageType.HDFS =>
        val path: String = getConfigValue[String](CommonConfig.STREAMPARK_WORKSPACE_REMOTE)
        path match {
          case p if p.isEmpty =>
            s"${HdfsUtils.getDefaultFS}${CommonConfig.STREAMPARK_WORKSPACE_REMOTE.defaultValue}"
          case p =>
            val defaultFs = HdfsUtils.getDefaultFS
            if (p.startsWith("hdfs://")) {
              if (p.startsWith(defaultFs)) {
                p
              } else {
                val path = URI.create(p).getPath
                s"${HdfsUtils.getDefaultFS}$path"
              }
            } else {
              s"${HdfsUtils.getDefaultFS}$p"
            }
        }
    }
  }

  lazy val APP_PLUGINS = s"$WORKSPACE/plugins"

  lazy val APP_CLIENT = s"$WORKSPACE/client"

  /** store flink multi version support jars */
  lazy val APP_SHIMS = s"$WORKSPACE/shims"

  lazy val APP_UPLOADS = s"$WORKSPACE/uploads"

  lazy val APP_PYTHON = s"$WORKSPACE/python"

  lazy val APP_PYTHON_VENV = s"$APP_PYTHON/venv.zip"

  lazy val APP_WORKSPACE = s"$WORKSPACE/workspace"

  lazy val APP_FLINK = s"$WORKSPACE/flink"

  lazy val APP_SPARK = s"$WORKSPACE/spark"

  lazy val APP_BACKUPS = s"$WORKSPACE/backups"

  lazy val APP_SAVEPOINTS = s"$WORKSPACE/savepoints"

  /** store global public jars */
  lazy val APP_JARS = s"$WORKSPACE/jars"

}
