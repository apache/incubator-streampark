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
package com.streamxhub.streamx.common.domain

import com.streamxhub.streamx.common.util.CommandUtils

import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.regex.Pattern
import scala.collection.JavaConversions._


/**
 * @author Al-assad
 * @author benjobs
 * @param flinkHome Autual flink home that must be a readable local path
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
    flinkVersion.get
  }

  // flink major version, like "1.13", "1.14"
  lazy val majorVersion: String = {
    val matcher = FLINK_VER_PATTERN.matcher(version)
    matcher.matches()
    matcher.group(1)
  }

  lazy val flinkDistJar: File = {
    val distJar = flinkLib.listFiles().filter(_.getName.matches("flink-dist_.*\\.jar"))
    distJar match {
      case x if x.isEmpty =>
        throw new IllegalArgumentException(s"[StreamX] can no found flink-dist jar in $x")
      case x if x.length > 1 =>
        throw new IllegalArgumentException(s"[StreamX] found multiple flink-dist jar in $x")
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
