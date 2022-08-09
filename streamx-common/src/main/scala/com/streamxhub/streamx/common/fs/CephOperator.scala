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

import com.amazonaws.{ClientConfiguration, Protocol}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.internal.StaticCredentialsProvider
import com.amazonaws.services.s3.{AmazonS3, AmazonS3Client, AmazonS3ClientBuilder, S3ClientOptions}
import com.streamxhub.streamx.common.util.Logger

object CephOperator extends FsOperator with Logger {

  lazy val s3Client: AmazonS3 = {
    val awsCredentials = new BasicAWSCredentials("", "")
    val clientConfig = new ClientConfiguration()
    clientConfig.setProtocol(Protocol.HTTP)

    AmazonS3ClientBuilder.defaultClient()

    val s3Client = AmazonS3Client.builder()
      .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
      .withClientConfiguration(clientConfig).build()
    s3Client.setEndpoint(endpoint)
    s3Client.setS3ClientOptions(new S3ClientOptions().withPathStyleAccess(true))
    s3Client
  }

  override def exists(path: String): Boolean = {
    s3Client.do
  }

  override def mkdirs(path: String): Unit = ???

  override def delete(path: String): Unit = ???

  override def mkCleanDirs(path: String): Unit = ???

  override def upload(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = ???

  override def copy(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = ???

  override def copyDir(srcPath: String, dstPath: String, delSrc: Boolean, overwrite: Boolean): Unit = ???

  override def move(srcPath: String, dstPath: String): Unit = ???

  override def fileMd5(path: String): String = ???
}
