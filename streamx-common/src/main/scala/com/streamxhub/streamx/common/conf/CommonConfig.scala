/*
 * Copyright 2019 The StreamX Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamxhub.streamx.common.conf

import java.lang.{Integer => JavaInt, Long => JavaLong}

/**
 * Common Configuration
 *
 * @author Al-assad
 */
object CommonConfig {

  val STREAMX_WORKSPACE_LOCAL: InternalOption = InternalOption(
    key = "streamx.workspace.local",
    defaultValue = "/streamx",
    classType = classOf[String])

  val STREAMX_WORKSPACE_REMOTE: InternalOption = InternalOption(
    key = "streamx.workspace.remote",
    defaultValue = "/streamx",
    classType = classOf[String])

  val STREAMX_HADOOP_USER_NAME: InternalOption = InternalOption(
    key = "streamx.hadoop-user-name",
    defaultValue = "hdfs",
    classType = classOf[String])

  val STREAMX_PROXY_YARN_URL: InternalOption = InternalOption(
    key = "streamx.proxy.yarn-url",
    defaultValue = "",
    classType = classOf[String],
    description = "proxy yarn url. ex: knox proxy or other")

  val STREAM_YARN_AUTH: InternalOption = InternalOption(
    key = "streamx.yarn.http-auth",
    defaultValue = "",
    classType = classOf[String],
    description = "yarn http auth type. ex: sample, kerberos")

  val DOCKER_MAX_CONNECTIONS: InternalOption = InternalOption(
    key = "streamx.docker.http-client.max-connections",
    defaultValue = 100,
    classType = classOf[JavaInt],
    description = "instantiating max connections for DockerHttpClient")

  val DOCKER_CONNECTION_TIMEOUT_SEC: InternalOption = InternalOption(
    key = "streamx.docker.http-client.connection-timeout-sec",
    defaultValue = 100L,
    classType = classOf[JavaLong],
    description = "instantiating connection timeout for DockerHttpClient")

  val DOCKER_RESPONSE_TIMEOUT_SEC: InternalOption = InternalOption(
    key = "streamx.docker.http-client.response-timeout-sec",
    defaultValue = 120L,
    classType = classOf[JavaLong],
    description = "instantiating connection timeout for DockerHttpClient")

  val MAVEN_REMOTE_URL: InternalOption = InternalOption(
    key = "streamx.maven.central.repository",
    defaultValue = "https://repo1.maven.org/maven2/",
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

  val MAVEN_AUTH_USER: InternalOption = InternalOption(
    key = "streamx.maven.auth.user",
    defaultValue = null,
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

  val MAVEN_AUTH_PASSWORD: InternalOption = InternalOption(
    key = "streamx.maven.auth.password",
    defaultValue = null,
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

}
