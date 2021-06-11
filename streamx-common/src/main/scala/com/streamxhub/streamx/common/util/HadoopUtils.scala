/*
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

package com.streamxhub.streamx.common.util

import com.google.common.io.Files
import com.streamxhub.streamx.common.conf.ConfigConst
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FileSystem, LocalFileSystem, Path}
import org.apache.hadoop.hdfs.DFSConfigKeys._
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.service.Service.STATE
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}
import org.apache.hadoop.yarn.util.RMHAUtils

import java.io.{File, IOException}
import java.net.InetAddress
import java.util
import java.util.concurrent.ConcurrentHashMap
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

/**
 * @author benjobs
 */
object HadoopUtils extends Logger {

  private[this] val HADOOP_HOME: String = "HADOOP_HOME"
  private[this] val HADOOP_CONF_DIR: String = "HADOOP_CONF_DIR"
  private[this] val CONF_SUFFIX: String = "/etc/hadoop"

  private[this] var innerYarnClient: YarnClient = _
  private[this] var rmHttpURL: String = _

  private[this] val configurationCache: util.Map[String, Configuration] = new ConcurrentHashMap[String, Configuration]

  lazy val kerberosConf: Map[String, String] = {
    SystemPropertyUtils.get("app.home", null) match {
      case null =>
        val inputStream = getClass.getResourceAsStream("kerberos.yml")
        if (inputStream == null) null else {
          PropertiesUtils.fromYamlFile(inputStream)
        }
      case f =>
        val file = new File(s"$f/conf/kerberos.yml")
        if (file.exists() && file.isFile) {
          PropertiesUtils.fromYamlFile(file.getAbsolutePath)
        } else null
    }
  }

  private[this] val kerberosEnable: Boolean = if (kerberosConf == null) false else {
    val enableString = kerberosConf.getOrElse(ConfigConst.KEY_SECURITY_KERBEROS_ENABLE, "false")
    Try(enableString.trim.toBoolean).getOrElse(false)
  }


  private[this] lazy val hadoopConfDir: String = Try(FileUtils.getPathFromEnv(HADOOP_CONF_DIR)) match {
    case Failure(_) => FileUtils.resolvePath(FileUtils.getPathFromEnv(HADOOP_HOME), CONF_SUFFIX)
    case Success(value) => value
  }

  def getConfigurationFromHadoopConfDir(confDir: String = hadoopConfDir): Configuration = {
    if (!configurationCache.containsKey(confDir)) {
      FileUtils.exists(confDir)
      val hadoopConfDir = new File(confDir)
      val files = hadoopConfDir.listFiles().filter(x => x.isFile && x.getName.endsWith(".xml")).toList
      val conf = new Configuration()
      if (CollectionUtils.isNotEmpty(files)) {
        conf.clear()
        files.foreach(x => conf.addResource(new Path(x.getAbsolutePath)))
      }
      configurationCache.put(confDir, conf)
    }
    configurationCache.get(confDir)
  }

  /**
   * <pre>
   * 注意:加载hadoop配置文件,有两种方式:<br>
   * 1) 将hadoop的core-site.xml,hdfs-site.xml,yarn-site.xml copy到 resources下<br>
   * 2) 程序自动去$HADOOP_HOME/etc/hadoop下加载配置<br>
   * 推荐第二种方法,不用copy配置文件.<br>
   * </pre>
   */
  lazy val conf: Configuration = {
    val conf = getConfigurationFromHadoopConfDir(hadoopConfDir)
    //add hadoopConfDir to classpath...you know why???
    ClassLoaderUtils.loadResource(hadoopConfDir)

    if (StringUtils.isBlank(conf.get("hadoop.tmp.dir"))) {
      conf.set("hadoop.tmp.dir", "/tmp")
    }
    if (StringUtils.isBlank(conf.get("hbase.fs.tmp.dir"))) {
      conf.set("hbase.fs.tmp.dir", "/tmp")
    }
    // disable timeline service as we only query yarn app here.
    // Otherwise we may hit this kind of ERROR:
    // java.lang.ClassNotFoundException: com.sun.jersey.api.client.config.ClientConfig
    conf.set("yarn.timeline-service.enabled", "false")
    conf.set("fs.hdfs.impl", classOf[DistributedFileSystem].getName)
    conf.set("fs.file.impl", classOf[LocalFileSystem].getName)
    conf.set("fs.hdfs.impl.disable.cache", "true")
    kerberosLogin(conf)
    conf
  }

