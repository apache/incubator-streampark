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
package com.streamxhub.console.core.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.streamxhub.common.util.ThreadUtils;
import com.streamxhub.console.base.utils.CommonUtil;
import com.streamxhub.console.core.entity.Application;
import com.streamxhub.console.core.enums.DeployState;
import com.streamxhub.console.core.enums.FlinkAppState;
import com.streamxhub.console.core.enums.StopFrom;
import com.streamxhub.console.core.metrics.flink.JobsOverview;
import com.streamxhub.console.core.metrics.yarn.AppInfo;
import com.streamxhub.console.core.service.ApplicationService;
import com.streamxhub.console.core.service.SavePointService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;
import java.net.ConnectException;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class FlinkMonitorTask {

    private final Map<Long, Tracker> canceling = new ConcurrentHashMap<>();

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private SavePointService savePointService;

    private ThreadFactory threadFactory = ThreadUtils.threadFactory("flink-monitor-executor");

    private ExecutorService executor = new ThreadPoolExecutor(
            Math.max(Runtime.getRuntime().availableProcessors() / 4, 2),
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            threadFactory
    );

    private AtomicLong atomicIndex = new AtomicLong(0);

    @Scheduled(fixedDelay = 1000 * 5)
    public void run() {
        Long index = atomicIndex.incrementAndGet();
        QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tracking", 1);
        applicationService.list(queryWrapper).forEach((application) -> executor.execute(() -> {
            StopFrom stopFrom = (StopFrom) CommonUtil.jvmCache.getOrDefault(application.getId(), StopFrom.NONE);
            try {
                /**
                 * 1)到flink的restApi中查询状态
                 */
                JobsOverview jobsOverview = application.getJobsOverview();
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();
                if (optional.isPresent()) {
                    callBack(application, optional.get(), stopFrom);
                }
            } catch (ConnectException exp) {
                /**
                 * 上一次的状态为canceling(在获取上次信息的时候flink restServer还未关闭为canceling),且本次如获取不到状态(flink restServer已关闭),则认为任务已经CANCELED
                 */
                log.info("[StreamX] flinkMonitorTask get state from flink restApi error {}", exp);
                Tracker tracker = canceling.remove(application.getId());
                if (tracker != null && tracker.isPrevious(index)) {
                    log.info("[StreamX] flinkMonitorTask previous state was canceling.");
                    if (StopFrom.NONE.equals(stopFrom)) {
                        log.error("[StreamX] flinkMonitorTask query previous state was canceling and stopFrom NotFound,savePoint obsoleted!");
                        CommonUtil.jvmCache.remove(application.getId());
                        savePointService.obsolete(application.getId());
                    }
                    application.setState(FlinkAppState.CANCELED.getValue());
                    applicationService.updateMonitor(application);
                } else {
                    log.info("[StreamX] flinkMonitorTask previous state was not canceling.");
                    try {
                        /**
                         * 2)到yarn的restApi中查询状态
                         */
                        AppInfo appInfo = application.getYarnAppInfo();
                        String state = appInfo.getApp().getFinalStatus();
                        FlinkAppState flinkAppState = FlinkAppState.valueOf(state);
                        if (FlinkAppState.KILLED.equals(flinkAppState)) {
                            CommonUtil.jvmCache.remove(application.getId());
                            if (StopFrom.NONE.equals(stopFrom)) {
                                log.error("[StreamX] flinkMonitorTask query jobsOverview from yarn,job was killed and stopFrom NotFound,savePoint obsoleted!");
                                savePointService.obsolete(application.getId());
                            }
                            flinkAppState = FlinkAppState.CANCELED;
                            application.setEndTime(new Date());
                        }
                        application.setState(flinkAppState.getValue());
                        applicationService.updateMonitor(application);
                    } catch (Exception e) {
                        /**s
                         * 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                         */
                        CommonUtil.jvmCache.remove(application.getId());
                        if (StopFrom.NONE.equals(stopFrom)) {
                            log.error("[StreamX] flinkMonitorTask query jobsOverview from restapi and yarn all error and stopFrom NotFound,savePoint obsoleted! {}", e);
                            savePointService.obsolete(application.getId());
                            application.setState(FlinkAppState.LOST.getValue());
                            //TODO send msg or emails
                        } else {
                            application.setState(FlinkAppState.CANCELED.getValue());
                        }
                        applicationService.updateMonitor(application);
                    }
                }
            } catch (IOException exception) {
                log.error("[StreamX] flinkMonitorTask query jobsOverview from restApi error,job failed,savePoint obsoleted! {}", exception);
                CommonUtil.jvmCache.remove(application.getId());
                savePointService.obsolete(application.getId());
                application.setState(FlinkAppState.FAILED.getValue());
                application.setEndTime(new Date());
                applicationService.updateMonitor(application);
            }
        }));
    }

    /**
     * 从flink restapi成功拿到当前任务的运行状态信息...
     *
     * @param application
     * @param job
     */
    private void callBack(Application application, JobsOverview.Job job, StopFrom stopFrom) {
        FlinkAppState currentState = FlinkAppState.valueOf(job.getState());
        /**
         * 1) savePoint obsolete check and NEED_START check
         */
        switch (currentState) {
            case CANCELLING:
                canceling.put(application.getId(), new Tracker(atomicIndex.get(), application.getId()));
                break;
            case CANCELED:
                log.info("[StreamX] flinkMonitorTask application state {}, delete stopFrom!", currentState.name());
                CommonUtil.jvmCache.remove(application.getId());
                if (StopFrom.NONE.equals(stopFrom)) {
                    log.info("[StreamX] flinkMonitorTask monitor callback from restApi, job cancel is not form streamX,savePoint obsoleted!");
                    savePointService.obsolete(application.getId());
                }
                break;
            case RUNNING:
                FlinkAppState previousState = FlinkAppState.of(application.getState());
                if (FlinkAppState.STARTING.equals(previousState)) {
                    /**
                     * 发布完重新启动后将"需重启"状态清空
                     */
                    if (DeployState.NEED_START.get() == application.getDeploy()) {
                        application.setDeploy(DeployState.NONE.get());
                    }
                }
                break;
            default:
                break;
        }

        /**
         * 2) duration
         */
        long startTime = job.getStartTime();
        long endTime = job.getEndTime() == -1 ? -1 : job.getEndTime();
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

        /**
         * 3) application状态以restapi返回的状态为准
         */
        application.setState(currentState.getValue());

        /**
         * 4) jobId以restapi返回的状态为准
         */
        application.setJobId(job.getId());

        this.applicationService.updateMonitor(application);
    }

    @Getter
    class Tracker implements Serializable {
        private long index;
        private long appId;

        public Tracker(long index, long appId) {
            this.index = index;
            this.appId = appId;
        }

        public boolean isPrevious(long index) {
            return index - this.index == 1;
        }
    }
}
