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

package org.apache.streampark.spark.client.proxy

import org.apache.streampark.common.Constant
import org.apache.streampark.common.conf.{ConfigKeys, SparkVersion}
import org.apache.streampark.common.util.{ClassLoaderUtils, Logger}
import org.apache.streampark.common.util.ImplicitsUtils._

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, File, ObjectOutputStream}
import java.net.URL
import java.util.function.{Function => JavaFunc}
import java.util.regex.Pattern

import scala.collection.mutable.{ListBuffer, Map => MutableMap}

object SparkShimsProxy extends Logger {

  private[this] val SHIMS_CLASS_LOADER_CACHE = MutableMap[String, ClassLoader]()

  private[this] val VERIFY_SQL_CLASS_LOADER_CACHE =
    MutableMap[String, ClassLoader]()

  private[this] val INCLUDE_PATTERN: Pattern =
    Pattern.compile(
      "(streampark-shaded-jackson-)(.*).jar",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL)

  private[this] def getSparkShimsResourcePattern(sparkLargeVersion: String) =
    Pattern.compile(
      s"spark-(.*)-$sparkLargeVersion(.*).jar",
      Pattern.CASE_INSENSITIVE | Pattern.DOTALL)

  private[this] lazy val SPARK_SHIMS_PREFIX = "streampark-spark-shims_spark"

  def proxy[T](sparkVersion: SparkVersion, func: ClassLoader => T): T = {
    val shimsClassLoader = getSParkShimsClassLoader(sparkVersion)
    ClassLoaderUtils
      .runAsClassLoader[T](shimsClassLoader, () => func(shimsClassLoader))
  }

  def proxy[T](sparkVersion: SparkVersion, func: JavaFunc[ClassLoader, T]): T = {
    val shimsClassLoader = getSParkShimsClassLoader(sparkVersion)
    ClassLoaderUtils
      .runAsClassLoader[T](shimsClassLoader, () => func(shimsClassLoader))
  }

  // need to load all spark-table dependencies compatible with different versions
  def getVerifySqlLibClassLoader(sparkVersion: SparkVersion): ClassLoader = {
    logInfo(s"Add verify sql lib,spark version: $sparkVersion")
    VERIFY_SQL_CLASS_LOADER_CACHE.getOrElseUpdate(
      s"${sparkVersion.fullVersion}", {
        val getSparkTable: File => Boolean = _.getName.startsWith("spark-table")
        // 1) spark/lib/spark-table*
        val libTableURL =
          getSparkHomeLib(sparkVersion.sparkHome, "lib", getSparkTable)

        // 2) After version 1.15 need add spark/opt/spark-table*
        val optTableURL =
          getSparkHomeLib(sparkVersion.sparkHome, "opt", getSparkTable)
        val shimsUrls = ListBuffer[URL](libTableURL ++ optTableURL: _*)

        // 3) add only streampark shims jar
        addShimsUrls(
          sparkVersion,
          file => {
            if (file.getName.startsWith("streampark-spark-shims")) {
              shimsUrls += file.toURI.toURL
            }
          })

        new ChildFirstClassLoader(
          shimsUrls.toArray,
          Thread.currentThread().getContextClassLoader,
          getSparkShimsResourcePattern(sparkVersion.majorVersion))
      })
  }

  def addShimsUrls(sparkVersion: SparkVersion, addShimUrl: File => Unit): Unit = {
    val appHome = System.getProperty(ConfigKeys.KEY_APP_HOME)
    require(
      appHome != null,
      String.format("%s is not found on System env.", ConfigKeys.KEY_APP_HOME))

    val libPath = new File(s"$appHome/lib")
    require(libPath.exists())

    val majorVersion = sparkVersion.majorVersion
    val scalaVersion = sparkVersion.scalaVersion

    libPath
      .listFiles()
      .foreach((jar: File) => {
        val jarName = jar.getName
        if (jarName.endsWith(Constant.JAR_SUFFIX)) {
          if (jarName.startsWith(SPARK_SHIMS_PREFIX)) {
            val prefixVer =
              s"$SPARK_SHIMS_PREFIX-${majorVersion}_$scalaVersion"
            if (jarName.startsWith(prefixVer)) {
              addShimUrl(jar)
              logInfo(s"Include spark shims jar lib: $jarName")
            }
          } else {
            if (INCLUDE_PATTERN.matcher(jarName).matches()) {
              addShimUrl(jar)
              logInfo(s"Include jar lib: $jarName")
            } else if (jarName.matches(s"^streampark-.*_$scalaVersion.*$$")) {
              addShimUrl(jar)
              logInfo(s"Include streampark lib: $jarName")
            }
          }
        }
      })
  }

  def proxyVerifySql[T](sparkVersion: SparkVersion, func: JavaFunc[ClassLoader, T]): T = {
    val shimsClassLoader = getVerifySqlLibClassLoader(sparkVersion)
    ClassLoaderUtils
      .runAsClassLoader[T](shimsClassLoader, () => func(shimsClassLoader))
  }

  private[this] def getSParkShimsClassLoader(sparkVersion: SparkVersion): ClassLoader = {
    logInfo(s"add spark shims urls classloader,spark version: $sparkVersion")

    SHIMS_CLASS_LOADER_CACHE.getOrElseUpdate(
      s"${sparkVersion.fullVersion}", {
        // 1) spark/lib
        val libURL = getSparkHomeLib(sparkVersion.sparkHome, "jars", !_.getName.startsWith("log4j"))
        val shimsUrls = ListBuffer[URL](libURL: _*)

        // 2) add all shims jar
        addShimsUrls(
          sparkVersion,
          file => {
            if (file != null) {
              shimsUrls += file.toURI.toURL
            }
          })

        new ChildFirstClassLoader(
          shimsUrls.toArray,
          Thread.currentThread().getContextClassLoader,
          getSparkShimsResourcePattern(sparkVersion.majorVersion))
      })
  }

  private[this] def getSparkHomeLib(
      sparkHome: String,
      childDir: String,
      filterFun: File => Boolean): List[URL] = {
    val file = new File(sparkHome, childDir)
    require(file.isDirectory, s"SPARK_HOME $file does not exist")
    file.listFiles.filter(filterFun).map(_.toURI.toURL).toList
  }

  @throws[Exception]
  def getObject[T](loader: ClassLoader, obj: Object): T = {
    val arrayOutputStream = new ByteArrayOutputStream
    new ObjectOutputStream(arrayOutputStream)
      .autoClose(objectOutputStream => {
        objectOutputStream.writeObject(obj)
        val byteArrayInputStream =
          new ByteArrayInputStream(arrayOutputStream.toByteArray)
        new ClassLoaderObjectInputStream(loader, byteArrayInputStream)
          .autoClose(_.readObject())
      })
      .asInstanceOf[T]
  }

}
