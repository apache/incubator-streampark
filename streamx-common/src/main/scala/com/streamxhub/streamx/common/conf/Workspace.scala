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
package com.streamxhub.streamx.common.conf

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.enums.StorageType
import com.streamxhub.streamx.common.util.{HdfsUtils, SystemPropertyUtils}

/**
 * @author benjobs
 * @param storageType
 */


/**
 * just for java.
 *
 * @author benjobs
 */
object WorkspaceGetter {
  lazy val local: Workspace = Workspace.of(StorageType.LFS)
  lazy val remote: Workspace = Workspace.of(StorageType.HDFS)

  def get(storageType: StorageType): Workspace = Workspace.of(storageType)
}

object Workspace {
  /**
   * specify the file system type
   */
  def of(storageType: StorageType): Workspace = new Workspace(storageType)

}

case class Workspace(storageType: StorageType) {

  lazy val WORKSPACE: String = {
    storageType match {
      case StorageType.LFS =>
        val path = SystemPropertyUtils.get(KEY_STREAMX_WORKSPACE_LOCAL)
        require(path != null)
        path
      case StorageType.HDFS =>
        SystemPropertyUtils.get(KEY_STREAMX_WORKSPACE_REMOTE) match {
          case null => STREAMX_WORKSPACE_DEFAULT
          case p =>
            require(p.startsWith("hdfs://"))
            val path = p.replaceFirst("^hdfs://((.*):\\d+|)", "")
            s"${HdfsUtils.getDefaultFS}$path"
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

  lazy val APP_SAVEPOINTS = s"$WORKSPACE/savepoints"

  /**
   * 存放全局公共的jar
   */
  lazy val APP_JARS = s"$WORKSPACE/jars"

  /**
   * dirpath of the maven local repository with built-in compilation process
   */
  lazy val MAVEN_LOCAL_DIR = s"${Workspace(StorageType.LFS)}/mvnrepo"

  /**
   * local sourceCode dir.(for git...)
   */
  lazy val PROJECT_LOCAL_DIR = s"${Workspace(StorageType.LFS)}/project"

}

