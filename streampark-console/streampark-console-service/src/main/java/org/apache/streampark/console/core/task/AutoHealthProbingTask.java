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

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.fabric8.kubernetes.client.V1AdmissionRegistrationAPIGroupClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.Options;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.common.tuple.Tuple2;
import org.apache.streampark.common.tuple.Tuple3;
import org.apache.streampark.console.core.bean.AlertProbeMsg;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nonnull;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Component
public class AutoHealthProbingTask {

  @Autowired
  private ApplicationService applicationService;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Autowired private AlertService alertService;

  /** probe interval every 30 seconds */
  private static final Duration PROBE_INTERVAL = Duration.ofSeconds(30);

  private static final Duration PROBE_WAIT_INTERVAL = Duration.ofSeconds(5);

  private static final Short PROBE_RETRY_COUNT = 10;

  private long lastWatchTime = 0L;

  private Boolean isProbing = false;

  private Short retryAttempts = PROBE_RETRY_COUNT;

  private static final Map<Long, Application> PROBE_APPS = new ConcurrentHashMap<>();


  @Scheduled(fixedDelay = 1000)
  private void schedule() {
    long timeMillis = System.currentTimeMillis();
    if (isProbing) {
      if (timeMillis - lastWatchTime >= PROBE_WAIT_INTERVAL.toMillis()) {
        handleProbeResults();
        lastWatchTime = timeMillis;
      }
    } else {
      if (timeMillis - lastWatchTime >= PROBE_INTERVAL.toMillis()) {
        lastWatchTime = timeMillis;
        probe();
        isProbing = true;
      }
    }
  }

  public void probe() {
    List<Application> applications = applicationService.getProbeApps();
    List<Application> probeApp = getLostApplications();
    updateProbingState(probeApp);
    probeApp.stream().forEach(this::monitorApp);
  }

  private List<Application> getLostApplications() {
    return PROBE_APPS
      .values()
      .stream()
      .filter(app -> FlinkAppState.isLost(app.getState()))
      .collect(Collectors.toList());
  }

  private void updateProbingState(List<Application> probeApp) {
    probeApp.forEach(app -> app.setState(FlinkAppState.PROBING.getValue()));
    applicationService.updateBatchById(probeApp);
  }

  private void handleProbeResults() {
    if (shouldRetry()) {
      probe();
    } else {
      List<Application> tempProbeApps  = applicationService.getProbeApps();
      List<AlertProbeMsg> alertProbeMsgs = generateProbeResults(tempProbeApps);
      alertProbeMsgs.stream().forEach(this::alert);
      resetProbing();
      retryAttempts = PROBE_RETRY_COUNT;
    }
  }

  private void resetProbing() {
    PROBE_APPS.values().forEach(application -> {
      application.setProbing(false);
    });
    applicationService.updateBatchById(PROBE_APPS.values());
  }

  private void alert(AlertProbeMsg alertProbeMsg) {
    alertService.alert(alertProbeMsg);
  }

  private List<AlertProbeMsg> generateProbeResults(List<Application> applications) {
    return applications
      .stream()
      .collect(Collectors.groupingBy(
        app -> app.getUserId(),
        Collectors.collectingAndThen(
          Collectors.toList(),
          apps -> {
            List<Integer> alertIds = new ArrayList<>();
            AlertProbeMsg alertProbeMsg = new AlertProbeMsg();
            apps.forEach(app -> {
              alertProbeMsg.setUser(app.getUserName());
              alertProbeMsg.incrementProbeJobs();
              if (app.getState() == FlinkAppState.LOST.getValue()) {
                alertProbeMsg.incrementFailedJobs();
              } else if (app.getState() == FlinkAppState.FAILED.getValue()) {
                alertProbeMsg.incrementFailedJobs();
              } else if (app.getState() == FlinkAppState.CANCELED.getValue()) {
                alertProbeMsg.incrementCancelledJobs();
              }
              alertIds.add(app.getAlertId());
            });

            alertProbeMsg.setAlertId(alertIds);
            return alertProbeMsg;
          }
        )
      ))
      .values()
      .stream()
      .collect(Collectors.toList());
  }

  private void monitorApp(Application app) {
    if (isKubernetesApp(app)) {
      k8SFlinkTrackMonitor.doWatching(toTrackId(app));
    } else {
      FlinkHttpWatcher.doWatching(app);
    }
  }

  private void cacheProbingApps() {
    PROBE_APPS.clear();
    List<Application> applications = applicationService.getProbeApps();
    applications.forEach(
        (app) -> {
          PROBE_APPS.put(app.getId(), app);
        });
  }

  private Boolean shouldRetry() {
    return PROBE_APPS.values().stream().anyMatch(application -> FlinkAppState.LOST.getValue() == application.getState().intValue())
      && (retryAttempts-- > 0);
  }
}
