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

import java.io.FileWriter
import java.io.IOException

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.FSDataInputStream
import org.apache.hadoop.fs.FSDataOutputStream
import org.apache.hadoop.fs.FileSystem
import org.apache.hadoop.fs.Path

import scala.util.{Failure, Success, Try}

object HdfsUtils {

  lazy val hdfs: FileSystem = Try(FileSystem.get(new Configuration)) match {
    case Success(fs) => fs
    case Failure(e) => new IllegalArgumentException(s"[StreamX] access hdfs error.$e")
      null
  }

  /**
   * 在hdfs 上创建一个新的文件，将某些数据写入到hdfs中
   *
   * @param fileName
   * @param content
   * @throws
   */
  @throws[IOException] def createFile(fileName: String, content: String): Unit = {
    val path: Path = getPath(fileName)
    require(hdfs.exists(path), s"[StreamX] hdfs $fileName is exists!! ")
    val outputStream: FSDataOutputStream = hdfs.create(path)
    outputStream.writeUTF(content)
    outputStream.flush()
    outputStream.close()
  }

  @throws[IOException] def readFile(fileName: String): String = {
    val path: Path = getPath(fileName)
    require(!hdfs.exists(path) | hdfs.isDirectory(path), s"[StreamX] path:$fileName not exists or isDirectory ")
    val in = hdfs.open(path)
    try in.readUTF finally in.close()
  }

  @throws[IOException] def deleteFile(fileName: String): Unit = {
    val path: Path = getPath(fileName)
    require(hdfs.exists(path))
    hdfs.delete(path, true)
  }

  @throws[IOException] def uploadFile(fileName: String, hdfsPath: String): Unit = {
    val src: Path = getPath(fileName)
    val dst: Path = getPath(hdfsPath)
    hdfs.copyFromLocalFile(src, dst)
  }

  @throws[Exception] def downloadFile(fileName: String, localPath: String): Unit = {
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
