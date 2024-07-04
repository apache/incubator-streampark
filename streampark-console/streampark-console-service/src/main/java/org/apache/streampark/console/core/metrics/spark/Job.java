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

package org.apache.streampark.console.core.metrics.spark;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import scala.collection.Map;

import java.io.Serializable;
import java.util.List;

@Data
public class JobsOverview implements Serializable {

  private Job[] jobs;

  @Data
  public static class Job implements Serializable {
    @JsonProperty("jobId")
    private Long id;

    private String name;

    private String submissionTime;

    private String completionTime;

    private List<Long> stageIds;

    private String status;

    private String numTasks;

    private String numActiveTasks;

    private String numCompletedTasks;

    private String numSkippedTasks;

    private String numFailedTasks;

    private String numKilledTasks;

    private String numCompletedIndices;

    private String numActiveStages;

    private String numCompletedStages;

    private String numSkippedStages;

    private String numFailedStages;

    private Map<String, Object> killedTasksSummary;
  }
}
