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

import java.io.{File, IOException}
import java.net.URL
import java.util.{Properties, UUID}
import java.util.jar.JarFile
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.util.function.BiConsumer
import java.util.jar.JarInputStream
import scala.util.{Failure, Success, Try}

object Utils {

  def uuid(): String = UUID.randomUUID().toString.replaceAll("-", "")

  @throws[IOException] def checkJarFile(jar: URL): Unit = {
    val jarFile: File = Try(new File(jar.toURI)) match {
      case Success(x) => x
      case Failure(_) => throw new IOException(s"JAR file path is invalid $jar")
    }
    if (!jarFile.exists) {
      throw new IOException(s"JAR file does not exist '${jarFile.getAbsolutePath}'")
    }
    if (!jarFile.canRead) {
      throw new IOException(s"JAR file can't be read '${jarFile.getAbsolutePath}'")
    }
    Try(new JarFile(jarFile)) match {
      case Failure(e) => throw new IOException(s"Error while opening jar file '${jarFile.getAbsolutePath}'", e)
      case Success(x) => x.close()
    }
  }

  def getJarManifest(jarFile: File) = {
    checkJarFile(jarFile.toURL)
    new JarInputStream(new BufferedInputStream(new FileInputStream(jarFile))).getManifest
  }

  def copyProperties(original: Properties, target: Properties): Unit = {
    original.forEach(new BiConsumer[Object, Object] {
      override def accept(k: Object, v: Object): Unit = target.put(k, v)
    })
  }

  def main(args: Array[String]): Unit = {
    val jar = "/Users/benjobs/Workspace/deploy/workspace/app/1/flink-quickstart-1.0/flink-quickstart-1.0.jar"
    val manifest = getJarManifest(new File(jar))
    println(manifest.getMainAttributes.getValue("Main-Class"))
  }

}
