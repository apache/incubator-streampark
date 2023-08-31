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
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.fs.Options;
import org.apache.streampark.common.enums.ExecutionMode;
import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.FlinkCluster;
import org.apache.streampark.console.core.enums.FlinkAppState;
import org.apache.streampark.console.core.enums.OptionState;
import org.apache.streampark.console.core.metrics.yarn.YarnAppInfo;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.flink.kubernetes.FlinkK8sWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.Bridge.toTrackId;
import static org.apache.streampark.console.core.task.FlinkK8sWatcherWrapper.isKubernetesApp;

@Slf4j
@Component
public class AutoHealthProbingTask {

  @Autowired
  private ApplicationService applicationService;

  @Autowired private FlinkK8sWatcher k8SFlinkTrackMonitor;

  /** probe interval every 30 seconds */
  private static final Duration PROBE_INTERVAL = Duration.ofSeconds(30);

  private static final Duration PROBE_WAIT_INTERVAL = Duration.ofSeconds(5);

  private Long lastWatchTime = 0L;

  private Boolean isProbing = false;

  private static final Map<Long, Application> PROBE_APPS = new ConcurrentHashMap<>();


  @Scheduled(fixedDelay = 1000)
  private void schedule() {
    Long timeMillis = System.currentTimeMillis();
    if (isProbing && timeMillis - lastWatchTime >= PROBE_WAIT_INTERVAL.toMillis()) {
      handleProbeResults();
    }
    if (!isProbing && (timeMillis - lastWatchTime >= PROBE_INTERVAL.toMillis())) {
      lastWatchTime = timeMillis;
      cacheLostApps();
      PROBE_APPS.values().stream().forEach(this::probe);
      isProbing = true;
    }
  }

  @Transactional(rollbackFor = {Exception.class})
  public void probe(Application app) {
    updateAppStateToProbing(app);
    monitorApp(app);
  }

  private void handleProbeResults() {
    // 根据判断是否有 LOST或重试次数是否结束本次探测
    // 统计 failed，LOST，cancelled个数，
    // 发送告警
  }

  private void updateAppStateToProbing(Application app) {
    app.setState(FlinkAppState.PROBING.getValue());
    app.setOptionState(OptionState.PROBING.getValue());
    app.setOptionTime(new Date());
    applicationService.updateById(app);
  }

  private void monitorApp(Application app) {
    if (isKubernetesApp(app)) {
      k8SFlinkTrackMonitor.doWatching(toTrackId(app));
    } else {
      FlinkHttpWatcher.setOptionState(app.getId(), OptionState.PROBING);
      FlinkHttpWatcher.doWatching(app);
    }
  }

  private void cacheLostApps() {
    PROBE_APPS.clear();
    List<Application> applications =
        applicationService.list(
            new LambdaQueryWrapper<Application>()
                .eq(Application::getTracking, 1)
                .eq(Application::getState, FlinkAppState.LOST.getValue())
                .notIn(Application::getExecutionMode, ExecutionMode.getKubernetesMode()));
    applications.forEach(
        (app) -> {
          PROBE_APPS.put(app.getId(), app);
        });
  }

  public void removeProbeCacheById(Long id) {
    PROBE_APPS.remove(id);
  }

  public Boolean isProbeOptionState(int state) {
    return state == OptionState.PROBING.getValue();
  }
}
