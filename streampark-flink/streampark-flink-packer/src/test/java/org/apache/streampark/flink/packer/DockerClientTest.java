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

package org.apache.streampark.flink.packer;

import org.apache.streampark.common.conf.CommonConfig;
import org.apache.streampark.common.conf.InternalConfigHolder;
import org.apache.streampark.flink.packer.docker.DockerImageExist;
import org.apache.streampark.flink.packer.docker.DockerRetriever;
import org.apache.streampark.flink.packer.docker.DockerUtils;

import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerClientTest {

    private static final String TEST_IMAGE = "flink:1.18.1-scala_2.12-java8";

    private static boolean dockerAvailable;

    @BeforeAll
    static void prepareDocker() {
        String dockerHost = System.getenv("DOCKER_HOST");
        if (dockerHost == null) {
            dockerHost = "/var/run/docker.sock";
        }
        dockerAvailable = Files.exists(Paths.get(dockerHost.replace("unix://", "")));
        if (!dockerAvailable) {
            return;
        }
        DockerUtils.usingDockerClient(
            client -> {
                try {
                    client.inspectImageCmd(TEST_IMAGE).exec();
                } catch (NotFoundException e) {
                    try {
                        client.pullImageCmd("hello-world:latest")
                            .exec(new PullImageResultCallback())
                            .awaitCompletion(2, TimeUnit.MINUTES);
                    } catch (InterruptedException interrupted) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(
                            "Interrupted while pulling docker test image: hello-world:latest",
                            interrupted);
                    }
                    client.tagImageCmd("hello-world:latest", "flink", "1.18.1-scala_2.12-java8")
                        .exec();
                }
                return null;
            },
            err -> {
                throw new RuntimeException("Failed to prepare docker test image: " + TEST_IMAGE, err);
            });
    }

    @Test
    void dockerClientConfigBuilder() {
        ApacheDockerHttpClient.Builder dockerHttpClientBuilder =
            new ApacheDockerHttpClient.Builder()
                .dockerHost(DockerRetriever.dockerClientConf.getDockerHost())
                .sslConfig(DockerRetriever.dockerClientConf.getSSLConfig())
                .maxConnections(InternalConfigHolder.get(CommonConfig.DOCKER_MAX_CONNECTIONS()))
                .connectionTimeout(
                    Duration.ofSeconds(
                        InternalConfigHolder.get(CommonConfig.DOCKER_CONNECTION_TIMEOUT_SEC())))
                .responseTimeout(
                    Duration.ofSeconds(
                        InternalConfigHolder.get(CommonConfig.DOCKER_RESPONSE_TIMEOUT_SEC())));
        assertNotNull(dockerHttpClientBuilder);
    }

    @Test
    void returnTrueIfImageExists() {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available");
        DockerImageExist dockerImageExist = new DockerImageExist();
        assertTrue(dockerImageExist.doesDockerImageExist(TEST_IMAGE));
    }

    @Test
    void returnFalseIfImageDoesNotExist() {
        Assumptions.assumeTrue(dockerAvailable, "Docker daemon is not available");
        DockerImageExist dockerImageExist = new DockerImageExist();
        String imageName = "flink:1.18.1-scala_2.12-java8-fail";
        assertFalse(dockerImageExist.doesDockerImageExist(imageName));
    }
}
