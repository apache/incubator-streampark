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
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{CommonConfigurationKeys, FileSystem, Path}
import org.apache.hadoop.ha.HAServiceProtocol
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.client.RMHAServiceTarget
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException

import java.io.{File, IOException}
import java.net.{InetAddress, InetSocketAddress}
import scala.util.{Success, Try}


object HadoopUtils extends Logger {

  private[this] var rmHttpAddr: String = _
  /**
   *
   * 注意:加载hadoop配置文件,有两种方式:<br>
   * 1) 将hadoop的core-site.xml,hdfs-site.xml,yarn-site.xml copy到 resources下<br>
   * 2) 项目在启动时动态加载 $HADOOP_HOME/etc/hadoop下的配置 到 classpath中<br>
   * 推荐第二种方法,不用copy配置文件.<br>
   *
   */
  lazy val conf: Configuration = {
    val conf = new Configuration()
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
    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
    conf.set("fs.hdfs.impl.disable.cache", "true")
    conf
  }

  lazy val yarnClient: YarnClient = {
    val yarnClient = YarnClient.createYarnClient
    val yarnConf = new YarnConfiguration(conf)
    yarnClient.init(yarnConf)
    yarnClient.start()
    yarnClient
  }

  /**
   * 从yarn源码里刨出来的...
   *
   * @param getLatest :
   *                  默认单例模式,如果getLatest=true则再次寻找活跃节点返回,主要是考虑到主备的情况,
   *                  如: 第一次获取的时候返回的是一个当前的活跃节点,之后可能这个活跃节点挂了,就不能提供服务了,
   *                  此时在调用该方法,只需要传入true即可再次获取一个最新的活跃节点返回
   * @return
   */
  def getRMWebAppURL(getLatest: Boolean = false): String = {
    if (rmHttpAddr == null || getLatest) {
      synchronized {
        if (rmHttpAddr == null || getLatest) {
          val useHttps = YarnConfiguration.useHttps(conf)
          val httpPrefix = "yarn.resourcemanager.webapp.address"
          val httpsPrefix = "yarn.resourcemanager.webapp.https.address"

          val inetSocketAddress = if (HAUtil.isHAEnabled(conf)) {
            val yarnConf = new YarnConfiguration(conf)
            val ids = HAUtil.getRMHAIds(conf).toArray()
            var address: InetSocketAddress = null
            ids.foreach(x => {
              if (address == null) {
                val conf = new YarnConfiguration(yarnConf)
                conf.set(YarnConfiguration.RM_HA_ID, x.toString)
                val serviceTarget = new RMHAServiceTarget(conf)
                val rpcTimeoutForChecks = yarnConf.getInt(
                  CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_KEY,
                  CommonConfigurationKeys.HA_FC_CLI_CHECK_TIMEOUT_DEFAULT)
                val proto = serviceTarget.getProxy(yarnConf, rpcTimeoutForChecks)
                if (proto.getServiceStatus.getState == HAServiceProtocol.HAServiceState.ACTIVE) {
                  address = if (useHttps) {
                    val name = HAUtil.addSuffix(httpsPrefix, x.toString)
                    conf.getSocketAddr(name, "0.0.0.0:8090", 8090)
                  } else {
                    val name = HAUtil.addSuffix(httpPrefix, x.toString)
                    conf.getSocketAddr(name, "0.0.0.0:8088", 8088)
                  }
                }
              }
            })
            if (address == null) {
              throw new YarnRuntimeException("[StreamX] can not found yarn active node")
            }
            address
          } else {
            if (useHttps) {
              conf.getSocketAddr(httpsPrefix, "0.0.0.0:8090", 8090)
            } else {
              conf.getSocketAddr(httpPrefix, "0.0.0.0:8088", 8088)
            }
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
          rmHttpAddr = buffer.toString
        }
      }
    }
    rmHttpAddr
  }

  def toApplicationId(appId: String): ApplicationId = {
    require(appId != null)
    val timestampAndId = appId.split("_")
    ApplicationId.newInstance(timestampAndId(1).toLong, timestampAndId.last.toInt)
  }

  def getYarnAppTrackingUrl(applicationId: ApplicationId): String = {
    yarnClient.getApplicationReport(applicationId).getTrackingUrl
  }

  @throws[IOException]
  def downloadJar(jarOnHdfs: String): String = {
    val tmpDir = Files.createTempDir
    val fs = FileSystem.get(new Configuration)
    val sourcePath = fs.makeQualified(new Path(jarOnHdfs))
    if (!fs.exists(sourcePath)) throw new IOException("jar file: " + jarOnHdfs + " doesn't exist.")
    val destPath = new Path(tmpDir.getAbsolutePath + "/" + sourcePath.getName)
    fs.copyToLocalFile(sourcePath, destPath)
    new File(destPath.toString).getAbsolutePath
  }
}
