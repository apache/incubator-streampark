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

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.SparkJobType;

import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/** Application-level fields for a Spark submit request. */
public class SparkSubmitApplicationSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private final SparkJobType jobType;
    private final long id;
    private final String appName;
    private final String mainClass;
    private final String appConf;
    private final Map<String, String> appProperties;
    private final List<String> appArgs;
    private final ApplicationType applicationType;
    @Nullable
    private final String hadoopUser;

    public SparkSubmitApplicationSpec(
                                      SparkJobType jobType,
                                      long id,
                                      String appName,
                                      String mainClass,
                                      String appConf,
                                      Map<String, String> appProperties,
                                      List<String> appArgs,
                                      ApplicationType applicationType,
                                      @Nullable String hadoopUser) {
        this.jobType = jobType;
        this.id = id;
        this.appName = appName;
        this.mainClass = mainClass;
        this.appConf = appConf;
        this.appProperties = appProperties;
        this.appArgs = appArgs;
        this.applicationType = applicationType;
        this.hadoopUser = hadoopUser;
    }

    public SparkJobType jobType() {
        return jobType;
    }

    public long id() {
        return id;
    }

    public String appName() {
        return appName;
    }

    public String mainClass() {
        return mainClass;
    }

    public String appConf() {
        return appConf;
    }

    public Map<String, String> appProperties() {
        return appProperties;
    }

    public List<String> appArgs() {
        return appArgs;
    }

    public ApplicationType applicationType() {
        return applicationType;
    }

    @Nullable
    public String hadoopUser() {
        return hadoopUser;
    }
}
