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

package org.apache.streampark.console.core.quartz;

import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.console.core.entity.Schedule;

import org.quartz.JobKey;

import java.util.HashMap;
import java.util.Map;

public final class QuartzTaskUtils {

  public static final String QUARTZ_JOB_PREFIX = "job";
  public static final String QUARTZ_JOB_GROUP_PREFIX = "jobgroup";
  public static final String UNDERLINE = "_";
  public static final String SCHEDULE_ID = "scheduleId";
  public static final String SCHEDULE = "schedule";

  /**
   * @param schedulerId scheduler id
   * @return quartz job name
   */
  public static JobKey getJobKey(int schedulerId) {
    String jobName = QUARTZ_JOB_PREFIX + UNDERLINE + schedulerId;
    String jobGroup = QUARTZ_JOB_GROUP_PREFIX + UNDERLINE;
    return new JobKey(jobName, jobGroup);
  }

  public static Map<String, Object> buildDataMap(Schedule schedule) {
    Map<String, Object> dataMap = new HashMap<>(8);
    dataMap.put(SCHEDULE_ID, schedule.getId());
    dataMap.put(SCHEDULE, JsonUtils.Marshal(schedule).toJson());

    return dataMap;
  }
}
