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

import java.io.{InputStream, IOException, ObjectInputStream, ObjectStreamClass}
import java.lang.reflect.Proxy

import scala.util.Try

/**
 * A special ObjectInputStream that loads a class based on a specified <code>ClassLoader</code>
 * rather than the system default. <p> This is useful in dynamic container environments.
 *
 * @since 1.1
 */
class ClassLoaderObjectInputStream(classLoader: ClassLoader, inputStream: InputStream)
  extends ObjectInputStream(inputStream) {

  /**
   * Resolve a class specified by the descriptor using the specified ClassLoader or the super
   * ClassLoader.
   *
   * @param objectStreamClass
   *   descriptor of the class
   * @return
   *   the Class object described by the ObjectStreamClass
   * @throws IOException
   *   in case of an I/O error
   * @throws ClassNotFoundException
   *   if the Class cannot be found
   */
  @throws[IOException]
  @throws[ClassNotFoundException]
  override protected def resolveClass(objectStreamClass: ObjectStreamClass): Class[_] = {
    // delegate to super class loader which can resolve primitives
    Try(Class.forName(objectStreamClass.getName, false, classLoader))
      .getOrElse(super.resolveClass(objectStreamClass))
  }

  /**
   * Create a proxy class that implements the specified interfaces using the specified ClassLoader
   * or the super ClassLoader.
   *
   * @param interfaces
   *   the interfaces to implement
   * @return
   *   a proxy class implementing the interfaces
   * @throws IOException
   *   in case of an I/O error
   * @throws ClassNotFoundException
   *   if the Class cannot be found
   * @see
   *   ObjectInputStream#resolveProxyClass(String[])
   * @since 2.1
   */
  @throws[IOException]
  @throws[ClassNotFoundException]
  override protected def resolveProxyClass(interfaces: Array[String]): Class[_] = {
    val interfaceClasses = new Array[Class[_]](interfaces.length)
    for (i <- interfaces.indices) {
      interfaceClasses(i) = Class.forName(interfaces(i), false, classLoader)
    }
    Try(Proxy.getProxyClass(classLoader, interfaceClasses: _*))
      .getOrElse(super.resolveProxyClass(interfaces))
  }

}
