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

package org.apache.streampark.flink.packer.docker

import org.apache.streampark.common.fs.LfsOperator
import org.apache.streampark.common.util.HadoopConfigUtils

import javax.annotation.Nullable

import java.nio.file.Paths

/**
 * flink-hadoop integration docker image template.
 *
 * @param workspacePath
 *   Path of dockerfile workspace, it should be a directory.
 * @param flinkBaseImage
 *   Flink base docker image name, see https://hub.docker.com/_/flink.
 * @param flinkMainJarPath
 *   Path of flink job main jar which would copy to $FLINK_HOME/usrlib/
 * @param flinkExtraLibPaths
 *   Path of additional flink lib path which would copy to $FLINK_HOME/lib/
 * @param hadoopConfDirPath
 *   Path of hadoop conf directory.
 * @param hiveConfDirPath
 *   Path of hive conf directory.
 */
case class FlinkHadoopDockerfileTemplate(
    workspacePath: String,
    flinkBaseImage: String,
    flinkMainJarPath: String,
    flinkExtraLibPaths: Set[String],
    @Nullable hadoopConfDirPath: String,
    @Nullable hiveConfDirPath: String)
  extends FlinkDockerfileTemplateTrait {

  val hadoopConfDir: String =
    workspace
      .relativize(Paths.get(Option(hadoopConfDirPath).getOrElse("")))
      .toString

  val hiveConfDir: String =
    workspace
      .relativize(Paths.get(Option(hiveConfDirPath).getOrElse("")))
      .toString

  /** offer content of DockerFile */
  override def offerDockerfileContent: String = {
    var dockerfile =
      s"""FROM $flinkBaseImage
         |RUN mkdir -p $FLINK_HOME/usrlib
         |""".stripMargin
    if (hadoopConfDir.nonEmpty) {
      dockerfile +=
        s"""
           |COPY $hadoopConfDir /opt/hadoop-conf
           |ENV HADOOP_CONF_DIR /opt/hadoop-conf
           |""".stripMargin
    }
    if (hiveConfDir.nonEmpty) {
      dockerfile +=
        s"""
           |COPY $hiveConfDir /opt/hive-conf
           |ENV HIVE_CONF_DIR /opt/hive-conf
           |""".stripMargin
    }
    dockerfile +=
      s"""
         |COPY $extraLibName $FLINK_HOME/lib/
         |COPY $mainJarName $FLINK_HOME/usrlib/$mainJarName
         |""".stripMargin
    dockerfile
  }

}

object FlinkHadoopDockerfileTemplate {

  /** Use relevant system variables as the value of hadoopConfDirPath, hiveConfDirPath. */
  def fromSystemHadoopConf(
      workspacePath: String,
      flinkBaseImage: String,
      flinkMainJarPath: String,
      flinkExtraLibPaths: Set[String]): FlinkHadoopDockerfileTemplate = {
    // get hadoop and hive config directory from system and copy to workspacePath
    val hadoopConfDir = HadoopConfigUtils.getSystemHadoopConfDir match {
      case None => ""
      case hadoopConf if !LfsOperator.exists(hadoopConf.get) => ""
      case hadoopConf =>
        val dstDir = s"$workspacePath/hadoop-conf"
        LfsOperator.mkCleanDirs(dstDir)
        LfsOperator.copyDir(hadoopConf.get, dstDir)
        dstDir
    }
    val hiveConfDir = HadoopConfigUtils.getSystemHiveConfDir match {
      case None => ""
      case hiveConf if !LfsOperator.exists(hiveConf.get) => ""
      case hiveConf =>
        val dstDir = s"$workspacePath/hive-conf"
        LfsOperator.mkCleanDirs(dstDir)
        LfsOperator.copyDir(hiveConf.get, dstDir)
        dstDir
    }
    FlinkHadoopDockerfileTemplate(
      workspacePath,
      flinkBaseImage,
      flinkMainJarPath,
      flinkExtraLibPaths,
      hadoopConfDir,
      hiveConfDir)
  }

}
