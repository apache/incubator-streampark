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

import com.ctc.wstx.io.{StreamBootstrapper, SystemId}
import com.ctc.wstx.stax.WstxInputFactory
import com.streamxhub.streamx.common.conf.ConfigConst._
import com.streamxhub.streamx.common.conf.{CommonConfig, InternalConfigHolder}
import org.apache.commons.collections.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs._
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.hadoop.security.UserGroupInformation
import org.apache.hadoop.service.Service.STATE
import org.apache.hadoop.util.StringInterner
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.YarnConfiguration
import org.codehaus.stax2.XMLStreamReader2

import java.io.{BufferedInputStream, File, FileInputStream, IOException}
import java.security.{PrivilegedAction}
import java.util
import java.util.concurrent._
import java.util.{Timer, TimerTask}
import javax.security.auth.kerberos.KerberosTicket
import javax.xml.stream.{XMLStreamConstants, XMLStreamException}
import scala.collection.JavaConversions._
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

  private[this] val XML_INPUT_FACTORY = new WstxInputFactory

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

  def getUgi(): UserGroupInformation = {
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
      val confName = List("hdfs-default.xml", "core-site.xml", "hdfs-site.xml", "yarn-site.xml")
      val files = hadoopConfDir.listFiles().filter(x => x.isFile && confName.contains(x.getName)).toList
      val conf = new Configuration()
      if (CollectionUtils.isNotEmpty(files)) {
        files.foreach(x => {
          //HDFS default value change (with adding time unit) breaks old version MR tarball work with Hadoop 3.x
          //detail: https://issues.apache.org/jira/browse/HDFS-12920
          if (x.getName == "hdfs-default.xml" || x.getName == "hdfs-site.xml") {
            val reader = parseHadoopConf(x)
            while (reader.hasNext) {
              if (reader.next() == XMLStreamConstants.START_ELEMENT) {
                reader.getLocalName match {
                  case "property" =>
                    val attrCount = reader.getAttributeCount
                    var name: String = null
                    var value: String = null
                    for (i <- 0 until attrCount) {
                      val propertyAttr = reader.getAttributeLocalName(i)
                      propertyAttr match {
                        case "name" =>
                          name = StringInterner.weakIntern(reader.getAttributeValue(i))
                        case "value" =>
                          value = StringInterner.weakIntern(reader.getAttributeValue(i))
                      }
                    }
                    if (name != null && value != null) {
                      if (value.matches("\\d+s$")) {
                        conf.set(name, value.dropRight(1))
                      } else {
                        conf.set(name, value)
                      }
                    }
                  case _ =>
                }
              }
            }
          } else {
            conf.addResource(new Path(x.getAbsolutePath))
          }
        })
      }
      configurationCache.put(confDir, conf)
    }
    configurationCache(confDir)
  }

  @throws[XMLStreamException] private[this] def parseHadoopConf(f: File): XMLStreamReader2 = {
    val is = new BufferedInputStream(new FileInputStream(f))
    val systemIdStr = new Path(f.getAbsolutePath).toString
    val systemId = SystemId.construct(systemIdStr)
    val readerConfig = XML_INPUT_FACTORY.createPrivateConfig
    XML_INPUT_FACTORY.createSR(
      readerConfig,
      systemId,
      StreamBootstrapper.getInstance(null, systemId, is),
      false,
      true
    )
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
      UserGroupInformation.setLoginUser(ugi)
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

  def toApplicationId(appId: String): ApplicationId = {
    require(appId != null, "[StreamX] HadoopUtils.toApplicationId: applicationId muse not be null")
    val timestampAndId = appId.split("_")
    ApplicationId.newInstance(timestampAndId(1).toLong, timestampAndId.last.toInt)
  }

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
