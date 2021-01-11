/*
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
import com.streamxhub.console.core.enums.OptionState;
import com.streamxhub.console.core.enums.DeployState;
import com.streamxhub.console.core.enums.FlinkAppState;
import com.streamxhub.console.core.enums.StopFrom;
import com.streamxhub.console.core.metrics.flink.JobsOverview;
import com.streamxhub.console.core.metrics.flink.Overview;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @author benjobs
 */
@Slf4j
@Component
public class FlinkTrackingTask {

    private static Cache<Long, Byte> trackingAppId = null;

    private static Cache<Long, Application> trackingAppCache = null;

    private static Cache<Long, Byte> savePointCache = null;

    private static Cache<Long, Byte> startingCache = null;

    private static final Map<Long, StopFrom> stopAppMap = new ConcurrentHashMap<>();

    private final Map<Long, Tracker> canceling = new ConcurrentHashMap<>();

    private ExecutorService executor = new ThreadPoolExecutor(
            Math.max(Runtime.getRuntime().availableProcessors() / 4, 2),
            Integer.MAX_VALUE,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            ThreadUtils.threadFactory("flink-monitor-executor")
    );

    @Autowired
    private SavePointService savePointService;

    private static ApplicationService applicationService;

    @Autowired
    public void setApplicationService(ApplicationService appService) {
        applicationService = appService;
    }

    @PostConstruct
    public void initialization() {
        trackingAppId = Caffeine.newBuilder().maximumSize(Long.MAX_VALUE).build();
        trackingAppCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(k -> applicationService.getById(k));
        startingCache = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();
        savePointCache = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
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
        trackingAppCache.asMap().forEach((k, v) -> persistent(v));
    }

    private final AtomicLong atomicIndex = new AtomicLong(0);

