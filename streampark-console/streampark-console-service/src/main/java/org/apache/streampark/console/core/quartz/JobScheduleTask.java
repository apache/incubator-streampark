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

import org.apache.streampark.console.core.entity.Schedule;
import org.apache.streampark.console.core.enums.ScheduleState;
import org.apache.streampark.console.core.service.SchedulerService;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;

@Slf4j
public class JobScheduleTask extends QuartzJobBean {

  private final SchedulerService schedulerService;

  public JobScheduleTask(SchedulerService schedulerService) {
    this.schedulerService = schedulerService;
  }

  @Override
  protected void executeInternal(JobExecutionContext context) {
    JobDataMap dataMap = context.getJobDetail().getJobDataMap();

    int scheduleId = dataMap.getInt(QuartzTaskUtils.SCHEDULE_ID);

    Date scheduledFireTime = context.getScheduledFireTime();

    Date fireTime = context.getFireTime();

    log.info(
        "Scheduled fire time :{}, fire time :{}, process id :{}.",
        scheduledFireTime,
        fireTime,
        scheduleId);

    // query schedule
    Schedule schedule = schedulerService.querySchedule(scheduleId);
    if (schedule == null || ScheduleState.OFFLINE == schedule.getScheduleState()) {
      log.warn(
          "Job schedule does not exist in db or process schedule offline，delete schedule job in quartz, scheduleId:{}.",
          scheduleId);
      deleteJob(context, scheduleId);
      return;
    }
    // start flink job
    // .......
  }

  private void deleteJob(JobExecutionContext context, int scheduleId) {
    final Scheduler scheduler = context.getScheduler();
    JobKey jobKey = QuartzTaskUtils.getJobKey(scheduleId);
    try {
      if (scheduler.checkExists(jobKey)) {
        log.info("Try to delete job: {}.", scheduleId);
        scheduler.deleteJob(jobKey);
      }
    } catch (Exception e) {
      log.error("Failed to delete job: {}.", jobKey);
    }
  }
}
