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

import org.apache.streampark.console.core.bean.AlertProbeMsg;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.service.alert.AlertService;
import org.apache.streampark.console.core.service.application.ApplicationManageService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Component
public class AutoHealthProbingTask {

  @Autowired private ApplicationManageService applicationManageService;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  @Autowired private AlertService alertService;

  /** probe interval every 30 seconds */
  private static final Duration PROBE_INTERVAL = Duration.ofSeconds(30);

  private static final Duration PROBE_WAIT_INTERVAL = Duration.ofSeconds(5);

  private static final Short PROBE_RETRY_COUNT = 10;

  private long lastWatchTime = 0L;

  private Boolean isProbing = false;

  private Short retryAttempts = PROBE_RETRY_COUNT;

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
        probe(Collections.emptyList());
        isProbing = true;
      }
    }
  }

  public void probe(List<Application> applications) {
    List<Application> probeApp =
        applications.isEmpty() ? applicationManageService.getProbeApps() : applications;
    updateProbingState(probeApp);
    probeApp.stream().forEach(this::monitorApp);
  }

  private void updateProbingState(List<Application> probeApp) {
    probeApp.forEach(app -> app.setState(FlinkAppState.PROBING.getValue()));
    applicationManageService.updateBatchById(probeApp);
  }

  private void handleProbeResults() {
    List<Application> probeApps = applicationManageService.getProbeApps();
    if (shouldRetry(probeApps)) {
      probe(probeApps);
    } else {
      List<AlertProbeMsg> alertProbeMsgs = generateProbeResults(probeApps);
      alertProbeMsgs.stream().forEach(this::alert);
      resetProbing(probeApps);
      retryAttempts = PROBE_RETRY_COUNT;
    }
  }

  private void resetProbing(List<Application> applications) {
    applications.forEach(
        application -> {
          application.setProbing(false);
        });
    applicationManageService.updateBatchById(applications);
  }

  private void alert(AlertProbeMsg alertProbeMsg) {
    alertService.alert(alertProbeMsg);
  }

  private List<AlertProbeMsg> generateProbeResults(List<Application> applications) {
    return applications.stream()
        .collect(
            Collectors.groupingBy(
                app -> app.getUserId(),
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    apps -> {
                      List<Integer> alertIds = new ArrayList<>();
                      AlertProbeMsg alertProbeMsg = new AlertProbeMsg();
                      apps.forEach(
                          app -> {
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
                    })))
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

  private Boolean shouldRetry(List<Application> applications) {
    return applications.stream()
            .anyMatch(
                application -> FlinkAppState.LOST.getValue() == application.getState().intValue())
        && (retryAttempts-- > 0);
  }
}
