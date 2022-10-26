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

package org.apache.streampark.archives;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

public class Task {
    @JsonProperty("total")
    private Integer total;
    @JsonProperty("created")
    private Integer created;
    @JsonProperty("scheduled")
    private Integer scheduled;
    @JsonProperty("deploying")
    private Integer deploying;

    @JsonProperty("running")
    private Integer running;

    @JsonProperty("finished")
    private Integer finished;

    @JsonProperty("canceling")
    private Integer canceling;

    @JsonProperty("canceled")
    private Integer canceled;

    @JsonProperty("failed")
    private Integer failed;

    @JsonProperty("reconciling")
    private Integer reconciling;
}

