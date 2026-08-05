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

import com.github.dockerjava.api.model.AuthConfig;

import javax.annotation.Nullable;

/** Authentication Configuration of Remote Docker Register */
public final class DockerConf {

    @Nullable
    private final String registerAddress;
    private final String imageNamespace;
    private final String registerUsername;
    private final String registerPassword;

    public DockerConf(
                      @Nullable String registerAddress,
                      String imageNamespace,
                      String registerUsername,
                      String registerPassword) {
        this.registerAddress = registerAddress;
        this.imageNamespace = imageNamespace;
        this.registerUsername = registerUsername;
        this.registerPassword = registerPassword;
    }

    @Nullable
    public String registerAddress() {
        return registerAddress;
    }

    public String imageNamespace() {
        return imageNamespace;
    }

    public String registerUsername() {
        return registerUsername;
    }

    public String registerPassword() {
        return registerPassword;
    }

    /** covert to com.github.docker.java.api.model.AuthConfig */
    public AuthConfig toAuthConf() {
        return new AuthConfig()
            .withRegistryAddress(registerAddress)
            .withUsername(registerUsername)
            .withPassword(registerPassword);
    }

    public static DockerConf of(
                                @Nullable String registerAddress,
                                String imageNameSpace,
                                String registerUsername,
                                String registerPassword) {
        return new DockerConf(registerAddress, imageNameSpace, registerUsername, registerPassword);
    }
}
