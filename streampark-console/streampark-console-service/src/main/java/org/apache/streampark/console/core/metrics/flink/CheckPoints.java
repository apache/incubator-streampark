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
package org.apache.streampark.console.core.metrics.flink;

import org.apache.streampark.console.core.enums.CheckPointStatusEnum;
import org.apache.streampark.console.core.enums.CheckPointTypeEnum;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
public class CheckPoints implements Serializable {

    private List<CheckPoint> history;

    private Latest latest;

    @JsonIgnore
    public List<CheckPoint> getLatestCheckpoint() {
        if (Objects.isNull(latest)) {
            return Collections.emptyList();
        }
        return latest.getLatestCheckpoint();
    }

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

        public CheckPointStatusEnum getCheckPointStatus() {
            return CheckPointStatusEnum.valueOf(this.status);
        }

        public CheckPointTypeEnum getCheckPointType() {
            if ("CHECKPOINT".equals(this.checkpointType)) {
                return CheckPointTypeEnum.CHECKPOINT;
            }
            if ("SAVEPOINT".equals(this.checkpointType)) {
                return CheckPointTypeEnum.SAVEPOINT;
            }
            return CheckPointTypeEnum.SYNC_SAVEPOINT;
        }

        public String getPath() {
            return this.getExternalPath().replaceFirst("^hdfs:/[^/]", "hdfs:///");
        }
    }

    @Data
    public static class Latest implements Serializable {

        private CheckPoint completed;
        private CheckPoint savepoint;

        @JsonIgnore
        public List<CheckPoint> getLatestCheckpoint() {
            List<CheckPoint> checkPoints = new ArrayList<>();
            if (Objects.nonNull(completed)) {
                checkPoints.add(completed);
            }
            if (Objects.nonNull(savepoint)) {
                checkPoints.add(savepoint);
            }
            return checkPoints;
        }
    }
}
