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

import com.github.benmanes.caffeine.cache.{Cache, CacheLoader, Caffeine}
import com.streamxhub.streamx.common.conf.ConfigConst._
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.service.Service.STATE
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}
import org.apache.hadoop.yarn.util.RMHAUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.impl.client.HttpClients

import java.io.{File, IOException}
import java.net.InetAddress
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.{HashMap => JavaHashMap}
import scala.collection.JavaConversions._
import scala.util.control.Breaks._
import scala.util.{Failure, Success, Try}

/**
 * @author benjobs
 */
object HadoopUtils extends Logger {

  private[this] lazy val HADOOP_HOME: String = "HADOOP_HOME"
  private[this] lazy val HADOOP_CONF_DIR: String = "HADOOP_CONF_DIR"
  private[this] lazy val CONF_SUFFIX: String = "/etc/hadoop"

  private[this] var reusableYarnClient: YarnClient = _
  private[this] var reusableConf: Configuration = _

  private[util] var hdfs: FileSystem = getFileSystem(hadoopConf)

  private[this] var rmHttpURL: String = _

  private[this] lazy val configurationCache: util.Map[String, Configuration] = new ConcurrentHashMap[String, Configuration]()

  private[this] var caffeine: Cache[String, Configuration] = _

  lazy val kerberosConf: Map[String, String] = SystemPropertyUtils.get("app.home", null) match {
    case null =>
      getClass.getResourceAsStream("/kerberos.yml") match {
        case x if x != null => PropertiesUtils.fromYamlFile(x)
        case _ => null
      }
    case f =>
      val file = new File(s"$f/conf/kerberos.yml")
      if (file.exists() && file.isFile) {
        PropertiesUtils.fromYamlFile(file.getAbsolutePath)
      } else null
  }

