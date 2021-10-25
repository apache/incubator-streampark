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
package com.streamxhub.streamx.flink.repl.shims

import com.streamxhub.streamx.common.conf.Workspace
import com.streamxhub.streamx.common.util.{BottomUpClassLoader, ClassLoaderUtils, Logger}

import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Supplier
import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

/**
 * author: Al-assad
 */
object FlinkShimsClassLoader extends Logger {

  // Cache for flink-shims ClassLoader
  val classLoaderPool = new ConcurrentHashMap[ReplFlinkVersion, ClassLoader]

  /**
   * Run the current thread with the class loader corresponding to the specified flink version.
   * For Scala calls.
   */
  def runAsSpecVersion[R](flinkVersion: ReplFlinkVersion, func: () => R): R = runAsSpecVersion(flinkVersion, fromCache = true, func)

  def runAsSpecVersion[R](flinkVersion: ReplFlinkVersion, fromCache: Boolean = true, func: () => R): R = {
    val clzLoader = choiceFlinkShimsClassLoader(flinkVersion, fromCache)
    ClassLoaderUtils.runAsClassLoader(clzLoader, func)
  }

  /**
   * Run the current thread with the class loader corresponding to the specified flink version.
   * For Java calls.
   */
  def runAsSpecVersion[R](flinkVersion: ReplFlinkVersion, supplier: Supplier[R]): R = runAsSpecVersion(flinkVersion, fromCache = true, supplier)

  def runAsSpecVersion[R](flinkVersion: ReplFlinkVersion, fromCache: Boolean, supplier: Supplier[R]): R = {
    val clzLoader = choiceFlinkShimsClassLoader(flinkVersion, fromCache)
    ClassLoaderUtils.runAsClassLoader(clzLoader, supplier)
  }

  /**
   * Choice the classloader corresponding to the specified flink version.
   */
  def choiceFlinkShimsClassLoader(flinkVersion: ReplFlinkVersion, fromCache: Boolean = true): ClassLoader = {
    if (fromCache && classLoaderPool.containsKey(flinkVersion)) {
      classLoaderPool.get(flinkVersion)
    } else {
      val loadedClasspath = retrieveExtractClasspath(flinkVersion)
      val classLoader = new BottomUpClassLoader(loadedClasspath, getClass.getClassLoader)
      //      val classLoader = new ParentLastURLClassLoader(retrieveExtractClasspath(flinkVersion))
      classLoaderPool.put(flinkVersion, classLoader)
      classLoader
    }
  }

  def retrieveExtractClasspath(flinkVersion: ReplFlinkVersion): ArrayBuffer[URL] = {
    val extraClzs = ArrayBuffer[URL]()
    // load flink shims jar.
    extraClzs ++= {
      val shimJars = new File(s"${Workspace.local.APP_SHIMS}/flink-${flinkVersion.majorVersion}")
        .listFiles
        .filter(_.isFile)
        .filter(_.getName.endsWith(".jar"))
        .filter(_.getName.startsWith(flinkVersion.shimsVersion))
      logInfo(s"[flink-repl] flink-version: ${flinkVersion.version}, load shims jar: ${shimJars.map(_.getAbsoluteFile).mkString(",")}")
      shimJars.map(_.toURI.toURL)
    }
    // load extract flink jars from flink home.
    extraClzs ++= {
      val flinkJars = new File(s"${flinkVersion.flinkHome}/lib")
        .listFiles
        .filter(_.isFile)
        .filter(_.getName.endsWith(".jar"))
        .filter(!_.getName.startsWith("log4j"))
      logInfo(s"[flink-repl] flink-version: ${flinkVersion.version}, load flink jars: ${flinkJars.map(_.getAbsoluteFile).mkString(",")}")
      flinkJars.map(_.toURI.toURL)
    }
  }


}
