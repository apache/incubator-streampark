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

public final class CommonConfig {

    private CommonConfig() {
    }

    private static final InternalOption<String> STREAMPARK_WORKSPACE_LOCAL =
        new InternalOption<>("streampark.workspace.local", "/streampark", String.class);

    public static InternalOption<String> STREAMPARK_WORKSPACE_LOCAL() {
        return STREAMPARK_WORKSPACE_LOCAL;
    }

    private static final InternalOption<String> STREAMPARK_WORKSPACE_REMOTE =
        new InternalOption<>("streampark.workspace.remote", "/streampark", String.class);

    public static InternalOption<String> STREAMPARK_WORKSPACE_REMOTE() {
        return STREAMPARK_WORKSPACE_REMOTE;
    }

    private static final InternalOption<String> STREAMPARK_HADOOP_USER_NAME =
        new InternalOption<>("streampark.hadoop-user-name", "hdfs", String.class);

    public static InternalOption<String> STREAMPARK_HADOOP_USER_NAME() {
        return STREAMPARK_HADOOP_USER_NAME;
    }

    private static final InternalOption<String> STREAMPARK_PROXY_YARN_URL =
        new InternalOption<>(
            "streampark.proxy.yarn-url", "", String.class, "proxy yarn url. ex: knox proxy or other");

    public static InternalOption<String> STREAMPARK_PROXY_YARN_URL() {
        return STREAMPARK_PROXY_YARN_URL;
    }

    private static final InternalOption<String> STREAMPARK_YARN_AUTH =
        new InternalOption<>(
            "streampark.yarn.http-auth",
            "",
            String.class,
            "yarn http auth type. ex: simple, kerberos");

    public static InternalOption<String> STREAMPARK_YARN_AUTH() {
        return STREAMPARK_YARN_AUTH;
    }

    private static final InternalOption<String> DOCKER_HOST =
        new InternalOption<>(
            "streampark.docker.http-client.docker-host",
            "",
            String.class,
            "docker host for DockerHttpClient");

    public static InternalOption<String> DOCKER_HOST() {
        return DOCKER_HOST;
    }

    private static final InternalOption<Integer> DOCKER_MAX_CONNECTIONS =
        new InternalOption<>(
            "streampark.docker.http-client.max-connections",
            100,
            Integer.class,
            "instantiating max connections for DockerHttpClient");

    public static InternalOption<Integer> DOCKER_MAX_CONNECTIONS() {
        return DOCKER_MAX_CONNECTIONS;
    }

    private static final InternalOption<Long> DOCKER_CONNECTION_TIMEOUT_SEC =
        new InternalOption<>(
            "streampark.docker.http-client.connection-timeout-sec",
            100L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    public static InternalOption<Long> DOCKER_CONNECTION_TIMEOUT_SEC() {
        return DOCKER_CONNECTION_TIMEOUT_SEC;
    }

    private static final InternalOption<Long> DOCKER_RESPONSE_TIMEOUT_SEC =
        new InternalOption<>(
            "streampark.docker.http-client.response-timeout-sec",
            120L,
            Long.class,
            "instantiating connection timeout for DockerHttpClient");

    public static InternalOption<Long> DOCKER_RESPONSE_TIMEOUT_SEC() {
        return DOCKER_RESPONSE_TIMEOUT_SEC;
    }

    private static final InternalOption<String> MAVEN_SETTINGS_PATH =
        new InternalOption<>(
            "streampark.maven.settings", null, String.class, "maven settings.xml full path");

    public static InternalOption<String> MAVEN_SETTINGS_PATH() {
        return MAVEN_SETTINGS_PATH;
    }

    private static final InternalOption<String> MAVEN_REMOTE_URL =
        new InternalOption<>(
            "streampark.maven.central.repository",
            "https://repo1.maven.org/maven2/",
            String.class,
            "maven repository used for built-in compilation");

    public static InternalOption<String> MAVEN_REMOTE_URL() {
        return MAVEN_REMOTE_URL;
    }

    private static final InternalOption<String> MAVEN_AUTH_USER =
        new InternalOption<>(
            "streampark.maven.auth.user",
            null,
            String.class,
            "maven repository used for built-in compilation");

    public static InternalOption<String> MAVEN_AUTH_USER() {
        return MAVEN_AUTH_USER;
    }

    private static final InternalOption<String> MAVEN_AUTH_PASSWORD =
        new InternalOption<>(
            "streampark.maven.auth.password",
            null,
            String.class,
            "maven repository used for built-in compilation");

    public static InternalOption<String> MAVEN_AUTH_PASSWORD() {
        return MAVEN_AUTH_PASSWORD;
    }

    private static final InternalOption<String> KERBEROS_TTL =
        new InternalOption<>("security.kerberos.ttl", "2h", String.class, "kerberos default ttl");

    public static InternalOption<String> KERBEROS_TTL() {
        return KERBEROS_TTL;
    }

    private static final InternalOption<String> READ_LOG_MAX_SIZE =
        new InternalOption<>(
            "streampark.read-log.max-size",
            "1mb",
            String.class,
            "The maximum size of the default read log");

    public static InternalOption<String> READ_LOG_MAX_SIZE() {
        return READ_LOG_MAX_SIZE;
    }

    private static final InternalOption<String> SPRING_PROFILES_ACTIVE =
        new InternalOption<>(
            "spring.profiles.active", "h2", String.class, "Use the database type");

    public static InternalOption<String> SPRING_PROFILES_ACTIVE() {
        return SPRING_PROFILES_ACTIVE;
    }
}
