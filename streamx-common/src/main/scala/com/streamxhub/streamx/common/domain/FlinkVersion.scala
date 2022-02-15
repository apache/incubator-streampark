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
package com.streamxhub.streamx.common.domain

import com.streamxhub.streamx.common.util.CommandUtils
import org.apache.commons.lang3.StringUtils

import java.io.File
import java.net.{URL => NetURL}
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.regex.Pattern
import scala.collection.JavaConversions._

/**
 * @param flinkHome actual flink home that must be a readable local path
 * @author benjobs
 */
class FlinkVersion(val flinkHome: String) extends java.io.Serializable {

  private[this] lazy val FLINK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$")

  private[this] lazy val FLINK_VERSION_PATTERN = Pattern.compile("^Version: (.*), Commit ID: (.*)$")

  private[this] lazy val FLINK_SCALA_VERSION_PATTERN = Pattern.compile("^flink-dist_(.*)-(.*).jar$")

  lazy val scalaVersion: String = {
    val matcher = FLINK_SCALA_VERSION_PATTERN.matcher(flinkDistJar.getName)
    matcher.matches()
    matcher.group(1)
  }

  lazy val fullVersion: String = s"${version}_$scalaVersion"

  lazy val flinkLib: File = {
    require(flinkHome != null, "[StreamX] flinkHome must not be null.")
    require(new File(flinkHome).exists(), "[StreamX] flinkHome must be exists.")
    val lib = new File(s"$flinkHome/lib")
    require(lib.exists() && lib.isDirectory, s"[StreamX] $flinkHome/lib must be exists and must be directory.")
    lib
  }

  lazy val flinkLibs: List[NetURL] = flinkLib.listFiles().map(_.toURI.toURL).toList

  lazy val version: String = {
    val flinkVersion = new AtomicReference[String]
    val cmd = List(
      s"cd ${flinkLib.getAbsolutePath}",
      s"java -classpath ${flinkDistJar.getAbsolutePath} org.apache.flink.client.cli.CliFrontend --version"
    )
    CommandUtils.execute(cmd, new Consumer[String]() {
      override def accept(out: String): Unit = {
        val matcher = FLINK_VERSION_PATTERN.matcher(out)
        if (matcher.find) {
          flinkVersion.set(matcher.group(1))
        }
      }
    })
    val version = flinkVersion.get
    doCheckVersion(version)
    version
  }

  def doCheckVersion(version: String): Unit = {
    if (StringUtils.isEmpty(version)) {
      throw new IllegalStateException("[StreamX] parse flink version failed.")
    }
    if (version.split("\\.").length != 3) {
      throw new IllegalStateException("[StreamX] parse illegal version.")
    }
  }

  // flink major version, like "1.13", "1.14"
  lazy val majorVersion: String = {
    if (version == null) {
      null
    } else {
      val matcher = FLINK_VER_PATTERN.matcher(version)
      matcher.matches()
      matcher.group(1)
    }
  }

  lazy val flinkDistJar: File = {
    val distJar = flinkLib.listFiles().filter(_.getName.matches("flink-dist_.*\\.jar"))
    distJar match {
      case x if x.isEmpty =>
        throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $flinkLib")
      case x if x.length > 1 =>
        throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $flinkLib")
      case _ =>
    }
    distJar.head
  }

  // streamx flink shims version, like "streamx-flink-shims_flink-1.13"
  lazy val shimsVersion: String = s"streamx-flink-shims_flink-$majorVersion"

  override def toString: String =
    s"""
       |----------------------------------------- flink version -----------------------------------
       |     flinkHome    : $flinkHome
       |     distJarName  : ${flinkDistJar.getName}
       |     flinkVersion : $version
       |     majorVersion : $majorVersion
       |     scalaVersion : $scalaVersion
       |     shimsVersion : $shimsVersion
       |-------------------------------------------------------------------------------------------
       |""".stripMargin

}
