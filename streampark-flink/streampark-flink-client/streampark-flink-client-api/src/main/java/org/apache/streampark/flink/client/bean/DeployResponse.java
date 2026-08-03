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

package org.apache.streampark.flink.client.bean;

import javax.annotation.Nullable;

import java.io.Serializable;

public class DeployResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    @Nullable
    private final String address;
    @Nullable
    private final String clusterId;
    @Nullable
    private final Throwable error;

    public DeployResponse(@Nullable String address, @Nullable String clusterId) {
        this(address, clusterId, null);
    }

    public DeployResponse(@Nullable Throwable error) {
        this(null, null, error);
    }

    public DeployResponse(
                          @Nullable String address,
                          @Nullable String clusterId,
                          @Nullable Throwable error) {
        this.address = address;
        this.clusterId = clusterId;
        this.error = error;
    }

    @Nullable
    public String address() {
        return address;
    }

    @Nullable
    public String clusterId() {
        return clusterId;
    }

    @Nullable
    public Throwable error() {
        return error;
    }
}
