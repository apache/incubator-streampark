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

package org.apache.streampark.console.core.task;

import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.ThreadUtils;
import org.apache.streampark.common.util.Utils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.LaunchState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.enums.StopFrom;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.metrics.flink.JobsOverview;
import org.apache.streampark.console.core.metrics.flink.Overview;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.FlinkClusterService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** This implementation is currently used for tracing flink job on yarn,standalone,remote mode */
@Slf4j
@Component
public class FlinkRESTAPIWatcher {

  @Autowired private ApplicationService applicationService;

  @Autowired private AlertService alertService;

  @Autowired private CheckpointProcessor checkpointProcessor;

  @Autowired private FlinkClusterService flinkClusterService;

  @Autowired private SavePointService savePointService;

  // track interval  every 5 seconds
  private static final long WATCHING_INTERVAL = 1000L * 5;
  // option interval within 10 seconds
  private static final long OPTION_INTERVAL = 1000L * 10;

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
   * StopFrom: marked a task stopped from the stream-park web or other ways.
   * If stop from stream-park web, you can know whether to make a savepoint when you stop the task, and if you make a savepoint,
   * you can set the savepoint as the last effect savepoint, and the next time start, will be automatically choose to start.
   * In other words, if stop from other ways, there is no way to know the savepoint has been done, directly set all the savepoint
   * to expire, and needs to be manually specified when started again.
   * </pre>
   */
  private static final Map<Long, StopFrom> STOP_FROM_MAP = new ConcurrentHashMap<>(0);

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

  private static final Map<Long, OptionState> OPTIONING = new ConcurrentHashMap<>(0);

  private Long lastWatchingTime = 0L;

  private Long lastOptionTime = 0L;

  private static final Byte DEFAULT_FLAG_BYTE = Byte.valueOf("0");

  private static final ExecutorService EXECUTOR =
      new ThreadPoolExecutor(
          Runtime.getRuntime().availableProcessors() * 5,
          Runtime.getRuntime().availableProcessors() * 10,
          60L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(1024),
          ThreadUtils.threadFactory("flink-restapi-watching-executor"));

