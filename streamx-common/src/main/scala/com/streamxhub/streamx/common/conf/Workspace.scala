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
package com.streamxhub.streamx.common.conf

import com.streamxhub.streamx.common.enums.StorageType
import com.streamxhub.streamx.common.util.HdfsUtils

import java.net.URI

/**
 * @author benjobs
 * @param storageType
 */

object Workspace {

  lazy val local: Workspace = Workspace.of(StorageType.LFS)

  lazy val remote: Workspace = Workspace.of(StorageType.HDFS)

  def of(storageType: StorageType): Workspace = Workspace(storageType)

}

case class Workspace(storageType: StorageType) {

  lazy val WORKSPACE: String = {
    storageType match {
      case StorageType.LFS =>
        val path: String = ConfigHub.get(CommonConfig.STREAMX_WORKSPACE_LOCAL)
        require(path != null, "[StreamX] streamx.workspace.local must not be null")
        path
      case StorageType.HDFS =>
        val path: String = ConfigHub.get(CommonConfig.STREAMX_WORKSPACE_REMOTE)
        path match {
          case p if p.isEmpty =>
            s"${HdfsUtils.getDefaultFS}${CommonConfig.STREAMX_WORKSPACE_REMOTE.defaultValue}"
          case p =>
            val defaultFs = HdfsUtils.getDefaultFS
            if (p.startsWith("hdfs://")) {
              if (p.startsWith(defaultFs)) {
                p
              } else {
                var path = URI.create(p).getPath
                s"${HdfsUtils.getDefaultFS}$path"
              }
            } else {
              s"${HdfsUtils.getDefaultFS}$p"
            }
        }
    }
  }

  lazy val APP_PLUGINS = s"$WORKSPACE/plugins"

  /**
   * 存放不同版本flink相关的jar
   */
  lazy val APP_SHIMS = s"$WORKSPACE/shims"

  lazy val APP_UPLOADS = s"$WORKSPACE/uploads"

  lazy val APP_WORKSPACE = s"$WORKSPACE/workspace"

  lazy val APP_FLINK = s"$WORKSPACE/flink"

  lazy val APP_BACKUPS = s"$WORKSPACE/backups"

  /**
   * 本地构建项目存放路径
   */
  lazy val APP_LOCAL_DIST = s"$WORKSPACE/dist"

  lazy val APP_SAVEPOINTS = s"$WORKSPACE/savepoints"

  /**
   * 存放全局公共的jar
   */
  lazy val APP_JARS = s"$WORKSPACE/jars"

  /**
   * dirpath of the maven local repository with built-in compilation process
   */
  lazy val MAVEN_LOCAL_DIR = s"${Workspace.local.WORKSPACE}/mvnrepo"

  /**
   * local sourceCode dir.(for git...)
   */
  lazy val PROJECT_LOCAL_DIR = s"${Workspace.local.WORKSPACE}/project"

}

