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

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.console.core.entity.Schedule;

import com.google.common.base.Strings;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
public class QuartzExecutors implements SchedulerApi {

  @Autowired Scheduler scheduler;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  @SneakyThrows
  @Override
  public void start() {
    try {
      scheduler.start();
    } catch (Exception e) {
      throw new SchedulerException("Failed to start quartz scheduler.", e);
    }
  }

  @SneakyThrows
  @Override
  public void insertOrUpdateScheduleTask(Schedule schedule) {
    JobKey jobKey = QuartzTaskUtils.getJobKey(schedule.getId());
    Map<String, Object> jobDataMap = QuartzTaskUtils.buildDataMap(schedule);
    String cronExpression = schedule.getCrontab();
    String timezoneId = schedule.getTimezoneId();
    Date startDate = DateUtils.transformTimezoneDate(schedule.getStartTime(), timezoneId);
    Date endDate = DateUtils.transformTimezoneDate(schedule.getEndTime(), timezoneId);

    lock.writeLock().lock();
    try {

      JobDetail jobDetail;
      if (scheduler.checkExists(jobKey)) {

        jobDetail = scheduler.getJobDetail(jobKey);
        jobDetail.getJobDataMap().putAll(jobDataMap);
      } else {
        jobDetail = newJob(JobScheduleTask.class).withIdentity(jobKey).build();

        jobDetail.getJobDataMap().putAll(jobDataMap);

        scheduler.addJob(jobDetail, false, true);

        log.info("Add job, job name: {}, group name: {}.", jobKey.getName(), jobKey.getGroup());
      }

      TriggerKey triggerKey = new TriggerKey(jobKey.getName(), jobKey.getGroup());

      CronTrigger cronTrigger =
          newTrigger()
              .withIdentity(triggerKey)
              .startAt(startDate)
              .endAt(endDate)
              .withSchedule(
                  cronSchedule(cronExpression)
                      .withMisfireHandlingInstructionDoNothing()
                      .inTimeZone(DateUtils.getTimezone(timezoneId).get()))
              .forJob(jobDetail)
              .build();

      if (scheduler.checkExists(triggerKey)) {
        CronTrigger oldCronTrigger = (CronTrigger) scheduler.getTrigger(triggerKey);
        String oldCronExpression = oldCronTrigger.getCronExpression();

        if (!Strings.nullToEmpty(cronExpression)
            .equalsIgnoreCase(Strings.nullToEmpty(oldCronExpression))) {
          scheduler.rescheduleJob(triggerKey, cronTrigger);
          log.info(
              "Reschedule job trigger, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
              triggerKey.getName(),
              triggerKey.getGroup(),
              cronExpression,
              startDate,
              endDate);
        }
      } else {
        scheduler.scheduleJob(cronTrigger);
        log.info(
            "Schedule job trigger, triggerName: {}, triggerGroupName: {}, cronExpression: {}, startDate: {}, endDate: {}",
            triggerKey.getName(),
            triggerKey.getGroup(),
            cronExpression,
            startDate,
            endDate);
      }
    } catch (Exception e) {
      throw new SchedulerException("Add schedule job failed.", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @SneakyThrows
  @Override
  public void deleteScheduleTask(int scheduleId) {
    JobKey jobKey = QuartzTaskUtils.getJobKey(scheduleId);
    try {
      if (scheduler.checkExists(jobKey)) {
        log.info("Try to delete scheduler task, schedulerId: {}.", scheduleId);
        scheduler.deleteJob(jobKey);
      }
    } catch (Exception e) {
      log.error("Failed to delete scheduler task, schedulerId: {}.", scheduleId, e);
      throw new SchedulerException("Failed to delete scheduler task.");
    }
  }

  @Override
  public void close() throws Exception {
    try {
      scheduler.shutdown();
    } catch (org.quartz.SchedulerException e) {
      throw new SchedulerException("Failed to shutdown scheduler.", e);
    }
  }
}
