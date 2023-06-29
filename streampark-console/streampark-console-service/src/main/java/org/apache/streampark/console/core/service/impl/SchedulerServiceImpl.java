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

package org.apache.streampark.console.core.service.impl;

import org.apache.streampark.common.util.DateUtils;
import org.apache.streampark.common.util.JsonUtils;
import org.apache.streampark.console.base.domain.Constant;
import org.apache.streampark.console.base.domain.RestRequest;
import org.apache.streampark.console.base.mybatis.pager.MybatisPager;
import org.apache.streampark.console.core.bean.ScheduleParam;
import org.apache.streampark.console.core.entity.Schedule;
import org.apache.streampark.console.core.enums.ScheduleState;
import org.apache.streampark.console.core.mapper.SchedulerMapper;
import org.apache.streampark.console.core.quartz.SchedulerApi;
import org.apache.streampark.console.core.service.SchedulerService;

import org.apache.commons.lang3.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

/** scheduler service impl */
@Service
@Slf4j
public class SchedulerServiceImpl extends ServiceImpl<SchedulerMapper, Schedule>
    implements SchedulerService {

  @Autowired private SchedulerMapper schedulerMapper;

  @Autowired private SchedulerApi schedulerApi;

  @Override
  @Transactional
  public boolean insertSchedule(Long appId, String schedule) {
    Schedule scheduleObj = new Schedule();
    Date now = new Date();

    scheduleObj.setAppId(appId);
    ScheduleParam scheduleParam = JsonUtils.read(schedule, ScheduleParam.class);
    if (DateUtils.differSec(scheduleParam.getStartTime(), scheduleParam.getEndTime()) == 0) {
      log.warn("The start time must not be the same as the end");
      return false;
    }
    if (scheduleParam.getStartTime().getTime() > scheduleParam.getEndTime().getTime()) {
      log.warn("The start time must smaller than end time");
      return false;
    }
    scheduleObj.setStartTime(scheduleParam.getStartTime());
    scheduleObj.setEndTime(scheduleParam.getEndTime());
    if (!org.quartz.CronExpression.isValidExpression(scheduleParam.getCrontab())) {
      log.error("{} verify failure", scheduleParam.getCrontab());
      return false;
    }
    scheduleObj.setCrontab(scheduleParam.getCrontab());
    scheduleObj.setTimezoneId(scheduleParam.getTimezoneId());
    scheduleObj.setCreateTime(now);
    scheduleObj.setUpdateTime(now);
    scheduleObj.setScheduleState(ScheduleState.OFFLINE);
    schedulerMapper.insert(scheduleObj);

    return false;
  }

  @Override
  @Transactional
  public boolean updateSchedule(Long appId, String scheduleExpression) {
    // check schedule exists
    Schedule schedule = schedulerMapper.selectById(appId);

    if (schedule == null) {
      return false;
    }

    Date now = new Date();

    if (!StringUtils.isEmpty(scheduleExpression)) {
      ScheduleParam scheduleParam = JsonUtils.read(scheduleExpression, ScheduleParam.class);
      if (scheduleParam == null) {
        return false;
      }
      if (DateUtils.differSec(scheduleParam.getStartTime(), scheduleParam.getEndTime()) == 0) {
        log.warn("The start time must not be the same as the end");
        return false;
      }
      if (scheduleParam.getStartTime().getTime() > scheduleParam.getEndTime().getTime()) {
        log.warn("The start time must smaller than end time");
        return false;
      }

      schedule.setStartTime(scheduleParam.getStartTime());
      schedule.setEndTime(scheduleParam.getEndTime());
      if (!org.quartz.CronExpression.isValidExpression(scheduleParam.getCrontab())) {
        return false;
      }
      schedule.setCrontab(scheduleParam.getCrontab());
      schedule.setTimezoneId(scheduleParam.getTimezoneId());
    }

    schedule.setUpdateTime(now);
    schedulerMapper.updateById(schedule);

    return true;
  }

  @Override
  @Transactional
  public void setScheduleState(Long appId, ScheduleState scheduleStatus) {
    // check schedule exists
    Schedule scheduleObj = schedulerMapper.selectById(appId);

    if (scheduleObj == null) {
      log.error("Schedule does not exist, scheduleId:{}.", appId);
    }
    // check schedule release state
    if (Objects.requireNonNull(scheduleObj).getScheduleState() == scheduleStatus) {
      log.warn(
          "Schedule state does not need to change due to schedule state is already {}, scheduleId:{}.",
          scheduleObj.getScheduleState().getDescp(),
          scheduleObj.getId());
    }
    scheduleObj.setScheduleState(scheduleStatus);

    schedulerMapper.updateById(scheduleObj);
    try {
      switch (scheduleStatus) {
        case ONLINE:
          log.info("Call master client set schedule online");
          setSchedule(scheduleObj);
          break;
        case OFFLINE:
          log.info("Call master client set schedule offline");
          deleteSchedule(scheduleObj.getId());
          break;
        default:
      }
    } catch (Exception e) {
    }
  }

  public void setSchedule(Schedule schedule) {
    log.info("set schedule, scheduleId: {}", schedule.getId());
    schedulerApi.insertOrUpdateScheduleTask(schedule);
  }

  @Override
  public void deleteSchedule(int scheduleId) {
    log.info("delete schedules of schedule id:{}", scheduleId);
    schedulerApi.deleteScheduleTask(scheduleId);
  }

  @Override
  public Map<String, Object> previewSchedule(String schedule) {
    return null;
  }

  @Override
  public IPage<Schedule> page(Schedule schedule, RestRequest request) {
    Page<Schedule> page =
        new MybatisPager<Schedule>().getPage(request, "option_time", Constant.ORDER_DESC);
    LambdaQueryWrapper<Schedule> queryWrapper =
        new LambdaQueryWrapper<Schedule>().eq(Schedule::getAppId, schedule.getAppId());
    return this.page(page, queryWrapper);
  }

  @Override
  public Schedule querySchedule(int appId) {
    return schedulerMapper.queryByAppId(appId);
  }
}
