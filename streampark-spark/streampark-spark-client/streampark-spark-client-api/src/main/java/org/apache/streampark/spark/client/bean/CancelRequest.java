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

package org.apache.streampark.spark.client.bean;

import org.apache.streampark.common.conf.SparkVersion;
import org.apache.streampark.common.enums.SparkDeployMode;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Map;

/** Request to cancel a running Spark application. */
public class CancelRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final SparkVersion sparkVersion;
    private final SparkDeployMode deployMode;
    @Nullable
    private final Map<String, String> properties;
    private final String appId;

    public CancelRequest(
                         long id,
                         SparkVersion sparkVersion,
                         SparkDeployMode deployMode,
                         @Nullable Map<String, String> properties,
                         String appId) {
        this.id = id;
        this.sparkVersion = sparkVersion;
        this.deployMode = deployMode;
        this.properties = properties;
        this.appId = appId;
    }

    public long id() {
        return id;
    }

    public long getId() {
        return id;
    }

    public SparkVersion sparkVersion() {
        return sparkVersion;
    }

    public SparkVersion getSparkVersion() {
        return sparkVersion;
    }

    public SparkDeployMode deployMode() {
        return deployMode;
    }

    public SparkDeployMode getDeployMode() {
        return deployMode;
    }

    @Nullable
    public Map<String, String> properties() {
        return properties;
    }

    @Nullable
    public Map<String, String> getProperties() {
        return properties;
    }

    public String appId() {
        return appId;
    }

    public String getAppId() {
        return appId;
    }
}
