/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.common.util

import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
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
import java.security.PrivilegedAction
import java.util
import java.util.concurrent.ConcurrentHashMap
import java.util.{Timer, TimerTask, HashMap => JavaHashMap}
import javax.security.auth.kerberos.KerberosTicket
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

  private[this] var ugi: UserGroupInformation = _

  private[this] var reusableConf: Configuration = _

  private[this] var reusableHdfs: FileSystem = _

  private[this] var tgt: KerberosTicket = _

  private[this] var rmHttpURL: String = _

  private lazy val hadoopUserName: String = InternalConfigHolder.get(CommonConfig.STREAMX_HADOOP_USER_NAME)

  private[this] lazy val configurationCache: util.Map[String, Configuration] = new ConcurrentHashMap[String, Configuration]()

  private[this] lazy val kerberosConf: Map[String, String] = SystemPropertyUtils.get("app.home", null) match {
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

  private[this] def getUgi(): UserGroupInformation = {
    if (ugi == null) {
      ugi = {
        val enableString = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_ENABLE, "false")
        val kerberosEnable = Try(enableString.trim.toBoolean).getOrElse(false)
        if (kerberosEnable) {
          kerberosLogin()
        } else {
          UserGroupInformation.createRemoteUser(hadoopUserName)
        }
      }
    }
    ugi
  }

  private[this] lazy val hadoopConfDir: String = Try(FileUtils.getPathFromEnv(HADOOP_CONF_DIR)) match {
    case Failure(_) => FileUtils.resolvePath(FileUtils.getPathFromEnv(HADOOP_HOME), CONF_SUFFIX)
    case Success(value) => value
  }

  private[this] lazy val tgtRefreshTime: Long = {
    val user = UserGroupInformation.getLoginUser
    val method = classOf[UserGroupInformation].getDeclaredMethod("getTGT")
    method.setAccessible(true)
    tgt = method.invoke(user).asInstanceOf[KerberosTicket]
    Option(tgt) match {
      case Some(value) =>
        val start = value.getStartTime.getTime
        val end = value.getEndTime.getTime
        ((end - start) * 0.90f).toLong
      case _ => 0
    }
  }

  def getConfigurationFromHadoopConfDir(confDir: String = hadoopConfDir): Configuration = {
    if (!configurationCache.containsKey(confDir)) {
      FileUtils.exists(confDir)
      val hadoopConfDir = new File(confDir)
      val confName = List("core-site.xml", "hdfs-site.xml", "yarn-site.xml")
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
  def hadoopConf: Configuration = Option(reusableConf).getOrElse {
    reusableConf = getConfigurationFromHadoopConfDir(hadoopConfDir)
    //add hadoopConfDir to classpath...you know why???
    ClassLoaderUtils.loadResource(hadoopConfDir)

    if (StringUtils.isBlank(reusableConf.get("hadoop.tmp.dir"))) {
      reusableConf.set("hadoop.tmp.dir", "/tmp")
    }
    if (StringUtils.isBlank(reusableConf.get("hbase.fs.tmp.dir"))) {
      reusableConf.set("hbase.fs.tmp.dir", "/tmp")
    }
    // disable timeline service as we only query yarn app here.
    // Otherwise we may hit this kind of ERROR:
    // java.lang.ClassNotFoundException: com.sun.jersey.api.client.config.ClientConfig
    reusableConf.set("yarn.timeline-service.enabled", "false")
    reusableConf.set("fs.hdfs.impl", classOf[DistributedFileSystem].getName)
    reusableConf.set("fs.file.impl", classOf[LocalFileSystem].getName)
    reusableConf.set("fs.hdfs.impl.disable.cache", "true")
    reusableConf
  }

  private[this] def closeHadoop(): Unit = {
    if (reusableHdfs != null) {
      reusableHdfs.close()
      reusableHdfs = null
    }
    if (reusableYarnClient != null) {
      reusableYarnClient.close()
      reusableYarnClient = null
    }
    if (tgt != null && !tgt.isDestroyed) {
      tgt.destroy()
      tgt = null
    }
    reusableConf = null
    ugi = null
  }

  private[this] def kerberosLogin(): UserGroupInformation = {
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
    hadoopConf.set(KEY_HADOOP_SECURITY_AUTHENTICATION, KEY_KERBEROS)
    Try {
      UserGroupInformation.setConfiguration(hadoopConf)
      val ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab)
      logInfo("kerberos authentication successful")
      ugi
    }.recover { case e => throw e }
      .get
  }

  def hdfs: FileSystem = {
    Option(reusableHdfs).getOrElse {
      reusableHdfs = Try {
        getUgi().doAs[FileSystem](new PrivilegedAction[FileSystem]() {
          // scalastyle:off FileSystemGet
          override def run(): FileSystem = FileSystem.get(hadoopConf)
          // scalastyle:on FileSystemGet
        })
      } match {
        case Success(fs) =>
          val enableString = kerberosConf.getOrElse(KEY_SECURITY_KERBEROS_ENABLE, "false")
          val kerberosEnable = Try(enableString.trim.toBoolean).getOrElse(false)
          if (kerberosEnable) {
            // reLogin...
            val timer = new Timer()
            timer.schedule(new TimerTask {
              override def run(): Unit = {
                closeHadoop()
                logInfo(s"Check Kerberos Tgt And reLogin From Keytab Finish:refresh time: ${DateUtils.format()}")
              }
            }, tgtRefreshTime, tgtRefreshTime)
          }
          fs
        case Failure(e) =>
          throw new IllegalArgumentException(s"[StreamX] access hdfs error: $e")
      }
      reusableHdfs
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
        val conf = hadoopConf
        val useHttps = YarnConfiguration.useHttps(conf)
        val (addressPrefix, defaultPort, protocol) = useHttps match {
          case x if x => (YarnConfiguration.RM_WEBAPP_HTTPS_ADDRESS, "8090", "https://")
          case _ => (YarnConfiguration.RM_WEBAPP_ADDRESS, "8088", "http://")
        }

        rmHttpURL = Option(conf.get("yarn.web-proxy.address", null)) match {
          case Some(proxy) => s"$protocol$proxy"
          case _ =>
            val name = if (!HAUtil.isHAEnabled(conf)) addressPrefix else {
              val yarnConf = new YarnConfiguration(conf)
              val activeRMId = {
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
              require(activeRMId != null, "[StreamX] HadoopUtils.getRMWebAppURL: can not found yarn active node")
              logInfo(s"current activeRMHAId: $activeRMId")
              val appActiveRMKey = HAUtil.addSuffix(addressPrefix, activeRMId)
              val hostnameActiveRMKey = HAUtil.addSuffix(YarnConfiguration.RM_HOSTNAME, activeRMId)
              if (null == HAUtil.getConfValueForRMInstance(appActiveRMKey, yarnConf) && null != HAUtil.getConfValueForRMInstance(hostnameActiveRMKey, yarnConf)) {
                logInfo(s"Find rm web address by : $hostnameActiveRMKey")
                hostnameActiveRMKey
              } else {
                logInfo(s"Find rm web address by : $appActiveRMKey")
                appActiveRMKey
              }
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
            buffer
              .append(":")
              .append(address.getPort)
              .toString()
        }
        logInfo(s"yarn resourceManager webapp url:$rmHttpURL")
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
    require(appId != null, "[StreamX] HadoopUtils.toApplicationId: applicationId muse not be null")
    val timestampAndId = appId.split("_")
    ApplicationId.newInstance(timestampAndId(1).toLong, timestampAndId.last.toInt)
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String = yarnClient.getApplicationReport(applicationId).getTrackingUrl

  @throws[IOException] def downloadJar(jarOnHdfs: String): String = {
    val tmpDir = FileUtils.createTempDir()
    val fs = FileSystem.get(new Configuration)
    val sourcePath = fs.makeQualified(new Path(jarOnHdfs))
    if (!fs.exists(sourcePath)) throw new IOException(s"jar file: $jarOnHdfs doesn't exist.")
    val destPath = new Path(tmpDir.getAbsolutePath + "/" + sourcePath.getName)
    fs.copyToLocalFile(sourcePath, destPath)
    new File(destPath.toString).getAbsolutePath
  }
}
