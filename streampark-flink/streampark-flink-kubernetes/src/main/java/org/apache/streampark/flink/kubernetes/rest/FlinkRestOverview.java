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

package org.apache.streampark.flink.kubernetes.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Optional;

/** bean for response message of flink-rest/overview */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FlinkRestOverview {

    @JsonProperty("taskmanagers")
    private Integer taskManagers = 0;

    @JsonProperty("slots-total")
    private Integer slotsTotal = 0;

    @JsonProperty("slots-available")
    private Integer slotsAvailable = 0;

    @JsonProperty("jobs-running")
    private Integer jobsRunning = 0;

    @JsonProperty("jobs-finished")
    private Integer jobsFinished = 0;

    @JsonProperty("jobs-cancelled")
    private Integer jobsCancelled = 0;

    @JsonProperty("jobs-failed")
    private Integer jobsFailed = 0;

    @JsonProperty("flink-version")
    private String flinkVersion;

    public Integer taskManagers() {
        return taskManagers;
    }

    public Integer slotsTotal() {
        return slotsTotal;
    }

    public Integer slotsAvailable() {
        return slotsAvailable;
    }

    public Integer jobsRunning() {
        return jobsRunning;
    }

    public Integer jobsFinished() {
        return jobsFinished;
    }

    public Integer jobsCancelled() {
        return jobsCancelled;
    }

    public Integer jobsFailed() {
        return jobsFailed;
    }

    public String flinkVersion() {
        return flinkVersion;
    }

    public static Optional<FlinkRestOverview> parse(String json) {
        try {
            return Optional.of(FlinkRestJsonMapper.MAPPER.readValue(json, FlinkRestOverview.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
