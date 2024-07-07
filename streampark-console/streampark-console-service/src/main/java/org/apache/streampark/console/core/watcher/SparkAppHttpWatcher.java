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

import org.apache.streampark.common.util.HttpClientUtils;
import org.apache.streampark.common.util.YarnUtils;
import org.apache.streampark.console.base.util.JacksonUtils;
import org.apache.streampark.console.base.util.Tuple2;
import org.apache.streampark.console.base.util.Tuple3;
import org.apache.streampark.console.core.bean.AlertTemplate;
import org.apache.streampark.console.core.entity.SparkApplication;
import org.apache.streampark.console.core.enums.OptionStateEnum;
import org.apache.streampark.console.core.enums.SparkAppStateEnum;
import org.apache.streampark.console.core.enums.StopFromEnum;
import org.apache.streampark.console.core.metrics.spark.Job;
import org.apache.streampark.console.core.metrics.spark.SparkExecutor;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.service.application.SparkApplicationActionService;
import org.apache.streampark.console.core.service.application.SparkApplicationInfoService;
import org.apache.streampark.console.core.service.application.SparkApplicationManageService;

import org.apache.flink.annotation.VisibleForTesting;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SparkAppHttpWatcher {

  @Autowired private SparkApplicationManageService applicationManageService;

  @Autowired private SparkApplicationActionService applicationActionService;

  @Autowired private SparkApplicationInfoService applicationInfoService;

  @Autowired private AlertService alertService;

  @Qualifier("sparkRestAPIWatchingExecutor")
  @Autowired
  private Executor executorService;

  // track interval  every 5 seconds
  public static final Duration WATCHING_INTERVAL = Duration.ofSeconds(5);

  // option interval within 10 seconds
  private static final Duration OPTION_INTERVAL = Duration.ofSeconds(10);

  private static final Timeout HTTP_TIMEOUT = Timeout.ofSeconds(5);

  /**
   * Record the status of the first tracking task, because after the task is started, the overview
   * of the task will be obtained during the first tracking
   */
  private static final Cache<Long, Byte> STARTING_CACHE =
      Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES).build();

  /** tracking task list */
  private static final Map<Long, SparkApplication> WATCHING_APPS = new ConcurrentHashMap<>(0);

  /**
   *
   *
   * <pre>
   * StopFrom: marked a flink job canceling from the StreamPark or other ways:
   *    1) If stop from streampark, We can know whether to make a savepoint when flink job
   * canceling, and if We make a savepoint,
   *    We can set the savepoint as the latest savepoint, and the next time start, will be
   * automatically choose to start.
   *    2) if stop from other ways, there is no way to know the savepoint has been done, directly
   * set all the savepoint to expire,
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

  private static final Map<Long, OptionStateEnum> OPTIONING = new ConcurrentHashMap<>(0);

  private Long lastWatchTime = 0L;

  private Long lastOptionTime = 0L;

  private static final Byte DEFAULT_FLAG_BYTE = Byte.valueOf("0");

  @PostConstruct
  public void init() {
    WATCHING_APPS.clear();
    List<SparkApplication> applications =
        applicationManageService.list(
            new LambdaQueryWrapper<SparkApplication>()
                .eq(SparkApplication::getTracking, 1)
                .ne(SparkApplication::getState, SparkAppStateEnum.LOST.getValue()));
    applications.forEach(
        (app) -> {
          WATCHING_APPS.put(app.getId(), app);
          STARTING_CACHE.put(app.getId(), DEFAULT_FLAG_BYTE);
        });
  }

  @PreDestroy
  public void doStop() {
    log.info(
        "[StreamPark][SparkAppHttpWatcher] StreamPark Console will be shutdown, persistent application to database.");
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
  public @Nullable SparkAppStateEnum tryQuerySparkAppState(@Nonnull Long appId) {
    // TODO: 可以用@VisibleForTesting测试
    SparkApplication app = WATCHING_APPS.get(appId);
    return (app == null || app.getState() == null) ? null : app.getStateEnum();
  }

  private void watch(Long id, SparkApplication application) {
    executorService.execute(
        () -> {
          try {
            getStateFromYarn(application);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private StopFromEnum getAppStopFrom(Long appId) {
    return STOP_FROM_MAP.getOrDefault(appId, StopFromEnum.NONE);
  }

  /**
   * <strong>Query the job history in yarn, indicating that the task has stopped, and the final
   * status of the task is CANCELED</strong>
   *
   * @param application application
   */
  private void getStateFromYarn(SparkApplication application) throws Exception {
    OptionStateEnum optionStateEnum = OPTIONING.get(application.getId());

    // query the status from the yarn rest Api
    YarnAppInfo yarnAppInfo = httpYarnAppInfo(application);
    if (yarnAppInfo == null) {
      throw new RuntimeException("[StreamPark][SparkAppHttpWatcher] getStateFromYarn failed!");
    } else {
      try {
        String state = yarnAppInfo.getApp().getState();
        SparkAppStateEnum sparkAppStateEnum = SparkAppStateEnum.of(state);
        if (SparkAppStateEnum.OTHER == sparkAppStateEnum) {
          return;
        }
        if (SparkAppStateEnum.isEndState(sparkAppStateEnum.getValue())) {
          log.info(
              "[StreamPark][SparkAppHttpWatcher] getStateFromYarn, app {} was ended, jobId is {}, state is {}",
              application.getId(),
              application.getJobId(),
              sparkAppStateEnum);
          application.setEndTime(new Date());
        }
        if (SparkAppStateEnum.RUNNING == sparkAppStateEnum) {
          Tuple3<Double, Double, Long> resourceStatus = getResourceStatus(application);
          double memoryUsed = resourceStatus.t1;
          double maxMemory = resourceStatus.t2;
          double totalCores = resourceStatus.t3;
          log.info(
              "[StreamPark][SparkAppHttpWatcher] getStateFromYarn, app {} was running, jobId is {}, memoryUsed: {}MB, maxMemory: {}MB, totalCores: {}",
              application.getId(),
              application.getJobId(),
              String.format("%.2f", memoryUsed),
              String.format("%.2f", maxMemory),
              totalCores);
          // TODO: Modify the table structure to persist the results
        }
        application.setState(sparkAppStateEnum.getValue());
        cleanOptioning(optionStateEnum, application.getId());
        doPersistMetrics(application, false);
        if (SparkAppStateEnum.FAILED == sparkAppStateEnum
            || SparkAppStateEnum.LOST == sparkAppStateEnum
            || applicationInfoService.checkAlter(application)) {
          doAlert(application, sparkAppStateEnum);
          if (SparkAppStateEnum.FAILED == sparkAppStateEnum) {
            applicationActionService.start(application, true);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException("[StreamPark][SparkAppHttpWatcher] getStateFromYarn failed!");
      }
    }
  }

  /**
   * Calculate spark task progress from Spark rest api. (proxyed by yarn) Only available when yarn
   * application status is RUNNING.
   *
   * @param application
   * @return task progress
   * @throws Exception
   */
  private double getTasksProgress(SparkApplication application) throws Exception {
    Job[] jobs = httpJobsStatus(application);
    if (jobs.length == 0) {
      return 0.0;
    }
    Optional<Tuple2<Integer, Integer>> jobsSumOption =
        Arrays.stream(jobs)
            .map(job -> new Tuple2<>(job.getNumCompletedTasks(), job.getNumTasks()))
            .reduce((val1, val2) -> new Tuple2<>(val1.t1 + val2.t1, val1.t2 + val2.t2));
    Tuple2<Integer, Integer> jobsSum = jobsSumOption.get();
    return jobsSum.t1 * 1.0 / jobsSum.t2;
  }

  private Tuple3<Double, Double, Long> getResourceStatus(SparkApplication application)
      throws Exception {
    SparkExecutor[] executors = httpExecutorsStatus(application);
    if (executors.length == 0) {
      return new Tuple3<>(0.0, 0.0, 0L);
    }
    SparkExecutor totalExecutor =
        Arrays.stream(executors)
            .reduce(
                (e1, e2) -> {
                  SparkExecutor temp = new SparkExecutor();
                  temp.setMemoryUsed(e1.getMemoryUsed() + e2.getMemoryUsed());
                  temp.setMaxMemory(e1.getMaxMemory() + e2.getMaxMemory());
                  temp.setTotalCores(e1.getTotalCores() + e2.getTotalCores());
                  return temp;
                })
            .get();
    return new Tuple3<>(
        totalExecutor.getMemoryUsed() * 1.0 / 1024 / 1024,
        totalExecutor.getMaxMemory() * 1.0 / 1024 / 1024,
        totalExecutor.getTotalCores());
  }

  private void doPersistMetrics(SparkApplication application, boolean stopWatch) {
    if (SparkAppStateEnum.isEndState(application.getState())) {
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

  private void cleanOptioning(OptionStateEnum optionStateEnum, Long key) {
    if (optionStateEnum != null) {
      lastOptionTime = System.currentTimeMillis();
      OPTIONING.remove(key);
    }
  }

  /** set current option state */
  public static void setOptionState(Long appId, OptionStateEnum state) {
    log.info("[StreamPark][SparkAppHttpWatcher]  setOptioning");
    OPTIONING.put(appId, state);
    if (OptionStateEnum.CANCELLING == state) {
      STOP_FROM_MAP.put(appId, StopFromEnum.STREAMPARK);
    }
  }

  public static void doWatching(SparkApplication application) {
    log.info(
        "[StreamPark][SparkAppHttpWatcher] add app to tracking, appId:{}", application.getId());
    WATCHING_APPS.put(application.getId(), application);
    STARTING_CACHE.put(application.getId(), DEFAULT_FLAG_BYTE);
  }

  public static void unWatching(Long appId) {
    log.info("[StreamPark][SparkAppHttpWatcher] stop app, appId:{}", appId);
    WATCHING_APPS.remove(appId);
  }

  public static void addCanceledApp(Long appId, Long userId) {
    log.info(
        "[StreamPark][SparkAppHttpWatcher] addCanceledApp app appId:{}, useId:{}", appId, userId);
    CANCELLED_JOB_MAP.put(appId, userId);
  }

  public static Long getCanceledJobUserId(Long appId) {
    return CANCELLED_JOB_MAP.get(appId) == null ? Long.valueOf(-1) : CANCELLED_JOB_MAP.get(appId);
  }

  public static Collection<SparkApplication> getWatchingApps() {
    return WATCHING_APPS.values();
  }

  private YarnAppInfo httpYarnAppInfo(SparkApplication application) throws Exception {
    String reqURL = "ws/v1/cluster/apps/".concat(application.getJobId());
    return yarnRestRequest(reqURL, YarnAppInfo.class);
  }

  private Job[] httpJobsStatus(SparkApplication application) throws Exception {
    String format = "proxy/%s/api/v1/applications/%s/jobs";
    String reqURL = String.format(format, application.getJobId(), application.getJobId());
    return yarnRestRequest(reqURL, Job[].class);
  }

  private SparkExecutor[] httpExecutorsStatus(SparkApplication application) throws Exception {
    // "executor" is used for active executors only.
    // "allexecutor" is used for all executors including the dead.
    String format = "proxy/%s/api/v1/applications/%s/executors";
    String reqURL = String.format(format, application.getJobId(), application.getJobId());
    return yarnRestRequest(reqURL, SparkExecutor[].class);
  }

  private <T> T yarnRestRequest(String url, Class<T> clazz) throws IOException {
    String result = YarnUtils.restRequest(url, HTTP_TIMEOUT);
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
   * @param application spark application
   * @param appState spark application state
   */
  private void doAlert(SparkApplication application, SparkAppStateEnum appState) {
    AlertTemplate alertTemplate = AlertTemplate.of(application, appState);
    alertService.alert(application.getAlertId(), alertTemplate);
  }
}
