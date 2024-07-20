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

import org.apache.streampark.common.util.{CommandUtils, Logger}
import org.apache.streampark.common.util.Implicits._

import org.apache.commons.lang3.StringUtils

import java.io.File
import java.net.{URL => NetURL}
import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}
import java.util.function.Consumer
import java.util.regex.Pattern

import scala.collection.mutable

/** @param sparkHome actual spark home that must be a readable local path */
class SparkVersion(val sparkHome: String) extends java.io.Serializable with Logger {

  private[this] lazy val SPARK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$")

  private[this] lazy val SPARK_VERSION_PATTERN = Pattern.compile("(version) (\\d+\\.\\d+\\.\\d+)")

  private[this] lazy val SPARK_SCALA_VERSION_PATTERN =
    Pattern.compile("^spark-core_(.*)-[0-9].*.jar$")

  lazy val scalaVersion: String = {
    val matcher = SPARK_SCALA_VERSION_PATTERN.matcher(sparkCoreJar.getName)
    if (matcher.matches()) {
      matcher.group(1);
    } else {
      "2.12"
    }
  }

  def checkVersion(throwException: Boolean = true): Boolean = {
    version.split("\\.").map(_.trim.toInt) match {
      case Array(3, v, _) if v >= 1 && v <= 3 => true
      case _ =>
        if (throwException) {
          throw new UnsupportedOperationException(s"Unsupported flink version: $version")
        } else {
          false
        }
    }
  }

  lazy val fullVersion: String = s"${version}_$scalaVersion"

  lazy val sparkLib: File = {
    require(sparkHome != null, "[StreamPark] sparkHome must not be null.")
    require(new File(sparkHome).exists(), "[StreamPark] sparkHome must be exists.")
    val lib = new File(s"$sparkHome/jars")
    require(
      lib.exists() && lib.isDirectory,
      s"[StreamPark] $sparkHome/lib must be exists and must be directory.")
    lib
  }

  lazy val sparkLibs: List[NetURL] = sparkLib.listFiles().map(_.toURI.toURL).toList

  lazy val majorVersion: String = {
    if (version == null) {
      null
    } else {
      val matcher = SPARK_VER_PATTERN.matcher(version)
      matcher.matches()
      matcher.group(1)
    }
  }

  lazy val version: String = {
    val sparkVersion = new AtomicReference[String]
    val cmd = List(s"$sparkHome/bin/spark-submit --version")
    val success = new AtomicBoolean(false)
    val buffer = new mutable.StringBuilder
    CommandUtils.execute(
      sparkHome,
      cmd,
      new Consumer[String]() {
        override def accept(out: String): Unit = {
          buffer.append(out).append("\n")
          val matcher = SPARK_VERSION_PATTERN.matcher(out)
          if (matcher.find && StringUtils.isBlank(sparkVersion.get())) {
            success.set(true)
            sparkVersion.set(matcher.group(2))
          }
        }
      })
    logInfo(buffer.toString())
    if (!success.get()) {
      throw new IllegalStateException(s"[StreamPark] parse spark version failed. $buffer")
    }
    buffer.clear()
    sparkVersion.get
  }

  lazy val sparkCoreJar: File = {
    val distJar = sparkLib.listFiles().filter(_.getName.matches("spark-core.*\\.jar"))
    distJar match {
      case x if x.isEmpty =>
        throw new IllegalArgumentException(s"[StreamPark] can no found spark-core jar in $sparkLib")
      case x if x.length > 1 =>
        throw new IllegalArgumentException(
          s"[StreamPark] found multiple spark-core jar in $sparkLib")
      case _ =>
    }
    distJar.head
  }

  override def toString: String =
    s"""
       |----------------------------------------- spark version -----------------------------------
       |     sparkHome    : $sparkHome
       |     sparkVersion : $version
       |     scalaVersion : $scalaVersion
       |-------------------------------------------------------------------------------------------
       |""".stripMargin

}
