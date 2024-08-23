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

import java.io.File
import java.util.function.Consumer
import java.util.regex.Pattern

import scala.collection.mutable

/** @param sparkHome actual spark home that must be a readable local path */
class SparkVersion(val sparkHome: String) extends Serializable with Logger {

  private[this] lazy val SPARK_VER_PATTERN = Pattern.compile("^(\\d+\\.\\d+)(\\.)?.*$")

  private[this] lazy val SPARK_VERSION_PATTERN = Pattern.compile("\\s{2}version\\s(\\d+\\.\\d+\\.\\d+)")

  private[this] lazy val SPARK_SCALA_VERSION_PATTERN = Pattern.compile("Using\\sScala\\sversion\\s(\\d+\\.\\d+)")

  val (version, scalaVersion) = {
    var sparkVersion: String = null
    var scalaVersion: String = null
    val cmd = List(s"export SPARK_HOME=$sparkHome&&$sparkHome/bin/spark-submit --version")
    val buffer = new mutable.StringBuilder

    CommandUtils.execute(
      sparkHome,
      cmd,
      new Consumer[String]() {
        override def accept(out: String): Unit = {
          buffer.append(out).append("\n")
          val matcher = SPARK_VERSION_PATTERN.matcher(out)
          if (matcher.find) {
            sparkVersion = matcher.group(1)
          } else {
            val matcher1 = SPARK_SCALA_VERSION_PATTERN.matcher(out)
            if (matcher1.find) {
              scalaVersion = matcher1.group(1)
            }
          }
        }
      })

    logInfo(buffer.toString())
    if (sparkVersion == null || scalaVersion == null) {
      throw new IllegalStateException(s"[StreamPark] parse spark version failed. $buffer")
    }
    buffer.clear()
    (sparkVersion, scalaVersion)
  }

  lazy val majorVersion: String = {
    if (version == null) {
      null
    } else {
      val matcher = SPARK_VER_PATTERN.matcher(version)
      matcher.matches()
      matcher.group(1)
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

  def checkVersion(throwException: Boolean = true): Boolean = {
    version.split("\\.").map(_.trim.toInt) match {
      case Array(v, _, _) if v == 2 || v == 3 => true
      case _ =>
        if (throwException) {
          throw new UnsupportedOperationException(s"Unsupported spark version: $version")
        } else {
          false
        }
    }
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
