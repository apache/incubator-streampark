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

import org.apache.streampark.console.core.entity.Application;
import org.apache.streampark.console.core.entity.SavePoint;
import org.apache.streampark.console.core.enums.CheckPointStatus;
import org.apache.streampark.console.core.enums.FailoverStrategy;
import org.apache.streampark.console.core.metrics.flink.CheckPoints;
import org.apache.streampark.console.core.service.ApplicationService;
import org.apache.streampark.console.core.service.SavePointService;
import org.apache.streampark.console.core.service.alert.AlertService;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class CheckpointProcessor {

  private final Cache<String, Long> checkPointCache =
      Caffeine.newBuilder().expireAfterAccess(1, TimeUnit.DAYS).build();

  private final Map<Long, Counter> checkPointFailedCache = new ConcurrentHashMap<>(0);

  @Autowired private ApplicationService applicationService;

  @Autowired private AlertService alertService;

  @Autowired private SavePointService savePointService;

  public void process(Long appId, CheckPoints checkPoints) {
    CheckPoints.Latest latest = checkPoints.getLatest();
    if (latest == null || latest.getCompleted() == null) {
      return;
    }
    CheckPoints.CheckPoint checkPoint = latest.getCompleted();
    Application application = applicationService.getById(appId);
    CheckPointStatus status = checkPoint.getCheckPointStatus();

    if (CheckPointStatus.COMPLETED.equals(status)) {
      String cacheId = appId + "_" + application.getJobId();
      Long latestId =
          checkPointCache.get(
              cacheId,
              key -> {
                SavePoint savePoint = savePointService.getLatest(appId);
                return Optional.ofNullable(savePoint).map(SavePoint::getChkId).orElse(null);
              });

      if (latestId == null || latestId < checkPoint.getId()) {
        saveSavepoint(checkPoint, application);
        checkPointCache.put(cacheId, checkPoint.getId());
      }
    } else if (CheckPointStatus.FAILED.equals(status) && application.cpFailedTrigger()) {
      Counter counter = checkPointFailedCache.get(appId);
      if (counter == null) {
        checkPointFailedCache.put(appId, new Counter(checkPoint.getTriggerTimestamp()));
      } else {
        long minute = counter.getDuration(checkPoint.getTriggerTimestamp());
        if (minute <= application.getCpFailureRateInterval()
            && counter.getCount() >= application.getCpMaxFailureInterval()) {
          checkPointFailedCache.remove(appId);
          FailoverStrategy failoverStrategy = FailoverStrategy.of(application.getCpFailureAction());
          if (failoverStrategy == null) {
            throw new IllegalArgumentException(
                "Unexpected cpFailureAction: " + application.getCpFailureAction());
          }
          switch (failoverStrategy) {
            case ALERT:
              alertService.alert(application, CheckPointStatus.FAILED);
              break;
            case RESTART:
              try {
                applicationService.restart(application);
              } catch (Exception e) {
                throw new RuntimeException(e);
              }
              break;
          }
        } else {
          counter.increment();
        }
      }
    }
  }

  private void saveSavepoint(CheckPoints.CheckPoint checkPoint, Application application) {
    SavePoint savePoint = new SavePoint();
    savePoint.setAppId(application.getId());
    savePoint.setChkId(checkPoint.getId());
    savePoint.setLatest(true);
    savePoint.setType(checkPoint.getCheckPointType().get());
    savePoint.setPath(checkPoint.getExternalPath());
    savePoint.setTriggerTime(new Date(checkPoint.getTriggerTimestamp()));
    savePoint.setCreateTime(new Date());
    savePointService.save(savePoint);
  }

  public static class Counter {
    private final Long timestamp;
    private final AtomicInteger count;

    public Counter(Long timestamp) {
      this.timestamp = timestamp;
      this.count = new AtomicInteger(1);
    }

    public void increment() {
      this.count.incrementAndGet();
    }

    public Integer getCount() {
      return count.get();
    }

    public long getDuration(Long currentTimestamp) {
      return (currentTimestamp - this.timestamp) / 1000 / 60;
    }
  }
}
