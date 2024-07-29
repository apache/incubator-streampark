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

import org.apache.streampark.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.streampark.common.conf.ConfigKeys._
import org.apache.streampark.common.fs.LfsOperator
import org.apache.streampark.common.util.Implicits._

import org.apache.commons.io.{FileUtils => ApacheFileUtils}

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.{Collections, Optional}

import scala.collection.immutable.ListMap
import scala.util.{Failure, Success, Try}

/** Hadoop client configuration tools mainly for flink use. */
object HadoopConfigUtils {

  private[this] lazy val kerberosConf: Map[String, String] =
    System.getProperties.filter(_._1.startsWith("security.kerberos")).toMap

  lazy val hadoopUserName: String =
    InternalConfigHolder.get(CommonConfig.STREAMPARK_HADOOP_USER_NAME)

  lazy val kerberosDebug =
    kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_DEBUG, "false")

  lazy val kerberosEnable =
    kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_ENABLE, "false").toBoolean

  lazy val kerberosPrincipal =
    kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_PRINCIPAL, "").trim

  val kerberosKeytab =
    kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_KEYTAB, "").trim

  val kerberosKrb5 = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_KRB5_CONF, "")

  private val HADOOP_CLIENT_CONF_FILES: Array[String] =
    Array("core-site.xml", "hdfs-site.xml", "yarn-site.xml")

  private val HIVE_CLIENT_CONF_FILES: Array[String] =
    Array("core-site.xml", "hdfs-site.xml", "hive-site.xml")

  /** Get Hadoop configuration directory path from system. */
  def getSystemHadoopConfDir: Option[String] = {
    Try(FileUtils.getPathFromEnv("HADOOP_CONF_DIR")) match {
      case Success(p) => Some(p)
      case Failure(_) =>
        val p = FileUtils.resolvePath(FileUtils.getPathFromEnv("HADOOP_HOME"), "/etc/hadoop")
        Some(p)
    }
  }

  def getSystemHadoopConfDirAsJava: Optional[String] =
    Optional.ofNullable(getSystemHadoopConfDir.orNull)

  /** Get Hive configuration directory path from system. */
  def getSystemHiveConfDir: Option[String] = Try(FileUtils.getPathFromEnv("HIVE_CONF_DIR")).toOption

  def getSystemHiveConfDirAsJava: Optional[String] =
    Optional.ofNullable(getSystemHiveConfDir.orNull)

  /**
   * Replace host information with ip of hadoop config file or hive config file. Such as
   * core-site.xml, hdfs-site.xml, hive-site.xml.
   */
  def replaceHostWithIP(configFile: File): Unit = {
    if (configFile.exists && configFile.isFile && configFile.getName.endsWith(".xml")) {
      // get hosts from system
      val hostsMap = HostsUtils.getSortSystemHosts
      if (hostsMap.nonEmpty) {
        rewriteHostIpMapper(configFile, hostsMap)
      }
    }
  }

  /** Replace host information with ip of configuration files under hadoop/hive config dir. */
  def batchReplaceHostWithIP(
      configDir: File,
      filter: Array[String] = HADOOP_CLIENT_CONF_FILES): Unit = {
    if (!configDir.isDirectory) {
      replaceHostWithIP(configDir)
      return
    }
    val hostsMap = HostsUtils.getSortSystemHosts
    if (hostsMap.isEmpty) {
      return
    }
    configDir.listFiles
      .filter(_.isFile)
      .filter(e => filter.contains(e.getName))
      .foreach(rewriteHostIpMapper(_, hostsMap))
  }

  private[this] def rewriteHostIpMapper(
      configFile: File,
      hostsMap: ListMap[String, String]): Unit = {
    // replace the host information in the configuration content
    val lines = ApacheFileUtils.readLines(configFile).map {
      case line if !line.trim.startsWith("<value>") => line
      case line =>
        var shot = hostsMap.find(e => line.contains(e._1))
        var li = line
        while (shot.nonEmpty) {
          li = li.replace(shot.get._1, shot.get._2)
          shot = hostsMap.find(e => li.contains(e._1))
        }
        li
    }
    // write content to original file
    ApacheFileUtils.writeLines(configFile, lines)
  }

  /** Read system hadoop config to Map */
  def readSystemHadoopConf: JavaMap[String, String] =
    getSystemHadoopConfDir
      .map(confDir =>
        LfsOperator
          .listDir(confDir)
          .filter(f => HADOOP_CLIENT_CONF_FILES.contains(f.getName))
          .map(f =>
            f.getName -> ApacheFileUtils
              .readFileToString(f, StandardCharsets.UTF_8))
          .toMap
          .asJava)
      .getOrElse(Collections.emptyMap[String, String]())

  /** Read system hive config to Map */
  def readSystemHiveConf: JavaMap[String, String] = {
    getSystemHiveConfDir
      .map(confDir =>
        LfsOperator
          .listDir(confDir)
          .filter(f => HIVE_CLIENT_CONF_FILES.contains(f.getName))
          .map(f =>
            f.getName -> ApacheFileUtils
              .readFileToString(f, StandardCharsets.UTF_8))
          .toMap
          .asJava)
      .getOrElse(Collections.emptyMap[String, String]())

  }

}
