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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JobsOverview {

    private List<JobDetail> jobs = Collections.emptyList();

    public List<JobDetail> jobs() {
        return jobs;
    }

    public static Optional<JobsOverview> parse(String json) {
        try {
            JobsOverview overview = FlinkRestJsonMapper.MAPPER.readValue(json, JobsOverview.class);
            if (overview.jobs == null) {
                overview.jobs = Collections.emptyList();
            }
            return Optional.of(overview);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
