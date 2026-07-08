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

package org.apache.streampark.flink.packer.docker;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.common.util.Utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.HackDockerClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;
import java.time.Duration;

public final class DockerRetriever {

    private static final DockerClientConfig DOCKER_CLIENT_CONF =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    private static final ApacheDockerHttpClient.Builder DOCKER_HTTP_CLIENT_BUILDER =
        new ApacheDockerHttpClient.Builder()
            .dockerHost(DOCKER_CLIENT_CONF.getDockerHost())
            .sslConfig(DOCKER_CLIENT_CONF.getSSLConfig())
            .maxConnections(InternalConfigHolder.get(CommonConfig.DOCKER_MAX_CONNECTIONS))
            .connectionTimeout(
                Duration.ofSeconds(InternalConfigHolder.get(CommonConfig.DOCKER_CONNECTION_TIMEOUT_SEC)))
            .responseTimeout(Duration.ofSeconds(InternalConfigHolder.get(CommonConfig.DOCKER_RESPONSE_TIMEOUT_SEC)));

    private DockerRetriever() {
    }

    public static DockerClient newDockerClient() {
        setDockerHost();
        return HackDockerClient.getInstance(DOCKER_CLIENT_CONF, DOCKER_HTTP_CLIENT_BUILDER.build());
    }

    private static void setDockerHost() {
        String dockerHost = InternalConfigHolder.get(CommonConfig.DOCKER_HOST);
        if (Utils.isNotEmpty(dockerHost)) {
            DOCKER_HTTP_CLIENT_BUILDER.dockerHost(URI.create(dockerHost));
        }
    }
}
