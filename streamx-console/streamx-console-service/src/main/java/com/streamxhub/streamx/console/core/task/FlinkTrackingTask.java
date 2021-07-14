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
package com.streamxhub.streamx.console.core.task;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.streamxhub.streamx.common.util.ThreadUtils;
import com.streamxhub.streamx.console.core.entity.Application;
import com.streamxhub.streamx.console.core.entity.SavePoint;
import com.streamxhub.streamx.console.core.enums.*;
import com.streamxhub.streamx.console.core.metrics.flink.CheckPoints;
import com.streamxhub.streamx.console.core.metrics.flink.JobsOverview;
import com.streamxhub.streamx.console.core.metrics.flink.Overview;
import com.streamxhub.streamx.console.core.metrics.yarn.AppInfo;
import com.streamxhub.streamx.console.core.service.AlertService;
import com.streamxhub.streamx.console.core.service.ApplicationService;
import com.streamxhub.streamx.console.core.service.SavePointService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * <pre><b>
 *  十步杀一人
 *  千里不留行
 *  事了拂衣去
 *  深藏身与名
 * </b></pre>
 * <br>
 * <strong>
 * NOTE:未曾想该类竟然是改动最多的
 * 最容易出问题的<br>
 * 看似一个简单的功能<br>
 * 每个状态的处理<br>
 * 每个操作<br>
 * 都是经过反复的思考<br>
 * 反复的测试<br>
 * 反复的修改<br>
 * 重要的事情说三遍:<br>
 * 魔鬼在细节中<br>
 * 魔鬼在细节中<br>
 * 魔鬼在细节中...<br>
 * </strong>
 *
 * @author benjobs
 */
@Slf4j
@Component
@DependsOn({"flyway", "flywayInitializer"})
public class FlinkTrackingTask {

    /**
     * <pre>
     * 记录任务是否需要savePoint
     * 只有在RUNNING状态下才会真正使用,如检查到任务正在运行,且需要savePoint,则设置该任务的状态为"savepoint"
     * </pre>
     */
    private static final Cache<Long, Byte> SAVEPOINT_CACHE = Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    /**
     * 记录第一次跟踪任务的状态,因为在任务启动后会在第一次跟踪时会获取任务的overview
     */
    private static final Cache<Long, Byte> STARTING_CACHE = Caffeine.newBuilder().expireAfterWrite(3, TimeUnit.MINUTES).build();

    /**
     * 跟踪任务列表
     */
    private static final Map<Long, Application> TRACKING_MAP = new ConcurrentHashMap<>(0);

    /**
     * <pre>
     * StopFrom: 用来记录任务是从StreamX web管理端停止的还是其他方式停止
     * 如从StreamX web管理端停止可以知道在停止任务时是否做savepoint,如做了savepoint,则将该savepoint设置为最后有效的savepoint,下次启动时,自动选择从该savepoint
     * 如:其他方式停止则,无法知道是否savepoint,直接将所有的savepoint设置为过期,任务再次启动时需要手动指定
     * </pre>
     */
    private static final Map<Long, StopFrom> STOP_FROM_MAP = new ConcurrentHashMap<>(0);

    /**
     * 检查到正在canceling的任务放到该cache中,过期时间为10秒(2次任务监控轮询的时间).
     */
    private final Cache<Long, Byte> cancelingCache = Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

    @Autowired
    private SavePointService savePointService;

    @Autowired
    private AlertService alertService;

    private static ApplicationService applicationService;

    private static final Map<String, Long> CHECK_POINT_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Counter> CHECK_POINT_FAILED_MAP = new ConcurrentHashMap<>();

    private static final Map<Long, OptionState> OPTIONING = new ConcurrentHashMap<>();

    private final Long STARTING_INTERVAL = 1000L * 30;

    private Long lastTrackTime = 0L;

    private Long lastOptionTime = 0L;

    private static Long optioningTime = 0L;

