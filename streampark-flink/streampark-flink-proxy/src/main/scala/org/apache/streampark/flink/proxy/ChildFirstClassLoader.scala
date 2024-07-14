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

package org.apache.streampark.flink.proxy

import java.io.{File, IOException}
import java.net.{URL, URLClassLoader}
import java.util
import java.util.function.Consumer
import java.util.regex.Pattern

import scala.util.Try

/**
 * A variant of the URLClassLoader that first loads from the URLs and only after that from the
 * parent.
 *
 * <p>{@link # getResourceAsStream ( String )} uses {@link # getResource ( String )} internally so
 * we don't override that.
 */

class ChildFirstClassLoader(
    urls: Array[URL],
    parent: ClassLoader,
    flinkResourcePattern: Pattern,
    classLoadingExceptionHandler: Consumer[Throwable])
  extends URLClassLoader(urls, parent) {

  ClassLoader.registerAsParallelCapable()

  def this(urls: Array[URL], parent: ClassLoader, flinkResourcePattern: Pattern) {
    this(
      urls,
      parent,
      flinkResourcePattern,
      new Consumer[Throwable] {
        override def accept(t: Throwable): Unit = { throw t }
      })
  }

  private val FLINK_PATTERN =
    Pattern.compile("flink-(.*).jar", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)

  private val JAR_PROTOCOL = "jar"

  private val PARENT_FIRST_PATTERNS = List(
    "java.",
    "javax.xml",
    "org.slf4j",
    "org.apache.log4j",
    "org.apache.logging",
    "org.apache.commons.logging",
    "org.apache.commons.cli",
    "ch.qos.logback",
    "org.xml",
    "org.w3c",
    "org.apache.hadoop"
  )

  @throws[ClassNotFoundException]
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    try {
      this.synchronized {
        super.findLoadedClass(name) match {
          case null =>
            // check whether the class should go parent-first
            PARENT_FIRST_PATTERNS.find(name.startsWith) match {
              case Some(_) => super.loadClass(name, resolve)
              case _ => Try(findClass(name)).getOrElse(super.loadClass(name, resolve))
            }
          case c =>
            if (resolve) {
              resolveClass(c)
            }
            c
        }
      }
    } catch {
      case e: Throwable =>
        classLoadingExceptionHandler.accept(e)
        null
    }
  }

  override def getResource(name: String): URL = {
    // first, try and find it via the URLClassloader
    val urlClassLoaderResource = findResource(name)
    if (urlClassLoaderResource != null) return urlClassLoaderResource
    // delegate to super
    super.getResource(name)
  }

  /**
   * e.g. flinkResourcePattern: flink-1.12 <p> flink-1.12.jar/resource flink-1.14.jar/resource
   * other.jar/resource \=> after filterFlinkShimsResource \=> flink-1.12.jar/resource
   * other.jar/resource
   *
   * @param urlClassLoaderResource
   * @return
   */
  private def filterFlinkShimsResource(urlClassLoaderResource: URL): URL = {
    if (urlClassLoaderResource != null && JAR_PROTOCOL == urlClassLoaderResource.getProtocol) {
      val spec = urlClassLoaderResource.getFile
      val filename = new File(spec.substring(0, spec.indexOf("!/"))).getName
      if (
        FLINK_PATTERN.matcher(filename).matches && !flinkResourcePattern.matcher(filename).matches
      ) {
        return null
      }
    }
    urlClassLoaderResource
  }

  private def addResources(result: util.List[URL], resources: util.Enumeration[URL]) = {
    while (resources.hasMoreElements) {
      val urlClassLoaderResource = filterFlinkShimsResource(resources.nextElement)
      if (urlClassLoaderResource != null) {
        result.add(urlClassLoaderResource)
      }
    }
    result
  }

  @throws[IOException]
  override def getResources(name: String): util.Enumeration[URL] = {
    // first get resources from URLClassloader
    val result = addResources(new util.ArrayList[URL], findResources(name))
    val parent = getParent
    if (parent != null) {
      // get parent urls
      addResources(result, parent.getResources(name))
    }
    new util.Enumeration[URL]() {
      final private[proxy] val iter = result.iterator

      override def hasMoreElements: Boolean = iter.hasNext

      override def nextElement: URL = iter.next
    }
  }

}
