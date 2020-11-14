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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class FlinkMonitorTask {

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

    private final Map<Long, Tracker> canceling = new ConcurrentHashMap<>();

    private long index;

    @Scheduled(fixedDelay = 1000 * 3)
    public void run() {
        ++index;
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

        appList.forEach((application) -> executor.execute(() -> {
            Serializable stop = CommonUtil.localCache.get(application.getId());
            StopFrom stopFrom = stop == null ? StopFrom.NONE : (StopFrom) stop;
            try {
                /**
                 * 1)到flink的restApi中查询状态
                 */
                JobsOverview jobsOverview = application.getJobsOverview();
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();
                if (optional.isPresent()) {
                    callBack(application, optional.get(), stopFrom);
                }
            } catch (ConnectException ex) {
                /**
                 * 上一次的状态为canceling(在获取上次信息的时候flink restServer还未关闭为canceling),且本次如获取不到状态(flink restServer已关闭),则认为任务已经CANCELED
                 */
                Tracker tracker = canceling.get(application.getId());
                if (tracker != null && tracker.isPrevious(index)) {
                    application.setState(FlinkAppState.CANCELED.getValue());
                    applicationService.updateMonitor(application);
                    canceling.remove(application.getId());
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
                        if (stopFrom == StopFrom.NONE) {
                            log.error("[StreamX] query jobsOverview from yarn,job was killed and stopNotFound,savePoint obsoleted!");
                            savePointService.obsolete(application.getId());
                            //过期
                            CommonUtil.localCache.put(application.getId(), StopFrom.EXPIRE);
                        }
                        application.setState(flinkAppState.getValue());
                        applicationService.updateMonitor(application);
                    } catch (Exception e1) {
                        /**s
                         * 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                         */
                        if (stopFrom == StopFrom.NONE) {
                            savePointService.obsolete(application.getId());
                            CommonUtil.localCache.put(application.getId(), StopFrom.EXPIRE);
                            application.setState(FlinkAppState.LOST.getValue());
                            //TODO send msg or emails
                            e1.printStackTrace();
                        } else {
                            application.setState(FlinkAppState.CANCELED.getValue());
                        }
                        applicationService.updateMonitor(application);
                    }
                }
            } catch (IOException exception) {
                log.error("[StreamX] query jobsOverview from restApi error,job failed,savePoint obsoleted!");
                exception.printStackTrace();
                if (stopFrom == StopFrom.NONE) {
                    savePointService.obsolete(application.getId());
                    CommonUtil.localCache.put(application.getId(), StopFrom.EXPIRE);
                }
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
        FlinkAppState previousState = FlinkAppState.of(application.getState());
        FlinkAppState currentState = FlinkAppState.valueOf(job.getState());
        /**
         * 1) application状态以restapi返回的状态为准
         */
        application.setState(currentState.getValue());

        /**
         * 2) application中记录的jobId和restapi返回的不一致,则以返回的jobId为准,更新application中记录的
         */
        if (!job.getId().equals(application.getJobId())) {
            application.setJobId(job.getId());
        }

        /**
         * 3) savePoint obsolete check
         */
        if (FlinkAppState.CANCELLING.equals(currentState)) {
            canceling.put(application.getId(), new Tracker(index, application.getId()));
        }

        /**
         * 当前从flink restAPI拿到最新的状态为cancelling,或者为CANCELED,且获取不到停止的时间
         * 此种情况为: 不是通过streamX发起的job的停止操作.此时的savepoint无法确认是否手动触发.则将savepoint设置为过期.
         * 在启动的时候如需要从savepoint恢复,则需要手动指定.
         */
        if (FlinkAppState.CANCELLING.equals(currentState) || FlinkAppState.CANCELED.equals(currentState)) {
            if (stopFrom == StopFrom.NONE) {
                log.info("[StreamX] monitor callback from restApi, job cancel is not form streamX,savePoint obsoleted!");
                savePointService.obsolete(application.getId());
                CommonUtil.localCache.put(application.getId(), StopFrom.EXPIRE);
            }
        }

        /**
         * 4) NEED_START check
         */
        if (FlinkAppState.RUNNING.equals(currentState) && FlinkAppState.STARTING.equals(previousState)) {
            /**
             * 发布完重新启动后将"需重启"状态清空
             */
            if (DeployState.NEED_START.get() == application.getDeploy()) {
                application.setDeploy(DeployState.NONE.get());
            }
        }

        /**
         * 5) duration
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
            return index - this.index <= 10;
        }
    }
}
