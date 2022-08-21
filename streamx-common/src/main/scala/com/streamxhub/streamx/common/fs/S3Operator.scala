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

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, S3Object}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.streamxhub.streamx.common.conf.ConfigOption
import com.streamxhub.streamx.common.util.Logger
import org.apache.commons.codec.digest.DigestUtils

import java.io.{ByteArrayInputStream, Closeable, File, FileOutputStream, InputStream}
import java.util.Properties
import scala.util.{Failure, Success, Try}

class S3Operator(properties: Properties) extends ObjectOperator with Closeable with Logger {

  implicit val prop = properties

  val endpoint: String = ConfigOption(
    key = "resource.s3.endpoint",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val region: String = ConfigOption(
    key = "resource.s3.region",
    required = false,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val accessKey: String = ConfigOption(
    key = "resource.s3.accessKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val secretKey: String = ConfigOption(
    key = "resource.s3.secretKey",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  val bucket: String = ConfigOption(
    key = "resource.s3.bucket",
    required = true,
    classType = classOf[String],
    defaultValue = null
  ).get()

  lazy val s3Client: AmazonS3 = {
    val s3Client = AmazonS3ClientBuilder
      .standard()
      .withPathStyleAccessEnabled(true)
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
      .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey)))
      .build()
    val exists = s3Client.doesBucketExistV2(bucket)
    if (!exists) {
      throw new IllegalArgumentException(s"`$bucket` is non exists")
    }
    s3Client
  }

  override def exists(objectPath: String): Boolean = {
    s3Client.doesObjectExist(bucket, objectPath)
  }

  override def mkdirs(objectPath: String): Unit = {
    if (!s3Client.doesObjectExist(bucket, objectPath)) {
      val metadata = new ObjectMetadata
      metadata.setContentLength(0)
      s3Client.putObject(
        bucket, objectPath, new ByteArrayInputStream(Array(0)), metadata
      )
    }
  }

  override def delete(objectPath: String): Unit = {
    s3Client.deleteObject(bucket, objectPath)
  }

  override def upload(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    s3Client.putObject(bucket, dstPath, new File(srcPath))
  }

  override def copy(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = {
    s3Client.copyObject(bucket, srcPath, bucket, dstPath)
    s3Client.deleteObject(bucket, srcPath)
  }

  override def fileMd5(objectPath: String): String = {
    val s3Object = s3Client.getObject(bucket, objectPath)
    val in = s3Object.getObjectContent
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
    s3Client.shutdown()
  }

  override def download(srcPath: String, dstPath: String): Unit = {
    val ossObject = s3Client.getObject(bucket, srcPath)
    download(ossObject.getObjectContent, new FileOutputStream(dstPath))
  }

  override def putObject(objectPath: String, obj: Array[Byte]): Unit = {
    val metadata = new ObjectMetadata()
    metadata.setContentLength(obj.length)
    s3Client.putObject(bucket, objectPath, new ByteArrayInputStream(obj), metadata)
  }

  override def getObject(objectPath: String): InputStream = {
    val ossObject = s3Client.getObject(bucket, objectPath)
    ossObject.getObjectContent
  }
}

object S3Operator {
  def apply(properties: Properties): S3Operator = new S3Operator(properties)
}

