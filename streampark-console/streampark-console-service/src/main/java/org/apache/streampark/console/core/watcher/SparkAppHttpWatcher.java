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

package org.apache.streampark.console.core.watcher;

import org.apache.streampark.common.enums.FlinkExecutionMode;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.component.FlinkCheckpointProcessor;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.enums.FlinkAppStateEnum;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.ReleaseStateEnum;
import org.apache.streampark.console.core.enums.StopFromEnum;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.metrics.flink.Overview;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.service.application.ApplicationActionService;
import org.apache.streampark.console.core.service.application.ApplicationInfoService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.hc.client5.http.config.RequestConfig;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.time.Duration;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** This implementation is currently used for tracing flink job on yarn,standalone,remote mode */
@Slf4j
@Component
public class FlinkAppHttpWatcher {

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private ApplicationActionService applicationActionService;

  @Autowired private ApplicationInfoService applicationInfoService;

  @Autowired private AlertService alertService;

  @Autowired private FlinkCheckpointProcessor checkpointProcessor;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private SavePointService savePointService;

  @Autowired private FlinkClusterWatcher flinkClusterWatcher;

  @Qualifier("flinkRestAPIWatchingExecutor")
  @Autowired
  private Executor executorService;

  // track interval  every 5 seconds
  public static final Duration WATCHING_INTERVAL = Duration.ofSeconds(5);

  // option interval within 10 seconds
  private static final Duration OPTION_INTERVAL = Duration.ofSeconds(10);

  /**
   *
   *
   * <pre>
   * record task requires save points
   * It will only be used in the RUNNING state. If it is checked that the task is running and the save point is required,
   * set the state of the task to savepoint
   * </pre>
   */
  private static final Cache<Long, Byte> SAVEPOINT_CACHE =
      Caffeine.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

  /**
   * Record the status of the first tracking task, because after the task is started, the overview
   * of the task will be obtained during the first tracking
   */
  private static final Cache<Long, Byte> STARTING_CACHE =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

  /** tracking task list */
  private static final Map<Long, Application> WATCHING_APPS = new ConcurrentHashMap<>(0);

  /**
   *
   *
   * <pre>
   * StopFrom: marked a flink job canceling from the StreamPark or other ways:
   *    1) If stop from streampark, We can know whether to make a savepoint when flink job canceling, and if We make a savepoint,
   *    We can set the savepoint as the latest savepoint, and the next time start, will be automatically choose to start.
   *    2) if stop from other ways, there is no way to know the savepoint has been done, directly set all the savepoint to expire,
   *    and needs to be manually specified when started again.
   * </pre>
   */
  private static final Map<Long, StopFromEnum> STOP_FROM_MAP = new ConcurrentHashMap<>(0);

  /**
   * Cancelling tasks are placed in this cache with an expiration time of 10 seconds (the time of 2
   * task monitoring polls).
   */
  private static final Cache<Long, Byte> CANCELING_CACHE =
      Caffeine.newBuilder().expireAfterWrite(10, TimeUnit.SECONDS).build();

  /**
   * Task canceled tracking list, record who cancelled the tracking task Map<applicationId,userId>
   */
  private static final Map<Long, Long> CANCELLED_JOB_MAP = new ConcurrentHashMap<>(0);

  private static final Map<Long, FlinkCluster> FLINK_CLUSTER_MAP = new ConcurrentHashMap<>(0);

  private static final Map<Long, OptionStateEnum> OPTIONING = new ConcurrentHashMap<>(0);

  private Long lastWatchTime = 0L;

  private Long lastOptionTime = 0L;

  private static final Byte DEFAULT_FLAG_BYTE = Byte.valueOf("0");

