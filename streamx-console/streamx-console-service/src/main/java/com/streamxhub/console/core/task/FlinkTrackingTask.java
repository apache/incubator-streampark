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
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.streamxhub.common.util.ThreadUtils;
import com.streamxhub.console.core.entity.Application;
import com.streamxhub.console.core.enums.CurrentAction;
import com.streamxhub.console.core.enums.DeployState;
import com.streamxhub.console.core.enums.FlinkAppState;
import com.streamxhub.console.core.enums.StopFrom;
import com.streamxhub.console.core.metrics.flink.JobsOverview;
import com.streamxhub.console.core.metrics.yarn.AppInfo;
import com.streamxhub.console.core.service.ApplicationService;
import com.streamxhub.console.core.service.SavePointService;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
public class FlinkTrackingTask {

    private static Cache<Long, Byte> trackingAppId = null;

    private static Cache<Long, Application> trackingAppCache = null;

    private static Map<Long, StopFrom> stopAppMap = new ConcurrentHashMap<>();

    private final Map<Long, Tracker> canceling = new ConcurrentHashMap<>();

    private ExecutorService executor = new ThreadPoolExecutor(
            Math.max(Runtime.getRuntime().availableProcessors() / 4, 2),
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            ThreadUtils.threadFactory("flink-monitor-executor")
    );

    private static ApplicationService applicationService;

    @Autowired
    private SavePointService savePointService;

    @Autowired
    public void setApplicationService(ApplicationService appService) {
        applicationService = appService;
    }

    @PostConstruct
    public void initialization() {
        trackingAppId = Caffeine.newBuilder().maximumSize(Long.MAX_VALUE).build();
        trackingAppCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(k -> applicationService.getById(k));
        QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tracking", 1);
        applicationService.list(queryWrapper).forEach((app) -> {
            trackingAppId.put(app.getId(), Byte.valueOf("0"));
            trackingAppCache.put(app.getId(), app);
        });
    }

    @PreDestroy
    public void ending() {
        log.info("[StreamX] flinkTrackingTask StreamXConsole will be shutdown,persistent application to database.");
        trackingAppCache.asMap().forEach((_k, v) -> persistent(v));
    }

    private AtomicLong atomicIndex = new AtomicLong(0);

    @Scheduled(fixedDelay = 1000 * 3)
    public void tracking() {
        Long index = atomicIndex.incrementAndGet();
        Map<Long, Byte> trackingIds = trackingAppId.asMap();
        trackingIds.forEach((k, _v) -> executor.execute(() -> {
            Application application = trackingAppCache.get(k, appId -> applicationService.getById(appId));
            StopFrom stopFrom = stopAppMap.getOrDefault(k, StopFrom.NONE);
            try {
                /**
                 * 1)到flink的restApi中查询状态
                 */
                JobsOverview jobsOverview = application.getJobsOverview();
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();
                if (optional.isPresent()) {
                    restApiCallback(application, optional.get(), stopFrom);
                }
            } catch (ConnectException exp) {
                /**
                 * 上一次的状态为canceling(在获取上次信息的时候flink restServer还未关闭为canceling),且本次如获取不到状态(flink restServer已关闭),则认为任务已经CANCELED
                 */
                log.info("[StreamX] flinkTrackingTask get state from flink restApi error");
                Tracker tracker = canceling.remove(application.getId());
                if (tracker != null && tracker.isPrevious(index)) {
                    log.info("[StreamX] flinkTrackingTask previous state was canceling.");
                    if (StopFrom.NONE.equals(stopFrom)) {
                        log.error("[StreamX] flinkTrackingTask query previous state was canceling and stopFrom NotFound,savePoint obsoleted!");
                        stopAppMap.remove(application.getId());
                        savePointService.obsolete(application.getId());
                    }
                    application.setState(FlinkAppState.CANCELED.getValue());
                    application.setAction(CurrentAction.NONE.getValue());
                    this.persistentAndClean(application);
                } else {
                    log.info("[StreamX] flinkTrackingTask previous state was not \"canceling\".");
                    try {
                        /**
                         * 2)到yarn的restApi中查询状态
                         */
                        AppInfo appInfo = application.getYarnAppInfo();
                        String state = appInfo.getApp().getFinalStatus();
                        FlinkAppState flinkAppState = FlinkAppState.valueOf(state);
                        if (FlinkAppState.KILLED.equals(flinkAppState)) {
                            stopAppMap.remove(application.getId());
                            if (StopFrom.NONE.equals(stopFrom)) {
                                log.error("[StreamX] flinkTrackingTask query jobsOverview from yarn,job was killed and stopFrom NotFound,savePoint obsoleted!");
                                savePointService.obsolete(application.getId());
                            }
                            flinkAppState = FlinkAppState.CANCELED;
                            application.setEndTime(new Date());
                            application.setAction(CurrentAction.NONE.getValue());
                        }
                        application.setState(flinkAppState.getValue());
                        this.persistentAndClean(application);
                    } catch (Exception e) {
                        /**s
                         * 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                         */
                        stopAppMap.remove(application.getId());
                        if (StopFrom.NONE.equals(stopFrom)) {
                            log.error("[StreamX] flinkTrackingTask query jobsOverview from restapi and yarn all error and stopFrom NotFound,savePoint obsoleted!");
                            savePointService.obsolete(application.getId());
                            application.setState(FlinkAppState.LOST.getValue());
                            //TODO send msg or emails
                        } else {
                            application.setState(FlinkAppState.CANCELED.getValue());
                        }
                        application.setAction(CurrentAction.NONE.getValue());
                        this.persistentAndClean(application);
                    }
                }
            } catch (IOException exception) {
                if (application.getState() != FlinkAppState.MAPPING.getValue()) {
                    log.error("[StreamX] flinkTrackingTask query jobsOverview from restApi error,job failed,savePoint obsoleted!");
                    stopAppMap.remove(application.getId());
                    if (StopFrom.NONE.equals(stopFrom)) {
                        savePointService.obsolete(application.getId());
                    }
                    application.setState(FlinkAppState.FAILED.getValue());
                    application.setAction(CurrentAction.NONE.getValue());
                    application.setEndTime(new Date());
                    this.persistentAndClean(application);
                }
            }
        }));
    }

