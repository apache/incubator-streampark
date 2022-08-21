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

package com.streamxhub.streamx.common.fs

import com.aliyun.oss.model.ObjectMetadata
import com.aliyun.oss.{OSS, OSSClientBuilder}
import com.streamxhub.streamx.common.conf.ConfigOption
import com.streamxhub.streamx.common.util.Logger
import org.apache.commons.codec.digest.DigestUtils

import java.io.{ByteArrayInputStream, Closeable, File, FileOutputStream, InputStream}
import java.util.Properties
import scala.util.{Failure, Success, Try}

class OssOperator(properties: Properties) extends ObjectOperator with Closeable with Logger {

  implicit val prop = properties

  val endpoint: String = ConfigOption(
    key = "resource.oss.endpoint",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val accessKey: String = ConfigOption(
    key = "resource.oss.accessKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val secretKey: String = ConfigOption(
    key = "resource.oss.secretKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val bucket: String = ConfigOption(
    key = "resource.oss.bucket",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  lazy val ossClient: OSS = {
    val ossClient = new OSSClientBuilder()
      .build(endpoint, accessKey, secretKey)
    val exists = ossClient.doesBucketExist(bucket)
    if (!exists) {
      throw new IllegalArgumentException(s"`$bucket` is non exists")
    }
    ossClient
  }

  override def exists(objectPath: String): Boolean = {
    ossClient.doesObjectExist(bucket, objectPath)
  }

  override def mkdirs(objectPath: String): Unit = {
    if (!ossClient.doesObjectExist(bucket, objectPath)) {
      ossClient.putObject(bucket, objectPath, new ByteArrayInputStream(Array(0)))
    }
  }

  override def delete(objectPath: String): Unit = {
    ossClient.deleteObject(bucket, objectPath)
  }

  override def upload(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    ossClient.putObject(bucket, dstPath, new File(srcPath))
  }

  override def copy(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    ossClient.copyObject(bucket, srcPath, bucket, dstPath)
    ossClient.deleteObject(bucket, srcPath)
  }

  override def fileMd5(objectPath: String): String = {
    val ossObject = ossClient.getObject(bucket, objectPath)
    val in = ossObject.getObjectContent
    Try(DigestUtils.md5Hex(in)) match {
      case Success(s) =>
        in.close()
        s
      case Failure(e) =>
        in.close()
        throw e
    }
  }

  override def close(): Unit = {
    ossClient.shutdown()
  }

  override def download(srcPath: String, dstPath: String): Unit = {
    val ossObject = ossClient.getObject(bucket, srcPath)
    download(ossObject.getObjectContent, new FileOutputStream(dstPath))
  }

  override def putObject(objectPath: String, obj: Array[Byte]): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(obj.length)
    ossClient.putObject(bucket, objectPath, new ByteArrayInputStream(obj), metadata)
  }

  override def getObject(objectPath: String): InputStream = {
    val ossObject = ossClient.getObject(bucket, objectPath)
    ossObject.getObjectContent
  }
}

object OssOperator {
  def apply(properties: Properties): OssOperator = new OssOperator(properties)
}