  @PostConstruct
  public void init() {
    WATCHING_APPS.clear();
    List<Application> applications =
        applicationManageService.list(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getTracking, 1)
                .ne(Application::getState, FlinkAppStateEnum.LOST.getValue())
                .notIn(Application::getExecutionMode, FlinkExecutionMode.getKubernetesMode()));
    applications.forEach(
        (app) -> {
          WATCHING_APPS.put(app.getId(), app);
          STARTING_CACHE.put(app.getId(), DEFAULT_FLAG_BYTE);
        });
  }

  @PreDestroy
  public void doStop() {
    log.info(
        "FlinkAppHttpWatcher StreamPark Console will be shutdown,persistent application to database.");
    WATCHING_APPS.forEach((k, v) -> applicationManageService.persistMetrics(v));
  }

  /**
   * <strong>NOTE: The following conditions must be met for execution</strong>
   *
   * <p><strong>1) Program started or page operated task, such as start/stop, needs to return the
   * state immediately. (the frequency of 1 second once, continued 10 seconds (10 times))</strong>
   *
   * <p><strong>2) Normal information obtain, once every 5 seconds</strong>
   */
  @Scheduled(fixedDelay = 1000)
  public void start() {
    Long timeMillis = System.currentTimeMillis();
    if (lastWatchTime == null
        || !OPTIONING.isEmpty()
        || timeMillis - lastOptionTime <= OPTION_INTERVAL.toMillis()
        || timeMillis - lastWatchTime >= WATCHING_INTERVAL.toMillis()) {
      lastWatchTime = timeMillis;
      WATCHING_APPS.forEach(this::watch);
    }
  }

  @VisibleForTesting
  public @Nullable FlinkAppStateEnum tryQueryFlinkAppState(@Nonnull Long appId) {
    Application app = WATCHING_APPS.get(appId);
    return (app == null || app.getState() == null) ? null : app.getStateEnum();
  }

  private void watch(Long id, Application application) {
    executorService.execute(
        () -> {
          try {
            // query status from flink rest api
            getStateFromFlink(application);
          } catch (Exception flinkException) {
            // query status from yarn rest api
            try {
              getStateFromYarn(application);
            } catch (Exception e) {
              doStateFailed(application);
            }
          }
        });
  }

  private StopFromEnum getAppStopFrom(Long appId) {
    return STOP_FROM_MAP.getOrDefault(appId, StopFromEnum.NONE);
  }

  /**
   * Get the current task running status information from Flink rest api.
   *
   * @param application The application for which to retrieve the information
   * @throws Exception if an error occurs while retrieving the information from the Flink REST API
   */
  private void getStateFromFlink(Application application) throws Exception {
    JobsOverview jobsOverview = httpJobsOverview(application);
    Optional<JobsOverview.Job> jobOptional = getJobsOverviewJob(application, jobsOverview);
    if (jobOptional.isPresent()) {

      processJobState(application, jobOptional);
    }
  }

  private void processJobState(Application application, Optional<JobsOverview.Job> jobOptional)
      throws Exception {
    JobsOverview.Job jobOverview = jobOptional.get();
    FlinkAppStateEnum currentState = FlinkAppStateEnum.of(jobOverview.getState());

    if (FlinkAppStateEnum.OTHER != currentState) {
      try {
        // 1) set info from JobOverview
        handleJobOverview(application, jobOverview);
      } catch (Exception e) {
        log.error("get flink jobOverview error: {}", e.getMessage(), e);
      }
      try {
        // 2) CheckPoints
        handleCheckPoints(application);
      } catch (Exception e) {
        log.error("get flink jobOverview error: {}", e.getMessage(), e);
      }
      // 3) savePoint obsolete check and NEED_START check
      OptionStateEnum optionStateEnum = OPTIONING.get(application.getId());
      if (FlinkAppStateEnum.RUNNING == currentState) {
        handleRunningState(application, optionStateEnum, currentState);
      } else {
        handleNotRunState(application, optionStateEnum, currentState);
      }
    }
  }

  @Nonnull
  private Optional<JobsOverview.Job> getJobsOverviewJob(
      Application application, JobsOverview jobsOverview) {
    Optional<JobsOverview.Job> optional;
    FlinkExecutionMode execMode = application.getFlinkExecutionMode();
    if (FlinkExecutionMode.isYarnPerJobOrAppMode(execMode)) {
      optional =
          !jobsOverview.getJobs().isEmpty()
              ? jobsOverview.getJobs().stream()
                  .filter(a -> StringUtils.equals(application.getJobId(), a.getId()))
                  .findFirst()
              : Optional.empty();
    } else {
      optional =
          jobsOverview.getJobs().stream()
              .filter(x -> x.getId().equals(application.getJobId()))
              .findFirst();
    }
    return optional;
  }

  /**
   * <strong>Query the job history in yarn, indicating that the task has stopped, and the final
   * status of the task is CANCELED</strong>
   *
   * @param application application
   */
  private void getStateFromYarn(Application application) throws Exception {
    OptionStateEnum optionStateEnum = OPTIONING.get(application.getId());
    /*
     If the status of the last time is CANCELING (flink rest server is not closed at the time of getting information)
     and the status is not obtained this time (flink rest server is closed),
     the task is considered CANCELED
    */
    Byte flag = CANCELING_CACHE.getIfPresent(application.getId());
    StopFromEnum stopFromEnum = getAppStopFrom(application.getId());

    if (flag != null) {
      log.info("FlinkAppHttpWatcher previous state: canceling.");
      FlinkAppStateEnum flinkAppStateEnum = FlinkAppStateEnum.CANCELED;
      try {
        YarnAppInfo yarnAppInfo = httpYarnAppInfo(application);
        if (yarnAppInfo != null) {
          String state = yarnAppInfo.getApp().getFinalStatus();
          flinkAppStateEnum = FlinkAppStateEnum.of(state);
        }
      } finally {
        if (stopFromEnum.isNone()) {
          log.error(
              "FlinkAppHttpWatcher query previous state was canceling and stopFrom NotFound,savePoint expired!");
          savePointService.expire(application.getId());
          if (flinkAppStateEnum == FlinkAppStateEnum.KILLED
              || flinkAppStateEnum == FlinkAppStateEnum.FAILED) {
            doAlert(application, flinkAppStateEnum);
          }
        }
        application.setState(flinkAppStateEnum.getValue());
        cleanSavepoint(application);
        cleanOptioning(optionStateEnum, application.getId());
        doPersistMetrics(application, true);
      }

    } else {
      // query the status from the yarn rest Api
      YarnAppInfo yarnAppInfo = httpYarnAppInfo(application);
      if (yarnAppInfo == null) {
        if (FlinkExecutionMode.REMOTE != application.getFlinkExecutionMode()) {
          throw new RuntimeException("FlinkAppHttpWatcher getStateFromYarn failed ");
        }
      } else {
        try {
          String state = yarnAppInfo.getApp().getFinalStatus();
          FlinkAppStateEnum flinkAppStateEnum = FlinkAppStateEnum.of(state);
          if (FlinkAppStateEnum.OTHER == flinkAppStateEnum) {
            return;
          }
          if (FlinkAppStateEnum.KILLED == flinkAppStateEnum) {
            if (stopFromEnum.isNone()) {
              log.error(
                  "FlinkAppHttpWatcher getStateFromYarn,job was killed and stopFrom NotFound,savePoint expired!");
              savePointService.expire(application.getId());
            }
            flinkAppStateEnum = FlinkAppStateEnum.CANCELED;
            cleanSavepoint(application);
            application.setEndTime(new Date());
          }
          if (FlinkAppStateEnum.SUCCEEDED == flinkAppStateEnum) {
            flinkAppStateEnum = FlinkAppStateEnum.FINISHED;
          }
          application.setState(flinkAppStateEnum.getValue());
          cleanOptioning(optionStateEnum, application.getId());
          doPersistMetrics(application, true);
          if (FlinkAppStateEnum.FAILED == flinkAppStateEnum
              || FlinkAppStateEnum.LOST == flinkAppStateEnum
              || (FlinkAppStateEnum.CANCELED == flinkAppStateEnum && stopFromEnum.isNone())
              || applicationInfoService.checkAlter(application)) {
            doAlert(application, flinkAppStateEnum);
            stopCanceledJob(application.getId());
            if (FlinkAppStateEnum.FAILED == flinkAppStateEnum) {
              applicationActionService.start(application, true);
            }
          }
        } catch (Exception e) {
          if (FlinkExecutionMode.REMOTE != application.getFlinkExecutionMode()) {
            throw new RuntimeException("FlinkAppHttpWatcher getStateFromYarn error,", e);
          }
        }
      }
    }
  }

  private void doStateFailed(Application application) {
    /*
     Query from flink's restAPI and yarn's restAPI both failed.
     In this case, it is necessary to decide whether to return to the final state depending on the state being operated
    */
    final OptionStateEnum optionStateEnum = OPTIONING.get(application.getId());
    if (OptionStateEnum.STARTING != optionStateEnum) {
      // non-mapping
      if (application.getStateEnum() != FlinkAppStateEnum.MAPPING) {
        log.error(
            "FlinkAppHttpWatcher getStateFromFlink and getStateFromYARN error,job failed, savePoint expired!");
        StopFromEnum stopFromEnum = getAppStopFrom(application.getId());
        if (stopFromEnum.isNone()) {
          savePointService.expire(application.getId());
          application.setState(FlinkAppStateEnum.LOST.getValue());
          doAlert(application, FlinkAppStateEnum.LOST);
        } else {
          application.setState(FlinkAppStateEnum.CANCELED.getValue());
        }
      }
      /*
       This step means that the above two ways to get information have failed, and this step is the last step,
       which will directly identify the mission as cancelled or lost.
       Need clean savepoint.
      */
      application.setEndTime(new Date());
      cleanSavepoint(application);
      cleanOptioning(optionStateEnum, application.getId());
      doPersistMetrics(application, true);
      FlinkAppStateEnum appState = application.getStateEnum();
      if (FlinkAppStateEnum.FAILED == appState || FlinkAppStateEnum.LOST == appState) {
        doAlert(application, application.getStateEnum());
        if (FlinkAppStateEnum.FAILED == appState) {
          try {
            applicationActionService.start(application, true);
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    }
  }

  /**
   * handle job overview
   *
   * @param application application
   * @param jobOverview jobOverview
   */
  private void handleJobOverview(Application application, JobsOverview.Job jobOverview)
      throws IOException {
    // compute duration
    long startTime = jobOverview.getStartTime();
    long endTime = jobOverview.getEndTime();
    if (application.getStartTime() == null || startTime != application.getStartTime().getTime()) {
      application.setStartTime(new Date(startTime));
    }
    if (endTime != -1
        && (application.getEndTime() == null || endTime != application.getEndTime().getTime())) {
      application.setEndTime(new Date(endTime));
    }

    application.setJobId(jobOverview.getId());
    application.setDuration(jobOverview.getDuration());
    application.setTotalTask(jobOverview.getTasks().getTotal());
    application.setOverview(jobOverview.getTasks());

    // get overview info at the first start time
    if (STARTING_CACHE.getIfPresent(application.getId()) != null) {
      STARTING_CACHE.invalidate(application.getId());
      Overview override = httpOverview(application);
      if (override != null && override.getSlotsTotal() > 0) {
        application.setTotalTM(override.getTaskmanagers());
        application.setTotalSlot(override.getSlotsTotal());
        application.setAvailableSlot(override.getSlotsAvailable());
      }
    }
  }

  /** get latest checkpoint */
  private void handleCheckPoints(Application application) throws Exception {
    CheckPoints checkPoints = httpCheckpoints(application);
    if (checkPoints != null) {
      checkpointProcessor.process(application, checkPoints);
    }
  }

  /**
   * Handle running task
   *
   * @param application application
   * @param optionStateEnum optionState
   * @param currentState currentState
   */
  private void handleRunningState(
      Application application, OptionStateEnum optionStateEnum, FlinkAppStateEnum currentState) {
    /*
     if the last recorded state is STARTING and the latest state obtained this time is RUNNING,
     which means it is the first tracking after restart.
     Then: the following the job status needs to be updated to the restart status:
     NEED_RESTART_AFTER_CONF_UPDATE (Need to restart  after modified configuration)
     NEED_RESTART_AFTER_SQL_UPDATE (Need to restart  after modified flink sql)
     NEED_RESTART_AFTER_ROLLBACK (Need to restart after rollback)
     NEED_RESTART_AFTER_DEPLOY (Need to rollback after deploy)
    */
    if (OptionStateEnum.STARTING == optionStateEnum) {
      Application latestApp = WATCHING_APPS.get(application.getId());
      ReleaseStateEnum releaseStateEnum = latestApp.getReleaseState();
      switch (releaseStateEnum) {
        case NEED_RESTART:
        case NEED_ROLLBACK:
          LambdaUpdateWrapper<Application> updateWrapper =
              new LambdaUpdateWrapper<Application>()
                  .eq(Application::getId, application.getId())
                  .set(Application::getRelease, ReleaseStateEnum.DONE.get());
          applicationManageService.update(updateWrapper);
          break;
        default:
          break;
      }
    }

    // The current state is running, and there is a current task in the savePointCache,
    // indicating that the task is doing savepoint
    if (SAVEPOINT_CACHE.getIfPresent(application.getId()) != null) {
      application.setOptionState(OptionStateEnum.SAVEPOINTING.getValue());
    } else {
      application.setOptionState(OptionStateEnum.NONE.getValue());
    }
    application.setState(currentState.getValue());
    doPersistMetrics(application, false);
    cleanOptioning(optionStateEnum, application.getId());
  }

  private void doPersistMetrics(Application application, boolean stopWatch) {
    if (FlinkAppStateEnum.isEndState(application.getState())) {
      application.setOverview(null);
      application.setTotalTM(null);
      application.setTotalSlot(null);
      application.setTotalTask(null);
      application.setAvailableSlot(null);
      application.setJmMemory(null);
      application.setTmMemory(null);
      unWatching(application.getId());
    } else if (stopWatch) {
      unWatching(application.getId());
    } else {
      WATCHING_APPS.put(application.getId(), application);
    }
    applicationManageService.persistMetrics(application);
  }

  /**
   * Handle not running task
   *
   * @param application application
   * @param optionStateEnum optionState
   * @param currentState currentState
   */
  private void handleNotRunState(
      Application application, OptionStateEnum optionStateEnum, FlinkAppStateEnum currentState)
      throws Exception {

    switch (currentState) {
      case CANCELLING:
        CANCELING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
        cleanSavepoint(application);
        application.setState(currentState.getValue());
        doPersistMetrics(application, false);
        break;
      case CANCELED:
        log.info(
            "FlinkAppHttpWatcher getFromFlinkRestApi, job state {}, stop tracking and delete stopFrom!",
            currentState.name());
        cleanSavepoint(application);
        application.setState(currentState.getValue());
        StopFromEnum stopFromEnum = getAppStopFrom(application.getId());
        if (stopFromEnum.isNone() || applicationInfoService.checkAlter(application)) {
          if (stopFromEnum.isNone()) {
            log.info(
                "FlinkAppHttpWatcher getFromFlinkRestApi, job cancel is not form StreamPark,savePoint expired!");
            savePointService.expire(application.getId());
          }
          stopCanceledJob(application.getId());
          doAlert(application, FlinkAppStateEnum.CANCELED);
        }
        STOP_FROM_MAP.remove(application.getId());
        doPersistMetrics(application, true);
        cleanOptioning(optionStateEnum, application.getId());
        break;
      case FAILED:
        cleanSavepoint(application);
        STOP_FROM_MAP.remove(application.getId());
        application.setState(FlinkAppStateEnum.FAILED.getValue());
        doPersistMetrics(application, true);
        doAlert(application, FlinkAppStateEnum.FAILED);
        applicationActionService.start(application, true);
        break;
      case RESTARTING:
        log.info(
            "FlinkAppHttpWatcher getFromFlinkRestApi, job state {},add to starting",
            currentState.name());
        STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
        break;
      default:
        application.setState(currentState.getValue());
        doPersistMetrics(application, false);
    }
  }

  private void cleanOptioning(OptionStateEnum optionStateEnum, Long key) {
    if (optionStateEnum != null) {
      lastOptionTime = System.currentTimeMillis();
      OPTIONING.remove(key);
    }
  }

  public void cleanSavepoint(Application application) {
    SAVEPOINT_CACHE.invalidate(application.getId());
    application.setOptionState(OptionStateEnum.NONE.getValue());
  }

  /** set current option state */
  public static void setOptionState(Long appId, OptionStateEnum state) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkAppHttpWatcher setOptioning");
    OPTIONING.put(appId, state);
    if (OptionStateEnum.CANCELLING == state) {
      STOP_FROM_MAP.put(appId, StopFromEnum.STREAMPARK);
    }
  }

  public static void doWatching(Application application) {
    if (isKubernetesApp(application)) {
      return;
    }
    log.info("FlinkAppHttpWatcher add app to tracking,appId:{}", application.getId());
    WATCHING_APPS.put(application.getId(), application);
    STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
  }

  public static void addSavepoint(Long appId) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkAppHttpWatcher add app to savepoint,appId:{}", appId);
    SAVEPOINT_CACHE.put(appId, DEFAULT_FLAG_BYTE);
  }

  public static void unWatching(Long appId) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkAppHttpWatcher stop app,appId:{}", appId);
    WATCHING_APPS.remove(appId);
  }

  public static void stopCanceledJob(Long appId) {
    if (!CANCELLED_JOB_MAP.containsKey(appId)) {
      return;
    }
    log.info("flink job canceled app appId:{} by useId:{}", appId, CANCELLED_JOB_MAP.get(appId));
    CANCELLED_JOB_MAP.remove(appId);
  }

  public static void addCanceledApp(Long appId, Long userId) {
    log.info("flink job addCanceledApp app appId:{}, useId:{}", appId, userId);
    CANCELLED_JOB_MAP.put(appId, userId);
  }

  public static Long getCanceledJobUserId(Long appId) {
    return CANCELLED_JOB_MAP.get(appId) == null ? Long.valueOf(-1) : CANCELLED_JOB_MAP.get(appId);
  }

  public static Collection<Application> getWatchingApps() {
    return WATCHING_APPS.values();
  }

  private static boolean isKubernetesApp(Application application) {
    return FlinkK8sWatcherWrapper.isKubernetesApp(application);
  }

  private static boolean isKubernetesApp(Long appId) {
    Application app = WATCHING_APPS.get(appId);
    return FlinkK8sWatcherWrapper.isKubernetesApp(app);
  }

  private YarnAppInfo httpYarnAppInfo(Application application) throws Exception {
    String reqURL = "ws/v1/cluster/apps/".concat(application.getClusterId());
    return yarnRestRequest(reqURL, YarnAppInfo.class);
  }

  private Overview httpOverview(Application application) throws IOException {
    String appId = application.getClusterId();
    if (appId != null
        && (FlinkExecutionMode.YARN_APPLICATION == application.getFlinkExecutionMode()
            || FlinkExecutionMode.YARN_PER_JOB == application.getFlinkExecutionMode())) {
      String reqURL;
      String jmURL = application.getJobManagerUrl();
      if (StringUtils.isNotBlank(jmURL) && Utils.checkHttpURL(jmURL)) {
        String format = "%s/overview";
        reqURL = String.format(format, jmURL);
      } else {
        String format = "proxy/%s/overview";
        reqURL = String.format(format, appId);
      }
      return yarnRestRequest(reqURL, Overview.class);
    }
    return null;
  }

  private JobsOverview httpJobsOverview(Application application) throws Exception {
    final String flinkUrl = "jobs/overview";
    FlinkExecutionMode execMode = application.getFlinkExecutionMode();
    if (FlinkExecutionMode.isYarnMode(execMode)) {
      String reqURL;
      String jmURL = application.getJobManagerUrl();
      if (StringUtils.isNotBlank(jmURL) && Utils.checkHttpURL(jmURL)) {
        String format = "%s/" + flinkUrl;
        reqURL = String.format(format, jmURL);
      } else {
        String format = "proxy/%s/" + flinkUrl;
        reqURL = String.format(format, application.getClusterId());
      }
      return yarnRestRequest(reqURL, JobsOverview.class);
    }

    if (application.getJobId() != null && FlinkExecutionMode.isRemoteMode(execMode)) {
      return httpRemoteCluster(
          application.getFlinkClusterId(),
          cluster -> {
            String remoteUrl = cluster.getAddress() + "/" + flinkUrl;
            JobsOverview jobsOverview = httpRestRequest(remoteUrl, JobsOverview.class);
            if (jobsOverview != null) {
              List<JobsOverview.Job> jobs =
                  jobsOverview.getJobs().stream()
                      .filter(x -> x.getId().equals(application.getJobId()))
                      .collect(Collectors.toList());
              jobsOverview.setJobs(jobs);
            }
            return jobsOverview;
          });
    }
    return null;
  }

  private CheckPoints httpCheckpoints(Application application) throws Exception {
    final String flinkUrl = "jobs/%s/checkpoints";
    FlinkExecutionMode execMode = application.getFlinkExecutionMode();
    if (FlinkExecutionMode.isYarnMode(execMode)) {
      String reqURL;
      String jmURL = application.getJobManagerUrl();
      if (StringUtils.isNotBlank(jmURL) && Utils.checkHttpURL(jmURL)) {
        String format = "%s/" + flinkUrl;
        reqURL = String.format(format, jmURL, application.getJobId());
      } else {
        String format = "proxy/%s/" + flinkUrl;
        reqURL = String.format(format, application.getClusterId(), application.getJobId());
      }
      return yarnRestRequest(reqURL, CheckPoints.class);
    }

    if (application.getJobId() != null && FlinkExecutionMode.isRemoteMode(execMode)) {
      return httpRemoteCluster(
          application.getFlinkClusterId(),
          cluster -> {
            String remoteUrl =
                cluster.getAddress() + "/" + String.format(flinkUrl, application.getJobId());
            return httpRestRequest(remoteUrl, CheckPoints.class);
          });
    }
    return null;
  }

  private <T> T yarnRestRequest(String url, Class<T> clazz) throws IOException {
    String result = YarnUtils.restRequest(url);
    if (null == result) {
      return null;
    }
    return JacksonUtils.read(result, clazz);
  }

  private <T> T httpRestRequest(String url, Class<T> clazz) throws IOException {
    String result =
        HttpClientUtils.httpGetRequest(
            url, RequestConfig.custom().setConnectTimeout(5000, TimeUnit.MILLISECONDS).build());
    if (null == result) {
      return null;
    }
    return JacksonUtils.read(result, clazz);
  }

  public boolean isWatchingApp(Long id) {
    return WATCHING_APPS.containsKey(id);
  }

  private <T> T httpRemoteCluster(Long clusterId, Callback<FlinkCluster, T> function)
      throws Exception {
    FlinkCluster flinkCluster = getFlinkRemoteCluster(clusterId, false);
    try {
      return function.call(flinkCluster);
    } catch (Exception e) {
      flinkCluster = getFlinkRemoteCluster(clusterId, true);
      return function.call(flinkCluster);
    }
  }

  private FlinkCluster getFlinkRemoteCluster(Long clusterId, boolean flush) {
    FlinkCluster flinkCluster = FLINK_CLUSTER_MAP.get(clusterId);
    if (flinkCluster == null || flush) {
      flinkCluster = flinkClusterService.getById(clusterId);
      FLINK_CLUSTER_MAP.put(clusterId, flinkCluster);
    }
    return flinkCluster;
  }

  interface Callback<T, R> {
    R call(T e) throws Exception;
  }

  /**
   * Describes the alarming behavior under abnormal operation for different job running modes:
   *
   * <p>- <strong>yarn per job</strong> or <strong>yarn application</strong>
   *
   * <p>Directly triggers an alarm when the job encounters an abnormal condition.<br>
   *
   * <p>- <strong>yarn session</strong> or <strong>remote</strong>
   *
   * <p>If the Flink cluster configuration lacks alarm information, it triggers an alarm directly
   * when the job is abnormal.<br>
   * If the Flink cluster configuration has alarm information:
   *
   * <p>When the job is abnormal due to an issue in the Flink cluster, the job's alarm will be held
   * back, instead waiting for the Flink cluster's alarm.<br>
   * When the job is abnormal due to the job itself and the Flink cluster is running normally, an
   * alarm specific to the job will be triggered.
   *
   * @param app application
   * @param appState application state
   */
  private void doAlert(Application app, FlinkAppStateEnum appState) {
    if (app.getProbing()) {
      log.info("application with id {} is probing, don't send alert", app.getId());
      return;
    }
    switch (app.getFlinkExecutionMode()) {
      case YARN_APPLICATION:
      case YARN_PER_JOB:
        alertService.alert(app.getAlertId(), AlertTemplate.of(app, appState));
        return;
      case YARN_SESSION:
      case REMOTE:
        FlinkCluster flinkCluster = flinkClusterService.getById(app.getFlinkClusterId());
        if (flinkClusterWatcher.verifyClusterConnection(flinkCluster)) {
          log.info(
              "application with id {} is yarn session or remote and flink cluster with id {} is alive, application send alert",
              app.getId(),
              app.getFlinkClusterId());
          alertService.alert(app.getAlertId(), AlertTemplate.of(app, appState));
        }
        break;
      default:
        break;
    }
  }
}
