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

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.listener.BuildImageCallbackListener;
import com.github.dockerjava.api.listener.PullImageCallbackListener;
import com.github.dockerjava.api.listener.PushImageCallbackListener;
import com.github.dockerjava.api.model.PullResponseItem;
import com.github.dockerjava.api.model.PushResponseItem;

import java.util.function.Consumer;
import java.util.function.Function;

public final class DockerClients {

    private DockerClients() {
    }

    public static BuildImageCallbackListener watchDockerBuildStep(Consumer<String> func) {
        return new BuildImageCallbackListener() {

            @Override
            public void watchBuildStep(String buildStepMsg) {
                func.accept(buildStepMsg);
            }
        };
    }

    public static PullImageCallbackListener watchDockerPullProcess(Consumer<PullResponseItem> func) {
        return new PullImageCallbackListener() {

            @Override
            public void watchPullProcess(PullResponseItem processDetail) {
                func.accept(processDetail);
            }
        };
    }

    public static PushImageCallbackListener watchDockerPushProcess(Consumer<PushResponseItem> func) {
        return new PushImageCallbackListener() {

            @Override
            public void watchPushProcess(PushResponseItem processDetail) {
                func.accept(processDetail);
            }
        };
    }

    public static <R> R usingDockerClient(Function<DockerClient, R> process, Function<Throwable, R> handleException) {
        try {
            DockerClient client = DockerRetriever.newDockerClient();
            try {
                return process.apply(client);
            } finally {
                client.close();
            }
        } catch (Throwable e) {
            return handleException.apply(e);
        }
    }
}