  @PostConstruct
  public void init() {
    WATCHING_APPS.clear();
    List<Application> applications =
        applicationService.list(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getTracking, 1)
                .notIn(Application::getExecutionMode, ExecutionMode.getKubernetesMode()));
    applications.forEach((app) -> WATCHING_APPS.put(app.getId(), app));
  }

  @PreDestroy
  public void doStop() {
    log.info(
        "FlinkRESTAPIWatcher StreamPark Console will be shutdown,persistent application to database.");
    WATCHING_APPS.forEach((k, v) -> applicationService.persistMetrics(v));
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
    // The application has been started at the first time, or the front-end is operating start/stop,
    // need to return status info immediately.
    if (lastWatchingTime == null || !OPTIONING.isEmpty()) {
      doWatch();
    } else if (System.currentTimeMillis() - lastOptionTime <= OPTION_INTERVAL) {
      // The last operation time is less than option interval.(10 seconds)
      doWatch();
    } else if (System.currentTimeMillis() - lastWatchingTime >= WATCHING_INTERVAL) {
      // Normal information obtain, check if there is 5 seconds interval between this time and the
      // last time.(once every 5 seconds)
      doWatch();
    }
  }

  private void doWatch() {
    lastWatchingTime = System.currentTimeMillis();
    for (Map.Entry<Long, Application> entry : WATCHING_APPS.entrySet()) {
      EXECUTOR.execute(
          () -> {
            long key = entry.getKey();
            Application application = entry.getValue();
            final StopFrom stopFrom =
                STOP_FROM_MAP.getOrDefault(key, null) == null
                    ? StopFrom.NONE
                    : STOP_FROM_MAP.get(key);
            final OptionState optionState = OPTIONING.get(key);
            try {
              // query status from flink rest api
              Utils.required(application.getId() != null);
              getFromFlinkRestApi(application, stopFrom);
            } catch (Exception flinkException) {
              // query status from yarn rest api
              try {
                getFromYarnRestApi(application, stopFrom);
              } catch (Exception yarnException) {
                /*
                 Query from flink's restAPI and yarn's restAPI both failed.
                 In this case, it is necessary to decide whether to return to the final state depending on the state being operated
                */
                if (optionState == null || !optionState.equals(OptionState.STARTING)) {
                  // non-mapping
                  if (application.getState() != FlinkAppState.MAPPING.getValue()) {
                    log.error(
                        "FlinkRESTAPIWatcher getFromFlinkRestApi and getFromYarnRestApi error,job failed,savePoint expired!");
                    if (StopFrom.NONE.equals(stopFrom)) {
                      savePointService.expire(application.getId());
                      application.setState(FlinkAppState.LOST.getValue());
                      alertService.alert(application, FlinkAppState.LOST);
                    } else {
                      application.setState(FlinkAppState.CANCELED.getValue());
                    }
                  }
                  /*
                   This step means that the above two ways to get information have failed, and this step is the last step,
                   which will directly identify the mission as cancelled or lost.
                   Need clean savepoint.
                  */
                  application.setEndTime(new Date());
                  cleanSavepoint(application);
                  cleanOptioning(optionState, key);
                  doPersistMetrics(application, true);
                  FlinkAppState appState = FlinkAppState.of(application.getState());
                  if (appState.equals(FlinkAppState.FAILED)
                      || appState.equals(FlinkAppState.LOST)) {
                    alertService.alert(application, FlinkAppState.of(application.getState()));
                    if (appState.equals(FlinkAppState.FAILED)) {
                      try {
                        applicationService.start(application, true);
                      } catch (Exception e) {
                        log.error(e.getMessage(), e);
                      }
                    }
                  }
                }
              }
            }
          });
    }
  }

  /**
   * Get the current task running status information from flink restapi
   *
   * @param application application
   * @param stopFrom stopFrom
   */
  private void getFromFlinkRestApi(Application application, StopFrom stopFrom) throws Exception {
    FlinkCluster flinkCluster = getFlinkCluster(application);
    JobsOverview jobsOverview = httpJobsOverview(application, flinkCluster);
    Optional<JobsOverview.Job> optional;
    ExecutionMode execMode = application.getExecutionModeEnum();
    if (ExecutionMode.YARN_APPLICATION.equals(execMode)
        || ExecutionMode.YARN_PER_JOB.equals(execMode)) {
      optional =
          jobsOverview.getJobs().size() > 1
              ? jobsOverview.getJobs().stream()
                  .filter(a -> StringUtils.equals(application.getJobId(), a.getId()))
                  .findFirst()
              : jobsOverview.getJobs().stream().findFirst();
    } else {
      optional =
          jobsOverview.getJobs().stream()
              .filter(x -> x.getId().equals(application.getJobId()))
              .findFirst();
    }
    if (optional.isPresent()) {

      JobsOverview.Job jobOverview = optional.get();
      FlinkAppState currentState = FlinkAppState.of(jobOverview.getState());

      if (!FlinkAppState.OTHER.equals(currentState)) {
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
        OptionState optionState = OPTIONING.get(application.getId());
        if (currentState.equals(FlinkAppState.RUNNING)) {
          handleRunningState(application, optionState, currentState);
        } else {
          handleNotRunState(application, optionState, currentState, stopFrom);
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
    if (endTime != -1) {
      if (application.getEndTime() == null || endTime != application.getEndTime().getTime()) {
        application.setEndTime(new Date(endTime));
      }
    }

    application.setJobId(jobOverview.getId());
    application.setDuration(jobOverview.getDuration());
    application.setTotalTask(jobOverview.getTasks().getTotal());
    application.setOverview(jobOverview.getTasks());

    // get overview info at the first start time
    if (STARTING_CACHE.getIfPresent(application.getId()) != null) {
      FlinkCluster flinkCluster = getFlinkCluster(application);
      Overview override = httpOverview(application, flinkCluster);
      if (override != null && override.getSlotsTotal() > 0) {
        application.setTotalTM(override.getTaskmanagers());
        application.setTotalSlot(override.getSlotsTotal());
        application.setAvailableSlot(override.getSlotsAvailable());
      }
      STARTING_CACHE.invalidate(application.getId());
    }
  }

  /** get latest checkpoint */
  private void handleCheckPoints(Application application) throws Exception {
    FlinkCluster flinkCluster = getFlinkCluster(application);
    CheckPoints checkPoints = httpCheckpoints(application, flinkCluster);
    if (checkPoints != null) {
      checkpointProcessor.process(application.getId(), checkPoints);
    }
  }

  /**
   * Handle running task
   *
   * @param application application
   * @param optionState optionState
   * @param currentState currentState
   */
  private void handleRunningState(
      Application application, OptionState optionState, FlinkAppState currentState) {
    /*
     if the last recorded state is STARTING and the latest state obtained this time is RUNNING,
     which means it is the first tracking after restart.
     Then: the following the job status needs to be updated to the restart status:
     NEED_RESTART_AFTER_CONF_UPDATE (Need to restart  after modified configuration)
     NEED_RESTART_AFTER_SQL_UPDATE (Need to restart  after modified flink sql)
     NEED_RESTART_AFTER_ROLLBACK (Need to restart after rollback)
     NEED_RESTART_AFTER_DEPLOY (Need to rollback after deploy)
    */
    if (OptionState.STARTING.equals(optionState)) {
      Application latestApp = WATCHING_APPS.get(application.getId());
      LaunchState launchState = latestApp.getLaunchState();
      switch (launchState) {
        case NEED_RESTART:
        case NEED_ROLLBACK:
          LambdaUpdateWrapper<Application> updateWrapper =
              new LambdaUpdateWrapper<Application>()
                  .eq(Application::getId, application.getId())
                  .set(Application::getLaunch, LaunchState.DONE.get());
          applicationService.update(updateWrapper);
          break;
        default:
          break;
      }
    }

    // The current state is running, and there is a current task in the savePointCache,
    // indicating that the task is doing savepoint
    if (SAVEPOINT_CACHE.getIfPresent(application.getId()) != null) {
      application.setOptionState(OptionState.SAVEPOINTING.getValue());
    } else {
      application.setOptionState(OptionState.NONE.getValue());
    }
    application.setState(currentState.getValue());
    doPersistMetrics(application, false);
    cleanOptioning(optionState, application.getId());
  }

  private void doPersistMetrics(Application application, boolean stopWatch) {
    if (FlinkAppState.isEndState(application.getState())) {
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
    applicationService.persistMetrics(application);
  }

  /**
   * Handle not running task
   *
   * @param application application
   * @param optionState optionState
   * @param currentState currentState
   * @param stopFrom stopFrom
   */
  private void handleNotRunState(
      Application application,
      OptionState optionState,
      FlinkAppState currentState,
      StopFrom stopFrom)
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
            "FlinkRESTAPIWatcher getFromFlinkRestApi, job state {}, stop tracking and delete stopFrom!",
            currentState.name());
        cleanSavepoint(application);
        application.setState(currentState.getValue());
        if (StopFrom.NONE.equals(stopFrom) || applicationService.checkAlter(application)) {
          if (StopFrom.NONE.equals(stopFrom)) {
            log.info(
                "FlinkRESTAPIWatcher getFromFlinkRestApi, job cancel is not form StreamPark,savePoint expired!");
            savePointService.expire(application.getId());
          }
          stopCanceledJob(application.getId());
          alertService.alert(application, FlinkAppState.CANCELED);
        }
        STOP_FROM_MAP.remove(application.getId());
        doPersistMetrics(application, true);
        cleanOptioning(optionState, application.getId());
        break;
      case FAILED:
        cleanSavepoint(application);
        STOP_FROM_MAP.remove(application.getId());
        application.setState(FlinkAppState.FAILED.getValue());
        doPersistMetrics(application, true);
        alertService.alert(application, FlinkAppState.FAILED);
        applicationService.start(application, true);
        break;
      case RESTARTING:
        log.info(
            "FlinkRESTAPIWatcher getFromFlinkRestApi, job state {},add to starting",
            currentState.name());
        STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
        break;
      default:
        application.setState(currentState.getValue());
        doPersistMetrics(application, false);
    }
  }

  /**
   * <strong>Query the job history in yarn, indicating that the task has stopped, and the final
   * status of the task is CANCELED</strong>
   *
   * @param application application
   * @param stopFrom stopFrom
   */
  private void getFromYarnRestApi(Application application, StopFrom stopFrom) throws Exception {
    log.debug("FlinkRESTAPIWatcher getFromYarnRestApi starting...");
    OptionState optionState = OPTIONING.get(application.getId());

    /*
     If the status of the last time is CANCELING (flink rest server is not closed at the time of getting information)
     and the status is not obtained this time (flink rest server is closed),
     the task is considered CANCELED
    */
    Byte flag = CANCELING_CACHE.getIfPresent(application.getId());
    if (flag != null) {
      log.info("FlinkRESTAPIWatcher previous state: canceling.");
      if (StopFrom.NONE.equals(stopFrom)) {
        log.error(
            "FlinkRESTAPIWatcher query previous state was canceling and stopFrom NotFound,savePoint expired!");
        savePointService.expire(application.getId());
      }
      application.setState(FlinkAppState.CANCELED.getValue());
      cleanSavepoint(application);
      cleanOptioning(optionState, application.getId());
      doPersistMetrics(application, true);
    } else {
      // query the status from the yarn rest Api
      YarnAppInfo yarnAppInfo = httpYarnAppInfo(application);
      if (yarnAppInfo == null) {
        if (!ExecutionMode.REMOTE.equals(application.getExecutionModeEnum())) {
          throw new RuntimeException("FlinkRESTAPIWatcher getFromYarnRestApi failed ");
        }
      } else {
        try {
          String state = yarnAppInfo.getApp().getFinalStatus();
          FlinkAppState flinkAppState = FlinkAppState.of(state);
          if (FlinkAppState.OTHER.equals(flinkAppState)) {
            return;
          }
          if (FlinkAppState.KILLED.equals(flinkAppState)) {
            if (StopFrom.NONE.equals(stopFrom)) {
              log.error(
                  "FlinkRESTAPIWatcher getFromYarnRestApi,job was killed and stopFrom NotFound,savePoint expired!");
              savePointService.expire(application.getId());
            }
            flinkAppState = FlinkAppState.CANCELED;
            cleanSavepoint(application);
            application.setEndTime(new Date());
          }
          if (FlinkAppState.SUCCEEDED.equals(flinkAppState)) {
            flinkAppState = FlinkAppState.FINISHED;
          }
          application.setState(flinkAppState.getValue());
          cleanOptioning(optionState, application.getId());
          doPersistMetrics(application, true);
          if (flinkAppState.equals(FlinkAppState.FAILED)
              || flinkAppState.equals(FlinkAppState.LOST)
              || (flinkAppState.equals(FlinkAppState.CANCELED) && StopFrom.NONE.equals(stopFrom))
              || applicationService.checkAlter(application)) {
            alertService.alert(application, flinkAppState);
            stopCanceledJob(application.getId());
            if (flinkAppState.equals(FlinkAppState.FAILED)) {
              applicationService.start(application, true);
            }
          }
        } catch (Exception e) {
          if (!ExecutionMode.REMOTE.equals(application.getExecutionModeEnum())) {
            throw new RuntimeException("FlinkRESTAPIWatcher getFromYarnRestApi error,", e);
          }
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

  /** set current option state */
  public static void setOptionState(Long appId, OptionState state) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkRESTAPIWatcher setOptioning");
    OPTIONING.put(appId, state);
    if (state.equals(OptionState.CANCELLING)) {
      STOP_FROM_MAP.put(appId, StopFrom.STREAMPARK);
    }
  }

  public static void doWatching(Application application) {
    if (isKubernetesApp(application)) {
      return;
    }
    log.info("FlinkRESTAPIWatcher add app to tracking,appId:{}", application.getId());
    WATCHING_APPS.put(application.getId(), application);
    STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
  }

  public static void addSavepoint(Long appId) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkRESTAPIWatcher add app to savepoint,appId:{}", appId);
    SAVEPOINT_CACHE.put(appId, DEFAULT_FLAG_BYTE);
  }

  public static void removeFlinkCluster(FlinkCluster flinkCluster) {
    if (FLINK_CLUSTER_MAP.containsKey(flinkCluster.getId())) {
      log.info("remove flink cluster:{}", flinkCluster.getId());
      FLINK_CLUSTER_MAP.remove(flinkCluster.getId());
    }
  }

  public static void unWatching(Long appId) {
    if (isKubernetesApp(appId)) {
      return;
    }
    log.info("FlinkRESTAPIWatcher stop app,appId:{}", appId);
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

  private FlinkCluster getFlinkCluster(Application application) {
    if (ExecutionMode.isRemoteMode(application.getExecutionModeEnum())
        || ExecutionMode.isSessionMode(application.getExecutionModeEnum())) {
      FlinkCluster flinkCluster = FLINK_CLUSTER_MAP.get(application.getFlinkClusterId());
      if (flinkCluster == null) {
        flinkCluster = flinkClusterService.getById(application.getFlinkClusterId());
        FLINK_CLUSTER_MAP.put(application.getFlinkClusterId(), flinkCluster);
      }
      return flinkCluster;
    }
    return null;
  }

  private YarnAppInfo httpYarnAppInfo(Application application) throws Exception {
    String reqURL = "ws/v1/cluster/apps/".concat(application.getAppId());
    return yarnRestRequest(reqURL, YarnAppInfo.class);
  }

  private Overview httpOverview(Application application, FlinkCluster flinkCluster)
      throws IOException {
    String appId = application.getAppId();
    if (appId != null) {
      if (application.getExecutionModeEnum().equals(ExecutionMode.YARN_APPLICATION)
          || application.getExecutionModeEnum().equals(ExecutionMode.YARN_PER_JOB)) {
        String reqURL;
        if (StringUtils.isEmpty(application.getJobManagerUrl())) {
          String format = "proxy/%s/overview";
          reqURL = String.format(format, appId);
        } else {
          String format = "%s/overview";
          reqURL = String.format(format, application.getJobManagerUrl());
        }
        return yarnRestRequest(reqURL, Overview.class);
      }
    }
    return null;
  }

  private JobsOverview httpJobsOverview(Application application, FlinkCluster flinkCluster)
      throws Exception {
    final String flinkUrl = "jobs/overview";
    ExecutionMode execMode = application.getExecutionModeEnum();
    if (ExecutionMode.YARN_PER_JOB.equals(execMode)
        || ExecutionMode.YARN_APPLICATION.equals(execMode)) {
      String reqURL;
      if (StringUtils.isEmpty(application.getJobManagerUrl())) {
        String format = "proxy/%s/" + flinkUrl;
        reqURL = String.format(format, application.getAppId());
      } else {
        String format = "%s/" + flinkUrl;
        reqURL = String.format(format, application.getJobManagerUrl());
      }
      return yarnRestRequest(reqURL, JobsOverview.class);
    } else if (ExecutionMode.REMOTE.equals(execMode)
        || ExecutionMode.YARN_SESSION.equals(execMode)) {
      if (application.getJobId() != null) {
        String remoteUrl = flinkCluster.getAddress() + "/" + flinkUrl;
        JobsOverview jobsOverview = httpRestRequest(remoteUrl, JobsOverview.class);
        if (jobsOverview != null) {
          List<JobsOverview.Job> jobs =
              jobsOverview.getJobs().stream()
                  .filter(x -> x.getId().equals(application.getJobId()))
                  .collect(Collectors.toList());
          jobsOverview.setJobs(jobs);
        }
        return jobsOverview;
      }
    }
    return null;
  }

  private CheckPoints httpCheckpoints(Application application, FlinkCluster flinkCluster)
      throws IOException {
    final String flinkUrl = "jobs/%s/checkpoints";
    ExecutionMode execMode = application.getExecutionModeEnum();
    if (ExecutionMode.YARN_PER_JOB.equals(execMode)
        || ExecutionMode.YARN_APPLICATION.equals(execMode)) {
      String reqURL;
      if (StringUtils.isEmpty(application.getJobManagerUrl())) {
        String format = "proxy/%s/" + flinkUrl;
        reqURL = String.format(format, application.getAppId(), application.getJobId());
      } else {
        String format = "%s/" + flinkUrl;
        reqURL = String.format(format, application.getJobManagerUrl(), application.getJobId());
      }
      return yarnRestRequest(reqURL, CheckPoints.class);
    } else if (ExecutionMode.REMOTE.equals(execMode)
        || ExecutionMode.YARN_SESSION.equals(execMode)) {
      if (application.getJobId() != null) {
        String remoteUrl =
            flinkCluster.getAddress() + "/" + String.format(flinkUrl, application.getJobId());
        return httpRestRequest(remoteUrl, CheckPoints.class);
      }
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
        HttpClientUtils.httpGetRequest(url, RequestConfig.custom().setConnectTimeout(5000).build());
    if (null == result) {
      return null;
    }
    return JacksonUtils.read(result, clazz);
  }

  public boolean isWatchingApp(Long id) {
    return WATCHING_APPS.containsKey(id);
  }
}
