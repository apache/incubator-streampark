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

package com.streamxhub.streamx.flink.proxy

import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.util.{ClassLoaderUtils, Logger, Utils}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, ObjectOutputStream}
import java.net.URL
import java.util.function.{Supplier, Function => JavaFunc}
import java.util.regex.Pattern
import scala.collection.mutable.{ListBuffer, Map => MutableMap}

/**
 *
 * @author benjobs
 * @author zzz
 */

object FlinkShimsProxy extends Logger {

  private[this] val INCLUDE_PATTERN: Pattern = Pattern.compile(
    "(json4s|jackson)(.*).jar",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  )

  private[this] val SHIMS_PATTERN = Pattern.compile(
    "streamx-flink-shims_flink-(1.12|1.13|1.14|1.15)_(2.11|2.12)-(.*).jar",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  )

  private[this] val SHIMS_CLASS_LOADER_CACHE = MutableMap[String, ClassLoader]()

  private[this] def getFlinkShimsResourcePattern(flinkLargeVersion: String) =
    Pattern.compile(
      s"flink-(.*)-$flinkLargeVersion(.*).jar",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    )

  private[this] def getStreamXLibPattern(scalaVersion: String): Pattern = Pattern.compile(
    s"streamx-(.*)_$scalaVersion-(.*).jar",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  )

  /**
   * 获取 shimsClassLoader 执行代码...
   *
   * @param flinkVersion
   * @param func
   * @tparam T
   * @return
   */
  def proxy[T](flinkVersion: FlinkVersion, func: ClassLoader => T): T = {
    val shimsClassLoader = getFlinkShimsClassLoader(flinkVersion)
    ClassLoaderUtils.runAsClassLoader[T](shimsClassLoader, () => func(shimsClassLoader))
  }

  /**
   * 获取 shimsClassLoader 执行代码...
   * for java
   *
   * @param flinkVersion
   * @param func
   * @tparam T
   * @return
   */
  def proxy[T](flinkVersion: FlinkVersion, func: JavaFunc[ClassLoader, T]): T = {
    val shimsClassLoader = getFlinkShimsClassLoader(flinkVersion)
    ClassLoaderUtils.runAsClassLoader[T](shimsClassLoader, new Supplier[T]() {
      override def get(): T = func.apply(shimsClassLoader)
    })
  }

  private[this] def getFlinkShimsClassLoader(flinkVersion: FlinkVersion): ClassLoader = {
    val majorVersion = flinkVersion.majorVersion
    val scalaVersion = flinkVersion.scalaVersion
    logInfo(flinkVersion.toString)

    SHIMS_CLASS_LOADER_CACHE.getOrElseUpdate(s"${flinkVersion.fullVersion}", {
      //1) flink/lib
      val libURL = getFlinkHomeLib(flinkVersion.flinkHome)
      val shimsUrls = ListBuffer[URL](libURL: _*)

      //2) shims jar
      val appHome = System.getProperty("app.home")
      require(appHome != null, "app.home is not found on System env.")

      val libPath = new File(s"$appHome/lib")
      require(libPath.exists())

      val streamxMatcher = getStreamXLibPattern(scalaVersion)

      libPath.listFiles().foreach(jar => {
        try {
          val shimsMatcher = SHIMS_PATTERN.matcher(jar.getName)
          if (shimsMatcher.matches()) {
            if (majorVersion == shimsMatcher.group(1) && scalaVersion == shimsMatcher.group(2)) {
              shimsUrls += jar.toURI.toURL
            }
          } else {
            if (INCLUDE_PATTERN.matcher(jar.getName).matches()) {
              shimsUrls += jar.toURI.toURL
              logInfo(s"include jar lib: ${jar.getName}")
            }
            if (streamxMatcher.matcher(jar.getName).matches()) {
              shimsUrls += jar.toURI.toURL
              logInfo(s"include streamx lib: ${jar.getName}")
            }
          }
        } catch {
          case e: Exception => e.printStackTrace()
        }
      })

      new ChildFirstClassLoader(
        shimsUrls.toArray,
        Thread.currentThread().getContextClassLoader,
        getFlinkShimsResourcePattern(majorVersion)
      )
    })
  }

  private[this] def getFlinkHomeLib(flinkHome: String): List[URL] = {
    val file = new File(flinkHome, "lib")
    require(file.isDirectory, s"FLINK_HOME $file does not exist")
    file.listFiles.filter(!_.getName.startsWith("log4j")).map(_.toURI.toURL).toList
  }

  @throws[Exception]
  def getObject[T](loader: ClassLoader, obj: Object): T = {
    val arrayOutputStream = new ByteArrayOutputStream
    val result = Utils.tryWithResource(new ObjectOutputStream(arrayOutputStream))(objectOutputStream => {
      objectOutputStream.writeObject(obj)
      val byteArrayInputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray)
      Utils.tryWithResource(new ClassLoaderObjectInputStream(loader, byteArrayInputStream))(_.readObject)
    })
    result.asInstanceOf[T]
  }

}
