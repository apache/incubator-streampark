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
package com.streamxhub.flink.monitor.core.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.flink.monitor.core.entity.Application;
import com.streamxhub.flink.monitor.core.enums.AppState;
import com.streamxhub.flink.monitor.core.metrics.flink.JobsOverview;
import com.streamxhub.flink.monitor.core.metrics.yarn.AppInfo;
import com.streamxhub.flink.monitor.core.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class FlinkMonitorTask {

    @Autowired
    private ApplicationService applicationService;

    private final Map<Long, AppState> jobStateMap = new ConcurrentHashMap<>();

    @Scheduled(fixedDelay = 1000 * 2)
    public void run() {
        QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        //以下状态的不再监控...
        queryWrapper.notIn("state",
                AppState.CREATED.getValue(),
                AppState.FINISHED.getValue(),
                AppState.FAILED.getValue(),
                AppState.CANCELED.getValue(),
                AppState.LOST.getValue()
        );
        List<Application> appList = applicationService.list(queryWrapper);
        appList.forEach((application) -> {
            try {
                /**
                 * 1)到flink的restApi中查询状态
                 */
                JobsOverview jobsOverview = application.getJobsOverview();
                /**
                 * 注意:yarnName是唯一的,不能重复...
                 */
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().filter((x) -> x.getName().equals(application.getYarnName())).findFirst();
                assert optional.isPresent();
                JobsOverview.Job job = optional.get();

                if (application.getJobId() == null) {
                    application.setJobId(job.getId());
                    applicationService.updateById(application);
                }
                AppState state = AppState.valueOf(job.getState());
                AppState preState = jobStateMap.get(application.getId());
                if (!state.equals(preState)) {
                    application.setState(state.getValue());
                    applicationService.updateById(application);
                    jobStateMap.put(application.getId(), state);
                }
                if (state == AppState.FAILED || state == AppState.FINISHED || state == AppState.CANCELED) {
                    jobStateMap.remove(application.getId());
                }
            } catch (Exception e) {
                if (e instanceof ConnectException) {
                    try {
                        /**
                         * 2)到yarn的restApi中查询状态
                         */
                        AppInfo appInfo = application.getYarnAppInfo();
                        String state = appInfo.getApp().getFinalStatus();
                        AppState appState = null;
                        if("KILLED".equals(state)) {
                            appState = AppState.CANCELED;
                        }else {
                            appState = AppState.valueOf(state);
                        }
                        application.setState(appState.getValue());
                        applicationService.updateById(application);
                    } catch (Exception e1) {
                        /**s
                         * 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                         */
                        application.setState(AppState.LOST.getValue());
                        applicationService.updateById(application);
                        jobStateMap.remove(application.getId());
                        //TODO send msg or emails
                        e1.printStackTrace();
                    }
                }
            }
        });

    }


}
