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

import org.apache.streampark.common.configuration.Configuration;
import org.apache.streampark.common.configuration.GlobalConfiguration;
import org.apache.streampark.common.configuration.option.DockerOptions;
import org.apache.streampark.common.util.Utils;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.HackDockerClient;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;

import java.net.URI;

/** Docker client factory. */
public final class DockerRetriever {

    public static final DockerClientConfig dockerClientConf =
        DefaultDockerClientConfig.createDefaultConfigBuilder().build();

    private DockerRetriever() {
    }

    /** Returns a new Docker client built from one consistent configuration snapshot. */
    public static DockerClient newDockerClient() {
        Configuration configuration = GlobalConfiguration.current();
        ApacheDockerHttpClient.Builder builder =
            new ApacheDockerHttpClient.Builder()
                .dockerHost(dockerClientConf.getDockerHost())
                .sslConfig(dockerClientConf.getSSLConfig())
                .maxConnections(configuration.get(DockerOptions.MAX_CONNECTIONS))
                .connectionTimeout(configuration.get(DockerOptions.CONNECTION_TIMEOUT))
                .responseTimeout(configuration.get(DockerOptions.RESPONSE_TIMEOUT));
        String dockerHost = configuration.get(DockerOptions.HOST);
        if (Utils.isNotEmpty(dockerHost)) {
            builder.dockerHost(URI.create(dockerHost));
        }
        return HackDockerClient.getInstance(dockerClientConf, builder.build());
    }
}