    private static void persistent(Application application) {
        applicationService.updateTracking(application);
    }

    private void persistentAndClean(Application application) {
        persistent(application);
        stopTracking(application.getId());
    }

    /**
     * 1分钟往数据库同步一次状态.
     * 注意:该操作可能会导致当程序挂了,所监控的状态没及时往数据库同步的情况,造成被监控的实际的application和数控库状态不一致的情况
     * 但是这种操作也仅在每次程序挂和升级手动停止的情况,但是带的是减少了对数据库读写的巨大提升,大大减小了数据的压力.
     */
    @Scheduled(fixedDelay = 1000 * 60)
    public void persistent() {
        trackingAppCache.asMap().forEach((_k, v) -> persistent(v));
    }

    /**
     * 从flink restapi成功拿到当前任务的运行状态信息...
     *
     * @param application
     * @param job
     */
    private void restApiCallback(Application application, JobsOverview.Job job, StopFrom stopFrom) {
        Integer currentAction = application.getAction() == null ? 0 : application.getAction();
        FlinkAppState currentState = FlinkAppState.valueOf(job.getState());
        /**
         * 1) savePoint obsolete check and NEED_START check
         */
        switch (currentState) {
            case CANCELLING:
                canceling.put(application.getId(), new Tracker(atomicIndex.get(), application.getId()));
                break;
            case CANCELED:
                log.info("[StreamX] flinkTrackingTask application state {}, stop tracking and delete stopFrom!", currentState.name());
                stopTracking(application.getId());
                stopAppMap.remove(application.getId());
                if (StopFrom.NONE.equals(stopFrom)) {
                    log.info("[StreamX] flinkTrackingTask monitor callback from restApi, job cancel is not form streamX,savePoint obsoleted!");
                    savePointService.obsolete(application.getId());
                }
                application.setAction(CurrentAction.NONE.getValue());
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
                application.setAction(CurrentAction.NONE.getValue());
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

        if (application.getAction() != null && !application.getAction().equals(currentAction)) {
            persistent(application);
            flushTracking(application.getId());
        } else {
            trackingAppCache.put(application.getId(), application);
        }
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

    //===============================  static public method...  =========================================

    public static void addTracking(Application application) {
        log.info("[StreamX] flinkTrackingTask add app to tracking,appId:{}", application.getId());
        trackingAppId.put(application.getId(), Byte.valueOf("0"));
        trackingAppCache.put(application.getId(), application);
    }

    public static void addStopping(Long appId) {
        log.info("[StreamX] flinkTrackingTask add app to stopping,appId:{}", appId);
        stopAppMap.put(appId, StopFrom.STREAMX);
    }

    public static void flushTracking(Long appId) {
        log.info("[StreamX] flinkTrackingTask flushing app,appId:{}", appId);
        trackingAppCache.invalidate(appId);
    }

    /**
     * @param appId
     * @param runnable
     */
    public static void persistentAfterRunnable(Long appId, Runnable runnable) {
        log.info("[StreamX] flinkTrackingTask persistentAfterRunnable,appId:{}", appId);
        Application application = trackingAppCache.getIfPresent(appId);
        if (application != null) {
            persistent(application);
        }
        runnable.run();
        flushTracking(appId);
    }

    @SneakyThrows
    public static <T> T persistentAfterCallable(Long appId, Callable<T> runnable) {
        log.info("[StreamX] flinkTrackingTask persistentAfterCallable,appId:{}", appId);
        Application application = trackingAppCache.getIfPresent(appId);
        if (application != null) {
            persistent(application);
        }
        T t = runnable.call();
        flushTracking(appId);
        return t;
    }

    public static void stopTracking(Long appId) {
        log.info("[StreamX] flinkTrackingTask stop app,appId:{}", appId);
        trackingAppId.invalidate(appId);
        trackingAppCache.invalidate(appId);
    }

    public static Application getTracking(Long appId) {
        return trackingAppCache.getIfPresent(appId);
    }

}
