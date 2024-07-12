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

import java.io.File
import java.security.{AccessController, PrivilegedAction}
import java.util

import scala.util.{Failure, Success, Try}

object SystemPropertyUtils extends Logger {

  def getUserHome(): String = System.getProperty("user.home")

  /**
   * Returns {@code true} if and only if the system property with the specified {@code key} exists.
   */
  def contains(key: String): Boolean = get(key) != null

  /**
   * Returns the value of the Java system property with the specified {@code key}, while falling
   * back to {@code null} if the property access fails.
   *
   * @return
   *   the property value or { @code null}
   */
  def get(key: String): String = get(key, null)

  def get(key: String, default: String): String = {
    require(key != null, "[StreamPark] key must not be null.")
    key match {
      case empty if empty.isEmpty => throw new IllegalArgumentException("Key must not be empty.")
      case other =>
        Try {
          System.getSecurityManager match {
            case null => System.getProperty(other)
            case _ =>
              AccessController.doPrivileged(new PrivilegedAction[String]() {
                override def run: String = System.getProperty(other)
              })
          }
        } match {
          case Success(ok) =>
            ok match {
              case null => default
              case value => value
            }
          case Failure(e) =>
            logger.warn(
              s"Unable to retrieve a system property '$other'; default values will be used, ${e.getMessage}.")
            default
        }
    }
  }

  def getBoolean(key: String, default: Boolean): Boolean = {
    val value = get(key)
    value match {
      case null => default
      case "true" | "yes" | "1" => true
      case "false" | "no" | "0" => false
      case other: String if other.isEmpty => false
      case _ =>
        logger.warn(
          s"Unable to parse the boolean system property '$key':$value - using the default value: $default.")
        default
    }
  }

  def getInt(key: String, default: Int): Int = {
    Try(get(key).toInt) match {
      case Success(ok) => ok
      case Failure(_) => default
    }
  }

  def getLong(key: String, default: Long): Long = {
    Try(get(key).toLong) match {
      case Success(ok) => ok
      case Failure(_) => default
    }
  }

  /** Sets the value of the Java system property with the specified {@code key} */
  def set(key: String, value: String): String =
    System.getProperties.setProperty(key, value).asInstanceOf[String]

  @throws[Exception]
  def setEnv(name: String, value: String): Unit = {
    val envClass = Class.forName("java.lang.ProcessEnvironment")
    val getEnv = envClass.getDeclaredMethod("getenv")
    getEnv.setAccessible(true)
    val unmodifiableEnvironment = getEnv.invoke(null)
    val clazz = Class.forName("java.util.Collections$UnmodifiableMap")
    val field = clazz.getDeclaredField("m")
    field.setAccessible(true)
    field
      .get(unmodifiableEnvironment)
      .asInstanceOf[util.Map[String, String]]
      .put(name, value)
  }

  def getOrElseUpdate(key: String, default: String): String = {
    get(key) match {
      case null =>
        set(key, default)
        default
      case other => other
    }
  }

  def setAppHome(key: String, clazz: Class[_]): Unit = {
    if (get(key) == null) { // get the jar location or class location where the main class is located
      val jarOrClassPath = clazz.getProtectionDomain.getCodeSource.getLocation.getPath
      val file = new File(jarOrClassPath)
      // jar package run, locate app.home to the current jar location two levels above the directory
      val appHome: String = if (jarOrClassPath.endsWith("jar")) {
        file.getParentFile.getParentFile.getPath
      } else { // during the development phase, locate the app.home under target.
        file.getPath.replaceAll("classes/$", "")
      }
      SystemPropertyUtils.set(key, appHome)
    }
  }

  def getTmpdir(): String = get("java.io.tmpdir", "temp")

}
