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

package org.apache.streampark.common.util

import java.io.{File, IOException}
import java.net.{URL, URLClassLoader}
import java.util

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
    parentFirstClasses: List[String],
    loadJarFilter: String => Boolean)
  extends URLClassLoader(urls, parent) {

  ClassLoader.registerAsParallelCapable()

  @throws[ClassNotFoundException]
  override def loadClass(name: String, resolve: Boolean): Class[_] = {
    try {
      this.synchronized {
        // First, check if the class has already been loaded
        super.findLoadedClass(name) match {
          case null =>
            // check whether the class should go parent-first
            parentFirstClasses.find(name.startsWith) match {
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
      case e: Throwable => throw e
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
   * @param urlClassLoaderResource
   * @return
   */
  private def filterResource(urlClassLoaderResource: URL): URL = {
    if (urlClassLoaderResource != null && "jar" == urlClassLoaderResource.getProtocol) {
      val spec = urlClassLoaderResource.getFile
      val jarName = new File(spec.substring(0, spec.indexOf("!/"))).getName
      if (loadJarFilter(jarName)) {
        return null
      }
    }
    urlClassLoaderResource
  }

  private def addResources(result: util.List[URL], resources: util.Enumeration[URL]) = {
    while (resources.hasMoreElements) {
      val urlClassLoaderResource = filterResource(resources.nextElement)
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
      private[this] val iter = result.iterator

      override def hasMoreElements: Boolean = iter.hasNext

      override def nextElement: URL = iter.next
    }
  }

}
