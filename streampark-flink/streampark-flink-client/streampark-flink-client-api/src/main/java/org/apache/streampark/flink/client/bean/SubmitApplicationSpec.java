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

import org.apache.streampark.common.enums.ApplicationType;
import org.apache.streampark.common.enums.FlinkJobType;
import org.apache.streampark.common.enums.FlinkRestoreMode;

import java.io.Serializable;

/** Application metadata for Flink submit requests. */
public final class SubmitApplicationSpec implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String flinkYaml;
    private final FlinkJobType jobType;
    private final long id;
    private final String jobId;
    private final String appName;
    private final String appConf;
    private final ApplicationType applicationType;
    private final String savePoint;
    private final FlinkRestoreMode restoreMode;
    private final String args;

    private SubmitApplicationSpec(Builder builder) {
        this.flinkYaml = builder.flinkYaml;
        this.jobType = builder.jobType;
        this.id = builder.id;
        this.jobId = builder.jobId;
        this.appName = builder.appName;
        this.appConf = builder.appConf;
        this.applicationType = builder.applicationType;
        this.savePoint = builder.savePoint;
        this.restoreMode = builder.restoreMode;
        this.args = builder.args;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String flinkYaml() {
        return flinkYaml;
    }

    public FlinkJobType jobType() {
        return jobType;
    }

    public long id() {
        return id;
    }

    public String jobId() {
        return jobId;
    }

    public String appName() {
        return appName;
    }

    public String appConf() {
        return appConf;
    }

    public ApplicationType applicationType() {
        return applicationType;
    }

    public String savePoint() {
        return savePoint;
    }

    public FlinkRestoreMode restoreMode() {
        return restoreMode;
    }

    public String args() {
        return args;
    }

    public static final class Builder {

        private String flinkYaml;
        private FlinkJobType jobType;
        private long id;
        private String jobId;
        private String appName;
        private String appConf;
        private ApplicationType applicationType;
        private String savePoint;
        private FlinkRestoreMode restoreMode;
        private String args;

        public Builder flinkYaml(String flinkYaml) {
            this.flinkYaml = flinkYaml;
            return this;
        }

        public Builder jobType(FlinkJobType jobType) {
            this.jobType = jobType;
            return this;
        }

        public Builder id(long id) {
            this.id = id;
            return this;
        }

        public Builder jobId(String jobId) {
            this.jobId = jobId;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder appConf(String appConf) {
            this.appConf = appConf;
            return this;
        }

        public Builder applicationType(ApplicationType applicationType) {
            this.applicationType = applicationType;
            return this;
        }

        public Builder savePoint(String savePoint) {
            this.savePoint = savePoint;
            return this;
        }

        public Builder restoreMode(FlinkRestoreMode restoreMode) {
            this.restoreMode = restoreMode;
            return this;
        }

        public Builder args(String args) {
            this.args = args;
            return this;
        }

        public SubmitApplicationSpec build() {
            return new SubmitApplicationSpec(this);
        }
    }
}
