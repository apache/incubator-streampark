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

package com.github.dockerjava.core;

import com.github.dockerjava.core.command.HackBuildImageCmd;
import com.github.dockerjava.core.command.HackPullImageCmd;
import com.github.dockerjava.core.command.HackPushImageCmd;
import com.github.dockerjava.transport.DockerHttpClient;

/** Enhancement of DockerClient to provide custom api with a hacked way. */
public class HackDockerClient extends DockerClientImpl {

    private final DockerClientConfig dockerClientConfig;

    public HackDockerClient(DockerClientConfig dockerClientConfig) {
        super(dockerClientConfig);
        this.dockerClientConfig = dockerClientConfig;
    }

    public static HackDockerClient getInstance(
                                               DockerClientConfig dockerClientConfig,
                                               DockerHttpClient dockerHttpClient) {
        HackDockerClient client = new HackDockerClient(dockerClientConfig);
        client.dockerCmdExecFactory = new DefaultDockerCmdExecFactory(dockerHttpClient,
            dockerClientConfig.getObjectMapper());
        ((DockerClientConfigAware) client.dockerCmdExecFactory).init(dockerClientConfig);
        return client;
    }

    @Override
    public HackBuildImageCmd buildImageCmd() {
        return new HackBuildImageCmd(dockerCmdExecFactory.createBuildImageCmdExec());
    }

    @Override
    public HackPullImageCmd pullImageCmd(String repository) {
        return new HackPullImageCmd(
            dockerCmdExecFactory.createPullImageCmdExec(),
            dockerClientConfig.effectiveAuthConfig(repository),
            repository);
    }

    @Override
    public HackPushImageCmd pushImageCmd(String name) {
        return new HackPushImageCmd(
            dockerCmdExecFactory.createPushImageCmdExec(),
            dockerClientConfig.effectiveAuthConfig(name),
            name);
    }
}
