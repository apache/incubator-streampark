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

package org.apache.streampark.common.conf;

/** Common internal configuration options. */
public final class CommonConfig {

    private CommonConfig() {
    }

    public static final InternalOption STREAMPARK_WORKSPACE_LOCAL =
        new InternalOption("streampark.workspace.local", "/streampark", String.class);

    public static final InternalOption STREAMPARK_WORKSPACE_REMOTE =
        new InternalOption("streampark.workspace.remote", "/streampark", String.class);

    public static final InternalOption STREAMPARK_HADOOP_USER_NAME =
        new InternalOption("streampark.hadoop-user-name", "hdfs", String.class);

    public static final InternalOption STREAMPARK_PROXY_YARN_URL =
        new InternalOption(
            "streampark.proxy.yarn-url",
            "",
            String.class,
            "proxy yarn url. ex: knox proxy or other");

    public static final InternalOption STREAMPARK_YARN_AUTH =
        new InternalOption(
            "streampark.yarn.http-auth",
            "",
            String.class,
            "yarn http auth type. ex: simple, kerberos");

    public static final InternalOption DOCKER_HOST =
        new InternalOption(
            "streampark.docker.http-client.docker-host",
            "",
            String.class,
            "docker host for DockerHttpClient");

    public static final InternalOption DOCKER_MAX_CONNECTIONS =
        new InternalOption(
            "streampark.docker.http-client.max-connections",
            100,
            Integer.class,
            "instantiating max connections for DockerHttpClient");

    public static final InternalOption DOCKER_CONNECTION_TIMEOUT_SEC =
        new InternalOption(
            "streampark.docker.http-client.connection-timeout-sec",
            100L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    public static final InternalOption DOCKER_RESPONSE_TIMEOUT_SEC =
        new InternalOption(
            "streampark.docker.http-client.response-timeout-sec",
            120L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    public static final InternalOption MAVEN_SETTINGS_PATH =
        new InternalOption(
            "streampark.maven.settings",
            null,
            String.class,
            "maven settings.xml full path");

    public static final InternalOption MAVEN_REMOTE_URL =
        new InternalOption(
            "streampark.maven.central.repository",
            "https://repo1.maven.org/maven2/",
            String.class,
            "maven repository used for built-in compilation");

    public static final InternalOption MAVEN_AUTH_USER =
        new InternalOption(
            "streampark.maven.auth.user",
            null,
            String.class,
            "maven repository used for built-in compilation");

    public static final InternalOption MAVEN_AUTH_PASSWORD =
        new InternalOption(
            "streampark.maven.auth.password",
            null,
            String.class,
            "maven repository used for built-in compilation");

    public static final InternalOption KERBEROS_TTL =
        new InternalOption("security.kerberos.ttl", "2h", String.class, "kerberos default ttl");

    public static final InternalOption READ_LOG_MAX_SIZE =
        new InternalOption(
            "streampark.read-log.max-size",
            "1mb",
            String.class,
            "The maximum size of the default read log");

    public static final InternalOption SPRING_PROFILES_ACTIVE =
        new InternalOption(
            "spring.profiles.active", "h2", String.class, "Use the database type");
}
