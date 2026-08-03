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

    private static final InternalOption STREAMPARK_WORKSPACE_LOCAL_OPTION =
        new InternalOption("streampark.workspace.local", "/streampark", String.class);

    private static final InternalOption STREAMPARK_WORKSPACE_REMOTE_OPTION =
        new InternalOption("streampark.workspace.remote", "/streampark", String.class);

    private static final InternalOption STREAMPARK_HADOOP_USER_NAME_OPTION =
        new InternalOption("streampark.hadoop-user-name", "hdfs", String.class);

    private static final InternalOption STREAMPARK_PROXY_YARN_URL_OPTION =
        new InternalOption(
            "streampark.proxy.yarn-url",
            "",
            String.class,
            "proxy yarn url. ex: knox proxy or other");

    private static final InternalOption STREAMPARK_YARN_AUTH_OPTION =
        new InternalOption(
            "streampark.yarn.http-auth",
            "",
            String.class,
            "yarn http auth type. ex: simple, kerberos");

    private static final InternalOption DOCKER_HOST_OPTION =
        new InternalOption(
            "streampark.docker.http-client.docker-host",
            "",
            String.class,
            "docker host for DockerHttpClient");

    private static final InternalOption DOCKER_MAX_CONNECTIONS_OPTION =
        new InternalOption(
            "streampark.docker.http-client.max-connections",
            100,
            Integer.class,
            "instantiating max connections for DockerHttpClient");

    private static final InternalOption DOCKER_CONNECTION_TIMEOUT_SEC_OPTION =
        new InternalOption(
            "streampark.docker.http-client.connection-timeout-sec",
            100L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    private static final InternalOption DOCKER_RESPONSE_TIMEOUT_SEC_OPTION =
        new InternalOption(
            "streampark.docker.http-client.response-timeout-sec",
            120L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    private static final InternalOption MAVEN_SETTINGS_PATH_OPTION =
        new InternalOption(
            "streampark.maven.settings",
            null,
            String.class,
            "maven settings.xml full path");

    private static final InternalOption MAVEN_REMOTE_URL_OPTION =
        new InternalOption(
            "streampark.maven.central.repository",
            "https://repo1.maven.org/maven2/",
            String.class,
            "maven repository used for built-in compilation");

    private static final InternalOption MAVEN_AUTH_USER_OPTION =
        new InternalOption(
            "streampark.maven.auth.user",
            null,
            String.class,
            "maven repository used for built-in compilation");

    private static final InternalOption MAVEN_AUTH_PASSWORD_OPTION =
        new InternalOption(
            "streampark.maven.auth.password",
            null,
            String.class,
            "maven repository used for built-in compilation");

    private static final InternalOption KERBEROS_TTL_OPTION =
        new InternalOption("security.kerberos.ttl", "2h", String.class, "kerberos default ttl");

    private static final InternalOption READ_LOG_MAX_SIZE_OPTION =
        new InternalOption(
            "streampark.read-log.max-size",
            "1mb",
            String.class,
            "The maximum size of the default read log");

    private static final InternalOption SPRING_PROFILES_ACTIVE_OPTION =
        new InternalOption(
            "spring.profiles.active", "h2", String.class, "Use the database type");

    public static InternalOption STREAMPARK_WORKSPACE_LOCAL() {
        return STREAMPARK_WORKSPACE_LOCAL_OPTION;
    }

    public static InternalOption STREAMPARK_WORKSPACE_REMOTE() {
        return STREAMPARK_WORKSPACE_REMOTE_OPTION;
    }

    public static InternalOption STREAMPARK_HADOOP_USER_NAME() {
        return STREAMPARK_HADOOP_USER_NAME_OPTION;
    }

    public static InternalOption STREAMPARK_PROXY_YARN_URL() {
        return STREAMPARK_PROXY_YARN_URL_OPTION;
    }

    public static InternalOption STREAMPARK_YARN_AUTH() {
        return STREAMPARK_YARN_AUTH_OPTION;
    }

    public static InternalOption DOCKER_HOST() {
        return DOCKER_HOST_OPTION;
    }

    public static InternalOption DOCKER_MAX_CONNECTIONS() {
        return DOCKER_MAX_CONNECTIONS_OPTION;
    }

    public static InternalOption DOCKER_CONNECTION_TIMEOUT_SEC() {
        return DOCKER_CONNECTION_TIMEOUT_SEC_OPTION;
    }

    public static InternalOption DOCKER_RESPONSE_TIMEOUT_SEC() {
        return DOCKER_RESPONSE_TIMEOUT_SEC_OPTION;
    }

    public static InternalOption MAVEN_SETTINGS_PATH() {
        return MAVEN_SETTINGS_PATH_OPTION;
    }

    public static InternalOption MAVEN_REMOTE_URL() {
        return MAVEN_REMOTE_URL_OPTION;
    }

    public static InternalOption MAVEN_AUTH_USER() {
        return MAVEN_AUTH_USER_OPTION;
    }

    public static InternalOption MAVEN_AUTH_PASSWORD() {
        return MAVEN_AUTH_PASSWORD_OPTION;
    }

    public static InternalOption KERBEROS_TTL() {
        return KERBEROS_TTL_OPTION;
    }

    public static InternalOption READ_LOG_MAX_SIZE() {
        return READ_LOG_MAX_SIZE_OPTION;
    }

    public static InternalOption SPRING_PROFILES_ACTIVE() {
        return SPRING_PROFILES_ACTIVE_OPTION;
    }
}
