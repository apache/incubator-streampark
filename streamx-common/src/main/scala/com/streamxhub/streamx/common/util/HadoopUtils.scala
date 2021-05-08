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
import org.apache.hadoop.fs.{FileSystem, Path}
import org.apache.hadoop.net.NetUtils
import org.apache.hadoop.yarn.api.records.ApplicationId
import org.apache.hadoop.yarn.client.api.YarnClient
import org.apache.hadoop.yarn.conf.{HAUtil, YarnConfiguration}

import java.io.{File, IOException}
import java.net.InetAddress
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

  lazy val yarnClient = {
    val yarnClient = YarnClient.createYarnClient
    val yarnConf = new YarnConfiguration(conf)
    yarnClient.init(yarnConf)
    yarnClient.start()
    yarnClient
  }

  def getRMWebAppURL(): String = {
    if (rmHttpAddr == null) {
      val rmId: String = if (HAUtil.isHAEnabled(conf)) HAUtil.getRMHAIds(conf).toArray().head.asInstanceOf[String] else null

      val url = if (YarnConfiguration.useHttps(conf)) {
        val key = "yarn.resourcemanager.webapp.https.address"
        val name = if (rmId == null) key else HAUtil.addSuffix(key, rmId)
        conf.getSocketAddr(name, "0.0.0.0:8090", 8090)
      } else {
        val key = "yarn.resourcemanager.webapp.address"
        val name = if (rmId == null) key else HAUtil.addSuffix(key, rmId)
        conf.getSocketAddr(name, "0.0.0.0:8088", 8088)
      }

      val address = NetUtils.getConnectAddress(url)
      val buffer = new StringBuilder
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