  private[this] val kerberosEnable: Boolean = if (kerberosConf == null) false else {
    val enableString = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_ENABLE, "false")
    Try(enableString.trim.toBoolean).getOrElse(false)
  }

  private[this] lazy val hadoopConfDir: String = Try(FileUtils.getPathFromEnv(HADOOP_CONF_DIR)) match {
    case Failure(_) => FileUtils.resolvePath(FileUtils.getPathFromEnv(HADOOP_HOME), CONF_SUFFIX)
    case Success(value) => value
  }

  private[this] def getFileSystem(hadoopConf: Configuration): FileSystem = {
    Try(FileSystem.get(hadoopConf)) match {
      case Success(fs) => fs
      case Failure(e) =>
        new IllegalArgumentException(s"[StreamX] access hdfs error.$e")
        null
    }
  }


  def getConfigurationFromHadoopConfDir(confDir: String = hadoopConfDir): Configuration = {
    if (!configurationCache.containsKey(confDir)) {
      FileUtils.exists(confDir)
      val hadoopConfDir = new File(confDir)
      val confName = List("core-site.xml", "hdfs-site.xml", "yarn-site.xml","mapred-site.xml")
      val files = hadoopConfDir.listFiles().filter(x => x.isFile && confName.contains(x.getName)).toList
      val conf = new Configuration()
      if (CollectionUtils.isNotEmpty(files)) {
        files.foreach(x => conf.addResource(new Path(x.getAbsolutePath)))
      }
      configurationCache.put(confDir, conf)
    }
    configurationCache(confDir)
  }

  /**
   * <pre>
   * 注意:加载hadoop配置文件,有两种方式:<br>
   * 1) 将hadoop的core-site.xml,hdfs-site.xml,yarn-site.xml copy到 resources下<br>
   * 2) 程序自动去$HADOOP_HOME/etc/hadoop下加载配置<br>
   * 推荐第二种方法,不用copy配置文件.<br>
   * </pre>
   */
  def hadoopConf: Configuration = {
    def initHadoopConf(): Configuration = {
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
      conf
    }

    def kerberosLogin(conf: Configuration): Unit = {
      logInfo("kerberos login starting....")
      val principal = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_PRINCIPAL, "").trim
      val keytab = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_KEYTAB, "").trim
      require(
        principal.nonEmpty && keytab.nonEmpty,
        s"$KEY_SECURITY_KERBEROS_PRINCIPAL and $KEY_SECURITY_KERBEROS_KEYTAB must not be empty"
      )

      val krb5 = kerberosConf.getOrElse(
        KEY_SECURITY_KERBEROS_KRB5_CONF,
        kerberosConf.getOrElse(KEY_JAVA_SECURITY_KRB5_CONF, "")
      ).trim

      if (krb5.nonEmpty) {
        System.setProperty("java.security.krb5.conf", krb5)
        System.setProperty("java.security.krb5.conf.path", krb5)
      }
      System.setProperty("sun.security.spnego.debug", "true")
      System.setProperty("sun.security.krb5.debug", "true")

      conf.set(KEY_HADOOP_SECURITY_AUTHENTICATION, KEY_KERBEROS)
      try {
        UserGroupInformation.setConfiguration(conf)
        UserGroupInformation.loginUserFromKeytab(principal, keytab)
        logInfo("kerberos authentication successful")
      } catch {
        case e: IOException =>
          logError(s"kerberos login failed,${e.getLocalizedMessage}")
          throw e
      }
    }

    def refreshConf(): Configuration = {
      val conf = initHadoopConf()
      kerberosLogin(conf)
      if (reusableYarnClient != null) {
        reusableYarnClient.close()
        reusableYarnClient = null
      }
      hdfs = getFileSystem(conf)
      conf
    }

    if (kerberosEnable) {
      val expire = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_EXPIRE, "2").trim
      val (duration, unit) = DateUtils.getTimeUnit(expire)
      if (caffeine == null) {
        caffeine = Caffeine.newBuilder().refreshAfterWrite(duration.toLong, unit)
          .build[String, Configuration](new CacheLoader[String, Configuration]() {
            override def load(key: String): Configuration = {
              logInfo(s"recertification kerberos: time:${DateUtils.now(DateUtils.fullFormat)}, duration:$expire")
              refreshConf()
            }
          })
        caffeine.put("config", refreshConf())
      }
      caffeine.getIfPresent("config")
    } else {
      if (reusableConf == null) {
        reusableConf = initHadoopConf()
      }
      reusableConf
    }
  }

  def yarnClient: YarnClient = {
    if (reusableYarnClient == null || !reusableYarnClient.isInState(STATE.STARTED)) {
      reusableYarnClient = YarnClient.createYarnClient
      val yarnConf = new YarnConfiguration(hadoopConf)
      reusableYarnClient.init(yarnConf)
      reusableYarnClient.start()
    }
    reusableYarnClient
  }

  /**
   * <pre>
   *
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

          val conf = hadoopConf

          val useHttps = YarnConfiguration.useHttps(conf)
          val (addressPrefix, defaultPort, protocol) = useHttps match {
            case x if x => (YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS, "8090", "https://")
            case _ => (YarnConfiguration.RM_WEBAPP_ADDRESS, "8088", "http://")
          }

          val name = if (!HAUtil.isHAEnabled(conf)) addressPrefix else {
            val activeRMId = {
              val yarnConf = new YarnConfiguration(conf)
              Option(RMHAUtils.findActiveRMHAId(yarnConf)) match {
                case Some(x) =>
                  logInfo("findActiveRMHAId successful")
                  x
                case None =>
                  //If you don't know why, don't modify it
                  logWarn(s"findActiveRMHAId is null,config yarn.acl.enable:${yarnConf.get("yarn.acl.enable")},now http try it.")
                  // url ==> rmId
                  val idUrlMap = new JavaHashMap[String, String]
                  val rmIds = HAUtil.getRMHAIds(conf)
                  rmIds.foreach(id => {
                    val address = conf.get(HAUtil.addSuffix(addressPrefix, id)) match {
                      case null =>
                        val hostname = conf.get(HAUtil.addSuffix("yarn.resourcemanager.hostname", id))
                        s"$hostname:$defaultPort"
                      case x => x
                    }
                    idUrlMap.put(s"$protocol$address", id)
                  })
                  var rmId: String = null
                  val rpcTimeoutForChecks = yarnConf.getInt(
                    CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_KEY,
                    CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_DEFAULT
                  )
                  breakable(idUrlMap.foreach(x => {
                    //test yarn url
                    val activeUrl = httpTestYarnRMUrl(x._1, rpcTimeoutForChecks)
                    if (activeUrl != null) {
                      rmId = idUrlMap(activeUrl)
                      break
                    }
                  }))
                  rmId
              }
            }
            require(activeRMId != null, "[StreamX] can not found yarn active node")
            logInfo(s"current activeRMHAId: $activeRMId")
            HAUtil.addSuffix(addressPrefix, activeRMId)
          }

          val inetSocketAddress = conf.getSocketAddr(name, s"0.0.0.0:$defaultPort", defaultPort.toInt)

          val address = NetUtils.getConnectAddress(inetSocketAddress)

          val buffer = new StringBuilder(protocol)
          val resolved = address.getAddress
          if (resolved != null && !resolved.isAnyLocalAddress && !resolved.isLoopbackAddress) {
            buffer.append(address.getHostName)
          } else {
            Try(InetAddress.getLocalHost.getCanonicalHostName) match {
              case Success(value) => buffer.append(value)
              case _ => buffer.append(address.getHostName)
            }
          }

          rmHttpURL = buffer
            .append(":")
            .append(address.getPort)
            .toString()

          logInfo(s"yarn resourceManager webapp url:$rmHttpURL")
        }
      }
    }
    rmHttpURL
  }

  private[this] def httpTestYarnRMUrl(url: String, timeout: Int): String = {
    val httpClient = HttpClients.createDefault();
    val context = HttpClientContext.create()
    val httpGet = new HttpGet(url)
    val requestConfig = RequestConfig
      .custom()
      .setSocketTimeout(timeout)
      .setConnectTimeout(timeout)
      .build()
    httpGet.setConfig(requestConfig)
    Try(httpClient.execute(httpGet, context)) match {
      case Success(_) => context.getTargetHost.toString
      case _ => null
    }
  }

  def toApplicationId(appId: String): ApplicationId = {
    require(appId != null)
    val timestampAndId = appId.split("_")
    ApplicationId.newInstance(timestampAndId(1).toLong, timestampAndId.last.toInt)
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String = yarnClient.getApplicationReport(applicationId).getTrackingUrl

  @throws[IOException] def downloadJar(jarOnHdfs: String): String = {
    val tmpDir = FileUtils.createTempDir()
    val fs = FileSystem.get(new Configuration)
    val sourcePath = fs.makeQualified(new Path(jarOnHdfs))
    if (!fs.exists(sourcePath)) throw new IOException("jar file: " + jarOnHdfs + " doesn't exist.")
    val destPath = new Path(tmpDir.getAbsolutePath + "/" + sourcePath.getName)
    fs.copyToLocalFile(sourcePath, destPath)
    new File(destPath.toString).getAbsolutePath
  }
}