    private static final Byte DEFAULT_FLAG_BYTE = Byte.valueOf("0");

    private final ExecutorService executor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors() * 2,
            200,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1024),
            ThreadUtils.threadFactory("flink-tracking-executor"));

    @Autowired
    public void setApplicationService(ApplicationService appService) {
        applicationService = appService;
    }

    @PostConstruct
    public void initialization() {
        getAllApplications().forEach((app) -> TRACKING_MAP.put(app.getId(), app));
    }

    @PreDestroy
    public void ending() {
        log.info("flinkTrackingTask StreamXConsole will be shutdown,persistent application to database.");
        TRACKING_MAP.forEach((k, v) -> persistent(v));
    }

    /**
     * <p> <strong> NOTE: 执行必须满足以下条件</strong>
     * <p> <strong>1) 工程刚启动或者管理端页面正常操作任务(启动|停止),该操作需要非常实时的返回状态,频率1秒一次,持续10秒种(10次)</strong></p>
     * <p> <strong>2) 正常的状态信息获取,5秒执行一次</strong></p>
     */
    @Scheduled(fixedDelay = 1000)
    public void execute() {
        // 正常5秒钟获取一次信息
        long trackInterval = 1000L * 5;
        //10秒之内
        long optionInterval = 1000L * 10;

        //1) 项目刚启动第一次执行,或者前端正在操作...(启动,停止)需要立即返回状态信息.
        if (lastTrackTime == null || !OPTIONING.isEmpty()) {
            tracking();
        } else if (System.currentTimeMillis() - lastOptionTime <= optionInterval) {
            //2) 如果在管理端正在操作时间的10秒中之内(每秒执行一次)
            tracking();
        } else if (System.currentTimeMillis() - lastTrackTime >= trackInterval) {
            //3) 正常信息获取,判断本次时间和上次时间是否间隔5秒(正常监控信息获取,每5秒一次)
            tracking();
        }
    }

    private void tracking() {
        Long now = System.currentTimeMillis();
        lastTrackTime = now;
        TRACKING_MAP.forEach((key, application) -> executor.execute(() -> {
            final StopFrom stopFrom = STOP_FROM_MAP.getOrDefault(key, null) == null ? StopFrom.NONE : STOP_FROM_MAP.get(key);
            final OptionState optionState = OPTIONING.get(key);
            try {
                // 1) 到flink的REST Api中查询状态
                assert application.getId() != null;
                getFromFlinkRestApi(application, stopFrom);
            } catch (Exception flinkException) {
                // 2) 到 YARN REST api中查询状态
                try {
                    getFromYarnRestApi(application, stopFrom);
                } catch (Exception yarnException) {
                    /**
                     * 3) 从flink的restAPI和yarn的restAPI都查询失败</br>
                     * 此时需要根据管理端正在操作的状态来决定是否返回最终状态,需满足:</br>
                     * 1: 操作状态为为取消和正常的状态跟踪(操作状态不为STARTING)</br>
                     * 2: 如果操作状态为STARTING,则需要判断操作间隔是否在30秒之内(启动可能需要时间,这里给足够多的时间去完成启动)</br>
                     */
                    if (optionState == null
                            || !optionState.equals(OptionState.STARTING)
                            || now - optioningTime >= STARTING_INTERVAL) {
                        //非正在手动映射appId
                        if (application.getState() != FlinkAppState.MAPPING.getValue()) {
                            log.error("flinkTrackingTask getFromFlinkRestApi and getFromYarnRestApi error,job failed,savePoint obsoleted!");
                            if (StopFrom.NONE.equals(stopFrom)) {
                                savePointService.obsolete(application.getId());
                                application.setState(FlinkAppState.LOST.getValue());
                                alertService.alert(application, FlinkAppState.LOST);
                            } else {
                                application.setState(FlinkAppState.CANCELED.getValue());
                            }
                        }
                        /**
                         * 进入到这一步说明前两种方式获取信息都失败,此步是最后一步,直接会判别任务取消或失联</br>
                         * 需清空savepoint.
                         */
                        cleanSavepoint(application);
                        cleanOptioning(optionState, key);
                        application.setEndTime(new Date());
                        this.persistentAndClean(application);

                        FlinkAppState appState = FlinkAppState.of(application.getState());
                        if (appState.equals(FlinkAppState.FAILED) || appState.equals(FlinkAppState.LOST)) {
                            alertService.alert(application, FlinkAppState.of(application.getState()));
                            if (appState.equals(FlinkAppState.FAILED)) {
                                try {
                                    applicationService.start(application, true);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        }));
    }

    /**
     * 从flink restapi成功拿到当前任务的运行状态信息...
     *
     * @param application
     * @param stopFrom
     * @throws Exception
     */
    private void getFromFlinkRestApi(Application application, StopFrom stopFrom) throws Exception {
        JobsOverview jobsOverview = application.httpJobsOverview();
        Optional<JobsOverview.Job> optional = jobsOverview.getJobs().stream().findFirst();

        if (optional.isPresent()) {

            JobsOverview.Job jobOverview = optional.get();
            FlinkAppState currentState = FlinkAppState.of(jobOverview.getState());

            if (!FlinkAppState.OTHER.equals(currentState)) {
                // 1) set info from JobOverview
                handleJobOverview(application, jobOverview);

                //2) CheckPoints
                handleCheckPoints(application);

                //3) savePoint obsolete check and NEED_START check
                OptionState optionState = OPTIONING.get(application.getId());
                // cpu分支预测,将Running的状态单独拿出来
                if (currentState.equals(FlinkAppState.RUNNING)) {
                    handleRunningState(application, optionState, currentState);
                } else {
                    handleNotRunState(application, optionState, currentState, stopFrom);
                }
            }
        }
    }

    /**
     * 基本信息回写等处理
     *
     * @param application
     * @param jobOverview
     * @throws IOException
     */
    private void handleJobOverview(Application application, JobsOverview.Job jobOverview) throws IOException {
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

        // 3) overview,刚启动第一次获取Overview信息.
        if (STARTING_CACHE.getIfPresent(application.getId()) != null) {
            Overview override = application.httpOverview();
            if (override.getSlotsTotal() > 0) {
                STARTING_CACHE.invalidate(application.getId());
                application.setTotalTM(override.getTaskmanagers());
                application.setTotalSlot(override.getSlotsTotal());
                application.setAvailableSlot(override.getSlotsAvailable());
            }
        }
    }

    /**
     * 获取最新的checkPoint
     *
     * @param application
     * @throws IOException
     */
    private void handleCheckPoints(Application application) throws Exception {
        CheckPoints checkPoints = application.httpCheckpoints();
        if (checkPoints != null) {
            CheckPoints.Latest latest = checkPoints.getLatest();
            if (latest != null) {
                CheckPoints.CheckPoint checkPoint = latest.getCompleted();
                if (checkPoint != null) {
                    CheckPointStatus status = checkPoint.getCheckPointStatus();
                    if (CheckPointStatus.COMPLETED.equals(status)) {
                        Long latestId = CHECK_POINT_MAP.get(application.getJobId());
                        if (latestId == null || latestId < checkPoint.getId()) {
                            SavePoint savePoint = new SavePoint();
                            savePoint.setAppId(application.getId());
                            savePoint.setLatest(true);
                            savePoint.setType(checkPoint.getCheckPointType().get());
                            savePoint.setPath(checkPoint.getExternalPath());
                            savePoint.setTriggerTime(new Date(checkPoint.getTriggerTimestamp()));
                            savePoint.setCreateTime(new Date());
                            savePointService.save(savePoint);
                            CHECK_POINT_MAP.put(application.getJobId(), checkPoint.getId());
                        }
                    } else if (CheckPointStatus.FAILED.equals(status) && application.cpFailedTrigger()) {
                        Counter counter = CHECK_POINT_FAILED_MAP.get(application.getJobId());
                        if (counter == null) {
                            CHECK_POINT_FAILED_MAP.put(application.getJobId(), new Counter(checkPoint.getTriggerTimestamp()));
                        } else {
                            //x分钟之内超过Y次CheckPoint失败触发动作
                            long minute = counter.getDuration(checkPoint.getTriggerTimestamp());
                            if (minute <= application.getCpFailureRateInterval()
                                    && counter.getCount() >= application.getCpMaxFailureInterval()) {
                                CHECK_POINT_FAILED_MAP.remove(application.getJobId());
                                if (application.getCpFailureAction() == 1) {
                                    alertService.alert(application, CheckPointStatus.FAILED);
                                } else {
                                    applicationService.restart(application);
                                }
                            } else {
                                counter.add();
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 当前任务正在运行,一系列状态处理.
     *
     * @param application
     * @param optionState
     * @param currentState
     */
    private void handleRunningState(Application application, OptionState optionState, FlinkAppState currentState) {
        /**
         * 上次记录的状态的 "STARTING" 本次获取到最新的状态为"RUNNING",说明是重启后的第一次跟踪
         * 则:job以下状态需要更新为重启状态:
         * NEED_RESTART_AFTER_CONF_UPDATE(配置文件修改后需要重新启动)
         * NEED_RESTART_AFTER_SQL_UPDATE(flink sql修改后需要重启)
         * NEED_RESTART_AFTER_ROLLBACK(任务回滚后需要重启)
         * NEED_RESTART_AFTER_DEPLOY(任务重新发布后需要回滚)
         */
        if (OptionState.STARTING.equals(optionState)) {
            DeployState deployState = DeployState.of(application.getDeploy());
            //如果任务更新后需要重新启动 或 发布后需要重新启动
            switch (deployState) {
                case NEED_RESTART_AFTER_CONF_UPDATE:
                case NEED_RESTART_AFTER_SQL_UPDATE:
                case NEED_RESTART_AFTER_ROLLBACK:
                case NEED_RESTART_AFTER_DEPLOY:
                case NEED_ROLLBACK:
                    //清空需要重新启动的状态.
                    application.setDeploy(DeployState.DONE.get());
                    break;
                default:
                    break;
            }
        }
        // 当前状态为running,且savePointCache里有当前任务,说明该任务正在做savepoint
        if (SAVEPOINT_CACHE.getIfPresent(application.getId()) != null) {
            application.setOptionState(OptionState.SAVEPOINTING.getValue());
        } else {
            application.setOptionState(OptionState.NONE.getValue());
        }
        application.setState(currentState.getValue());
        TRACKING_MAP.put(application.getId(), application);
        cleanOptioning(optionState, application.getId());
    }


    /**
     * 当前任务未运行,状态处理
     *
     * @param application
     * @param optionState
     * @param currentState
     * @param stopFrom
     */
    private void handleNotRunState(Application application,
                                   OptionState optionState,
                                   FlinkAppState currentState,
                                   StopFrom stopFrom) throws Exception {
        switch (currentState) {
            case CANCELLING:
                cancelingCache.put(application.getId(), DEFAULT_FLAG_BYTE);
                cleanSavepoint(application);
                application.setState(currentState.getValue());
                TRACKING_MAP.put(application.getId(), application);
                break;
            case CANCELED:
                log.info("flinkTrackingTask getFromFlinkRestApi, job state {}, stop tracking and delete stopFrom!", currentState.name());
                cleanSavepoint(application);
                application.setState(currentState.getValue());
                if (StopFrom.NONE.equals(stopFrom)) {
                    log.info("flinkTrackingTask getFromFlinkRestApi, job cancel is not form streamX,savePoint obsoleted!");
                    savePointService.obsolete(application.getId());
                    alertService.alert(application, FlinkAppState.CANCELED);
                }
                //清理stopFrom
                STOP_FROM_MAP.remove(application.getId());
                //持久化application并且移除跟踪监控
                persistentAndClean(application);
                cleanOptioning(optionState, application.getId());
                break;
            case FAILED:
                cleanSavepoint(application);
                //清理stopFrom
                STOP_FROM_MAP.remove(application.getId());
                application.setState(FlinkAppState.FAILED.getValue());
                //持久化application并且移除跟踪监控
                persistentAndClean(application);
                alertService.alert(application, FlinkAppState.FAILED);
                applicationService.start(application, true);
                break;
            case RESTARTING:
                log.info("flinkTrackingTask getFromFlinkRestApi, job state {},add to starting", currentState.name());
                STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
                break;
            default:
                application.setState(currentState.getValue());
                TRACKING_MAP.put(application.getId(), application);
        }
    }

    /**
     * <p><strong>到 yarn中查询job的历史记录,说明flink任务已经停止,任务的最终状态为"CANCELED"</strong>
     *
     * @param application
     * @param stopFrom
     */
    private void getFromYarnRestApi(Application application, StopFrom stopFrom) throws Exception {
        log.debug("flinkTrackingTask getFromYarnRestApi starting...");
        OptionState optionState = OPTIONING.get(application.getId());

        /**
         * 上一次的状态为canceling(在获取信息时flink restServer还未关闭为canceling)
         * 且本次如获取不到状态(flink restServer已关闭),则认为任务已经CANCELED
         */
        Byte flag = cancelingCache.getIfPresent(application.getId());
        if (flag != null) {
            log.info("flinkTrackingTask previous state: canceling.");
            if (StopFrom.NONE.equals(stopFrom)) {
                log.error("flinkTrackingTask query previous state was canceling and stopFrom NotFound,savePoint obsoleted!");
                savePointService.obsolete(application.getId());
            }
            application.setState(FlinkAppState.CANCELED.getValue());
            cleanSavepoint(application);
            cleanOptioning(optionState, application.getId());
            this.persistentAndClean(application);
        } else {
            // 2)到yarn的restApi中查询状态
            AppInfo appInfo = application.httpYarnAppInfo();
            if (appInfo == null) {
                throw new RuntimeException("flinkTrackingTask getFromYarnRestApi failed ");
            } else {
                try {
                    String state = appInfo.getApp().getFinalStatus();
                    FlinkAppState flinkAppState = FlinkAppState.of(state);
                    if (FlinkAppState.OTHER.equals(flinkAppState)) {
                        return;
                    }
                    if (FlinkAppState.KILLED.equals(flinkAppState)) {
                        if (StopFrom.NONE.equals(stopFrom)) {
                            log.error("flinkTrackingTask getFromYarnRestApi,job was killed and stopFrom NotFound,savePoint obsoleted!");
                            savePointService.obsolete(application.getId());
                        }
                        flinkAppState = FlinkAppState.CANCELED;
                        cleanSavepoint(application);
                        application.setEndTime(new Date());
                    }
                    application.setState(flinkAppState.getValue());
                    //能运行到这一步,说明到YARN REST api中成功查询到信息
                    cleanOptioning(optionState, application.getId());
                    this.persistentAndClean(application);

                    if (flinkAppState.equals(FlinkAppState.FAILED) || flinkAppState.equals(FlinkAppState.LOST)) {
                        alertService.alert(application, flinkAppState);
                        if (flinkAppState.equals(FlinkAppState.FAILED)) {
                            applicationService.start(application, true);
                        }
                    }
                } catch (Exception e) {
                    throw new RuntimeException("flinkTrackingTask getFromYarnRestApi error,", e);
                }
            }
        }

    }

    private void cleanOptioning(OptionState optionState, Long key) {
        if (optionState != null) {
            lastOptionTime = System.currentTimeMillis();
            OPTIONING.remove(key);
        }
    }

    private void cleanSavepoint(Application application) {
        SAVEPOINT_CACHE.invalidate(application.getId());
        application.setOptionState(OptionState.NONE.getValue());
    }

    private static List<Application> getAllApplications() {
        QueryWrapper<Application> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tracking", 1);
        return applicationService.list(queryWrapper);
    }

    private static void persistent(Application application) {
        applicationService.updateTracking(application);
    }

    private void persistentAndClean(Application application) {
        persistent(application);
        stopTracking(application.getId());
    }


    /**
     * <p><strong>1分钟往数据库同步一次状态</strong></p></br>
     * <p><strong>NOTE:该操作可能会导致当程序挂了,所监控的状态没及时往数据库同步的情况,造成被监控的实际的application和数控库状态不一致的情况
     * 但是这种操作也仅在每次程序挂和升级手动停止的情况,但是带的是减少了对数据库读写的次数,减小了数据的压力.
     * </strong></p>
     */
    @Scheduled(fixedDelay = 1000 * 60)
    public void persistent() {
        TRACKING_MAP.forEach((k, v) -> persistent(v));
    }

    // ===============================  static public method...  =========================================

    /**
     * 设置正在操作中...
     */
    public static void setOptionState(Long appId, OptionState state) {
        log.info("flinkTrackingTask setOptioning");
        optioningTime = System.currentTimeMillis();
        OPTIONING.put(appId, state);
        //从streamx停止
        if (state.equals(OptionState.CANCELLING)) {
            STOP_FROM_MAP.put(appId, StopFrom.STREAMX);
        }
    }

    public static void addTracking(Application application) {
        log.info("flinkTrackingTask add app to tracking,appId:{}", application.getId());
        TRACKING_MAP.put(application.getId(), application);
        STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
    }

    public static void addSavepoint(Long appId) {
        log.info("flinkTrackingTask add app to savepoint,appId:{}", appId);
        SAVEPOINT_CACHE.put(appId, DEFAULT_FLAG_BYTE);
    }

    /**
     * 重新加载最新的application到数据库,防止如修改等操作,导致cache和实际数据库中信息不一致的问题.
     *
     * @param appId
     * @param callable
     */
    public static Object refreshTracking(Long appId, Callable callable) throws Exception {
        log.info("flinkTrackingTask flushing app,appId:{}", appId);
        Application application = TRACKING_MAP.get(appId);
        if (application != null) {
            persistent(application);
            Object result = callable.call();
            TRACKING_MAP.put(appId, applicationService.getById(appId));
            return result;
        }
        return callable.call();
    }

    public static void refreshTracking(Runnable runnable) {
        log.info("flinkTrackingTask flushing all application starting");
        getAllTrackingApp().values().forEach(app -> {
            Application application = TRACKING_MAP.get(app.getId());
            if (application != null) {
                persistent(application);
            }
        });

        runnable.run();

        getAllApplications().forEach((app) -> {
            if (TRACKING_MAP.get(app.getId()) != null) {
                TRACKING_MAP.put(app.getId(), app);
            }
        });
        log.info("flinkTrackingTask flushing all application end!");
    }

    public static void stopTracking(Long appId) {
        log.info("flinkTrackingTask stop app,appId:{}", appId);
        TRACKING_MAP.remove(appId);
    }

    public static Map<Long, Application> getAllTrackingApp() {
        return TRACKING_MAP;
    }

    public static Application getTracking(Long appId) {
        return TRACKING_MAP.get(appId);
    }

    @Data
    public static class Counter {
        private Long timestamp;
        private Integer count;

        public Counter(Long timestamp) {
            this.timestamp = timestamp;
            this.count = 1;
        }

        public void add() {
            this.count += 1;
        }

        public long getDuration(Long currentTimestamp) {
            return (currentTimestamp - this.getTimestamp()) / 1000 / 60;
        }
    }

}
