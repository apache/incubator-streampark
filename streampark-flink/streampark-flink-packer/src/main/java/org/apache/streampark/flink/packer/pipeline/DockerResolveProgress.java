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

package org.apache.streampark.flink.packer.pipeline;

/** cache storage for docker resolved progress */
public class DockerResolveProgress {

    private final DockerPullProgress pull;
    private final DockerBuildProgress build;
    private final DockerPushProgress push;

    public DockerResolveProgress(
                                 DockerPullProgress pull,
                                 DockerBuildProgress build,
                                 DockerPushProgress push) {
        this.pull = pull;
        this.build = build;
        this.push = push;
    }

    public DockerPullProgress getPull() {
        return pull;
    }

    public DockerBuildProgress getBuild() {
        return build;
    }

    public DockerPushProgress getPush() {
        return push;
    }
}
