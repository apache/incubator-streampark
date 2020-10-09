/**
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
package com.streamxhub.common.util

import org.apache.hadoop.hdfs.HAUtil
import java.io.{ByteArrayOutputStream, FileWriter, IOException}

import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.Path
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.hdfs.client.HdfsUtils
import org.apache.hadoop.io.IOUtils

import scala.util.{Failure, Success, Try}

object HdfsUtils extends Logger {

  private[this] val classLoader = Thread.currentThread().getContextClassLoader() match {
    case null => classOf[HdfsUtils].getClassLoader
    case loader => loader
  }

  /**
   * 注意:加载hadoop配置文件,有两种方式:
   * 1) 将hadoop的core-site.xml,hdfs-site.xml,yarn-site.xml copy到 resources下
   * 2) 项目在启动时动态加载 $HADOOP_HOME/etc/hadoop下的配置 到 classpath中
   * e.g: java -cp $HADOOP_HOME/etc/hadoop ...
   * 推荐第二种方法,不用copy配置文件.
   */
  lazy val conf: Configuration = {
    val conf = new Configuration()
    conf.setClassLoader(classLoader)
    if (StringUtils.isBlank(conf.get("hadoop.tmp.dir"))) {
      conf.set("hadoop.tmp.dir", "/tmp")
    }
    if (StringUtils.isBlank(conf.get("hbase.fs.tmp.dir"))) {
      conf.set("hbase.fs.tmp.dir", "/tmp")
    }
    conf.set("yarn.timeline-service.enabled", "false")
    conf.set("fs.hdfs.impl", "org.apache.hadoop.hdfs.DistributedFileSystem")
    conf.set("fs.hdfs.impl.disable.cache", "true")
    conf
  }


  lazy val hdfs: FileSystem = Try(FileSystem.get(conf)) match {
    case Success(fs) => fs
    case Failure(e) => new IllegalArgumentException(s"[StreamX] access hdfs error.$e")
      null
  }

  def getDefaultFS: String = conf.get(FileSystem.FS_DEFAULT_NAME_KEY)

  @throws[Exception] def getNameNode: String = {
    Try(HAUtil.getAddressOfActive(hdfs).getHostString) match {
      case Success(value) => value
      case Failure(exception) => throw exception
    }
  }

  /**
   * 在hdfs 上创建一个新的文件，将某些数据写入到hdfs中
   *
   * @param fileName
   * @param content
   * @throws
   */
  @throws[IOException] def create(fileName: String, content: String): Unit = {
    val path: Path = getPath(fileName)
    require(hdfs.exists(path), s"[StreamX] hdfs $fileName is exists!! ")
    val outputStream: FSDataOutputStream = hdfs.create(path)
    outputStream.writeUTF(content)
    outputStream.flush()
    outputStream.close()
  }

  def exists(path: String) = hdfs.exists(getPath(path))

  @throws[IOException] def read(fileName: String): String = {
    val path: Path = getPath(fileName)
    require(hdfs.exists(path) && !hdfs.isDirectory(path), s"[StreamX] path:$fileName not exists or isDirectory ")
    val in = hdfs.open(path)
    val out = new ByteArrayOutputStream()
    IOUtils.copyBytes(in, out, 4096, false)
    out.flush()
    IOUtils.closeStream(in)
    IOUtils.closeStream(out)
    new String(out.toByteArray)
  }

  @throws[IOException] def deleteFile(fileName: String): Unit = {
    val path: Path = getPath(fileName)
    require(hdfs.exists(path))
    hdfs.delete(path, true)
  }

  @throws[IOException] def list(hdfsPath: String): List[String] = {
    val path: Path = getPath(hdfsPath)
    hdfs.listStatus(path).map(_.getPath.getName).toList
  }

  @throws[IOException] def upload(fileName: String, hdfsPath: String): Unit = {
    val src: Path = getPath(fileName)
    val dst: Path = getPath(hdfsPath)
    hdfs.copyFromLocalFile(src, dst)
  }

  @throws[IOException] def movie(fileName: String, hdfsPath: String): Unit = {
    val src: Path = getPath(fileName)
    val dst: Path = getPath(hdfsPath)
    hdfs.rename(src, dst)
  }

  @throws[IOException] def mkdirs(fileName: String): Unit = {
    val path: Path = getPath(fileName)
    hdfs.mkdirs(path);
  }

  @throws[Exception] def download(fileName: String, localPath: String): Unit = {
    val src: Path = getPath(fileName)
    val dst: Path = getPath(localPath)
    hdfs.copyToLocalFile(src, dst)
    hdfs.copyToLocalFile(false, src, dst, true)
  }

  // 下载文件到local
  @throws[IOException] def downToLocal(hdfsPath: String, localPath: String): Unit = {
    val path: Path = getPath(hdfsPath)
    val input: FSDataInputStream = hdfs.open(path)
    val content: String = input.readUTF
    val fw: FileWriter = new FileWriter(localPath)
    fw.write(content)
    fw.close()
    input.close()
  }

  private[this] def getPath(hdfsPath: String) = new Path(hdfsPath)

}