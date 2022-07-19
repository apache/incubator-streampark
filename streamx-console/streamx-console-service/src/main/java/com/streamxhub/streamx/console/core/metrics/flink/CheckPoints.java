/*
 * Copyright (c) 2019 The StreamX Project
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.streamxhub.streamx.console.core.metrics.flink;

import com.streamxhub.streamx.console.core.enums.CheckPointStatus;
import com.streamxhub.streamx.console.core.enums.CheckPointType;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author benjobs
 */
@Data
public class CheckPoints implements Serializable {

    private List<CheckPoint> history;

    private Latest latest;

    @Data
    public static class CheckPoint implements Serializable {
        private Long id;
        private String status;

        @JsonProperty("external_path")
        private String externalPath;

        @JsonProperty("is_savepoint")
        private Boolean isSavepoint;

        @JsonProperty("latest_ack_timestamp")
        private Long latestAckTimestamp;

        @JsonProperty("checkpoint_type")
        private String checkpointType;

        @JsonProperty("trigger_timestamp")
        private Long triggerTimestamp;

        @JsonProperty("state_size")
        private Long stateSize;

        @JsonProperty("end_to_end_duration")
        private Long endToEndDuration;

        private Boolean discarded;

        public CheckPointStatus getCheckPointStatus() {
            return CheckPointStatus.valueOf(this.status);
        }

        public CheckPointType getCheckPointType() {
            if ("CHECKPOINT".equals(this.checkpointType)) {
                return CheckPointType.CHECKPOINT;
            } else if ("SAVEPOINT".equals(this.checkpointType)) {
                return CheckPointType.SAVEPOINT;
            }
            return CheckPointType.SYNC_SAVEPOINT;
        }

        public String getPath() {
            return this.getExternalPath().replaceFirst("^hdfs:/[^/]", "hdfs:///");
        }
    }

    @Data
    public static class Latest implements Serializable {
        private CheckPoint completed;
    }

}
