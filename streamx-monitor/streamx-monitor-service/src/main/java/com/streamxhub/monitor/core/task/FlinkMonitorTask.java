/**
 * Copyright (c) 2019 The StreamX Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.streamxhub.monitor.core.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.monitor.base.utils.CommonUtil;
import com.streamxhub.monitor.core.entity.Application;
import com.streamxhub.monitor.core.enums.DeployState;
import com.streamxhub.monitor.core.enums.FlinkAppState;
import com.streamxhub.monitor.core.metrics.flink.JobsOverview;
import com.streamxhub.monitor.core.metrics.yarn.AppInfo;
import com.streamxhub.monitor.core.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class FlinkMonitorTask {

    @Autowired
    private ApplicationService applicationService;

    private final Map<Long, Long> cancelingMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000 * 5)
    public void run() {
        QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        //以下状态的不再监控...
        queryWrapper.notIn("state",
                FlinkAppState.DEPLOYING.getValue(),
                FlinkAppState.DEPLOYED.getValue(),
                FlinkAppState.CREATED.getValue(),
                FlinkAppState.FINISHED.getValue(),
                FlinkAppState.FAILED.getValue(),
                FlinkAppState.CANCELED.getValue(),
                FlinkAppState.LOST.getValue()
        );

        List<Application> appList = applicationService.list(queryWrapper);
        appList.forEach((application) -> {
            try {
                /**
                 * 1)到flink的restApi中查询状态
                 */
                JobsOverview jobsOverview = application.getJobsOverview();
                Integer deploy = application.getDeploy();
                application.setDeploy(null);

                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();
                if (optional.isPresent()) {
                    JobsOverview.Job job = optional.get();

                    FlinkAppState state = FlinkAppState.valueOf(job.getState());
                    long startTime = job.getStartTime();
                    long endTime = job.getEndTime() == -1 ? -1 : job.getEndTime();

                    if (!job.getId().equals(application.getJobId())) {
                        application.setJobId(job.getId());
                    }

                    if (application.getState() == FlinkAppState.STARTING.getValue() && state == FlinkAppState.RUNNING) {
                        /**
                         * 发布完重新启动后将"需重启"状态清空
                         */
                        if (DeployState.NEED_START.get() == deploy) {
                            application.setDeploy(DeployState.NONE.get());
                        }
                    }

                    if (!application.getState().equals(state.getValue())) {
                        application.setState(state.getValue());
                    }

                    if (application.getStartTime() == null) {
                        application.setStartTime(new Date(startTime));
                    } else if (startTime != application.getStartTime().getTime()) {
                        application.setStartTime(new Date(startTime));
                    }

                    if (endTime != -1) {
                        if (application.getEndTime() == null || endTime != application.getEndTime().getTime()) {
                            application.setEndTime(new Date(endTime));
                        }
                    }

                    application.setDuration(job.getDuration());

                    this.applicationService.updateMonitor(application);

                    if (state == FlinkAppState.CANCELLING) {
                        cancelingMap.put(application.getId(), application.getId());
                    }
                }
            } catch (ConnectException ex) {
                /**
                 * 上一次的状态为canceling,如在获取上次信息的时候flink restServer还未关闭为canceling,且本次如获取不到(flink restServer已关闭),则认为任务已经CANCELED
                 */
                if (cancelingMap.containsKey(application.getId())) {
                    application.setState(FlinkAppState.CANCELED.getValue());
                    applicationService.updateMonitor(application);
                    cancelingMap.remove(application.getId());
                } else {
                    try {
                        /**
                         * 2)到yarn的restApi中查询状态
                         */
                        AppInfo appInfo = application.getYarnAppInfo();
                        String state = appInfo.getApp().getFinalStatus();
                        FlinkAppState flinkAppState;
                        if ("KILLED".equals(state)) {
                            flinkAppState = FlinkAppState.CANCELED;
                            application.setEndTime(new Date());
                        } else {
                            flinkAppState = FlinkAppState.valueOf(state);
                        }
                        application.setState(flinkAppState.getValue());
                        applicationService.updateMonitor(application);
                    } catch (Exception e1) {
                        Serializable timeMillis = CommonUtil.localCache.remove(application.getId());
                        boolean flag = false;
                        if (timeMillis != null) {
                            long timeOut = 1 * 60 * 1000;
                            flag = System.currentTimeMillis() - (Long) timeMillis <= timeOut;
                        }
                        if (flag) {
                            application.setState(FlinkAppState.CANCELED.getValue());
                        } else {
                            /**s
                             * 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                             */
                            application.setState(FlinkAppState.LOST.getValue());
                            //TODO send msg or emails
                            e1.printStackTrace();
                        }
                        applicationService.updateMonitor(application);
                    }
                }
            } catch (IOException exception) {
                application.setState(FlinkAppState.FAILED.getValue());
                application.setEndTime(new Date());
                applicationService.updateMonitor(application);
            }
        });

    }


}
