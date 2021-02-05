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

import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang.StringUtils
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, FileSystem, FileUtil, Path}
import org.apache.hadoop.hdfs.HAUtil
import org.apache.hadoop.io.IOUtils

import java.io.{ByteArrayOutputStream, FileWriter, IOException}
import scala.util.{Failure, Success, Try}

object HdfsUtils extends Logger {

  /**
   * 注意:加载hadoop配置文件,有两种方式:
   * 1) 将hadoop的core-site.xml,hdfs-site.xml,yarn-site.xml copy到 resources下
   * 2) 项目在启动时动态加载 $HADOOP_HOME/etc/hadoop下的配置 到 classpath中
   * 推荐第二种方法,不用copy配置文件.
   */
  lazy val conf: Configuration = {
    val conf = new Configuration()
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

  def list(src: String): List[String] = hdfs.listStatus(getPath(src)).map(_.getPath.getName).toList

  def movie(src: String, dst: String): Unit = hdfs.rename(getPath(src), getPath(dst))

  def mkdirs(path: String): Unit = hdfs.mkdirs(getPath(path))

  def copyHdfs(src: String, dst: String, delSrc: Boolean = false, overwrite: Boolean = true): Unit =
    FileUtil.copy(hdfs, getPath(src), hdfs, getPath(dst), delSrc, overwrite, conf)

  def upload(src: String, dst: String, delSrc: Boolean = false, overwrite: Boolean = true): Unit =
    hdfs.copyFromLocalFile(delSrc, overwrite, getPath(src), getPath(dst))

  def upload2(srcs: Array[String], dst: String, delSrc: Boolean = false, overwrite: Boolean = true): Unit =
    hdfs.copyFromLocalFile(delSrc, overwrite, srcs.map(getPath), getPath(dst))

  def download(src: String, dst: String, delSrc: Boolean = false, useRawLocalFileSystem: Boolean = false): Unit =
    hdfs.copyToLocalFile(delSrc, getPath(src), getPath(dst), useRawLocalFileSystem)


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

  def exists(path: String): Boolean = hdfs.exists(getPath(path))

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

  @throws[IOException] def delete(src: String): Unit = {
    val path: Path = getPath(src)
    if (hdfs.exists(path)) {
      hdfs.delete(path, true)
    } else {
      logWarn(s"hdfs delete $src,bust file $src is not exists!")
    }
  }

  @throws[IOException] def fileMd5(fileName: String): String = {
    val path = getPath(fileName)
    val in = hdfs.open(path)
    Try(DigestUtils.md5Hex(in)) match {
      case Success(s) =>
        in.close()
        s
      case Failure(e) =>
        in.close()
        throw e
    }
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
