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
package com.streamxhub.streamx.flink.packer.pipeline

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.enums.DevelopmentMode

import scala.collection.mutable.ArrayBuffer

/**
 * @author Al-assad
 */
object BuildPipelineHelper {

  private[this] val localWorkspace = Workspace.local

  /**
   * Let appName more suitable as part of the file path
   */
  def letAppNameSafe(appName: String): String = appName.replace(" ", "_")

  /**
   * Extract provided flink libs from StreamX Workspace
   */
  @throws[UnsupportedOperationException]
  def extractFlinkProvidedLibs(buildParams: FlinkBuildParam): Set[String] = {
    val providedLibs = ArrayBuffer(
      localWorkspace.APP_JARS,
      localWorkspace.APP_PLUGINS,
      buildParams.customFlinkUsrJarPath)
    if (buildParams.developmentMode == DevelopmentMode.FLINKSQL) {
      providedLibs += {
        val version = buildParams.flinkVersion.version.split("\\.").map(_.trim.toInt)
        version match {
          case Array(1, 12, _) => s"${localWorkspace.APP_SHIMS}/flink-1.12"
          case Array(1, 13, _) => s"${localWorkspace.APP_SHIMS}/flink-1.13"
          case Array(1, 14, _) => s"${localWorkspace.APP_SHIMS}/flink-1.14"
          case _ => throw new UnsupportedOperationException(s"Unsupported flink version: ${buildParams.flinkVersion}")
        }
      }
    }
    providedLibs.toSet
  }


}