  private[this] def kerberosLogin(conf: Configuration): Unit = if (kerberosEnable) {
    val principal = kerberosConf.getOrElse(ConfigConst.KEY_SECURITY_KERBEROS_PRINCIPAL, "").trim
    val keytab = kerberosConf.getOrElse(ConfigConst.KEY_SECURITY_KERBEROS_KEYTAB, "").trim
    require(
      principal.nonEmpty && keytab.nonEmpty,
      s"${ConfigConst.KEY_SECURITY_KERBEROS_PRINCIPAL} and ${ConfigConst.KEY_SECURITY_KERBEROS_KEYTAB} must be not empty"
    )

    conf.set(DFS_NAMENODE_KERBEROS_PRINCIPAL_KEY, principal)
    conf.set(DFS_DATANODE_KERBEROS_PRINCIPAL_KEY, principal)
    conf.set(DFS_WEB_AUTHENTICATION_KERBEROS_PRINCIPAL_KEY, principal)
    conf.set(DFS_NAMENODE_KEYTAB_FILE_KEY, keytab)
    conf.set(DFS_DATANODE_KEYTAB_FILE_KEY, keytab)
    conf.set(DFS_WEB_AUTHENTICATION_KERBEROS_KEYTAB_KEY, keytab)

    conf.set("mapreduce.jobhistory.principal", principal)
    conf.set("yarn.resourcemanager.principal", principal)
    conf.set("yarn.nodemanager.principal", principal)
    conf.set("yarn.resourcemanager.keytab", keytab)
    conf.set("yarn.nodemanager.keytab", keytab)
    conf.set("mapreduce.jobhistory.keytab", keytab)

    conf.set(ConfigConst.KEY_HADOOP_SECURITY_AUTHENTICATION, ConfigConst.KEY_KERBEROS)
    try {
      UserGroupInformation.setConfiguration(conf)
      UserGroupInformation.loginUserFromKeytab(principal, keytab)
      logInfo("kerberos authentication successful")
    } catch {
      case e: IOException => throw e
    }
  }

  def yarnClient: YarnClient = {
    if (innerYarnClient == null || !innerYarnClient.isInState(STATE.STARTED)) {
      innerYarnClient = YarnClient.createYarnClient
      val yarnConf = new YarnConfiguration(conf)
      kerberosLogin(yarnConf)
      innerYarnClient.init(yarnConf)
      innerYarnClient.start()
    }
    innerYarnClient
  }

  /**
   * <pre>
   * @param getLatest :
   *                  默认单例模式,如果getLatest=true则再次寻找活跃节点返回,主要是考虑到主备的情况,
   *                  如: 第一次获取的时候返回的是一个当前的活跃节点,之后可能这个活跃节点挂了,就不能提供服务了,
   *                  此时在调用该方法,只需要传入true即可再次获取一个最新的活跃节点返回
   * @return
   * </pre>
   */
  def getRMWebAppURL(getLatest: Boolean = false): String = {
    if (rmHttpURL == null || getLatest) {
      synchronized {
        if (rmHttpURL == null || getLatest) {

          val useHttps = YarnConfiguration.useHttps(conf)

          val addressPrefix = useHttps match {
            case x if x => YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS
            case _ => YarnConfiguration.RM_WEBAPP_ADDRESS
          }

          val name = if (!HAUtil.isHAEnabled(conf)) addressPrefix else {
            val yarnConf = new YarnConfiguration(conf)
            if (conf.get("yarn.resourcemanager.principal") != null) {
              yarnConf.set("yarn.resourcemanager.principal", conf.get("yarn.resourcemanager.principal"))
            }
            val activeRMId = RMHAUtils.findActiveRMHAId(yarnConf)
            require(activeRMId != null, "[StreamX] can not found yarn active node")
            logInfo(s"current activeRMHAId: $activeRMId")
            HAUtil.addSuffix(addressPrefix, activeRMId)
          }

          val inetSocketAddress = useHttps match {
            case x if x => conf.getSocketAddr(name, "0.0.0.0:8090", 8090)
            case _ => conf.getSocketAddr(name, "0.0.0.0:8088", 8088)
          }

          val address = NetUtils.getConnectAddress(inetSocketAddress)

          val buffer = new StringBuilder(if (useHttps) "https://" else "http://")
          val resolved = address.getAddress
          if (resolved != null && !resolved.isAnyLocalAddress && !resolved.isLoopbackAddress) {
            buffer.append(address.getHostName)
          } else {
            Try(InetAddress.getLocalHost.getCanonicalHostName) match {
              case Success(value) => buffer.append(value)
              case _ => buffer.append(address.getHostName)
            }
          }
          buffer.append(":").append(address.getPort)
          rmHttpURL = buffer.toString
        }
      }
    }
    rmHttpURL
  }

  def toApplicationId(appId: String): ApplicationId = {
    require(appId != null)
    val timestampAndId = appId.split("_")
    ApplicationId.newInstance(timestampAndId(1).toLong, timestampAndId.last.toInt)
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String = yarnClient.getApplicationReport(applicationId).getTrackingUrl

  @throws[IOException] def downloadJar(jarOnHdfs: String): String = {
    val tmpDir = Files.createTempDir
    val fs = FileSystem.get(new Configuration)
    val sourcePath = fs.makeQualified(new Path(jarOnHdfs))
    if (!fs.exists(sourcePath)) throw new IOException("jar file: " + jarOnHdfs + " doesn't exist.")
    val destPath = new Path(tmpDir.getAbsolutePath + "/" + sourcePath.getName)
    fs.copyToLocalFile(sourcePath, destPath)
    new File(destPath.toString).getAbsolutePath
  }

}
