/*
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License") you may not use this file except in compliance
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
package com.streamxhub.streamx.flink.proxy

import com.streamxhub.streamx.common.domain.FlinkVersion
import com.streamxhub.streamx.common.util.{ClassLoaderUtils, Logger, Utils}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, ObjectOutputStream}
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.function.{Supplier, Function => JavaFunc}
import java.util.regex.Pattern
import scala.collection.mutable.ListBuffer

/**
 *
 * @author benjobs
 * @author zzz
 */

object FlinkShimsProxy extends Logger {

  private[this] val FLINK_PATTERN: Pattern = Pattern.compile(
    "flink-(.*).jar",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  )

  private[this] val SHIMS_PATTERN = Pattern.compile(
    "streamx-flink-shims_flink-(1.12|1.13|1.14)-(.*).jar",
    Pattern.CASE_INSENSITIVE | Pattern.DOTALL
  )

  private[this] val SHIMS_CLASS_LOADER_CACHE = new ConcurrentHashMap[String, ClassLoader]

  private[this] def getFlinkShimsResourcePattern(flinkLargeVersion: String) =
    Pattern.compile(
      "flink-(.*)-" + flinkLargeVersion + "(.*).jar",
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
    logInfo(s"flink version: $flinkVersion, shims version: $majorVersion")
    var classLoader = SHIMS_CLASS_LOADER_CACHE.get(majorVersion)

    SHIMS_CLASS_LOADER_CACHE.get(majorVersion) match {
      case null =>
        //1) flink/lib
        val libURL = getFlinkHomeLib(flinkVersion.flinkHome)
        val shimsUrls = ListBuffer[URL](libURL: _*)

        //2) shims jar
        val appHome = System.getProperty("app.home")
        require(appHome != null)

        val pluginsPath = new File(s"$appHome/plugins")
        require(pluginsPath.exists())

        shimsUrls += pluginsPath.listFiles()
          .find(_.getName.matches("streamx-flink-submit-core-(.*).jar"))
          .get.toURI.toURL

        val libPath = new File(s"$appHome/lib")
        require(libPath.exists())

        libPath.listFiles().foreach(jar => {
          try {
            val shimsMatcher = SHIMS_PATTERN.matcher(jar.getName)
            if (shimsMatcher.matches()) {
              if (majorVersion != null && majorVersion.equals(shimsMatcher.group(1))) {
                shimsUrls += jar.toURI.toURL
              }
            } else if (!FLINK_PATTERN.matcher(jar.getName).matches()) {
              shimsUrls += jar.toURI.toURL
            } else {
              logInfo(s"exclude ${jar.getName}")
            }
          } catch {
            case e: Exception => e.printStackTrace()
          }
        })
        val urls = shimsUrls.toArray
        classLoader = new ChildFirstClassLoader(urls, getFlinkShimsResourcePattern(majorVersion))
        SHIMS_CLASS_LOADER_CACHE.put(majorVersion, classLoader)
        classLoader
      case c => c
    }
  }

  private[this] def getFlinkHomeLib(flinkHome: String): List[URL] = {
    val file = new File(flinkHome, "lib")
    require(file.isDirectory, s"FLINK_HOME $file does not exist")
    file.listFiles.filter(!_.getName.startsWith("log4j")).map(_.toURI.toURL).toList
  }

  @throws[Exception]
  def getObject(loader: ClassLoader, obj: Object): Object = {
    val arrayOutputStream = new ByteArrayOutputStream
    Utils.tryWithResource(new ObjectOutputStream(arrayOutputStream))(objectOutputStream => {
      objectOutputStream.writeObject(obj)
      val byteArrayInputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray)
      Utils.tryWithResource(new ClassLoaderObjectInputStream(loader, byteArrayInputStream))(_.readObject)
    })
  }

}