    @Scheduled(fixedDelay = 1000 * 2)
    public void tracking() {
        Long index = atomicIndex.incrementAndGet();
        Map<Long, Byte> trackingIds = trackingAppId.asMap();
        trackingIds.forEach((k, v) -> executor.execute(() -> {
            Application application = trackingAppCache.get(k, appId -> applicationService.getById(appId));
            StopFrom stopFrom = stopAppMap.getOrDefault(k, StopFrom.NONE);
            try {
                // 1)到flink的restApi中查询状态
                assert application != null;
                JobsOverview jobsOverview = application.httpJobsOverview();
                Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();
                optional.ifPresent(job -> restApiCallback(application, job, stopFrom));
            } catch (ConnectException exp) {
                // 2)到 yarn api中查询状态
                failoverFromYarn(application, index, stopFrom);
            } catch (IOException exception) {
                if (application.getState() != FlinkAppState.MAPPING.getValue()) {
                    log.error("[StreamX] flinkTrackingTask query jobsOverview from restApi error,job failed,savePoint obsoleted!");
                    if (StopFrom.NONE.equals(stopFrom)) {
                        savePointService.obsolete(application.getId());
                    }
                    application.setState(FlinkAppState.FAILED.getValue());
                    application.setOptionState(OptionState.NONE.getValue());
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
     * @param jobOverview
     */
    private void restApiCallback(Application application, JobsOverview.Job jobOverview, StopFrom stopFrom) {
        FlinkAppState currentState = FlinkAppState.valueOf(jobOverview.getState());

        // 1) jobId以restapi返回的状态为准
        application.setJobId(jobOverview.getId());
        application.setTotalTask(jobOverview.getTasks().getTotal());
        application.setOverview(jobOverview.getTasks());
        // 2) duration
        long startTime = jobOverview.getStartTime();
        long endTime = jobOverview.getEndTime();
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
        application.setDuration(jobOverview.getDuration());

        // 3) overview
        if (startingCache.getIfPresent(application.getId()) != null) {
            try {
                Overview override = application.httpOverview();
                if (override != null && override.getSlotsTotal() > 0) {
                    startingCache.invalidate(application.getId());
                    application.setTotalTM(override.getTaskmanagers());
                    application.setTotalSlot(override.getSlotsTotal());
                    application.setAvailableSlot(override.getSlotsAvailable());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 4) savePoint obsolete check and NEED_START check
        if (currentState.equals(FlinkAppState.RUNNING)) {
            FlinkAppState previousState = FlinkAppState.of(application.getState());
            if (FlinkAppState.STARTING.equals(previousState)) {
                // 发布完需重启和更新完匹配需重新的状态清空...
                startingCache.put(application.getId(), Byte.valueOf("0"));
                DeployState deployState = DeployState.of(application.getDeploy());
                if (DeployState.NEED_RESTART_AFTER_UPDATE.equals(deployState) || DeployState.NEED_RESTART_AFTER_DEPLOY.equals(deployState)) {
                    application.setDeploy(DeployState.NONE.get());
                }
            }
            // 保留一分钟的savepoint状态...
            if (savePointCache.getIfPresent(application.getId()) != null) {
                application.setState(FlinkAppState.CANCELLING.getValue());
            } else {
                application.setState(currentState.getValue());
                application.setOptionState(OptionState.NONE.getValue());
            }
            trackingAppCache.put(application.getId(), application);
        } else if (currentState.equals(FlinkAppState.CANCELLING)) {
            canceling.put(application.getId(), new Tracker(atomicIndex.get(), application.getId()));
            // savepoint已完毕.清空状态.
            if (savePointCache.getIfPresent(application.getId()) != null) {
                savePointCache.invalidate(application.getId());
                application.setOptionState(OptionState.NONE.getValue());
            }
            application.setState(currentState.getValue());
            trackingAppCache.put(application.getId(), application);
        } else if (currentState.equals(FlinkAppState.CANCELED)) {
            log.info("[StreamX] flinkTrackingTask application state {}, stop tracking and delete stopFrom!", currentState.name());
            application.setOptionState(OptionState.NONE.getValue());
            application.setState(currentState.getValue());
            if (StopFrom.NONE.equals(stopFrom)) {
                log.info("[StreamX] flinkTrackingTask monitor callback from restApi, job cancel is not form streamX,savePoint obsoleted!");
                savePointService.obsolete(application.getId());
            }
            savePointCache.invalidate(application.getId());
            persistentAndClean(application);
        } else if (currentState.equals(FlinkAppState.FAILED)) {
            application.setState(FlinkAppState.FAILED.getValue());
            application.setOptionState(OptionState.NONE.getValue());
            persistentAndClean(application);
        } else if (currentState.equals(FlinkAppState.RESTARTING)) {
            log.info("[StreamX] flinkTrackingTask application state {},add to starting", currentState.name());
            startingCache.put(application.getId(), Byte.valueOf("0"));
        } else {
            application.setState(currentState.getValue());
            trackingAppCache.put(application.getId(), application);
        }
    }

    /**
     * @param application
     * @param index
     * @param stopFrom
     */
    private void failoverFromYarn(Application application, Long index, StopFrom stopFrom) {
        log.info("[StreamX] flinkTrackingTask failoverRestApi starting...");
        // 上一次的状态为canceling(在获取上次信息的时候flink restServer还未关闭为canceling),且本次如获取不到状态(flink restServer已关闭),则认为任务已经CANCELED
        Tracker tracker = canceling.remove(application.getId());
        if (tracker != null && tracker.isPrevious(index)) {
            log.info("[StreamX] flinkTrackingTask previous state was canceling.");
            if (StopFrom.NONE.equals(stopFrom)) {
                log.error("[StreamX] flinkTrackingTask query previous state was canceling and stopFrom NotFound,savePoint obsoleted!");
                savePointService.obsolete(application.getId());
            }
            application.setState(FlinkAppState.CANCELED.getValue());
            application.setOptionState(OptionState.NONE.getValue());
            this.persistentAndClean(application);
        } else {
            log.info("[StreamX] flinkTrackingTask previous state was not \"canceling\".");
            try {
                // 2)到yarn的restApi中查询状态
                AppInfo appInfo = application.httpYarnAppInfo();
                String state = appInfo.getApp().getFinalStatus();
                FlinkAppState flinkAppState = FlinkAppState.valueOf(state);
                if (FlinkAppState.KILLED.equals(flinkAppState)) {
                    if (StopFrom.NONE.equals(stopFrom)) {
                        log.error("[StreamX] flinkTrackingTask query jobsOverview from yarn,job was killed and stopFrom NotFound,savePoint obsoleted!");
                        savePointService.obsolete(application.getId());
                    }
                    flinkAppState = FlinkAppState.CANCELED;
                    application.setEndTime(new Date());
                    application.setOptionState(OptionState.NONE.getValue());
                }
                application.setState(flinkAppState.getValue());
                this.persistentAndClean(application);
            } catch (Exception e) {
                // 3)如果从flink的restAPI和yarn的restAPI都查询失败,则任务失联.
                if (StopFrom.NONE.equals(stopFrom)) {
                    log.error("[StreamX] flinkTrackingTask query jobsOverview from restapi and yarn all error and stopFrom NotFound,savePoint obsoleted!");
                    savePointService.obsolete(application.getId());
                    application.setState(FlinkAppState.LOST.getValue());
                    //TODO send msg or emails
                } else {
                    application.setState(FlinkAppState.CANCELED.getValue());
                }
                application.setOptionState(OptionState.NONE.getValue());
                application.setEndTime(new Date());
                this.persistentAndClean(application);
            }
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
        startingCache.put(application.getId(), Byte.valueOf("0"));
        trackingAppCache.put(application.getId(), application);
    }

    public static void addStopping(Long appId) {
        log.info("[StreamX] flinkTrackingTask add app to stopping,appId:{}", appId);
        stopAppMap.put(appId, StopFrom.STREAMX);
    }

    public static void addSavepoint(Long appId) {
        log.info("[StreamX] flinkTrackingTask add app to savepoint,appId:{}", appId);
        savePointCache.put(appId, Byte.valueOf("0"));
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
        stopAppMap.remove(appId);
        trackingAppId.invalidate(appId);
        trackingAppCache.invalidate(appId);
    }

    public static ConcurrentMap<Long, Application> getAllTrackingApp() {
        return trackingAppCache.asMap();
    }

    public static Application getTracking(Long appId) {
        return trackingAppCache.getIfPresent(appId);
    }

}
