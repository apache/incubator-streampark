/**
 * Copyright (c) 2019 The StreamX Project
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
package com.streamxhub.common.util

import java.io.File
import java.net.URL
import java.lang.reflect.Method
import java.net.URLClassLoader

object ClassLoaderUtils {

  private val classloader = ClassLoader.getSystemClassLoader.asInstanceOf[URLClassLoader]

  def loadJar(jarFilePath: String): Unit = {
    val jarFile = new File(jarFilePath)
    require(jarFile.exists, s"[StreamX] jarFilePath:$jarFilePath is not exists")
    require(jarFile.isFile, s"[StreamX] jarFilePath:$jarFilePath is not file")
    loadPath(jarFile.getAbsolutePath)
  }

  def loadJars(path: String): Unit = {
    val jarDir = new File(path)
    require(jarDir.exists, s"[StreamX] jarPath: $path is not exists")
    require(jarDir.isFile, s"[StreamX] jarPath: $path is not directory")
    require(jarDir.listFiles.length > 0, s"[StreamX] have not jar in path:$path")
    for (jarFile <- jarDir.listFiles) {
      loadPath(jarFile.getAbsolutePath)
    }
  }

  /**
   * URLClassLoader的addURL方法
   */
  private val addURL: Method = try {
    val add = classOf[URLClassLoader].getDeclaredMethod("addURL", Array(classOf[URL]): _*)
    add.setAccessible(true)
    add
  } catch {
    case e: Exception => throw new RuntimeException(e)
  }

  private def loadPath(filepath: String): Unit = {
    val file = new File(filepath)
    loopFiles(file)
  }

  private def loadResourceDir(filepath: String): Unit = {
    val file = new File(filepath)
    loopDirs(file)
  }

  private def loopDirs(file: File): Unit = { // 资源文件只加载路径
    if (file.isDirectory) {
      addURL(file)
      file.listFiles.foreach(x => loopDirs(x))
    }
  }


  private def loopFiles(file: File): Unit = {
    if (file.isDirectory) {
      file.listFiles.foreach(x => loopFiles(x))
    } else if (file.getAbsolutePath.endsWith(".jar") || file.getAbsolutePath.endsWith(".zip")) {
      addURL(file)
    }
  }

  private def addURL(file: File): Unit = {
    try {
      addURL.invoke(classloader, Array[AnyRef](file.toURI.toURL))
    } catch {
      case e: Exception =>
    }
  }


}
