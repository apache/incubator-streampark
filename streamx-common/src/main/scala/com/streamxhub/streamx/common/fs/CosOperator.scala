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

import com.qcloud.cos.{COSClient, ClientConfig}
import com.qcloud.cos.auth.BasicCOSCredentials
import com.qcloud.cos.model.ObjectMetadata
import com.qcloud.cos.region.Region
import com.streamxhub.streamx.common.conf.ConfigOption
import com.streamxhub.streamx.common.util.Logger
import org.apache.commons.codec.digest.DigestUtils

import java.io.{ByteArrayInputStream, Closeable, File, FileOutputStream, InputStream}
import java.util.Properties
import scala.util.{Failure, Success, Try}

class CosOperator(properties: Properties) extends ObjectOperator with Closeable with Logger {

  implicit val prop = properties

  val region: String = ConfigOption(
    key = "resource.cos.region",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val accessKey: String = ConfigOption(
    key = "resource.cos.accessKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val secretKey: String= ConfigOption(
    key = "resource.cos.secretKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val bucket: String = ConfigOption(
    key = "resource.cos.bucket",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  lazy val cosClient: COSClient = {
    val cred = new BasicCOSCredentials(accessKey, secretKey)
    val config = new ClientConfig(new Region(region))
    val cosClient = new COSClient(cred, config)
    val exists = cosClient.doesBucketExist(bucket)
    if (!exists) {
      throw new IllegalArgumentException(s"`$bucket` is non exists")
    }
    cosClient
  }

  override def exists(objectPath: String): Boolean = {
    cosClient.doesObjectExist(bucket, objectPath)
  }

  override def mkdirs(objectPath: String): Unit = {
    if (!cosClient.doesObjectExist(bucket, objectPath)) {
      cosClient.putObject(bucket, objectPath, new ByteArrayInputStream(Array(0)), new ObjectMetadata())
    }
  }

  override def delete(objectPath: String): Unit = {
    cosClient.deleteObject(bucket, objectPath)
  }

  override def upload(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    cosClient.putObject(bucket, dstPath, new File(srcPath))
  }

  override def copy(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    cosClient.copyObject(bucket, srcPath, bucket, dstPath)
    cosClient.deleteObject(bucket, srcPath)
  }

  override def copyDir(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = ???

  override def move(srcPath: String, dstPath: String): Unit = {}

  override def fileMd5(objectPath: String): String = {
    val cosObject = cosClient.getObject(bucket, objectPath)
    val in = cosObject.getObjectContent
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
    cosClient.shutdown()
  }

  override def download(srcPath: String, dstPath: String): Unit = {
    val cosObject = cosClient.getObject(bucket, srcPath)
    download(cosObject.getObjectContent, new FileOutputStream(dstPath))
  }

  override def putObject(objectPath: String, obj: Array[Byte]): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(obj.length)
    cosClient.putObject(bucket, objectPath, new ByteArrayInputStream(obj), metadata)
  }

  override def getObject(objectPath: String): InputStream = {
    val cosObject = cosClient.getObject(bucket, objectPath)
    cosObject.getObjectContent
  }
}

object CosOperator {
  def apply(properties: Properties): CosOperator = new CosOperator(properties)
}
