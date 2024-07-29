/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.streampark.common.conf

import java.lang.{Integer => JavaInt, Long => JavaLong}

object CommonConfig {

  val STREAMPARK_WORKSPACE_LOCAL: InternalOption = InternalOption(
    key = "streampark.workspace.local",
    defaultValue = "/streampark",
    classType = classOf[String])

  val STREAMPARK_WORKSPACE_REMOTE: InternalOption = InternalOption(
    key = "streampark.workspace.remote",
    defaultValue = "/streampark",
    classType = classOf[String])

  val STREAMPARK_HADOOP_USER_NAME: InternalOption = InternalOption(
    key = "streampark.hadoop-user-name",
    defaultValue = "hdfs",
    classType = classOf[String])

  val STREAMPARK_PROXY_YARN_URL: InternalOption = InternalOption(
    key = "streampark.proxy.yarn-url",
    defaultValue = "",
    classType = classOf[String],
    description = "proxy yarn url. ex: knox proxy or other")

  val STREAMPARK_YARN_AUTH: InternalOption = InternalOption(
    key = "streampark.yarn.http-auth",
    defaultValue = "",
    classType = classOf[String],
    description = "yarn http auth type. ex: simple, kerberos")

  val DOCKER_HOST: InternalOption = InternalOption(
    key = "streampark.docker.http-client.docker-host",
    defaultValue = "",
    classType = classOf[String],
    description = "docker host for DockerHttpClient")

  val DOCKER_MAX_CONNECTIONS: InternalOption = InternalOption(
    key = "streampark.docker.http-client.max-connections",
    defaultValue = 100,
    classType = classOf[JavaInt],
    description = "instantiating max connections for DockerHttpClient")

  val DOCKER_CONNECTION_TIMEOUT_SEC: InternalOption = InternalOption(
    key = "streampark.docker.http-client.connection-timeout-sec",
    defaultValue = 100L,
    classType = classOf[JavaLong],
    description = "instantiating connection timeout for DockerHttpClient")

  val DOCKER_RESPONSE_TIMEOUT_SEC: InternalOption = InternalOption(
    key = "streampark.docker.http-client.response-timeout-sec",
    defaultValue = 120L,
    classType = classOf[JavaLong],
    description = "instantiating connection timeout for DockerHttpClient")

  val MAVEN_SETTINGS_PATH: InternalOption = InternalOption(
    key = "streampark.maven.settings",
    defaultValue = null,
    classType = classOf[String],
    description = "maven settings.xml full path")

  val MAVEN_REMOTE_URL: InternalOption = InternalOption(
    key = "streampark.maven.central.repository",
    defaultValue = "https://repo1.maven.org/maven2/",
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

  val MAVEN_AUTH_USER: InternalOption = InternalOption(
    key = "streampark.maven.auth.user",
    defaultValue = null,
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

  val MAVEN_AUTH_PASSWORD: InternalOption = InternalOption(
    key = "streampark.maven.auth.password",
    defaultValue = null,
    classType = classOf[String],
    description = "maven repository used for built-in compilation")

  val KERBEROS_TTL: InternalOption = InternalOption(
    key = "security.kerberos.ttl",
    defaultValue = "2h",
    classType = classOf[String],
    description = "kerberos default ttl")

  val READ_LOG_MAX_SIZE: InternalOption = InternalOption(
    key = "streampark.read-log.max-size",
    defaultValue = "1mb",
    classType = classOf[String],
    description = "The maximum size of the default read log")

  val SPRING_PROFILES_ACTIVE: InternalOption = InternalOption(
    key = "spring.profiles.active",
    defaultValue = "h2",
    classType = classOf[String],
    description = "Use the database type")

}
